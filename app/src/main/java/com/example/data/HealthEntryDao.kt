package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEntryDao {
    @Insert
    suspend fun insert(entry: HealthEntry)

    @Query("SELECT * FROM entries ORDER BY date DESC")
    fun getAll(): Flow<List<HealthEntry>>

    @Delete
    suspend fun delete(entry: HealthEntry)

    @Update
    suspend fun update(entry: HealthEntry)
}