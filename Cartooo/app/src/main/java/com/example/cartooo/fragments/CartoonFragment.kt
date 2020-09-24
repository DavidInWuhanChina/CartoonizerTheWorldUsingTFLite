package com.example.cartooo.fragments

import android.content.Intent
import android.content.Context
import androidx.fragment.app.Fragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.cartooo.ImageUtils
import com.example.cartooo.MainActivity
import com.example.cartooo.R
import com.example.cartooo.ml.WhiteboxCartoonGanDr
import com.example.cartooo.ml.WhiteboxCartoonGanFp16
import com.example.cartooo.ml.WhiteboxCartoonGanInt8
import kotlinx.android.synthetic.main.fragment_selfie2cartoon.*
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.io.File
import java.text.SimpleDateFormat
import java.util.*



//展示捕获的输入图像和由tflite模型卡通化的输出图像
class CartoonFragment :Fragment(){

    private val args: CartoonFragmentArgs by navArgs()
    private lateinit var filePath: String
    private var modelType: Int = 0

    private val parentJob = Job()
//    协程
    private val coroutineScope = CoroutineScope(
    Dispatchers.Main + parentJob
)
    private fun  getOutputAsync(bitmap: Bitmap): Deferred<Pair<Bitmap, Long>> =
//使用async（）在IO优化的分派器中创建协程以进行模型推断
    coroutineScope.async(Dispatchers.IO){
//GPU代理
        val option = Model.Options.Builder()
            .setDevice(Model.Device.GPU)
            .setNumThreads(4)
            .build()
//        输入
        val sourceImage = TensorImage.fromBitmap(bitmap)

//        输出
        val cartoonizedImage: TensorImage
        val  startTime = SystemClock.uptimeMillis()
        when (modelType){
            0 -> cartoonizedImage = inferenceWithDrModel(sourceImage)       //DR
            1 -> cartoonizedImage = inferenceWithFp16Model(sourceImage)     //Fp16
            2 -> cartoonizedImage = inferenceWithInt8Model(sourceImage, option) //Int8
            else -> cartoonizedImage = inferenceWithDrModel(sourceImage)
//        此推断时间包括预处理和后处理

        }
        val inferenceTime = SystemClock.uptimeMillis() - startTime
        val cartoonizedImageBitmap = cartoonizedImage.bitmap

        return@async Pair(cartoonizedImageBitmap, inferenceTime)
    }
//使用动态范围tflite模型进行推断
    private fun inferenceWithDrModel(sourceImage: TensorImage): TensorImage {
    val model = WhiteboxCartoonGanDr.newInstance(requireContext())

//    运行模型推断并获取结果。
    val outputs = model.process(sourceImage)
    val cartoonizedImage = outputs.cartoonizedImageAsTensorImage

//如果不再使用，则释放模型资源。
    model.close()

    return cartoonizedImage
}

//    使用fp16 tflite模型进行推断
    private fun inferenceWithFp16Model(sourceImage: TensorImage): TensorImage {
    val model = WhiteboxCartoonGanFp16.newInstance(requireContext())

    //    运行模型推断并获取结果。
    val outputs = model.process(sourceImage)
    val cartoonizedImage = outputs.cartoonizedImageAsTensorImage

    //如果不再使用，则释放模型资源。
    model.close()

    return cartoonizedImage
}

    //    使用int8 tflite模型进行推断
    private fun inferenceWithInt8Model(
        sourceImage: TensorImage,
        options:Model.Options
    ): TensorImage {
        val model = WhiteboxCartoonGanInt8.newInstance(requireContext(), options)

        //    运行模型推断并获取结果。
        val outputs = model.process(sourceImage)
        val cartoonizedImage = outputs.cartoonizedImageAsTensorImage

        //如果不再使用，则释放模型资源。
        model.close()

        return cartoonizedImage
    }
    private fun updateUI(outputBitmap: Bitmap, inferenceTime:Long)  {
        prograssbar.visibility = View.GONE
        imageview_output ?.setImageBitmap(outputBitmap)
        inference_info.setText("推断时间: " + inferenceTime.toString() + "ms")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setHasOptionsMenu(true) //启用工具栏

        retainInstance = true  //保留实例
        filePath = args.rootDir
        modelType = args.modelType

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        填充此片段的布局
        return inflater.inflate(R.layout.fragment_selfie2cartoon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val photoFile = File(filePath)

        Glide.with(imageview_input.context)
            .load(photoFile)
            .into(imageview_input)

        val selfieBitmap = BitmapFactory.decodeFile(filePath)
        coroutineScope.launch(Dispatchers.Main) {
            val (outputBitmap , inferenceTime) = getOutputAsync(selfieBitmap).await()
            updateUI(outputBitmap, inferenceTime)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//清理协程任务
        parentJob.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveCartoon()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveCartoon():String {
        val cartoonBitmap = imageview_output.drawable.toBitmap()
        val file = File(
            MainActivity.getOutputDirectory(requireContext()),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.CHINA
            ).format(System.currentTimeMillis()) + "_cartoon.jgp")
        ImageUtils.saveBitmap(cartoonBitmap, file)
        Toast.makeText(context, "已被保存至" + file.absolutePath.toString(), Toast.LENGTH_SHORT).show()

        return file.absolutePath
    }

    companion object {
        private const val TAG = "CartoonFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }


}