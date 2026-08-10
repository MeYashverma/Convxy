/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bakes the home background's blur into a cached file so the runtime draw is a plain
 * bitmap instead of a live `Modifier.blur`.
 *
 * The live blur is a RenderEffect on a layer that sits under the whole app, so it is
 * re-run by the GPU on every frame that replays it — including every scroll frame,
 * where it competes with the glass surfaces for the same budget. The image and the
 * blur radius are both static, so that work is the same pixels recomputed forever.
 *
 * An earlier attempt to bake this through a Coil `Transformation` rendered unblurred
 * on-device regardless of cache-key/hardware-bitmap fixes (see [HomeImageBackground]).
 * This path deliberately does not involve Coil: it decodes, resamples and writes a
 * file itself, and the caller falls back to the live blur until that file exists, so
 * the failure mode of the previous attempt (a sharp image) cannot reappear.
 *
 * The blur itself is successive bilinear resampling rather than a true gaussian:
 * heavy downscale destroys the high frequencies, and stepping back up in halves
 * smooths the interpolation seams. At these radii it is visually indistinguishable
 * from the RenderEffect blur it replaces, and it runs once per (image, radius).
 *
 * ponytail: resample approximation, swap for RenderEffect-into-bitmap (API 31+) if a
 * radius ever lands where the seams show.
 */
object HomeBackgroundBlurCache {

    /** Width the baked file is stored at; the runtime upscale from here is smooth. */
    private const val BakedWidth = 320

    /** Cap on decode size, so a 4000px pick doesn't allocate a huge source bitmap. */
    private const val MaxDecodeWidth = 1080

    /**
     * The baked file for this image and radius, generating it if absent.
     * Returns null if the source can't be decoded — caller keeps the live blur.
     */
    suspend fun get(context: Context, path: String, blurDp: Float): File? =
        withContext(Dispatchers.IO) {
            val source = File(path)
            if (!source.exists()) return@withContext null

            // Keyed on mtime too: picking a new image can reuse the same path.
            val key = "${path.hashCode()}_${source.lastModified()}_${blurDp.toInt()}"
            val cached = File(context.cacheDir, "homebg_blur/$key.png")
            if (cached.exists()) return@withContext cached

            val baked = runCatching { bake(source, blurDp) }.getOrNull() ?: return@withContext null
            runCatching {
                cached.parentFile?.mkdirs()
                // Written to a temp name first: a torn file from a killed process would
                // otherwise be cached forever under a key that says it is complete.
                val tmp = File(cached.parentFile, "${cached.name}.tmp")
                tmp.outputStream().use { baked.compress(Bitmap.CompressFormat.PNG, 100, it) }
                tmp.renameTo(cached)
                prune(cached.parentFile, keep = cached)
            }.getOrElse { return@withContext null }

            cached.takeIf { it.exists() }
        }

    private fun bake(source: File, blurDp: Float): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decoded = BitmapFactory.decodeFile(
            source.path,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, MaxDecodeWidth)
            },
        ) ?: return null

        val aspect = decoded.height.toFloat() / decoded.width

        // Radius drives how far down we go: the smaller the intermediate, the more
        // detail is gone when it comes back up. Floor of 8px keeps a 1dp radius sane.
        val shrunkWidth = (BakedWidth / blurDp.coerceAtLeast(1f) * 4f).toInt().coerceIn(8, BakedWidth)
        var step = scaled(decoded, shrunkWidth, aspect)
        if (step !== decoded) decoded.recycle()

        // Back up in doublings — each bilinear pass smooths the one before it.
        var width = shrunkWidth
        while (width < BakedWidth) {
            width = (width * 2).coerceAtMost(BakedWidth)
            val next = scaled(step, width, aspect)
            if (next !== step) step.recycle()
            step = next
        }
        return step
    }

    private fun scaled(bitmap: Bitmap, width: Int, aspect: Float): Bitmap {
        val height = (width * aspect).toInt().coerceAtLeast(1)
        if (bitmap.width == width && bitmap.height == height) return bitmap
        return Bitmap.createScaledBitmap(bitmap, width, height, /* filter = */ true)
    }

    internal fun sampleSizeFor(sourceWidth: Int, target: Int): Int {
        var sample = 1
        while (sourceWidth / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Only the current bake is ever needed; older radii/images are dead weight. */
    private fun prune(dir: File?, keep: File) {
        dir?.listFiles()?.forEach { if (it != keep) it.delete() }
    }
}
