package com.davidcarranco.oneloop.medtracker.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidcarranco.oneloop.medtracker.data.preferences.UserPreferences
import com.davidcarranco.oneloop.medtracker.data.remote.SupabaseManager
import com.davidcarranco.oneloop.medtracker.ui.components.PrimaryButton
import com.davidcarranco.oneloop.medtracker.ui.theme.OneLoopTheme
import kotlinx.coroutines.launch

private enum class AuthMode { Login, Register }

@Composable
fun AuthCard(
    supabase: SupabaseManager,
    preferences: UserPreferences,
    showsSignInLater: Boolean = false,
    onAuthenticated: () -> Unit = {},
    onSignInLater: () -> Unit = {},
) {
    val colors = OneLoopTheme.colors
    val scope = rememberCoroutineScope()
    val isBusy by supabase.isBusy.collectAsState()
    val lastError by supabase.lastError.collectAsState()
    val lastStatus by supabase.lastStatus.collectAsState()
    val isSignedIn by supabase.isSignedIn.collectAsState()
    val prefs by preferences.snapshot.collectAsState(
        initial = UserPreferences.PreferenceSnapshot(false, false, false, true, false, false, ""),
    )

    var mode by remember { mutableStateOf(AuthMode.Login) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(prefs.rememberedEmail) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var acceptedTerms by remember { mutableStateOf(false) }

    LaunchedEffect(prefs.rememberedEmail) {
        if (email.isBlank() && prefs.rememberedEmail.isNotBlank()) {
            email = prefs.rememberedEmail
        }
    }
    LaunchedEffect(isSignedIn) {
        if (isSignedIn) onAuthenticated()
    }

    val canSubmit = email.trim().isNotEmpty() &&
        password.length >= 6 &&
        (mode == AuthMode.Login || acceptedTerms)

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "oneloop",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp,
                color = colors.navy,
            )
            Text(
                "uiv2",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp,
                color = colors.navy,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(colors.cardBackground)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(28.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (mode == AuthMode.Login) "Stay in your loop" else "Create an account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = colors.navy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (mode == AuthMode.Register) {
                SmartField("Name", fullName, onChange = { fullName = it }, keyboard = KeyboardType.Text)
            }
            SmartField("Email", email, onChange = { email = it }, keyboard = KeyboardType.Email)
            SmartField(
                title = "Password",
                value = password,
                onChange = { password = it },
                keyboard = KeyboardType.Password,
                visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = colors.mutedText,
                        )
                    }
                },
            )

            if (mode == AuthMode.Login) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuthCheckbox(rememberMe, "Remember me") { rememberMe = it }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (email.trim().isEmpty()) {
                                    supabase.clearMessages()
                                }
                                supabase.sendPasswordReset(email)
                            }
                        },
                    ) { Text("Forget password?", color = colors.blue) }
                }
            } else {
                AuthCheckbox(acceptedTerms, "I agree to the Terms of Service") { acceptedTerms = it }
            }

            if (isBusy) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.blue)
                }
            } else {
                PrimaryButton(
                    title = if (mode == AuthMode.Login) "Login" else "Create account",
                    enabled = canSubmit,
                ) {
                    scope.launch {
                        if (mode == AuthMode.Login) {
                            if (rememberMe) preferences.setRememberedEmail(email.trim())
                            else preferences.setRememberedEmail(null)
                            supabase.signInWithEmailPassword(email, password)
                        } else {
                            if (!acceptedTerms) return@launch
                            supabase.signUpWithEmailPassword(email, password, fullName)
                        }
                        if (supabase.isSignedIn.value || (mode == AuthMode.Register && supabase.lastError.value == null)) {
                            onAuthenticated()
                        }
                    }
                }
            }

            lastStatus?.let { Text(it, color = colors.teal, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            lastError?.let { Text(it, color = colors.warning, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }

            TextButton(
                onClick = {
                    mode = if (mode == AuthMode.Login) AuthMode.Register else AuthMode.Login
                    supabase.clearMessages()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (mode == AuthMode.Login) "New here? Create an account" else "Already have an account? Login",
                    color = colors.blue,
                )
            }

            Text("Or Sign in with", color = colors.mutedText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.softBackground)
                        .border(1.dp, colors.cardBorder, CircleShape)
                        .clickable(enabled = !isBusy) {
                            scope.launch { supabase.signInWithGoogle() }
                        }
                        .semantics { contentDescription = "Google" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("G", fontWeight = FontWeight.Bold, color = colors.navy, fontSize = 20.sp)
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.softBackground)
                        .border(1.dp, colors.cardBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "A",
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText.copy(alpha = 0.35f),
                        fontSize = 20.sp,
                    )
                }
            }
        }

        if (showsSignInLater) {
            TextButton(onClick = onSignInLater, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in later", fontWeight = FontWeight.SemiBold, color = colors.blue)
            }
            Text(
                "You can create an account anytime in Settings.",
                fontSize = 12.sp,
                color = colors.mutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SmartField(
    title: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = OneLoopTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Medium, color = colors.navy)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = visual,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            trailingIcon = trailing,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.fieldFill,
                unfocusedContainerColor = colors.fieldFill,
                focusedBorderColor = colors.blue,
                unfocusedBorderColor = colors.cardBorder,
            ),
        )
    }
}

@Composable
private fun AuthCheckbox(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val colors = OneLoopTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onChange(!checked) },
    ) {
        Icon(
            if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked) colors.blue else colors.mutedText,
        )
        Spacer(Modifier.size(8.dp))
        Text(label, color = colors.navy)
    }
}
