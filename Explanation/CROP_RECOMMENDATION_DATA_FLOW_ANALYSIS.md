# Crop Recommendation Data Flow Analysis

## Overview
The crop recommendation feature in your app **IS actually performing real-time analysis** using the `crop_recommendation_model.onnx` file. However, there are some important points about how the system works and why you might perceive it as "hardcoded."

---

## The Complete Data Flow

### Step 1: Data Collection (GetSoilData.kt)
- User collects soil samples at different GPS locations
- Each sample contains: **Nitrogen, Phosphorus, Potassium, pH Level, Temperature, Moisture**
- All samples are stored in the `SoilDataViewModel` database

### Step 2: Data Aggregation (MappingInfo.kt)
**Location**: Lines 62-93 in `MappingInfo.kt`

The `calculateAverageSoilParameters()` function:
```kotlin
// Iterates through ALL stored locations
for (location in locations) {
    val soilData = viewModel.getSoilData(location)
    if (soilData != null) {
        totalNitrogen += soilData.nitrogen
        totalPhosphorus += soilData.phosphorus
        totalPotassium += soilData.potassium
        totalPhLevel += soilData.phLevel
        totalMoisture += soilData.moisture
        totalTemperature += soilData.temperature
        validCount++
    }
}

// Calculates averages
AverageSoilParameters(
    avgNitrogen = totalNitrogen / validCount,
    avgPhosphorus = totalPhosphorus / validCount,
    avgPotassium = totalPotassium / validCount,
    avgPhLevel = totalPhLevel / validCount,
    avgMoisture = totalMoisture / validCount,
    avgTemperature = totalTemperature / validCount
)
```

**What happens**: 
- When user clicks the **"Analyze"** button in MappingInfo
- The calculated averages are passed as navigation parameters to CropRecommendation
- Navigation route: `crop_recommendation/true/{avgN}/{avgP}/{avgK}/{avgPH}/{avgMoisture}/{avgTemp}`

### Step 3: Model Inference (CropRecommendation.kt)
**Location**: Lines 170-265 in `CropRecommendation.kt` (`runOnnxInference()` function)

#### When `skipStartScreen = true` and parameters are provided:

```kotlin
// These parameters come from MappingInfo averages
avgNitrogen: Float? = null,
avgPhosphorus: Float? = null,
avgPotassium: Float? = null,
avgPhLevel: Float? = null,
avgMoisture: Float? = null,
avgTemperature: Float? = null
```

The function executes these steps:

1. **Receives Real Data**:
   ```kotlin
   if (skipStartScreen && avgNitrogen != null && avgPhosphorus != null &&
       avgPotassium != null && avgPhLevel != null && avgMoisture != null &&
       avgTemperature != null) {
       
       // Creates synthetic SoilData with the REAL AVERAGES
       val syntheticSoilData = listOf(
           SoilData(
               nitrogen = avgNitrogen.toInt(),
               phosphorus = avgPhosphorus.toInt(),
               potassium = avgPotassium.toInt(),
               phLevel = avgPhLevel,
               temperature = avgTemperature,
               moisture = avgMoisture.toInt()
           )
       )
   ```

2. **Normalizes the Data** (Lines 195-200):
   ```kotlin
   val normalizedInputData = floatArrayOf(
       avgNitrogen / 100f,           // 0-1 range
       avgPhosphorus / 100f,         // 0-1 range
       avgPotassium / 100f,          // 0-1 range
       avgPhLevel / 14f,             // 0-1 range (pH 0-14)
       (avgTemperature + 40f) / 90f, // 0-1 range (-40 to 50°C)
       avgMoisture / 100f            // 0-1 range
   )
   ```

3. **Loads and Runs the ONNX Model** (Lines 202-250):
   ```kotlin
   val ortEnv = ai.onnxruntime.OrtEnvironment.getEnvironment()
   val sessionOptions = ai.onnxruntime.OrtSession.SessionOptions()
   
   // Load model from assets
   val modelAsset = context.assets.open("crop_recommendation_model.onnx")
   val modelBytes = modelAsset.readBytes()
   
   val session = ortEnv.createSession(modelBytes, sessionOptions)
   
   // Create input tensor with REAL normalized data
   val inputTensor = ai.onnxruntime.OnnxTensor.createTensor(ortEnv, normalizedInputData)
   
   // Run inference
   val results = session.run(mapOf(inputName to inputTensor))
   ```

4. **Extracts Model Output** (Lines 251-276):
   ```kotlin
   val confidences = results[outputName]?.let { output ->
       when (output) {
           is ai.onnxruntime.OnnxTensor -> {
               val floatBuffer = output.floatBuffer
               val scores = mutableListOf<Float>()
               while (floatBuffer.hasRemaining()) {
                   scores.add(floatBuffer.get())  // REAL model predictions
               }
               scores
           }
       }
   }
   ```

5. **Creates Prediction Objects** (Lines 281-295):
   ```kotlin
   confidences.mapIndexed { index, confidence ->
       CropPrediction(
           cropName = CropConstants.CROP_NAMES[index],
           confidence = confidence.coerceIn(0f, 1f),  // MODEL OUTPUT
           percentage = (confidence * 100).roundToInt(),
           color = CropConstants.getCropColor(cropName),
           icon = CropConstants.getCropIcon(cropName),
           reasoning = CropConstants.getReasoningForConfidence(cropName, confidence)
       )
   }
   ```

---

## Why You Might Think It's Hardcoded

### The `getDefaultRecommendations()` Function
This function exists as a **fallback mechanism** (Lines 306-341) when:
- The model fails to load
- The ONNX runtime encounters an error
- No valid data is available
- An exception occurs during inference

These default values are **NOT the actual results** - they're placeholder data to prevent app crashes.

---

## The Complete Real-Time Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. USER COLLECTS SAMPLES IN GETSOILDATA                         │
│    ✓ Sample 1: N=80, P=40, K=200, pH=6.5, Temp=28, Moist=60   │
│    ✓ Sample 2: N=75, P=35, K=180, pH=6.8, Temp=29, Moist=65   │
│    ✓ Sample 3: N=90, P=50, K=220, pH=6.6, Temp=27, Moist=62   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. USER NAVIGATES TO MAPPINGINFO                                │
│    ✓ Displays all 3 collected samples                           │
│    ✓ calculateAverageSoilParameters() computes:                 │
│      - avgNitrogen = (80+75+90)/3 = 81.67                       │
│      - avgPhosphorus = (40+35+50)/3 = 41.67                     │
│      - avgPotassium = (200+180+220)/3 = 200                     │
│      - avgPhLevel = (6.5+6.8+6.6)/3 = 6.63                      │
│      - avgTemperature = (28+29+27)/3 = 28                       │
│      - avgMoisture = (60+65+62)/3 = 62.33                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. USER CLICKS "ANALYZE" BUTTON                                 │
│    ✓ Passes real averages via navigation:                       │
│      crop_recommendation/true/81.67/41.67/200/6.63/62.33/28     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. CROPRECOMMENDATION RECEIVES PARAMETERS                       │
│    ✓ avgNitrogen = 81.67 (REAL)                                 │
│    ✓ avgPhosphorus = 41.67 (REAL)                               │
│    ✓ avgPotassium = 200 (REAL)                                  │
│    ✓ avgPhLevel = 6.63 (REAL)                                   │
│    ✓ avgTemperature = 28 (REAL)                                 │
│    ✓ avgMoisture = 62.33 (REAL)                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. NORMALIZATION (Maps to 0-1 range)                            │
│    ✓ Nitrogen: 81.67/100 = 0.8167                               │
│    ✓ Phosphorus: 41.67/100 = 0.4167                             │
│    ✓ Potassium: 200/100 = 1.0                                   │
│    ✓ pH: 6.63/14 = 0.473                                        │
│    ✓ Temperature: (28+40)/90 = 0.756                            │
│    ✓ Moisture: 62.33/100 = 0.6233                               │
│    → Input Array: [0.8167, 0.4167, 1.0, 0.473, 0.756, 0.6233]  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. ONNX MODEL INFERENCE                                          │
│    ✓ Loads: crop_recommendation_model.onnx (FROM ASSETS)         │
│    ✓ Input Tensor Created with REAL normalized data             │
│    ✓ Model runs inference on THIS SPECIFIC INPUT                │
│    ✓ Model outputs predictions for each crop                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. MODEL OUTPUT (Example - varies based on input data)          │
│    ✓ Banana: 0.87 (87%)     ← Model's prediction               │
│    ✓ Cassava: 0.72 (72%)    ← Model's prediction               │
│    ✓ Sweet Potato: 0.68 (68%) ← Model's prediction             │
│    ✓ Corn: 0.60 (60%)       ← Model's prediction               │
│    ✓ Mango: 0.55 (55%)      ← Model's prediction               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8. DISPLAY RESULTS                                               │
│    ✓ Sorted by confidence (highest first)                       │
│    ✓ Shows: Name, Icon, Confidence %, Reasoning, Progress Bar   │
│    ✓ ALL VALUES DERIVED FROM MODEL OUTPUT                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Evidence That This Is Real-Time Analysis

### 1. **Dynamic Data Source**
- Data comes from `soilDataViewModel.getAllStoredLocations()` and `getSoilData(location)`
- Every new sample collected changes the averages
- Different average values produce different model predictions

### 2. **Mathematical Averaging**
```kotlin
avgNitrogen = totalNitrogen / validCount  // Not hardcoded, calculated
avgPhosphorus = totalPhosphorus / validCount  // Not hardcoded, calculated
// ... etc for all 6 parameters
```

### 3. **Model Loading from Assets**
```kotlin
val modelAsset = context.assets.open("crop_recommendation_model.onnx")
val modelBytes = modelAsset.readBytes()
val session = ortEnv.createSession(modelBytes, sessionOptions)
```
The actual ML model is loaded and executed, not returning predetermined values.

### 4. **Real-Time Input Processing**
```kotlin
val normalizedInputData = floatArrayOf(
    avgNitrogen / 100f,           // DYNAMIC - changes per sample set
    avgPhosphorus / 100f,         // DYNAMIC - changes per sample set
    // ... other dynamic parameters
)
val inputTensor = ai.onnxruntime.OnnxTensor.createTensor(ortEnv, normalizedInputData)
val results = session.run(mapOf(inputName to inputTensor))  // Inference on THIS input
```

### 5. **Logging Shows Real Values**
```kotlin
Log.d("CropRecommendation", 
    "Average Soil Data - N: $avgNitrogen, P: $avgPhosphorus, K: $avgPotassium, " +
    "pH: $avgPhLevel, Temp: $avgTemperature, Moisture: $avgMoisture")

Log.d("CropRecommendation", "Raw confidences: $confidences")
```
These logs will show different values based on the actual collected data.

---

## Why Use Fallback Defaults?

The `getDefaultRecommendations()` exists for **production stability**:

| Scenario | What Happens |
|----------|-------------|
| Model loads successfully | Real predictions from ONNX model |
| Model fails to load | Falls back to default recommendations |
| No data collected | Error dialog + fallback to start screen |
| ONNX runtime error | Falls back to default recommendations |
| Invalid normalization | Falls back to default recommendations |

This is a **defensive programming practice** to ensure the app doesn't crash even if the ML pipeline fails.

---

## Testing to Verify Real-Time Analysis

To confirm this is real-time:

1. **Collect Soil Sample #1**: High Nitrogen (90), Low Phosphorus (20)
2. Navigate to MappingInfo → Click Analyze
3. **Note the predictions**

4. **Collect Soil Sample #2**: Low Nitrogen (30), High Phosphorus (80)
5. Navigate to MappingInfo → Click Analyze
6. **The predictions should be different** because averages changed

7. **Check Logcat output** for:
   - "Average Soil Data - N: X, P: Y, K: Z, pH: A, Temp: B, Moisture: C"
   - "Raw confidences: [0.XX, 0.XX, 0.XX, 0.XX, 0.XX]"

If the values in logs change based on your collected samples, it's **100% real-time analysis**.

---

## Summary

| Question | Answer |
|----------|--------|
| Is the result hardcoded? | **NO** - Results come from ONNX model output based on input data |
| Are average parameters analyzed? | **YES** - Calculated from all samples, normalized, and fed to model |
| Is crop_recommendation_model.onnx used? | **YES** - Loaded from assets and executes inference |
| Is it real-time? | **YES** - Changes based on collected soil samples |
| What are the default values for? | **Fallback only** - Error handling if model fails |

The system correctly implements an **end-to-end ML pipeline**: Data Collection → Averaging → Normalization → Model Inference → Results Display.


