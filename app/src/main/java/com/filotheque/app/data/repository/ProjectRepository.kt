package com.filotheque.app.data.repository

import com.filotheque.app.data.local.dao.ProjectDao
import com.filotheque.app.data.local.dao.ThreadDao
import com.filotheque.app.data.local.entity.ProjectEntity
import com.filotheque.app.data.local.entity.ProjectThreadCrossRef
import com.filotheque.app.data.local.entity.ProjectWithThreads
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val threadDao: ThreadDao
) {
    fun observeProjects(): Flow<List<ProjectWithThreads>> = projectDao.observeProjectsWithThreads()

    suspend fun createProject(name: String, patternSource: String, threadIds: List<Long>) {
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                patternSource = patternSource.trim()
            )
        )
        threadIds.forEach { tid ->
            projectDao.addThreadToProject(ProjectThreadCrossRef(projectId = projectId, threadId = tid))
        }
    }

    suspend fun addThreadToProject(projectId: Long, threadId: Long) {
        projectDao.addThreadToProject(ProjectThreadCrossRef(projectId = projectId, threadId = threadId))
    }

    suspend fun removeThreadFromProject(projectId: Long, threadId: Long) {
        projectDao.removeThreadFromProject(projectId, threadId)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    suspend fun addMissingThreadsToShoppingList(project: ProjectWithThreads): Int {
        var added = 0
        project.threads.filter { it.quantity == 0 && !it.isOnShoppingList }.forEach { thread ->
            threadDao.setShoppingList(thread.id, true)
            added++
        }
        return added
    }
}
