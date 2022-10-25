package com.danilau.sockets.packet

private val UNSIGNED_SHORT_HALF_MAX_VALUE = (UShort.MAX_VALUE / 2.toUShort() + 1.toUShort()).toUShort()

/**
 * Representation of packet's sequence number.
 *
 * All arithmetic operation are implemented considering wrap around - overflowing of value
 * causes value to wrap around - to start again from 0 (for *plus* operation) or from 65535 (for *minus* operation).
 *
 * E.g. 65535 + 1 = 0, 65530 + 7 = 1, 0 - 1 = 65535, 5 - 7 = 65534.
 *
 * @see SequenceNumber.isGreaterThen
 */
data class SequenceNumber(
    val value: UShort
) : Comparable<SequenceNumber> {

    companion object {

        val MIN_VALUE = SequenceNumber(UShort.MIN_VALUE)
        val MAX_VALUE = SequenceNumber(UShort.MAX_VALUE)
        const val SIZE_BYTES = UShort.SIZE_BYTES

        fun valueOf(intValue: Int) = SequenceNumber(intValue.toUShort())
    }

    infix fun downTo(to: SequenceNumber) = SequenceNumberProgression(this, to, -1)

    infix fun until(toNotInclusive: SequenceNumber) = SequenceNumberProgression(this, toNotInclusive - 1, 1)

    operator fun rangeTo(to: SequenceNumber) = SequenceNumberProgression(this, to, 1)

    override operator fun compareTo(other: SequenceNumber): Int {
        if (value.compareTo(other.value) == 0) return 0
        return if (this.isGreaterThen(other)) 1 else -1
    }

    operator fun plus(other: UShort): SequenceNumber {
        val rangeToMax = (UShort.MAX_VALUE - value).toUShort()
        if (rangeToMax > other) {
            return SequenceNumber((value + other).toUShort())
        }
        return SequenceNumber((other - rangeToMax - 1.toUShort()).toUShort())
    }

    operator fun plus(other: SequenceNumber) = this + other.value

    operator fun plus(other: Int) = this + other.toUShort()

    operator fun minus(other: UShort): SequenceNumber {
        if (other > value) {
            return SequenceNumber((UShort.MAX_VALUE - other + value + 1.toUShort()).toUShort())
        }
        return SequenceNumber((value - other).toUShort())
    }

    operator fun minus(other: SequenceNumber) = this - other.value

    operator fun minus(other: Int) = this - other.toUShort()

    operator fun inc(): SequenceNumber {
        return this + 1.toUShort()
    }

    operator fun dec(): SequenceNumber {
        return this - 1.toUShort()
    }

    /**
     * Compares two sequence numbers **taking into consideration the possible *wrap around***.
     *
     * ***The wrap around*** happens when the sequence number reaches its maximum value (65535 in case of 16-bit integer)
     * and the next sequence number will be 0 once again, e.g. [..., 65534, 65535, 0, 1, ...].
     *
     * Handling wrap around is required for sequence numbers as if a packet with sequence number 0 is received after a packet with sequence number 255, 0 is a more recent packet.
     *
     * The wrap around is handled in the following way:
     * 1. If a > b AND difference a-b <= 32768 (half of 16-bit integer max value), then values are considered close to each other and a > b, as usual.
     * 2. If a < b AND difference b-a > 32768, it is considered that wrap around happened, and it is assumed that a > b.
     *
     * @param other sequence number to compare with
     * @return **true** if **this > other** considering wrap around, **false** otherwise.
     */
    private fun isGreaterThen(other: SequenceNumber) =
        (value > other.value && (value - other.value <= UNSIGNED_SHORT_HALF_MAX_VALUE)) ||
            (value < other.value) && (other.value - value > UNSIGNED_SHORT_HALF_MAX_VALUE)

    override fun toString(): String {
        return value.toString()
    }
}

class SequenceNumberProgression(
    val start: SequenceNumber,
    val endInclusive: SequenceNumber,
    val step: Int
) : Iterable<SequenceNumber> {

    override fun iterator() = SequenceNumberProgressionIterator(start, endInclusive, step)
}

class SequenceNumberProgressionIterator(
    first: SequenceNumber,
    last: SequenceNumber,
    step: Int,
) : Iterator<SequenceNumber> {

    private val finalElement = last
    private var hasNext = if (step > 0) first <= last else first >= last
    private val step = step.toUShort()
    private var next = if (hasNext) first else finalElement

    override fun hasNext() = hasNext

    override fun next(): SequenceNumber {
        val value = next
        if (next == finalElement) {
            if (!hasNext) throw NoSuchElementException()
            hasNext = false
        } else {
            next += step
        }
        return value
    }
}
