package com.minimal.tvlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(15_000)
        }
    }
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    // SPIKE TEMPORAL (Task 2): lista Watch Next; se elimina en la Task 5.
    val context = androidx.compose.ui.platform.LocalContext.current
    var spike by remember { mutableStateOf<List<com.minimal.tvlauncher.data.WatchNextItem>?>(null) }
    LaunchedEffect(Unit) {
        val items = com.minimal.tvlauncher.data.WatchNextRepository(context).load()
        android.util.Log.d("WatchNextSpike", "filas=${items.size}")
        items.forEach { android.util.Log.d("WatchNextSpike", "${it.packageName} | ${it.title} | ${it.progress}") }
        spike = items
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0D))) {
        Text(
            text = spike?.joinToString("\n") { "${it.packageName} · ${it.title}" } ?: "cargando…",
            color = Color(0xFFCFCFD4),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp),
        )
        Text(
            text = format.format(now),
            color = Color(0xFFCFCFD4),
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 48.dp),
        )
    }
}
