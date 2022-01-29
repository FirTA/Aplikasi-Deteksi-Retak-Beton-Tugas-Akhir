package com.example.deteksiretakbetonv1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.deteksiretakbetonv1.databinding.ActivityMainBinding
import java.io.File


data class Tflite(var TFLITE_SELECT: String, var TFLITE_SELECT_SIZE: Int)

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var KEY_TFLITE = "TFLITE"
    private var KEY_INPUT_SIZE = "INPUT_SIZE"
    private var KEY_HISTORY_DIRECTORY = "100"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tflite = resources.getStringArray(R.array.tflite_list)

        // create ArrayAdapter
        val adapter = ArrayAdapter.createFromResource(this,R.array.tflite_list,android.R.layout.simple_spinner_item)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        binding.spinner.adapter = adapter

        val intentCamera = Intent(this@MainActivity, UsingCameraActivity::class.java)
        val intentGallery = Intent(this@MainActivity, UsingGalleryActivity::class.java)


        val path = applicationContext.externalCacheDir
        val historyDirectory = File(path, "history_camera")
        historyDirectory.mkdirs()

        try{
            val filename = "logcat_" + System.currentTimeMillis() + ".txt"
            val outputFile = File(applicationContext.externalCacheDir, filename)
            Runtime.getRuntime().exec(("logcat_ -f" + outputFile.absolutePath))
        }catch(e: Exception){

        }

//        Toast.makeText(this, historyDirectory.toString(),Toast.LENGTH_SHORT).show()


        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                var TFLITE_SELECT = tflite[position]
                var TFLITE_SELECT_SIZE = tflite[position]
                var inputSize = 0


                when(TFLITE_SELECT){
                    "MobileNet" -> TFLITE_SELECT = "TA_model_crack_concrete_MobileNet.tflite"
//                    "MobileNet" -> inputSize = 64
                    "ResNet50"-> TFLITE_SELECT = "TA_model_crack_concrete_ResNet50.tflite"
//                    "ResNet50" -> inputSize = 64
                    "InceptionV3" -> TFLITE_SELECT = "TA_model_crack_concrete_InceptionV3.tflite"
//                    "InceptionV3" -> inputSize = 75
                }

                when(TFLITE_SELECT_SIZE){
//                    "MobileNet" -> TFLITE_SELECT = "TA_model_crack_concrete_MobileNet.tflite"
                    "MobileNet" -> inputSize = 64
//                    "ResNet50"-> TFLITE_SELECT = "TA_model_crack_concrete_ResNet50.tflite"
                    "ResNet50" -> inputSize = 64
//                    "InceptionV3" -> TFLITE_SELECT = "TA_model_crack_concrete_InceptionV3.tflite"
                    "InceptionV3" -> inputSize = 75
                }


//                myInfo("selected arsitektur" + tflite[position])
                intentCamera.putExtra(KEY_TFLITE, TFLITE_SELECT)
                intentCamera.putExtra(KEY_INPUT_SIZE, inputSize)
                intentGallery.putExtra(KEY_TFLITE, TFLITE_SELECT)
                intentGallery.putExtra(KEY_INPUT_SIZE, inputSize)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
//        val itemSelectedId = binding.spinner.selectedItemId
//        val itemSelected = binding.spinner.selectedItem
//        Toast.makeText(this, itemSelectedId.toString() ,Toast.LENGTH_SHORT).show()
//        Toast.makeText(this, itemSelected.toString() ,Toast.LENGTH_SHORT).show()

        binding.buttonUsingCamera.setOnClickListener {
            startActivity(intentCamera)
        }

        binding.buttonUsingGallery.setOnClickListener {
            startActivity(intentGallery)
        }
    }


    fun myInfo(msg: String){
        Log.d(UsingGalleryActivity.TAG,msg)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}