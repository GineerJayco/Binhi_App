# Save Data Feature Implementation Guide

## Overview
This document explains the new "Save Data" feature that has been added to your Binhi App. Users can now save complete soil mapping sessions including all the map data, soil measurements, and field information to the SavedData screen.

---

## What Was Added

### 1. **SavedSession Data Model** (`SavedSession.kt`)
A new data class that represents a complete saved session containing:
- **Session Metadata**: Name, ID, and timestamp
- **Field Information**: Crop type, land area, field dimensions (length × width)
- **Map Data**: Polygon center coordinates, rotation angle, map type (SATELLITE/NORMAL)
- **Soil Data**: All collected soil measurements for each location
- **Progress**: Total dots and number of data points collected

```kotlin
data class SavedSession(
    val id: String,
    val sessionName: String,
    val landArea: Double,
    val length: Double,
    val width: Double,
    val crop: String,
    val polygonCenter: Pair<Double, Double>,
    val rotation: Float,
    val mapType: String,
    val totalDots: Int,
    val soilDataPoints: Map<Pair<Double, Double>, SoilData>,
    val timestamp: Long
)
```

### 2. **ViewModel Updates** (`SoilDataViewModel.kt`)
Added new methods to manage saved sessions:

- **`saveCurrentSession()`**: Saves the entire current session with all data
- **`loadSession()`**: Restores a previously saved session
- **`getAllSavedSessions()`**: Retrieves all saved sessions
- **`deleteSavedSession()`**: Removes a session by ID
- **`savedSessions`**: Observable state containing all saved sessions

### 3. **GetSoilData.kt - Save Button & Dialog**
Added a new **"Save Data"** button (orange color) at the bottom of the map alongside the existing "Get Crop Recommendation" button.

**Button Features**:
- Available at all times (not grayed out like the recommendation button)
- Opens a save dialog when clicked
- Allows users to name their session before saving

**Save Dialog Features**:
- Shows a session summary with key information:
  - Crop type
  - Land area
  - Field size
  - Total dots and data collected
  - Completion percentage
- Text field for entering a custom session name
- Save and Cancel buttons
- Loading state during save operation

### 4. **SavedData.kt - Enhanced Screen**
Completely redesigned to display all saved sessions with:

**Session Cards**:
- Session name and timestamp
- Quick info: Crop, area, and completion stats
- Progress bar showing data collection completion
- Delete button (red) for removing sessions

**Session Details Dialog**:
- Click any session card to view full details including:
  - Complete date/time
  - Crop information
  - Land area and field dimensions
  - Total dots and data points
  - Completion percentage
  - Map type and rotation angle

**Delete Confirmation**:
- Confirmation dialog before deleting any session
- Prevents accidental data loss

---

## How to Use

### Saving a Session

1. **Collect Soil Data**:
   - In the GetSoilData screen, click on dots on the map
   - Click "Receive Data" to collect soil measurements from your sensor
   - Save the measurements for each location

2. **Save the Session**:
   - Once you've collected data, click the orange **"Save Data"** button at the bottom
   - Enter a descriptive name for your session (e.g., "Field A - January 2026")
   - Review the session summary to confirm all data
   - Click **"Save Session"** to save

3. **Access Saved Data**:
   - Navigate to the SavedData screen from the main menu
   - View all your saved sessions as cards
   - Click any card to see detailed information
   - Delete sessions using the trash icon if needed

---

## Data Structure

### What Gets Saved
When you save a session, the following information is preserved:

```
Session Data:
├── Metadata
│   ├── Session Name (user-defined)
│   ├── Unique ID (auto-generated)
│   └── Timestamp (date/time created)
├── Field Information
│   ├── Crop Type
│   ├── Land Area (m²)
│   ├── Length (m)
│   └── Width (m)
├── Map Settings
│   ├── Polygon Center (latitude, longitude)
│   ├── Rotation Angle
│   └── Map Type (SATELLITE or NORMAL)
├── Grid Information
│   ├── Total Dots Count
│   └── Data Points: {Location → Soil Measurements}
└── Soil Data (for each location)
    ├── Nitrogen (NPK)
    ├── Phosphorus (NPK)
    ├── Potassium (NPK)
    ├── pH Level
    ├── Temperature (°C)
    └── Moisture (%)
```

---

## Technical Implementation

### State Management
All saved sessions are stored in the `SoilDataViewModel` using Compose's `mutableStateOf`:
```kotlin
var savedSessions by mutableStateOf(listOf<SavedSession>())
```

This ensures the UI automatically updates whenever sessions are saved or deleted.

### Data Conversion
Locations are converted between Kotlin's `LatLng` objects and serializable `Pair<Double, Double>`:
```kotlin
SavedSession.latLngToPair(latLng)    // LatLng → Pair
SavedSession.pairToLatLng(pair)      // Pair → LatLng
```

### Coroutines
Session saving runs in a coroutine to prevent UI blocking:
```kotlin
coroutineScope.launch {
    // Saving operation
}
```

---

## Future Enhancements

Possible improvements for future versions:

1. **Persistent Storage**: Save sessions to device storage or cloud
2. **Session Export**: Export data as CSV or PDF
3. **Session Comparison**: Compare multiple sessions side-by-side
4. **Map Replay**: Replay the exact map state and sensor readings
5. **Data Analytics**: Show trends across multiple sessions
6. **Sharing**: Share session data with team members
7. **Filtering**: Filter sessions by crop, date range, or area
8. **Search**: Search for specific sessions by name or crop

---

## Files Modified

1. ✅ `SavedSession.kt` - NEW
2. ✅ `GetSoilData.kt` - Added save button and dialog
3. ✅ `SavedData.kt` - Complete redesign
4. ✅ `SoilDataViewModel.kt` - Added session management methods

---

## Testing Checklist

- [ ] Save button appears at the bottom of the map
- [ ] Clicking "Save Data" opens the session dialog
- [ ] Session summary displays correct information
- [ ] Session can be saved with a custom name
- [ ] Saved sessions appear in SavedData screen
- [ ] Session cards display correct information
- [ ] Clicking a session shows full details
- [ ] Delete button removes sessions
- [ ] Delete confirmation dialog appears
- [ ] Sessions persist after navigation
- [ ] Multiple sessions can be saved and displayed

---

## Summary

The Save Data feature provides a complete solution for storing and managing soil mapping sessions. Users can:
- Save complete session state with a single click
- Give sessions meaningful names for easy identification
- View all saved sessions with progress indicators
- Access detailed information about any saved session
- Delete sessions when no longer needed

This enhancement makes the app more useful for tracking multiple field surveys and maintaining historical data for analysis.

