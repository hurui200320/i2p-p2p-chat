package info.skyblond.i2p.p2p.chat.core

import info.skyblond.i2p.p2p.chat.I2PHelper.runThread
import info.skyblond.i2p.p2p.chat.message.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KLogger
import net.i2p.client.streaming.I2PSocket
import net.i2p.data.Destination
import java.io.IOException
import java.util.*
import java.util.concurrent.BlockingQueue

/**
 * Data class representing a session, aka, connection.
 * The session is async, aka no waiting between sending a message
 * and reading the response.
 *
 * Each session has a context, where you can store something like
 * authentication status, protocol status, etc.
 * */
class PeerSession<ContextType : SessionContext>(
    peerInfo: PeerInfo,
    applicationName: String,
    private val json: Json,
    private val socket: I2PSocket,
    private val sessionContext: ContextType,
    private val incomingMessageQueue: BlockingQueue<MessageQueueEntry<ContextType>>,
    private val logger: KLogger,
    private val onSocketClosed: (PeerSession<ContextType>) -> Unit = {},
) : AutoCloseable {
    private val input = socket.inputStream.bufferedReader()
    private val output = socket.outputStream.bufferedWriter()

    fun <T> useContextSync(block: ContextType.() -> T): T =
        synchronized(sessionContext) {
            block(sessionContext)
        }

    fun getPeerDestination(): Destination =
        socket.peerDestination ?: error("Null destination caused by null connection")

    fun getDisplayName(): String = getPeerDestination().toBase32()
        .let { b32 -> useContextSync { nickname }?.let { nickname -> "$nickname ($b32)" } ?: b32 }

    init {
        runThread {
            try {
                input.useLines { lines ->
                    // read line one by one forever
                    lines.forEach { base64Line ->
                        val jsonText = try {
                            Base64.getDecoder().decode(base64Line).decodeToString()
                        } catch (e: IllegalArgumentException) {
                            sendError(InvalidIncomingDataError("Malformed base64: ${e.message}"))
                            return@forEach
                        }
                        val message = try {
                            json.decodeFromString<P2PMessage>(jsonText)
                        } catch (e: SerializationException) {
                            sendError(InvalidIncomingDataError("Error when deserializing json: ${e.message}"))
                            close("Malformed data")
                            return@forEach
                        } catch (e: IllegalArgumentException) {
                            sendError(InvalidIncomingDataError("Malformed json: ${e.message}"))
                            close("Malformed data")
                            return@forEach
                        }
                        // parsing ok, add to queue
                        incomingMessageQueue.add(MessageQueueEntry(this, message))
                    }
                }
            } catch (_: IOException) {
                logger.warn { "Socket from ${getDisplayName()} closed due to IO exception" }
            } catch (t: Throwable) {
                logger.warn(t) { "Socket from ${getDisplayName()} closed!" }
            }
            // something happened, close the socket
            close()
        }
        // send auth
        sendMessage(P2PMessage.createRequest(AuthRequest(applicationName, peerInfo)))
    }

    /**
     * Send message.
     * @return true if sent without IO exception.
     * */
    private fun sendMessageInternal(message: P2PMessage): Boolean {
        synchronized(socket) {
            if (socket.isClosed) {
                logger.warn { "Sending message to closed socket ${getDisplayName()}" }
                return false
            }
            val jsonText = json.encodeToString(message)
            logger.debug { "Send to ${getDisplayName()}: $jsonText" }
            val base64 = Base64.getEncoder().encodeToString(jsonText.encodeToByteArray())
            try {
                output.write(base64)
                if (!base64.endsWith("\n"))
                    output.newLine()
                output.flush()
            } catch (e: IOException) {
                logger.warn { "Failed to write socket. Socket ${getDisplayName()} closed!" }
                return false
            }
        }
        return true
    }

    /**
     * Send message. Close socket if IO exception occurred.
     * */
    private fun sendMessage(message: P2PMessage) {
        synchronized(socket) {
            if (!sendMessageInternal(message)) {
                // failed to send due to I/O errors
                // then close the socket
                close()
            }
        }
    }

    /**
     * Same as [sendMessage], but wrapped general [payload] payload as [P2PMessage].
     * */
    fun sendPayload(payload: MessagePayload) {
        sendMessage(P2PMessage.createMessage(payload))
    }

    /**
     * Same as [sendMessage], but wrapped [request] payload as [P2PMessage].
     * */
    fun sendRequest(request: RequestMessagePayload) {
        sendMessage(P2PMessage.createRequest(request))
    }

    /**
     * Same as [sendMessage], but wrapped [reply] payload as [P2PMessage].
     * */
    fun sendReply(reply: ReplyMessagePayload) {
        sendMessage(P2PMessage.createReply(reply))
    }

    /**
     * Same as [sendMessage], but wrapped [error] payload as [P2PMessage].
     * */
    fun sendError(error: ErrorMessagePayload) {
        sendMessage(P2PMessage.createError(error))
    }

    fun isClosed(): Boolean = synchronized(socket) { socket.isClosed }

    /**
     * Send a [ByeRequest] and close the socket.
     * Used when you want to close a functioning socket.
     * */
    fun close(reason: String?) {
        synchronized(socket) {
            if (socket.isClosed) return
            reason?.let { sendMessageInternal(P2PMessage.createRequest(ByeRequest(it))) }
            try {
                socket.close()
                input.close()
                output.close()
            } catch (_: IOException) {
                // It won't hurt anything if the close failed.
                // It might be already broken/closed.
            }
            onSocketClosed(this)
        }
    }

    /**
     * Close the session without sending [ByeRequest].
     * Use when I/O exception occurred and want to close the socket.
     * */
    override fun close() {
        close(null)
    }
}
