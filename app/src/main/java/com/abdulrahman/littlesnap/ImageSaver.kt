package com.abdulrahman.littlesnap

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import com.abdulrahman.littlesnap.callbacks.SaveImageCallback
import com.abdulrahman.littlesnap.utlities.PIC_FILE_NAME
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ImageSaver"


class ImageSaver : Runnable {

    var image: Image? = null
    var file: File
    var callback: SaveImageCallback? = null
    var bitmap: Bitmap? = null

    constructor(image: Image?, file: File, callback: SaveImageCallback?) {
        this.image = image
        this.file = file
        this.callback = callback
    }

    constructor(bitmap: Bitmap, file: File, callback: SaveImageCallback?) {
        this.bitmap = bitmap
        this.file = file
        this.callback = callback
    }


    override fun run() {
        if (image != null) {
            val buffer = image!!.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            var outputStream: FileOutputStream? = null
            try {
                val f = File(file, PIC_FILE_NAME)
                outputStream = FileOutputStream(f).apply {
                    write(bytes)
                }

            } catch (e: IOException) {
                Log.d(TAG, " ImageSaver throw exception ${e.message}")
                callback!!.done(e)
            } finally {
                image?.close()
                outputStream?.let {
                    try {
                        it.close()
                    } catch (e: IOException) {
                        Log.d(TAG, "finally  ImageSaver throw exception ${e.message}")
                    }
                }

                callback!!.done(null)
            }

        }
        //Save image to disk after draw
        else if (bitmap != null) {
            var stream: ByteArrayOutputStream? = null
            var imageByteArray: ByteArray? = null
            stream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            imageByteArray = stream.toByteArray()
            val dateFormate = SimpleDateFormat("ddMMyyyyhhmmss")
            val formate = dateFormate.format(Date())
            val f = File(file, "_image$formate.jpg")
            //Save mirrored array
            var output: FileOutputStream? = null
            try {
                output = FileOutputStream(f)
                output.write(imageByteArray)
            } catch (e: IOException) {
                callback?.done(e)
            } finally {
                if (null != output) {
                    try {
                        output.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callback?.done(null)
            }
        }
    }
}
