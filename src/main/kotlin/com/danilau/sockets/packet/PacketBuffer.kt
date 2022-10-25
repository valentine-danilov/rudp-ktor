package com.danilau.sockets.packet

import java.time.Instant

class PacketBuffer(private val size: Int = 1024) {

    private val buffer: Array<PacketData?> = Array(size) { null }

    fun insert(sequence: SequenceNumber) =
        PacketData(sequence, false, Instant.now())
            .also { buffer[index(sequence)] = it }

    fun ack(sequence: SequenceNumber): PacketData? {
        return buffer[index(sequence)]
            ?.takeIf { it.sequence == sequence && !it.acked }
            ?.also {
                it.acked = true
            }
    }

    operator fun get(sequence: SequenceNumber) =
        buffer[index(sequence)]?.takeIf { it.sequence == sequence }

    private fun index(sequence: SequenceNumber) =
        (sequence.value % size.toUShort()).toInt()
}

data class PacketData(
    val sequence: SequenceNumber,
    var acked: Boolean,
    val sentAt: Instant,
)
