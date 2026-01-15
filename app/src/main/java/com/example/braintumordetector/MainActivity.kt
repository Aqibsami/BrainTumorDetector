package com.example.braintumordetector

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.braintumordetector.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    private var selectedImageBitmap: Bitmap? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val inputStream = contentResolver.openInputStream(it)
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                binding.imgPreview.setImageBitmap(selectedImageBitmap)
                binding.btnAnalyze.isEnabled = true
                showToast("Image selected successfully ✅")
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                showToast("Error: Image not found")
            }
        } ?: showToast("Image selection cancelled ❌")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        binding.btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnAnalyze.setOnClickListener {
            analyzeSelectedImage()
        }

        binding.btnReset.setOnClickListener {
            resetUI()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })
    }

    private fun analyzeSelectedImage() {
        selectedImageBitmap?.let { bitmap ->
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_IMAGE_BITMAP, byteArray)
                putExtra(ResultActivity.EXTRA_IMAGE_URI, selectedImageUri.toString())
            }
            startActivity(intent)
        } ?: showToast("⚠️ Please select an image first!")
    }

    private fun resetUI() {
        selectedImageUri = null
        selectedImageBitmap = null
        binding.imgPreview.setImageBitmap(null)
        binding.btnAnalyze.isEnabled = false
        showToast("Reset successful 🔄")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_edit_doctor_info -> {
                startActivity(Intent(this, DoctorInfoActivity::class.java))
            }
            R.id.nav_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }
            R.id.nav_version -> {
                showToast("App Version: 1.0")
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
