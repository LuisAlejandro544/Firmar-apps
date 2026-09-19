package com.example.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de detalle de una Keystore individual.
 */
class KeystoreDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KeystoreRepository
    private val _keystore = MutableStateFlow<KeystoreEntity?>(null)
    val keystore: StateFlow<KeystoreEntity?> = _keystore.asStateFlow()

    private val _isStorePasswordRevealed = MutableStateFlow(false)
    val isStorePasswordRevealed: StateFlow<Boolean> = _isStorePasswordRevealed.asStateFlow()

    private val _isKeyPasswordRevealed = MutableStateFlow(false)
    val isKeyPasswordRevealed: StateFlow<Boolean> = _isKeyPasswordRevealed.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KeystoreRepository(database.keystoreDao())
    }

    fun loadKeystore(id: Long) {
        viewModelScope.launch {
            repository.getKeystoreById(id).collect { item ->
                _keystore.value = item
            }
        }
    }

    fun toggleStorePasswordVisibility() {
        _isStorePasswordRevealed.value = !_isStorePasswordRevealed.value
    }

    fun toggleKeyPasswordVisibility() {
        _isKeyPasswordRevealed.value = !_isKeyPasswordRevealed.value
    }

    fun deleteKeystore(onDeleted: () -> Unit) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            repository.deleteKeystore(current)
            onDeleted()
        }
    }
}
