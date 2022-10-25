package com.danilau.sockets

import com.danilau.sockets.congestion.AverageValueAccumulator
import com.danilau.sockets.congestion.BinaryCongestionAvoidanceStrategy
import com.danilau.sockets.congestion.CongestionAvoidanceStrategy
import com.danilau.sockets.packet.DatagramPacket
import com.danilau.sockets.packet.PayloadAndAddress
import com.danilau.sockets.packet.SequenceNumber
import com.danilau.sockets.serialization.toByteReadPacket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.readByteBuffer
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

class RUDP(
    socketAddress: SocketAddress,
    selectorManager: SelectorManager = SelectorManager(Dispatchers.IO),
    bufferSize: Int = 1024,
    avgRoundTripTimeAccumulator: AverageValueAccumulator = AverageValueAccumulator(),
    private val socketName: String? = null,
    private val congestionAvoidanceStrategy: CongestionAvoidanceStrategy = BinaryCongestionAvoidanceStrategy()
) : AbstractRUDP(bufferSize, avgRoundTripTimeAccumulator), AutoCloseable {

    override val log = KotlinLogging.logger(
        if (socketName != null) "${this::class.qualifiedName}@$socketName"
        else toString()
    )
    private val socket = aSocket(selectorManager).udp().bind(socketAddress)
    private val receiveChannel = socket.incoming
    private val sendQueue = ConcurrentLinkedQueue<PayloadAndAddress>()
    private val sendChannel = socket.outgoing

    suspend fun start(onReceive: (ByteBuffer) -> Unit = {}, onPacketAcked: (SequenceNumber) -> Unit = {}) {
        while (true) {
            sendNextScheduled()
            onReceive(receive(onPacketAcked))
            val delay = congestionAvoidanceStrategy
                .getThroughputManager()
                .updateAndGetThroughput(rtt())
                .delay
            delay(delay)
        }
    }

    override fun send(socketAddress: SocketAddress, data: ByteBuffer) {
        sendQueue.add(data to socketAddress)
    }

    override suspend fun receive(onPacketAcked: (SequenceNumber) -> Unit): ByteBuffer {
        val packet = doReceive()
        processReceive(packet, onPacketAcked)
        return packet.data
    }

    override fun close() {
        socket.close()
    }

    private suspend fun sendNextScheduled() {
        val (payload, address) = sendQueue.poll() ?: return
        val packet = prepareSend(payload)
        val datagram = Datagram(packet.serialize().toByteReadPacket(), address)
        sendChannel.send(datagram)
        processSend(packet)
        log.trace { "Sent packet [$packet]." }
    }

    private suspend fun doReceive(): DatagramPacket {
        val datagram = receiveChannel.receive()
        val dataBuffer = datagram.packet.readByteBuffer(direct = true)
        return DatagramPacket.deserializeFrom(dataBuffer)
    }
}
