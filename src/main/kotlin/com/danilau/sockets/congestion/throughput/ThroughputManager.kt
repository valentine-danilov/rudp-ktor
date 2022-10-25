package com.vdanilau.udp.rudp.throughput

interface ThroughputManager {
    fun updateAndGetThroughput(currentRtt: Double): Throughput
    fun getThroughput(): Throughput
    fun getCurrentMode(): ThroughputMode
}

data class Throughput(
    val framesPerSeconds: Int
) {
    val delay = (1000 / framesPerSeconds).toLong()
}

enum class ThroughputMode { HIGH, LOW; }
