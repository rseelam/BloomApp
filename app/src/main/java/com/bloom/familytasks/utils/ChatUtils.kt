// app/src/main/java/com/bloom/familytasks/utils/ChatUtils.kt
package com.bloom.familytasks.utils

import java.text.SimpleDateFormat
import java.util.*

object ChatUtils {
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val messageDate = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return today.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR)
    }
}