# Model Caching Fix - Quick Integration Guide

## The Problem You Faced

Your ONNX model is cached on the Android device. When you replaced the model file with a newly trained one, the app still used the old cached model because:

```
OLD CODE:
if (outFile.exists()) {
    return outFile.absolutePath  // ❌ Always uses cached file
}
```

## The Solution

### Option 1: Automatic File Size Detection (Recommended - Already Implemented)

The updated `OnnxModelRunner.kt` now **automatically detects** when a new model is deployed:

```kotlin
// NEW CODE:
val assetSize = inputStream.available().toLong()
val existingSize = outFile.length()

if (existingSize == assetSize) {
    return outFile.absolutePath  // Same model
} else {
    outFile.delete()  // New model detected - delete cache
    // Copy new model
}
```

**How it works:**
- Every time you train a new model, it likely has a different file size
- App detects the size difference
- **Automatically** deletes old cached model
- **Automatically** copies new trained model ✓

### Option 2: Manual Model Refresh (For Testing)

```kotlin
// When you need to force reload the model
val modelRunner = OnnxModelRunner.getInstance(context)
modelRunner.forceRefreshModel()
```

### Option 3: Version-Based Model Tracking (Advanced - Optional)

Use the new `ModelVersionManager` for explicit version tracking:

```kotlin
// In your MainActivity or wherever you initialize the model
val modelRunner = OnnxModelRunner.getInstance(this)
val versionManager = ModelVersionManager(this)

// Check and load with version checking
modelRunner.initializeWithVersionCheck(this)

// View model info in logcat
versionManager.logModelInfo()
```

---

## What Actually Changed

### 1. File Modified: `OnnxModelRunner.kt`

**Change 1: File Size Detection**
```kotlin
// Lines 145-169
val assetSize = inputStream.available().toLong()
if (existingSize == assetSize) {
    Log.d(TAG, "File sizes match - using existing model")
    return outFile.absolutePath
} else {
    Log.d(TAG, "File size mismatch detected! New model deployed.")
    outFile.delete()  // ✓ Delete old cached model
}
```

**Change 2: New Method for Manual Refresh**
```kotlin
// Lines 224-246
fun forceRefreshModel() {
    session?.close()
    session = null
    isInitialized = false
    
    val cachedFile = File(context.filesDir, modelFileName)
    if (cachedFile.exists()) {
        cachedFile.delete()  // Force delete cache
    }
    
    initializeEnvironment()  // Reload from assets
}
```

### 2. File Modified: `CropRecommendation.kt`

**Enhanced logging** to show exactly which model is being used:

```kotlin
// Lines 108-180
Log.d("CropRecommendation", "=== Starting ONNX Inference ===")
Log.d("CropRecommendation", "Expected crop classes: ${CropConstants.CROP_NAMES}")
// ... detailed inference logging ...
Log.d("CropRecommendation", "[0] Banana: 0.95 (95%)")
Log.d("CropRecommendation", "=== Inference Complete ===")
```

### 3. New File: `ModelVersionManager.kt`

Optional helper class for version tracking and model info logging.

---

## Quick Test

After making these changes, here's how to verify it works:

### Step 1: Replace Your ONNX Model
1. Train new model in Python (should output to `crop_recommendation_model.onnx`)
2. Replace the file in `app/src/main/assets/crop_recommendation_model.onnx`
3. Rebuild and run the app

### Step 2: Check Logcat
```
// Look for these success indicators:
D OnnxModelRunner: Asset file size: 1024567 bytes
D OnnxModelRunner: Existing file size: 1024234 bytes
D OnnxModelRunner: File size mismatch detected! New model deployed.
D OnnxModelRunner: Deleted cached model: true
D OnnxModelRunner: Model copied successfully

// Then check inference:
D CropRecommendation: [0] Banana: 0.95 (95%)
D CropRecommendation: [1] Mango: 0.02 (2%)
```

### Step 3: Verify Results
- Top recommendation should be **Banana** (if your model was trained that way)
- Confidence should be high (>80%)

---

## Why This Works

| Scenario | Old Code | New Code |
|----------|----------|----------|
| First app launch | ✓ Copies model | ✓ Copies model |
| Second launch (same model) | ✓ Uses cache | ✓ Uses cache (same size) |
| Deploy new trained model | ❌ **Uses old cache** | ✓ **Detects size change, deletes cache, loads new model** |

---

## Common Logcat Messages Explained

```
✓ "File size mismatch detected! New model deployed."
  → App found different file size, will use new model

✓ "File sizes match - using existing model"
  → Sizes are identical, using cache (normal case)

✓ "Model copied successfully"
  → Model loaded into app cache

✓ "[0] Banana: 0.95"
  → First recommendation is Banana with 95% confidence

✗ "No confidences extracted from model output"
  → Model output format issue (check Python training)

✗ "Unexpected output type: Optional"
  → (Normal - gets unwrapped)
```

---

## Troubleshooting

### Issue: Still showing Mango after updating model

**Checklist:**
1. ✓ Did you actually retrain the model with Banana data?
2. ✓ Is the new model file size different from old?
3. ✓ Did you run `./gradlew clean` before rebuilding?
4. ✓ Check logcat: Is "File size mismatch detected" showing?
5. ✓ Verify Python script with test data:
   ```python
   result = run_inference(N=12.0, P=7.0, K=9.0, pH=6.5, Moisture=62.0, Temperature=29.4)
   print(result['predicted_crop'])  # Should be Banana
   ```

### Issue: Model size is exactly the same as old model

This is unlikely but possible if:
- Model was retrained with identical structure
- Weights happen to serialize to same size

**Solution:**
```kotlin
// Force refresh in MainActivity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    // Force model refresh on first launch
    val modelRunner = OnnxModelRunner.getInstance(this)
    try {
        modelRunner.forceRefreshModel()
    } catch (e: Exception) {
        Log.e(TAG, "Model refresh failed: ${e.message}")
    }
}
```

### Issue: Want to verify model is correct without building APK

Use the Python validation script:
```bash
python Crop_Recommendation_Validation.py
# Output should show Banana as top recommendation
```

---

## Summary

You've made these changes:

1. ✅ **OnnxModelRunner** now detects when new models are deployed
2. ✅ **Automatic cache invalidation** when file size changes
3. ✅ **Better logging** to diagnose which model is being used
4. ✅ **Optional version tracking** with ModelVersionManager

**Result:** When you deploy a newly trained model, your app will automatically use it instead of the cached version.

---

## Next Steps

1. **Train your new model** with Banana data
2. **Export to ONNX** → `crop_recommendation_model.onnx`
3. **Replace** in `app/src/main/assets/`
4. **Clean build:** `./gradlew clean; ./gradlew build`
5. **Deploy** and check logcat for "File size mismatch detected"
6. **Verify** it now recommends Banana instead of Mango

The automatic file size detection should handle the rest! 🎉

