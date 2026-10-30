package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isApproved = 1 ORDER BY createdAt DESC")
    suspend fun getApprovedMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()
}
