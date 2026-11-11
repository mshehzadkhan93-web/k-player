package com.captain.kplayer.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.Parameters
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.ui.theme.CyanAccent
import com.captain.kplayer.ui.theme.PinkAccent
import com.captain.kplayer.ui.theme.VioletAccent

enum class ArtKind { VIDEO, MUSIC, FOLDER, STREAM }

private val PALETTES = listOf(
    Pair(Color(0xFF0EA5E9), Color(0xFF7C3AED)),
    Pair(Color(0xFFEC4899), Color(0xFFF97316)),
    Pair(Color(0xFF22D3EE), Color(0xFF3B82F6)),
    Pair(Color(0xFFA855F7), Color(0xFFEC4899)),
    Pair(Color(0xFF10B981), Color(0xFF0EA5E9)),
    Pair(Color(0xFFF59E0B), Color(0xFFEF4444)),
    Pair(Color(0xFF6366F1), Color(0xFF22D3EE)),
    Pair(Color(0xFF14B8A6), Color(0xFF84CC16))
)

private object ArtCache {
    // 32 entries max: a 512px ARGB_8888 bitmap is ~1 MB, so the old
    // 64-entry cap could hold ~64 MB of placeholders. 32 (~32 MB)
    // still covers every visible row plus scroll-ahead.
    private val cache = LinkedHashMap<String, Bitmap>(32, 0.75f, true)
    fun get(key: String): Bitmap? = synchronized(this) { cache[key] }
    fun put(key: String, bitmap: Bitmap) = synchronized(this) {
        if (cache.size > 32) {
            val oldest = cache.keys.firstOrNull()
            if (oldest != null) cache.remove(oldest)
        }
        cache[key] = bitmap
    }
}

private fun drawArtBitmap(key: String, kind: ArtKind, size: Int): Bitmap {
    val palette = PALETTES[(key.hashCode().and(Int.MAX_VALUE)) % PALETTES.size]
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val gradient = LinearGradient(
        0f, 0f, size.toFloat(), size.toFloat(),
        palette.first.toArgb(), palette.second.toArgb(), Shader.TileMode.CLAMP
    )
    val bgPaint = Paint().apply { shader = gradient }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    // Soft diagonal sheen.
    val sheenPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        alpha = 18
    }
    val sheen = Path().apply {
        moveTo(0f, size.toFloat())
        lineTo(size * 0.55f, 0f)
        lineTo(size.toFloat(), 0f)
        lineTo(size.toFloat(), size * 0.45f)
        close()
    }
    canvas.drawPath(sheen, sheenPaint)

    val glyphPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        alpha = 235
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val center = size / 2f
    when (kind) {
        ArtKind.VIDEO -> {
            val triangle = Path().apply {
                val w = size * 0.30f
                val h = size * 0.36f
                moveTo(center - w * 0.4f, center - h / 2)
                lineTo(center + w * 0.6f, center)
                lineTo(center - w * 0.4f, center + h / 2)
                close()
            }
            canvas.drawPath(triangle, glyphPaint)
        }
        ArtKind.MUSIC -> {
            glyphPaint.textSize = size * 0.5f
            glyphPaint.textAlign = Paint.Align.CENTER
            val metrics = glyphPaint.fontMetrics
            val y = center - (metrics.ascent + metrics.descent) / 2
            canvas.drawText("\u266A", center, y, glyphPaint)
        }
        ArtKind.FOLDER -> {
            val folder = Path().apply {
                val left = size * 0.24f
                val top = size * 0.30f
                val right = size * 0.76f
                val bottom = size * 0.70f
                moveTo(left, top + size * 0.08f)
                lineTo(left + size * 0.18f, top)
                lineTo(left + size * 0.32f, top)
                lineTo(left + size * 0.36f, top + size * 0.08f)
                lineTo(right, top + size * 0.08f)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
            canvas.drawPath(folder, glyphPaint)
        }
        ArtKind.STREAM -> {
            glyphPaint.style = Paint.Style.STROKE
            glyphPaint.strokeWidth = size * 0.045f
            glyphPaint.strokeCap = Paint.Cap.ROUND
            canvas.drawCircle(center, center, size * 0.09f, glyphPaint.apply { style = Paint.Style.FILL })
            glyphPaint.style = Paint.Style.STROKE
            canvas.drawArc(center - size * 0.20f, center - size * 0.20f, center + size * 0.20f, center + size * 0.20f, -55f, 110f, false, glyphPaint)
            canvas.drawArc(center - size * 0.32f, center - size * 0.32f, center + size * 0.32f, center + size * 0.32f, -55f, 110f, false, glyphPaint)
        }
    }
    return bitmap
}

/** Returns a cached gradient placeholder bitmap for a library item. */
fun artBitmap(key: String, kind: ArtKind, size: Int = 512): Bitmap {
    val cacheKey = "$kind:$key:$size"
    return ArtCache.get(cacheKey) ?: drawArtBitmap(key, kind, size).also {
        ArtCache.put(cacheKey, it)
    }
}

/**
 * Thumbnail image drawn entirely in code (gradient + icon glyph) and loaded
 * through Coil, so no network images are ever required.
 *
 * [sizePx] is the bitmap edge in pixels: pass the size the image is
 * actually displayed at instead of the 512px default, so a ~100dp list
 * row does not hold a 1 MB bitmap. The bitmap is cached per
 * (key, kind, size), so different sizes do not thrash each other.
 */
@Composable
fun ArtImage(
    key: String,
    kind: ArtKind,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    isVideo: Boolean = false,
    sizePx: Int = 512
) {
    val bitmap = remember(key, kind, sizePx) { artBitmap(key, kind, sizePx) }
    val request = ImageRequest.Builder(LocalContext.current)
        .data(bitmap)
        .crossfade(true)
        .build()
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .then(if (isVideo) Modifier.aspectRatio(16f / 9f) else Modifier)
            .clip(RoundedCornerShape(16.dp))
    )
}

/**
 * Real video thumbnail: extracts an actual frame from the video with
 * Coil's [VideoFrameDecoder] (works for local content:// files from
 * MediaStore and for remote URLs). The gradient [ArtImage] is used ONLY
 * as a fallback when no frame can be extracted (unsupported file, missing
 * file, decode error).
 *
 * Scroll performance: the frame is decoded at exactly the size it is
 * displayed at ([widthPx] x [heightPx]) instead of full resolution — a
 * 4K frame decoded for a ~100dp grid cell was the main scroll jank
 * source — and the request is remembered, so the memory-cache key
 * (data + size + frame time) stays stable across recompositions and a
 * decoded frame is reused instead of re-running MediaMetadataRetriever
 * on every bind while scrolling. The frame is taken at 1s in, so the
 * thumbnail is a real scene instead of a black first frame.
 * [widthPx]/[heightPx] defaults cover the 3-column grid (~100dp cells)
 * and the 128dp list rows; the 220dp-wide Continue/Recent cards pass
 * the larger size explicitly.
 */
@Composable
fun RealThumbnail(
    video: VideoItem,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    widthPx: Int = 320,
    heightPx: Int = 180
) {
    var frameFailed by remember(video.id, video.url) { mutableStateOf(false) }
    if (frameFailed) {
        ArtImage(
            key = video.thumbKey,
            kind = ArtKind.VIDEO,
            modifier = modifier,
            contentDescription = contentDescription,
            isVideo = true,
            // Fallback placeholder at the same size the frame was
            // requested at, not the 512px default.
            sizePx = maxOf(widthPx, heightPx)
        )
        return
    }
    val context = LocalContext.current
    val request = remember(video.url, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(video.url)
            .decoderFactory(VideoFrameDecoder.Factory())
            .size(widthPx, heightPx)
            .parameters(
                Parameters.Builder()
                    .set(VideoFrameDecoder.VIDEO_FRAME_MICROS_KEY, 1_000_000L)
                    .build()
            )
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp)),
        onError = { frameFailed = true }
    )
}
