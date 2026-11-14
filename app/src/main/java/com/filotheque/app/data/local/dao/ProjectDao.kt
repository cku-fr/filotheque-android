package com.filotheque.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.filotheque.app.data.local.entity.ProjectEntity
import com.filotheque.app.data.local.entity.ProjectThreadCrossRef
import com.filotheque.app.data.local.entity.ProjectWithThreads
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Transaction
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeProjectsWithThreads(): Flow<List<ProjectWithThreads>>

    @Transaction
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun getAllProjectsWithThreads(): List<ProjectWithThreads>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addThreadToProject(crossRef: ProjectThreadCrossRef)

    @Query("DELETE FROM project_threads WHERE projectId = :projectId AND threadId = :threadId")
    suspend fun removeThreadFromProject(projectId: Long, threadId: Long)
}
