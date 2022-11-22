package info.skyblond.i2p.p2p.chat.core

import info.skyblond.i2p.p2p.chat.I2PHelper.runThread
import info.skyblond.i2p.p2p.chat.message.PEXRequest
import info.skyblond.i2p.p2p.chat.message.RequestMessagePayload
import kotlinx.serialization.json.Json
import mu.KLogger
import mu.KotlinLogging
import net.i2p.I2PAppContext
import net.i2p.I2PException
import net.i2p.client.streaming.I2PServerSocket
import net.i2p.client.streaming.I2PSocket
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.data.Destination
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This is a self-contain peer.
 * */
class Peer<ContextType : SessionContext>(
    private val json: Json,
    private val socketManager: I2PSocketManager,
    val applicationName: String,
    private val sessionContextProvider: (SessionSource) -> ContextType,
    private val selfPeerInfoProvider: (Peer<ContextType>) -> PeerInfo,
    private val messageHandler: IncomingMessageHandler<ContextType>,
    /**
     * All time-consuming operation will happen here.
     * */
    private val threadPool: ThreadPoolExecutor,
    /**
     * Interval between PEX, measured in ms.
     * Default: 10s.
     * */
    private val pexInterval: Long = 10 * 1000,
    private val logger: KLogger = KotlinLogging.logger { },
    /**
     * Do something when underlying socket closed
     * */
    private val onSessionSocketClose: (PeerSession<ContextType>) -> Unit = {}
) : AutoCloseable {

    private val incomingMessageQueue: BlockingQueue<MessageQueueEntry<ContextType>> = LinkedBlockingQueue()
    private val serverSocket: I2PServerSocket = socketManager.serverSocket
    private val sessions = ConcurrentHashMap<Destination, PeerSession<ContextType>>()
    private val closedFlag = AtomicBoolean(false)

    /**
     * The queue might contain two address: b32 or dest.
     * By using this callback, when adding new peers, it can be () -> peer.connect(b32),
     * or () -> peer.connect(dest).
     *
     * The second is error handler
     * */
    private val newPeerQueue: BlockingQueue<Pair<() -> Unit, (Throwable) -> Unit>> = LinkedBlockingQueue()


    /**
     * Create a peer session.
     * */
    private fun createPeerSession(
        sessionSource: SessionSource,
        socket: I2PSocket
    ): PeerSession<ContextType> = PeerSession(
        selfPeerInfoProvider(this), applicationName, json, socket,
        sessionContextProvider(sessionSource), incomingMessageQueue, logger
    ) { sessions.remove(it.getPeerDestination()); onSessionSocketClose(it) }

    init {
        // handle new peers
        runThread {
            while (!closedFlag.get()) {
                val (connect, handleError) = newPeerQueue.take()
                threadPool.execute {
                    try {
                        connect()
                    } catch (t: Throwable) {
                        handleError(t)
                    }
                }
            }
        }
        // handle incoming message
        runThread {
            // if peer closed, no need to process messages
            while (!closedFlag.get()) {
                val entry = incomingMessageQueue.take()
                // do things sync, ensure first in first handled
                messageHandler.handle(
                    this,
                    entry.session,
                    entry.message.id,
                    entry.message.payload,
                    threadPool
                )
            }
        }
        // handle server part
        runThread {
            while (!closedFlag.get()) {
                try {
                    val socket = serverSocket.accept() ?: continue
                    // We might connect to this peer while they're trying to connect us
                    // For example:
                    //     T1: We connect to peer, peer accepts the socket.
                    //     T2: We know socket is accepted, we create a session (we -> peer).
                    //     T3: The peer's PEX kicks in and make connection to us.
                    //         Now peer's server has to wait PEX to finish to use the session map.
                    //     T4: Peer's PEX connects to us, but we notice it's duplicated.
                    //         Duplicated session will be closed immediately (peer -> we)
                    //     T5: Peer create a client session for the socket in T4 (peer -> we)
                    //         This session is a ghost session for peer.
                    //     T6: Peer's server notices the ghost session and think it's duplicated.
                    //         Our session was closed by peer (we -> peer)
                    // The result is: both peer and us think they're getting duplicate connections,
                    //                while both get closed sessions.
                    // However, if we don't detect duplicate connections, the same message
                    // to same peer will be sent multiple times. Also, someone might spam
                    // connections to us, draining our CPU resources.
                    // A simple fix is try later.
                    sessions.compute(socket.peerDestination) { dest, oldSession ->
                        if (oldSession != null && !oldSession.isClosed()) {
                            // old still alive, kill new one and return old one
                            logger.warn { "Duplicated connection from ${dest.toBase32()}" }
                            socket.reset()
                            return@compute oldSession
                        }
                        // accept new socket
                        createPeerSession(SessionSource.SERVER, socket)
                    }
                } catch (e: ConnectException) {
                    logger.warn { "Server socket ${getMyB32Address()} closed or interrupted!" }
                    break
                } catch (e: I2PException) {
                    logger.warn { "I2CP session for ${getMyB32Address()} closed or broken!" }
                    break
                } catch (_: Throwable) {
                }
            }
            logger.warn { "Peer stop handle incoming connections: ${getMyB32Address()}" }
            close()
        }
        // exchange peer every X minutes
        runThread {
            var lastActionTime = System.currentTimeMillis()
            while (!closedFlag.get()) {
                try {
                    while (System.currentTimeMillis() - lastActionTime < pexInterval) Thread.sleep(1000)
                    val request = PEXRequest(dumpKnownPeer())
                    logger.debug { "Doing PEX for peer ${getMyB32Address()}" }
                    sessions.filter { it.value.useContextSync { isAuthed() && isAccepted() } && !it.value.isClosed() }
                        .forEach { threadPool.execute { it.value.sendRequest(request) } }
                    lastActionTime = System.currentTimeMillis()
                } catch (_: InterruptedException) {
                } catch (t: Throwable) {
                    logger.warn(t) { "Failed to PEX" }
                    lastActionTime = System.currentTimeMillis()
                }
            }
            logger.warn { "Stop PEX due to peer ${getMyB32Address()} closed" }
        }
        // print self info
        logger.info { "I'm ${getMyB32Address()}" }
    }

    fun getMyDestination(): Destination = socketManager.session.myDestination ?: error("Our destination not available")

    fun getMyB32Address(): String = getMyDestination().toBase32()

    fun addNewPeer(
        b32: String,
        errorHandler: (t: Throwable) -> Unit
    ) {
        newPeerQueue.add({ connect(b32) } to errorHandler)
    }

    fun addNewPeer(
        dest: Destination,
        errorHandler: (t: Throwable) -> Unit
    ) {
        newPeerQueue.add({ connect(dest) } to errorHandler)
    }

    fun dumpKnownPeer(): List<PeerInfo> =
        sessions.mapNotNull { it.value.useContextSync { peerInfo } }.toList()

    fun dumpSessions(): List<PeerSession<ContextType>> = sessions.values.toList()

    /**
     * Connect to a b32 address: something.b32.i2p
     * */
    @Throws(
        ConnectException::class,
        NoRouteToHostException::class,
        InterruptedIOException::class,
        I2PException::class,
        IOException::class,
        UnknownHostException::class
    )
    fun connect(b32: String) {
        val nameService = I2PAppContext.getGlobalContext().namingService()
        val dest = nameService.lookup(b32) ?: throw UnknownHostException("Host not found: $b32")
        connect(dest)
    }

    /**
     * Connect to a given peer, one by one.
     * */
    @Throws(
        ConnectException::class,
        NoRouteToHostException::class,
        InterruptedIOException::class,
        I2PException::class,
        IOException::class
    )
    fun connect(dest: Destination) {
        if (closedFlag.get()) return
        // do not connect to ourselves
        if (dest == getMyDestination()) return

        sessions.compute(dest) { _, oldSession ->
            if (oldSession != null && !oldSession.isClosed()) {
                // old still alive, do nothing and return old one
                return@compute oldSession
            }
            // make new connection
            logger.info { "Connecting to peer ${dest.toBase32()}" }
            val socket = socketManager.connect(dest)
            createPeerSession(SessionSource.CLIENT, socket)
        }
    }

    /**
     * Disconnect all sessions that [filter] return true.
     * */
    fun disconnect(reason: String = "Disconnect by user", filter: (PeerSession<ContextType>) -> Boolean) {
        if (closedFlag.get()) return
        sessions.forEach { if (filter(it.value)) it.value.close(reason) }
    }

    fun sendRequest(message: RequestMessagePayload, filter: (PeerSession<ContextType>) -> Boolean) {
        if (closedFlag.get()) return
        sessions.forEach { if (filter(it.value)) threadPool.execute { it.value.sendRequest(message) } }
    }

    fun isClosed(): Boolean = closedFlag.get()

    override fun close() {
        closedFlag.set(true)
        sessions.forEach { it.value.close("Peer closing") }
        serverSocket.close()
    }
}
