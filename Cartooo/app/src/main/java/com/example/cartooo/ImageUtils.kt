package com.example.cartooo

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class ImageUtils {
    companion object {
        fun saveBitmap(bitmap: Bitmap?, file: File): String {
            try {
                val stream: OutputStream = FileOutputStream(file)
                // Bitmap.CompressFormat format 图像的压缩格式；
                //int quality 图像压缩率，0-100。 0 压缩100%，100意味着不压缩；
                //OutputStream stream 写入压缩数据的输出流；
                //返回值
                //如果成功地把压缩数据写入输出流，则返回true。
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                stream.flush()
                stream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return file.absolutePath
        }
    }
}