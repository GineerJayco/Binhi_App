# ONNX Model Caching Fix - Complete Guide

## Problem Summary

Your app was still recommending **Mango** instead of **Banana** even after you replaced the ONNX model file. This is a classic Android caching issue.

### Root Cause
The `OnnxModelRunner.kt` was checking if the model file exists and **skipping the copy** if it did:

```kotlin
// OLD CODE (PROBLEMATIC)
if (outFile.exists()) {
    Log.d(TAG, "Model file already exists: ${outFile.absolutePath}")
    return outFile.absolutePath  // ❌ Uses old cached model!
}
```

When you rebuilt your app with a new trained ONNX model:
1. New model is added to `assets/crop_recommendation_model.onnx`
2. App copies it to internal storage
3. But the old model file still exists from previous installation
4. App detects existing file and **uses the OLD model** ✗

---

## Solution Implemented

### 1. **File Size Detection**
The updated code now compares file sizes:

```kotlin
val assetSize = inputStream.available().toLong()
val existingSize = outFile.length()

if (existingSize == assetSize) {
    Log.d(TAG, "File sizes match - using existing model")
    return outFile.absolutePath
} else {
    Log.d(TAG, "File size mismatch detected! New model deployed.")
    outFile.delete()  // ✓ Delete old model
}
```

**How it works:**
- When you train a new model, its file size is different from the old one
- App detects size mismatch → Automatically deletes old model
- Fresh copy of new model is loaded ✓

### 2. **Force Refresh Method**
For development/testing, you can now force refresh:

```kotlin
val modelRunner = OnnxModelRunner.getInstance(context)
modelRunner.forceRefreshModel()  // Deletes cache and reloads
```

### 3. **Comprehensive Logging**
Added detailed debug logs to trace which model is being used:

```
D CropRecommendation: Asset file size: 1024567 bytes
D CropRecommendation: Existing file size: 1024234 bytes
D CropRecommendation: File size mismatch detected! New model deployed.
D CropRecommendation: === Starting ONNX Inference ===
D CropRecommendation: Input shape [1, 6]: [12.0, 7.0, 9.0, 6.5, 29.4, 62.0]
D CropRecommendation: [0] Banana: 0.95 (95%)
D CropRecommendation: [1] Cassava: 0.02 (2%)
```

---

## Verification Steps

### Step 1: Check Logcat During First Run After Model Update

1. **Build and deploy your app** with the new trained model
2. **Open Logcat** (Android Studio → View → Tool Windows → Logcat)
3. **Look for these logs:**

```
OnnxModelRunner: Asset file size: [SIZE]
OnnxModelRunner: File size mismatch detected! New model deployed.
OnnxModelRunner: Deleted cached model: true
OnnxModelRunner: Model copied successfully
OnnxModelRunner: Model refresh completed successfully
```

**What to look for:**
- ✓ "File size mismatch detected" → New model will be used
- ✓ "Deleted cached model: true" → Old model was removed
- ✗ "File sizes match" → Model wasn't updated (check your build)

### Step 2: Verify Model Output

In Logcat, search for "=== Inference Results ===" and check:

```
D CropRecommendation: [0] Banana: 0.95 (95%)
D CropRecommendation: [1] Mango: 0.02 (2%)
```

Should show **Banana first** if your new model was trained correctly.

### Step 3: Manual Testing

Use the Crop_Recommendation_Validation.py with test data:

```python
# Should recommend Banana (not Mango)
result = run_inference(
    N=12.0,
    P=7.0,
    K=9.0,
    pH=6.5,
    Moisture=62.0,
    Temperature=29.4
)
print(result['predicted_crop'])  # Should print: Banana
```

---

## Common Issues & Solutions

### Issue 1: Still showing old results after rebuild

**Solution:**
```kotlin
// In MainActivity or wherever you initialize the model
val modelRunner = OnnxModelRunner.getInstance(this)
modelRunner.forceRefreshModel()  // Force reload model
```

Or **manually clear app data:**
- Settings → Apps → Your App → Storage → Clear Cache
- Settings → Apps → Your App → Storage → Clear Storage

### Issue 2: Model file sizes are identical (new and old)

This can happen if:
- The new model has the same size as the old one
- The model wasn't actually retrained

**Solution - Use version checking:**

Create a `model_metadata.json` in assets:
```json
{
  "version": "2.0",
  "trained_date": "2026-04-07",
  "crops": ["Banana", "Cassava", "Corn", "Mango", "Sweet Potato"]
}
```

Then check version before loading.

### Issue 3: Wrong crop order in model

Ensure your model training uses the same crop order:

**In Python (training):**
```python
from sklearn.preprocessing import LabelEncoder

CROP_NAMES = ["Banana", "Cassava", "Corn", "Mango", "Sweet Potato"]
le = LabelEncoder()
le.fit(CROP_NAMES)  # IMPORTANT: Must be alphabetical order

# Verify:
print(le.classes_)  # Should print: ['Banana' 'Cassava' 'Corn' 'Mango' 'Sweet Potato']
```

**In Kotlin (matching):**
```kotlin
object CropConstants {
    val CROP_NAMES = listOf(
        "Banana",           // Index 0
        "Cassava",          // Index 1
        "Corn",             // Index 2
        "Mango",            // Index 3
        "Sweet Potato"      // Index 4
    )
}
```

**They MUST match!** If your Python model uses different order, update Kotlin.

---

## Testing Checklist

- [ ] New model file has different size from old model
- [ ] Logcat shows "File size mismatch detected"
- [ ] Logcat shows "Deleted cached model: true"
- [ ] App shows "Banana" as top recommendation for test data
- [ ] Confidence is above 90% (not below 60%)
- [ ] `Crop_Recommendation_Validation.py` also shows Banana

---

## Key Files Modified

1. **OnnxModelRunner.kt**
   - `copyAssetToInternalStorage()` - Now detects file size changes
   - `forceRefreshModel()` - New method to force model reload

2. **CropRecommendation.kt**
   - `runOnnxInference()` - Enhanced logging for debugging

---

## Production Deployment Tips

### For App Updates:

1. **Always ensure new model has different file size**
   - If model size is identical, users won't update
   - Add metadata or compress differently if needed

2. **Test the model locally first:**
   ```bash
   python Crop_Recommendation_Validation.py
   # Verify it recommends the correct crop
   ```

3. **Before building APK:**
   - Check model file size: `ls -lh assets/crop_recommendation_model.onnx`
   - Ensure it's different from previous version
   - Update version number in app/build.gradle.kts

4. **After deployment, monitor logcat for:**
   - File size detection logs
   - Model loading success/failure
   - Inference results

---

## Advanced: Version-Based Model Updates

For production apps with many users, consider version-based updates:

```kotlin
// Enhanced initialization with version checking
fun initializeEnvironmentWithVersion() {
    val savedVersion = loadSavedModelVersion()  // Shared preferences
    val assetVersion = loadAssetModelVersion()  // From metadata.json
    
    if (savedVersion != assetVersion) {
        Log.d(TAG, "Model version changed: $savedVersion → $assetVersion")
        forceRefreshModel()
        saveSavedModelVersion(assetVersion)
    }
}
```

This ensures users get the latest model even if file size happens to match.

---

## Debug Logs Reference

| Log | Meaning |
|-----|---------|
| `Asset file size: XXX` | Size of model in assets |
| `Existing file size: XXX` | Size of cached model |
| `File size mismatch detected!` | New model will be used ✓ |
| `File sizes match` | Using old cached model |
| `Model copied successfully` | Model loaded successfully ✓ |
| `=== Starting ONNX Inference ===` | Inference starting |
| `[0] Banana: 0.95` | First recommendation |
| `Confidence sum: 0.98` | Model output is normalized |

---

## Next Steps

1. **Build and deploy** with your new trained model
2. **Check logcat** for file size mismatch detection
3. **Verify Banana** appears as top recommendation
4. **Test with Crop_Recommendation_Validation.py** in Python

If you still see Mango instead of Banana, check:
- [ ] Did you actually retrain the model?
- [ ] Does your test data match?
- [ ] Are crop names in same order in Python and Kotlin?

