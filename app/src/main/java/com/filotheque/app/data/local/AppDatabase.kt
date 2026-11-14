package com.filotheque.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.filotheque.app.data.local.dao.EquivalenceDao
import com.filotheque.app.data.local.dao.ProjectDao
import com.filotheque.app.data.local.dao.ThreadDao
import com.filotheque.app.data.local.entity.BrandEntity
import com.filotheque.app.data.local.entity.EquivalenceEntity
import com.filotheque.app.data.local.entity.ProjectEntity
import com.filotheque.app.data.local.entity.ProjectThreadCrossRef
import com.filotheque.app.data.local.entity.ThreadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RawChartThread(
    val code: String,
    val name: String,
    val hex: String,
    val family: String
)

@Serializable
private data class RawEquivalence(
    val dmcCode: String,
    val anchorCode: String,
    val matchQuality: Int = 1
)

@Database(
    entities = [
        BrandEntity::class,
        ThreadEntity::class,
        EquivalenceEntity::class,
        ProjectEntity::class,
        ProjectThreadCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
    abstract fun equivalenceDao(): EquivalenceDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val json = Json { ignoreUnknownKeys = true }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "filotheque.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { prepopulate(context.applicationContext, it) }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulate(context: Context, db: AppDatabase) {
            val threadDao = db.threadDao()
            val eqDao = db.equivalenceDao()

            threadDao.insertBrands(
                listOf(
                    BrandEntity(id = "DMC", name = "DMC", isBuiltIn = true),
                    BrandEntity(id = "ANCHOR", name = "Anchor", isBuiltIn = true)
                )
            )

            val dmcList = loadChart(context, "dmc.json", "DMC", "DMC")
            val anchorList = loadChart(context, "anchor.json", "ANCHOR", "Anchor")
            threadDao.insertThreads(dmcList + anchorList)

            val eqRaw = context.assets.open("equivalence.json").bufferedReader().use { it.readText() }
            val eqList = json.decodeFromString<List<RawEquivalence>>(eqRaw).map {
                EquivalenceEntity(
                    dmcCode = it.dmcCode,
                    anchorCode = it.anchorCode,
                    matchQuality = it.matchQuality
                )
            }
            eqDao.insertAll(eqList)
        }

        private fun loadChart(
            context: Context,
            assetName: String,
            brandId: String,
            brandName: String
        ): List<ThreadEntity> {
            val raw = context.assets.open(assetName).bufferedReader().use { it.readText() }
            return json.decodeFromString<List<RawChartThread>>(raw).map { item ->
                ThreadEntity(
                    brandId = brandId,
                    brandName = brandName,
                    code = item.code,
                    name = item.name,
                    hexColor = item.hex,
                    colorFamily = item.family,
                    quantity = 0,
                    isOnShoppingList = false
                )
            }
        }
    }
}
