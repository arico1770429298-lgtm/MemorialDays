package com.example.memorialdays.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.example.memorialdays.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Off-screen poster rendering. This runs entirely on Dispatchers.IO and uses
 * the platform Canvas rather than a Compose screenshot: a ComposeView must be
 * measured and drawn on the main thread, which would violate the no-main-thread
 * constraint for a 1080x1440 bitmap.
 */
class PosterRenderer(context: Context) {

    private val appContext = context.applicationContext

    fun render(data: PosterData, style: PosterStyle): Result<PosterShare> = runCatching {
        cleanupOld()
        val directory = File(appContext.cacheDir, POSTER_DIR).apply { mkdirs() }
        val file = File(directory, "memorial-${System.currentTimeMillis()}.png")
        val bitmap = createBitmap(WIDTH, HEIGHT)
        try {
            val canvas = Canvas(bitmap)
            drawPoster(canvas, data, style)
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "poster encode failed"
                }
            }
        } finally {
            bitmap.recycle()
        }
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
        PosterShare(uri = uri, file = file)
    }

    fun cleanup(file: File) {
        runCatching {
            if (file.exists()) file.delete()
        }
    }

    fun cleanupOld() {
        runCatching {
            val directory = File(appContext.cacheDir, POSTER_DIR)
            val cutoff = System.currentTimeMillis() - MAX_AGE_MILLIS
            directory.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) file.delete()
            }
        }
    }

    private fun drawPoster(canvas: Canvas, data: PosterData, style: PosterStyle) {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                HEIGHT.toFloat(),
                style.background,
                ColorUtils.blendARGB(style.background, style.accent, 0.10f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), backgroundPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.muted
            textSize = 30f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.18f
        }
        canvas.drawText(
            appContext.getString(R.string.brand_name).uppercase(Locale.ROOT),
            MARGIN,
            120f,
            brandPaint
        )

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (data.isToday) style.accent else style.muted
            textSize = 38f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText(data.status, MARGIN, 540f, statusPaint)

        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (data.isToday) style.accent else style.text
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textSize = 320f
        }
        var numberSize = 320f
        while (numberPaint.measureText(data.number) > WIDTH * 0.84f && numberSize > 120f) {
            numberSize -= 10f
            numberPaint.textSize = numberSize
        }
        canvas.drawText(data.number, MARGIN, 900f, numberPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.text
            textSize = 56f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val titleLayout = StaticLayout.Builder.obtain(
            data.title,
            0,
            data.title.length,
            titlePaint,
            (WIDTH - MARGIN * 2).toInt()
        )
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.withTranslation(MARGIN, 980f) {
            titleLayout.draw(this)
        }

        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.text
            textSize = 42f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(data.primaryDate, MARGIN, 1270f, primaryPaint)

        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.muted
            textSize = 30f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(data.secondaryDate, MARGIN, 1320f, secondaryPaint)

        val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.muted
            textSize = 28f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText(data.type, MARGIN, 1380f, typePaint)
    }

    companion object {
        private const val POSTER_DIR = "posters"
        private const val WIDTH = 1080
        private const val HEIGHT = 1440
        private const val MARGIN = 80f
        private const val MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L
    }
}
