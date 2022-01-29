package com.example.deteksiretakbetonv1

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.deteksiretakbetonv1.Class.Classifier
import com.example.deteksiretakbetonv1.databinding.ActivityUsingCameraxBinding
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class UsingCameraActivity: AppCompatActivity() {

    private lateinit var classifier: Classifier
    lateinit var binding: ActivityUsingCameraxBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory : File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var modelPath: String
    private var inputSize = 0
    private var KEY_INPUT_SIZE = "INPUT_SIZE"
    private var KEY_TFLITE = "TFLITE"
    private var uriImage: Uri? = null
    private var bitmapImage: Bitmap? =null
    private var lastProcessingTimeMs: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsingCameraxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE)
        }

        val actionBar = supportActionBar

        actionBar!!.title = " Deteksi Dengan Camera"

        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            modelPath = bundle.getString(KEY_TFLITE).toString()
            inputSize = bundle.getInt(KEY_INPUT_SIZE)
//            myInfo("anda menggunakan arsitektur : " + modelPath)
        }





        binding.capture.setOnClickListener {
            takePhoto()
        }

        binding.again.setOnClickListener{
            binding.captureView.visibility = View.GONE
            binding.cameraPreview.visibility = View.VISIBLE
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()


        val recordFiles = ContextCompat.getExternalFilesDirs(this, Environment.DIRECTORY_MOVIES)
        val storageDirectory = recordFiles[0]


    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun takePhoto() {

//        val imageCapture =ImageCapture.Builder()
//            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
//            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//            .build()

        val photoFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object: ImageCapture.OnImageSavedCallback{
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    binding.captureView.visibility = View.VISIBLE
                    binding.captureView.setImageURI(savedUri)
                    binding.cameraPreview.visibility = View.GONE

//                    Toast.makeText(baseContext, "Photo Captured Succeeded: $savedUri",Toast.LENGTH_SHORT).show()
                }
            }
        )


        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(
                    image: ImageProxy
                ) {
//                    lgd("Image format: ${image.format}")
                    // show captured image
                    val uriImage = Uri.fromFile(photoFile)

                    bitmapImage = imageProxyToBitmap(image)

//                    binding.captureView.visibility = View.VISIBLE
//                    binding.captureView.setImageBitmap(bitmapImage)
//                    binding.captureView.setImageURI(uriImage)
//                    binding.cameraPreview.visibility = View.GONE
                    val nameFile = uriImage?.path.toString()
                    val file = File(nameFile)
                    val fileName = file.name
                    myInfo(fileName)

                    val startTime = SystemClock.uptimeMillis()
                    classifier = Classifier(assets, modelPath, labelPath, inputSize)
                    val resultClass = classifier.recognizeImage(bitmapImage!!)
                    val output = resultClass.component1()
                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                    val waktu = lastProcessingTimeMs.toString()
                    val outputText = "Nama file = " + fileName + "\n" + output.toString() + "\nprocess Time = " + waktu + " ms"
                    binding.result.text = outputText
                    myInfo("klasifikasi berhasil....")

                    try{
                        val path = applicationContext.externalCacheDir
                        val historyDirectory = File(path, "history_camera")
                        historyDirectory.mkdirs()
                        val file = File(historyDirectory, modelPath+"_history_"+ SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) +".txt")
                        val writer = FileWriter(file)
                        writer.appendLine(outputText+ "\n")
                        writer.flush()
                        writer.close()
//
                    }catch (e: Exception){
                        Toast.makeText(this@UsingCameraActivity,"write file history error",Toast.LENGTH_SHORT).show()
                    }
                    image.close()
                    // show panel
//                    showSavePanel(true)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            })
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy: ImageProxy.PlaneProxy = image.planes[0]

        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable{

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val Preview = Preview.Builder().build().also{
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector,Preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionGranted(): Boolean = Companion.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED

    }
    fun myInfo(msg: String){
        Log.d(TAG,msg)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object{
        val TAG = "DRB.UCA"
        private val labelPath = "label.txt"
        private const val FILENAME_FORMAT = "yyyyMMdd-HHmmss"
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        val REQUEST_PERMISSIONS_CODE = 100
    }
}



























