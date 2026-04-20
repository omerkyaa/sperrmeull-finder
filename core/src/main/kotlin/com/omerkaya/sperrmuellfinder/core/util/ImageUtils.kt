package com.omerkaya.sperrmuellfinder.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Image processing utilities for photo handling
 * Rules.md compliant - Professional image optimization
 */
object ImageUtils {
    
    /**
     * Maximum image dimensions for profile photos
     */
    private const val MAX_WIDTH = 800
    private const val MAX_HEIGHT = 800
    private const val JPEG_QUALITY = 85
    
    /**
     * Process and compress image for profile photo upload
     * - Resizes to max dimensions
     * - Compresses to reduce file size
     * - Fixes orientation
     * @param context Application context
     * @param uri Image URI to process
     * @return Processed image as ByteArray
     */
    suspend fun processProfilePhoto(
        context: Context,
        uri: Uri
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.Error(IOException("Cannot open image"))
            
            // Decode image
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                ?: return@withContext Result.Error(IOException("Cannot decode image"))
            
            inputStream.close()
            
            // Fix orientation
            val rotatedBitmap = fixImageOrientation(context, uri, originalBitmap)
            
            // Resize image
            val resizedBitmap = resizeImage(rotatedBitmap, MAX_WIDTH, MAX_HEIGHT)
            
            // Compress to JPEG
            val compressedBytes = compressToJpeg(resizedBitmap, JPEG_QUALITY)
            
            // Clean up bitmaps
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
            originalBitmap.recycle()
            resizedBitmap.recycle()
            
            Result.Success(compressedBytes)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixImageOrientation(
        context: Context,
        uri: Uri,
        bitmap: Bitmap
    ): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap // No rotation needed
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap // Return original if orientation fix fails
        }
    }
    
    /**
     * Resize image while maintaining aspect ratio
     */
    private fun resizeImage(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Calculate scale factor
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height,
            1f // Don't upscale
        )
        
        if (scale >= 1f) {
            return bitmap // No resizing needed
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Compress bitmap to JPEG format
     */
    private fun compressToJpeg(
        bitmap: Bitmap,
        quality: Int
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Calculate sample size for efficient image loading
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Get image dimensions without loading full bitmap
     */
    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }
}