# Soil Data Storage - Quick Integration Guide

## What Was Implemented

A complete soil data storage system for the Binhi App that:
1. Receives soil data via Bluetooth Classic from ESP32
2. Parses complete format: `NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62`
3. Stores data per map dot (LatLng) using a ViewModel
4. Persists data across screen recompositions
5. Shows GREEN markers for saved dots, BLUE for unsaved
6. Automatically loads and displays saved data when clicking markers

## Files Created

```
app/src/main/java/com/example/binhi/
├── data/
│   └── SoilData.kt                    (NEW - Data class for stored soil measurements)
├── viewmodel/
│   └── SoilDataViewModel.kt           (UPDATED - Storage management for soil data)
├── bluetooth/
│   └── SoilSensorData.kt              (UPDATED - Enhanced parsing for all fields)
└── GetSoilData.kt                     (UPDATED - UI integration with storage)
```

## Key Features

### 1. SoilData Model
```kotlin
data class SoilData(
    val nitrogen: Int,
    val phosphorus: Int,
    val potassium: Int,
    val phLevel: Float,
    val temperature: Float,
    val moisture: Int,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 2. ViewModel Methods
```kotlin
fun saveSoilData(location: LatLng, data: SoilData): Boolean
fun getSoilData(location: LatLng): SoilData?
fun hasSoilData(location: LatLng): Boolean
fun getAllStoredLocations(): Set<LatLng>
fun deleteSoilData(location: LatLng)
fun clearAllData()
fun getStoredDataCount(): Int
```

### 3. Enhanced Bluetooth Parsing
- Before: `"NPK=12,7,9"` (3 fields)
- After: `"NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"` (6 fields)
- Flexible regex-based parsing with fallback defaults

### 4. UI Dialog Flow

**When user clicks a dot:**
1. Dialog shows coordinates
2. If data exists → shows stored values (green header)
3. If no data → shows "No Data Stored" message
4. "Receive Data" button sends Bluetooth request

**When Bluetooth data arrives:**
1. Response dialog shows all 6 soil parameters
2. Each field displayed in formatted DataRow
3. "Save Data" button appears (green, only if no error)
4. After save → success message appears for 2 seconds
5. Marker changes from BLUE to GREEN

**When user clicks GREEN marker:**
1. Stored data loads automatically
2. Dialog shows the saved soil values
3. User can receive new data to update

## Usage Example

```kotlin
// In GetSoilData composable
val soilDataViewModel: SoilDataViewModel = viewModel()

// When user saves data
val soilData = bluetoothResponse!!.toSoilData()
if (soilDataViewModel.saveSoilData(selectedDot!!, soilData)) {
    // Show success message
    showSaveSuccessMessage = true
}

// When user clicks a dot
LaunchedEffect(selectedDot) {
    if (selectedDot != null && soilDataViewModel.hasSoilData(selectedDot!!)) {
        currentSoilData = soilDataViewModel.getSoilData(selectedDot!!)
    }
}

// Check marker color
val markerColor = if (soilDataViewModel.hasSoilData(dot)) {
    BitmapDescriptorFactory.HUE_GREEN  // Saved
} else {
    BitmapDescriptorFactory.HUE_BLUE   // Unsaved
}
```

## Data Validation

Before saving, `SoilData.isValid()` checks:
- nitrogen: 0 to unlimited
- phosphorus: 0 to unlimited
- potassium: 0 to unlimited
- phLevel: 0 < pH ≤ 14
- temperature: -40 to 80°C
- moisture: 0 to 100%

Invalid data will NOT be saved and user sees no success message.

## Data Persistence Scope

✅ **Persists across:**
- Dialog close/open cycles
- Screen recompositions
- Navigation away and back

❌ **Does NOT persist across:**
- App restart (in-memory storage only)
- Device rotation (unless handled separately)
- Process kill

**To add persistent storage:**
1. Add Room database dependency
2. Create `SoilDataEntity` and `SoilDataDao`
3. Replace `mutableMapOf` with Room queries
4. See `SOIL_DATA_IMPLEMENTATION.md` for details

## Testing the Implementation

### Manual Test Steps

1. **Basic Flow**
   - Open app → Navigate to soil data screen
   - Click any blue dot
   - Click "Receive Data"
   - Wait for Bluetooth response
   - Verify all 6 fields display
   - Click "Save Data"
   - Verify green success message
   - Verify marker turns green

2. **Data Reload**
   - Click the green marker again
   - Verify stored data appears immediately
   - Verify no "Receive Data" needed

3. **Multiple Locations**
   - Save data to 3 different dots
   - Click each one
   - Verify correct data for each location
   - Verify 3 green, rest blue

4. **Error Handling**
   - Disable Bluetooth
   - Try to receive data
   - Verify error message displays

## Performance Notes

- **Memory**: O(n) where n = number of dots with data
- **Lookup time**: O(1) - HashMap based
- **Recomposition**: Minimal - only on state changes
- **Bluetooth**: Non-blocking coroutine
- **No database queries**: Instant responses

## Architecture Diagram

```
┌─────────────────────────────────────┐
│      GetSoilData Composable         │
│  (UI Layer - Dialogs & Map)         │
└────────────────┬────────────────────┘
                 │
                 ↓
        ┌────────────────────┐
        │ SoilDataViewModel  │
        │  (State Layer)     │
        │ Map<LatLng, Data>  │
        └────────────────────┘
                 ↑
                 │
    ┌────────────┼────────────┐
    ↓            ↓            ↓
┌─────────┐  ┌─────────┐  ┌──────────┐
│SoilData │  │SoilSenor│  │Bluetooth │
│(Model)  │  │Data(Trnx)│  │Manager   │
└─────────┘  └─────────┘  └──────────┘
```

## Troubleshooting

### Green marker not showing
- Check: `soilDataViewModel.hasSoilData(dot)` in marker rendering
- Check: `saveSoilData()` returned true
- Check: `data.isValid()` passed validation

### Stored data not loading
- Check: `LaunchedEffect(selectedDot)` is triggered
- Check: `currentSoilData` is set correctly
- Check: `hasSoilData()` returns true

### Bluetooth parsing fails
- Check: Format matches `"NPK=X,X,X;PH=X;TEMP=X;MOIST=X"`
- Check: Regex patterns in `SoilSensorData.fromResponse()`
- Check: Error message in dialog shows what failed

## Next Steps

1. **Test the implementation** in your app
2. **Customize data ranges** in `SoilData.isValid()` if needed
3. **Add Room database** when persistent storage is needed
4. **Add export features** for CSV/JSON export
5. **Add statistics** dashboard showing soil quality metrics

## Support

For questions about the implementation:
- See `SOIL_DATA_IMPLEMENTATION.md` for detailed documentation
- Check code comments in each file
- Review data flow diagrams in implementation doc

## Files Modified Summary

| File | Changes |
|------|---------|
| `GetSoilData.kt` | Added ViewModel injection, dialogs, marker coloring, data display |
| `SoilSensorData.kt` | Added pH, temp, moisture parsing; added `toSoilData()` conversion |
| `SoilDataViewModel.kt` | Created - manages all data storage operations |
| `SoilData.kt` | Created - immutable data model with validation |

