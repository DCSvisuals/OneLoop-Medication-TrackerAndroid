package com.davidcarranco.oneloop.medtracker.data.remote

object SupabaseConfig {
    const val PROJECT_URL = "https://eraojepflpmvcymumggy.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_43Gtyki76s6iUMRhPKBfrA_Mz2qZNMa"

    const val AUTH_REDIRECT_URL =
        "https://dcsvisuals.github.io/OneLoop-MedicationTracker-PrivacyPolicy/auth-callback/"

    const val AUTH_CALLBACK_SCHEME = "oneloopuiv2"
    const val LEGACY_AUTH_CALLBACK_SCHEME = "oneloop"
    const val AUTH_CALLBACK_HOST = "auth-callback"
    const val AUTH_CALLBACK_URL = "oneloopuiv2://auth-callback"

    val isConfigured: Boolean
        get() = PROJECT_URL.startsWith("https://") &&
            !PROJECT_URL.contains("YOUR_") &&
            PUBLISHABLE_KEY.isNotBlank() &&
            !PUBLISHABLE_KEY.contains("YOUR_")
}
