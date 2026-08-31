package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.HabitDao
import com.sunjk.sunjktool.data.local.dao.HabitRecordDao
import com.sunjk.sunjktool.data.model.HabitEntity
import com.sunjk.sunjktool.data.model.HabitRecordEntity
import com.sunjk.sunjktool.data.sync.SyncTrigger
import com.sunjk.sunjktool.domain.model.Habit
import com.sunjk.sunjktool.domain.model.HabitRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitRecordDao: HabitRecordDao
) : HabitRepository {

    override fun getAll(): Flow<List<Habit>> =
        habitDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getById(id: Long): Flow<Habit?> =
        habitDao.getById(id).map { it?.toDomain() }

    override fun getRecordsByHabitId(habitId: Long): Flow<List<HabitRecord>> =
        habitRecordDao.getByHabitId(habitId).map { entities -> entities.map { it.toDomain() } }

    override fun getRecordsByHabitIdSince(habitId: Long, since: LocalDate): Flow<List<HabitRecord>> {
        val sinceStr = "${habitId}_${since}"
        return habitRecordDao.getByHabitIdSince(habitId, sinceStr).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllRecords(): Flow<List<HabitRecordEntity>> =
        habitRecordDao.getAll()

    override suspend fun save(habit: Habit): Long {
        val now = System.currentTimeMillis()
        val entity = habit.toEntity(now)
        SyncTrigger.bumpEntity("habits")
        SyncTrigger.requestAutoSync()
        return if (habit.id == 0L) {
            habitDao.insert(entity)
        } else {
            habitDao.update(entity)
            habit.id
        }
    }

    override suspend fun delete(id: Long) {
        // Delete all associated records first
        habitRecordDao.deleteByHabitId(id)
        habitDao.delete(HabitEntity(id = id, name = "", description = "", colorArgb = 0, createdAt = 0, updatedAt = 0))
        SyncTrigger.bumpEntity("habits")
        SyncTrigger.bumpEntity("habit_records")
        SyncTrigger.requestAutoSync()
    }

    override suspend fun toggleRecord(habitId: Long, date: LocalDate) {
        val dateKey = "${habitId}_${date}"
        val existing = habitRecordDao.getByDate(dateKey)
        val now = System.currentTimeMillis()
        if (existing != null) {
            habitRecordDao.upsert(existing.copy(isCompleted = !existing.isCompleted, updatedAt = now))
        } else {
            habitRecordDao.upsert(
                HabitRecordEntity(
                    date = dateKey,
                    habitId = habitId,
                    isCompleted = true,
                    updatedAt = now
                )
            )
        }
        SyncTrigger.bumpEntity("habit_records")
        SyncTrigger.requestAutoSync()
    }

    override suspend fun isCompleted(habitId: Long, date: LocalDate): Boolean {
        val dateKey = "${habitId}_${date}"
        return habitRecordDao.getByDate(dateKey)?.isCompleted == true
    }

    // ─── Entity ↔ Domain mapping ──────────────────────────────────────

    private fun HabitEntity.toDomain(): Habit = Habit(
        id = id,
        name = name,
        description = description,
        colorArgb = colorArgb,
        createdAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault()),
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault())
    )

    private fun Habit.toEntity(now: Long): HabitEntity = HabitEntity(
        id = id,
        name = name,
        description = description,
        colorArgb = colorArgb,
        createdAt = if (id == 0L) now else createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedAt = now
    )

    private fun HabitRecordEntity.toDomain(): HabitRecord = HabitRecord(
        date = parseDateFromKey(date),
        habitId = habitId,
        isCompleted = isCompleted,
        updatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault())
    )

    private fun parseDateFromKey(key: String): LocalDate {
        // key format: "{habitId}_yyyy-MM-dd"
        val underscoreIdx = key.indexOf('_')
        return if (underscoreIdx >= 0) LocalDate.parse(key.substring(underscoreIdx + 1))
        else LocalDate.now()
    }
}
