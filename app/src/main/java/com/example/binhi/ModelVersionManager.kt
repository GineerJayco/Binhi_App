package com.example.binhi

import android.content.Context
import android.util.Log
import android.content.SharedPreferences

/**
 * ModelVersionManager - Manages ONNX model versioning and updates
 *
 * This helps track when models are updated and ensures
 * new trained models are used instead of cached ones
 *
 * Usage:
 * ```
 * val versionManager = ModelVersionManager(context)
 * if (versionManager.isNewModelAvailable()) {
 *     Log.d(TAG, "New model detected - will be loaded")
 *     versionManager.recordModelLoad()
 * }
 * ```
 */
class ModelVersionManager(private val context: Context) {
    companion object {
        private const val TAG = "ModelVersionManager"
        private const val PREFS_NAME = "model_version_prefs"
        private const val KEY_MODEL_HASH = "last_model_hash"
        private const val KEY_LOAD_COUNT = "model_load_count"
        private const val KEY_LAST_UPDATE = "last_model_update"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Calculate hash of model file to detect changes
     */
    fun calculateModelHash(): String {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open("xgboost.onnx")
            val bytes = inputStream.readBytes()
            inputStream.close()

            // Simple hash based on file size and first/last bytes
            val fileSize = bytes.size.toLong()
            val firstBytes = bytes.take(32).joinToString("") { "%02x".format(it) }
            val lastBytes = bytes.takeLast(32).joinToString("") { "%02x".format(it) }

            "$fileSize:${firstBytes.take(16)}:${lastBytes.takeLast(16)}".hashCode().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating model hash: ${e.message}")
            "unknown"
        }
    }

    /**
     * Check if a new model version is available
     */
    fun isNewModelAvailable(): Boolean {
        val currentHash = calculateModelHash()
        val savedHash = prefs.getString(KEY_MODEL_HASH, "")

        val isNew = currentHash != savedHash
        Log.d(TAG, "Current hash: $currentHash")
        Log.d(TAG, "Saved hash: $savedHash")
        Log.d(TAG, "Is new model: $isNew")

        return isNew
    }

    /**
     * Record that model was loaded (update saved hash)
     */
    fun recordModelLoad() {
        val currentHash = calculateModelHash()
        val timestamp = System.currentTimeMillis()
        val loadCount = (prefs.getInt(KEY_LOAD_COUNT, 0) + 1)

        prefs.edit().apply {
            putString(KEY_MODEL_HASH, currentHash)
            putLong(KEY_LAST_UPDATE, timestamp)
            putInt(KEY_LOAD_COUNT, loadCount)
            apply()
        }

        Log.d(TAG, "Model load recorded")
        Log.d(TAG, "  Hash: $currentHash")
        Log.d(TAG, "  Timestamp: $timestamp")
        Log.d(TAG, "  Load count: $loadCount")
    }

    /**
     * Get model load information
     */
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "hash" to (prefs.getString(KEY_MODEL_HASH, "") ?: ""),
            "loadCount" to prefs.getInt(KEY_LOAD_COUNT, 0),
            "lastUpdate" to prefs.getLong(KEY_LAST_UPDATE, 0),
            "currentHash" to calculateModelHash(),
            "isNew" to isNewModelAvailable()
        )
    }

    /**
     * Reset all model version tracking
     */
    fun reset() {
        prefs.edit().apply {
            remove(KEY_MODEL_HASH)
            remove(KEY_LOAD_COUNT)
            remove(KEY_LAST_UPDATE)
            apply()
        }
        Log.d(TAG, "Model version tracking reset")
    }

    /**
     * Print model info to logcat
     */
    fun logModelInfo() {
        val info = getModelInfo()
        Log.d(TAG, "========== Model Version Info ==========")
        Log.d(TAG, "Current Hash: ${info["currentHash"]}")
        Log.d(TAG, "Saved Hash: ${info["hash"]}")
        Log.d(TAG, "Is New Model: ${info["isNew"]}")
        Log.d(TAG, "Load Count: ${info["loadCount"]}")
        Log.d(TAG, "Last Update: ${info["lastUpdate"]}")
        Log.d(TAG, "========================================")
    }
}

/**
 * Extension function to initialize model with version checking
 *
 * Usage in MainActivity:
 * ```
 * val modelRunner = OnnxModelRunner.getInstance(this)
 * val versionManager = ModelVersionManager(this)
 * if (versionManager.isNewModelAvailable()) {
 *     modelRunner.forceRefreshModel()
 * }
 * versionManager.recordModelLoad()
 * versionManager.logModelInfo()
 * ```
 */
fun OnnxModelRunner.initializeWithVersionCheck(context: Context) {
    val versionManager = ModelVersionManager(context)

    Log.d("ModelInit", "Checking model version...")
    versionManager.logModelInfo()

    if (versionManager.isNewModelAvailable()) {
        Log.d("ModelInit", "New model detected - forcing refresh")
        try {
            this.forceRefreshModel()
            versionManager.recordModelLoad()
        } catch (e: Exception) {
            Log.e("ModelInit", "Error during forced refresh: ${e.message}")
        }
    } else {
        Log.d("ModelInit", "Using existing model")
        this.initializeEnvironment()
    }
}

