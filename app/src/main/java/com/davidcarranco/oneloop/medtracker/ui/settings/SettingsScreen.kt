package com.davidcarranco.oneloop.medtracker.ui.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.davidcarranco.oneloop.medtracker.data.AppInfo
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseConfig
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseManager
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.notifications.DoseNotificationScheduler
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingMenuSpacer
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: MedicationStore,
    preferences: UserPreferences,
    supabase: SupabaseManager,
    notificationScheduler: DoseNotificationScheduler,
    showFloatingClearance: Boolean,
    onOpenAccount: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by preferences.snapshot.collectAsState(
        initial = UserPreferences.PreferenceSnapshot(
            hasCompletedOnboarding = true,
            hasAcceptedDisclaimer = true,
            useDarkMode = false,
            useSystemAppearance = true,
            useMaterialNavigation = false,
            notificationsEnabled = false,
            rememberedEmail = "",
        ),
    )
    val signedIn by supabase.isSignedIn.collectAsState()
    val email by supabase.userEmail.collectAsState()
    var showSettingsHint by remember { mutableStateOf(false) }
    var notificationsDenied by remember { mutableStateOf(false) }

    fun refreshNotificationState() {
        val manager = context.getSystemService(NotificationManager::class.java)
        notificationsDenied = !manager.areNotificationsEnabled()
        if (prefs.notificationsEnabled && notificationsDenied) {
            scope.launch { preferences.setNotificationsEnabled(false) }
        }
    }

    LaunchedEffect(Unit) {
        refreshNotificationState()
        supabase.refreshSession()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            if (granted) {
                preferences.setNotificationsEnabled(true)
                notificationScheduler.rescheduleAll(store.medications.value)
                showSettingsHint = false
            } else {
                preferences.setNotificationsEnabled(false)
                showSettingsHint = true
            }
            refreshNotificationState()
        }
    }

    Scaffold(
        containerColor = colors.softBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.softBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsHeader("Account")
            ListItem(
                headlineContent = { Text(if (signedIn) "Account" else "Sign in / Register") },
                supportingContent = {
                    Text(
                        when {
                            !SupabaseConfig.isConfigured -> "Cloud not configured"
                            signedIn -> email ?: "Signed in"
                            else -> "Email, password, or Google"
                        },
                    )
                },
                leadingContent = { Icon(Icons.Filled.AccountCircle, null, tint = colors.blue) },
                modifier = Modifier.clickable(onClick = onOpenAccount),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            Text(
                "Optional cloud backup. After you sign in, medications sync automatically across your devices.",
                fontSize = 12.sp,
                color = colors.mutedText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            SettingsHeader("Notifications")
            ListItem(
                headlineContent = { Text("Medication reminders") },
                supportingContent = {
                    Text(
                        if (notificationsDenied) {
                            "Notifications are turned off for OneLoop in Android Settings. Enable them to receive dose reminders."
                        } else {
                            "Receive a reminder at every scheduled medication dose."
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                if (enabled) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        preferences.setNotificationsEnabled(true)
                                        if (!notificationScheduler.canScheduleExactAlarms()) {
                                            context.startActivity(
                                                notificationScheduler.exactAlarmSettingsIntent(),
                                            )
                                        }
                                        notificationScheduler.rescheduleAll(store.medications.value)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    preferences.setNotificationsEnabled(false)
                                    notificationScheduler.removeAllMedicationNotifications()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.success),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            if (notificationsDenied || showSettingsHint) {
                TextButton(
                    onClick = {
                        context.startActivity(notificationScheduler.appNotificationSettingsIntent())
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) { Text("Open Settings") }
            }

            SettingsHeader("Appearance")
            ListItem(
                headlineContent = { Text("Use system appearance") },
                supportingContent = {
                    Text(
                        if (prefs.useSystemAppearance) {
                            "OneLoop follows your device appearance setting."
                        } else if (prefs.useDarkMode) {
                            "Dark mode is enabled."
                        } else {
                            "Light mode is enabled."
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = prefs.useSystemAppearance,
                        onCheckedChange = {
                            scope.launch { preferences.setAppearance(it, prefs.useDarkMode) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.success),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Dark mode") },
                trailingContent = {
                    Switch(
                        checked = prefs.useDarkMode,
                        enabled = !prefs.useSystemAppearance,
                        onCheckedChange = {
                            scope.launch { preferences.setAppearance(false, it) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.success),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Material navigation") },
                supportingContent = {
                    Text(
                        if (prefs.useMaterialNavigation) {
                            "System Material navigation bar with a center add action."
                        } else {
                            "Floating capsule (pill) menu with center add button."
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = prefs.useMaterialNavigation,
                        onCheckedChange = {
                            scope.launch { preferences.setUseMaterialNavigation(it) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.success),
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            SettingsHeader("Legal")
            ListItem(
                headlineContent = { Text("Medical Disclaimer") },
                leadingContent = { Icon(Icons.Filled.VerifiedUser, null, tint = colors.blue) },
                modifier = Modifier.clickable(onClick = onOpenDisclaimer),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Privacy Policy") },
                leadingContent = { Icon(Icons.Filled.Policy, null, tint = colors.blue) },
                modifier = Modifier.clickable(onClick = onOpenPrivacy),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Privacy Policy (Online)") },
                leadingContent = { Icon(Icons.Filled.Language, null, tint = colors.blue) },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppInfo.PRIVACY_POLICY_URL)))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            Text(
                AppInfo.MEDICAL_DISCLAIMER_SHORT,
                fontSize = 12.sp,
                color = colors.mutedText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            SettingsHeader("Support")
            ListItem(
                headlineContent = { Text("Email Support") },
                supportingContent = { Text(AppInfo.SUPPORT_EMAIL) },
                leadingContent = { Icon(Icons.Filled.Email, null, tint = colors.blue) },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(AppInfo.SUPPORT_MAILTO)))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Support Website") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.blue) },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppInfo.SUPPORT_URL)))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )

            SettingsHeader("About")
            ListItem(
                headlineContent = { Text("App") },
                trailingContent = { Text(AppInfo.APP_NAME) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = { Text("${AppInfo.marketingVersion} (${AppInfo.buildNumber})") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                headlineContent = { Text("Medications") },
                trailingContent = { Text(store.medications.value.size.toString()) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            FloatingMenuSpacer(showFloatingClearance)
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        title.uppercase(),
        color = OneLoopTheme.colors.mutedText,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}
