package com.filotheque.app.data.backup

import com.filotheque.app.data.local.dao.ProjectDao
import com.filotheque.app.data.local.dao.ThreadDao
import com.filotheque.app.data.local.entity.BrandEntity
import com.filotheque.app.data.local.entity.ProjectEntity
import com.filotheque.app.data.local.entity.ProjectThreadCrossRef
import com.filotheque.app.data.local.entity.ThreadEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val threads: List<BackupThread> = emptyList(),
    val projects: List<BackupProject> = emptyList()
)

@Serializable
data class BackupThread(
    val brandId: String,
    val brandName: String,
    val code: String,
    val name: String,
    val hexColor: String,
    val colorFamily: String,
    val quantity: Int,
    val isOnShoppingList: Boolean,
    val note: String
)

@Serializable
data class BackupProject(
    val name: String,
    val patternSource: String,
    val status: String,
    val note: String,
    val threadRefs: List<BackupThreadRef>
)

@Serializable
data class BackupThreadRef(
    val brandId: String,
    val code: String
)

class BackupManager(
    private val threadDao: ThreadDao,
    private val projectDao: ProjectDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToJson(): String {
        val modifiedThreads = threadDao.getUserModifiedOrCustomThreads().map {
            BackupThread(
                brandId = it.brandId,
                brandName = it.brandName,
                code = it.code,
                name = it.name,
                hexColor = it.hexColor,
                colorFamily = it.colorFamily,
                quantity = it.quantity,
                isOnShoppingList = it.isOnShoppingList,
                note = it.note
            )
        }
        val projects = projectDao.getAllProjectsWithThreads().map { pwt ->
            BackupProject(
                name = pwt.project.name,
                patternSource = pwt.project.patternSource,
                status = pwt.project.status,
                note = pwt.project.note,
                threadRefs = pwt.threads.map { BackupThreadRef(it.brandId, it.code) }
            )
        }
        return json.encodeToString(
            BackupPayload(threads = modifiedThreads, projects = projects)
        )
    }

    suspend fun importFromJson(rawJson: String): Int {
        val payload = json.decodeFromString<BackupPayload>(rawJson)
        var restoredCount = 0

        payload.threads.forEach { bt ->
            val existing = threadDao.findByBrandAndCode(bt.brandId, bt.code)
            if (existing != null) {
                threadDao.updateThread(
                    existing.copy(
                        quantity = bt.quantity,
                        isOnShoppingList = bt.isOnShoppingList,
                        note = bt.note,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                threadDao.insertBrand(
                    BrandEntity(id = bt.brandId, name = bt.brandName, isBuiltIn = false)
                )
                threadDao.insertThread(
                    ThreadEntity(
                        brandId = bt.brandId,
                        brandName = bt.brandName,
                        code = bt.code,
                        name = bt.name,
                        hexColor = bt.hexColor,
                        colorFamily = bt.colorFamily,
                        quantity = bt.quantity,
                        isOnShoppingList = bt.isOnShoppingList,
                        note = bt.note
                    )
                )
            }
            restoredCount++
        }

        payload.projects.forEach { bp ->
            val projectId = projectDao.insertProject(
                ProjectEntity(
                    name = bp.name,
                    patternSource = bp.patternSource,
                    status = bp.status,
                    note = bp.note
                )
            )
            bp.threadRefs.forEach { ref ->
                val thread = threadDao.findByBrandAndCode(ref.brandId, ref.code)
                if (thread != null) {
                    projectDao.addThreadToProject(
                        ProjectThreadCrossRef(projectId = projectId, threadId = thread.id)
                    )
                }
            }
        }
        return restoredCount
    }
}
