package com.filotheque.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.filotheque.app.data.local.entity.BrandEntity
import com.filotheque.app.data.local.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {

    @Query("SELECT * FROM threads ORDER BY brandId ASC, code ASC")
    fun observeAllThreads(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE quantity > 0 ORDER BY brandId ASC, code ASC")
    fun observeOwnedThreads(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE isOnShoppingList = 1 ORDER BY brandName ASC, code ASC")
    fun observeShoppingList(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE id = :id")
    suspend fun getThreadById(id: Long): ThreadEntity?

    @Query("SELECT * FROM threads WHERE brandId = :brandId AND UPPER(code) = UPPER(:code) LIMIT 1")
    suspend fun findByBrandAndCode(brandId: String, code: String): ThreadEntity?

    @Query("SELECT * FROM threads WHERE brandId = 'DMC'")
    suspend fun getAllDmcThreads(): List<ThreadEntity>

    @Query("SELECT * FROM threads WHERE quantity > 0 OR isOnShoppingList = 1 OR note != '' OR brandId NOT IN ('DMC', 'ANCHOR')")
    suspend fun getUserModifiedOrCustomThreads(): List<ThreadEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBrands(brands: List<BrandEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBrand(brand: BrandEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertThreads(threads: List<ThreadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ThreadEntity): Long

    @Update
    suspend fun updateThread(thread: ThreadEntity)

    @Query("UPDATE threads SET quantity = MAX(0, quantity + :delta), updatedAt = :now WHERE id = :threadId")
    suspend fun adjustQuantity(threadId: Long, delta: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE threads SET isOnShoppingList = :onList, updatedAt = :now WHERE id = :threadId")
    suspend fun setShoppingList(threadId: Long, onList: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE threads SET quantity = quantity + 1, isOnShoppingList = 0, updatedAt = :now WHERE id = :threadId")
    suspend fun markPurchased(threadId: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE threads SET note = :note, updatedAt = :now WHERE id = :threadId")
    suspend fun updateNote(threadId: Long, note: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM threads WHERE id = :threadId AND brandId NOT IN ('DMC', 'ANCHOR')")
    suspend fun deleteCustomThread(threadId: Long)

    @Query("UPDATE threads SET isGlitter = 1 WHERE brandId = 'DMC' AND ((UPPER(code) LIKE 'E%' AND UPPER(code) != 'ECRU') OR UPPER(code) LIKE 'C%')")
    suspend fun markSpecialDmcGlitter()
}
