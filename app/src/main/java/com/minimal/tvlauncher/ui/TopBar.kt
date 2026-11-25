package com.minimal.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.minimal.tvlauncher.data.City
import java.util.TimeZone
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
/** [attention]: marca con un punto el acceso a los ajustes del launcher (p. ej. el botón HOME está inactivo). */
fun TopBar(onAction: (QuickAction) -> Unit, modifier: Modifier = Modifier, attention: Boolean = false) {
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
                        if (attention && action == QuickAction.LauncherSettings) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(2.dp).size(8.dp)
                                    .background(Palette.Accent, CircleShape),
                            )
                        }
                    }
                }
            }
        }
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
    Label(format.format(now), modifier, color, 24.sp, weight = FontWeight.Bold, letterSpacing = 1.sp)
}

/** Reloj principal con la fecha y, opcional, uno secundario más pequeño debajo. La fecha se muestra una sola vez. */
@Composable
fun DualClock(primary: City, secondary: City?, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(10_000)
        }
    }
    fun timeIn(city: City): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone(city.zoneId) }.format(now)
    val date = SimpleDateFormat("EEE d MMM", Locale("es")).apply { timeZone = TimeZone.getTimeZone(primary.zoneId) }
        .format(now).uppercase()
    // Fecha a la izquierda de los relojes; ambos bloques comparten la misma línea inferior.
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Label(date, color = Palette.TextMuted.copy(alpha = 0.85f), size = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 3.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(primary.name.uppercase(), color = Palette.TextMuted.copy(alpha = 0.8f), size = 8.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 4.dp))
                Label(timeIn(primary), color = Palette.TextMuted, size = 24.sp, weight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            if (secondary != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label(secondary.name.uppercase(), color = Palette.TextMuted.copy(alpha = 0.7f), size = 7.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 2.dp))
                    Label(timeIn(secondary), color = Palette.TextMuted.copy(alpha = 0.85f), size = 14.sp, weight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
