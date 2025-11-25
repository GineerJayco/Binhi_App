package com.example.binhi.utils

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

fun convertToDMS(decimal: Double, isLatitude: Boolean): String {
    val direction = when {
        isLatitude && decimal >= 0 -> "N"
        isLatitude && decimal < 0 -> "S"
        !isLatitude && decimal >= 0 -> "E"
        else -> "W"
    }

    val absolute = abs(decimal)
    val degrees = floor(absolute).toInt()
    val minutes = floor((absolute - degrees) * 60).toInt()
    val seconds = ((absolute - degrees) * 60 - minutes) * 60

    return String.format(Locale.US, "%d° %d' %.4f\" %s", degrees, minutes, seconds, direction)
}
