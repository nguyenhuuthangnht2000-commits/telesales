package com.nhakhoaquangninh.telesales.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallRecordEntity::class, FailedCallEntity::class], version = 1, exportSchema = false)
abstract class TelesalesDatabase : RoomDatabase() {
    abstract fun callRecordDao(): CallRecordDao
    abstract fun failedCallDao(): FailedCallDao

    companion object {
        @Volatile
        private var INSTANCE: TelesalesDatabase? = null

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
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
