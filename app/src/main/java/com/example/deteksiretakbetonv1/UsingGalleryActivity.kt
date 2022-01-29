package com.example.deteksiretakbetonv1

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.deteksiretakbetonv1.Class.Classifier
import com.example.deteksiretakbetonv1.databinding.ActivityUsingGalleryBinding
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates




class UsingGalleryActivity:AppCompatActivity() {

    private lateinit var classifier: Classifier

    lateinit var binding: ActivityUsingGalleryBinding
    private lateinit var modelPath: String
    private lateinit var imageBitmap: Bitmap
    private var inputSize = 0
    private var KEY_INPUT_SIZE = "INPUT_SIZE"
    private var KEY_TFLITE = "TFLITE"
    private var imageUri: Uri? = null
    private var lastProcessingTimeMs: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsingGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar

        actionBar!!.title = "Deteksi Dengan Gallery"

        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            modelPath = bundle.getString(KEY_TFLITE).toString()
            inputSize = bundle.getInt(KEY_INPUT_SIZE)
//            myInfo("anda menggunakan arsitektur : " + modelPath)
        }


        binding.importGallery.setOnClickListener {
//            myInfo("import Clicked")
            val gallery = Intent(Intent.ACTION_PICK)
            gallery.type = "image/*"
            startActivityForResult(gallery, PICK_IMAGE_CODE)

        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        myInfo("on activity result entered")
        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE_CODE){
            imageUri = data?.data
//            myInfo("get imageUri")
            binding.imageView.setImageURI(imageUri)
            imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

            val nameFile = imageUri?.path.toString()
            val file = File(nameFile)
            val fileName = file.name
            binding.result.text = "Nama file = " + fileName

            binding.detect.setOnClickListener {
                val startTime = SystemClock.uptimeMillis()
                classifier = Classifier(assets, modelPath, labelPath, inputSize)
                val resultClass = classifier.recognizeImage(imageBitmap)
                val output = resultClass.component1()
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                val waktu = lastProcessingTimeMs.toString()
                val outputText = "Nama file = " + fileName + "\n" + output.toString() + "\nprocess Time = " + waktu + " ms"
                binding.result.text = outputText
                myInfo("klasifikasi berhasil....")
//                myInfo(output.toString())


                try{
                    val path = applicationContext.externalCacheDir
                    val historyDirectory = File(path, "history_gallery")
                    historyDirectory.mkdirs()
                    val file = File(historyDirectory, modelPath+"_history_"+ fileName +".txt")
                    val writer = FileWriter(file)
                    writer.appendLine(outputText)
                    writer.flush()
                    writer.close()
//
                }catch (e: Exception){
                    Toast.makeText(this,"write file history error",Toast.LENGTH_SHORT).show()
                }

            }
        }
    }



    fun myInfo(msg: String){
        Log.d(TAG,msg)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun printOut(msg: String, value: Any){
        Log.i(TAG,msg.toString())
        val msgC = msg + value
        Toast.makeText(this, msgC.toString(), Toast.LENGTH_SHORT).show()
    }

    companion object{
        val TAG = "DRB.UGA"
        var PICK_IMAGE_CODE = 100
        private val labelPath = "label.txt"

    }
}