package com.example.fasttrackjapan

import java.time.LocalDate

object GarbageScheduleCalculator {
    private fun weekdayOrdinalInMonth(date: LocalDate): Int = ((date.dayOfMonth - 1) / 7) + 1

    fun matches(date: LocalDate, schedule: GarbageSchedule): Boolean {
        if (date.dayOfWeek.value != schedule.weekday) return false
        val weeks = schedule.weeksOfMonth ?: return true
        return weekdayOrdinalInMonth(date) in weeks
    }
    fun collectionsOn(date: LocalDate, schedules: List<GarbageSchedule>): List<String> =
        schedules.filter { matches(date, it) }
            .map { it.categoryCode }
            .distinct()

    fun nextCollectionFor(categoryCode: String, from: LocalDate, schedules: List<GarbageSchedule>): LocalDate? {
        val relevant = schedules.filter { it.categoryCode == categoryCode }
        if (relevant.isEmpty()) return null
        var d = from
        val limit = from.plusDays(366)
        while (d.isBefore(limit)) {
            if (relevant.any { matches(d, it) }) return d
            d = d.plusDays(1)
        }
        return null
    }
    fun upcoming(from: LocalDate, days: Int, schedules: List<GarbageSchedule>): List<UpcomingCollection> {
        val result = mutableListOf<UpcomingCollection>()
        for (offset in 0 until days) {
            val d = from.plusDays(offset.toLong())
            val codes = collectionsOn(d, schedules)
            if (codes.isNotEmpty()) result.add(UpcomingCollection(d, codes))
        }
        return result
    }
}
