package com.danilau.sockets

import com.danilau.sockets.congestion.AverageValueAccumulator
import com.danilau.sockets.congestion.elapsedFromNow
import com.danilau.sockets.packet.DatagramPacket
import com.danilau.sockets.packet.Metadata
import com.danilau.sockets.packet.PacketBuffer
import com.danilau.sockets.packet.SequenceNumber
import com.danilau.sockets.packet.toAckList
import com.danilau.sockets.packet.toReceivedAcks
import com.danilau.sockets.packet.toUInt
import io.ktor.network.sockets.SocketAddress
import java.nio.ByteBuffer
import java.time.Instant
import mu.KLogger

abstract class AbstractRUDP(
    bufferSize: Int,
    protected val avgRoundTripTimeAccumulator: AverageValueAccumulator
) {

    protected abstract val log: KLogger
    protected var latestSentSequence: SequenceNumber? = null
    protected var latestReceivedSequence: SequenceNumber? = null
    protected val sentBuffer: PacketBuffer = PacketBuffer(bufferSize)
    protected val receiveBuffer: MutableMap<SequenceNumber, DatagramPacket> = mutableMapOf()

    abstract fun send(socketAddress: SocketAddress, data: ByteBuffer)
    abstract suspend fun receive(onPacketAcked: (SequenceNumber) -> Unit = {}): ByteBuffer?

    protected fun prepareSend(data: ByteBuffer): DatagramPacket {
        val sequenceNumber = latestSentSequence?.plus(1U) ?: SequenceNumber(0U)
        val ack = latestReceivedSequence
        val ackBits = ack?.let { receiveBuffer.toReceivedAcks(it).toUInt() } ?: 0U
        val metadata = Metadata(sequenceNumber, ack, ackBits)
        return DatagramPacket(metadata, data)
    }

    protected fun processSend(packet: DatagramPacket) {
        val latestSentSequence = latestSentSequence
        sentBuffer.insert(packet.metadata.sequence)
        this.latestSentSequence = packet.metadata.sequence
        log.trace { "latestSentSequence updated: [$latestSentSequence -> ${this.latestSentSequence}]" }
    }

    protected fun processReceive(packet: DatagramPacket, onPacketAcked: (SequenceNumber) -> Unit = {}) {
        log.trace { "Received packet [${packet}]." }
        val latestReceivedSequence = latestReceivedSequence
        val receivedSequence = packet.metadata.sequence
        if (latestReceivedSequence == null || latestReceivedSequence < receivedSequence) {
            this.latestReceivedSequence = receivedSequence
            log.trace { "latestReceivedSequence updated: [$latestReceivedSequence -> ${this.latestSentSequence}]" }
        }
        receiveBuffer[receivedSequence] = packet
        packet.metadata.ack?.let { doAck(it, onPacketAcked) }
        packet.metadata.ackBits.toAckList(packet.metadata.ack)
            .forEach { sn -> doAck(sn, onPacketAcked)}
    }

    fun rtt() = avgRoundTripTimeAccumulator.value()

    private fun doAck(sequenceNumber: SequenceNumber, onPacketAcked: (SequenceNumber) -> Unit) {
        sentBuffer.ack(sequenceNumber)?.also {
            log.trace { "Acked packet [sn=$sequenceNumber] at ${Instant.now()}." }
            val rtt = it.sentAt.elapsedFromNow()
            val avgRtt = avgRoundTripTimeAccumulator.applyNextValue(rtt)
            log.trace { "average rtt updated: [lastPacketSeq=${it.sequence}, lastPacketRtt=$rtt, " +
                "newAvgRtt=$avgRtt]" }
            onPacketAcked(sequenceNumber)
        }
    }
}
