package com.example.aichat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aichat.theme.AIChatTheme
import com.example.aichat.ui.ChatScreen
import com.example.aichat.ui.SettingsScreen

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.example.aichat.data.SettingsManager

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val settingsManager = SettingsManager(context)
      val themeMode by settingsManager.themeModeFlow.collectAsState(initial = "system")
      val colorPalette by settingsManager.colorPaletteFlow.collectAsState(initial = "neon")
      val language by settingsManager.languageFlow.collectAsState(initial = "system")
      val customColor by settingsManager.customColorFlow.collectAsState(initial = "")

      LaunchedEffect(language) {
          val localeList = if (language == "system") {
              LocaleListCompat.getEmptyLocaleList()
          } else {
              LocaleListCompat.forLanguageTags(language)
          }
          AppCompatDelegate.setApplicationLocales(localeList)
      }

      AIChatTheme(themeMode = themeMode, colorPalette = colorPalette, customColor = customColor) {
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
              MainNavigation() 
          } 
      }
    }
  }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

