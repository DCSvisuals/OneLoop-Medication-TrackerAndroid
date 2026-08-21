package com.davidcarranco.oneloop.medtracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.davidcarranco.oneloop.medtracker.ui.OneLoopApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleAuthIntent(intent)
        val app = application as OneLoopApplication
        setContent {
            OneLoopApp(
                store = app.medicationStore,
                preferences = app.preferences,
                supabase = app.supabase,
                notificationScheduler = app.notificationScheduler,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val app = application as? OneLoopApplication ?: return
        scope.launch {
            app.supabase.handleAuthCallback(uri)
        }
    }
}
