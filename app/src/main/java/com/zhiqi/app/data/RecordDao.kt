package com.zhiqi.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RecordEntity): Long

    @Update
    suspend fun update(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("SELECT * FROM records ORDER BY timeMillis DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records ORDER BY timeMillis DESC")
    suspend fun snapshotAll(): List<RecordEntity>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun findById(id: Long): RecordEntity?

    @Query("DELETE FROM records")
    suspend fun clearAll()
}
