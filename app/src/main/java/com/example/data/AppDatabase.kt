package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HealthEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun healthEntryDao(): HealthEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_db"
                ).build().also { INSTANCE = it }
            }
    }
}