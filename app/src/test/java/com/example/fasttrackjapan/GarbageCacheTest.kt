package com.example.fasttrackjapan

import org.junit.Assert.assertEquals
import org.junit.Test

class GarbageCacheTest {

    @Test
    fun snapshot_survives_encode_then_decode() {
        val snapshot = GarbageScheduleSnapshot(
            area = GarbageArea(id = "area1", wardCode = "setagaya", nameJa = "三軒茶屋一丁目"),
            categories = listOf(
                GarbageCategory(code = "BURNABLE", nameJa = "燃えるゴミ", nameEn = "Burnable", sort = 0)
            ),
            schedules = listOf(
                GarbageSchedule(id = "s1", areaId = "area1", categoryCode = "BURNABLE", weekday = 1),
                GarbageSchedule(id = "s2", areaId = "area1", categoryCode = "BURNABLE", weekday = 4, weeksOfMonth = listOf(1, 3))
            )
        )
        val json = GarbageCache.encode(snapshot)
        val decoded = GarbageCache.decode(json)
        assertEquals(snapshot, decoded)
    }

    @Test
    fun decode_returns_null_on_garbage_input() {
        assertEquals(null, GarbageCache.decode("not json"))
    }
}
