package com.zhiqi.app.data

import kotlinx.coroutines.flow.Flow

class DailyIndicatorRepository(private val indicatorDao: DailyIndicatorDao) {
    fun indicatorsByDate(dateKey: String): Flow<List<DailyIndicatorEntity>> = indicatorDao.observeByDate(dateKey)

    fun allIndicators(): Flow<List<DailyIndicatorEntity>> = indicatorDao.observeAll()

    suspend fun getAll(): List<DailyIndicatorEntity> = indicatorDao.snapshotAll()

    suspend fun save(indicator: DailyIndicatorEntity): Long = indicatorDao.upsert(indicator)

    suspend fun deleteByDateAndMetric(dateKey: String, metricKey: String): Int =
        indicatorDao.deleteByDateAndMetricKey(dateKey, metricKey)

    suspend fun clearAll() = indicatorDao.clearAll()
}
