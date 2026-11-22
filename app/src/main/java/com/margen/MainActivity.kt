package com.margen

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.margen.navigation.MargenNavHost
import com.margen.ui.theme.MargenTheme
import com.margen.ui.theme.isMargenDark
import com.margen.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MargenApp).container

        setContent {
            val themeMode by container.settingsStore.themeMode.collectAsStateWithLifecycle()
            val dark = isMargenDark(themeMode)

            // Los íconos de las barras del sistema siguen el tema elegido en la app.
            DisposableEffect(dark) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        LIGHT_NAV_SCRIM,
                        DARK_NAV_SCRIM
                    ) { dark }
                )
                onDispose { }
            }

            MargenTheme(themeMode = themeMode) {
                var showWelcome by rememberSaveable { mutableStateOf(true) }
                Crossfade(targetState = showWelcome, label = "welcome") { welcome ->
                    if (welcome) {
                        WelcomeScreen(onFinished = { showWelcome = false })
                    } else {
                        MargenNavHost()
                    }
                }
            }
        }
    }

    private companion object {
        val LIGHT_NAV_SCRIM: Int = AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_NAV_SCRIM: Int = AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
