package com.siren.player.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import androidx.palette.graphics.Palette

/**
 * 从专辑封面提取的颜色集合
 */
data class AlbumColors(
    val dominant: Color?,
    val vibrant: Color?,
    val darkVibrant: Color?,
    val lightVibrant: Color?,
    val muted: Color?,
    val darkMuted: Color?,
    val lightMuted: Color?
)

/**
 * 从专辑封面提取多种颜色。
 */
suspend fun extractColorsFromImage(context: android.content.Context, url: String): AlbumColors? {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(200, 200)
            .scale(Scale.FILL)
            .allowHardware(false)
            .build()

        val result = loader.execute(request)
        if (result !is SuccessResult) return null

        val drawable = result.drawable
        val bitmap: Bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> {
                val b = android.graphics.Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(b)
                drawable.setBounds(0, 0, b.width, b.height)
                drawable.draw(canvas)
                b
            }
        }

        val palette = Palette.from(bitmap).generate()
        AlbumColors(
            dominant = palette.getDominantSwatch()?.rgb?.let { Color(it) },
            vibrant = palette.getVibrantSwatch()?.rgb?.let { Color(it) },
            darkVibrant = palette.getDarkVibrantSwatch()?.rgb?.let { Color(it) },
            lightVibrant = palette.getLightVibrantSwatch()?.rgb?.let { Color(it) },
            muted = palette.getMutedSwatch()?.rgb?.let { Color(it) },
            darkMuted = palette.getDarkMutedSwatch()?.rgb?.let { Color(it) },
            lightMuted = palette.getLightMutedSwatch()?.rgb?.let { Color(it) }
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Composable that remembers colors extracted from album art.
 */
@Composable
fun rememberAlbumColors(coverUrl: String?): AlbumColors? {
    val context = LocalContext.current
    val colorsState = produceState<AlbumColors?>(initialValue = null, coverUrl) {
        value = if (coverUrl != null) {
            extractColorsFromImage(context, coverUrl)
        } else {
            null
        }
    }
    return colorsState.value
}
