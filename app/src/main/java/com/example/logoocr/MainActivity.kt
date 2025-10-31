package com.example.logoocr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.logoocr.ui.theme.LogoOcrTheme
import com.example.logoocr.ui.navigation.LogoOcrNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            LogoOcrTheme {
                LogoOcrNavHost()
            }
        }
    }
}
