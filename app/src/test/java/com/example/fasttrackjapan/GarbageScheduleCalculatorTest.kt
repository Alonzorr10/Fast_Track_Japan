package com.example.fasttrackjapan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GarbageScheduleCalculatorTest {

    // In July 2026: 2026-07-06 is a Monday, 2026-07-01 is a Wednesday.
    private val burnableWeekly = listOf(
        GarbageSchedule(areaId = "a", categoryCode = "BURNABLE", weekday = 1), // Mon
        GarbageSchedule(areaId = "a", categoryCode = "BURNABLE", weekday = 4)  // Thu
    )
    private val nonBurnable2nd4thWed = listOf(
        GarbageSchedule(areaId = "a", categoryCode = "NON_BURNABLE", weekday = 3, weeksOfMonth = listOf(2, 4))
    )

    @Test
    fun weekly_matches_every_matching_weekday() {
        val mon = LocalDate.of(2026, 7, 6)
        val tue = LocalDate.of(2026, 7, 7)
        assertEquals(listOf("BURNABLE"), GarbageScheduleCalculator.collectionsOn(mon, burnableWeekly))
        assertTrue(GarbageScheduleCalculator.collectionsOn(tue, burnableWeekly).isEmpty())
    }

    @Test
    fun nth_weekday_matches_only_listed_occurrences() {
        val firstWed = LocalDate.of(2026, 7, 1)   // 1st Wed -> no
        val secondWed = LocalDate.of(2026, 7, 8)  // 2nd Wed -> yes
        val thirdWed = LocalDate.of(2026, 7, 15)  // 3rd Wed -> no
        val fourthWed = LocalDate.of(2026, 7, 22) // 4th Wed -> yes
        assertTrue(GarbageScheduleCalculator.collectionsOn(firstWed, nonBurnable2nd4thWed).isEmpty())
        assertEquals(listOf("NON_BURNABLE"), GarbageScheduleCalculator.collectionsOn(secondWed, nonBurnable2nd4thWed))
        assertTrue(GarbageScheduleCalculator.collectionsOn(thirdWed, nonBurnable2nd4thWed).isEmpty())
        assertEquals(listOf("NON_BURNABLE"), GarbageScheduleCalculator.collectionsOn(fourthWed, nonBurnable2nd4thWed))
    }

    @Test
    fun nextCollectionFor_is_inclusive_of_from_date() {
        val mon = LocalDate.of(2026, 7, 6)
        assertEquals(mon, GarbageScheduleCalculator.nextCollectionFor("BURNABLE", mon, burnableWeekly))
    }

    @Test
    fun nextCollectionFor_finds_following_date() {
        val from = LocalDate.of(2026, 7, 2) // Thursday is 07-02
        // Next NON_BURNABLE (2nd/4th Wed) after 07-02 is 2nd Wed = 07-08
        assertEquals(LocalDate.of(2026, 7, 8), GarbageScheduleCalculator.nextCollectionFor("NON_BURNABLE", from, nonBurnable2nd4thWed))
    }

    @Test
    fun nextCollectionFor_skips_months_without_the_nth_occurrence() {
        // 5th Wednesday only. July 2026 has a 5th Wed (07-29); August 2026 does not; Sept has 09-30.
        val fifthWedOnly = listOf(
            GarbageSchedule(areaId = "a", categoryCode = "PAPER", weekday = 3, weeksOfMonth = listOf(5))
        )
        val fromAfterJuly = LocalDate.of(2026, 7, 30)
        assertEquals(LocalDate.of(2026, 9, 30), GarbageScheduleCalculator.nextCollectionFor("PAPER", fromAfterJuly, fifthWedOnly))
    }

    @Test
    fun nextCollectionFor_returns_null_when_no_schedule() {
        val from = LocalDate.of(2026, 7, 6)
        assertNull(GarbageScheduleCalculator.nextCollectionFor("BURNABLE", from, emptyList()))
    }

    @Test
    fun upcoming_lists_only_days_with_collections() {
        val from = LocalDate.of(2026, 7, 6) // Mon
        val result = GarbageScheduleCalculator.upcoming(from, 7, burnableWeekly) // 07-06..07-12
        // Mon 07-06 and Thu 07-09
        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2026, 7, 6), result[0].date)
        assertEquals(LocalDate.of(2026, 7, 9), result[1].date)
    }

    @Test
    fun collectionsOn_merges_multiple_categories_same_day() {
        val schedules = burnableWeekly + listOf(
            GarbageSchedule(areaId = "a", categoryCode = "PLASTIC", weekday = 1) // also Monday
        )
        val mon = LocalDate.of(2026, 7, 6)
        val codes = GarbageScheduleCalculator.collectionsOn(mon, schedules)
        assertTrue(codes.contains("BURNABLE"))
        assertTrue(codes.contains("PLASTIC"))
        assertEquals(2, codes.size)
    }
}
