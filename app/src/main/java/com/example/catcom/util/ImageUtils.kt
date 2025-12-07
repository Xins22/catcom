package com.example.catcom.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    /**
     * Mengompres gambar dari Uri menjadi ByteArray (JPEG, Quality 60).
     * Siap diupload ke Firebase Storage.
     */
    fun compressImage(context: Context, uri: Uri): ByteArray? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            val outputStream = ByteArrayOutputStream()
            // Kompresi ke JPEG dengan kualitas 60%
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            inputStream?.close()
        }
    }
}
