package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.KeystoreDao
import com.example.data.model.KeystoreEntity

/**
 * Base de datos principal de la aplicación construida con Room.
 * Gestiona de forma segura el almacenamiento local de las claves y metadatos generados.
 */
@Database(
    entities = [KeystoreEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /** Retorna el DAO para interactuar con la tabla de keystores */
    abstract fun keystoreDao(): KeystoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migración 1 -> 2: Agrega las columnas 'city' y 'state' para soportar los campos
         * completos del estándar X.500 Distinguished Name exigidos por Google / Android Studio.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE keystores ADD COLUMN city TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE keystores ADD COLUMN state TEXT NOT NULL DEFAULT ''")
            }
        }

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
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
