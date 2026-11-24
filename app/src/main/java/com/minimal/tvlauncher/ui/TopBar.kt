package com.minimal.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopBar(onAction: (QuickAction) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QuickAction.entries.forEach { action ->
                FocusTile(CircleShape, Modifier.size(30.dp), onClick = { onAction(action) }) { focused ->
                    Box(
                        Modifier.fillMaxSize()
                            .background(Palette.Surface.copy(alpha = 0.7f), CircleShape)
                            .border(1.dp, Palette.Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        QuickIcon(action, if (focused) Palette.Accent else Palette.TextMuted, 15.dp)
                    }
                }
            }
        }
        Clock()
    }
}

@Composable
fun Clock(modifier: Modifier = Modifier, color: Color = Palette.TextMuted) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(15_000)
        }
    }
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Label(format.format(now), modifier, color, 14.sp, letterSpacing = 1.sp)
}
