package com.minimal.tvlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Saludo de bienvenida: a la derecha (a la izquierda va el logo del programa), entre los botones superiores y "Seguir viendo". Entra deslizando y se desvanece. */
@Composable
fun Greeting(visible: Boolean, name: String, line: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(end = 48.dp), contentAlignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            visible,
            Modifier.offset(y = (-100).dp),
            enter = fadeIn(tween(700)) + slideInHorizontally(tween(700)) { it / 6 },
            exit = fadeOut(tween(600)) + slideOutHorizontally(tween(600)) { it / 6 },
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Label("Hola, $name", color = Palette.Text, size = 30.sp, textAlign = TextAlign.End)
                Spacer(Modifier.height(6.dp))
                Label(line, Modifier.width(460.dp), Palette.Accent, 14.sp, letterSpacing = 0.5.sp, maxLines = 3, textAlign = TextAlign.End)
            }
        }
    }
}
