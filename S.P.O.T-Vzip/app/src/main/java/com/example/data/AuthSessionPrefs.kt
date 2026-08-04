package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth_session")

/** Local, on-device login session — this app has no backend, so "logged in" just gates the UI. */
class AuthSessionPrefs(private val context: Context) {

    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
    private val emailKey = stringPreferencesKey("email")

    suspend fun isLoggedIn(): Boolean {
        return context.authDataStore.data.first()[isLoggedInKey] ?: false
    }

    suspend fun getEmail(): String? {
        return context.authDataStore.data.first()[emailKey]
    }

    suspend fun setLoggedIn(email: String) {
        context.authDataStore.edit { prefs ->
            prefs[isLoggedInKey] = true
            prefs[emailKey] = email
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs[isLoggedInKey] = false
            prefs.remove(emailKey)
        }
    }
}
