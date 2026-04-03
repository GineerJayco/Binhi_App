# Crop Recommendation Feature - Code Examples

## Complete Implementation Example

### 1. ViewModel Implementation (SoilDataViewModel.kt)

```kotlin
package com.example.binhi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.example.binhi.data.SoilData

class SoilDataViewModel : ViewModel() {
    
    private val soilDataStorage = mutableMapOf<LatLng, SoilData>()
    
    /**
     * Track the total number of dots that need data
     */
    var totalDotsCount by mutableStateOf(0)
        private set
    
    /**
     * Derived state - automatically re-evaluates when dependencies change
     * True only when: totalDotsCount > 0 AND all dots have saved data
     */
    val allDotsComplete by derivedStateOf {
        totalDotsCount > 0 && soilDataStorage.size == totalDotsCount
    }
    
    /**
     * Called whenever dot grid is recalculated
     */
    fun setTotalDotsCount(count: Int) {
        totalDotsCount = count
    }
    
    /**
     * Save data for a location
     */
    fun saveSoilData(location: LatLng, data: SoilData): Boolean {
        return if (data.isValid()) {
            soilDataStorage[location] = data
            true
        } else {
            false
        }
    }
    
    /**
     * Get progress as percentage (0-100)
     */
    fun getCompletionPercentage(): Int {
        return if (totalDotsCount > 0) {
            (soilDataStorage.size * 100) / totalDotsCount
        } else {
            0
        }
    }
    
    // ... other methods ...
}
```

### 2. UI Implementation (GetSoilData.kt)

#### Part A: Update Total Dots Count

```kotlin
@Composable
fun GetSoilData(
    navController: NavController,
    landArea: String?,
    length: String?,
    width: String?,
    crop: String?,
    soilDataViewModel: SoilDataViewModel = viewModel()
) {
    // ... existing state variables ...
    
    // Calculate dots based on area
    val dots = remember(polygonCenter, rotation, length, width, dotSpacing) {
        val lengthInMeters = length?.toDoubleOrNull() ?: 0.0
        val widthInMeters = width?.toDoubleOrNull() ?: 0.0
        if (lengthInMeters <= 0 || widthInMeters <= 0) return@remember emptyList()

        val numDotsLength = max((lengthInMeters / dotSpacing).toInt(), 2)
        val numDotsWidth = max((widthInMeters / dotSpacing).toInt(), 2)

        val dots = mutableListOf<LatLng>()

        for (i in 0 until numDotsLength) {
            for (j in 0 until numDotsWidth) {
                val x = if (numDotsWidth > 1) {
                    (j.toDouble() / (numDotsWidth - 1)) * widthInMeters - (widthInMeters / 2)
                } else {
                    0.0
                }

                val y = if (numDotsLength > 1) {
                    (i.toDouble() / (numDotsLength - 1)) * lengthInMeters - (lengthInMeters / 2)
                } else {
                    0.0
                }

                val angleRad = Math.toRadians(rotation.toDouble())
                val rotatedX = x * cos(angleRad) - y * sin(angleRad)
                val rotatedY = x * sin(angleRad) + y * cos(angleRad)

                val centerLatRad = Math.toRadians(polygonCenter.latitude)
                val latOffset = rotatedY / 111132.0
                val lonOffset = rotatedX / (111320.0 * cos(centerLatRad))

                dots.add(LatLng(
                    polygonCenter.latitude + latOffset,
                    polygonCenter.longitude + lonOffset
                ))
            }
        }
        dots
    }

    // ✅ KEY: Update total dots count whenever dots list changes
    LaunchedEffect(dots.size) {
        soilDataViewModel.setTotalDotsCount(dots.size)
    }
    
    // ... rest of composable ...
}
```

#### Part B: Add Crop Recommendation Button

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            // ... drag handling ...
        }
) {
    // ... GoogleMap and other controls ...
    
    // ✅ KEY: Conditional button rendering
    // Shows only when all dots have saved data
    if (soilDataViewModel.allDotsComplete) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    // TODO: Replace with actual recommendation logic
                    // Examples:
                    // 1. navController.navigate("crop_recommendations")
                    // 2. showRecommendationDialog = true
                    // 3. coroutineScope.launch { 
                    //      val recs = api.getRecommendations(
                    //          soilDataViewModel.getAllStoredLocations()
                    //      )
                    //    }
                    
                    Log.d(
                        "GetSoilData",
                        "Get Crop Recommendation clicked - " +
                        "All ${soilDataViewModel.totalDotsCount} dots have data"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)  // Material Blue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = "Get Recommendation",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Get Crop Recommendation",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Confirmation text showing progress
            Text(
                text = "All ${soilDataViewModel.totalDotsCount} sampling points have been collected",
                fontSize = 12.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
```

## Usage Examples

### Example 1: Monitor Progress in Real-time

```kotlin
@Composable
fun ProgressIndicator(soilDataViewModel: SoilDataViewModel) {
    Column {
        // Show progress bar
        val progress = soilDataViewModel.getCompletionPercentage()
        LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Show progress text
        Text(
            "${soilDataViewModel.getStoredDataCount()} / " +
            "${soilDataViewModel.totalDotsCount} dots complete ($progress%)"
        )
    }
}
```

### Example 2: Navigate to Recommendations Screen

```kotlin
Button(
    onClick = {
        // Get all saved locations with data
        val locations = soilDataViewModel.getAllStoredLocations()
        val soilDataMap = locations.associateWith { location ->
            soilDataViewModel.getSoilData(location)
        }
        
        // Navigate to recommendation screen with data
        navController.navigate(
            "crop_recommendations?locations=${locations.size}"
        )
    },
    // ... button styling ...
)
```

### Example 3: Call Recommendation API

```kotlin
Button(
    onClick = {
        coroutineScope.launch {
            try {
                // Gather all soil data
                val locations = soilDataViewModel.getAllStoredLocations()
                val soilDataList = locations.mapNotNull { location ->
                    soilDataViewModel.getSoilData(location)?.copy(location = location.toString())
                }
                
                // Call API for recommendations
                val recommendations = recommendationService.getCropRecommendations(
                    soilDataList = soilDataList,
                    cropType = crop
                )
                
                // Display recommendations
                showRecommendations(recommendations)
            } catch (e: Exception) {
                Log.e("GetCropRec", "Error getting recommendations", e)
                showErrorDialog(e.message ?: "Unknown error")
            }
        }
    },
    // ... button styling ...
)
```

### Example 4: Show Completion Dialog

```kotlin
if (soilDataViewModel.allDotsComplete) {
    Dialog(onDismissRequest = { showDialog = false }) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Complete",
                    modifier = Modifier.size(48.dp),
                    tint = Color.Green
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "All Sampling Complete!",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All ${soilDataViewModel.totalDotsCount} sampling points " +
                    "have soil data collected. Ready for analysis.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        showDialog = false
                        // Proceed to recommendations
                    }
                ) {
                    Text("Get Recommendations")
                }
            }
        }
    }
}
```

## Testing Code Examples

### Unit Test: Completion Logic

```kotlin
@Test
fun testAllDotsComplete_NoDotsSet() {
    val viewModel = SoilDataViewModel()
    
    assertFalse(viewModel.allDotsComplete)
}

@Test
fun testAllDotsComplete_PartialData() {
    val viewModel = SoilDataViewModel()
    viewModel.setTotalDotsCount(10)
    
    val location1 = LatLng(0.0, 0.0)
    val data1 = SoilData(nitrogen = 50, phosphorus = 30, potassium = 20, 
                        phLevel = 7.0f, temperature = 25.0f, moisture = 50)
    
    viewModel.saveSoilData(location1, data1)
    
    assertFalse(viewModel.allDotsComplete)
    assertEquals(10, viewModel.getCompletionPercentage())
}

@Test
fun testAllDotsComplete_AllDataSaved() {
    val viewModel = SoilDataViewModel()
    
    val locations = (1..5).map { LatLng(it.toDouble(), it.toDouble()) }
    viewModel.setTotalDotsCount(locations.size)
    
    locations.forEach { location ->
        val data = SoilData(50, 30, 20, 7.0f, 25.0f, 50)
        viewModel.saveSoilData(location, data)
    }
    
    assertTrue(viewModel.allDotsComplete)
    assertEquals(100, viewModel.getCompletionPercentage())
}

@Test
fun testAllDotsComplete_DataDeletedAfterCompletion() {
    val viewModel = SoilDataViewModel()
    val location = LatLng(0.0, 0.0)
    
    viewModel.setTotalDotsCount(1)
    val data = SoilData(50, 30, 20, 7.0f, 25.0f, 50)
    viewModel.saveSoilData(location, data)
    
    assertTrue(viewModel.allDotsComplete)
    
    // Delete and verify completion reverts
    viewModel.deleteSoilData(location)
    assertFalse(viewModel.allDotsComplete)
}
```

## Performance Considerations

### Why `derivedStateOf`?

```kotlin
// ❌ WRONG: Causes unnecessary recompositions
var allComplete = remember {
    mutableStateOf(totalDotsCount > 0 && soilDataStorage.size == totalDotsCount)
}

// ✅ CORRECT: Only recomposes when dependencies actually change
val allDotsComplete by derivedStateOf {
    totalDotsCount > 0 && soilDataStorage.size == totalDotsCount
}
```

### Why `LaunchedEffect` for dots count?

```kotlin
// ✅ CORRECT: Only updates when dots.size changes
LaunchedEffect(dots.size) {
    soilDataViewModel.setTotalDotsCount(dots.size)
}

// ❌ WRONG: Updates on every recomposition
soilDataViewModel.setTotalDotsCount(dots.size)
```

---

## Key Takeaways

1. **Separation of Concerns**: Business logic in ViewModel, UI in Composables
2. **Reactive State**: `derivedStateOf` automatically updates UI when dependencies change
3. **No Memory Leaks**: `LaunchedEffect` with proper dependencies ensures cleanup
4. **Type Safety**: Kotlin prevents null pointer and type errors
5. **Testability**: Pure logic in ViewModel is easy to unit test

