package info.skyblond.i2p.p2p.chat.message

import info.skyblond.i2p.p2p.chat.json.UUIDAsStringSerializer
import info.skyblond.i2p.p2p.chat.message.MessagePayload.Type.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

/**
 * The root of all customizable message contents.
 * Currently, there are three types of messages:
 * Request, Reply, and Error.
 * */
interface MessagePayload {
    val messageType: Type

    /**
     * There are two message type:
     * [REQUEST]: Someone sends to our, request us to do something.
     * [REPLY]: We handled the request, return the result.
     * [ERROR]: Failed to handle the request.
     * */
    enum class Type {
        REQUEST, REPLY, ERROR
    }
}

/**
 * A general representation of request.
 * */
abstract class RequestMessagePayload : MessagePayload {
    final override val messageType: MessagePayload.Type = MessagePayload.Type.REQUEST
}

/**
 * A general representation of reply.
 * The [replyTo] is the message UUID that you want to reply.
 * */
abstract class ReplyMessagePayload : MessagePayload {
    abstract val replyTo: UUID
    final override val messageType: MessagePayload.Type = MessagePayload.Type.REPLY
}

/**
 * A general representation of error.
 * Normally, all errors have a [msg] to tell human what's going wrong.
 * The [errorAt] will try to figure out which message has error. But sometimes it can be zero.
 *
 * Also, you may want to include other infos like what's the bad parameter in 404,
 * what's the cause of a 500, etc.
 * */
abstract class ErrorMessagePayload : MessagePayload {
    abstract val errorAt: UUID
    abstract val msg: String
    final override val messageType: MessagePayload.Type = MessagePayload.Type.ERROR
}

/**
 * This is a simple and basic message framework.
 * All advanced feature should be implemented in [payload],
 * like signature, command, response, etc.
 * */
@Serializable
data class P2PMessage(
    @Serializable(with = UUIDAsStringSerializer::class)
    val id: UUID,
    @Contextual
    val payload: MessagePayload
) {
    companion object {
        // The randomness of UUID is relatively ok.
        // the message id is used to reference a previous sent message,
        // and discarded after the message is processed.
        // Relatively short lifespan.
        fun createRequest(payload: RequestMessagePayload): P2PMessage =
            P2PMessage(UUID.randomUUID(), payload)

        fun createReply(payload: ReplyMessagePayload): P2PMessage =
            P2PMessage(UUID.randomUUID(), payload)

        fun createError(payload: ErrorMessagePayload): P2PMessage =
            P2PMessage(UUID.randomUUID(), payload)

        fun createMessage(payload: MessagePayload): P2PMessage =
            P2PMessage(UUID.randomUUID(), payload)
    }
}
