package com.example.recycleviewpractice.attendanceSystem.relmModels

import android.graphics.Bitmap
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Student : RealmObject() {
    @PrimaryKey
    var id: Long = 0
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var rollNo: String = ""
    var phoneNo: String = ""
    var userImg: String = ""
    var isPresent: Boolean = false
//    var ImagebyteArray: ByteArray = byteArrayOf()
//    var imageBitmap: Bitmap? = null
}
