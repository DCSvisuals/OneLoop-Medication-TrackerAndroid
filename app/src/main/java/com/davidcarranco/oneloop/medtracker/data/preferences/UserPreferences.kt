package com.davidcarranco.oneloop.medtracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "oneloop_settings",
)

class UserPreferences(private val context: Context) {

    val snapshot: Flow<PreferenceSnapshot> = context.dataStore.data.map { prefs ->
        PreferenceSnapshot(
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            hasAcceptedDisclaimer = prefs[Keys.HAS_ACCEPTED_DISCLAIMER] ?: false,
            useDarkMode = prefs[Keys.USE_DARK_MODE] ?: false,
            useSystemAppearance = prefs[Keys.USE_SYSTEM_APPEARANCE] ?: true,
            useMaterialNavigation = prefs[Keys.USE_MATERIAL_NAV] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            rememberedEmail = prefs[Keys.REMEMBERED_EMAIL].orEmpty(),
        )
    }

    suspend fun current(): PreferenceSnapshot = snapshot.first()

    suspend fun completeOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = true
            prefs[Keys.HAS_ACCEPTED_DISCLAIMER] = true
        }
    }

    suspend fun setAppearance(
        useSystemAppearance: Boolean,
        useDarkMode: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_SYSTEM_APPEARANCE] = useSystemAppearance
            prefs[Keys.USE_DARK_MODE] = useDarkMode
        }
    }

    suspend fun setUseMaterialNavigation(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_MATERIAL_NAV] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setRememberedEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email.isNullOrBlank()) {
                prefs.remove(Keys.REMEMBERED_EMAIL)
            } else {
                prefs[Keys.REMEMBERED_EMAIL] = email
            }
        }
    }

    data class PreferenceSnapshot(
        val hasCompletedOnboarding: Boolean,
        val hasAcceptedDisclaimer: Boolean,
        val useDarkMode: Boolean,
        val useSystemAppearance: Boolean,
        val useMaterialNavigation: Boolean,
        val notificationsEnabled: Boolean,
        val rememberedEmail: String,
    )

    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("hasCompletedOnboarding_v3")
        val HAS_ACCEPTED_DISCLAIMER = booleanPreferencesKey("hasAcceptedMedicalDisclaimer")
        val USE_DARK_MODE = booleanPreferencesKey("useDarkMode")
        val USE_SYSTEM_APPEARANCE = booleanPreferencesKey("useSystemAppearance")
        val USE_MATERIAL_NAV = booleanPreferencesKey("useMaterialNavigation")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notificationsEnabled")
        val REMEMBERED_EMAIL = stringPreferencesKey("rememberedEmail")
    }
}
