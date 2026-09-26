package org.mobilenativefoundation.trails.foundation.designsystem.util

object CountFormatUtils {
    fun Int.formatCount(): String {
        return when {
            this >= 1_000_000 -> {
                val value = this / 1_000_000.0
                "${(value * 10).toInt() / 10.0}M".replace(".0M", "M")
            }

            this >= 1_000 -> {
                val value = this / 1_000.0
                "${(value * 10).toInt() / 10.0}K".replace(".0K", "K")
            }

            else -> this.toString()
        }
    }

    fun Int.formatWithCommas(): String {
        val str = this.toString()
        if (str.length <= 3) return str

        val reversed = str.reversed()
        val chunked = reversed.chunked(3).joinToString(",")
        return chunked.reversed()
    }
}
