package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia en Room para almacenar los metadatos de las keystores generadas.
 *
 * Contiene información sobre el archivo generado en el almacenamiento local,
 * las credenciales de acceso (alias, contraseñas), el algoritmo y las huellas
 * digitales del certificado (SHA-1 y SHA-256) para facilitar la configuración de firmas.
 */
@Entity(tableName = "keystores")
data class KeystoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Nombre descriptivo o etiqueta dada por el usuario (ej. "Mi App Release") */
    val title: String,

    /** Nombre del archivo físico generado (ej. "mi_clave_release.jks") */
    val fileName: String,

    /** Ruta absoluta del archivo en el almacenamiento interno de la app */
    val filePath: String,

    /** Tamaño del archivo en bytes */
    val fileSizeBytes: Long,

    /** Alias asignado a la clave privada dentro del keystore */
    val alias: String,

    /** Contraseña para abrir y proteger el almacén de claves (Keystore) */
    val storePassword: String,

    /** Contraseña individual de la clave privada dentro del almacén */
    val keyPassword: String,

    /** Algoritmo y tamaño de clave criptográfica (ej. "RSA 2048 bits") */
    val keyAlgorithm: String,

    /** Periodo de validez en años desde su generación */
    val validityYears: Int,

    /** Nombre común o titular del certificado (CN) */
    val commonName: String,

    /** Organización o desarrollador (O) */
    val organization: String,

    /** Unidad organizativa (OU) */
    val organizationalUnit: String,

    /** Ciudad o Localidad (L) según estándar X.500 de Google / Android Studio */
    val city: String = "",

    /** Estado o Provincia (ST) según estándar X.500 de Google / Android Studio */
    val state: String = "",

    /** Código de país de 2 caracteres (C) */
    val countryCode: String,

    /** Huella digital SHA-256 del certificado (ej. para Firebase / Google Sign-In) */
    val sha256Fingerprint: String,

    /** Huella digital SHA-1 del certificado */
    val sha1Fingerprint: String,

    /** Fecha y hora de creación en milisegundos */
    val createdAt: Long = System.currentTimeMillis()
)
