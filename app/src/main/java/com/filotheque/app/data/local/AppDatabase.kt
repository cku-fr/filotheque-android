package com.filotheque.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    val family: String,
    val isGlitter: Boolean = false,
    val gradientColors: String = ""
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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
    abstract fun equivalenceDao(): EquivalenceDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val PREFS_NAME = "filotheque_prefs"
        private const val KEY_SEED_VERSION = "seed_version"
        private const val CURRENT_SEED_VERSION = 3

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE threads ADD COLUMN isGlitter INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE threads ADD COLUMN gradientColors TEXT NOT NULL DEFAULT ''")
            }
        }

        private val json = Json { ignoreUnknownKeys = true }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "filotheque.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let {
                                    prepopulate(appContext, it)
                                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                        .edit().putInt(KEY_SEED_VERSION, CURRENT_SEED_VERSION).apply()
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            if (prefs.getInt(KEY_SEED_VERSION, 1) < CURRENT_SEED_VERSION) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    INSTANCE?.let {
                                        syncMissingBuiltInThreads(appContext, it)
                                        prefs.edit().putInt(KEY_SEED_VERSION, CURRENT_SEED_VERSION).apply()
                                    }
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun syncMissingBuiltInThreads(context: Context, db: AppDatabase) {
            val threadDao = db.threadDao()
            val dmcList = loadChart(context, "dmc.json", "DMC", "DMC")
            val anchorList = loadChart(context, "anchor.json", "ANCHOR", "Anchor")
            threadDao.insertThreads(dmcList + anchorList)
            threadDao.markSpecialDmcGlitter()
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
                    isOnShoppingList = false,
                    isGlitter = item.isGlitter,
                    gradientColors = item.gradientColors
                )
            }
        }
    }
}
