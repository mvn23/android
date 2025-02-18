package io.homeassistant.companion.android.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.data.SimplifiedEntity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private lateinit var homePresenter: HomePresenter

    // TODO: This is bad, do this instead: https://stackoverflow.com/questions/46283981/android-viewmodel-additional-arguments
    fun init(homePresenter: HomePresenter) {
        this.homePresenter = homePresenter
        loadEntities()
    }

    // entities
    var entities = mutableStateMapOf<String, Entity<*>>()
        private set
    var favoriteEntityIds = mutableStateListOf<String>()
        private set
    var shortcutEntities = mutableStateListOf<SimplifiedEntity>()
        private set
    var scenes = mutableStateListOf<Entity<*>>()
        private set
    var scripts = mutableStateListOf<Entity<*>>()
        private set
    var lights = mutableStateListOf<Entity<*>>()
        private set
    var locks = mutableStateListOf<Entity<*>>()
        private set
    var inputBooleans = mutableStateListOf<Entity<*>>()
        private set
    var switches = mutableStateListOf<Entity<*>>()
        private set

    // Content of EntityListView
    var entityLists = mutableStateMapOf<Int, List<Entity<*>>>()

    // settings
    var isHapticEnabled = mutableStateOf(false)
        private set
    var isToastEnabled = mutableStateOf(false)
        private set

    private fun loadEntities() {
        viewModelScope.launch {
            if (!homePresenter.isConnected()) {
                return@launch
            }
            favoriteEntityIds.addAll(homePresenter.getWearHomeFavorites())
            shortcutEntities.addAll(homePresenter.getTileShortcuts())
            isHapticEnabled.value = homePresenter.getWearHapticFeedback()
            isToastEnabled.value = homePresenter.getWearToastConfirmation()
            homePresenter.getEntities().forEach {
                entities[it.entityId] = it
            }
            updateEntityDomains()
            homePresenter.getEntityUpdates().collect {
                entities[it.entityId] = it
                updateEntityDomains()
            }
        }
    }

    fun updateEntityDomains() {
        val entitiesList = entities.values.toList().sortedBy { it.entityId }
        scenes.clear()
        scenes.addAll(entitiesList.filter { it.entityId.split(".")[0] == "scene" })
        scripts.clear()
        scripts.addAll(entitiesList.filter { it.entityId.split(".")[0] == "script" })
        lights.clear()
        lights.addAll(entitiesList.filter { it.entityId.split(".")[0] == "light" })
        locks.clear()
        locks.addAll(entitiesList.filter { it.entityId.split(".")[0] == "lock" })
        inputBooleans.clear()
        inputBooleans.addAll(entitiesList.filter { it.entityId.split(".")[0] == "input_boolean" })
        switches.clear()
        switches.addAll(entitiesList.filter { it.entityId.split(".")[0] == "switch" })
    }

    fun toggleEntity(entityId: String, state: String) {
        viewModelScope.launch {
            homePresenter.onEntityClicked(entityId, state)
        }
    }

    fun addFavorite(entityId: String) {

        viewModelScope.launch {
            favoriteEntityIds.add(entityId)
            homePresenter.setWearHomeFavorites(favoriteEntityIds)
        }
    }

    fun removeFavorite(entity: String) {

        viewModelScope.launch {
            favoriteEntityIds.remove(entity)
            homePresenter.setWearHomeFavorites(favoriteEntityIds)
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            favoriteEntityIds.clear()
            homePresenter.setWearHomeFavorites(favoriteEntityIds)
        }
    }

    // TODO: Remove the below as we should save favorites to the DB so we can use a proper flow like above
    fun updateFavorites() {
        viewModelScope.launch {
            favoriteEntityIds.clear()
            favoriteEntityIds.addAll(homePresenter.getWearHomeFavorites())
        }
    }

    fun setTileShortcut(index: Int, entity: SimplifiedEntity) {
        viewModelScope.launch {
            if (index < shortcutEntities.size) {
                shortcutEntities[index] = entity
            } else {
                shortcutEntities.add(entity)
            }
            homePresenter.setTileShortcuts(shortcutEntities)
        }
    }

    fun clearTileShortcut(index: Int) {
        viewModelScope.launch {
            if (index < shortcutEntities.size) {
                shortcutEntities.removeAt(index)
                homePresenter.setTileShortcuts(shortcutEntities)
            }
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            homePresenter.setWearHapticFeedback(enabled)
            isHapticEnabled.value = enabled
        }
    }

    fun setToastEnabled(enabled: Boolean) {
        viewModelScope.launch {
            homePresenter.setWearToastConfirmation(enabled)
            isToastEnabled.value = enabled
        }
    }

    fun logout() {
        homePresenter.onLogoutClicked()
    }
}
