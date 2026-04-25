package com.sindromradiospb.ttsprototypev2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeViewModel
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryViewModel
import com.sindromradiospb.ttsprototypev2.feature.settings.SettingsViewModel
import com.sindromradiospb.ttsprototypev2.ui.AppRoot
import com.sindromradiospb.ttsprototypev2.ui.theme.TtsPrototypeTheme

class MainActivity : ComponentActivity() {
    private val classicModeViewModel: ClassicModeViewModel by viewModels {
        val app = application as TtsPrototypeApplication
        ClassicModeViewModel.Factory(
            repository = app.libraryRepository,
            translationProviders = app.translationProviderRegistry,
            ttsProviders = app.ttsProviderRegistry,
        )
    }
    private val libraryViewModel: LibraryViewModel by viewModels {
        LibraryViewModel.Factory(
            (application as TtsPrototypeApplication).libraryRepository,
        )
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(
            (application as TtsPrototypeApplication).providerSettingsRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TtsPrototypeTheme {
                Surface {
                    AppRoot(
                        classicModeViewModel = classicModeViewModel,
                        libraryViewModel = libraryViewModel,
                        settingsViewModel = settingsViewModel,
                    )
                }
            }
        }
    }
}
