package com.filotheque.app

import android.app.Application
import com.filotheque.app.data.backup.BackupManager
import com.filotheque.app.data.local.AppDatabase
import com.filotheque.app.data.repository.ProjectRepository
import com.filotheque.app.data.repository.ThreadRepository

class AppContainer(application: Application) {
    private val database = AppDatabase.getInstance(application)
    private val backupManager = BackupManager(database.threadDao(), database.projectDao())

    val threadRepository = ThreadRepository(
        threadDao = database.threadDao(),
        equivalenceDao = database.equivalenceDao(),
        backupManager = backupManager
    )

    val projectRepository = ProjectRepository(
        projectDao = database.projectDao(),
        threadDao = database.threadDao()
    )
}

class FilothequeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
