package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.KeystoreEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la gestión de keystores en la base de datos Room.
 * Proporciona métodos reactivos mediante Kotlin Flow para observar cambios en tiempo real.
 */
@Dao
interface KeystoreDao {

    /**
     * Obtiene la lista completa de keystores ordenadas por fecha de creación descendente (más recientes primero).
     */
    @Query("SELECT * FROM keystores ORDER BY createdAt DESC")
    fun getAllKeystores(): Flow<List<KeystoreEntity>>

    /**
     * Obtiene una keystore específica por su identificador único.
     */
    @Query("SELECT * FROM keystores WHERE id = :id LIMIT 1")
    fun getKeystoreById(id: Long): Flow<KeystoreEntity?>

    /**
     * Inserta una nueva keystore en la base de datos y retorna su identificador autogenerado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeystore(keystore: KeystoreEntity): Long

    /**
     * Elimina el registro de una keystore de la base de datos.
     */
    @Delete
    suspend fun deleteKeystore(keystore: KeystoreEntity)

    /**
     * Elimina una keystore por su identificador primario.
     */
    @Query("DELETE FROM keystores WHERE id = :id")
    suspend fun deleteKeystoreById(id: Long)
}
