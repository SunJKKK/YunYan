package com.sunjk.sunjktool.domain.repository

import com.sunjk.sunjktool.data.local.dao.CountdownDao
import com.sunjk.sunjktool.data.model.CountdownEntity
import com.sunjk.sunjktool.domain.model.Countdown
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CountdownRepositoryImpl(
    private val dao: CountdownDao
) : CountdownRepository {

    override fun getAll(): Flow<List<Countdown>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getById(id: Long): Flow<Countdown?> =
        dao.getById(id).map { it?.toDomain() }

    override suspend fun save(countdown: Countdown): Long {
        val entity = countdown.toEntity()
        return if (countdown.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            countdown.id
        }
    }

    override suspend fun delete(id: Long) {
        dao.delete(
            CountdownEntity(
                id = id, title = "",
                targetDate = 0L, createdDate = 0L, updatedDate = 0L
            )
        )
    }

    private fun CountdownEntity.toDomain() = Countdown(
        id = id,
        title = title,
        targetDate = Instant.ofEpochMilli(targetDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        note = note,
        createdDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(createdDate), ZoneId.systemDefault()
        ),
        updatedDate = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(updatedDate), ZoneId.systemDefault()
        )
    )

    private fun Countdown.toEntity() = CountdownEntity(
        id = id,
        title = title,
        targetDate = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        note = note,
        createdDate = createdDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        updatedDate = updatedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}
