package com.davidcarranco.oneloop.medtracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.davidcarranco.oneloop.medtracker.data.AppInfo
import com.davidcarranco.oneloop.medtracker.ui.components.FloatingMenuSpacer
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    title: String,
    body: String,
    showFloatingClearance: Boolean,
    onBack: () -> Unit,
) {
    val colors = OneLoopTheme.colors
    Scaffold(
        containerColor = colors.softBackground,
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .background(colors.softBackground)
                .padding(20.dp),
        ) {
            Text(body, color = colors.navy)
            FloatingMenuSpacer(showFloatingClearance)
        }
    }
}

@Composable
fun MedicalDisclaimerScreen(showFloatingClearance: Boolean, onBack: () -> Unit) {
    LegalDocumentScreen(
        title = "Medical Disclaimer",
        body = AppInfo.MEDICAL_DISCLAIMER_FULL,
        showFloatingClearance = showFloatingClearance,
        onBack = onBack,
    )
}

@Composable
fun PrivacyPolicyScreen(showFloatingClearance: Boolean, onBack: () -> Unit) {
    LegalDocumentScreen(
        title = "Privacy Policy",
        body = AppInfo.PRIVACY_POLICY_FULL,
        showFloatingClearance = showFloatingClearance,
        onBack = onBack,
    )
}
