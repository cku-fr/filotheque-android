package com.filotheque.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.data.repository.ThreadRepository
import com.filotheque.app.domain.color.ColorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ViewMode { GRID, LIST }
enum class SortMode { BY_SHADE, BY_CODE }
enum class BrandFilter(val label: String) {
    ALL("Toutes"),
    DMC("DMC"),
    ANCHOR("Anchor"),
    CUSTOM("Tiers")
}
enum class ChartStatusFilter(val label: String) {
    ALL("Tous"),
    OWNED("En stock"),
    MISSING("Manquants"),
    SHOPPING("À acheter")
}

data class ThreadFilterState(
    val searchQuery: String = "",
    val brandFilter: BrandFilter = BrandFilter.ALL,
    val chartBrand: BrandFilter = BrandFilter.DMC,
    val chartStatus: ChartStatusFilter = ChartStatusFilter.ALL,
    val colorFamily: String = "Toutes",
    val viewMode: ViewMode = ViewMode.GRID,
    val sortMode: SortMode = SortMode.BY_SHADE
)

class ThreadViewModel(private val repository: ThreadRepository) : ViewModel() {

    private val _filter = MutableStateFlow(ThreadFilterState())
    val filter: StateFlow<ThreadFilterState> = _filter.asStateFlow()

    private val _selectedThread = MutableStateFlow<ThreadEntity?>(null)
    val selectedThread: StateFlow<ThreadEntity?> = _selectedThread.asStateFlow()

    private val _equivalents = MutableStateFlow<List<ThreadEntity>>(emptyList())
    val equivalents: StateFlow<List<ThreadEntity>> = _equivalents.asStateFlow()

    private val _closestDmcSuggestion = MutableStateFlow<ThreadEntity?>(null)
    val closestDmcSuggestion: StateFlow<ThreadEntity?> = _closestDmcSuggestion.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val allThreads: StateFlow<List<ThreadEntity>> = repository.observeAllThreads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryThreads: StateFlow<List<ThreadEntity>> = combine(
        repository.observeOwnedThreads(),
        _filter
    ) { owned, f ->
        applyFiltersAndSort(
            threads = owned,
            query = f.searchQuery,
            brandFilter = f.brandFilter,
            colorFamily = f.colorFamily,
            sortMode = f.sortMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chartThreads: StateFlow<List<ThreadEntity>> = combine(
        allThreads,
        _filter
    ) { all, f ->
        val statusFiltered = when (f.chartStatus) {
            ChartStatusFilter.ALL -> all
            ChartStatusFilter.OWNED -> all.filter { it.quantity > 0 }
            ChartStatusFilter.MISSING -> all.filter { it.quantity == 0 }
            ChartStatusFilter.SHOPPING -> all.filter { it.isOnShoppingList }
        }
        applyFiltersAndSort(
            threads = statusFiltered,
            query = f.searchQuery,
            brandFilter = f.chartBrand,
            colorFamily = f.colorFamily,
            sortMode = f.sortMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingList: StateFlow<List<ThreadEntity>> = repository.observeShoppingList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applyFiltersAndSort(
        threads: List<ThreadEntity>,
        query: String,
        brandFilter: BrandFilter,
        colorFamily: String,
        sortMode: SortMode
    ): List<ThreadEntity> {
        val q = query.trim().lowercase()
        val filtered = threads.filter { t ->
            val matchQuery = q.isEmpty() ||
                t.code.lowercase().contains(q) ||
                t.name.lowercase().contains(q) ||
                t.brandName.lowercase().contains(q)
            val matchBrand = when (brandFilter) {
                BrandFilter.ALL -> true
                BrandFilter.DMC -> t.brandId == "DMC"
                BrandFilter.ANCHOR -> t.brandId == "ANCHOR"
                BrandFilter.CUSTOM -> t.brandId != "DMC" && t.brandId != "ANCHOR"
            }
            val matchFamily = colorFamily == "Toutes" || t.colorFamily == colorFamily
            matchQuery && matchBrand && matchFamily
        }
        return when (sortMode) {
            SortMode.BY_SHADE -> filtered.sortedBy { ColorUtils.colorSortKey(it.hexColor) }
            SortMode.BY_CODE -> filtered.sortedWith(compareBy({ it.brandName }, { it.code.padStart(6, '0') }))
        }
    }

    fun setSearchQuery(q: String) { _filter.value = _filter.value.copy(searchQuery = q) }
    fun setBrandFilter(b: BrandFilter) { _filter.value = _filter.value.copy(brandFilter = b) }
    fun setChartBrand(b: BrandFilter) { _filter.value = _filter.value.copy(chartBrand = b) }
    fun setChartStatus(s: ChartStatusFilter) { _filter.value = _filter.value.copy(chartStatus = s) }
    fun setColorFamily(f: String) { _filter.value = _filter.value.copy(colorFamily = f) }
    fun toggleViewMode() {
        val next = if (_filter.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        _filter.value = _filter.value.copy(viewMode = next)
    }
    fun toggleSortMode() {
        val next = if (_filter.value.sortMode == SortMode.BY_SHADE) SortMode.BY_CODE else SortMode.BY_SHADE
        _filter.value = _filter.value.copy(sortMode = next)
    }

    fun selectThread(thread: ThreadEntity?) {
        _selectedThread.value = thread
        if (thread != null) {
            viewModelScope.launch {
                _equivalents.value = repository.getEquivalents(thread)
            }
        } else {
            _equivalents.value = emptyList()
        }
    }

    fun adjustQuantity(threadId: Long, delta: Int) {
        viewModelScope.launch {
            repository.adjustQuantity(threadId, delta)
            _selectedThread.value?.let { current ->
                if (current.id == threadId) {
                    _selectedThread.value = current.copy(quantity = (current.quantity + delta).coerceAtLeast(0))
                }
            }
        }
    }

    fun toggleShoppingList(thread: ThreadEntity) {
        viewModelScope.launch {
            val next = !thread.isOnShoppingList
            repository.setShoppingList(thread.id, next)
            _selectedThread.value = thread.copy(isOnShoppingList = next)
        }
    }

    fun markPurchased(thread: ThreadEntity) {
        viewModelScope.launch {
            repository.markPurchased(thread.id)
            _snackbarMessage.value = "${thread.brandName} ${thread.code} ajouté au stock !"
        }
    }

    fun removeFromShoppingList(thread: ThreadEntity) {
        viewModelScope.launch {
            repository.setShoppingList(thread.id, false)
            _snackbarMessage.value = "${thread.brandName} ${thread.code} retiré des courses"
        }
    }

    fun updateNote(threadId: Long, note: String) {
        viewModelScope.launch {
            repository.updateNote(threadId, note)
            _selectedThread.value = _selectedThread.value?.copy(note = note)
        }
    }

    fun addCustomThread(brand: String, code: String, name: String, hex: String) {
        viewModelScope.launch {
            repository.addCustomThread(brand, code, name, hex, initialQuantity = 1)
            _snackbarMessage.value = "$brand $code ajouté à l'inventaire !"
        }
    }

    fun updateCustomThread(
        thread: ThreadEntity,
        brand: String,
        code: String,
        name: String,
        hex: String
    ) {
        viewModelScope.launch {
            val updated = repository.updateCustomThread(thread, brand, code, name, hex)
            if (_selectedThread.value?.id == thread.id) {
                _selectedThread.value = updated
            }
            _snackbarMessage.value = "${updated.brandName} ${updated.code} mis à jour"
        }
    }

    fun deleteCustomThread(thread: ThreadEntity) {
        viewModelScope.launch {
            repository.deleteCustomThread(thread.id)
            if (_selectedThread.value?.id == thread.id) {
                _selectedThread.value = null
            }
            _snackbarMessage.value = "${thread.brandName} ${thread.code} supprimé du nuancier"
        }
    }

    fun computeClosestDmc(hex: String) {
        viewModelScope.launch {
            _closestDmcSuggestion.value = repository.findClosestDmcForHex(hex)
        }
    }

    fun batchAddByCodes(brandId: String, rawCodes: String) {
        viewModelScope.launch {
            val (found, notFound) = repository.batchIncrementByCodes(brandId, rawCodes)
            val msg = buildString {
                append("${found.size} fil(s) ajouté(s) au stock")
                if (notFound.isNotEmpty()) {
                    append(" · Introuvable(s) : ${notFound.joinToString(", ")}")
                }
            }
            _snackbarMessage.value = msg
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            try {
                val count = repository.importBackupJson(rawJson)
                _snackbarMessage.value = "Sauvegarde restaurée ($count fils mis à jour)"
            } catch (e: Exception) {
                _snackbarMessage.value = "Erreur d'import : fichier invalide"
            }
        }
    }

    fun clearSnackbar() { _snackbarMessage.value = null }

    class Factory(private val repository: ThreadRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThreadViewModel(repository) as T
        }
    }
}
