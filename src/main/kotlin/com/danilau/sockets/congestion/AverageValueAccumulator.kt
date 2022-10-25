package com.danilau.sockets.congestion

class AverageValueAccumulator(
    private val averageProvider: AverageProvider = EWMAverageProvider(),
) {

    private var value = 0.0

    fun applyNextValue(nextValue: Double): Double {
        if (value == 0.0) {
            value = nextValue
        } else {
            value = averageProvider.nextAverage(value, nextValue)
        }
        return value
    }

    fun value() = value
}

sealed interface AverageProvider {

    fun nextAverage(currentAverage: Double, nextValue: Double): Double
}

/**
 * Calculates new average value via Exponentially Moving Average strategy.
 */
class EWMAverageProvider(
    private val weight: Double = 0.2,
) : AverageProvider {

    override fun nextAverage(currentAverage: Double, nextValue: Double) =
        currentAverage * (1 - weight) + nextValue * weight
}
