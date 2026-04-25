package com.sindromradiospb.ttsprototypev2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.sindromradiospb.ttsprototypev2.ui.AppRoot
import com.sindromradiospb.ttsprototypev2.ui.theme.TtsPrototypeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TtsPrototypeTheme {
                Surface {
                    AppRoot()
                }
            }
        }
    }
}
