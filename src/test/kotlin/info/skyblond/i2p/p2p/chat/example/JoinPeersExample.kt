package info.skyblond.i2p.p2p.chat.example

import info.skyblond.i2p.p2p.chat.I2PHelper
import info.skyblond.i2p.p2p.chat.core.Peer
import info.skyblond.i2p.p2p.chat.core.PeerInfo
import info.skyblond.i2p.p2p.chat.message.getSerializersModule
import kotlinx.serialization.json.Json
import mu.KotlinLogging

/**
 * This example shows the peer will join the whole group
 * by just connecting to one peer in that group. PEX will do the work.
 * */
object JoinPeersExample {
    private val json = Json { serializersModule = getSerializersModule() }
    private val threadPool = I2PHelper.createThreadPool(32)

    @JvmStatic
    fun main(args: Array<String>) {
        val logger = KotlinLogging.logger { }

        val peerList = mutableListOf<Peer<ExampleContext>>()

        // during the loop, each time we create a new peer, connecting to 1 existing peer
        // then let the PEX do its job, and check if it connects to all existing peer
        logger.info { "Creating peers..." }
        repeat(20) { index ->
            val peer = createPeer(json, "Peer$index", threadPool)
            if (peerList.isNotEmpty()) {
                val existingPeer = peerList.random()
                peer.connect(existingPeer.getMyDestination())
            }
            peerList.add(peer)
        }

        // for now, peers are partly interconnected, give PEX some time
        logger.info { "Waiting PEX..." }
        // keep checking
        loop@ while (true) {
            for (peer in peerList) {
                // known peers are connected and authed
                val count = peer.dumpKnownPeer().mapNotNull { it.info[PeerInfo.INFO_KEY_DEST] }.count()
                if (count != peerList.size - 1) {
                    // continue while loop
                    continue@loop
                }
            }
            break
        }

        logger.info { "Every peer knows ${peerList.size - 1} peers!" }
    }
}
