package com.example.braintumordetector

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.braintumordetector.databinding.ActivityPatientDetailsBinding
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatientDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tumorHighlighter: TumorHighlighter

    private var result: String? = null
    private var confidence: Float = 0.0f
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("DoctorInfo", MODE_PRIVATE)
        tumorHighlighter = TumorHighlighter()

        result = intent.getStringExtra("result")
        confidence = intent.getFloatExtra("confidence", 0.0f)
        intent.getStringExtra("imageUri")?.let {
            imageUri = it.toUri()
        }

        loadDoctorInfo()
        setupGenderDropdown()
        setupReportDate()

        binding.btnGeneratePdf.setOnClickListener {
            generatePdfReport()
        }
    }

    private fun loadDoctorInfo() {
        binding.etDoctorName.setText(sharedPreferences.getString("doctor_name", ""))
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter
    }

    private fun setupReportDate() {
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.etReportDate.setText(currentDate)
    }

    private fun generatePdfReport() {
        try {
            val patientName = binding.etPatientName.text.toString()
            val patientAge = binding.etAge.text.toString()
            val patientGender = binding.spinnerGender.selectedItem.toString()
            val reportDate = binding.etReportDate.text.toString()
            val doctorName = binding.etDoctorName.text.toString()

            // Validation
            if (patientName.isEmpty() || patientAge.isEmpty() || doctorName.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return
            }

            val pdfFile = File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "BrainTumorReport_${System.currentTimeMillis()}.pdf"
            )
            val pdfWriter = PdfWriter(FileOutputStream(pdfFile))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // ✅ FIXED: Using proper iText7 API
            val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            val normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

            // PDF Header
            val header = Paragraph("BRAIN TUMOR DETECTION REPORT")
                .setFont(boldFont)
                .setFontSize(18f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(header)
            document.add(Paragraph("\n"))

            // Patient Information Section
            document.add(Paragraph("PATIENT INFORMATION").setFont(boldFont).setFontSize(14f))
            document.add(Paragraph("Name: $patientName").setFont(normalFont))
            document.add(Paragraph("Age: $patientAge years").setFont(normalFont))
            document.add(Paragraph("Gender: $patientGender").setFont(normalFont))
            document.add(Paragraph("Report Date: $reportDate").setFont(normalFont))
            document.add(Paragraph("\n"))

            // Doctor Information
            document.add(Paragraph("EXAMINING PHYSICIAN").setFont(boldFont).setFontSize(14f))
            document.add(Paragraph("Dr. $doctorName").setFont(normalFont))
            document.add(Paragraph("\n"))

            // Diagnosis Results
            document.add(Paragraph("DIAGNOSIS RESULTS").setFont(boldFont).setFontSize(14f))
            document.add(Paragraph("Tumor Type: ${result ?: "Unknown"}").setFont(normalFont))
            // ✅ Confidence removed for defense presentation
            document.add(Paragraph("\n"))

            // Add highlighted MRI scan image to PDF
            imageUri?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)

                    // Apply tumor highlighting
                    val highlightedBitmap = if (result != null && result != "no_tumor") {
                        tumorHighlighter.highlightTumorArea(
                            originalBitmap,
                            result!!,
                            confidence
                        )
                    } else {
                        originalBitmap
                    }

                    // Convert to byte array for PDF
                    val stream = ByteArrayOutputStream()
                    highlightedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

                    // Add to PDF with proper sizing
                    val image = Image(ImageDataFactory.create(stream.toByteArray()))
                    image.setWidth(400f)
                    image.setAutoScale(true)

                    document.add(
                        Paragraph("MRI SCAN IMAGE (WITH TUMOR HIGHLIGHTING)")
                            .setFont(boldFont)
                            .setFontSize(14f)
                    )
                    document.add(image)
                    document.add(Paragraph("\n"))

                } catch (e: Exception) {
                    e.printStackTrace()
                    document.add(Paragraph("Error: Could not load MRI image").setFont(normalFont))
                }
            } ?: run {
                document.add(Paragraph("No MRI image available").setFont(normalFont))
            }

            // Add interpretation note
            document.add(Paragraph("\n"))
            document.add(Paragraph("NOTE:").setFont(boldFont))
            document.add(
                Paragraph(
                    "The highlighted area on the MRI scan indicates the suspected tumor region detected by AI analysis. " +
                            "This report is generated by an AI-based detection system and should be reviewed by a qualified medical professional."
                ).setFont(normalFont).setFontSize(10f)
            )

            // Footer
            document.add(Paragraph("\n\n"))
            document.add(
                Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
                    .setFont(normalFont)
                    .setFontSize(9f)
                    .setTextAlignment(TextAlignment.RIGHT)
            )

            document.close()

            Toast.makeText(this, "✅ PDF Report Generated Successfully!", Toast.LENGTH_LONG).show()
            openPdf(pdfFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPdf(file: File) {
        try {
            val photoURI = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(photoURI, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No PDF viewer found. File saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }
}