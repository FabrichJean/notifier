package com.notifier.client.ui

import com.notifier.client.model.NotificationData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DailyCount(val label: String, val count: Int)

data class NotificationStats(
    val totalCount: Int,
    val alarmCount: Int,
    val notificationCount: Int,
    val last7Days: List<DailyCount>,
)

fun computeNotificationStats(list: List<NotificationData>): NotificationStats {
    val alarmCount = list.count { it.type == "alarm" }
    val notificationCount = list.size - alarmCount
    val countsByDate = list.groupingBy { it.createdAt.take(10) }.eachCount()

    val today = LocalDate.now()
    val last7Days = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .replaceFirstChar { it.uppercase() }
        DailyCount(label = label, count = countsByDate[key] ?: 0)
    }

    return NotificationStats(
        totalCount = list.size,
        alarmCount = alarmCount,
        notificationCount = notificationCount,
        last7Days = last7Days,
    )
}
