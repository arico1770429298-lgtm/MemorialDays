package com.example.memorialdays

import android.app.Application
import android.content.Context
import com.example.memorialdays.data.EventRepository
import com.example.memorialdays.data.BackupRepository
import com.example.memorialdays.data.MemorialDatabase
import com.example.memorialdays.share.PosterRenderer
import com.example.memorialdays.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MemorialApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = MemorialDatabase.get(appContext)
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository = EventRepository(database, database.eventDao())
    val backupRepository = BackupRepository(appContext, repository)
    val posterRenderer = PosterRenderer(appContext)
    val widgetUpdater = WidgetUpdater(appContext, applicationScope)
}
