package info.skyblond.i2p.p2p.chat.example

import info.skyblond.i2p.p2p.chat.I2PHelper
import net.i2p.client.streaming.I2PSocketManagerFactory
import java.util.*

/**
 * This example shows you how to keep using the same destination after restart
 * the application.
 * */
object PersistentI2PSessionExample {
    /**
     * Base64 encoded key. Save it somewhere, so you can get it on next run.
     * */
    private const val keyBase64 =
        "MUc5p9svJemwH0pMNzej+KsywM7khMrll5IgL5THvkw1V0R9h6K0xf/8mc4fazeTXyrKv2MGhtaMZEeTORrK3+icXP9FSWwzqVRONgobbAjFwfiFvYCX7nAl2HUumds4RUMR0CpYHTQVpOh0eQ/dWj9X+IhORxf35JiTdwY83r+nncAbRy7X0xY4zNwMWB4vZrQdgzEakEO96BnT0hQElkItxjJY5OpmTLJki0pGFCyaa7hQhilIO279tpMCXCKtx3ztlAUvUqsqOD94LZRWEgwScAr9O/mZY+EinAc9mPYl8H1b5ZAGJoeuKjYE/x23yOIfRrORGM0yw69kWp6tdTl1vEuoEzCZ/QdwdKbuQq8mFntjVnBn8LMU289arsHbbSPjr81Ke8S+332yszRQlN0qSaCAM5AGZJegZjkcpkBC4IgPVNONCidaLmAd4I+WvvWD2G7MkeoG6XmCFsN9H+mjqe+Y8DAuTLVnoi1y38i5+gqSBqWvKg+v3OP6+OUVBQAEAAcAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAZz9ntzGVcMpL9QPNz9WJ2Wg8IBNoc+ovXv6Zc+Aso8tjmOft0UUmV31KGRg1nkdRizqEwN0IGmZVDYsww=="

    /**
     * The destination is calculated from that key, thus the session address is fixed.
     * */
    private val fixedManager = I2PHelper.createSocketManager(Base64.getDecoder().decode(keyBase64))

    /**
     * By default, you get random key generated each time you create a new manager.
     * */
    private val randomManager = I2PSocketManagerFactory.createManager()

    @JvmStatic
    fun main(args: Array<String>) {
        println("Fixed address:  " + fixedManager.session.myDestination.toBase32())
        println("Random address: " + randomManager.session.myDestination.toBase32())
        println("Random key: " + Base64.getEncoder().encodeToString(I2PHelper.generateDestinationKey()))
    }
}
