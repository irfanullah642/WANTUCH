package com.example.wantuch.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.sqrt

class FaceRecognitionEngine(private val context: Context) {
    private val wrapper = TfLiteWrapper()
    private val executor = Executors.newSingleThreadExecutor()

    // Model constants (adjust based on your .tflite model)
    private var IMG_SIZE = 160 // Dynamically updated on model load
    private var EMBEDDING_SIZE = 512
    init {
        wrapper.initialize(context)
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = context.assets.open("mobile_facenet.tflite").readBytes()
            val byteBuffer = ByteBuffer.allocateDirect(modelBuffer.size)
            byteBuffer.order(ByteOrder.nativeOrder())
            byteBuffer.put(modelBuffer)
            wrapper.loadModel(byteBuffer)
            IMG_SIZE = wrapper.inputSize
            EMBEDDING_SIZE = wrapper.outputSize
            Log.d("FaceEngine", "Model loaded successfully. Shape: $IMG_SIZE -> $EMBEDDING_SIZE")
        } catch (e: Exception) {
            Log.e("FaceEngine", "Error loading model: ${e.message}")
        }
    }

    fun getEmbedding(bitmap: Bitmap): FloatArray? {
        if (!wrapper.isReady) {
            Log.e("FaceEngine", "Wrapper not ready — model failed to load")
            return null
        }

        return try {
            // Manual Pre-processing (Replaces TFLite Support)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true)
            val inputBuffer = ByteBuffer.allocateDirect(4 * IMG_SIZE * IMG_SIZE * 3)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            val intValues = IntArray(IMG_SIZE * IMG_SIZE)
            scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
            
            if (wrapper.isModelNCHW()) {
                // Layout: RRR... GGG... BBB...
                for (pixel in intValues) { inputBuffer.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 128.0f) }
                for (pixel in intValues) { inputBuffer.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 128.0f) }
                for (pixel in intValues) { inputBuffer.putFloat(((pixel and 0xFF) - 127.5f) / 128.0f) }
            } else {
                // Layout: RGB RGB RGB
                for (pixel in intValues) {
                    inputBuffer.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 128.0f)
                    inputBuffer.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 128.0f)
                    inputBuffer.putFloat(((pixel and 0xFF) - 127.5f) / 128.0f)
                }
            }

            val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
            wrapper.runInference(inputBuffer, output)
            
            val outputArray = output[0]
            var norm = 0.0f
            for (v in outputArray) { norm += v * v }
            norm = kotlin.math.sqrt(norm.toDouble()).toFloat()
            if (norm > 0f) {
                for (i in outputArray.indices) { outputArray[i] = outputArray[i] / norm }
            }
            return outputArray
        } catch (e: Exception) {
            Log.e("FaceEngine", "Inference error: ${e.message}")
            null
        }
    }

    /**
     * Calculates Euclidean (L2) Distance between two embeddings.
     * Lower is more similar. Standard FaceNet match threshold is usually ~1.0
     */
    fun calculateL2Distance(emb1: FloatArray, emb2: FloatArray): Float {
        var sum = 0.0f
        for (i in emb1.indices) {
            val diff = emb1[i] - emb2[i]
            sum += diff * diff
        }
        return kotlin.math.sqrt(sum.toDouble()).toFloat()
    }
    
    /**
     * Calculates Cosine Similarity between two embeddings.
     * Higher is more similar. Range: [-1, 1].
     */
    fun calculateSimilarity(emb1: FloatArray, emb2: FloatArray): Float {
        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f
        for (i in emb1.indices) {
            dotProduct += emb1[i] * emb2[i]
            norm1 += emb1[i] * emb1[i]
            norm2 += emb2[i] * emb2[i]
        }
        return dotProduct / (sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())).toFloat()
    }
}
