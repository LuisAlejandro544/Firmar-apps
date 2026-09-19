package com.example.ui.generator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.KeystoreGenerator
import com.example.crypto.KeystoreParams
import com.example.crypto.PasswordLengthOption
import com.example.crypto.PasswordSecurityEngine
import com.example.crypto.PasswordStrength
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI del formulario de generación de keystore.
 * Soporta selección de formatos (.jks y .keystore), auto-detección de extensión,
 * generador integrado de contraseñas ultra seguras con 3 niveles de longitud (16, 24, 32 caracteres)
 * y rango de validez granular desde 1 día hasta 100 años.
 */
data class GeneratorUiState(
    val title: String = "Clave Release",
    val fileBaseName: String = "release_key",
    val selectedExtension: String = ".jks", // ".jks" o ".keystore"
    val alias: String = "key0",
    val storePassword: String = "",
    val keyPassword: String = "",
    val useSamePassword: Boolean = true,
    val validityDays: Int = 25 * 365, // 9,125 días (25 años por defecto en Android)
    val keySize: Int = 2048,
    val commonName: String = "Desarrollador Android",
    val organization: String = "Mobile Apps",
    val organizationalUnit: String = "Desarrollo",
    val countryCode: String = "ES",
    val isStorePasswordVisible: Boolean = false,
    val isKeyPasswordVisible: Boolean = false,
    val isAdvancedSectionExpanded: Boolean = false,
    val isGenerating: Boolean = false,
    val createdKeystore: KeystoreEntity? = null,
    val errorMessage: String? = null,
    // Estados del diálogo y selector de contraseñas ultra seguras
    val isPasswordGeneratorDialogOpen: Boolean = false,
    val passwordGeneratorTarget: String = "store", // "store" o "key"
    val selectedPasswordLength: Int = PasswordLengthOption.VERY_HIGH.length, // 24 caracteres por defecto
    val previewGeneratedPassword: String = ""
) {
    /**
     * Nombre final del archivo combinado de forma limpia con la extensión seleccionada.
     * Si el usuario escribió la extensión en el nombre base, se previene duplicación.
     */
    val fullFileName: String
        get() {
            val cleanBase = fileBaseName.trim()
                .removeSuffix(".jks")
                .removeSuffix(".keystore")
                .ifEmpty { "release_key" }
            return "$cleanBase$selectedExtension"
        }

    /** Propiedad de compatibilidad que retorna el nombre completo del archivo */
    val fileName: String
        get() = fullFileName

    /** Validez en años redondeada hacia abajo (o 1 si es menor a un año) */
    val validityYears: Int
        get() = if (validityDays >= 365) validityDays / 365 else 1
}

/**
 * ViewModel que gestiona la lógica del formulario de creación y coordina
 * la generación criptográfica y guardado en la base de datos Room.
 */
class GeneratorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KeystoreRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KeystoreRepository(database.keystoreDao())
    }

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    /**
     * Actualiza el nombre base del archivo y detecta de forma inteligente si el usuario
     * escribió explícitamente '.jks' o '.keystore' para seleccionar el formato y limpiar el nombre.
     */
    fun onFileBaseNameChange(value: String) = _uiState.update { current ->
        val trimmed = value.trim()
        val detectedExtension = when {
            trimmed.endsWith(".keystore", ignoreCase = true) -> ".keystore"
            trimmed.endsWith(".jks", ignoreCase = true) -> ".jks"
            else -> null
        }
        val cleanName = if (detectedExtension != null) {
            trimmed.substring(0, trimmed.length - detectedExtension.length)
        } else {
            value
        }
        current.copy(
            fileBaseName = cleanName,
            selectedExtension = detectedExtension ?: current.selectedExtension
        )
    }

    /** Compatibilidad para cuando se invoca onFileNameChange */
    fun onFileNameChange(value: String) = onFileBaseNameChange(value)

    /** Cambia el formato de archivo entre .jks y .keystore */
    fun onFileExtensionChange(extension: String) = _uiState.update {
        if (extension == ".jks" || extension == ".keystore") {
            it.copy(selectedExtension = extension)
        } else {
            it
        }
    }

    fun onAliasChange(value: String) = _uiState.update { it.copy(alias = value) }

    fun onStorePasswordChange(value: String) = _uiState.update {
        if (it.useSamePassword) {
            it.copy(storePassword = value, keyPassword = value)
        } else {
            it.copy(storePassword = value)
        }
    }

    fun onKeyPasswordChange(value: String) = _uiState.update { it.copy(keyPassword = value) }

    fun onUseSamePasswordToggle(checked: Boolean) = _uiState.update {
        it.copy(
            useSamePassword = checked,
            keyPassword = if (checked) it.storePassword else it.keyPassword
        )
    }

    /** Actualiza los días de validez con rango restringido de 1 día a 36500 días (100 años) */
    fun onValidityDaysChange(days: Int) = _uiState.update {
        it.copy(validityDays = days.coerceIn(1, 36500))
    }

    /** Actualiza los días de validez a partir de años */
    fun onValidityYearsChange(years: Int) = onValidityDaysChange(years * 365)

    fun onKeySizeChange(size: Int) = _uiState.update { it.copy(keySize = size) }
    fun onCommonNameChange(value: String) = _uiState.update { it.copy(commonName = value) }
    fun onOrganizationChange(value: String) = _uiState.update { it.copy(organization = value) }
    fun onOrganizationalUnitChange(value: String) = _uiState.update { it.copy(organizationalUnit = value) }
    fun onCountryCodeChange(value: String) = _uiState.update { it.copy(countryCode = value.take(2).uppercase()) }

    fun toggleStorePasswordVisibility() = _uiState.update { it.copy(isStorePasswordVisible = !it.isStorePasswordVisible) }
    fun toggleKeyPasswordVisibility() = _uiState.update { it.copy(isKeyPasswordVisible = !it.isKeyPasswordVisible) }
    fun toggleAdvancedSection() = _uiState.update { it.copy(isAdvancedSectionExpanded = !it.isAdvancedSectionExpanded) }

    /**
     * Abre el diálogo interactivo para generar contraseñas ultra seguras con 3 opciones de longitud.
     * @param target "store" para la contraseña del keystore, o "key" para la contraseña individual de la clave.
     */
    fun openPasswordGeneratorDialog(target: String = "store") {
        val currentLength = _uiState.value.selectedPasswordLength
        val generated = PasswordSecurityEngine.generateSecurePassword(currentLength)
        _uiState.update {
            it.copy(
                isPasswordGeneratorDialogOpen = true,
                passwordGeneratorTarget = target,
                previewGeneratedPassword = generated
            )
        }
    }

    /** Cierra el diálogo del generador de contraseñas */
    fun closePasswordGeneratorDialog() = _uiState.update { it.copy(isPasswordGeneratorDialogOpen = false) }

    /** Cambia la opción de longitud (16, 24 o 32 caracteres) y regenera inmediatamente */
    fun onSelectPasswordLength(length: Int) {
        val generated = PasswordSecurityEngine.generateSecurePassword(length)
        _uiState.update {
            it.copy(
                selectedPasswordLength = length,
                previewGeneratedPassword = generated
            )
        }
    }

    /** Regenera otra contraseña de la misma longitud seleccionada */
    fun regeneratePasswordPreview() {
        val currentLength = _uiState.value.selectedPasswordLength
        val generated = PasswordSecurityEngine.generateSecurePassword(currentLength)
        _uiState.update { it.copy(previewGeneratedPassword = generated) }
    }

    /**
     * Aplica la contraseña generada al campo correspondiente (y al alias si 'useSamePassword' está activo).
     */
    fun applyGeneratedPassword() {
        val password = _uiState.value.previewGeneratedPassword
        val target = _uiState.value.passwordGeneratorTarget
        _uiState.update { current ->
            if (target == "store") {
                if (current.useSamePassword) {
                    current.copy(
                        storePassword = password,
                        keyPassword = password,
                        isStorePasswordVisible = true,
                        isPasswordGeneratorDialogOpen = false
                    )
                } else {
                    current.copy(
                        storePassword = password,
                        isStorePasswordVisible = true,
                        isPasswordGeneratorDialogOpen = false
                    )
                }
            } else {
                current.copy(
                    keyPassword = password,
                    isKeyPasswordVisible = true,
                    isPasswordGeneratorDialogOpen = false
                )
            }
        }
    }

    /**
     * Generación directa y rápida de contraseña ultra segura con longitud especificada.
     */
    fun generateQuickSecurePassword(length: Int = 24, target: String = "store") {
        val password = PasswordSecurityEngine.generateSecurePassword(length)
        _uiState.update { current ->
            if (target == "store") {
                if (current.useSamePassword) {
                    current.copy(
                        storePassword = password,
                        keyPassword = password,
                        isStorePasswordVisible = true
                    )
                } else {
                    current.copy(
                        storePassword = password,
                        isStorePasswordVisible = true
                    )
                }
            } else {
                current.copy(
                    keyPassword = password,
                    isKeyPasswordVisible = true
                )
            }
        }
    }

    fun dismissSuccessDialog() = _uiState.update { it.copy(createdKeystore = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    /**
     * Valida los datos y ejecuta el proceso de generación criptográfica en segundo plano.
     */
    fun generateKeystore() {
        val state = _uiState.value

        // Validaciones básicas
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa un título descriptivo") }
            return
        }
        if (state.fileBaseName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa un nombre para el archivo") }
            return
        }
        if (state.alias.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El alias de la clave no puede estar vacío") }
            return
        }
        if (state.storePassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña del almacén debe tener al menos 6 caracteres") }
            return
        }
        val finalKeyPassword = if (state.useSamePassword) state.storePassword else state.keyPassword
        if (finalKeyPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña de la clave debe tener al menos 6 caracteres") }
            return
        }

        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

        viewModelScope.launch {
            val params = KeystoreParams(
                title = state.title,
                fileName = state.fullFileName,
                alias = state.alias,
                storePassword = state.storePassword,
                keyPassword = finalKeyPassword,
                keySize = state.keySize,
                validityYears = state.validityYears,
                validityDays = state.validityDays,
                commonName = state.commonName,
                organization = state.organization,
                organizationalUnit = state.organizationalUnit,
                countryCode = state.countryCode
            )

            val result = KeystoreGenerator.generateKeystore(getApplication(), params)

            result.onSuccess { entity ->
                // Guardar en Room
                val insertedId = repository.insertKeystore(entity)
                val savedEntity = entity.copy(id = insertedId)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        createdKeystore = savedEntity
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Error al generar Keystore: ${exception.localizedMessage ?: "Error desconocido"}"
                    )
                }
            }
        }
    }

    /**
     * Rellena el formulario con valores predeterminados rápidos para agilizar pruebas.
     */
    fun applyQuickPreset() {
        val timestamp = System.currentTimeMillis() % 10000
        _uiState.update {
            it.copy(
                title = "App Release $timestamp",
                fileBaseName = "release_$timestamp",
                selectedExtension = if (timestamp % 2L == 0L) ".jks" else ".keystore",
                alias = "key$timestamp",
                storePassword = "password$timestamp",
                keyPassword = "password$timestamp",
                useSamePassword = true,
                validityDays = 25 * 365,
                keySize = 2048,
                commonName = "Android Dev $timestamp",
                countryCode = "ES"
            )
        }
    }
}
