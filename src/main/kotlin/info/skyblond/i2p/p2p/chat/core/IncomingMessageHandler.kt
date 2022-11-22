package info.skyblond.i2p.p2p.chat.core

import info.skyblond.i2p.p2p.chat.message.*
import mu.KotlinLogging
import net.i2p.data.Destination
import java.util.*
import java.util.concurrent.ThreadPoolExecutor
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.jvm.jvmName

/**
 * The interface of handling messages.
 *
 * @see [GeneralIncomingMessageHandler]
 * */
interface IncomingMessageHandler<ContextType : SessionContext> {
    fun handle(
        peer: Peer<ContextType>,
        session: PeerSession<ContextType>,
        messageId: UUID,
        payload: MessagePayload,
        threadPool: ThreadPoolExecutor
    )
}


abstract class MessagePayloadHandler<ContextType : SessionContext, PayloadType : MessagePayload>(
    internal val payloadType: KClass<PayloadType>
) {
    /**
     * @see Class.isInterface
     * */
    fun isInterface(): Boolean = payloadType.java.isInterface

    /**
     * @see KClass.isAbstract
     * */
    fun isAbstractClass(): Boolean = payloadType.isAbstract

    /**
     * @see KClass.isInstance
     * */
    fun canHandle(value: MessagePayload): Boolean = payloadType.isInstance(value)

    /**
     * @see KClass.cast
     * */
    private fun cast(value: MessagePayload): PayloadType = payloadType.cast(value)

    fun handle(
        peer: Peer<ContextType>,
        session: PeerSession<ContextType>,
        messageId: UUID,
        payload: MessagePayload,
        threadPool: ThreadPoolExecutor
    ): MessagePayload? = handleTyped(peer, session, messageId, cast(payload), threadPool)

    protected abstract fun handleTyped(
        peer: Peer<ContextType>,
        session: PeerSession<ContextType>,
        messageId: UUID,
        payload: PayloadType,
        threadPool: ThreadPoolExecutor
    ): MessagePayload?
}

/**
 * A default message handler. With some built-in features.
 * */
class GeneralIncomingMessageHandler<ContextType : SessionContext>(
    /**
     * Search class, if not found then abstract class, if not found then interface.
     * */
    vararg _handlers: MessagePayloadHandler<ContextType, *>
) : IncomingMessageHandler<ContextType> {
    private val logger = KotlinLogging.logger { }

    init {
        val set = mutableSetOf<KClass<*>>()
        _handlers.forEach {
            require(!set.contains(it.payloadType)) {
                "Duplicated handlers for payload ${it.payloadType.jvmName}. Each type of payload can be only handled by one handler."
            }
            set.add(it.payloadType)
        }
    }

    // copy array into a immutable list
    private val handlers = _handlers.toList()

    override fun handle(
        peer: Peer<ContextType>,
        session: PeerSession<ContextType>,
        messageId: UUID,
        payload: MessagePayload,
        threadPool: ThreadPoolExecutor
    ) {
        // pre-process, for unauthorized messages
        if (session.useContextSync { !isAuthed() }) {
            if (payload !is AuthRequest && payload !is ByeRequest) {
                // reject all messages other than auth request and bye request
                session.sendError(UnauthorizedError(messageId))
                session.close("Unauthorized session")
                return // stop processing
            }
        }

        // for all bye request, close session
        if (payload is ByeRequest) {
            session.close()
        }

        // perform additional check on auth request
        if (payload is AuthRequest) {
            // check application
            // Different applications might have different custom payloads
            if (peer.applicationName != payload.applicationName) {
                // wrong application name, reject
                session.sendError(AuthenticationFailedError("Different application", messageId))
                session.close("Failed to auth: different application")
                return // stop processing
            }
            // check destination
            try {
                // additional check on auth, make sure the dest is correct
                val dest = Destination(payload.peerInfo.info[PeerInfo.INFO_KEY_DEST])
                require(dest == session.getPeerDestination())
            } catch (t: Throwable) {
                session.sendError(AuthenticationFailedError("Destination not match", messageId))
                session.close("Failed to auth: wrong dest")
                return // stop processing if failed
            }
        }

        // find normal class, then abstract class, then interface
        val handler = handlers.filterNot { it.isInterface() || it.isAbstractClass() }.find { it.canHandle(payload) }
            ?: handlers.filter { !it.isInterface() && it.isAbstractClass() }.find { it.canHandle(payload) }
            ?: handlers.filter { it.isInterface() }.find { it.canHandle(payload) }

        if (handler == null) {
            logger.warn {
                "Message handler not found for type ${payload::class.jvmName}\n" +
                        "Message from ${session.getDisplayName()}: $payload"
            }
            if (payload is RequestMessagePayload || payload is ReplyMessagePayload) {
                // send error if the message is request or reply
                session.sendError(UnsupportedMessageError(messageId, payload::class.jvmName))
            }
            return
        }

        // handle message
        var reply = handler.handle(peer, session, messageId, payload, threadPool)
        if (payload is RequestMessagePayload && reply == null) {
            // make sure request always get reply
            reply = NoContentReply(messageId)
        }
        if (reply != null) {
            // decide if we should send reply
            val shouldSendReply = when (payload) {
                is RequestMessagePayload -> reply is ReplyMessagePayload || reply is ErrorMessagePayload
                is ReplyMessagePayload -> reply is ErrorMessagePayload
                else -> false
            }
            if (shouldSendReply) {
                session.sendPayload(reply)
            }
            // post-process for auth
            if (payload is AuthRequest) {
                if (reply is ErrorMessagePayload) {
                    // auth failed, disconnect
                    session.close("Authentication failed")
                } else {
                    // auth ok
                    session.useContextSync {
                        peerInfo = payload.peerInfo
                        onAuthAccepted()
                    }
                }
            }
        }
    }
}
