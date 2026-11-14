package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserDataStore: DataStore<Preferences> by preferencesDataStore(name = "vectra_prefs")

data class BrowserSettings(
    val adBlockEnabled: Boolean = true,
    val stripTrackingParams: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val searchEngineId: Int = 0,
    val customBlockedDomains: Set<String> = emptySet()
)

class BrowserPreferences(private val context: Context) {

    private object Keys {
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val STRIP_TRACKING_PARAMS = booleanPreferencesKey("strip_tracking_params")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val SEARCH_ENGINE_ID = intPreferencesKey("search_engine_id")
        val CUSTOM_BLOCKED_DOMAINS = stringSetPreferencesKey("custom_blocked_domains")
    }

    val settingsFlow: Flow<BrowserSettings> = context.browserDataStore.data.map { prefs ->
        BrowserSettings(
            adBlockEnabled = prefs[Keys.AD_BLOCK_ENABLED] ?: true,
            stripTrackingParams = prefs[Keys.STRIP_TRACKING_PARAMS] ?: true,
            javascriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
            searchEngineId = prefs[Keys.SEARCH_ENGINE_ID] ?: 0,
            customBlockedDomains = prefs[Keys.CUSTOM_BLOCKED_DOMAINS] ?: emptySet()
        )
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun setStripTrackingParams(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.STRIP_TRACKING_PARAMS] = enabled }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun setSearchEngineId(engineId: Int) {
        context.browserDataStore.edit { it[Keys.SEARCH_ENGINE_ID] = engineId }
    }

    suspend fun addCustomBlockedDomain(domain: String) {
        context.browserDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_BLOCKED_DOMAINS] ?: emptySet()
            prefs[Keys.CUSTOM_BLOCKED_DOMAINS] = current + domain
        }
    }

    suspend fun removeCustomBlockedDomain(domain: String) {
        context.browserDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_BLOCKED_DOMAINS] ?: emptySet()
            prefs[Keys.CUSTOM_BLOCKED_DOMAINS] = current - domain
        }
    }
}
