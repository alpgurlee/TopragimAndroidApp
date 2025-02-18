// util/Extensions.kt
package com.alperengurle.EmlakApp.util

import java.text.NumberFormat
import java.util.Locale

fun Double.formatPrice(): String {
    return NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        maximumFractionDigits = 0
    }.format(this)
}

// util/Extensions.kt
fun Long.formatPrice(): String {
    return String.format("%,d", this)
}