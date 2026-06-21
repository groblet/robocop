package com.robocop.textexpander

import android.app.Application
import com.robocop.textexpander.data.SnippetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RobocopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val repository = SnippetRepository.get(this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedBuiltInAutoCorrectIfEmpty()
        }
    }
}
