package com.davidcarranco.oneloop.medtracker.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.davidcarranco.oneloop.medtracker.data.model.Medication
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseManager(
    private val context: Context,
    private val preferences: UserPreferences,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val http = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val sessionPrefs = context.getSharedPreferences("oneloop_session", Context.MODE_PRIVATE)

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _lastStatus = MutableStateFlow<String?>(null)
    val lastStatus: StateFlow<String?> = _lastStatus.asStateFlow()

    private val _authenticated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authenticated: SharedFlow<Unit> = _authenticated.asSharedFlow()

    private var session: AuthSession? = loadPersistedSession()

    init {
        applySession(session)
    }

    suspend fun refreshSession() {
        val current = session ?: return
        withBusy {
            val response = http.post("${SupabaseConfig.PROJECT_URL}/auth/v1/token") {
                parameter("grant_type", "refresh_token")
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                contentType(ContentType.Application.Json)
                setBody(mapOf("refresh_token" to current.refreshToken))
            }
            if (response.status.isSuccess()) {
                persistSession(response.body())
                _lastError.value = null
            } else {
                clearSession()
            }
        }
    }

    suspend fun signInWithEmailPassword(email: String, password: String) {
        if (requireConfigured().not()) return
        withBusy {
            val response = http.post("${SupabaseConfig.PROJECT_URL}/auth/v1/token") {
                parameter("grant_type", "password")
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email.trim(), "password" to password))
            }
            if (!response.status.isSuccess()) {
                _lastError.value = errorMessage(response.bodyAsText(), "Could not sign in.")
                return@withBusy
            }
            persistSession(response.body())
            _lastStatus.value = "Signed in."
            completeAuthenticationSideEffects()
        }
    }

    suspend fun signUpWithEmailPassword(email: String, password: String, fullName: String?) {
        if (requireConfigured().not()) return
        withBusy {
            val body = buildJsonObject {
                put("email", email.trim())
                put("password", password)
                val name = fullName?.trim().orEmpty()
                if (name.isNotEmpty()) {
                    put("data", buildJsonObject { put("full_name", name) })
                }
            }
            val response = http.post("${SupabaseConfig.PROJECT_URL}/auth/v1/signup") {
                parameter("redirect_to", SupabaseConfig.AUTH_REDIRECT_URL)
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                _lastError.value = errorMessage(response.bodyAsText(), "Could not create account.")
                return@withBusy
            }
            val created: AuthSession? = runCatching { response.body<AuthSession>() }.getOrNull()
            if (created?.accessToken.isNullOrBlank()) {
                _lastStatus.value =
                    "Account created. Check your email to confirm, then open the link on this device."
                completeAuthenticationSideEffects()
            } else {
                persistSession(created)
                _lastStatus.value = "Account created and signed in."
                completeAuthenticationSideEffects()
            }
        }
    }

    suspend fun sendPasswordReset(email: String) {
        if (requireConfigured().not()) return
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            _lastError.value = "Enter your email first, then tap Forget password."
            return
        }
        withBusy {
            val response = http.post("${SupabaseConfig.PROJECT_URL}/auth/v1/recover") {
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "email" to trimmed,
                        "gotrue_meta_security" to emptyMap<String, String>(),
                    ),
                )
            }
            if (!response.status.isSuccess()) {
                _lastError.value = errorMessage(response.bodyAsText(), "Could not send reset email.")
                return@withBusy
            }
            _lastStatus.value = "Password reset email sent. Check your inbox."
        }
    }

    fun signInWithGoogle() {
        if (requireConfigured().not()) return
        val authorize = "${SupabaseConfig.PROJECT_URL}/auth/v1/authorize".toUri()
            .buildUpon()
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", SupabaseConfig.AUTH_CALLBACK_URL)
            .build()
        val tabs = CustomTabsIntent.Builder().build()
        tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tabs.launchUrl(context, authorize)
    }

    suspend fun handleAuthCallback(uri: Uri) {
        if (uri.scheme?.equals(SupabaseConfig.AUTH_CALLBACK_SCHEME, ignoreCase = true) != true) {
            return
        }
        val fragment = uri.encodedFragment.orEmpty()
        val query = uri.encodedQuery.orEmpty()
        val params = parseParams(fragment.ifBlank { query })
        val access = params["access_token"]
        val refresh = params["refresh_token"]
        if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) {
            persistSession(
                AuthSession(
                    accessToken = access,
                    refreshToken = refresh,
                    tokenType = params["token_type"] ?: "bearer",
                    expiresIn = params["expires_in"]?.toLongOrNull() ?: 3600,
                    user = AuthUser(id = params["user_id"], email = params["email"]),
                ),
            )
            fetchUser()
            _lastStatus.value = "Signed in."
            completeAuthenticationSideEffects()
            return
        }
        val code = params["code"]
        if (!code.isNullOrBlank()) {
            withBusy {
                val response = http.post("${SupabaseConfig.PROJECT_URL}/auth/v1/token") {
                    parameter("grant_type", "pkce")
                    header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("auth_code" to code))
                }
                if (response.status.isSuccess()) {
                    persistSession(response.body())
                    _lastStatus.value = "Signed in."
                    completeAuthenticationSideEffects()
                } else {
                    _lastError.value = errorMessage(response.bodyAsText(), "Could not complete sign-in.")
                }
            }
        }
    }

    suspend fun signOut() {
        val current = session
        withBusy {
            if (current != null) {
                runCatching {
                    http.post("${SupabaseConfig.PROJECT_URL}/auth/v1/logout") {
                        header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                        header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
                    }
                }
            }
            clearSession()
            _lastStatus.value = "Signed out."
        }
    }

    suspend fun deleteAccount() {
        val current = session
        val userId = current?.user?.id
        if (current == null || userId == null) {
            _lastError.value = "Sign in before deleting your account."
            return
        }
        withBusy {
            http.delete("${SupabaseConfig.PROJECT_URL}/rest/v1/medications") {
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
                header("Prefer", "return=minimal")
                parameter("user_id", "eq.$userId")
            }
            val response = http.delete("${SupabaseConfig.PROJECT_URL}/auth/v1/user") {
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
            }
            if (!response.status.isSuccess()) {
                _lastError.value =
                    "Could not delete account (HTTP ${response.status.value}). " +
                        "In Supabase → Authentication, enable “Allow users to delete their own accounts”."
                return@withBusy
            }
            clearSession()
            _lastStatus.value = "Your account was deleted."
        }
    }

    private var isSyncing = false

    suspend fun syncMedications(store: MedicationStore) {
        val current = session
        val userId = current?.user?.id
        if (current == null || userId == null || isSyncing) return
        isSyncing = true
        try {
            withBusy {
                val remote = fetchRemoteMedications(current) ?: return@withBusy
                store.mergeIncomingMedications(remote)
                if (!upsertMedications(store, current, userId)) return@withBusy
                val count = store.medications.value.size
                _lastStatus.value =
                    "Synced $count medication" + (if (count == 1) "" else "s") + " with your account."
            }
        } finally {
            isSyncing = false
        }
    }

    suspend fun pushMedications(store: MedicationStore, quiet: Boolean = false) {
        val current = session
        val userId = current?.user?.id
        if (current == null || userId == null) {
            if (!quiet) _lastError.value = "Sign in before syncing."
            return
        }
        if (quiet) {
            upsertMedications(store, current, userId)
            return
        }
        withBusy {
            if (!upsertMedications(store, current, userId)) return@withBusy
            val count = store.medications.value.size
            _lastStatus.value =
                "Uploaded $count medication" + (if (count == 1) "" else "s") + " to the cloud."
        }
    }

    suspend fun pullMedications(store: MedicationStore) {
        val current = session
        if (current == null) {
            _lastError.value = "Sign in before syncing."
            return
        }
        withBusy {
            val remote = fetchRemoteMedications(current) ?: return@withBusy
            store.replaceAllMedications(remote)
            _lastStatus.value =
                "Downloaded ${remote.size} medication" +
                    (if (remote.size == 1) "" else "s") +
                    " from the cloud."
        }
    }

    suspend fun deleteRemoteMedication(id: String) {
        val current = session ?: return
        val userId = current.user?.id ?: return
        runCatching {
            http.delete("${SupabaseConfig.PROJECT_URL}/rest/v1/medications") {
                header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
                header("Prefer", "return=minimal")
                parameter("id", "eq.$id")
                parameter("user_id", "eq.$userId")
            }
        }
    }

    private suspend fun fetchRemoteMedications(current: AuthSession): List<Medication>? {
        val response = http.get("${SupabaseConfig.PROJECT_URL}/rest/v1/medications") {
            header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
            parameter("select", "*")
        }
        if (!response.status.isSuccess()) {
            _lastError.value = errorMessage(response.bodyAsText(), "Could not download medications.")
            return null
        }
        val rows = json.decodeFromString<List<MedicationRemoteRow>>(response.bodyAsText())
        return rows.map { it.asMedication() }
    }

    private suspend fun upsertMedications(
        store: MedicationStore,
        current: AuthSession,
        userId: String,
    ): Boolean {
        val rows = store.medications.value.map { MedicationRemoteRow.from(it, userId) }
        if (rows.isEmpty()) return true
        val response = http.post("${SupabaseConfig.PROJECT_URL}/rest/v1/medications") {
            header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            contentType(ContentType.Application.Json)
            setBody(rows)
        }
        if (!response.status.isSuccess()) {
            _lastError.value = errorMessage(response.bodyAsText(), "Could not upload medications.")
            return false
        }
        return true
    }

    fun clearMessages() {
        _lastError.value = null
        _lastStatus.value = null
    }

    private suspend fun fetchUser() {
        val current = session ?: return
        val response = http.get("${SupabaseConfig.PROJECT_URL}/auth/v1/user") {
            header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            header(HttpHeaders.Authorization, "Bearer ${current.accessToken}")
        }
        if (response.status.isSuccess()) {
            val user = response.body<AuthUser>()
            persistSession(current.copy(user = user))
        }
    }

    private suspend fun completeAuthenticationSideEffects() {
        preferences.completeOnboarding()
        _authenticated.tryEmit(Unit)
    }

    private fun requireConfigured(): Boolean {
        if (SupabaseConfig.isConfigured) return true
        _lastError.value = "Add your Supabase URL and publishable key in SupabaseConfig.kt."
        return false
    }

    private fun persistSession(next: AuthSession) {
        session = next
        sessionPrefs.edit()
            .putString(SESSION_KEY, json.encodeToString(AuthSession.serializer(), next))
            .apply()
        applySession(next)
    }

    private fun clearSession() {
        session = null
        sessionPrefs.edit().remove(SESSION_KEY).apply()
        applySession(null)
    }

    private fun applySession(next: AuthSession?) {
        _isSignedIn.value = next?.accessToken?.isNotBlank() == true
        _userEmail.value = next?.user?.email
    }

    private fun loadPersistedSession(): AuthSession? {
        val raw = sessionPrefs.getString(SESSION_KEY, null) ?: return null
        return runCatching { json.decodeFromString(AuthSession.serializer(), raw) }.getOrNull()
    }

    private fun parseParams(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            Uri.decode(parts[0]) to Uri.decode(parts[1])
        }.toMap()
    }

    private fun errorMessage(body: String, fallback: String): String {
        val parsed = runCatching { json.decodeFromString<AuthError>(body) }.getOrNull()
        return parsed?.message ?: parsed?.errorDescription ?: parsed?.msg ?: fallback
    }

    private suspend fun withBusy(block: suspend () -> Unit) {
        _isBusy.value = true
        _lastError.value = null
        _lastStatus.value = null
        try {
            block()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _lastError.value = error.message ?: error.toString()
        } finally {
            _isBusy.value = false
        }
    }

    companion object {
        private const val SESSION_KEY = "session"
    }
}

@Serializable
private data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: AuthUser? = null,
)

@Serializable
private data class AuthUser(
    val id: String? = null,
    val email: String? = null,
)

@Serializable
private data class AuthError(
    val message: String? = null,
    val msg: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)
