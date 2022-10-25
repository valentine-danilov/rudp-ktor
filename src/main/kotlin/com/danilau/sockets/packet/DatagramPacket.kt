package com.danilau.sockets.packet

import com.danilau.sockets.serialization.putSequenceNumber
import com.danilau.sockets.serialization.putUInt
import com.danilau.sockets.serialization.sequenceNumber
import com.danilau.sockets.serialization.uint
import io.ktor.network.sockets.SocketAddress
import java.nio.ByteBuffer

data class DatagramPacket(
    val metadata: Metadata,
    val data: ByteBuffer,
) : Transferable {

    fun serialize(): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(sizeBytes())
        serializeTo(buffer)
        return buffer
    }

    override fun serializeTo(buffer: ByteBuffer) {
        metadata.serializeTo(buffer)
        buffer.put(data).also { data.clear() }
        buffer.clear()
    }

    override fun sizeBytes() = metadata.sizeBytes() + data.capacity()

    override fun toString() =
        "DatagramPacket(sequence=${metadata.sequence}, ack=${metadata.ack}, ackBits=${metadata.ackBits})"

    companion object {

        fun deserializeFrom(buffer: ByteBuffer): DatagramPacket {
            val metadata = Metadata.deserializeFrom(buffer)
            val dataBuffer = ByteBuffer.allocateDirect(buffer.remaining())
            dataBuffer.put(buffer)
            buffer.clear()
            dataBuffer.clear()
            return DatagramPacket(metadata, dataBuffer)
        }
    }
}

data class Metadata(
    val sequence: SequenceNumber,
    val ack: SequenceNumber?,
    val ackBits: UInt,
) : Transferable {

    override fun serializeTo(buffer: ByteBuffer) {
        buffer.putSequenceNumber(sequence)
        buffer.putSequenceNumber(ack)
        buffer.putUInt(ackBits)
    }

    override fun sizeBytes() = SequenceNumber.SIZE_BYTES * 2 + UInt.SIZE_BYTES

    companion object {

        fun deserializeFrom(buffer: ByteBuffer): Metadata {
            val sequence = buffer.sequenceNumber()!!
            val ack = buffer.sequenceNumber()
            val ackBits = buffer.uint()
            return Metadata(sequence, ack, ackBits)
        }
    }
}

typealias PayloadAndAddress = Pair<ByteBuffer, SocketAddress>
