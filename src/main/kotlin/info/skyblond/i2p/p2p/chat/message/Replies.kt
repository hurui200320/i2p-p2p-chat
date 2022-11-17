package info.skyblond.i2p.p2p.chat.message

import info.skyblond.i2p.p2p.chat.core.PeerInfo
import info.skyblond.i2p.p2p.chat.json.UUIDAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

/**
 * OK with no further content, like HTTP 204.
 * */
@Serializable
@SerialName("NoContentReply")
data class NoContentReply(
    @Serializable(with = UUIDAsStringSerializer::class)
    override val replyTo: UUID
) : ReplyMessagePayload()

/**
 * We accepted peer's auth request.
 * Now they can send message to us.
 * */
@Serializable
@SerialName("AuthAcceptedReply")
data class AuthAcceptedReply(
    @Serializable(with = UUIDAsStringSerializer::class)
    override val replyTo: UUID
) : ReplyMessagePayload()

/**
 * OK with no further content, like HTTP 204.
 * */
@Serializable
@SerialName("PEXReply")
data class PEXReply(
    @Serializable(with = UUIDAsStringSerializer::class)
    override val replyTo: UUID,
    val peerInfos: List<PeerInfo>
) : ReplyMessagePayload()
