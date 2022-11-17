package info.skyblond.i2p.p2p.chat.example

import info.skyblond.i2p.p2p.chat.I2PHelper
import info.skyblond.i2p.p2p.chat.message.TextMessageRequest
import info.skyblond.i2p.p2p.chat.message.getSerializersModule
import kotlinx.serialization.json.Json
import mu.KotlinLogging

/**
 * In this example, there will be 2 peers connecting to each other,
 * sending messages, disconnects.
 * */
object TwoPeerExample {
    private val json = Json { serializersModule = getSerializersModule() }
    private val threadPool = I2PHelper.createThreadPool(16)

    @JvmStatic
    fun main(args: Array<String>) {
        val logger = KotlinLogging.logger { }
        logger.info { "Creating peers..." }

        val peer1 = createPeer(json, "Peer1", threadPool)
        val peer2 = createPeer(json, "Peer2", threadPool)

        logger.info { "Peer1 connect to peer2..." }
        // you may see
        peer1.connect(peer2.getMyDestination())

        logger.info { "Waiting for auth..." }
        Thread.sleep(5 * 1000)
        logger.info { "Known peers\nPeer1: ${peer1.dumpKnownPeer()}\nPeer2: ${peer2.dumpKnownPeer()}" }

        logger.info { "Peer2 -> Peer1" }
        peer2.sendRequest(TextMessageRequest("public", "test")) { true }
        Thread.sleep(5 * 1000)

        logger.info { "Waiting for PEX" }
        Thread.sleep(30 * 1000)

        logger.info { "Peer1 Bye" }
        peer1.close()
        Thread.sleep(5 * 1000)
        logger.info { "Peer2 Bye" }
        peer2.close()
        Thread.sleep(30 * 1000)
    }
}
