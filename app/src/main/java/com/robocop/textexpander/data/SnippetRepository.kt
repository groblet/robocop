package com.robocop.textexpander.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SnippetRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val snippetDao = db.snippetDao()
    private val autoCorrectDao = db.autoCorrectDao()

    fun observeSnippets(): Flow<List<Snippet>> = snippetDao.observeAll()
    fun observeEnabledSnippets(): Flow<List<Snippet>> = snippetDao.observeEnabled()
    suspend fun getSnippet(id: Long): Snippet? = snippetDao.getById(id)
    suspend fun isTriggerTaken(trigger: String, excludeId: Long = -1): Boolean =
        snippetDao.countByTrigger(trigger, excludeId) > 0
    suspend fun saveSnippet(snippet: Snippet): Long = snippetDao.upsert(snippet)
    suspend fun deleteSnippet(snippet: Snippet) = snippetDao.delete(snippet)

    fun observeAutoCorrectEntries(): Flow<List<AutoCorrectEntry>> = autoCorrectDao.observeAll()
    fun observeEnabledAutoCorrectEntries(): Flow<List<AutoCorrectEntry>> = autoCorrectDao.observeEnabled()
    suspend fun saveAutoCorrectEntry(entry: AutoCorrectEntry): Long = autoCorrectDao.upsert(entry)
    suspend fun deleteAutoCorrectEntry(entry: AutoCorrectEntry) = autoCorrectDao.delete(entry)

    suspend fun seedBuiltInAutoCorrectIfEmpty() {
        if (autoCorrectDao.count() == 0) {
            autoCorrectDao.insertAll(DefaultAutoCorrectSeed.toEntities())
        }
    }

    companion object {
        @Volatile private var instance: SnippetRepository? = null
        fun get(context: Context): SnippetRepository =
            instance ?: synchronized(this) {
                instance ?: SnippetRepository(context).also { instance = it }
            }
    }
}
