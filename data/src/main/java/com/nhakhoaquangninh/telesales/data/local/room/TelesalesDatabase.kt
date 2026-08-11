package com.nhakhoaquangninh.telesales.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CallRecordEntity::class, FailedCallEntity::class], version = 1, exportSchema = false)
abstract class TelesalesDatabase : RoomDatabase() {
    abstract fun callRecordDao(): CallRecordDao
    abstract fun failedCallDao(): FailedCallDao

    companion object {
        @Volatile
        private var INSTANCE: TelesalesDatabase? = null

        // Mẫu cơ chế Migration từ version 1 lên 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Renamed param to 'database' to match standard or 'db'
                // Thêm các câu lệnh SQL ở đây khi thay đổi schema. 
                // Ví dụ: database.execSQL("ALTER TABLE CallRecordEntity ADD COLUMN new_column TEXT DEFAULT '' NOT NULL")
            }
        }

        fun getDatabase(context: Context): TelesalesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TelesalesDatabase::class.java,
                    "telesales_database"
                )
                // allowMainThreadQueries is used safely because the legacy API was synchronous via SharedPreferences.
                // We preserve this to avoid massive refactoring of callers.
                .allowMainThreadQueries()
                // Thêm fallback để tránh crash nếu lỡ quên viết migration khi tăng version
                .fallbackToDestructiveMigration()
                // Thêm cơ chế migration (đang để sẵn mẫu)
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
