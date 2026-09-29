package com.friday.mistakenotebook.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUtil @Inject constructor() {

    companion object {
        // OCR 送检图片的最大边长：超过会显著增大上传体积且识别率无明显提升
        private const val MAX_DIMENSION = 2048
        private const val JPEG_QUALITY = 80
    }

    /**
     * 将图片 URI 转换为 Base64 字符串。
     * 先按 MAX_DIMENSION 计算 inSampleSize 降载解码，再矫正 EXIF 方向，
     * 最后精确缩放并压缩，避免大图导致 OOM、上传超时或识别率下降。
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            // 第一遍：只读尺寸，不解码像素
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // 第二遍：按采样率解码，控制内存
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val rotated = applyExifRotation(context, uri, bitmap)
            val scaled = scaleDown(rotated, MAX_DIMENSION)
            bitmapToBase64(scaled)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var maxSide = maxOf(width, height)
        while (maxSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * 按照片的 EXIF 方向信息旋转 Bitmap（decodeStream 不处理 EXIF，竖拍会横着送识别）
     */
    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    /**
     * 将 Bitmap 转换为 Base64 字符串
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * 压缩图片
     */
    fun compressBitmap(bitmap: Bitmap, maxWidth: Int = 1920, maxHeight: Int = 1080): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight, 1f)

        return if (scale < 1f) {
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    /**
     * 保存图片到本地
     */
    fun saveImageToLocal(context: Context, bitmap: Bitmap, filename: String): String? {
        return try {
            val file = File(context.filesDir, "images")
            if (!file.exists()) {
                file.mkdirs()
            }

            val imageFile = File(file, filename)
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            fos.flush()
            fos.close()

            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从本地加载图片
     */
    fun loadLocalImage(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(path)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
