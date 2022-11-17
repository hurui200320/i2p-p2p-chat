package info.skyblond.i2p.p2p.chat.example

import info.skyblond.i2p.p2p.chat.I2PHelper
import info.skyblond.i2p.p2p.chat.message.RequestMessagePayload
import info.skyblond.i2p.p2p.chat.message.getSerializersModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.subclass

/**
 * This example shows how you extend your own message (request/reply/error)
 * */
object CustomMessageExample {

    /**
     * Here is the new message
     * */
    @Serializable
    data class SomeTypeOfRequest(
        val data: String
    ) : RequestMessagePayload()

    @JvmStatic
    fun main(args: Array<String>) {
        val json = Json {
            serializersModule = getSerializersModule(
                customMessagePayload = {
                    // register here
                    subclass(SomeTypeOfRequest::class)
                }
            )
        }
        val threadPool = I2PHelper.createThreadPool(16)

        val peer1 = createPeer(json, "peer1", threadPool)
        val peer2 = createPeer(json, "peer2", threadPool)
        peer1.connect(peer2.getMyDestination())
        // here you will see our new request
        peer2.sendRequest(SomeTypeOfRequest("This is our new request!")) { true }

        Thread.sleep(10 * 1000)
    }
}
