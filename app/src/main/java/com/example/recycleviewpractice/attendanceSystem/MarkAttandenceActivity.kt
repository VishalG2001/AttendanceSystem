package com.example.recycleviewpractice.attendanceSystem

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.CompareFacesRequest
import com.amazonaws.services.rekognition.model.CompareFacesResult
import com.amazonaws.services.rekognition.model.Image
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.attendanceSystem.adapters.StudentAdapter
import com.example.recycleviewpractice.attendanceSystem.qr_attendance.QrAttendanceActivity
import com.example.recycleviewpractice.attendanceSystem.relmModels.Student
import com.example.recycleviewpractice.databinding.ActivityMarkAttandenceBinding
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarkAttandenceActivity : AppCompatActivity(), StudentClickListener {

    private lateinit var binding: ActivityMarkAttandenceBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var realm: Realm
    private lateinit var studentList: List<Student>
    private var selectedStudent: Student? = null

    private val scanQrCodeLauncher = registerForActivityResult(ScanQRCode()) { result ->
        // handle QRResult
        when (result) {
            is QRResult.QRSuccess -> result.content.rawValue?.let { validateQrData(it) }
            is QRResult.QRUserCanceled -> Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
            is QRResult.QRMissingPermission ->Toast.makeText(this, "Missing Permissions", Toast.LENGTH_SHORT)

            is QRResult.QRError -> Toast.makeText(this, "Error: ${result.exception.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    private val credentials = BasicAWSCredentials("AKIAXYCOTWVS3PJHOIEM", "+zREr/UWwkxUlEVNJqJI2LKSqUMQvZmeachrEBwf")
    private val rekognitionClient = AmazonRekognitionClient(credentials).apply {
        setRegion(com.amazonaws.regions.Region.getRegion(Regions.US_EAST_1))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMarkAttandenceBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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
                this,
                Manifest.permission.CAMERA
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

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                imageBitmap?.let {
                    selectedStudent?.let { student ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                compareFaces(student.userImg, it)
                            } catch (e: Exception) {
                                Log.e("MarkAttendanceActivity", "Error comparing faces: ${e.message}")
                                showToastOnMainThread("Error comparing faces")
                            }
                        }
                    }
                }
            }
        }

    private fun compareFaces(sourceImageUrl: String, targetImage: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            performCompareFaces(sourceImageUrl, targetImage)
        }
    }

    private suspend fun performCompareFaces(sourceImageUrl: String, targetImage: Bitmap) {
        try {
            // Convert the captured Bitmap to bytes
            val targetImageBytes = ByteArrayOutputStream()
            targetImage.compress(Bitmap.CompressFormat.JPEG, 100, targetImageBytes)
            val targetImageRequest = Image().withBytes(ByteBuffer.wrap(targetImageBytes.toByteArray()))

            val localImageBytes = File(sourceImageUrl).readBytes()
            val sourceImageRequest = Image().withBytes(ByteBuffer.wrap(localImageBytes))

            // Create CompareFacesRequest
            val facesRequest = CompareFacesRequest()
                .withSourceImage(sourceImageRequest)
                .withTargetImage(targetImageRequest)
                .withSimilarityThreshold(70f)

            // Perform compareFaces operation
            val compareFacesResult = rekognitionClient.compareFaces(facesRequest)

            // Handle results on the Main/UI thread
            handleCompareFacesResult(compareFacesResult)

        } catch (e: Exception) {
            e.printStackTrace()
            showToastOnMainThread("Error comparing faces")
            Log.e("MarkAttendance", "Error comparing faces: ${e.message}")
        }
    }

    private suspend fun handleCompareFacesResult(compareFacesResult: CompareFacesResult) {
        withContext(Dispatchers.Main) {
            val faceDetails = compareFacesResult.faceMatches
            if (faceDetails != null && faceDetails.isNotEmpty()) {
                val match = faceDetails[0]
                val similarity = match.similarity
                if (similarity >= 70f) {
                    // Faces match, mark attendance
                    markAttendance(selectedStudent!!)
                } else {
                    showToastOnMainThread("Face similarity is too low")
                }
            } else {
                showToastOnMainThread("No matching faces found")
            }
        }
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
    private fun validateQrData(qrData: String?) {
        qrData?.let {
            try {
                val lines = it.split("\n")
                val id = lines[0].split(": ")[1]
                val dateString = lines[1].split(": ")[1]

                val qrDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString)
                val currentTime = Date()

                if (qrDate != null && currentTime.time - qrDate.time <= 60000) { // Check if the QR code is not older than 1 minute
                    selectedStudent?.let { student ->
                        markAttendance(student)
                    }
                    Toast.makeText(this, "Attendance Marked Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "QR code expired", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
