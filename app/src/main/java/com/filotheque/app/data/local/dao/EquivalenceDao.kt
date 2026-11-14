package com.filotheque.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.filotheque.app.data.local.entity.EquivalenceEntity
import com.filotheque.app.data.local.entity.ThreadEntity

@Dao
interface EquivalenceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<EquivalenceEntity>)

    @Query(
        """
        SELECT t.* FROM threads t
        INNER JOIN equivalences e ON t.code = e.anchorCode
        WHERE t.brandId = 'ANCHOR' AND UPPER(e.dmcCode) = UPPER(:dmcCode)
        """
    )
    suspend fun findAnchorEquivalentsForDmc(dmcCode: String): List<ThreadEntity>

    @Query(
        """
        SELECT t.* FROM threads t
        INNER JOIN equivalences e ON t.code = e.dmcCode
        WHERE t.brandId = 'DMC' AND UPPER(e.anchorCode) = UPPER(:anchorCode)
        """
    )
    suspend fun findDmcEquivalentsForAnchor(anchorCode: String): List<ThreadEntity>
}
