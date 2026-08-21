package com.davidcarranco.oneloop.medtracker.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseConfig
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseManager
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingMenuSpacer
import com.davidcarranco.oneloop.medtracker.ui.components.OneLoopCard
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    store: MedicationStore,
    supabase: SupabaseManager,
    preferences: UserPreferences,
    showFloatingClearance: Boolean,
    onBack: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val scope = rememberCoroutineScope()
    val signedIn by supabase.isSignedIn.collectAsState()
    val email by supabase.userEmail.collectAsState()
    val isBusy by supabase.isBusy.collectAsState()
    val lastError by supabase.lastError.collectAsState()
    val lastStatus by supabase.lastStatus.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supabase.refreshSession()
        if (supabase.isSignedIn.value) {
            supabase.syncMedications(store)
        }
    }

    Scaffold(
        containerColor = colors.softBackground,
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.softBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when {
                !SupabaseConfig.isConfigured -> {
                    OneLoopCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Icon(Icons.Filled.Warning, null, tint = colors.warning)
                        Text("Cloud account is not configured", color = colors.warning)
                        Text(
                            "Add your Supabase Project URL and Publishable key in SupabaseConfig.kt, then rebuild the app.",
                            fontSize = 12.sp,
                            color = colors.mutedText,
                        )
                    }
                }

                signedIn -> {
                    OneLoopCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("Signed in", color = colors.blue)
                        Text("Email", color = colors.navy)
                        Text(email ?: "—", color = colors.mutedText)
                        Text(
                            "Your medications can be backed up to the cloud. Only your account can access them.",
                            fontSize = 12.sp,
                            color = colors.mutedText,
                        )
                        lastStatus?.let { Text(it, color = colors.teal, fontSize = 12.sp) }
                        lastError?.let { Text(it, color = colors.warning, fontSize = 12.sp) }
                    }
                    OneLoopCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Button(
                            onClick = { scope.launch { supabase.pushMedications(store) } },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.CloudUpload, null)
                            Text("  Upload medications")
                        }
                        OutlinedButton(
                            onClick = { scope.launch { supabase.pullMedications(store) } },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.CloudDownload, null)
                            Text("  Download medications")
                        }
                        Text(
                            "Medications sync automatically when you sign in. Upload and download can still force a copy. Download replaces local medications with the cloud copy.",
                            fontSize = 12.sp,
                            color = colors.mutedText,
                        )
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { confirmSignOut = true },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null)
                            Text("  Sign out")
                        }
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        ) {
                            Icon(Icons.Filled.Delete, null)
                            Text("  Delete account")
                        }
                        Text(
                            "Delete account removes your cloud login and backed-up medications. Local medications on this device are kept.",
                            fontSize = 12.sp,
                            color = colors.mutedText,
                        )
                    }
                }

                else -> {
                    AuthCard(
                        supabase = supabase,
                        preferences = preferences,
                        modifierPadding = true,
                    )
                }
            }
            FloatingMenuSpacer(showFloatingClearance)
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text("You’ll stay signed out on this device until you sign in again. Medications already on this phone are not removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        scope.launch { supabase.signOut() }
                    },
                ) { Text("Sign out", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete account?") },
            text = {
                Text("This permanently deletes your OneLoop cloud account and backed-up medications. Medications already saved on this device are not removed. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch { supabase.deleteAccount() }
                    },
                ) { Text("Delete account", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AuthCard(
    supabase: SupabaseManager,
    preferences: UserPreferences,
    modifierPadding: Boolean,
) {
    Column(modifier = Modifier.padding(horizontal = if (modifierPadding) 20.dp else 0.dp)) {
        com.davidcarranco.oneloop.medtracker.ui.auth.AuthCard(
            supabase = supabase,
            preferences = preferences,
        )
    }
}
