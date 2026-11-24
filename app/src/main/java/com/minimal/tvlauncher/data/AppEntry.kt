package com.minimal.tvlauncher.data

import android.content.Intent

data class AppEntry(
    val packageName: String,
    val label: String,
    val launchIntent: Intent,
)
