package info.skyblond.i2p.p2p.chat.message

import info.skyblond.i2p.p2p.chat.json.UUIDAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Error happens before putting message into incoming queue.
 * Like malformed base64/json.
 * */
@Serializable
@SerialName("InvalidIncomingDataError")
data class InvalidIncomingDataError(
    val cause: String,
) : ErrorMessagePayload() {
    @Serializable(with = UUIDAsStringSerializer::class)
    override val errorAt: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    override val msg: String = "Incoming data is invalid to parse"
}

/**
 * Error happens when handling auth request, like wrong signature, invalid public key.
 * */
@Serializable
@SerialName("AuthenticationFailedError")
data class AuthenticationFailedError(
    val cause: String,
    @Serializable(with = UUIDAsStringSerializer::class)
    override val errorAt: UUID
) : ErrorMessagePayload() {
    override val msg: String = "Failed to authenticate your credential"
}

/**
 * Request sent before peer is authed.
 * */
@Serializable
@SerialName("UnauthorizedError")
data class UnauthorizedError(
    @Serializable(with = UUIDAsStringSerializer::class)
    override val errorAt: UUID
) : ErrorMessagePayload() {
    override val msg: String = "Unauthorized session"
}

/**
 * Invalid message scope, like not in the same server.
 * */
@Serializable
@SerialName("InvalidScopeError")
data class InvalidScopeError(
    @Serializable(with = UUIDAsStringSerializer::class)
    override val errorAt: UUID,
    val reason: String
) : ErrorMessagePayload() {
    override val msg: String = "Invalid message scope"
}

/**
 * The server has no handler to message
 * */
@Serializable
@SerialName("UnsupportedMessageError")
data class UnsupportedMessageError(
    @Serializable(with = UUIDAsStringSerializer::class)
    override val errorAt: UUID,
    val payloadJvmName: String
) : ErrorMessagePayload() {
    override val msg: String = "The message cannot be handled because of missing handler"
}
