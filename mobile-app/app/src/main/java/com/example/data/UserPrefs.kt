package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

data class CachedUser(val id: String, val name: String, val phone: String, val ward: String?)

/** Persists the locally-created account across app restarts. */
class UserPrefs(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val PHONE = stringPreferencesKey("phone")
        val WARD = stringPreferencesKey("ward")
    }

    val cachedUserFlow: Flow<CachedUser?> = context.userDataStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID] ?: return@map null
        CachedUser(
            id = id,
            name = prefs[Keys.NAME].orEmpty(),
            phone = prefs[Keys.PHONE].orEmpty(),
            ward = prefs[Keys.WARD]
        )
    }

    suspend fun getUserId(): String? = context.userDataStore.data.map { it[Keys.USER_ID] }.first()

    suspend fun save(id: String, name: String, phone: String, ward: String?) {
        context.userDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = id
            prefs[Keys.NAME] = name
            prefs[Keys.PHONE] = phone
            if (ward != null) prefs[Keys.WARD] = ward else prefs.remove(Keys.WARD)
        }
    }

    suspend fun clear() {
        context.userDataStore.edit { it.clear() }
    }
}
