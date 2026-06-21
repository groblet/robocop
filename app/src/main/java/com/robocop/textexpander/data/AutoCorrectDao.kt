package com.robocop.textexpander.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoCorrectDao {
    @Query("SELECT * FROM autocorrect_entries ORDER BY typo")
    fun observeAll(): Flow<List<AutoCorrectEntry>>

    @Query("SELECT * FROM autocorrect_entries WHERE enabled = 1")
    fun observeEnabled(): Flow<List<AutoCorrectEntry>>

    @Query("SELECT COUNT(*) FROM autocorrect_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<AutoCorrectEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AutoCorrectEntry): Long

    @Update
    suspend fun update(entry: AutoCorrectEntry)

    @Delete
    suspend fun delete(entry: AutoCorrectEntry)
}
