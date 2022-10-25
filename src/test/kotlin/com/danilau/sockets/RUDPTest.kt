package com.danilau.sockets

import com.danilau.sockets.congestion.BinaryCongestionAvoidanceStrategy
import io.ktor.network.sockets.InetSocketAddress
import java.nio.ByteBuffer
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class RUDPTest {

    init {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")
    }

    @Test
    fun test() {
        val address1 = InetSocketAddress("127.0.0.1", 4444)
        val ca1 = BinaryCongestionAvoidanceStrategy(normalRate = 200, lowerRate = 30)
        val socket1 = RUDP(socketAddress = address1, socketName = "server", congestionAvoidanceStrategy = ca1)
        val address2 = InetSocketAddress("127.0.0.1", 4445)
        val ca2 = BinaryCongestionAvoidanceStrategy(normalRate = 200, lowerRate = 30)
        val socket2 = RUDP(socketAddress = address2, socketName = "client", congestionAvoidanceStrategy = ca2)

        runBlocking {

            repeat(250) {
                socket1.send(address2, ByteBuffer.wrap("Hello there!".toByteArray()))
                socket2.send(address1, ByteBuffer.wrap("GENERAL KENOBI".toByteArray()))
            }

            async { socket1.start() }
            async { socket2.start() }

            println()
        }
    }
}
