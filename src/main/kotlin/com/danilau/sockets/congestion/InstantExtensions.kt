package com.danilau.sockets.congestion

import java.time.Duration
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toKotlinDuration

fun Instant.elapsedFromNow(unit: DurationUnit = MILLISECONDS) =
    Duration.between(this, Instant.now()).toKotlinDuration().toDouble(unit)
