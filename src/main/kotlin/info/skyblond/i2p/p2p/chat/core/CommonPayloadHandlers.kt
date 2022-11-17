package info.skyblond.i2p.p2p.chat.core

import info.skyblond.i2p.p2p.chat.message.MessagePayload
import info.skyblond.i2p.p2p.chat.message.NoContentReply
import info.skyblond.i2p.p2p.chat.message.PEXReply
import info.skyblond.i2p.p2p.chat.message.PEXRequest
import mu.KLogger
import net.i2p.data.DataFormatException
import net.i2p.data.Destination
import java.util.*

private fun <ContextType : SessionContext> Peer<ContextType>.handlePEX(
    session: PeerSession<ContextType>, peerInfos: List<PeerInfo>, logger: KLogger,
    filter: (PeerInfo) -> Boolean,
    onException: (Throwable, PeerInfo) -> Unit,
) {
    var count = 0
    peerInfos
        .filter(filter)
        .forEach { peerInfo ->
            val addr = peerInfo.info[PeerInfo.INFO_KEY_DEST] ?: return@forEach
            if (addr.endsWith(".b32.i2p")) {
                this.addNewPeer(addr, { onException(it, peerInfo) })
                count++
            } else {
                try {
                    val dest = Destination(addr)
                    this.addNewPeer(dest, { onException(it, peerInfo) })
                    count++
                } catch (e: DataFormatException) {
                    onException(e, peerInfo)
                }
            }
        }
    logger.info { "Collected $count peers from ${session.getDisplayName()}" }
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
    onException: (Throwable, PeerInfo) -> Unit = { t, p -> logger.warn(t) { "Failed to connect new peer $p" } },
): MessagePayloadHandler<ContextType, PEXRequest> =
    object : MessagePayloadHandler<ContextType, PEXRequest>(PEXRequest::class) {
        override fun handleTyped(
            peer: Peer<ContextType>, session: PeerSession<ContextType>,
            messageId: UUID, payload: PEXRequest
        ): MessagePayload = PEXReply(messageId, peer.dumpKnownPeer()).also {
            peer.handlePEX(session, payload.peerInfos, logger, filter, onException)
        }
    }

/**
 * Generate a handler for [PEXReply]
 * */
fun <ContextType : SessionContext> createPEXReplyHandler(
    logger: KLogger,
    filter: (PeerInfo) -> Boolean,
    onException: (Throwable, PeerInfo) -> Unit = { t, p -> logger.warn(t) { "Failed to connect new peer $p" } },
): MessagePayloadHandler<ContextType, PEXReply> =
    object : MessagePayloadHandler<ContextType, PEXReply>(PEXReply::class) {
        override fun handleTyped(
            peer: Peer<ContextType>, session: PeerSession<ContextType>,
            messageId: UUID, payload: PEXReply
        ): MessagePayload = PEXReply(messageId, peer.dumpKnownPeer()).also {
            peer.handlePEX(session, payload.peerInfos, logger, filter, onException)
        }
    }

/**
 * A handler that do nothing on [NoContentReply].
 * */
fun <ContextType : SessionContext> createNoContentReplyHandler(): MessagePayloadHandler<ContextType, NoContentReply> =
    object : MessagePayloadHandler<ContextType, NoContentReply>(NoContentReply::class) {
        override fun handleTyped(
            peer: Peer<ContextType>, session: PeerSession<ContextType>,
            messageId: UUID, payload: NoContentReply
        ): MessagePayload? = null
    }

