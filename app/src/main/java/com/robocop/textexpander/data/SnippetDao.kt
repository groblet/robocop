package com.robocop.textexpander.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY `group`, name")
    fun observeAll(): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets WHERE enabled = 1")
    fun observeEnabled(): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets WHERE id = :id")
    suspend fun getById(id: Long): Snippet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snippet: Snippet): Long

    @Update
    suspend fun update(snippet: Snippet)

    @Delete
    suspend fun delete(snippet: Snippet)

    @Query("SELECT COUNT(*) FROM snippets WHERE trigger = :trigger AND id != :excludeId")
    suspend fun countByTrigger(trigger: String, excludeId: Long = -1): Int
}
