package com.example.memorialdays.share

import android.net.Uri
import java.io.File

data class PosterStyle(
    val background: Int,
    val surface: Int,
    val text: Int,
    val muted: Int,
    val accent: Int
)

data class PosterData(
    val title: String,
    val number: String,
    val status: String,
    val type: String,
    val primaryDate: String,
    val secondaryDate: String,
    val isToday: Boolean
)

data class PosterShare(
    val uri: Uri,
    val file: File
)
