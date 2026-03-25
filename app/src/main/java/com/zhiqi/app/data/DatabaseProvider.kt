package com.zhiqi.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.zhiqi.app.security.CryptoManager

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null
    private const val DATABASE_NAME = "zhiqi.db"

    fun get(context: Context): AppDatabase {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(appContext).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        val supportFactory = createSupportFactory(context)
        return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(supportFactory)
            .addMigrations(MIGRATION_1_TO_2)
            .build()
    }

    private fun createSupportFactory(context: Context): SupportFactory {
        SQLiteDatabase.loadLibs(context)
        val passphrase = CryptoManager(context).getOrCreateDbPassphrase()
        return SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
    }

    private val MIGRATION_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `daily_indicators` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `dateKey` TEXT NOT NULL,
                    `metricKey` TEXT NOT NULL,
                    `optionValue` TEXT NOT NULL,
                    `displayLabel` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_indicators_dateKey_metricKey`
                ON `daily_indicators` (`dateKey`, `metricKey`)
                """.trimIndent()
            )
        }
    }
}
