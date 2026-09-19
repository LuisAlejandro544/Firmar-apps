package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.KeystoreDao
import com.example.data.model.KeystoreEntity

/**
 * Base de datos principal de la aplicación construida con Room.
 * Gestiona de forma segura el almacenamiento local de las claves y metadatos generados.
 */
@Database(
    entities = [KeystoreEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /** Retorna el DAO para interactuar con la tabla de keystores */
    abstract fun keystoreDao(): KeystoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia singleton de la base de datos de Room.
         * Garantiza una única conexión durante el ciclo de vida de la app.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keystore_creator_database"
                ).fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
