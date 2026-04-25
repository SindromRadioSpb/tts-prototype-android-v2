package com.sindromradiospb.ttsprototypev2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF265D73),
    secondary = Color(0xFF74663F),
    tertiary = Color(0xFF7A3F4B),
    background = Color(0xFFFAFAF7),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F4F3),
)

@Composable
fun TtsPrototypeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
