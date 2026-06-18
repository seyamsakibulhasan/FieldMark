package com.fieldmark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import com.fieldmark.app.i18n.LocaleManager
import com.fieldmark.app.nav.AppNav
import com.fieldmark.app.ui.theme.FieldMarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val locale by LocaleManager.current.collectAsState()
            androidx.compose.runtime.SideEffect {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(locale)
                )
            }
            FieldMarkTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }
}
