package com.abdulrahman.littlesnap

import android.media.Image
import android.util.Log
import com.abdulrahman.littlesnap.utlities.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageSaver(private val image: Image, private val file: File) : Runnable {


    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var outputStream : FileOutputStream? = null
        try{
            outputStream = FileOutputStream(file).apply {
                write(bytes)
            }

        }catch (e : IOException){

            Log.d(TAG," ImageSaver throw exception ${e.message}")
        }
        finally {
            image.close()
            outputStream?.let {
                try{
                    it.close()
                }catch (e :IOException){
                    Log.d(TAG,"finally  ImageSaver throw exception ${e.message}")
                }
            }
        }

    }
}