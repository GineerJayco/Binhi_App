package com.example.binhi

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import ai.onnxruntime.OnnxSequence
import ai.onnxruntime.OnnxMap

/**
 * OnnxModelRunner - Manages ONNX Runtime environment and model inference
 * Provides a singleton pattern for efficient model lifecycle management
 *
 * Handles:
 * - ONNX Runtime environment initialization
 * - Model loading from assets or files
 * - Session creation and management
 * - Inference execution with proper error handling
 * - Resource cleanup
 */
class OnnxModelRunner(
    private val context: Context,
    private val modelFileName: String = "crop_recommendation_model.onnx"
) {

    companion object {
        private const val TAG = "OnnxModelRunner"

        // Singleton instance - efficient for repeated inference calls
        @Volatile
        private var instance: OnnxModelRunner? = null

        fun getInstance(
            context: Context,
            modelFileName: String = "crop_recommendation_model.onnx"
        ): OnnxModelRunner {
            return instance ?: synchronized(this) {
                instance ?: OnnxModelRunner(context, modelFileName).also {
                    instance = it
                    it.initializeEnvironment()
                }
            }
        }
    }

    // ONNX Runtime environment - shared across all sessions
    private var env: OrtEnvironment? = null

    // Current session
    private var session: OrtSession? = null

    // Model path
    private var modelPath: String? = null

    // Input and output names
    private var inputName: String? = null
    private var outputNames: List<String>? = null

    // Initialization flag
    private var isInitialized = false

    /**
     * Initialize the ONNX Runtime environment and load model
     */
    fun initializeEnvironment() {
        try {
            if (isInitialized && session != null) {
                Log.d(TAG, "ONNX environment already initialized")
                return
            }

            // Get or create ONNX Runtime environment
            env = OrtEnvironment.getEnvironment()
            Log.d(TAG, "ONNX Runtime environment initialized")

            // Copy model from assets to internal storage
            // This will automatically detect new models deployed (size checking)
            modelPath = copyAssetToInternalStorage(context, modelFileName)
            if (modelPath == null) {
                throw IOException("Failed to copy model file from assets")
            }

            Log.d(TAG, "Model path: $modelPath")

            // Create session with options
            val sessionOptions = OrtSession.SessionOptions()
            session = env!!.createSession(modelPath)

            // Extract input and output names
            inputName = session?.inputNames?.firstOrNull()
                ?: throw IllegalStateException("No input names found in model")

            outputNames = session?.outputNames?.toList() ?: emptyList()

            Log.d(TAG, "ONNX model session created successfully")
            Log.d(TAG, "Input name: $inputName")
            Log.d(TAG, "Output names: $outputNames")

            isInitialized = true

        } catch (e: OrtException) {
            Log.e(TAG, "ONNX Runtime error: ${e.message}", e)
            session = null
            isInitialized = false
            throw RuntimeException("Failed to initialize ONNX model", e)
        } catch (e: IOException) {
            Log.e(TAG, "IO error: ${e.message}", e)
            session = null
            isInitialized = false
            throw RuntimeException("Failed to load model from assets", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during initialization: ${e.message}", e)
            session = null
            isInitialized = false
            throw RuntimeException("Failed to initialize ONNX model", e)
        }
    }

    /**
     * Force refresh the model from assets
     * Useful when a new model is deployed (development/testing)
     * Closes current session and reinitializes with fresh model copy
     */
    fun forceRefreshModel() {
        Log.d(TAG, "Force refreshing model from assets...")
        try {
            // Close current session
            session?.close()
            session = null
            isInitialized = false

            // Delete cached model file to force re-copy
            try {
                val cachedFile = File(context.filesDir, modelFileName)
                if (cachedFile.exists()) {
                    val deleted = cachedFile.delete()
                    Log.d(TAG, "Deleted cached model: $deleted")
                } else {
                    Log.d(TAG, "No cached model file found")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting cached model: ${e.message}")
            }

            // Reinitialize with fresh model
            initializeEnvironment()
            Log.d(TAG, "Model refresh completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during model refresh: ${e.message}", e)
            throw RuntimeException("Failed to refresh model", e)
        }
    }

    /**
     * Copy model file from assets to internal storage
     * This is necessary because ONNX Runtime needs file path access
     *
     * IMPORTANT: Always checks file size to detect when model is updated
     * This ensures new trained models are not cached
     */
    private fun copyAssetToInternalStorage(
        context: Context,
        assetFileName: String
    ): String? {
        return try {
            val assetManager: AssetManager = context.assets
            val inputStream = assetManager.open(assetFileName)
            val outFile = File(context.filesDir, assetFileName)

            // Get asset file size
            val assetSize = inputStream.available().toLong()
            Log.d(TAG, "Asset file size: $assetSize bytes")

            // Check if file exists and compare sizes
            if (outFile.exists()) {
                val existingSize = outFile.length()
                Log.d(TAG, "Model file already exists: ${outFile.absolutePath}")
                Log.d(TAG, "Existing file size: $existingSize bytes, Asset size: $assetSize bytes")

                // If sizes match, skip copy (same model)
                if (existingSize == assetSize) {
                    Log.d(TAG, "File sizes match - using existing model")
                    return outFile.absolutePath
                } else {
                    // Size mismatch - this means a new model was deployed!
                    Log.d(TAG, "File size mismatch detected! New model deployed.")
                    Log.d(TAG, "Deleting old model and copying new one...")
                    outFile.delete()
                }
            }

            val outputStream = FileOutputStream(outFile)
            val buffer = ByteArray(8192) // Larger buffer for faster copy
            var bytesRead: Int
            var totalBytes = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }

            inputStream.close()
            outputStream.close()

            // Verify file exists and has content
            if (!outFile.exists() || outFile.length() == 0L) {
                Log.e(TAG, "Model file copy failed or file is empty")
                return null
            }

            Log.d(TAG, "Model copied successfully: ${outFile.absolutePath}")
            Log.d(TAG, "Copied $totalBytes bytes total")
            outFile.absolutePath

        } catch (e: IOException) {
            Log.e(TAG, "Error copying model from assets: ${e.message}", e)
            null
        }
    }

    /**
     * Run inference on raw soil data
     * Input: 2D array of shape [batch_size, 6] with features: N, P, K, pH, Temp, Moisture
     * Returns: Raw output from the model
     */
    @Throws(OrtException::class)
    fun runInference(inputData: Array<FloatArray>): Map<String, Any?> {
        if (!isInitialized || session == null) {
            throw IllegalStateException("ONNX model not initialized. Call initializeEnvironment() first.")
        }

        return try {
            Log.d(TAG, "Running inference with input shape [${inputData.size}, ${inputData[0].size}]")
            Log.d(TAG, "Input data: ${inputData[0].contentToString()}")

            // Create input tensor
            val inputTensor = OnnxTensor.createTensor(env!!, inputData)
            Log.d(TAG, "Input tensor created successfully")

            // Run inference
            val results = session!!.run(mapOf(inputName!! to inputTensor))
            Log.d(TAG, "Inference completed")

            // Unwrap Optional types and log
            val processedResults = mutableMapOf<String, Any?>()
            outputNames?.forEach { name ->
                val output = results[name]
                val unwrappedOutput = unwrapOptional(output)
                processedResults[name] = unwrappedOutput
                Log.d(TAG, "Output '$name' type: ${unwrappedOutput?.javaClass?.simpleName ?: "null"}")
            }

            // Return processed results with Optionals unwrapped
            processedResults

        } catch (e: OrtException) {
            Log.e(TAG, "ONNX Runtime inference error: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during inference: ${e.message}", e)
            throw RuntimeException("Inference failed", e)
        }
    }

    /**
     * Unwrap Java Optional type if present
     */
    private fun unwrapOptional(obj: Any?): Any? {
        if (obj == null) return null

        return if (obj.javaClass.simpleName == "Optional") {
            try {
                val getMethod = obj.javaClass.getMethod("get")
                getMethod.invoke(obj)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unwrap Optional: ${e.message}")
                obj
            }
        } else {
            obj
        }
    }

    /**
     * Extract probability scores from model output
     * Handles various output formats: Optional<*>, List<Map>, Map, Tensor, OnnxSequence
     */
    fun extractProbabilities(output: Any?, expectedSize: Int): List<Float> {
        val scores = mutableListOf<Float>()

        return try {
            // Unwrap Optional if present
            val unwrappedOutput = when {
                output != null && output.javaClass.simpleName == "Optional" -> {
                    // Use reflection to get the value from Optional
                    try {
                        val getMethod = output.javaClass.getMethod("get")
                        getMethod.invoke(output)
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed to unwrap Optional: ${e.message}")
                        output
                    }
                }
                else -> output
            }

            Log.d(TAG, "Processing output type: ${unwrappedOutput?.javaClass?.simpleName ?: "null"}")

            when {
                unwrappedOutput is OnnxSequence -> {
                    // OnnxSequence format: Sequence<Map<Int, Float>> or Map<String, Float>
                    Log.d(TAG, "Processing OnnxSequence")
                    scores.addAll(extractFromOnnxSequence(unwrappedOutput, expectedSize))
                    if (scores.isNotEmpty()) {
                        Log.d(TAG, "Extracted ${scores.size} probability scores from OnnxSequence")
                    }
                }

                unwrappedOutput is List<*> && unwrappedOutput.isNotEmpty() -> {
                    // output_probability is returned as List<Map<*, *>>
                    val firstElement = unwrappedOutput[0]
                    if (firstElement is Map<*, *>) {
                        for (i in 0 until expectedSize) {
                            val rawValue = firstElement[i]
                            val floatValue = when (rawValue) {
                                is Double -> rawValue.toFloat()
                                is Float -> rawValue
                                is Number -> rawValue.toFloat()
                                else -> 0f
                            }
                            scores.add(floatValue)
                        }
                        Log.d(TAG, "Extracted ${scores.size} probability scores from List<Map>")
                    } else {
                        Log.w(TAG, "First element of output list is not a Map: ${firstElement?.javaClass?.simpleName}")
                    }
                }

                unwrappedOutput is Map<*, *> -> {
                    // Direct map format (index-based or label-based keys)
                    Log.d(TAG, "Processing direct Map with ${unwrappedOutput.size} entries")

                    // Try numeric keys first (0, 1, 2, 3, 4)
                    val numericSuccess = extractFromNumericMap(unwrappedOutput, expectedSize, scores)

                    if (scores.isEmpty() && !numericSuccess) {
                        // Fall back to string keys if numeric keys didn't work
                        Log.d(TAG, "Numeric keys not found, trying string keys")
                        extractFromStringMap(unwrappedOutput, expectedSize, scores)
                    }

                    if (scores.isNotEmpty()) {
                        Log.d(TAG, "Extracted ${scores.size} probability scores from Map")
                    }
                }

                unwrappedOutput is OnnxTensor -> {
                    // Float tensor format
                    val floatBuffer = unwrappedOutput.floatBuffer
                    while (floatBuffer.hasRemaining()) {
                        scores.add(floatBuffer.get())
                    }
                    Log.d(TAG, "Extracted ${scores.size} probability scores from OnnxTensor")
                }

                else -> {
                    Log.e(TAG, "Unexpected output type: ${unwrappedOutput?.javaClass?.simpleName ?: "null"}")
                }
            }
            scores
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting probabilities: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Extract probabilities from OnnxSequence
     * Handles Sequence<Map<Int, Float>> and Sequence<Map<String, Float>> formats
     * Uses getValue() method and safe casting with detailed debugging
     */
    private fun extractFromOnnxSequence(
        sequence: OnnxSequence,
        expectedSize: Int
    ): List<Float> {
        val scores = mutableListOf<Float>()

        return try {
            Log.d(TAG, "=== Starting OnnxSequence Extraction ===")
            Log.d(TAG, "OnnxSequence class: ${sequence.javaClass.name}")
            Log.d(TAG, "OnnxSequence type: ${sequence.javaClass.simpleName}")

            // Method 1: Try to get values using getValue() method
            val sequenceValues = try {
                Log.d(TAG, "Attempting to extract values using getValue() method...")
                val value = sequence.getValue()
                Log.d(TAG, "getValue() returned type: ${value?.javaClass?.simpleName ?: "null"}")
                Log.d(TAG, "getValue() returned value: $value")

                // Try to cast to List
                when (value) {
                    is List<*> -> {
                        Log.d(TAG, "getValue() returned List with ${value.size} elements")
                        value
                    }
                    is Iterable<*> -> {
                        Log.d(TAG, "getValue() returned Iterable, converting to List")
                        value.toList()
                    }
                    else -> {
                        Log.d(TAG, "getValue() returned unexpected type, attempting to iterate as Iterable")
                        (value as? Iterable<*>)?.toList() ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "getValue() failed: ${e.message}, trying direct iteration...")
                try {
                    // Method 2: Direct iteration as Iterable
                    Log.d(TAG, "Attempting direct iteration as Iterable...")
                    (sequence as? Iterable<*>)?.toList() ?: emptyList()
                } catch (e2: Exception) {
                    Log.d(TAG, "Direct iteration also failed: ${e2.message}")
                    emptyList()
                }
            }

            Log.d(TAG, "Extracted sequence values count: ${sequenceValues.size}")

            // Debug: Print all elements in sequence
            if (sequenceValues.isNotEmpty()) {
                Log.d(TAG, "=== Sequence Elements Debug ===")
                sequenceValues.forEachIndexed { index, element ->
                    Log.d(TAG, "Element[$index] type: ${element?.javaClass?.simpleName ?: "null"}")
                    when (element) {
                        is OnnxMap -> {
                            Log.d(TAG, "Element[$index] is OnnxMap")
                            Log.d(TAG, "Element[$index] OnnxMap class: ${element.javaClass.name}")
                        }
                        is Map<*, *> -> {
                            Log.d(TAG, "Element[$index] is Map with ${element.size} entries")
                            Log.d(TAG, "Element[$index] Map keys: ${element.keys.take(5)}")
                            Log.d(TAG, "Element[$index] Map sample: ${element.entries.take(2)}")
                        }
                        is OnnxTensor -> {
                            Log.d(TAG, "Element[$index] is OnnxTensor")
                        }
                        is List<*> -> {
                            Log.d(TAG, "Element[$index] is List with ${element.size} items")
                        }
                        else -> {
                            Log.d(TAG, "Element[$index] value: $element")
                        }
                    }
                }
            }

            // Process the extracted values
            if (sequenceValues.isNotEmpty()) {
                val firstElement = sequenceValues[0]
                Log.d(TAG, "Processing first element of type: ${firstElement?.javaClass?.simpleName ?: "null"}")

                when (firstElement) {
                    is OnnxMap -> {
                        Log.d(TAG, "Processing OnnxMap from OnnxSequence")
                        scores.addAll(extractFromOnnxMap(firstElement, expectedSize))
                        if (scores.isNotEmpty()) {
                            Log.d(TAG, "Successfully extracted ${scores.size} scores from OnnxMap")
                        }
                    }

                    is Map<*, *> -> {
                        Log.d(TAG, "Processing Map from OnnxSequence with ${firstElement.size} entries")
                        Log.d(TAG, "Map keys (first 10): ${firstElement.keys.take(10)}")

                        // Try numeric keys first
                        val numericSuccess = extractFromNumericMap(firstElement, expectedSize, scores)
                        Log.d(TAG, "Numeric key extraction: ${if (numericSuccess) "SUCCESS" else "FAILED"}")

                        if (scores.isEmpty() && !numericSuccess) {
                            // Fall back to string keys
                            Log.d(TAG, "Falling back to string key extraction...")
                            val stringSuccess = extractFromStringMap(firstElement, expectedSize, scores)
                            Log.d(TAG, "String key extraction: ${if (stringSuccess) "SUCCESS" else "FAILED"}")
                        }
                    }

                    is OnnxTensor -> {
                        Log.d(TAG, "Processing OnnxTensor from OnnxSequence")
                        try {
                            val floatBuffer = (firstElement as OnnxTensor).floatBuffer
                            while (floatBuffer.hasRemaining()) {
                                scores.add(floatBuffer.get())
                            }
                            Log.d(TAG, "Extracted ${scores.size} scores from OnnxTensor")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to extract from OnnxTensor: ${e.message}")
                        }
                    }

                    is List<*> -> {
                        Log.d(TAG, "First element is nested List with ${firstElement.size} items")
                        // Handle nested lists
                        if (firstElement.isNotEmpty()) {
                            val nestedFirst = firstElement[0]
                            Log.d(TAG, "Nested element type: ${nestedFirst?.javaClass?.simpleName}")
                            when (nestedFirst) {
                                is OnnxMap -> {
                                    Log.d(TAG, "Processing nested OnnxMap")
                                    scores.addAll(extractFromOnnxMap(nestedFirst, expectedSize))
                                }
                                is Map<*, *> -> {
                                    Log.d(TAG, "Processing nested Map")
                                    extractFromNumericMap(nestedFirst, expectedSize, scores)
                                    if (scores.isEmpty()) {
                                        extractFromStringMap(nestedFirst, expectedSize, scores)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Log.w(TAG, "Unknown first element type in OnnxSequence: ${firstElement?.javaClass?.simpleName}")
                        Log.d(TAG, "First element value: $firstElement")
                    }
                }
            } else {
                Log.w(TAG, "OnnxSequence is empty after extraction")
            }

            Log.d(TAG, "=== OnnxSequence Extraction Complete ===")
            Log.d(TAG, "Final extracted scores: $scores")
            scores

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error extracting from OnnxSequence: ${e.message}", e)
            e.printStackTrace()
            scores
        }
    }

    /**
     * Extract probabilities from OnnxMap
     * Handles OnnxMap<Long, Float> and OnnxMap<Integer, Float> formats
     * Uses .value property to get the underlying map
     */
    private fun extractFromOnnxMap(
        onnxMap: OnnxMap,
        expectedSize: Int
    ): List<Float> {
        val scores = mutableListOf<Float>()

        return try {
            Log.d(TAG, "=== Starting OnnxMap Extraction ===")
            Log.d(TAG, "OnnxMap class: ${onnxMap.javaClass.name}")
            Log.d(TAG, "OnnxMap type: ${onnxMap.javaClass.simpleName}")

            // Get the underlying map using .value property
            val mapValue = try {
                Log.d(TAG, "Attempting to extract map using .value property...")
                val value = onnxMap.value
                Log.d(TAG, "OnnxMap.value type: ${value?.javaClass?.simpleName ?: "null"}")
                Log.d(TAG, "OnnxMap.value size: ${(value as? Map<*, *>)?.size ?: "unknown"}")
                value
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get OnnxMap.value: ${e.message}")
                null
            }

            if (mapValue == null) {
                Log.w(TAG, "OnnxMap.value is null")
                return scores
            }

            if (mapValue !is Map<*, *>) {
                Log.e(TAG, "OnnxMap.value is not a Map, got: ${mapValue.javaClass.simpleName}")
                return scores
            }

            Log.d(TAG, "OnnxMap contains ${mapValue.size} entries")
            if (mapValue.isNotEmpty()) {
                Log.d(TAG, "Sample keys: ${mapValue.keys.take(5)}")
                val sampleEntry = mapValue.entries.first()
                Log.d(TAG, "Sample entry - Key type: ${sampleEntry.key?.javaClass?.simpleName}, Value type: ${sampleEntry.value?.javaClass?.simpleName}")
            }

            // Try Long keys first (0L, 1L, 2L, 3L, 4L)
            Log.d(TAG, "Attempting to extract using Long keys...")
            var successCount = 0
            for (i in 0 until expectedSize) {
                try {
                    val longKey = i.toLong()
                    val rawValue = mapValue[longKey]

                    if (rawValue != null) {
                        Log.d(TAG, "Long key[$longKey] found: value type = ${rawValue.javaClass.simpleName}, value = $rawValue")
                        val floatValue = when (rawValue) {
                            is Double -> {
                                Log.d(TAG, "Long key[$longKey]: Converting Double($rawValue) to Float")
                                rawValue.toFloat()
                            }
                            is Float -> {
                                Log.d(TAG, "Long key[$longKey]: Float value = $rawValue")
                                rawValue
                            }
                            is Number -> {
                                Log.d(TAG, "Long key[$longKey]: Converting Number($rawValue) to Float")
                                rawValue.toFloat()
                            }
                            else -> {
                                Log.w(TAG, "Long key[$longKey]: Unknown type ${rawValue.javaClass.simpleName}, using 0f")
                                0f
                            }
                        }
                        scores.add(floatValue)
                        successCount++
                    } else {
                        Log.d(TAG, "Long key[$longKey]: not found in map")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Long key[$i]: Exception while extracting: ${e.message}")
                }
            }

            Log.d(TAG, "Long key extraction result: $successCount/$expectedSize scores extracted")

            if (successCount == 0) {
                // Fallback: Try integer keys
                Log.d(TAG, "Long keys failed, attempting Integer keys...")
                for (i in 0 until expectedSize) {
                    try {
                        val intKey = i
                        val rawValue = mapValue[intKey]

                        if (rawValue != null) {
                            Log.d(TAG, "Integer key[$intKey] found: value type = ${rawValue.javaClass.simpleName}, value = $rawValue")
                            val floatValue = when (rawValue) {
                                is Double -> rawValue.toFloat()
                                is Float -> rawValue
                                is Number -> rawValue.toFloat()
                                else -> 0f
                            }
                            scores.add(floatValue)
                            successCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Integer key[$i]: Exception while extracting: ${e.message}")
                    }
                }
                Log.d(TAG, "Integer key extraction result: $successCount/$expectedSize scores extracted")
            }

            if (successCount == 0) {
                // Final fallback: Try any numeric keys in order
                Log.d(TAG, "Numeric keys failed, attempting arbitrary keys...")
                mapValue.keys.take(expectedSize).forEachIndexed { index, key ->
                    try {
                        val rawValue = mapValue[key]
                        if (rawValue != null) {
                            Log.d(TAG, "Key[$key] (index $index): extracted value type = ${rawValue.javaClass.simpleName}")
                            val floatValue = when (rawValue) {
                                is Double -> rawValue.toFloat()
                                is Float -> rawValue
                                is Number -> rawValue.toFloat()
                                else -> 0f
                            }
                            scores.add(floatValue)
                            successCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Key[$key]: Exception: ${e.message}")
                    }
                }
                Log.d(TAG, "Arbitrary key extraction result: $successCount/$expectedSize scores extracted")
            }

            Log.d(TAG, "=== OnnxMap Extraction Complete ===")
            Log.d(TAG, "Final extracted scores: $scores")
            scores

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error extracting from OnnxMap: ${e.message}", e)
            e.printStackTrace()
            scores
        }
    }

    /**
     * Extract from numeric-keyed map (0, 1, 2, 3, 4)
     * Returns true if extraction was successful
     * Includes detailed debugging for key type inspection
     */
    private fun extractFromNumericMap(
        map: Map<*, *>,
        expectedSize: Int,
        scores: MutableList<Float>
    ): Boolean {
        return try {
            Log.d(TAG, "=== Starting Numeric Map Extraction ===")
            Log.d(TAG, "Map size: ${map.size}")
            Log.d(TAG, "Expected size: $expectedSize")

            // Inspect key types
            if (map.isNotEmpty()) {
                val keyTypes = map.keys.map { it?.javaClass?.simpleName ?: "null" }.distinct()
                Log.d(TAG, "Key types found: $keyTypes")
                Log.d(TAG, "Sample keys: ${map.keys.take(5)}")
            }

            var successCount = 0
            var failedKeys = mutableListOf<Int>()

            for (i in 0 until expectedSize) {
                try {
                    val rawValue = map[i]
                    if (rawValue != null) {
                        Log.d(TAG, "Key[$i] found: value type = ${rawValue.javaClass.simpleName}, value = $rawValue")
                        val floatValue = when (rawValue) {
                            is Double -> {
                                Log.d(TAG, "Key[$i]: Converting Double($rawValue) to Float")
                                rawValue.toFloat()
                            }
                            is Float -> {
                                Log.d(TAG, "Key[$i]: Float value = $rawValue")
                                rawValue
                            }
                            is Number -> {
                                Log.d(TAG, "Key[$i]: Converting Number($rawValue) to Float")
                                rawValue.toFloat()
                            }
                            else -> {
                                Log.w(TAG, "Key[$i]: Unknown type ${rawValue.javaClass.simpleName}, using 0f")
                                0f
                            }
                        }
                        scores.add(floatValue)
                        successCount++
                    } else {
                        Log.d(TAG, "Key[$i]: not found in map")
                        failedKeys.add(i)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Key[$i]: Exception while extracting: ${e.message}")
                    failedKeys.add(i)
                }
            }

            Log.d(TAG, "Numeric extraction result: $successCount/$expectedSize scores extracted")
            if (failedKeys.isNotEmpty()) {
                Log.d(TAG, "Failed keys: $failedKeys")
            }
            Log.d(TAG, "=== Numeric Map Extraction Complete ===")

            successCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in numeric map extraction: ${e.message}", e)
            false
        }
    }

    /**
     * Extract from string-keyed map (crop names or other labels)
     * Supports both ordered crops and arbitrary labels
     * Includes detailed debugging for key matching
     */
    private fun extractFromStringMap(
        map: Map<*, *>,
        expectedSize: Int,
        scores: MutableList<Float>
    ): Boolean {
        return try {
            Log.d(TAG, "=== Starting String Map Extraction ===")
            Log.d(TAG, "Map size: ${map.size}")
            Log.d(TAG, "Expected size: $expectedSize")

            // Inspect key types
            if (map.isNotEmpty()) {
                val keyTypes = map.keys.map { it?.javaClass?.simpleName ?: "null" }.distinct()
                Log.d(TAG, "Key types found: $keyTypes")
                Log.d(TAG, "All keys in map: ${map.keys}")
            }

            // List of crop names in expected order
            val cropNames = CropConstants.CROP_NAMES
            Log.d(TAG, "Trying crop names: $cropNames")

            // Try to match keys by crop name
            var successCount = 0
            for (cropName in cropNames.take(expectedSize)) {
                try {
                    val rawValue = map[cropName]
                    if (rawValue != null) {
                        Log.d(TAG, "Crop[$cropName] found: value type = ${rawValue.javaClass.simpleName}, value = $rawValue")
                        val floatValue = when (rawValue) {
                            is Double -> {
                                Log.d(TAG, "Crop[$cropName]: Converting Double($rawValue) to Float")
                                rawValue.toFloat()
                            }
                            is Float -> {
                                Log.d(TAG, "Crop[$cropName]: Float value = $rawValue")
                                rawValue
                            }
                            is Number -> {
                                Log.d(TAG, "Crop[$cropName]: Converting Number($rawValue) to Float")
                                rawValue.toFloat()
                            }
                            else -> {
                                Log.w(TAG, "Crop[$cropName]: Unknown type ${rawValue.javaClass.simpleName}, using 0f")
                                0f
                            }
                        }
                        scores.add(floatValue)
                        successCount++
                    } else {
                        Log.d(TAG, "Crop[$cropName] not found in map")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Crop[$cropName]: Exception while extracting: ${e.message}")
                }
            }

            if (successCount == 0) {
                // If crop names don't match, try any string keys in order
                Log.d(TAG, "Crop names not found, trying arbitrary string keys...")
                map.keys.take(expectedSize).forEach { key ->
                    try {
                        val rawValue = map[key]
                        if (rawValue != null) {
                            Log.d(TAG, "Key[$key] found: value type = ${rawValue.javaClass.simpleName}, value = $rawValue")
                            val floatValue = when (rawValue) {
                                is Double -> {
                                    Log.d(TAG, "Key[$key]: Converting Double($rawValue) to Float")
                                    rawValue.toFloat()
                                }
                                is Float -> {
                                    Log.d(TAG, "Key[$key]: Float value = $rawValue")
                                    rawValue
                                }
                                is Number -> {
                                    Log.d(TAG, "Key[$key]: Converting Number($rawValue) to Float")
                                    rawValue.toFloat()
                                }
                                else -> {
                                    Log.w(TAG, "Key[$key]: Unknown type ${rawValue.javaClass.simpleName}, using 0f")
                                    0f
                                }
                            }
                            scores.add(floatValue)
                            successCount++
                        } else {
                            Log.d(TAG, "Key[$key]: value is null")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Key[$key]: Exception while extracting: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "String extraction result: $successCount/$expectedSize scores extracted")
            Log.d(TAG, "=== String Map Extraction Complete ===")

            successCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in string map extraction: ${e.message}", e)
            false
        }
    }

    /**
     * Get input tensor name
     */
    fun getInputName(): String? = inputName

    /**
     * Get output tensor names
     */
    fun getOutputNames(): List<String>? = outputNames

    /**
     * Check if model is initialized and ready for inference
     */
    fun isReady(): Boolean = isInitialized && session != null

    /**
     * Close and cleanup resources
     */
    fun close() {
        try {
            session?.close()
            session = null
            Log.d(TAG, "ONNX session closed")
        } catch (e: OrtException) {
            Log.e(TAG, "Error closing session: ${e.message}", e)
        }
    }

    /**
     * Release the singleton instance
     */
    fun releaseInstance() {
        close()
        instance = null
        isInitialized = false
        Log.d(TAG, "OnnxModelRunner instance released")
    }
}



