package com.fieldmark.app.capture

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhotoMetadata(
    val heading: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val cardinal: String
        get() = when (((heading % 360f) + 360f) % 360f) {
            in 0f..22.5f, in 337.5f..360f -> "N"
            in 22.5f..67.5f -> "NE"
            in 67.5f..112.5f -> "E"
            in 112.5f..157.5f -> "SE"
            in 157.5f..202.5f -> "S"
            in 202.5f..247.5f -> "SW"
            in 247.5f..292.5f -> "W"
            in 292.5f..337.5f -> "NW"
            else -> "N"
        }

    fun formattedTime(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
}

object CaptureState {
    @Volatile var pendingMetadata: PhotoMetadata? = null
    @Volatile var pendingFile: File? = null

    fun set(file: File, metadata: PhotoMetadata) {
        pendingFile = file
        pendingMetadata = metadata
    }

    fun consume(): Pair<File?, PhotoMetadata?> {
        val f = pendingFile
        val m = pendingMetadata
        pendingFile = null
        pendingMetadata = null
        return f to m
    }
}
