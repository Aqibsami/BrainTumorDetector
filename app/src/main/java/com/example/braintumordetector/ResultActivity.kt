package com.example.braintumordetector

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.braintumordetector.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var modelHelper: ModelHelper? = null
    private var tumorHighlighter: TumorHighlighter? = null
    private var imageUri: Uri? = null
    private var results: List<ClassificationResult>? = null

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_IMAGE_BITMAP = "extra_image_bitmap"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            modelHelper = ModelHelper(this)
            tumorHighlighter = TumorHighlighter() // ✅ Highlighter initialize kiya
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("FATAL: Error initializing model: ${e.message}")
            finish()
            return
        }

        val byteArray = intent.getByteArrayExtra(EXTRA_IMAGE_BITMAP)
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)

        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString)
        }

        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            displayImageAndAnalyze(bitmap)
        } else {
            showToast("Error: No image data provided")
            finish()
        }

        binding.btnAddPatientDetails.setOnClickListener {
            val intent = Intent(this, PatientDetailsActivity::class.java).apply {
                putExtra("imageUri", imageUri.toString())
                results?.let {
                    if (it.isNotEmpty()) {
                        putExtra("result", it[0].label)
                        putExtra("confidence", it[0].confidence)
                    }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }

    private fun displayImageAndAnalyze(bitmap: Bitmap) {
        try {
            // Pehle original image show karo
            binding.imgPreview.setImageBitmap(bitmap)

            modelHelper?.let {
                results = it.classify(bitmap)
                runOnUiThread {
                    if (results != null && results!!.isNotEmpty()) {
                        displayResults(results!!)

                        // ✅ Tumor detection ke baad highlighting apply karo
                        val topResult = results!![0]
                        highlightTumorOnImage(bitmap, topResult)
                    } else {
                        showToast("Error: Could not get classification results.")
                    }
                }
            } ?: showToast("Model is not initialized")

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error: Failed to process image data.")
        }
    }

    /**
     * ✅ Tumor area ko highlight karta hai
     */
    private fun highlightTumorOnImage(originalBitmap: Bitmap, result: ClassificationResult) {
        try {
            tumorHighlighter?.let { highlighter ->
                // Heatmap overlay apply karo
                val highlightedBitmap = highlighter.highlightTumorArea(
                    originalBitmap,
                    result.label,
                    result.confidence
                )

                // Highlighted image show karo
                binding.imgPreview.setImageBitmap(highlightedBitmap)

                // Log for debugging
                if (!result.label.equals("no_tumor", ignoreCase = true)) {
                    showToast("Tumor area highlighted ✅")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Agar highlighting fail ho to original image hi dikhaate raho
            showToast("Note: Could not apply highlighting")
        }
    }

    private fun displayResults(results: List<ClassificationResult>) {
        if (results.isNotEmpty()) {
            val topResult = results[0]
            binding.tvTumorResult.text = "Tumor Result: ${topResult.label}"
            binding.tvConfidence.visibility = View.GONE
        } else {
            binding.tvTumorResult.text = "Tumor Result: No confident prediction"
            binding.tvConfidence.visibility = View.GONE
            showToast("Model returned no confident predictions.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        modelHelper?.close()
    }
}