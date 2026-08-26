package com.davidcarranco.oneloop.medtracker.ui.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.davidcarranco.oneloop.medtracker.R
import com.davidcarranco.oneloop.medtracker.data.AppInfo
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseManager
import com.davidcarranco.oneloop.medtracker.notifications.DoseNotificationScheduler
import com.davidcarranco.oneloop.medtracker.ui.auth.AuthCard
import com.davidcarranco.oneloop.medtracker.ui.components.PageDots
import com.davidcarranco.oneloop.medtracker.ui.components.PrimaryButton
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6
private const val PAGE_SPLASH = 0
private const val PAGE_WELCOME = 1
private const val PAGE_TUTORIAL = 2
private const val PAGE_NOTIFICATIONS = 3
private const val PAGE_POLICY = 4
private const val PAGE_AUTH = 5

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
    val signedIn by supabase.isSignedIn.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    var acceptedPolicy by remember { mutableStateOf(false) }
    var requestingNotifications by remember { mutableStateOf(false) }
    var didAutoAdvanceSplash by remember { mutableStateOf(false) }

    val notificationsAlreadyAllowed = remember {
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    LaunchedEffect(signedIn) { if (signedIn) onFinished() }
    LaunchedEffect(acceptedPolicy, pagerState.currentPage) {
        if (pagerState.currentPage > PAGE_POLICY && !acceptedPolicy) {
            pagerState.scrollToPage(PAGE_POLICY)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        requestingNotifications = false
        scope.launch {
            if (granted) preferences.setNotificationsEnabled(true)
            pagerState.animateScrollToPage(PAGE_POLICY)
        }
    }

    fun goTo(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    fun handleAllowNotifications() {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.areNotificationsEnabled()) {
            scope.launch {
                preferences.setNotificationsEnabled(true)
                pagerState.animateScrollToPage(PAGE_POLICY)
            }
            return
        }
        if (!manager.areNotificationsEnabled() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            context.startActivity(notificationScheduler.appNotificationSettingsIntent())
            return
        }
        requestingNotifications = true
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun handleContinue() {
        when (pagerState.currentPage) {
            PAGE_WELCOME -> goTo(PAGE_TUTORIAL)
            PAGE_TUTORIAL -> goTo(PAGE_NOTIFICATIONS)
            PAGE_NOTIFICATIONS -> handleAllowNotifications()
            PAGE_POLICY -> if (acceptedPolicy) goTo(PAGE_AUTH)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pagerState.currentPage == PAGE_SPLASH) colors.splashFill else colors.softBackground),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                PAGE_SPLASH -> SplashPage(
                    onContinue = { goTo(PAGE_WELCOME) },
                    autoAdvance = !didAutoAdvanceSplash,
                    onAutoAdvanceStarted = { didAutoAdvanceSplash = true },
                )
                PAGE_WELCOME -> PhotoStoryPage(
                    title = "Welcome",
                    subtitle = "we're glad that you are here",
                    image = R.drawable.onboarding_welcome_plant,
                    imageFill = true,
                )
                PAGE_TUTORIAL -> PhotoStoryPage(
                    title = "Your Daily Loop",
                    subtitle = "Add a medication, get a reminder, mark it taken. History stays even after you remove a med.",
                    image = R.drawable.onboarding_shelf_plants,
                    imageFill = false,
                )
                PAGE_NOTIFICATIONS -> NotificationsPage(
                    alreadyGranted = notificationsAlreadyAllowed,
                )
                PAGE_POLICY -> PolicyPage(
                    accepted = acceptedPolicy,
                    onAcceptedChange = { acceptedPolicy = it },
                )
                else -> AuthPage(
                    supabase = supabase,
                    preferences = preferences,
                    onFinished = onFinished,
                )
            }
        }

        if (pagerState.currentPage != PAGE_SPLASH) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (pagerState.currentPage != PAGE_AUTH) {
                    PrimaryButton(
                        title = "Continue",
                        enabled = when {
                            requestingNotifications -> false
                            pagerState.currentPage == PAGE_POLICY -> acceptedPolicy
                            else -> true
                        },
                        onClick = { handleContinue() },
                    )
                }
                if (pagerState.currentPage == PAGE_NOTIFICATIONS) {
                    Text(
                        "Not now",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.navy,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .height(36.dp)
                            .clickable(enabled = !requestingNotifications) {
                                goTo(PAGE_POLICY)
                            },
                    )
                } else if (pagerState.currentPage != PAGE_AUTH) {
                    Spacer(Modifier.height(36.dp))
                }
                PageDots(
                    count = PAGE_COUNT - 1,
                    current = (pagerState.currentPage - 1).coerceAtLeast(0),
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SplashPage(
    onContinue: () -> Unit,
    autoAdvance: Boolean,
    onAutoAdvanceStarted: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    LaunchedEffect(autoAdvance) {
        if (!autoAdvance) return@LaunchedEffect
        onAutoAdvanceStarted()
        delay(1800)
        onContinue()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.splashFill)
            .clickable(onClick = onContinue)
            .semantics { contentDescription = "OneLoop UIv2" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ONELOOP",
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 6.sp,
                color = colors.splashWordmark,
            )
            Text(
                "UIv2",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                color = colors.splashWordmark.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun PhotoStoryPage(
    title: String,
    subtitle: String,
    image: Int,
    imageFill: Boolean,
) {
    val colors = OneLoopTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .statusBarsPadding()
            .padding(bottom = 200.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = colors.navy,
            )
            Text(subtitle, fontSize = 20.sp, color = colors.teal)
        }
        Spacer(Modifier.height(12.dp))
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = if (imageFill) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (imageFill) 360.dp else 240.dp)
                .then(
                    if (imageFill) {
                        Modifier
                    } else {
                        Modifier
                            .padding(horizontal = 28.dp)
                            .clip(RoundedCornerShape(8.dp))
                    },
                ),
        )
    }
}

@Composable
private fun NotificationsPage(alreadyGranted: Boolean) {
    val colors = OneLoopTheme.colors
    val context = LocalContext.current
    val manager = context.getSystemService(NotificationManager::class.java)
    val denied = !manager.areNotificationsEnabled() && !alreadyGranted
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .statusBarsPadding()
            .padding(bottom = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Image(
            painter = painterResource(R.drawable.onboarding_root_plant),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 36.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Stay In Your Loop",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colors.navy,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
        Text(
            "Allow notifications so OneLoop UIv2 can remind you at every scheduled dose. Reminders stay on this phone.",
            color = colors.teal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
        )
        if (alreadyGranted || manager.areNotificationsEnabled()) {
            Text(
                "Notifications are already allowed for OneLoop UIv2.",
                fontSize = 13.sp,
                color = colors.teal,
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.elevatedCard)
                    .padding(14.dp),
            )
        } else if (denied &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Text(
                "Notifications are turned off for OneLoop UIv2 in Android Settings. You can enable them there, or continue and turn them on later.",
                fontSize = 13.sp,
                color = colors.warning,
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.elevatedCard)
                    .padding(14.dp),
            )
        }
    }
}

@Composable
private fun PolicyPage(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
) {
    val colors = OneLoopTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .statusBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 48.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "A quiet note",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = colors.navy,
        )
        Text("Please read carefully before using OneLoop UIv2.", color = colors.teal)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp)),
        ) {
            Text(
                AppInfo.MEDICAL_DISCLAIMER_FULL,
                color = colors.navy,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            )
        }
        RowToggle(
            checked = accepted,
            onChange = onAcceptedChange,
            title = "I understand OneLoop UIv2 is a personal reminder tool, not a medical device or source of medical advice, and that optional account data may be stored with Supabase.",
        )
    }
}

@Composable
private fun RowToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    title: String,
) {
    val colors = OneLoopTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.elevatedCard)
            .padding(14.dp),
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = colors.navy, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.blue),
            )
        }
    }
}

@Composable
private fun AuthPage(
    supabase: SupabaseManager,
    preferences: UserPreferences,
    onFinished: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.softBackground)
            .statusBarsPadding()
            .padding(bottom = 72.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Text(
                "Skip",
                fontWeight = FontWeight.SemiBold,
                color = colors.navy,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBackground.copy(alpha = 0.7f))
                    .clickable(onClick = onFinished)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { contentDescription = "Skip sign in" },
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding_root_plant),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(120.dp),
            )
            AuthCard(
                supabase = supabase,
                preferences = preferences,
                onAuthenticated = onFinished,
            )
            Text(
                "You can create an account anytime in Settings.",
                fontSize = 12.sp,
                color = colors.mutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}
