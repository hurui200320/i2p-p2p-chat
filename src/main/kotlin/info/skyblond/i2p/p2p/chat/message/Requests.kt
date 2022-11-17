package info.skyblond.i2p.p2p.chat.message

import info.skyblond.i2p.p2p.chat.core.PeerInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Proof the identity to peer.
 * */
@Serializable
@SerialName("AuthRequest")
data class AuthRequest(
    val applicationName: String,
    val peerInfo: PeerInfo
) : RequestMessagePayload()

/**
 * Exchange peer.
 * Sending our known peers (including ourselves) and requesting their know peers.
 *
 * Note: When handing exchanged peers, we still need auth with them. Auth in reply
 * only help us to remove invalid dest, like sending us a lot of garbage dest.
 * */
@Serializable
@SerialName("PEXRequest")
data class PEXRequest(
    val peerInfos: List<PeerInfo>
) : RequestMessagePayload()

/**
 * Sending a text message to peer.
 * */
@Serializable
@SerialName("TextMessageRequest")
data class TextMessageRequest(
    /**
     * The scope of the message, aka, who should get this message
     * */
    val scope: String,
    /**
     * The message itself
     * */
    val content: String
) : RequestMessagePayload() {

    companion object {
        /**
         * A public message, aka, normal message.
         * */
        const val SCOPE_PUBLIC = "public"

        /**
         * A private message, like /msg someone message.
         * */
        const val SCOPE_PRIVATE = "private"

        // you may extend the scope, like `team:name`, `channel:something`, etc.
    }
}

/**
 * Bye, aka, disconnect.
 * Optional [reason] to describe what's happened, like disconnect, exit, etc.
 * */
@Serializable
@SerialName("ByeRequest")
data class ByeRequest(
    val reason: String = ""
) : RequestMessagePayload()
