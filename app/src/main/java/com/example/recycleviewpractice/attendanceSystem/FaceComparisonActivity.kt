package com.example.recycleviewpractice.attendanceSystem

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.example.recycleviewpractice.R
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class FaceComparisonActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var resultTextView: TextView
    private var selectedImage1: Bitmap? = null
    private var selectedImage2: Bitmap? = null

    private val REQUEST_IMAGE1 = 1
    private val REQUEST_IMAGE2 = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_comparison)

        imageView1 = findViewById(R.id.imageView1)
        imageView2 = findViewById(R.id.imageView2)
        resultTextView = findViewById(R.id.resultTextView)

        // Initialize the TFLite model
        loadModel()

        findViewById<Button>(R.id.uploadButton1).setOnClickListener {
            openGallery(REQUEST_IMAGE1)
        }

        findViewById<Button>(R.id.uploadButton2).setOnClickListener {
            openGallery(REQUEST_IMAGE2)
        }

        findViewById<Button>(R.id.compareButton).setOnClickListener {
            compareFaces()
        }
    }

    private fun loadModel() {
        val modelPath = "mobile_face_net.tflite"
        val assetManager = assets
        val model = assetManager.openFd(modelPath)
        val fileInputStream = FileInputStream(model.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, model.startOffset, model.declaredLength)
        interpreter = Interpreter(mappedByteBuffer)
    }

    private fun openGallery(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            when (requestCode) {
                REQUEST_IMAGE1 -> {
                    imageView1.setImageURI(imageUri)
                    selectedImage1 = imageView1.drawable.toBitmap()
                }
                REQUEST_IMAGE2 -> {
                    imageView2.setImageURI(imageUri)
                    selectedImage2 = imageView2.drawable.toBitmap()
                }
            }
        }
    }

    private fun compareFaces() {
        if (selectedImage1 != null && selectedImage2 != null) {
            val embedding1 = extractFaceEmbedding(selectedImage1!!)
            val embedding2 = extractFaceEmbedding(selectedImage2!!)

            val distance = calculateEuclideanDistance(embedding1, embedding2)
            val threshold = 1.0f // Adjust this threshold for accuracy

            if (distance < threshold) {
                resultTextView.text = "Faces match!"
            } else {
                resultTextView.text = "Faces do not match."
            }
        } else {
            Toast.makeText(this, "Please upload both images", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractFaceEmbedding(bitmap: Bitmap): FloatArray {
        val inputBuffer = bitmapToByteBuffer(bitmap)
        val outputBuffer = FloatArray(128) // Model output size
        interpreter.run(inputBuffer, outputBuffer)
        return outputBuffer
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 112  // Based on MobileFaceNet input size (112x112)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val intValues = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in intValues) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)  // Red
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)   // Green
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)            // Blue
        }

        return byteBuffer
    }

    private fun calculateEuclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        var sum = 0.0f
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }
        return kotlin.math.sqrt(sum)
    }
}
