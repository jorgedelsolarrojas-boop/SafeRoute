package com.example.saferouter.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import com.example.saferouter.data.local.converters.Converters  // 🔥 CORRECCIÓN: import correcto
import com.example.saferouter.data.local.dao.AlertDao
import com.example.saferouter.data.local.entity.AlertEntity

@Database(
    entities = [AlertEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safe_router_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}