package com.example.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI para la pantalla del listado de keystores guardadas.
 */
data class KeystoreListUiState(
    val keystores: List<KeystoreEntity> = emptyList(),
    val searchQuery: String = "",
    val keystoreToDelete: KeystoreEntity? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel que proporciona la lista reactiva de keystores y permite filtrado y eliminación.
 */
class KeystoreListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KeystoreRepository
    private val _searchQuery = MutableStateFlow("")
    private val _keystoreToDelete = MutableStateFlow<KeystoreEntity?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KeystoreRepository(database.keystoreDao())
    }

    val uiState: StateFlow<KeystoreListUiState> = combine(
        repository.allKeystores,
        _searchQuery,
        _keystoreToDelete
    ) { allItems, query, toDelete ->
        val filtered = if (query.isBlank()) {
            allItems
        } else {
            allItems.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.fileName.contains(query, ignoreCase = true) ||
                item.alias.contains(query, ignoreCase = true)
            }
        }
        KeystoreListUiState(
            keystores = filtered,
            searchQuery = query,
            keystoreToDelete = toDelete,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KeystoreListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun requestDeleteKeystore(keystore: KeystoreEntity) {
        _keystoreToDelete.value = keystore
    }

    fun cancelDeleteKeystore() {
        _keystoreToDelete.value = null
    }

    fun confirmDeleteKeystore() {
        val keystore = _keystoreToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteKeystore(keystore)
            _keystoreToDelete.value = null
        }
    }
}
