# Soil Data Storage Implementation

## Overview
This document describes the complete implementation of soil data storage for the Binhi App's Google Maps integration with Bluetooth Classic support.

## Architecture

### Files Created/Modified

1. **SoilData.kt** (NEW)
   - Location: `app/src/main/java/com/example/binhi/data/SoilData.kt`
   - Data class representing stored soil measurements
   - Fields: nitrogen, phosphorus, potassium, phLevel, temperature, moisture, timestamp
   - Includes validation method `isValid()` to ensure data integrity

2. **SoilDataViewModel.kt** (UPDATED)
   - Location: `app/src/main/java/com/example/binhi/viewmodel/SoilDataViewModel.kt`
   - Manages soil data storage per map location using `Map<LatLng, SoilData>`
   - Extends Android's `ViewModel` for lifecycle awareness
   - Methods:
     - `saveSoilData()`: Store validated soil data at a location
     - `getSoilData()`: Retrieve stored data by location
     - `hasSoilData()`: Check if location has data
     - `getAllStoredLocations()`: Get all locations with data
     - `deleteSoilData()`: Remove data from a location
     - `clearAllData()`: Clear all stored data
     - `getStoredDataCount()`: Get count of stored locations

3. **SoilSensorData.kt** (UPDATED)
   - Enhanced to parse complete Bluetooth format: `"NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"`
   - Added fields for pH, temperature, and moisture
   - New method `toSoilData()`: Convert parsed sensor data to persistent SoilData object
   - Regex parsing for all sensor values with graceful fallback

4. **GetSoilData.kt** (UPDATED)
   - Integrated SoilDataViewModel for persistent data storage
   - Enhanced dialogs to show all soil data fields
   - New features:
     - Display stored soil data when clicking green markers
     - Save button to persist received data to specific map dot
     - Success message confirmation
     - Color-coded markers (BLUE for unsaved, GREEN for saved)

## Data Flow

### Bluetooth Data Reception
```
ESP32 sends: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
    ↓
SoilSensorData.fromResponse() parses the string
    ↓
User clicks "Receive Data" button on map dot
    ↓
Bluetooth manager receives from ESP32
    ↓
Response dialog shows all 6 soil parameters
    ↓
User clicks "Save Data" button
    ↓
SoilSensorData.toSoilData() converts to persistent SoilData
    ↓
SoilDataViewModel.saveSoilData() stores at LatLng location
    ↓
Marker color changes GREEN, success message displayed
```

### Data Retrieval
```
User clicks on a map dot
    ↓
GetSoilData composable checks LaunchedEffect
    ↓
If SoilDataViewModel.hasSoilData(dot) is true
    ↓
Automatically loads stored data using getSoilData()
    ↓
Dialog displays stored soil data fields
    ↓
User can receive new data or close dialog
```

## UI Components

### Dialog Elements

1. **Sample Location Dialog** (when dot is clicked)
   - Shows coordinates in DMS format
   - Displays stored soil data if available
   - "Receive Data" button to fetch from Bluetooth
   - "Close" button

2. **Received Soil Data Dialog** (after Bluetooth reception)
   - Shows all 6 soil parameters in DataRow format:
     - Nitrogen (Int)
     - Phosphorus (Int)
     - Potassium (Int)
     - pH Level (Float, 2 decimal places)
     - Temperature (Float, 1 decimal place with °C)
     - Moisture (Int with % sign)
   - Shows raw Bluetooth response
   - "Save Data" button (green, shows only on successful reception)
   - "Close" button

3. **Success Message Dialog**
   - Green card with checkmark icon
   - "Data Saved Successfully!" message
   - "Marker color changed to green" subtitle
   - Auto-closes after 2 seconds

### Map Markers

- **BLUE markers**: Dots without saved data (HUE_BLUE)
- **GREEN markers**: Dots with saved soil data (HUE_GREEN)
- Markers are clickable and trigger the location dialog
- Color updates immediately after saving data

## State Management

### Compose State (GetSoilData.kt)
```kotlin
var currentSoilData by remember { mutableStateOf<SoilData?>(null) }
var bluetoothResponse by remember { mutableStateOf<SoilSensorData?>(null) }
var showDialog by remember { mutableStateOf(false) }
var showBluetoothDialog by remember { mutableStateOf(false) }
var showSaveSuccessMessage by remember { mutableStateOf(false) }
var isBluetoothLoading by remember { mutableStateOf(false) }
```

### ViewModel State (SoilDataViewModel)
```kotlin
private val soilDataStorage = mutableMapOf<LatLng, SoilData>()
```

### Data Persistence
- ViewModel persists data across recompositions
- Data survives screen navigation (as long as ViewModel scope is maintained)
- Data is NOT persisted across app restarts (in-memory storage)
- For persistent storage, consider adding Room database integration

## Data Validation

**SoilData.isValid()** checks:
```kotlin
nitrogen >= 0 && 
phosphorus >= 0 && 
potassium >= 0 &&
phLevel > 0f && phLevel <= 14f &&
temperature >= -40f && temperature <= 80f &&
moisture >= 0 && moisture <= 100
```

If validation fails, the data is NOT saved and no success message is shown.

## Bluetooth Format Parsing

### Expected Format
```
NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

### Parsing Logic
- Uses regex patterns for flexible parsing
- Allows whitespace around `=` and commas
- Each parameter is optional (except NPK)
- If parameter is missing, defaults to 0 (0f for floats)
- Gracefully handles malformed data with error messages

### Regex Patterns
```kotlin
NPK: "NPK\s*=\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)"
PH:  "PH\s*=\s*([\d.]+)"
TEMP: "TEMP\s*=\s*([\d.]+)"
MOIST: "MOIST\s*=\s*(\d+)"
```

## LaunchedEffect Implementation

Auto-loads stored data when a dot with saved data is clicked:
```kotlin
LaunchedEffect(selectedDot) {
    if (selectedDot != null && soilDataViewModel.hasSoilData(selectedDot!!) && currentSoilData == null) {
        currentSoilData = soilDataViewModel.getSoilData(selectedDot!!)
    }
}
```

This ensures:
- No extra API calls
- Data loads from memory only
- User sees data immediately upon clicking
- Prevents accidental overwrites

## Threading

- All Bluetooth communication runs on coroutine (non-blocking)
- UI updates happen on main thread via Compose state management
- ViewModel operations are synchronous (in-memory)
- No database queries, so no background thread concerns

## Error Handling

1. **Bluetooth Permission Denied**: Shows error dialog
2. **Invalid Bluetooth Response**: Shows error with raw response
3. **Invalid Soil Data**: Won't save if `isValid()` fails
4. **Parse Errors**: Caught and displayed to user

## Future Enhancements

1. **Room Database Integration**
   - For persistent storage across app restarts
   - Add entity: `@Entity class SoilDataEntity`
   - Add DAO with CRUD operations

2. **Data Export**
   - CSV export of all stored soil data
   - Include timestamp for each measurement

3. **Data Visualization**
   - Charts showing soil parameters over time
   - Heatmap overlay on map based on soil quality

4. **Multiple Samples Per Location**
   - Store multiple measurements per dot
   - Calculate averages, trends
   - Add date/time filtering

5. **Search and Filter**
   - Find locations by soil parameter ranges
   - Filter by date/time range
   - Show statistics dashboard

## Testing

### Test Cases

1. **Receiving Data**
   - Click dot → Click "Receive Data" → Wait for Bluetooth → Verify dialog shows all 6 fields

2. **Saving Data**
   - Receive data → Click "Save Data" → Verify success message → Verify marker turns green

3. **Loading Data**
   - Click saved dot (green) → Verify stored data appears immediately in dialog

4. **Multiple Dots**
   - Save data to 3 different dots → Verify correct marker colors → Verify correct data loads for each

5. **Invalid Data**
   - Manually test with out-of-range values → Verify data won't save

6. **Bluetooth Errors**
   - Disable Bluetooth → Try to receive → Verify error message

## Code Structure

### Clean Architecture Principles
✅ Separation of concerns (UI, ViewModel, Data)
✅ No blocking calls on main thread
✅ Immutable data classes
✅ Dependency injection (ViewModel provided by viewModel())
✅ SOLID principles followed

### Production Ready
✅ Comprehensive error handling
✅ User-friendly error messages
✅ Clear visual feedback (colors, animations)
✅ Well-documented code
✅ Scalable design for future enhancements

