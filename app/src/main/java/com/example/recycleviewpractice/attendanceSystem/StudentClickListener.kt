package com.example.recycleviewpractice.attendanceSystem

import com.example.recycleviewpractice.attendanceSystem.relmModels.Student

interface StudentClickListener {
    fun onImageViewClick(student: Student)
    fun onQrButtonClick(student: Student) // New method for handling QR button clicks
}

