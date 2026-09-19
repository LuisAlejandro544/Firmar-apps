package com.example.data.repository

import com.example.data.dao.KeystoreDao
import com.example.data.model.KeystoreEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repositorio que unifica las operaciones de base de datos con la gestión del sistema de archivos local.
 * Asegura que al eliminar una keystore se limpie tanto su registro en Room como el archivo físico.
 */
class KeystoreRepository(private val keystoreDao: KeystoreDao) {

    /**
     * Flujo reactivo con todas las keystores ordenadas por fecha de creación.
     */
    val allKeystores: Flow<List<KeystoreEntity>> = keystoreDao.getAllKeystores()

    /**
     * Obtiene una keystore por su identificador primario.
     */
    fun getKeystoreById(id: Long): Flow<KeystoreEntity?> = keystoreDao.getKeystoreById(id)

    /**
     * Guarda una nueva keystore en la base de datos local.
     */
    suspend fun insertKeystore(keystore: KeystoreEntity): Long = withContext(Dispatchers.IO) {
        keystoreDao.insertKeystore(keystore)
    }

    /**
     * Elimina el registro de la keystore en Room y su archivo físico correspondiente del disco.
     */
    suspend fun deleteKeystore(keystore: KeystoreEntity) = withContext(Dispatchers.IO) {
        // Eliminar archivo físico si existe
        try {
            val file = File(keystore.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Continuar incluso si el archivo ya no estaba en disco
        }
        keystoreDao.deleteKeystore(keystore)
    }
}
