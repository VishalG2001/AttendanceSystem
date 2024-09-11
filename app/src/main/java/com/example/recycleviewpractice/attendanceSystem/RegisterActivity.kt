package com.example.recycleviewpractice.attendanceSystem

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.metrics.AwsSdkMetrics.setRegion
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.DetectFacesRequest
import com.amazonaws.services.rekognition.model.Image
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.attendanceSystem.relmModels.Student
import com.example.recycleviewpractice.databinding.ActivityRegisterBinding
import io.realm.Realm
import io.realm.kotlin.createObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var realm: Realm
    private var imageBitmapUser: Bitmap? = null
    private var currentPhotoPath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpView()
        binding.captureImageButton.setOnClickListener { checkCameraPermission() }
        binding.registerButton.setOnClickListener { saveToRealmAndNavigate() }
    }
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Permission denied to use camera", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap?
            imageBitmap?.let {
                // Display the captured image in ImageView
                imageBitmapUser=it
//                binding.studentImage.setImageBitmap(it)
                // Save the image to a file
                saveImageToFile(it)
                detectFacesInImage(currentPhotoPath)
            }
        }
    }

    private fun detectFacesInImage(sourceImage: String?) {
        val credentials = BasicAWSCredentials("AKIAXYCOTWVS3PJHOIEM", "+zREr/UWwkxUlEVNJqJI2LKSqUMQvZmeachrEBwf")
        val rekognitionClient = AmazonRekognitionClient(credentials).apply {
            setRegion(com.amazonaws.regions.Region.getRegion(Regions.US_EAST_1))
        }

        val imageBytes = File(sourceImage).readBytes()
        val request = DetectFacesRequest().withImage(Image().withBytes(ByteBuffer.wrap(imageBytes)))

        Thread {
            try {
                val result = rekognitionClient.detectFaces(request)
                val faceDetails = result.faceDetails
                if (faceDetails.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this, "No face detected in the image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        binding.studentImage.setImageBitmap(imageBitmapUser)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to detect faces: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }



    private fun saveImageToFile(bitmap: Bitmap) {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
        currentPhotoPath = imageFile.absolutePath
        // Save the bitmap to the file
        val fos = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.flush()
        fos.close()
        // Log the image path for debugging
        Log.d("RegisterActivity", "Image saved to: $currentPhotoPath")
    }

    private fun saveToRealmAndNavigate() {
        val firstName = binding.layoutFirstName.etCommonEditText.text.toString()
        val lastName = binding.layoutLastName.etCommonEditText.text.toString()
        val email = binding.layoutEmail.etCommonEditText.text.toString()
        val rollNo = binding.layoutRollNo.etCommonEditText.text.toString()
        val phoneNo = binding.layoutPhoneNo.etCommonEditText.text.toString()

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || rollNo.isBlank() || phoneNo.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        realm.executeTransactionAsync({ realm ->
            // Create a new Student object
            val nextId = realm.where(Student::class.java).max("id")?.toLong()?.plus(1) ?: 1
            val student = realm.createObject<Student>(nextId)
            student.firstName = firstName
            student.lastName = lastName
            student.email = email
            student.rollNo = rollNo
            student.phoneNo = phoneNo
            student.userImg = currentPhotoPath.toString()

            // Optional: Save image as byte array if needed
//            imageBitmap?.let {
//                val byteArray = bitmapToByteArray(it)
//                student.userImg = byteArray
//            }
        }, {
            // Transaction was successful, navigate to Attendance Home activity
            Toast.makeText(this@RegisterActivity, "Successfully registered", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@RegisterActivity, AttendanceHome::class.java))
            finish() // Optional: finish current activity if not needed anymore
            realm.close()
        }, { error ->
            // Transaction failed, show error message
            Toast.makeText(this@RegisterActivity, "Failed to save data: ${error.message}", Toast.LENGTH_SHORT).show()
            Log.e("register", "Failed to save data: ${error.message}")
            realm.close()
        })
    }

    private fun setUpView() {
        binding.apply {
            layoutFirstName.apply {
                tvCommonLabel.setText(R.string.first_name)
                etCommonEditText.setHint(R.string.ash)
            }
            layoutLastName.apply {
                tvCommonLabel.setText(R.string.last_name)
                etCommonEditText.setHint(R.string.shaikh)
            }
            layoutEmail.apply {
                tvCommonLabel.setText(R.string.email)
                etCommonEditText.setHint(R.string.emailHint)
            }
            layoutRollNo.apply {
                tvCommonLabel.setText(R.string.RollNo)
                etCommonEditText.setHint(R.string.RollNoHint)
                etCommonEditText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            layoutPhoneNo.apply {
                tvCommonLabel.setText(R.string.Phone)
                etCommonEditText.setHint(R.string.numberHint)
                etCommonEditText.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }
}
