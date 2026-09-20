package com.example.ui.importzip

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.ImportedKeystoreValidation
import com.example.crypto.ZipAnalysisResult
import com.example.crypto.ZipImportHelper
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI para el flujo de importación y restauración de paquetes ZIP.
 */
data class ZipImportUiState(
    val isAnalyzing: Boolean = false,
    val analysisResult: ZipAnalysisResult? = null,
    val titleInput: String = "",
    val aliasInput: String = "",
    val storePasswordInput: String = "",
    val keyPasswordInput: String = "",
    val isSamePassword: Boolean = true,
    val isStorePasswordVisible: Boolean = false,
    val isKeyPasswordVisible: Boolean = false,
    val isImporting: Boolean = false,
    val validationPreview: ImportedKeystoreValidation? = null,
    val importedKeystore: KeystoreEntity? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel que gestiona la máquina de estados del proceso de importación de archivos ZIP.
 */
class ZipImportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KeystoreRepository
    private val _uiState = MutableStateFlow(ZipImportUiState())
    val uiState: StateFlow<ZipImportUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KeystoreRepository(database.keystoreDao())
    }

    /**
     * Procesa el archivo ZIP seleccionado a través de SAF e inicializa los campos
     * con los valores extraídos o detectados por heurística.
     */
    fun onZipFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            // Limpiar extracción previa si existía
            _uiState.value.analysisResult?.tempExtractionDir?.let {
                ZipImportHelper.cleanTempDirectory(it)
            }

            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    errorMessage = null,
                    analysisResult = null,
                    importedKeystore = null,
                    validationPreview = null
                )
            }

            val analysisResult = ZipImportHelper.analyzeZipUri(context, uri)

            if (analysisResult.isSuccess) {
                val data = analysisResult.getOrThrow()
                val isSame = data.suggestedKeyPassword.isEmpty() || data.suggestedKeyPassword == data.suggestedStorePassword
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisResult = data,
                        titleInput = data.suggestedTitle,
                        aliasInput = data.suggestedAlias,
                        storePasswordInput = data.suggestedStorePassword,
                        keyPasswordInput = if (isSame) data.suggestedStorePassword else data.suggestedKeyPassword,
                        isSamePassword = isSame,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = analysisResult.exceptionOrNull()?.message ?: "Error al procesar el archivo ZIP."
                    )
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(titleInput = newTitle) }
    }

    fun onAliasChanged(newAlias: String) {
        _uiState.update { it.copy(aliasInput = newAlias) }
    }

    fun onStorePasswordChanged(newPassword: String) {
        _uiState.update { current ->
            current.copy(
                storePasswordInput = newPassword,
                keyPasswordInput = if (current.isSamePassword) newPassword else current.keyPasswordInput
            )
        }
    }

    fun onKeyPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(keyPasswordInput = newPassword) }
    }

    fun onSamePasswordToggle(checked: Boolean) {
        _uiState.update { current ->
            current.copy(
                isSamePassword = checked,
                keyPasswordInput = if (checked) current.storePasswordInput else current.keyPasswordInput
            )
        }
    }

    fun toggleStorePasswordVisibility() {
        _uiState.update { it.copy(isStorePasswordVisible = !it.isStorePasswordVisible) }
    }

    fun toggleKeyPasswordVisibility() {
        _uiState.update { it.copy(isKeyPasswordVisible = !it.isKeyPasswordVisible) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Valida la firma criptográfica y credenciales contra el almacén y almacena la clave en Room.
     */
    fun commitImport(context: Context) {
        val state = _uiState.value
        val analysis = state.analysisResult ?: return

        if (state.storePasswordInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debes ingresar la contraseña del almacén (Store Password).") }
            return
        }

        val effectiveKeyPassword = if (state.isSamePassword) state.storePasswordInput else state.keyPasswordInput
        if (effectiveKeyPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debes ingresar la contraseña de la clave privada (Key Password).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }

            // 1. Validar e inspeccionar el almacén criptográficamente
            val validationResult = ZipImportHelper.validateAndInspectKeystore(
                jksFile = analysis.jksTempFile,
                storePassword = state.storePasswordInput,
                alias = state.aliasInput,
                keyPassword = effectiveKeyPassword
            )

            if (validationResult.isFailure) {
                val error = validationResult.exceptionOrNull()?.message ?: "Fallo de validación de contraseñas o alias."
                _uiState.update { it.copy(isImporting = false, errorMessage = error) }
                return@launch
            }

            val validation = validationResult.getOrThrow()

            // 2. Persistir en almacenamiento interno y base de datos con AES-256-GCM
            val commitResult = ZipImportHelper.commitImport(
                context = context,
                repository = repository,
                analysis = analysis,
                title = state.titleInput.ifEmpty { "Keystore ${validation.validatedAlias}" },
                storePassword = state.storePasswordInput,
                keyPassword = effectiveKeyPassword,
                validation = validation
            )

            if (commitResult.isSuccess) {
                val savedEntity = commitResult.getOrThrow()
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        validationPreview = validation,
                        importedKeystore = savedEntity,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = commitResult.exceptionOrNull()?.message ?: "Error al guardar la keystore importada."
                    )
                }
            }
        }
    }

    /**
     * Limpia recursos al destruir el ViewModel.
     */
    override fun onCleared() {
        super.onCleared()
        _uiState.value.analysisResult?.tempExtractionDir?.let {
            ZipImportHelper.cleanTempDirectory(it)
        }
    }
}
