package com.example.recycleviewpractice


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var resultTextView: TextView

    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null

    private val REQUEST_IMAGE1 = 101
    private val REQUEST_IMAGE2 = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the MobileFaceNet model
        interpreter = loadModel()

        imageView1 = findViewById(R.id.imageView1)
        imageView2 = findViewById(R.id.imageView2)
        resultTextView = findViewById(R.id.resultTextView)

        val uploadBtn1: Button = findViewById(R.id.uploadImage1)
        val uploadBtn2: Button = findViewById(R.id.uploadImage2)
        val compareBtn: Button = findViewById(R.id.compareButton)

        uploadBtn1.setOnClickListener { pickImage(REQUEST_IMAGE1) }
        uploadBtn2.setOnClickListener { pickImage(REQUEST_IMAGE2) }

        compareBtn.setOnClickListener {
            if (bitmap1 != null && bitmap2 != null) {
                val embedding1 = extractFaceEmbedding(bitmap1!!)
                val embedding2 = extractFaceEmbedding(bitmap2!!)
                val similarity = calculateCosineSimilarity(embedding1, embedding2)
                resultTextView.text = "Similarity: %.2f".format(similarity)
            } else {
                resultTextView.text = "Please upload both images."
            }
        }
    }

    private fun pickImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!
            val bitmap = getBitmapFromUri(imageUri)

            when (requestCode) {
                REQUEST_IMAGE1 -> {
                    bitmap1 = bitmap
                    imageView1.setImageBitmap(bitmap1)
                }
                REQUEST_IMAGE2 -> {
                    bitmap2 = bitmap
                    imageView2.setImageBitmap(bitmap2)
                }
            }
        }
    }

    private fun getBitmapFromUri(imageUri: Uri): Bitmap {
        val bitmap = if (android.os.Build.VERSION.SDK_INT >= 29) {
            val source = ImageDecoder.createSource(this.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        }

        // Ensure the bitmap is mutable and in ARGB_8888 format
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }


    private fun loadModel(): Interpreter {
        val assetManager = assets
        val modelDescriptor = assetManager.openFd("mobile_face_net.tflite")
        val inputStream = FileInputStream(modelDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = modelDescriptor.startOffset
        val declaredLength = modelDescriptor.declaredLength
        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        return Interpreter(model)
    }

    private fun extractFaceEmbedding(faceBitmap: Bitmap): FloatArray {
        val inputBuffer = bitmapToByteBuffer(faceBitmap)

        // The output size of MobileFaceNet is 192, as per your error message.
        val outputBuffer = Array(1) { FloatArray(192) } // 2D array [1, 192] for output

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Return the 1D embedding of size 192
        return outputBuffer[0] // Extract the 192-length array from the 2D array
    }


    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 112 // The expected input size for MobileFaceNet is 112x112
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 4 bytes per float for RGB
        byteBuffer.order(ByteOrder.nativeOrder())

        // Resize the bitmap to 112x112
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Convert the resized bitmap to a float array
        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        // Put the pixel values into the ByteBuffer
        for (pixel in intValues) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // Red
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // Green
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)           // Blue
        }

        return byteBuffer
    }


    private fun calculateCosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in embedding1.indices) {
            dotProduct += embedding1[i] * embedding2[i]
            normA += embedding1[i] * embedding1[i]
            normB += embedding2[i] * embedding2[i]
        }
        return dotProduct / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble())).toFloat()
    }
}
