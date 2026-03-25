package com.zhiqi.app.data

import kotlinx.coroutines.flow.Flow

class RecordRepository(private val recordDao: RecordDao) {
    fun records(): Flow<List<RecordEntity>> = recordDao.observeAll()

    suspend fun getAll(): List<RecordEntity> = recordDao.snapshotAll()

    suspend fun add(record: RecordEntity): Long = recordDao.insert(record)

    suspend fun update(record: RecordEntity) = recordDao.update(record)

    suspend fun delete(record: RecordEntity) = recordDao.delete(record)

    suspend fun getById(id: Long): RecordEntity? = recordDao.findById(id)

    suspend fun clearAll() = recordDao.clearAll()
}
