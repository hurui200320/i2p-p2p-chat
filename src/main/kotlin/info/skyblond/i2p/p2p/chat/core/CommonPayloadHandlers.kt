package info.skyblond.i2p.p2p.chat.core

import info.skyblond.i2p.p2p.chat.message.MessagePayload
import info.skyblond.i2p.p2p.chat.message.NoContentReply
import info.skyblond.i2p.p2p.chat.message.PEXReply
import info.skyblond.i2p.p2p.chat.message.PEXRequest
import mu.KLogger
import net.i2p.data.DataFormatException
import net.i2p.data.Destination
import java.util.*
import java.util.concurrent.ThreadPoolExecutor

/**
 * Create [MessagePayloadHandler] using lambda.
 * */
inline fun <C : SessionContext, reified T : MessagePayload> createMessageHandlerWithReply(
    crossinline block: (
        peer: Peer<C>, session: PeerSession<C>,
        messageId: UUID, payload: T,
        threadPool: ThreadPoolExecutor
    ) -> MessagePayload
): MessagePayloadHandler<C, T> =
    object : MessagePayloadHandler<C, T>(T::class) {
        override fun handleTyped(
            peer: Peer<C>, session: PeerSession<C>,
            messageId: UUID, payload: T,
            threadPool: ThreadPoolExecutor
        ): MessagePayload = block(peer, session, messageId, payload, threadPool)
    }

inline fun <C : SessionContext, reified T : MessagePayload> createMessageHandlerNoReply(
    crossinline block: (
        peer: Peer<C>, session: PeerSession<C>, payload: T,
        threadPool: ThreadPoolExecutor
    ) -> Unit
): MessagePayloadHandler<C, T> =
    object : MessagePayloadHandler<C, T>(T::class) {
        override fun handleTyped(
            peer: Peer<C>, session: PeerSession<C>,
            messageId: UUID, payload: T,
            threadPool: ThreadPoolExecutor
        ): MessagePayload? = block(peer, session, payload, threadPool).let { null }
    }

/**
 * A handler that do nothing on [NoContentReply].
 * */
fun <C : SessionContext> createNoContentReplyHandler(): MessagePayloadHandler<C, NoContentReply> =
    createMessageHandlerNoReply { _, _, _, _ -> }

private fun <ContextType : SessionContext> Peer<ContextType>.handlePEX(
    session: PeerSession<ContextType>, peerInfos: List<PeerInfo>, logger: KLogger,
    filter: (PeerInfo) -> Boolean,
    onException: (Throwable, PeerInfo) -> Unit
) {
    var count = 0
    peerInfos
        .filter(filter)
        .forEach { peerInfo ->
            val addr = peerInfo.info[PeerInfo.INFO_KEY_DEST] ?: return@forEach
            if (addr.endsWith(".b32.i2p")) {
                this.addNewPeer(addr) { onException(it, peerInfo) }
                count++
            } else {
                try {
                    val dest = Destination(addr)
                    this.addNewPeer(dest) { onException(it, peerInfo) }
                    count++
                } catch (e: DataFormatException) {
                    onException(e, peerInfo)
                }
            }
        }
    logger.debug { "Got $count peers from ${session.getDisplayName()}" }
}

/**
 * Generate a handler for [PEXRequest]
 * */
fun <ContextType : SessionContext> createPEXRequestHandler(
    logger: KLogger,
    /**
     * Do your verify here
     * */
    filter: (PeerInfo) -> Boolean,
    /**
     * Handle when things go wrong.
     * */
    onException: (Throwable, PeerInfo) -> Unit
): MessagePayloadHandler<ContextType, PEXRequest> =
    createMessageHandlerWithReply { peer, session, messageId, payload, threadPool ->
        threadPool.execute { peer.handlePEX(session, payload.peerInfos, logger, filter, onException) }
        PEXReply(messageId, peer.dumpKnownPeer())
    }

/**
 * Generate a handler for [PEXReply]
 * */
fun <ContextType : SessionContext> createPEXReplyHandler(
    logger: KLogger,
    filter: (PeerInfo) -> Boolean,
    onException: (Throwable, PeerInfo) -> Unit
): MessagePayloadHandler<ContextType, PEXReply> =
    createMessageHandlerNoReply { peer, session, payload, threadPool ->
        threadPool.execute { peer.handlePEX(session, payload.peerInfos, logger, filter, onException) }
    }
