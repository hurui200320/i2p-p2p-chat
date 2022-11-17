package info.skyblond.i2p.p2p.chat.example

import info.skyblond.i2p.p2p.chat.core.*
import info.skyblond.i2p.p2p.chat.message.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.i2p.client.streaming.I2PSocketManagerFactory
import java.util.*
import java.util.concurrent.ThreadPoolExecutor
import kotlin.reflect.jvm.jvmName

/**
 * A custom [SessionContext]. Put your own data here.
 *
 * Since this is an example, there is only a username.
 * */
class ExampleContext(
    override val sessionSource: SessionSource
) : SessionContext {
    override val nickname: String?
        get() = peerInfo?.info?.get("peer.username")
    override var peerInfo: PeerInfo? = null
    var authAccepted: Boolean = false

    // we got their username and they accept ours
    override fun isAuthed(): Boolean = super.isAuthed() && nickname != null
    override fun isAccepted(): Boolean = authAccepted
}

/**
 * A handy way to create example peers.
 *
 * Those peers only do essential jobs, and print everything on screen.
 * */
fun createPeer(json: Json, name: String, threadPool: ThreadPoolExecutor): Peer<ExampleContext> {
    // each peer has its own logger
    val logger = KotlinLogging.logger(name)
    // message handler, replacing the content with your own code
    val messageHandler: IncomingMessageHandler<ExampleContext> = GeneralIncomingMessageHandler(
        // For auth, just take the username. You may run your own signature check, etc.
        object : MessagePayloadHandler<ExampleContext, AuthRequest>(AuthRequest::class) {
            override fun handleTyped(
                peer: Peer<ExampleContext>, session: PeerSession<ExampleContext>,
                messageId: UUID, payload: AuthRequest
            ): MessagePayload = payload.peerInfo.info["peer.username"]
                ?.let {
                    logger.info { "Authed ${session.getDisplayName()} as $it" }
                    AuthAcceptedReply(messageId)
                }
                ?: AuthenticationFailedError("Missing key 'peer.username'", messageId)
        },
        // print text message
        object : MessagePayloadHandler<ExampleContext, TextMessageRequest>(TextMessageRequest::class) {
            override fun handleTyped(
                peer: Peer<ExampleContext>, session: PeerSession<ExampleContext>,
                messageId: UUID, payload: TextMessageRequest
            ): MessagePayload = NoContentReply(messageId).also {
                logger.info {
                    "Get ${payload.scope} message from peer ${session.getDisplayName()}: ${payload.content}"
                }
            }
        },
        // print disconnect message
        object : MessagePayloadHandler<ExampleContext, ByeRequest>(ByeRequest::class) {
            override fun handleTyped(
                peer: Peer<ExampleContext>, session: PeerSession<ExampleContext>, messageId: UUID, payload: ByeRequest
            ): MessagePayload? = null.also {
                logger.info { "Peer ${session.getDisplayName()} disconnected: ${payload.reason}" }
            }
        },
        // handler for AuthAcceptedReply
        object : MessagePayloadHandler<ExampleContext, AuthAcceptedReply>(AuthAcceptedReply::class) {
            override fun handleTyped(
                peer: Peer<ExampleContext>, session: PeerSession<ExampleContext>,
                messageId: UUID, payload: AuthAcceptedReply
            ): MessagePayload? = null.also {
                session.useContextSync { this.authAccepted = true }
            }
        },
        // handle PEX request
        createPEXRequestHandler(logger, { true }),
        // handle PEX reply, here we do nothing about verification
        createPEXReplyHandler(logger, { true }),
        // do nothing on no content reply (204 OK)
        createNoContentReplyHandler(),
        // for unsupported message error
        object : MessagePayloadHandler<ExampleContext, UnsupportedMessageError>(UnsupportedMessageError::class) {
            override fun handleTyped(
                peer: Peer<ExampleContext>, session: PeerSession<ExampleContext>,
                messageId: UUID, payload: UnsupportedMessageError
            ): MessagePayload? = null.also {
                logger.warn { "Peer ${session.getDisplayName()} doesn't support payload: ${payload.payloadJvmName}" }
            }
        },
        // You don't need rest of the handlers, they just make sure everything
        // is handled by printing incoming messages on screen.
        object : MessagePayloadHandler<ExampleContext, MessagePayload>(MessagePayload::class) {
            override fun handleTyped(
                peer: Peer<ExampleContext>, session: PeerSession<ExampleContext>,
                messageId: UUID, payload: MessagePayload
            ): MessagePayload = UnsupportedMessageError(messageId, payload::class.jvmName).also {
                logger.warn { "Got unhandled message from ${session.getDisplayName()}: $payload" }
            }
        }
    )

    val manager = I2PSocketManagerFactory.createManager()

    return Peer(
        json, manager, "test", { ExampleContext(it) },
        {
            PeerInfo(
                mapOf(
                    PeerInfo.INFO_KEY_DEST to it.getMyDestination().toBase64(),
                    "peer.username" to name
                ), "", ""
            )
        },
        messageHandler, threadPool,
        pexInterval = 10 * 1000, logger = logger
    )
}
