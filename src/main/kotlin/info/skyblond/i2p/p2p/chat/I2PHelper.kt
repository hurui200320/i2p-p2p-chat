package info.skyblond.i2p.p2p.chat

import mu.KotlinLogging
import net.i2p.I2PAppContext
import net.i2p.client.I2PClientFactory
import net.i2p.client.streaming.I2PSocketManager
import net.i2p.client.streaming.I2PSocketManagerFactory
import net.i2p.crypto.SigType
import net.i2p.util.I2PAppThread
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Some handy methods for working with I2P.
 * */
object I2PHelper {
    private val logger = KotlinLogging.logger { }

    init {
        // warmup i2p's logger
        warmupLogger()
    }

    /**
     * Warm up i2p's internal logger by writing an error message.
     * Thus, the logger will create a writer. If something goes wrong
     * and program crashed, the logger can flush remaining logs into file.
     * */
    private fun warmupLogger() {
        if (I2PAppContext.getGlobalContext().logManager().currentFile() != "No log file created yet") {
            logger.warn { "I2P logger has been already warmed up" }
            return
        }
        I2PAppContext.getGlobalContext().logManager().let { logManager ->
            logManager.getLog("Application").let {
                it.log(it.minimumPriority, "Warm up logger...")
            }
            while (logManager.currentFile() == "No log file created yet") {
                Thread.sleep(1000)
            }
        }
    }

    /**
     * Generate a destination key. It can be used when creating socket manager.
     * @see [createSocketManager]
     * */
    @JvmStatic
    fun generateDestinationKey(): ByteArray = ByteArrayOutputStream().use {
        I2PClientFactory.createClient().createDestination(
            it, SigType.EdDSA_SHA512_Ed25519
        )
        it.toByteArray()
    }

    /**
     * Create a socket manager.
     * The [destinationKey] will decide the server destination.
     * The [appName] is the tunnel name shown on i2p router dashboard.
     * */
    @JvmStatic
    fun createSocketManager(destinationKey: ByteArray, appName: String? = null): I2PSocketManager {
        val props = Properties()
        if (appName != null) {
            props.setProperty("inbound.nickname", appName)
            props.setProperty("outbound.nickname", "$appName (out)")
        }

        return destinationKey.inputStream()
            .use { I2PSocketManagerFactory.createManager(it, props) }
            ?: error(
                "Failed to create I2P socket manager. " +
                        "See i2p log '${I2PAppContext.getGlobalContext().logManager().currentFile()}' for details."
            )
    }

    /**
     * Run something in [I2PAppThread].
     *
     * Use this when using I2P related resources
     * */
    @JvmStatic
    fun runThread(name: String? = null, runnable: Runnable) = runThread(name, runnable::run)

    /**
     * Run something in [I2PAppThread].
     *
     * Use this when using I2P related resources
     * */
    @JvmStatic
    fun runThread(name: String? = null, task: () -> Unit) {
        val t = I2PAppThread {
            try {
                task()
            } catch (t: Throwable) {
                logger.error(t) { "Uncaught exception cause thread terminated" }
            }
        }
        t.isDaemon = true
        name?.let { t.name = it }
        t.start()
    }

    /**
     * Build a new [ThreadPoolExecutor] with given [nThreads] parameter.
     * The threads are [I2PAppThread] and are daemon threads.
     * */
    @JvmStatic
    fun createThreadPool(nThreads: Int): ThreadPoolExecutor =
        ThreadPoolExecutor(
            nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            { I2PAppThread(it).apply { isDaemon = true } },
            ThreadPoolExecutor.CallerRunsPolicy()
        )
}
