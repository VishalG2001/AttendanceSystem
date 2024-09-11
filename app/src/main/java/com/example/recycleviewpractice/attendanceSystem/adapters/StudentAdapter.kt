package com.example.recycleviewpractice.attendanceSystem.adapters

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.attendanceSystem.StudentClickListener
import com.example.recycleviewpractice.attendanceSystem.relmModels.Student

class StudentAdapter(
    private val context: Context,
    private val studentList: List<Student>,
    private val listener: StudentClickListener,
    private val isQrFlow: Boolean // Flag to determine which flow to use
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_student_row, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        Log.d("StudentAdapter", "Binding view at position $position")
        if (position == 0) {
            // Set static text for header
            holder.setHeader()
        } else {
            val student = studentList[position - 1]
            holder.bind(student, listener, isQrFlow)
        }
    }

    override fun getItemCount(): Int {
        // Add one for the header
        return studentList.size + 1
    }

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFirstName: TextView = itemView.findViewById(R.id.textFirstName)
        private val textViewLastName: TextView = itemView.findViewById(R.id.textLastName)
        private val textViewEmail: TextView = itemView.findViewById(R.id.textEmail)
        private val textViewRollNo: TextView = itemView.findViewById(R.id.textRollNo)
        private val textViewPhoneNo: TextView = itemView.findViewById(R.id.textPhoneNo)
        private val imageViewAttendance: ImageView = itemView.findViewById(R.id.imageViewAttendance)
        private val imageViewQR: ImageView = itemView.findViewById(R.id.imageViewQR) // Add QR image view

        fun bind(student: Student, listener: StudentClickListener, isQrFlow: Boolean) {
            Log.d("StudentViewHolder", "Binding student: ${student.firstName} ${student.lastName}")

            textViewFirstName.text = student.firstName
            textViewLastName.text = student.lastName
            textViewEmail.text = student.email
            textViewRollNo.text = student.rollNo
            textViewPhoneNo.text = student.phoneNo

            if (isQrFlow) {
                // Show QR button, hide camera button
                imageViewAttendance.visibility = GONE
                imageViewQR.visibility = View.VISIBLE
                imageViewQR.setImageResource(
                    if (student.isPresent) R.drawable.ic_tick
                    else R.drawable.ic_qr
                )
                imageViewQR.setOnClickListener {
                    listener.onQrButtonClick(student)
                }
            } else {
                // Show camera button, hide QR button
                imageViewQR.visibility = GONE
                imageViewAttendance.visibility = View.VISIBLE
                imageViewAttendance.setImageResource(
                    if (student.isPresent) R.drawable.mark_attendance else R.drawable.un_mark
                )
                imageViewAttendance.setOnClickListener {
                    listener.onImageViewClick(student)
                }
            }
        }

        fun setHeader() {
            // Set text to bold
            textViewFirstName.setTypeface(null, Typeface.BOLD)
            textViewLastName.setTypeface(null, Typeface.BOLD)
            textViewEmail.setTypeface(null, Typeface.BOLD)
            textViewRollNo.setTypeface(null, Typeface.BOLD)
            textViewPhoneNo.setTypeface(null, Typeface.BOLD)

            // Hide image views in header
            imageViewAttendance.visibility = GONE
            imageViewQR.visibility = GONE

            // Set background color
            val backgroundColor = ContextCompat.getColor(
                itemView.context,
                R.color.regent_st_blue // Replace with your color resource
            )
            itemView.setBackgroundColor(backgroundColor)

            // Set header text
            textViewFirstName.text = "First Name"
            textViewLastName.text = "Last Name"
            textViewEmail.text = "Email"
            textViewRollNo.text = "Roll No"
            textViewPhoneNo.text = "Phone No"
        }
    }
}
