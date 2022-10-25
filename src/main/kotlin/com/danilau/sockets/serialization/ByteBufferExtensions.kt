package com.danilau.sockets.serialization

import com.danilau.sockets.packet.SequenceNumber
import io.ktor.utils.io.core.ByteReadPacket
import java.nio.ByteBuffer

fun ByteBuffer.toByteReadPacket() = ByteReadPacket(this)


fun ByteBuffer.putUInt(value: UInt): ByteBuffer {
    putInt(value.toInt())
    return this
}
fun ByteBuffer.uint() = int.toUInt()

fun ByteBuffer.putSequenceNumber(value: SequenceNumber?): ByteBuffer {
    putShort(value?.value?.toShort() ?: -1)
    return this
}
fun ByteBuffer.sequenceNumber(): SequenceNumber? {
    val value = short
    if ((-1).toShort() == value) return null

    return SequenceNumber(value.toUShort())
}
