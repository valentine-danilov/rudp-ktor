package com.danilau.sockets.packet

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class PreviousAcksTest {

    @Test
    fun `test case 1`() {
        val relativeSequence = SequenceNumber(33U)
        val bits = BooleanArray(32) { false }
        repeat(5) {
            bits[it] = true
        }

        val acks = bits.toUInt().toAckList(relativeSequence)
        acks.take(5).forEachIndexed { index, sn -> assertEquals(relativeSequence - (index + 1), sn) }
    }

    @Test
    fun `previous 32 packets should be acked`() {
        val relativeSequence = SequenceNumber(33U)
        val receivedPackets = mutableMapOf<SequenceNumber, String>()
        repeat(33) {
            receivedPackets[SequenceNumber(it.toUShort())] = "$it"
        }

        val receivedAcks = receivedPackets.toReceivedAcks(relativeSequence)
        receivedAcks.forEach { assertEquals(true, it) }
    }

    @Test
    fun `previous 1 packet should be acked`() {
        val relativeSequence = SequenceNumber(33U)
        val receivedPackets = mapOf(SequenceNumber(32U) to "")

        val receivedAcks = receivedPackets.toReceivedAcks(relativeSequence)
        assertEquals(true, receivedAcks[0])
        receivedAcks.sliceArray(1 until receivedAcks.size).forEach { assertEquals(false, it) }
    }

    @Test
    fun `previous 5 packets should be acked`() {
        val relativeSequence = SequenceNumber(33U)
        val receivedPackets = mutableMapOf<SequenceNumber, String>()
        repeat(5) {
            receivedPackets[relativeSequence - it - 1] = ""
        }

        val receivedAcks = receivedPackets.toReceivedAcks(relativeSequence)
        receivedAcks.sliceArray(0 until 5).forEach { assertEquals(true, it) }
        receivedAcks.sliceArray(5 until receivedAcks.size).forEach { assertEquals(false, it) }
    }

    @Test
    fun `previous 28 packets not acked, packets from (33 - 28, 33 - 32)  should be acked`() {
        val relativeSequence = SequenceNumber(33U)
        val receivedPackets = mutableMapOf<SequenceNumber, String>()
        repeat(5) {
            receivedPackets[relativeSequence - it - 28] = ""
        }

        val receivedAcks = receivedPackets.toReceivedAcks(relativeSequence)
        receivedAcks.sliceArray(0 until 27).forEach { assertEquals(false, it) }
        receivedAcks.sliceArray(27 until receivedAcks.size).forEach { assertEquals(true, it) }
    }
}
