package com.filotheque.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.filotheque.app.data.local.entity.ProjectEntity
import com.filotheque.app.data.local.entity.ProjectWithThreads
import com.filotheque.app.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {

    val projects: StateFlow<List<ProjectWithThreads>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    fun selectProject(projectId: Long?) {
        _selectedProjectId.value = projectId
    }

    fun createProject(name: String, patternSource: String, threadIds: List<Long>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createProject(name, patternSource, threadIds)
        }
    }

    fun addThreadToProject(projectId: Long, threadId: Long) {
        viewModelScope.launch {
            repository.addThreadToProject(projectId, threadId)
        }
    }

    fun syncProjectThreads(projectId: Long, currentIds: Set<Long>, targetIds: Set<Long>) {
        viewModelScope.launch {
            (targetIds - currentIds).forEach { id ->
                repository.addThreadToProject(projectId, id)
            }
            (currentIds - targetIds).forEach { id ->
                repository.removeThreadFromProject(projectId, id)
            }
        }
    }

    fun removeThreadFromProject(projectId: Long, threadId: Long) {
        viewModelScope.launch {
            repository.removeThreadFromProject(projectId, threadId)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_selectedProjectId.value == project.id) _selectedProjectId.value = null
        }
    }

    fun addMissingToShoppingList(project: ProjectWithThreads, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.addMissingThreadsToShoppingList(project)
            onResult(count)
        }
    }

    class Factory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectViewModel(repository) as T
        }
    }
}
