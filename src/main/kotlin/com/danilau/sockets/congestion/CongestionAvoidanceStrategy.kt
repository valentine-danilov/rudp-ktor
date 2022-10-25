package com.danilau.sockets.congestion

import com.danilau.sockets.congestion.throughput.BinaryThroughputManager
import com.vdanilau.udp.rudp.throughput.ThroughputManager

sealed class CongestionAvoidanceStrategy(
    val maxRoundTripTimeMillis: Int,
    val initialTransitionPeriodSeconds: Int,
    val minTransitionPeriodSeconds: Int,
    val maxTransitionPeriodSeconds: Int,
) {

    abstract fun getThroughputManager(): ThroughputManager
}

class BinaryCongestionAvoidanceStrategy(
    maxRoundTripTimeMillis: Int = 1000,
    initialTransitionPeriodSeconds: Int = 4,
    minTransitionPeriodSeconds: Int = 1,
    maxTransitionPeriodSeconds: Int = 60,
    val normalRate: Int = 30,
    val lowerRate: Int = 15,
    val thresholdRoundTripTime: Double = 100.0
) : CongestionAvoidanceStrategy(
    maxRoundTripTimeMillis,
    initialTransitionPeriodSeconds,
    minTransitionPeriodSeconds,
    maxTransitionPeriodSeconds,
) {
    private val throughputManager = BinaryThroughputManager(this)

    override fun getThroughputManager() = throughputManager
}
