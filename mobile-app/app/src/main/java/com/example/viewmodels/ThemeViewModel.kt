package com.example.viewmodels

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "theme_preferences")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
    private val dataStore = application.dataStore

    private val initialDarkMode: Boolean = try {
        runBlocking {
            dataStore.data.first()[IS_DARK_MODE_KEY] ?: false
        }
    } catch (e: Exception) {
        false
    }

    private val _isDarkMode = MutableStateFlow<Boolean?>(initialDarkMode)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collect { preferences ->
                    _isDarkMode.value = preferences[IS_DARK_MODE_KEY] ?: false
                }
        }
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[IS_DARK_MODE_KEY] = isDark
            }
        }
    }
}
