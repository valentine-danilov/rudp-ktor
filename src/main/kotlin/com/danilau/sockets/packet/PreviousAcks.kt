package com.danilau.sockets.packet

fun UInt.toAckList(relativeSequence: SequenceNumber?): List<SequenceNumber> {
    relativeSequence ?: return emptyList()
    val bits = toBits()
    return bits
        .mapIndexed { index, bit -> if (bit) relativeSequence - (index + 1) else null }
        .filterNotNull()
}

fun <T : Any> Map<SequenceNumber, T>.toReceivedAcks(relativeSequence: SequenceNumber) =
    ((relativeSequence - 1) downTo (relativeSequence - 32))
        .map { sn -> containsKey(sn) }
        .toBooleanArray()

fun UInt.toBits() = (31 downTo 0).map { i -> (this and (1 shl i).toUInt()) != 0.toUInt() }.toBooleanArray()

fun BooleanArray.toUInt() = Integer.parseUnsignedInt(joinToString(separator = "") { if (it) "1" else "0" }, 2).toUInt()
