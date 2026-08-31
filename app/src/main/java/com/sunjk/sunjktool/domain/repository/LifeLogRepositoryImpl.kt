package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.LifeLogEntryDao
import com.sunjk.sunjktool.data.model.LifeLogEntryEntity
import com.sunjk.sunjktool.domain.model.LifeLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LifeLogRepositoryImpl(
    private val dao: LifeLogEntryDao
) : LifeLogRepository {

    override fun getAllEntries(): Flow<List<LifeLogEntry>> =
        dao.getAllEntries().map { entities -> entities.map { it.toDomain() } }

    override fun getEntryById(id: Long): Flow<LifeLogEntry?> =
        dao.getEntryById(id).map { it?.toDomain() }

    override suspend fun saveEntry(entry: LifeLogEntry): Long {
        val entity = entry.toEntity()
        return if (entry.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entry.id
        }
    }

    override suspend fun deleteEntry(id: Long) {
        dao.delete(
            LifeLogEntryEntity(
                id = id, content = "",
                createdDate = 0L, updatedDate = 0L
            )
        )
    }

    private fun LifeLogEntryEntity.toDomain() = LifeLogEntry(
        id = id,
        content = content,
        moods = LifeLogEntry.decodeMoods(mood),
        imagePaths = LifeLogEntry.decodePaths(imagePath),
        createdDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()),
        updatedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault())
    )

    private fun LifeLogEntry.toEntity() = LifeLogEntryEntity(
        id = id,
        content = content,
        mood = LifeLogEntry.encodeMoods(moods),
        imagePath = LifeLogEntry.encodePaths(imagePaths),
        createdDate = createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedDate = updatedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}
