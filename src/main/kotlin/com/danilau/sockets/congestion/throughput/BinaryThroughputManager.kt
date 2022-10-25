package com.danilau.sockets.congestion.throughput

import com.danilau.sockets.congestion.BinaryCongestionAvoidanceStrategy
import com.danilau.sockets.congestion.elapsedFromNow
import com.vdanilau.udp.rudp.throughput.Throughput
import com.vdanilau.udp.rudp.throughput.ThroughputManager
import com.vdanilau.udp.rudp.throughput.ThroughputMode.HIGH
import com.vdanilau.udp.rudp.throughput.ThroughputMode.LOW
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.time.DurationUnit.SECONDS
import mu.KotlinLogging

class BinaryThroughputManager(
    private val config: BinaryCongestionAvoidanceStrategy
) : ThroughputManager {

    private val log = KotlinLogging.logger(toString())
    private var activeMode = HIGH
    private var currentMode = HIGH
    private var currentTransitionPeriodSeconds = config.initialTransitionPeriodSeconds
    private var inCurrentModeSince = Instant.now()
    private var lastTransitionAt = Instant.now()

    override fun updateAndGetThroughput(currentRtt: Double): Throughput {
        adjust(currentRtt)
        return doGetThroughput()
    }

    override fun getThroughput() = doGetThroughput()

    override fun getCurrentMode() = currentMode

    private fun doGetThroughput() = when(activeMode) {
        HIGH -> Throughput(config.normalRate)
        LOW -> Throughput(config.lowerRate)
    }

    private fun adjust(currentRtt: Double) {
        updateCurrentMode(currentRtt)
        when (activeMode) {
            HIGH -> {
                when (currentMode) {
                    LOW -> {
                        log.trace { "Throughput mode transition: [$activeMode -> $currentMode]" }
                        activeMode = currentMode
                        if (lastTransitionAt.elapsedFromNow(SECONDS) < 10) {
                            increaseTransitionPeriod()
                        }
                        lastTransitionAt = Instant.now()
                    }
                    HIGH -> {
                        if (inCurrentModeSince.elapsedFromNow(SECONDS) >= 10) {
                            decreaseTransitionPeriod()
                            inCurrentModeSince = Instant.now()
                        }
                    }
                }
            }
            LOW -> {
                if (currentMode == HIGH && isTransitionAllowed()) {
                    log.trace { "Throughput mode transition: [$activeMode -> $currentMode]" }
                    activeMode = currentMode
                    lastTransitionAt = Instant.now()
                }
            }
        }
    }

    private fun updateCurrentMode(currentRtt: Double) {
        val nextMode = if (currentRtt > config.thresholdRoundTripTime) LOW else HIGH
        if (nextMode != currentMode) {
            inCurrentModeSince = Instant.now()
        }
        currentMode = nextMode
    }

    private fun isTransitionAllowed() = inCurrentModeSince.elapsedFromNow(SECONDS) >= currentTransitionPeriodSeconds

    private fun increaseTransitionPeriod() {
        if (currentTransitionPeriodSeconds < config.maxTransitionPeriodSeconds) {
            currentTransitionPeriodSeconds = min(currentTransitionPeriodSeconds * 2, config.maxTransitionPeriodSeconds)
        }
    }

    private fun decreaseTransitionPeriod() {
        if (currentTransitionPeriodSeconds > config.minTransitionPeriodSeconds) {
            currentTransitionPeriodSeconds = max(currentTransitionPeriodSeconds / 2, config.minTransitionPeriodSeconds)
        }
    }
}
