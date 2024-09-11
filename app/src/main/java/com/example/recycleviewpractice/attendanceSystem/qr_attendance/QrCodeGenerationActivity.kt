package com.example.recycleviewpractice.attendanceSystem.qr_attendance

import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.databinding.ActivityQrCodeGenerationBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class QrCodeGenerationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrCodeGenerationBinding
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var countDownTimer: CountDownTimer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrCodeGenerationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startQrCodeGeneration()
    }
    private fun startQrCodeGeneration() {
        val runnable = object : Runnable {
            override fun run() {
                generateQrCode()
                startTimer()
                handler.postDelayed(this, 60000) // 1 minute
            }
        }
        handler.post(runnable)
    }

    private fun generateQrCode() {
        val id = UUID.randomUUID().toString()
        val currentDateTime =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val qrData = "ID: $id\nDate: $currentDateTime"
        try {
            val bitMatrix = QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) -0x1000000 else -0x1)
                }
                binding.qrCodeImageView.setImageBitmap(bitmap)
            }
        }catch (e: WriterException) {
            e.printStackTrace()
        }
    }
    private fun startTimer() {
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.timerTextView.text = "Time until refresh: $secondsRemaining seconds"
            }

            override fun onFinish() {
                binding.timerTextView.text = "Refreshing QR code..."
            }
        }.start()
    }
}