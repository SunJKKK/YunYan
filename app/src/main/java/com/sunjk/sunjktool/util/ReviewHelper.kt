package com.sunjk.sunjktool.util

import com.sunjk.sunjktool.data.local.dao.ReviewStatusDao
import com.sunjk.sunjktool.data.model.ReviewStatusEntity
import com.sunjk.sunjktool.domain.model.LogEntry
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class ReviewHelper(private val dao: ReviewStatusDao) {

    /** Generate review entries for a learning record. */
    suspend fun generateFor(entry: LogEntry, skipExisting: Boolean = true) {
        val recordDate = entry.createdDate.toLocalDate()
        val today = LocalDate.now()

        // Daily review: same day (appears after 21:00 today)
        val dailyToday = toEpochMillis(recordDate)
        upsertIfNeeded(entry.id, dailyToday, "daily", skipExisting)

        // Daily review: next day (appears next morning 7:30)
        val dailyNext = toEpochMillis(recordDate.plusDays(1))
        upsertIfNeeded(entry.id, dailyNext, "daily", skipExisting)

        // Weekly review: Sunday of the record's week
        val sunday = recordDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        upsertIfNeeded(entry.id, toEpochMillis(sunday), "weekly", skipExisting)

        // Monthly review: last day of the record's month
        val monthEnd = recordDate.with(TemporalAdjusters.lastDayOfMonth())
        upsertIfNeeded(entry.id, toEpochMillis(monthEnd), "monthly", skipExisting)
    }

    /** Generate missing reviews for all entries up to today. */
    suspend fun ensureAllGenerated(entries: List<LogEntry>) {
        for (entry in entries) {
            val existing = try { dao.getByEntryId(entry.id).first() } catch (_: Exception) { emptyList() }
            val existingCount = existing.size
            if (existingCount < 4) {
                generateFor(entry, skipExisting = false)
            }
        }
    }

    /** Get the effective "today" date for review queries. Always returns today. */
    fun todayReviewDates(): Pair<Long, String> {
        val today = LocalDate.now()
        return toEpochMillis(today) to "daily"
    }

    /** Delete all review tasks for a log entry. */
    suspend fun deleteByEntryId(entryId: Long) {
        dao.deleteByEntryId(entryId)
    }

    private suspend fun upsertIfNeeded(entryId: Long, dateMillis: Long, type: String, skip: Boolean) {
        if (skip) {
            val existing = try { dao.getByEntryId(entryId).first() } catch (_: Exception) { emptyList() }
            if (existing.any { it.reviewDate == dateMillis && it.reviewType == type }) return
        }
        dao.upsert(ReviewStatusEntity(logEntryId = entryId, reviewDate = dateMillis, reviewType = type))
    }

    private fun toEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
