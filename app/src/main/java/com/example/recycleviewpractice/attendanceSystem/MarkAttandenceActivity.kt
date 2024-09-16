package com.example.recycleviewpractice.attendanceSystem

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.attendanceSystem.adapters.StudentAdapter
import com.example.recycleviewpractice.attendanceSystem.relmModels.Student
import com.example.recycleviewpractice.databinding.ActivityMarkAttandenceBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarkAttandenceActivity : AppCompatActivity(), StudentClickListener {

    private lateinit var croppedFaceBitmap: Bitmap
    private lateinit var binding: ActivityMarkAttandenceBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var realm: Realm
    private lateinit var studentList: List<Student>
    private var selectedStudent: Student? = null
    private lateinit var interpreter: Interpreter
    private var attendanceCheck: Boolean = false

    private val scanQrCodeLauncher = registerForActivityResult(ScanQRCode()) { result ->
        // handle QRResult
        when (result) {
            is QRResult.QRSuccess -> result.content.rawValue?.let { validateQrData(it) }
            is QRResult.QRUserCanceled -> Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT)
                .show()

            is QRResult.QRMissingPermission -> Toast.makeText(
                this, "Missing Permissions", Toast.LENGTH_SHORT
            )

            is QRResult.QRError -> Toast.makeText(
                this, "Error: ${result.exception.localizedMessage}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadModel(): Interpreter {
        val assetManager = assets
        val modelDescriptor = assetManager.openFd("mobilefacenet.tflite")
        val inputStream = FileInputStream(modelDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = modelDescriptor.startOffset
        val declaredLength = modelDescriptor.declaredLength
        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        return Interpreter(model)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMarkAttandenceBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        interpreter = loadModel()

        // Initialize Realm
        realm = Realm.getDefaultInstance()

        // Retrieve student list from Realm
        studentList = retrieveStudentList()
        val isQrFlow = intent.getBooleanExtra("sourceActivity", false)

        recyclerView = findViewById(R.id.recyclerViewStudents)
        recyclerView.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(this, studentList, this, isQrFlow)
        recyclerView.adapter = studentAdapter
    }

    private fun checkCameraPermission(student: Student) {
        selectedStudent = student
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Permission denied to use camera", Toast.LENGTH_SHORT).show()
            }
        }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun markAttendance(student: Student) {
        realm.executeTransactionAsync { realm ->
            student.isPresent = true
            realm.insertOrUpdate(student)
        }
        showToastOnMainThread("Attendance marked for ${student.firstName} ${student.lastName}")
        studentAdapter.notifyDataSetChanged()
    }

    private fun unMarkAttendance(student: Student) {
        realm.executeTransactionAsync { realm ->
            student.isPresent = false
            realm.insertOrUpdate(student)
        }
        showToastOnMainThread("Attendance unmarked for ${student.firstName} ${student.lastName}")
        studentAdapter.notifyDataSetChanged()
    }

    private fun showToastOnMainThread(message: String) {
        runOnUiThread {
            Toast.makeText(this@MarkAttandenceActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrieveStudentList(): List<Student> {
        val students = realm.where(Student::class.java).findAll()
        return realm.copyFromRealm(students)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    override fun onImageViewClick(student: Student) {
        checkCameraPermission(student)
    }

    override fun onQrButtonClick(student: Student) {
        selectedStudent = student
        scanQrCodeLauncher.launch(null)
    }

    private fun compareFaces1(bitmap1: Bitmap, bitmap2: Bitmap): Boolean {
        val embedding1 = extractFaceEmbedding(bitmap1)
        val embedding2 = extractFaceEmbedding(bitmap2)

        // Calculate the distance between two embeddings (Euclidean distance)
        val distance = calculateEuclideanDistance(embedding1, embedding2)
        Log.d("Vishal", "compareFaces1: $distance")
        return distance < 0.5 // Adjust threshold based on your needs
    }

    private fun extractFaceEmbedding(bitmap: Bitmap): FloatArray {
        val inputBuffer = bitmapToByteBuffer(bitmap)
        val outputBuffer = Array(1) { FloatArray(192) } // Adjust the size based on your model
        interpreter.run(inputBuffer, outputBuffer)
        return outputBuffer[0]
    }

    private fun calculateEuclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        var sum = 0.0
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }
        return Math.sqrt(sum).toFloat()
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 112 // Assuming the input size is 112x112 for MobileFaceNet
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixel = resizedBitmap.getPixel(i, j)
                byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
        }

        return byteBuffer
    }

    private fun validateQrData(qrData: String?) {
        qrData?.let {
            try {
                val lines = it.split("\n")
                val id = lines[0].split(": ")[1]
                val dateString = lines[1].split(": ")[1]

                val qrDate =
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString)
                val currentTime = Date()

                if (qrDate != null && currentTime.time - qrDate.time <= 60000) { // Check if the QR code is not older than 1 minute
                    selectedStudent?.let { student ->
                        markAttendance(student)
                    }
                    Toast.makeText(this, "Attendance Marked Successfully", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this, "QR code expired", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                imageBitmap?.let {
                    selectedStudent?.let { student ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            // Convert the image to InputImage format for ML Kit
                            val inputImage = InputImage.fromBitmap(imageBitmap, 0)

                            // Configure high accuracy options for ML Kit face detection
                            val highAccuracyOpts = FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                .setMinFaceSize(0.15f)
                                .enableTracking()
                                .build()

                            // Get the FaceDetector instance
                            val detector = FaceDetection.getClient(highAccuracyOpts)

                            // Process the image and detect faces
                            detector.process(inputImage)
                                .addOnSuccessListener { faces ->
                                    if (faces.isEmpty()) {
                                        // No faces detected
                                        showToastOnMainThread("No face detected in the image")
                                    } else {
                                        // At least one face detected
                                        val face = faces[0]
                                        val bounds = face.boundingBox

                                        // Crop the face from the original image
                                        croppedFaceBitmap = cropFaceFromBitmap(imageBitmap, bounds)

                                        // Now that croppedFaceBitmap is initialized, proceed with face comparison
                                        try {
                                            val photoPath1 = student.userImg
                                            val bitmap1 = BitmapFactory.decodeFile(photoPath1)

                                            if (bitmap1 != null) {
                                                // Compare the original image with the cropped face
                                                if (compareFaces1(bitmap1, croppedFaceBitmap)) {
                                                    showToastOnMainThread("Face matched")
                                                    lifecycleScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            markAttendance(student)
                                                        }
                                                    }
                                                    attendanceCheck = true
                                                } else {
                                                    showToastOnMainThread("Face not matched")
                                                }
                                            } else {
                                                showToastOnMainThread("Failed to convert image path to Bitmap")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "MarkAttendanceActivity",
                                                "Error comparing faces: ${e.message}"
                                            )
                                            showToastOnMainThread("Error comparing faces")
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Handle detection failure
                                    showToastOnMainThread("Failed to detect faces: ${e.message}")
                                }
                        }
                    }
                }
            }
        }

    // Function to crop the face from the original bitmap
    private fun cropFaceFromBitmap(sourceBitmap: Bitmap, faceBounds: Rect): Bitmap {
        // Ensure the face bounds are within the bitmap dimensions
        val left = faceBounds.left.coerceAtLeast(0)
        val top = faceBounds.top.coerceAtLeast(0)
        val right = faceBounds.right.coerceAtMost(sourceBitmap.width)
        val bottom = faceBounds.bottom.coerceAtMost(sourceBitmap.height)

        // Crop the face region from the original bitmap
        return Bitmap.createBitmap(sourceBitmap, left, top, right - left, bottom - top)
    }
}

