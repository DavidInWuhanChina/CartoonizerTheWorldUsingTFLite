package com.example.cartooo.fragments
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.contentValuesOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cartooo.ImageUtils
import com.example.cartooo.MainActivity.Companion.getOutputDirectory
import com.example.cartooo.R
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
/**
 * 一个简单的[Fragment]子类，使用CameraX捕获并保存照片
 */
class CameraFragment :Fragment(){
//    提供带有与实现无关的选项的静态配置
    private var preview:Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var bitmap:Bitmap? = null
    private var modelSpinner:Spinner? = null
    private var modelType:Int = 0

//    设备上的摄像头朝向与设备屏幕相同的方向
    private var lensFacing:Int = CameraSelector.LENS_FACING_FRONT
    private lateinit var outputDirectory: File
    val fromAlbum = 2
//    启动先前提交的有序关闭
//      *任务已执行，但不会接受新任务。
//      *如果调用已经关闭，则调用不会产生任何其他影响。
    private lateinit var cameraExecutor:ExecutorService
    companion object{
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.CHINA
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        when (requestCode) {
            fromAlbum -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let { uri ->
                        //将选择的图片显示
                        //将文件路径传递到下一个屏幕
                        val bitmap = activity?.contentResolver?.openFileDescriptor(uri, 'r'.toString())?.use {
                        BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                    }
                        val filePath = ImageUtils.saveBitmap(bitmap, photoFile)
                        val action = CameraFragmentDirections.actionCameraToSelf2cartoon(filePath, modelType)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }
    override fun onCreateView(

        inflater: LayoutInflater, container:ViewGroup?,
        saveINstanceState: Bundle?
    ): View? {
//填充该片段的布局

        return inflater.inflate(R.layout.fragment_camera, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AlbumBtn.setOnClickListener {
            //打开文件选择器
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            //指定只显示图片
            intent.type = "image/*"
            startActivityForResult(intent, fromAlbum)
        }
        //创建一个下拉列表以选择TensorFlow Lite模型
        modelSpinner = view.findViewById(R.id.model_spinner)
        ///创建适配器
        val  modelAdapter = ArrayAdapter.createFromResource(
            requireContext(), //内容
            R.array.tflite_models,  //下拉tflite 模型列表
            R.layout.spinner_item
        )   //spinner layer
//设置下拉列表资源布局
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

//        创建适配器
        model_spinner.adapter = modelAdapter

////设置项目选择的侦听器
        model_spinner.onItemSelectedListener = object :AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                modelType = position

            }

            override fun onNothingSelected(p0:AdapterView<*>?) {
                modelType = 0
            }

        }
        //        设置照相机转换
        camera_switch_button.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            }else{
                CameraSelector.LENS_FACING_FRONT
            }
            //重新绑定用例以更新选定的摄像机
            startCamera()
        }
//        照相按钮设置监听器
        camera_capture_button.setOnClickListener { takePhoto()}
        outputDirectory = getOutputDirectory(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()
    }


    private fun startCamera() {
//    当 ProcessCameraProvider 初始化完成会调用 listener
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
///        屏幕纵横比
        val screenAspectRatio  = 1.0/1.0
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")


        cameraProviderFuture.addListener(Runnable {
//            用于将摄像机的生命周期绑定到生命周期所有者
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//设置预览
            preview = Preview.Builder().build()


//        设置捕获
            imageCapture = ImageCapture.Builder()
//                    //CAPTURE_MODE_MAXIMIZE_QUALITY 拍摄高质量图片，图像质量优先于延迟，可能需要更长的时间
//					//CAPTURE_MODE_MINIMIZE_LATENCY
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(Surface.ROTATION_270)
                .setTargetRotation(viewFinder.display.rotation)
                .setTargetResolution(Size(512, 512)) //将目标分辨率设置为512x512
                .build()
//            选择前置摄像头作为默认自拍照
            val cameraSelector =
//                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                CameraSelector.Builder().requireLensFacing(lensFacing)
                    .build()        //将相机更换为正面

            try {
                //重新绑定之前取消绑定用例
                cameraProvider.unbindAll()

                //将用例绑定到相机
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
            }catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        },ContextCompat.getMainExecutor(requireContext()))

    }

    private fun takePhoto() {
//        获得有关可修改图像捕获用例的稳定参考
        val  imageCapture = imageCapture ?:return

        //创建带有时间戳的输出文件以保存图像
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.CHINA
            ).format(System.currentTimeMillis()) + ".jpg"
        )

//        创建包含文件+元数据的输出选项对象
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    //获取旋转角度
                    val degree: Int = rotationDegrees(photoFile)

                    //从jgp图像建立bitmap
                    bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                    //如果需要旋转图像
                    if (degree != 0) {
                        bitmap = rotateBitmap(bitmap!!, degree)
                    }

                    //保存bitmap图像
                    val filePath = ImageUtils.saveBitmap(bitmap, photoFile)

                    //将文件路径传递到下一个屏幕
                    val action = CameraFragmentDirections.actionCameraToSelf2cartoon(filePath, modelType)
                    findNavController().navigate(action)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

//     从图像exif获取旋转度
    private fun rotationDegrees(file: File): Int {
    val ei = ExifInterface(file.absolutePath);
    val orientation =
        ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
//根据exif的方向返回旋转角度
    return when (orientation){
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int):Bitmap {
        val rotationMatrix = Matrix()
        rotationMatrix.postRotate(rotationDegrees.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
//            释放C部分的内存
        bitmap.recycle()
        return rotatedBitmap
    }
    override fun onResume() {
        super.onResume()
//        确保所有权限仍被授予
        if (!PermissionsFragment.allPermissionsGranted(
                requireContext()
            )
        ){
            findNavController().navigate(R.id.action_camera_to_permissions)
        }
    }
}