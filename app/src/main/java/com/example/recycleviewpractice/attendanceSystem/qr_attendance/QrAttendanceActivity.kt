package com.example.recycleviewpractice.attendanceSystem.qr_attendance

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.databinding.ActivityQrAttendanceBinding
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class QrAttendanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrAttendanceBinding
    private lateinit var qrCodeScannerLauncher: ActivityResultLauncher<Intent>
    private val scanQrCodeLauncher = registerForActivityResult(ScanQRCode()) { result ->
        // handle QRResult
        when (result) {
            is QRResult.QRSuccess -> result.content.rawValue?.let { validateQrData(it) }
            is QRResult.QRUserCanceled -> binding.scanResultTextView.text = "Scan canceled."
            is QRResult.QRMissingPermission -> binding.scanResultTextView.text =
                "Missing permission."

            is QRResult.QRError -> binding.scanResultTextView.text =
                "Error: ${result.exception.localizedMessage}"
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanButton.setOnClickListener {
            scanQrCodeLauncher.launch(null)

        }
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
                    binding.scanResultTextView.text = "ID: $id\nDate: $dateString"
                    Toast.makeText(this, "Attendance Marked Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.scanResultTextView.text = "QR code expired."
                }
            } catch (e: Exception) {
                binding.scanResultTextView.text = "Invalid QR code data."
            }
        }
    }

}