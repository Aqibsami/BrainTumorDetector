package com.example.braintumordetector

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

class ModelHelper(private val context: Context) {

    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String>

    private val modelPath = "brain_tumor_classifier_float32.tflite"
    private val labelsPath = "labels.txt"

    init {
        setupModel()
    }

    private fun setupModel() {
        try {
            labels = context.assets.open(labelsPath).bufferedReader().readLines()
            val model = loadModelFile()
            interpreter = Interpreter(model)
            Log.i("ModelHelper", "Model loaded successfully with ${labels.size} classes")
        } catch (e: Exception) {
            Log.e("ModelHelper", "Error setting up model", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun classify(bitmap: Bitmap): List<ClassificationResult> {
        if (interpreter == null) {
            Log.e("ModelHelper", "Interpreter is null")
            return emptyList()
        }

        return try {
            // Get input tensor details
            val inputTensor = interpreter!!.getInputTensor(0)
            val inputShape = inputTensor.shape() // [batch, height, width, channels]
            val inputType = inputTensor.dataType()

            Log.i("ModelHelper", "Input shape: ${inputShape.contentToString()}")
            Log.i("ModelHelper", "Input type: $inputType")

            // Extract dimensions (handle different shape formats)
            val height = inputShape[1]
            val width = inputShape[2]

            Log.i("ModelHelper", "Expected input size: ${width}x${height}")

            // ✅ FIX 1: Try normalization to [0, 1] range instead of [-1, 1]
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(height, width, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))  // Changed from (127.5f, 127.5f) to (0f, 255f)
                .build()

            // Process image
            var tensorImage = TensorImage(inputType)
            tensorImage.load(bitmap)

            Log.i("ModelHelper", "Original bitmap: ${bitmap.width}x${bitmap.height}")

            tensorImage = imageProcessor.process(tensorImage)

            // Get output details
            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val numClasses = outputShape[1]

            Log.i("ModelHelper", "Output shape: ${outputShape.contentToString()}")
            Log.i("ModelHelper", "Number of classes: $numClasses")

            // Run inference
            val output = Array(1) { FloatArray(numClasses) }
            interpreter!!.run(tensorImage.buffer, output)

            Log.i("ModelHelper", "Raw output before softmax: ${output[0].contentToString()}")

            // Apply softmax
            val probs = softmax(output[0])

            Log.i("ModelHelper", "After softmax: ${probs.contentToString()}")
            Log.i("ModelHelper", "Softmax sum: ${probs.sum()}")

            // Verify labels match classes
            if (labels.size != numClasses) {
                Log.w("ModelHelper", "Warning: Label count (${labels.size}) != output classes ($numClasses)")
            }

            // Create results
            val results = probs.mapIndexed { index, confidence ->
                val label = labels.getOrElse(index) { "unknown_$index" }
                Log.i("ModelHelper", "Class $index ($label): ${confidence * 100}%")
                ClassificationResult(label, confidence)
            }.sortedByDescending { it.confidence }

            Log.i("ModelHelper", "Top prediction: ${results[0].label} with ${results[0].confidence * 100}%")

            results

        } catch (e: Exception) {
            Log.e("ModelHelper", "Classification error", e)
            e.printStackTrace()
            emptyList()
        }
    }

    private fun softmax(arr: FloatArray): FloatArray {
        val max = arr.maxOrNull() ?: 0f
        val expVals = arr.map { exp((it - max).toDouble()) }
        val sum = expVals.sum()
        return expVals.map { (it / sum).toFloat() }.toFloatArray()
    }

    fun close() {
        interpreter?.close()
    }
}