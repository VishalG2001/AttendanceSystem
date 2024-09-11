package com.example.recycleviewpractice.attendanceSystem


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.recycleviewpractice.attendanceSystem.qr_attendance.QrAttendanceActivity
import com.example.recycleviewpractice.attendanceSystem.qr_attendance.QrCodeGenerationActivity
import com.example.recycleviewpractice.databinding.ActivityAttendenceHomeBinding

class AttendanceHome : AppCompatActivity() {
    private lateinit var binding: ActivityAttendenceHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendenceHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            btnRegister.setOnClickListener {
                val intent = Intent(this@AttendanceHome, RegisterActivity::class.java)
                startActivity(intent)
            }
            btnMarkAttendance.setOnClickListener {
                val intent = Intent(this@AttendanceHome, MarkAttandenceActivity::class.java)
                intent.putExtra("sourceActivity",false)
                startActivity(intent)
            }
            btnTeacherLogin.setOnClickListener {
                val intent = Intent(this@AttendanceHome, QrCodeGenerationActivity::class.java)
                startActivity(intent)
            }
            btnMarkAttendanceQR.setOnClickListener {
                val intent = Intent(this@AttendanceHome, MarkAttandenceActivity::class.java)
                intent.putExtra("sourceActivity",true)
                startActivity(intent)
            }
        }


    }
}