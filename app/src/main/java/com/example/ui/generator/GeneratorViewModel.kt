package com.example.ui.generator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.KeystoreGenerator
import com.example.crypto.KeystoreParams
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
 */
data class GeneratorUiState(
    val title: String = "Clave Release",
    val fileName: String = "release_key.jks",
    val alias: String = "key0",
    val storePassword: String = "",
    val keyPassword: String = "",
    val useSamePassword: Boolean = true,
    val validityYears: Int = 25,
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
    val errorMessage: String? = null
)

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
    fun onFileNameChange(value: String) = _uiState.update { it.copy(fileName = value) }
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

    fun onValidityYearsChange(years: Int) = _uiState.update { it.copy(validityYears = years) }
    fun onKeySizeChange(size: Int) = _uiState.update { it.copy(keySize = size) }
    fun onCommonNameChange(value: String) = _uiState.update { it.copy(commonName = value) }
    fun onOrganizationChange(value: String) = _uiState.update { it.copy(organization = value) }
    fun onOrganizationalUnitChange(value: String) = _uiState.update { it.copy(organizationalUnit = value) }
    fun onCountryCodeChange(value: String) = _uiState.update { it.copy(countryCode = value.take(2).uppercase()) }

    fun toggleStorePasswordVisibility() = _uiState.update { it.copy(isStorePasswordVisible = !it.isStorePasswordVisible) }
    fun toggleKeyPasswordVisibility() = _uiState.update { it.copy(isKeyPasswordVisible = !it.isKeyPasswordVisible) }
    fun toggleAdvancedSection() = _uiState.update { it.copy(isAdvancedSectionExpanded = !it.isAdvancedSectionExpanded) }

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
        if (state.fileName.isBlank()) {
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
                fileName = state.fileName,
                alias = state.alias,
                storePassword = state.storePassword,
                keyPassword = finalKeyPassword,
                keySize = state.keySize,
                validityYears = state.validityYears,
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
                fileName = "release_$timestamp.jks",
                alias = "key$timestamp",
                storePassword = "password$timestamp",
                keyPassword = "password$timestamp",
                useSamePassword = true,
                validityYears = 25,
                keySize = 2048,
                commonName = "Android Dev $timestamp",
                countryCode = "ES"
            )
        }
    }
}
