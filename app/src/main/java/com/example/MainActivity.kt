package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notifications.BirthdayNotificationHelper
import com.example.ui.AppNavigation
import com.example.ui.theme.AmaderPoribarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BirthdayNotificationHelper.ensureNotificationChannel(this)
        setContent {
            AmaderPoribarTheme {
                AppNavigation()
            }
        }
    }
}
