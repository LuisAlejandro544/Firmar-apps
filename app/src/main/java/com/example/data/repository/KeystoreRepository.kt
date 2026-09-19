package com.example.data.repository

import com.example.crypto.SecureCredentialsCipher
import com.example.data.dao.KeystoreDao
import com.example.data.model.KeystoreEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repositorio que unifica las operaciones de base de datos con la gestión del sistema de archivos local
 * y el cifrado de credenciales en reposo con AES-256-GCM respaldado por hardware.
 * Asegura que al eliminar una keystore se limpie tanto su registro en Room como el archivo físico,
 * y que las contraseñas persistan siempre cifradas en la base de datos SQLite interna.
 */
class KeystoreRepository(private val keystoreDao: KeystoreDao) {

    /**
     * Flujo reactivo con todas las keystores ordenadas por fecha de creación,
     * con sus contraseñas descifradas transparentemente para la capa de presentación.
     */
    val allKeystores: Flow<List<KeystoreEntity>> = keystoreDao.getAllKeystores().map { list ->
        list.map { entity ->
            entity.copy(
                storePassword = SecureCredentialsCipher.decrypt(entity.storePassword),
                keyPassword = SecureCredentialsCipher.decrypt(entity.keyPassword)
            )
        }
    }

    /**
     * Obtiene una keystore por su identificador primario con sus credenciales descifradas.
     */
    fun getKeystoreById(id: Long): Flow<KeystoreEntity?> = keystoreDao.getKeystoreById(id).map { entity ->
        entity?.copy(
            storePassword = SecureCredentialsCipher.decrypt(entity.storePassword),
            keyPassword = SecureCredentialsCipher.decrypt(entity.keyPassword)
        )
    }

    /**
     * Guarda una nueva keystore en la base de datos local, cifrando previamente las contraseñas
     * con AES-256-GCM mediante Android KeyStore Provider.
     */
    suspend fun insertKeystore(keystore: KeystoreEntity): Long = withContext(Dispatchers.IO) {
        val encryptedEntity = keystore.copy(
            storePassword = SecureCredentialsCipher.encrypt(keystore.storePassword),
            keyPassword = SecureCredentialsCipher.encrypt(keystore.keyPassword)
        )
        keystoreDao.insertKeystore(encryptedEntity)
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
