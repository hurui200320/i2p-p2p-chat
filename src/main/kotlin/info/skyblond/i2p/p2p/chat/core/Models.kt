package info.skyblond.i2p.p2p.chat.core

import com.dampcake.bencode.Bencode
import info.skyblond.i2p.p2p.chat.json.MapAsBencodeSerializer
import info.skyblond.i2p.p2p.chat.message.P2PMessage
import kotlinx.serialization.Serializable
import java.util.*

/**
 * This is the entry of incoming message queue.
 * It contains the [message] itself, along with a [session] for processing.
 * */
data class MessageQueueEntry<ContextType : SessionContext>(
    val session: PeerSession<ContextType>,
    val message: P2PMessage
)

/**
 * Representing a signed peer info, can be authed and shared with others.
 * */
@Serializable
data class PeerInfo(
    /**
     * Put want ever you want here.
     * It will be encoded as bencode.
     * */
    @Serializable(with = MapAsBencodeSerializer::class)
    val info: Map<String, String>,
    /**
     * The signature of the bencode info.
     * */
    val signatureBase64: String,
    /**
     * The public key of that signature
     * */
    val publicKeyBase64: String
) {
    fun getInfoBencodeBytes(): ByteArray = getDataForSign(this.info)

    fun getSignatureBytes(): ByteArray {
        return Base64.getDecoder().decode(signatureBase64)
    }

    fun getPublicKeyBytes(): ByteArray {
        return Base64.getDecoder().decode(publicKeyBase64)
    }

    companion object {
        /**
         * The key for peer's destination.
         * */
        const val INFO_KEY_DEST = "peer.dest"

        fun getDataForSign(info: Map<String, String>): ByteArray = Bencode().encode(info)
    }
}

/**
 * Where does the session/socket come from.
 * */
enum class SessionSource {
    /**
     * We connect them as a client
     * */
    CLIENT,

    /**
     * We're the server, they connect us
     * */
    SERVER
}
