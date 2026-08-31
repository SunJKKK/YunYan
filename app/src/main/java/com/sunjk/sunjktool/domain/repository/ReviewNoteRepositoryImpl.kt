package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.ReviewNoteDao
import com.sunjk.sunjktool.data.model.ReviewNoteEntity
import com.sunjk.sunjktool.domain.model.ReviewNote
import com.sunjk.sunjktool.domain.model.ReviewNoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReviewNoteRepositoryImpl(
    private val dao: ReviewNoteDao
) : ReviewNoteRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getByLogEntryId(logEntryId: Long): Flow<List<ReviewNote>> =
        dao.getAllByLogEntryId(logEntryId).map { list -> list.map { it.toDomain() } }

    override fun getById(id: Long): Flow<ReviewNote?> =
        dao.getById(id).map { it?.toDomain() }

    override fun getAll(): Flow<List<ReviewNote>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(note: ReviewNote): Long {
        val entity = note.toEntity()
        return if (note.id == 0L) {
            dao.insert(entity)
        } else {
            dao.insert(entity)
            note.id
        }
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteAllForEntry(logEntryId: Long) {
        dao.deleteAllForEntry(logEntryId)
    }

    override suspend fun count(): Int = dao.count()

    private fun ReviewNoteEntity.toDomain(): ReviewNote {
        val paths: List<String> = try {
            if (imagePaths.isNullOrBlank()) emptyList()
            else json.decodeFromString(ListSerializer(String.serializer()), imagePaths)
        } catch (_: Exception) { emptyList() }

        return ReviewNote(
            id = id,
            logEntryId = logEntryId,
            content = content,
            imagePaths = paths,
            sourceType = try { ReviewNoteSource.valueOf(sourceType) } catch (_: Exception) { ReviewNoteSource.MANUAL },
            flashcardSessionId = flashcardSessionId,
            createdDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()),
            updatedDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault())
        )
    }

    private fun ReviewNote.toEntity(): ReviewNoteEntity {
        val pathsJson = if (imagePaths.isEmpty()) null else json.encodeToString(ListSerializer(String.serializer()), imagePaths)
        return ReviewNoteEntity(
            id = id,
            logEntryId = logEntryId,
            content = content,
            imagePaths = pathsJson,
            sourceType = sourceType.name,
            flashcardSessionId = flashcardSessionId,
            createdDate = createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedDate = updatedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }
}
