package com.example.braintumordetector

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.braintumordetector.databinding.ActivityDoctorInfoBinding

class DoctorInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorInfoBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("DoctorInfo", MODE_PRIVATE)

        loadDoctorInfo()

        binding.btnSave.setOnClickListener {
            saveDoctorInfo()
        }
    }

    private fun loadDoctorInfo() {
        binding.etDoctorName.setText(sharedPreferences.getString("doctor_name", ""))
        binding.etHospitalName.setText(sharedPreferences.getString("hospital_name", ""))
        binding.etDesignation.setText(sharedPreferences.getString("designation", ""))
    }

    private fun saveDoctorInfo() {
        sharedPreferences.edit {
            putString("doctor_name", binding.etDoctorName.text.toString())
            putString("hospital_name", binding.etHospitalName.text.toString())
            putString("designation", binding.etDesignation.text.toString())
        }
        Toast.makeText(this, "Doctor information saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
