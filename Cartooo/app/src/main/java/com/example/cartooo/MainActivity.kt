package com.example.cartooo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.graphics.drawable.DrawableContainer
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)
//        setSupportActionBar(toolbar)
    }


    companion object {
        /** 使用外部媒体（如果可用），否则使用我们应用程序的文件目录 */
        fun  getOutputDirectory(context: Context):File {
            val appContext = context.applicationContext
            val mediaDir =
                context.externalMediaDirs.firstOrNull()?.let {
                    File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }
}