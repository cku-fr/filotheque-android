package com.filotheque.app.data.repository

import com.filotheque.app.data.backup.BackupManager
import com.filotheque.app.data.local.dao.EquivalenceDao
import com.filotheque.app.data.local.dao.ThreadDao
import com.filotheque.app.data.local.entity.BrandEntity
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.domain.color.ColorUtils
import kotlinx.coroutines.flow.Flow

class ThreadRepository(
    private val threadDao: ThreadDao,
    private val equivalenceDao: EquivalenceDao,
    private val backupManager: BackupManager
) {
    fun observeAllThreads(): Flow<List<ThreadEntity>> = threadDao.observeAllThreads()
    fun observeOwnedThreads(): Flow<List<ThreadEntity>> = threadDao.observeOwnedThreads()
    fun observeShoppingList(): Flow<List<ThreadEntity>> = threadDao.observeShoppingList()

    suspend fun adjustQuantity(threadId: Long, delta: Int) {
        threadDao.adjustQuantity(threadId, delta)
    }

    suspend fun setShoppingList(threadId: Long, onList: Boolean) {
        threadDao.setShoppingList(threadId, onList)
    }

    suspend fun markPurchased(threadId: Long) {
        threadDao.markPurchased(threadId)
    }

    suspend fun updateNote(threadId: Long, note: String) {
        threadDao.updateNote(threadId, note)
    }

    suspend fun getEquivalents(thread: ThreadEntity): List<ThreadEntity> {
        return when (thread.brandId) {
            "DMC" -> equivalenceDao.findAnchorEquivalentsForDmc(thread.code)
            "ANCHOR" -> equivalenceDao.findDmcEquivalentsForAnchor(thread.code)
            else -> emptyList()
        }
    }

    suspend fun findClosestDmcForHex(hex: String): ThreadEntity? {
        val dmcList = threadDao.getAllDmcThreads()
        return ColorUtils.findClosestDmc(hex, dmcList)
    }

    suspend fun addCustomThread(
        brandName: String,
        code: String,
        name: String,
        hexColor: String,
        initialQuantity: Int = 1,
        isGlitter: Boolean = false
    ) {
        val cleanBrand = brandName.trim().ifEmpty { "Perso" }
        val brandId = "CUSTOM_${cleanBrand.uppercase().replace(Regex("[^A-Z0-9]"), "_")}"
        threadDao.insertBrand(BrandEntity(id = brandId, name = cleanBrand, isBuiltIn = false))
        threadDao.insertThread(
            ThreadEntity(
                brandId = brandId,
                brandName = cleanBrand,
                code = code.trim().ifEmpty { "PERSO" },
                name = name.trim().ifEmpty { "Couleur perso" },
                hexColor = hexColor,
                colorFamily = ColorUtils.classifyColorFamily(hexColor),
                quantity = initialQuantity.coerceAtLeast(0),
                isOnShoppingList = false,
                isGlitter = isGlitter
            )
        )
    }

    suspend fun batchIncrementByCodes(brandId: String, rawCodes: String): Pair<List<String>, List<String>> {
        val tokens = rawCodes.split(Regex("[,;\\s]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val found = mutableListOf<String>()
        val notFound = mutableListOf<String>()

        for (rawCode in tokens) {
            val code = if (brandId == "DMC" && rawCode == "5200") "B5200" else rawCode
            val thread = threadDao.findByBrandAndCode(brandId, code)
            if (thread != null) {
                threadDao.adjustQuantity(thread.id, 1)
                found.add(thread.code)
            } else {
                notFound.add(code)
            }
        }
        return found to notFound
    }

    suspend fun updateCustomThread(
        thread: ThreadEntity,
        brandName: String,
        code: String,
        name: String,
        hexColor: String,
        isGlitter: Boolean = false
    ): ThreadEntity {
        val cleanBrand = brandName.trim().ifEmpty { "Perso" }
        val cleanCode = code.trim().ifEmpty { "PERSO" }
        val brandId = "CUSTOM_${cleanBrand.uppercase().replace(Regex("[^A-Z0-9]"), "_")}"
        val existing = threadDao.findByBrandAndCode(brandId, cleanCode)
        if (existing != null && existing.id != thread.id) {
            throw IllegalArgumentException("Le code $cleanCode existe déjà pour $cleanBrand")
        }
        threadDao.insertBrand(BrandEntity(id = brandId, name = cleanBrand, isBuiltIn = false))
        val updated = thread.copy(
            brandId = brandId,
            brandName = cleanBrand,
            code = cleanCode,
            name = name.trim().ifEmpty { "Couleur perso" },
            hexColor = hexColor,
            colorFamily = ColorUtils.classifyColorFamily(hexColor),
            isGlitter = isGlitter,
            updatedAt = System.currentTimeMillis()
        )
        threadDao.updateThread(updated)
        return updated
    }

    suspend fun deleteCustomThread(threadId: Long) {
        threadDao.deleteCustomThread(threadId)
    }

    suspend fun exportBackupJson(): String = backupManager.exportToJson()
    suspend fun importBackupJson(rawJson: String): Int = backupManager.importFromJson(rawJson)
}
