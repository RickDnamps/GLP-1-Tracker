package com.rickdnamps.glp1tracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/**
 * Progress photos are copied into the app's private storage (never the public gallery),
 * downscaled to save space, and stay on the device.
 */
object PhotoStore {
    private const val MAX_SIDE = 1600

    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, fileName: String) = File(dir(context), fileName)

    /** Copies [uri] (from the photo picker or camera) as a JPEG and returns the file name. */
    fun import(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2
        val decoded = resolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Unreadable image")
        val rotation = runCatching {
            resolver.openInputStream(uri)!!.use { ExifInterface(it).rotationDegrees }
        }.getOrDefault(0)
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val name = "${UUID.randomUUID()}.jpg"
        file(context, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return name
    }

    fun load(context: Context, fileName: String, maxSide: Int = 1200): Bitmap? {
        val f = file(context, fileName)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
