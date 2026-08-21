package com.davidcarranco.oneloop.medtracker.ui.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.davidcarranco.oneloop.medtracker.data.AppInfo
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseManager
import com.davidcarranco.oneloop.medtracker.notifications.DoseNotificationScheduler
import com.davidcarranco.oneloop.medtracker.ui.auth.AuthCard
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingCapsuleNav
import com.davidcarranco.oneloop.medtracker.ui.components.PrimaryButton
import com.davidcarranco.oneloop.medtracker.ui.navigation.AppTab
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6
private const val POLICY_PAGE = 4

@Composable
fun OnboardingScreen(
    preferences: UserPreferences,
    supabase: SupabaseManager,
    notificationScheduler: DoseNotificationScheduler,
    onFinished: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs by preferences.snapshot.collectAsState(
        initial = UserPreferences.PreferenceSnapshot(false, false, false, true, false, false, ""),
    )
    val signedIn by supabase.isSignedIn.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    var acceptedPolicy by remember { mutableStateOf(false) }
    var requestingNotifications by remember { mutableStateOf(false) }
    val notificationsEnabled = remember {
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    LaunchedEffect(signedIn) { if (signedIn) onFinished() }
    LaunchedEffect(acceptedPolicy, pagerState.currentPage) {
        if (pagerState.currentPage > POLICY_PAGE && !acceptedPolicy) {
            pagerState.scrollToPage(POLICY_PAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        requestingNotifications = false
        scope.launch {
            if (granted) preferences.setNotificationsEnabled(true)
            pagerState.animateScrollToPage(POLICY_PAGE)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground),
    ) {
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(colors.blue.copy(0.06f)),
        )
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(colors.lime.copy(0.08f)),
        )

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = colors.navy)
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    repeat(PAGE_COUNT) { index ->
                        val selected = index == pagerState.currentPage
                        val color by animateColorAsState(
                            if (selected) colors.blue else colors.mutedText.copy(0.25f),
                            label = "dot",
                        )
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(if (selected) 22.dp else 8.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }
                Spacer(Modifier.size(48.dp))
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = pagerState.currentPage != POLICY_PAGE || acceptedPolicy,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> AboutPage { scope.launch { pagerState.animateScrollToPage(1) } }
                    1 -> TutorialPage { scope.launch { pagerState.animateScrollToPage(2) } }
                    2 -> AppearancePage(prefs, preferences) { scope.launch { pagerState.animateScrollToPage(3) } }
                    3 -> NotificationsPage(
                        alreadyGranted = notificationsEnabled,
                        requesting = requestingNotifications,
                        onAllow = {
                            if (notificationsEnabled) {
                                scope.launch {
                                    preferences.setNotificationsEnabled(true)
                                    pagerState.animateScrollToPage(POLICY_PAGE)
                                }
                            } else if (
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                scope.launch {
                                    preferences.setNotificationsEnabled(true)
                                    pagerState.animateScrollToPage(POLICY_PAGE)
                                }
                            } else {
                                requestingNotifications = true
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenSettings = {
                            context.startActivity(notificationScheduler.appNotificationSettingsIntent())
                        },
                        onSkip = { scope.launch { pagerState.animateScrollToPage(POLICY_PAGE) } },
                    )
                    4 -> PolicyPage(acceptedPolicy, onAcceptedChange = { acceptedPolicy = it }) {
                        scope.launch { pagerState.animateScrollToPage(5) }
                    }
                    else -> AuthPage(
                        supabase = supabase,
                        preferences = preferences,
                        onFinished = onFinished,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutPage(onContinue: () -> Unit) {
    OnboardingScaffold(
        title = "About OneLoop",
        subtitle = "Your personal medication schedule and reminder companion.",
        footer = { PrimaryButton("Continue", onClick = onContinue) },
    ) {
        IconBadge(Icons.Filled.MedicalServices)
        Bullet(Icons.Filled.Notifications, "Smart reminders", "Local notifications for every scheduled dose.")
        Bullet(Icons.Filled.CalendarMonth, "Flexible schedules", "Daily plans, intervals, and staged dose changes.")
        Bullet(Icons.Filled.History, "History that lasts", "Keep records even after removing a medication.")
        Bullet(Icons.Filled.Cloud, "Optional cloud backup", "Sign in later to sync across devices when you’re ready.")
    }
}

@Composable
private fun TutorialPage(onContinue: () -> Unit) {
    val colors = OneLoopTheme.colors
    OnboardingScaffold(
        title = "How to use OneLoop",
        subtitle = "Add medications in a few taps, then pin widgets for a quick glance.",
        footer = { PrimaryButton("Continue", onClick = onContinue) },
    ) {
        Text("Add a medication", fontWeight = FontWeight.SemiBold, color = colors.navy)
        Numbered("1", "Tap the + button", "Use the center + in the bottom menu, or the + in the Today toolbar.")
        Numbered("2", "Enter name, dose, and times", "Choose form (pill, injection, cream), amount, first reminder, and how often.")
        Numbered("3", "Save and follow Today", "Mark doses taken, snooze when needed, and review History anytime.")
        Text("Home Screen widgets", fontWeight = FontWeight.SemiBold, color = colors.navy)
        Bullet(Icons.Filled.Widgets, "Home Screen", "Add the OneLoop widget to see next medication and today’s progress.")
        Text(
            "Tip: touch and hold the Home Screen → Widgets → OneLoop.",
            color = colors.mutedText,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.elevatedCard)
                .padding(14.dp),
        )
    }
}

@Composable
private fun AppearancePage(
    prefs: UserPreferences.PreferenceSnapshot,
    preferences: UserPreferences,
    onContinue: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val scope = rememberCoroutineScope()
    OnboardingScaffold(
        title = "Make it yours",
        subtitle = "Choose how OneLoop looks. You can change this later in Settings.",
        footer = { PrimaryButton("Continue", onClick = onContinue) },
    ) {
        Text("Appearance", fontWeight = FontWeight.SemiBold, color = colors.navy)
        ToggleRow("Use system appearance", prefs.useSystemAppearance) {
            scope.launch { preferences.setAppearance(it, prefs.useDarkMode) }
        }
        ToggleRow("Dark mode", prefs.useDarkMode, enabled = !prefs.useSystemAppearance) {
            scope.launch { preferences.setAppearance(false, it) }
        }
        Text(
            when {
                prefs.useSystemAppearance -> "OneLoop follows your device light/dark setting."
                prefs.useDarkMode -> "Dark mode is selected."
                else -> "Light mode is selected."
            },
            fontSize = 12.sp,
            color = colors.mutedText,
        )
        Text("Bottom menu", fontWeight = FontWeight.SemiBold, color = colors.navy)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.elevatedCard.copy(0.65f))
                .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                .padding(12.dp),
        ) {
            if (prefs.useMaterialNavigation) {
                Text("Material navigation bar preview", color = colors.mutedText, fontSize = 12.sp)
            } else {
                FloatingCapsuleNav(AppTab.Today, onSelect = {}, onAdd = {})
            }
        }
        ChoiceRow(
            title = "Pill menu",
            detail = "Floating capsule with a center + button.",
            selected = !prefs.useMaterialNavigation,
        ) { scope.launch { preferences.setUseMaterialNavigation(false) } }
        ChoiceRow(
            title = "Material navigation",
            detail = "System-style navigation bar with a center add action.",
            selected = prefs.useMaterialNavigation,
        ) { scope.launch { preferences.setUseMaterialNavigation(true) } }
    }
}

@Composable
private fun NotificationsPage(
    alreadyGranted: Boolean,
    requesting: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    val manager = LocalContext.current.getSystemService(NotificationManager::class.java)
    val denied = !manager.areNotificationsEnabled() && alreadyGranted.not() &&
        manager.importance == NotificationManager.IMPORTANCE_NONE
    OnboardingScaffold(
        title = "Stay on track",
        subtitle = "Allow notifications so OneLoop can remind you at every scheduled dose.",
        footer = {
            PrimaryButton(
                title = when {
                    requesting -> "Requesting…"
                    alreadyGranted -> "Continue"
                    denied -> "Open Settings"
                    else -> "Allow notifications"
                },
                enabled = !requesting,
                onClick = if (denied) onOpenSettings else onAllow,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(if (alreadyGranted) "Continue without changes" else "Not now", color = colors.navy)
            }
        },
    ) {
        IconBadge(Icons.Filled.Notifications)
        Bullet(Icons.Filled.CalendarMonth, "On-time dose reminders", "Get a local alert when each medication is due.")
        Bullet(Icons.Filled.Lock, "Stays on your device", "Reminders are scheduled on this phone — nothing is sent to a third-party messaging service.")
        Bullet(Icons.Filled.Tune, "Change anytime", "You can turn reminders on or off later in Settings.")
        if (alreadyGranted) {
            Text(
                "Notifications are already allowed for OneLoop.",
                color = colors.teal,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.elevatedCard).padding(14.dp),
            )
        }
    }
}

@Composable
private fun PolicyPage(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    OnboardingScaffold(
        title = "Medical disclaimer",
        subtitle = "Please read carefully before using OneLoop.",
        footer = {
            PrimaryButton("I Agree — Continue", enabled = accepted, onClick = onContinue)
        },
    ) {
        Text(
            AppInfo.MEDICAL_DISCLAIMER_FULL,
            color = colors.navy,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(18.dp))
                .padding(18.dp),
        )
        ToggleRow(
            title = "I understand OneLoop is a personal reminder tool, not a medical device or source of medical advice, and that optional account data may be stored with Supabase.",
            checked = accepted,
            onChange = onAcceptedChange,
        )
    }
}

@Composable
private fun AuthPage(
    supabase: SupabaseManager,
    preferences: UserPreferences,
    onFinished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        AuthCard(
            supabase = supabase,
            preferences = preferences,
            showsSignInLater = true,
            onAuthenticated = onFinished,
            onSignInLater = onFinished,
        )
    }
}

@Composable
private fun OnboardingScaffold(
    title: String,
    subtitle: String,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = OneLoopTheme.colors
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.navy)
            Text(subtitle, fontSize = 18.sp, color = colors.mutedText)
            content()
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) { footer() }
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = OneLoopTheme.colors
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.blue.copy(0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = colors.blue, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun Bullet(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    val colors = OneLoopTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.blue)
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = colors.navy)
            Text(detail, color = colors.mutedText)
        }
    }
}

@Composable
private fun Numbered(number: String, title: String, detail: String) {
    val colors = OneLoopTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(colors.blue),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = colors.navy)
            Text(detail, color = colors.mutedText)
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val colors = OneLoopTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = colors.navy, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.blue),
        )
    }
}

@Composable
private fun ChoiceRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val colors = OneLoopTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) colors.blue.copy(0.45f) else colors.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = colors.navy)
            Text(detail, fontSize = 12.sp, color = colors.mutedText)
        }
        Text(if (selected) "●" else "○", color = if (selected) colors.blue else colors.mutedText, fontSize = 18.sp)
    }
}
