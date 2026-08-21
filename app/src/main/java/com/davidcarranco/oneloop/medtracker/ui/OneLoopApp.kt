package com.davidcarranco.oneloop.medtracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseManager
import com.davidcarranco.oneloop.medtracker.data.repository.MedicationStore
import com.davidcarranco.oneloop.medtracker.notifications.DoseNotificationScheduler
import com.davidcarranco.oneloop.medtracker.ui.auth.AccountScreen
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingCapsuleNav
import com.davidcarranco.oneloop.medtracker.ui.history.HistoryScreen
import com.davidcarranco.oneloop.medtracker.ui.medication.AddMedicationScreen
import com.davidcarranco.oneloop.medtracker.ui.medication.EditMedicationScreen
import com.davidcarranco.oneloop.medtracker.ui.medication.MedicationDetailScreen
import com.davidcarranco.oneloop.medtracker.ui.navigation.AppTab
import com.davidcarranco.oneloop.medtracker.ui.onboarding.OnboardingScreen
import com.davidcarranco.oneloop.medtracker.ui.schedule.ScheduleScreen
import com.davidcarranco.oneloop.medtracker.ui.settings.MedicalDisclaimerScreen
import com.davidcarranco.oneloop.medtracker.ui.settings.PrivacyPolicyScreen
import com.davidcarranco.oneloop.medtracker.ui.settings.SettingsScreen
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import com.davidcarranco.oneloop.medtracker.ui.today.TodayScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private sealed interface Overlay {
    data object None : Overlay
    data object AddMedication : Overlay
    data object Account : Overlay
    data object Disclaimer : Overlay
    data object Privacy : Overlay
    data class Detail(val medicationId: String) : Overlay
    data class Edit(val medicationId: String) : Overlay
}

@Composable
fun OneLoopApp(
    store: MedicationStore,
    preferences: UserPreferences,
    supabase: SupabaseManager,
    notificationScheduler: DoseNotificationScheduler,
) {
    val prefs by preferences.snapshot.collectAsState(
        initial = UserPreferences.PreferenceSnapshot(
            hasCompletedOnboarding = false,
            hasAcceptedDisclaimer = false,
            useDarkMode = false,
            useSystemAppearance = true,
            useMaterialNavigation = false,
            notificationsEnabled = false,
            rememberedEmail = "",
        ),
    )
    val signedIn by supabase.isSignedIn.collectAsState()
    val revision by store.revision.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(AppTab.Today) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }

    LaunchedEffect(Unit) {
        store.resetDosesIfNeeded()
        supabase.refreshSession()
        while (isActive) {
            delay(30_000)
            store.resetDosesIfNeeded()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        store.resetDosesIfNeeded()
        scope.launch { supabase.refreshSession() }
    }
    LaunchedEffect(signedIn) {
        if (signedIn && !prefs.hasCompletedOnboarding) {
            preferences.completeOnboarding()
        }
        if (signedIn) {
            supabase.syncMedications(store)
        }
    }

    OneLoopTheme(
        useSystemAppearance = prefs.useSystemAppearance,
        useDarkMode = prefs.useDarkMode,
    ) {
        if (!prefs.hasCompletedOnboarding) {
            OnboardingScreen(
                preferences = preferences,
                supabase = supabase,
                notificationScheduler = notificationScheduler,
                onFinished = { scope.launch { preferences.completeOnboarding() } },
            )
            return@OneLoopTheme
        }

        @Suppress("UNUSED_VARIABLE")
        val observed = revision

        when (val current = overlay) {
            Overlay.AddMedication -> AddMedicationScreen(store) { overlay = Overlay.None }
            Overlay.Account -> AccountScreen(
                store = store,
                supabase = supabase,
                preferences = preferences,
                showFloatingClearance = !prefs.useMaterialNavigation,
                onBack = { overlay = Overlay.None },
            )
            Overlay.Disclaimer -> MedicalDisclaimerScreen(!prefs.useMaterialNavigation) { overlay = Overlay.None }
            Overlay.Privacy -> PrivacyPolicyScreen(!prefs.useMaterialNavigation) { overlay = Overlay.None }
            is Overlay.Detail -> MedicationDetailScreen(
                medicationId = current.medicationId,
                store = store,
                showFloatingClearance = !prefs.useMaterialNavigation,
                onEdit = { overlay = Overlay.Edit(current.medicationId) },
                onBack = { overlay = Overlay.None },
            )
            is Overlay.Edit -> {
                val medication = store.medication(current.medicationId)
                if (medication == null) {
                    overlay = Overlay.None
                } else {
                    EditMedicationScreen(medication, store) {
                        overlay = Overlay.Detail(current.medicationId)
                    }
                }
            }
            Overlay.None -> MainShell(
                store = store,
                preferences = preferences,
                supabase = supabase,
                notificationScheduler = notificationScheduler,
                prefs = prefs,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                onAdd = { overlay = Overlay.AddMedication },
                onOpenMedication = { overlay = Overlay.Detail(it) },
                onOpenAccount = { overlay = Overlay.Account },
                onOpenDisclaimer = { overlay = Overlay.Disclaimer },
                onOpenPrivacy = { overlay = Overlay.Privacy },
            )
        }
    }
}

@Composable
private fun MainShell(
    store: MedicationStore,
    preferences: UserPreferences,
    supabase: SupabaseManager,
    notificationScheduler: DoseNotificationScheduler,
    prefs: UserPreferences.PreferenceSnapshot,
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    onAdd: () -> Unit,
    onOpenMedication: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    if (prefs.useMaterialNavigation) {
        Scaffold(
            containerColor = colors.softBackground,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = colors.lime,
                    contentColor = colors.scheduleSelectionText,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add medication")
                }
            },
            bottomBar = {
                NavigationBar(containerColor = colors.cardBackground) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            icon = { Icon(tab.barIcon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.blue,
                                selectedTextColor = colors.blue,
                                indicatorColor = colors.blue.copy(alpha = 0.14f),
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                TabContent(
                    tab = selectedTab,
                    store = store,
                    preferences = preferences,
                    supabase = supabase,
                    notificationScheduler = notificationScheduler,
                    showFloatingClearance = false,
                    onAdd = onAdd,
                    onOpenMedication = onOpenMedication,
                    onOpenAccount = onOpenAccount,
                    onOpenDisclaimer = onOpenDisclaimer,
                    onOpenPrivacy = onOpenPrivacy,
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            TabContent(
                tab = selectedTab,
                store = store,
                preferences = preferences,
                supabase = supabase,
                notificationScheduler = notificationScheduler,
                showFloatingClearance = true,
                onAdd = onAdd,
                onOpenMedication = onOpenMedication,
                onOpenAccount = onOpenAccount,
                onOpenDisclaimer = onOpenDisclaimer,
                onOpenPrivacy = onOpenPrivacy,
            )
            FloatingCapsuleNav(
                selected = selectedTab,
                onSelect = onSelectTab,
                onAdd = onAdd,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun TabContent(
    tab: AppTab,
    store: MedicationStore,
    preferences: UserPreferences,
    supabase: SupabaseManager,
    notificationScheduler: DoseNotificationScheduler,
    showFloatingClearance: Boolean,
    onAdd: () -> Unit,
    onOpenMedication: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    when (tab) {
        AppTab.Today -> TodayScreen(store, showFloatingClearance, onAdd, onOpenMedication)
        AppTab.Schedule -> ScheduleScreen(store, showFloatingClearance)
        AppTab.History -> HistoryScreen(store, showFloatingClearance)
        AppTab.Settings -> SettingsScreen(
            store = store,
            preferences = preferences,
            supabase = supabase,
            notificationScheduler = notificationScheduler,
            showFloatingClearance = showFloatingClearance,
            onOpenAccount = onOpenAccount,
            onOpenDisclaimer = onOpenDisclaimer,
            onOpenPrivacy = onOpenPrivacy,
        )
    }
}
