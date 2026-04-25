package com.sindromradiospb.ttsprototypev2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeViewModel
import com.sindromradiospb.ttsprototypev2.ui.AppRoot
import com.sindromradiospb.ttsprototypev2.ui.theme.TtsPrototypeTheme

class MainActivity : ComponentActivity() {
    private val classicModeViewModel: ClassicModeViewModel by viewModels {
        ClassicModeViewModel.Factory(
            (application as TtsPrototypeApplication).libraryRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TtsPrototypeTheme {
                Surface {
                    AppRoot(classicModeViewModel)
                }
            }
        }
    }
}
