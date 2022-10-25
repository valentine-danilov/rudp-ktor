package com.danilau.sockets.packet

import java.nio.ByteBuffer

interface Transferable {
    fun serializeTo(buffer: ByteBuffer)
    fun sizeBytes(): Int
}
