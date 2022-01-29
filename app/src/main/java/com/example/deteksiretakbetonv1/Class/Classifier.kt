package com.example.deteksiretakbetonv1.Class

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.example.deteksiretakbetonv1.Data.Recognition
import com.example.deteksiretakbetonv1.UsingGalleryActivity
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class Classifier(assetManager: AssetManager, modelPath: String, labelPath: String, inputSizeModel: Int) {
    private var interpreter: Interpreter
    private var labellist: List<String>
    private val pixelSize = 3
    private val inputSize = inputSizeModel
    private val imageMean = 0
    private val imageStd = 255.0f
    private val batchSize = 1
    private val maxresults = 3
    private val threshold = 0.4f

    init{
        interpreter = Interpreter(loadModelFile(assetManager, modelPath))
        labellist = loadLabelList(assetManager, labelPath)
    }


    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @Throws(IOException::class)
    private fun loadLabelList(assetManager: AssetManager, labelPath: String): List<String> {
        val labelList = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(assetManager.open(labelPath)))
        while (true) {
            val line = reader.readLine() ?: break
            labelList.add(line)
        }
        reader.close()
        return labelList
    }

    fun recognizeImage(imageBitmap: Bitmap): List<Recognition> {
        val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, inputSize, inputSize, false)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)
        val result = Array(1) { FloatArray(labellist.size) }
        interpreter.run(byteBuffer, result)
        return getSortedResult(result)
    }

    private fun getSortedResult(result: Array<FloatArray>): List<Recognition> {
        val pq = PriorityQueue(
            maxresults,
            Comparator<Recognition> {
                    (_, _, confidence1), (_, _, confidence2)
                -> confidence1.compareTo(confidence2) * -1
            })

        for (i in labellist.indices) {
            val confidence = result[0][i]
            if (confidence >= threshold) {
                pq.add(Recognition("" + i,
                    if (labellist.size > i) labellist[i] else "Unknown", confidence)
                )
            }
        }
        Log.d("KlasifikasiDariGaleri", "pqsize:(%d)".format(pq.size))

        val recognitions = ArrayList<Recognition>()
        val recognitionsSize = pq.size.coerceAtMost(maxresults)
        for (i in 0 until recognitionsSize) {
            recognitions.add(pq.poll())
        }
        return recognitions
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {

        val byteBuffer =   ByteBuffer.allocateDirect(4 * batchSize * inputSize * inputSize * pixelSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        try{
            bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }catch(e: IOException){
            e.printStackTrace()
        }

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16 and 0xFF) - imageMean) / imageStd)
                byteBuffer.putFloat(((`val` shr 8 and 0xFF) - imageMean) / imageStd)
                byteBuffer.putFloat(((`val` and 0xFF) - imageMean) / imageStd)
            }
        }
        return byteBuffer
    }

}