package com.example.saferouter.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import com.example.saferouter.data.local.converters.Converters
import com.example.saferouter.data.local.dao.AlertDao // Ya existía
import com.example.saferouter.model.Reporte // 👈 TU MODELO EXISTENTE (debe ser una entidad)
import com.example.saferouter.data.dao.ReporteDao // 👈 EL DAO QUE CREAMOS PARA LOS REPORTES
import com.example.saferouter.data.local.entity.AlertEntity // Ya existía

// 🚨 IMPORTANTE: Necesitas incrementar la versión si ya tenías una app instalada con la DB.
// Asumo que tu modelo 'Reporte' tiene la anotación @Entity
@Database(
    entities = [AlertEntity::class, Reporte::class], // 👈 AÑADIR TU MODELO REPORTE
    version = 2, // 👈 INCREMENTAMOS LA VERSIÓN para incluir Reporte (Si no, fallará)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao // DAO existente
    abstract fun reporteDao(): ReporteDao // 👈 AÑADIR ESTO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safe_router_db"
                ).fallbackToDestructiveMigration() // 👈 Añadir esto ayuda a evitar fallos en el desorden
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}