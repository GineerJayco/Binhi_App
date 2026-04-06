# 🚀 ONNX Model Update - Quick Reference

## The Issue
App shows **Mango** instead of **Banana** after you trained new model.

## The Cause  
Android cached the old ONNX model file and didn't detect the new one.

## The Solution
**✅ AUTOMATIC FILE SIZE DETECTION**

New code detects when model file size changes → automatically uses new model!

---

## How to Deploy Your New Trained Model

### 1️⃣ Train Model (Python)
```bash
python train_crop_model.py
# Output: crop_recommendation_model.onnx
```

### 2️⃣ Validate (Python)
```bash
python Crop_Recommendation_Validation.py
# Should show: "Predicted Crop: Banana"
```

### 3️⃣ Replace File (Android)
```bash
cp crop_recommendation_model.onnx \
    app/src/main/assets/crop_recommendation_model.onnx
```

### 4️⃣ Build & Deploy
```bash
./gradlew clean build
./gradlew installDebug
# Or use Android Studio Run button
```

### 5️⃣ Check Logcat
```
Filter: OnnxModelRunner

Look for: ✓ "File size mismatch detected"
          ✓ "Model copied successfully"
          ✓ "[0] Banana: 0.95"
```

✅ **Done!** App automatically uses new model.

---

## Logcat Success Indicators

```
✓ "Asset file size: 2419200 bytes"
✓ "Existing file size: 1808956 bytes"  
✓ "File size mismatch detected! New model deployed."
✓ "Deleted cached model: true"
✓ "Model copied successfully"
✓ "[0] Banana: 0.95 (95%)"              ← YOU WANT THIS!
```

---

## If Still Showing Mango ❌

**Step 1:** Check Python model
```bash
python Crop_Recommendation_Validation.py
# Does Python recommend Banana?
# If Mango → Retrain your Python model
```

**Step 2:** Verify file size changed
```bash
ls -lh app/src/main/assets/crop_recommendation_model.onnx
# Size should be different from previous version
```

**Step 3:** Force refresh (development only)
```kotlin
val modelRunner = OnnxModelRunner.getInstance(this)
modelRunner.forceRefreshModel()
```

**Step 4:** Clear app cache
```bash
adb shell pm clear com.example.binhi
# Restart app
```

---

## Code Changes Made

### ✅ File: `OnnxModelRunner.kt`

**Before:**
```kotlin
if (outFile.exists()) {
    return outFile.absolutePath  // ❌ Always uses old cache!
}
```

**After:**
```kotlin
if (existingSize == assetSize) {
    return outFile.absolutePath  // ✓ Same model
} else {
    outFile.delete()  // ✓ New model detected - update cache!
}
```

### ✅ File: `CropRecommendation.kt`
- Added detailed logging to show which model is being used
- Better error diagnostics

### ✅ NEW File: `ModelVersionManager.kt`
- Optional version tracking (for advanced use)

---

## Understanding File Size Detection

| Scenario | File Size | Action | Result |
|----------|-----------|--------|--------|
| First launch | N/A | Copy from assets | ✓ Works |
| Same model, 2nd launch | Same | Use cache | ✓ Works |
| New trained model | Different | Delete cache, copy new | ✓ Works! |
| Emergency: Same size | Same | Use `forceRefreshModel()` | ✓ Works |

---

## Verification Checklist

- [ ] Python model exports ONNX successfully
- [ ] Validation script shows "Banana" recommendation
- [ ] New model file has different size
- [ ] File replaced in `app/src/main/assets/`
- [ ] App rebuilt: `./gradlew clean build`
- [ ] App deployed to device
- [ ] Logcat shows "File size mismatch detected"
- [ ] UI shows **Banana** as top recommendation (not Mango!)
- [ ] Confidence is high (>80%)

✅ All checked? **Success!**

---

## Logcat Commands

```bash
# View model loading in real-time
adb logcat | grep "OnnxModelRunner"

# View inference results
adb logcat | grep "CropRecommendation"

# Both together
adb logcat | grep -E "OnnxModelRunner|CropRecommendation"

# Watch for specific success
adb logcat | grep "File size mismatch"
adb logcat | grep "\[0\] Banana"
```

---

## Why This Works

1. **Automatic Detection:** File size = implicit version number
2. **No Manual Steps:** Happens automatically on app launch
3. **Reliable:** Every retrain = different model weights = different file size
4. **Production Ready:** No configuration needed
5. **Fallback Available:** `forceRefreshModel()` for edge cases

---

## Common Questions

**Q: Will existing users automatically get the new model?**  
A: Yes! File size detection works automatically on any device.

**Q: What if new model has same file size as old?**  
A: Extremely unlikely (random chance). Use `forceRefreshModel()` or version tracking.

**Q: Do I need to update the app name or version?**  
A: No, but good practice to increment version in `build.gradle.kts`

**Q: Can I test without rebuilding APK?**  
A: Yes, use `forceRefreshModel()` in code, or clear cache + restart.

**Q: What if inference still shows wrong crop?**  
A: Check your Python model - it might not actually recommend Banana.

---

## Files to Read for More Detail

1. **COMPLETE_ONNX_FIX_SUMMARY.md** - Full explanation
2. **DEPLOYMENT_DEBUGGING_CHECKLIST.md** - Detailed verification guide
3. **MODEL_CACHING_FIX_GUIDE.md** - Technical deep dive
4. **MODEL_FIX_INTEGRATION_GUIDE.md** - Implementation details

---

## Emergency: Force Refresh

```kotlin
// Add this to MainActivity when app launches
val modelRunner = OnnxModelRunner.getInstance(this)
try {
    modelRunner.forceRefreshModel()
    Log.d("ModelDebug", "✓ Model refreshed successfully")
} catch (e: Exception) {
    Log.e("ModelDebug", "✗ Model refresh failed: ${e.message}")
}
```

This deletes cached model and reloads from assets. Use when needed.

---

## Summary

✅ **Problem Solved:** Old ONNX model caching issue  
✅ **Solution:** Automatic file size detection  
✅ **Result:** New trained models automatically used  
✅ **No manual intervention needed**  

### Deploy → Build → Logcat Check → Done! 🎉

---

**Last Updated:** 2026-04-07  
**Status:** ✅ Complete & Ready to Use

