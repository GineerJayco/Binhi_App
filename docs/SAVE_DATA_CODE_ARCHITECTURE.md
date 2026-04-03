# Save Data Feature - Code Architecture

## Component Overview

### 1. Data Model: SavedSession.kt

**Purpose**: Defines the structure of a saved session

```kotlin
data class SavedSession(
    val id: String,                              // Unique ID
    val sessionName: String,                     // User-defined name
    val landArea: Double,                        // Area in m²
    val length: Double,                          // Field length in m
    val width: Double,                           // Field width in m
    val crop: String,                            // Crop type
    val polygonCenter: Pair<Double, Double>,     // Map center (lat, lng)
    val rotation: Float,                         // Rotation angle
    val mapType: String,                         // SATELLITE or NORMAL
    val totalDots: Int,                          // Total grid dots
    val soilDataPoints: Map<...>,                // All measurements
    val timestamp: Long                          // Creation time
)
```

**Helper Methods**:
- `getFormattedDate()` - Returns formatted date string
- `getCompletionInfo()` - Returns summary text
- `latLngToPair()` - Converts LatLng to serializable Pair
- `pairToLatLng()` - Converts Pair back to LatLng

---

### 2. ViewModel: SoilDataViewModel.kt

**New State Variable**:
```kotlin
var savedSessions by mutableStateOf(listOf<SavedSession>())
```

**New Methods**:

#### saveCurrentSession()
```kotlin
fun saveCurrentSession(
    sessionName: String,
    landArea: Double,
    length: Double,
    width: Double,
    crop: String,
    polygonCenter: LatLng,
    rotation: Float,
    mapType: String
): SavedSession
```
- Converts current soil data to SavedSession
- Adds to savedSessions list
- Returns the created session

#### loadSession()
```kotlin
fun loadSession(session: SavedSession)
```
- Restores soil data from a session
- Updates totalDotsCount
- Ready for viewing/editing

#### getAllSavedSessions()
```kotlin
fun getAllSavedSessions(): List<SavedSession>
```
- Returns all saved sessions

#### deleteSavedSession()
```kotlin
fun deleteSavedSession(sessionId: String)
```
- Removes session by ID

---

### 3. GetSoilData Screen Changes

**New State Variables**:
```kotlin
var showSaveSessionDialog by remember { mutableStateOf(false) }
var sessionName by remember { mutableStateOf("") }
var isSavingSession by remember { mutableStateOf(false) }
var sessionSaveSuccess by remember { mutableStateOf(false) }
```

**Save Session Dialog**:
```kotlin
if (showSaveSessionDialog) {
    Dialog(...) {
        Column(...) {
            // Title
            // Session summary display
            // Name input field
            // Save button
            // Cancel button
        }
    }
}
```

**Save Button (in Button Row)**:
```kotlin
Button(
    onClick = { showSaveSessionDialog = true },
    modifier = Modifier
        .weight(1f)
        .height(48.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFFF9800)  // Orange
    )
) {
    Icon(Icons.Default.Save, ...)
    Spacer(...)
    Text("Save Data")
}
```

**Save Operation**:
```kotlin
coroutineScope.launch {
    val mapTypeStr = if (mapType == MapType.SATELLITE) 
        "SATELLITE" else "NORMAL"
    
    soilDataViewModel.saveCurrentSession(
        sessionName = sessionName,
        landArea = landArea?.toDoubleOrNull() ?: 0.0,
        length = length?.toDoubleOrNull() ?: 0.0,
        width = width?.toDoubleOrNull() ?: 0.0,
        crop = crop ?: "Unknown",
        polygonCenter = polygonCenter,
        rotation = rotation,
        mapType = mapTypeStr
    )
    
    // Close dialog after save
    showSaveSessionDialog = false
}
```

---

### 4. SavedData Screen Redesign

**Main Components**:

#### SavedDataScreen()
```kotlin
@Composable
fun SavedDataScreen(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel = viewModel()
) {
    // State variables for dialogs
    // LazyColumn of SessionCards
    // Dialogs for details and delete confirmation
}
```

#### SessionCard()
```kotlin
@Composable
fun SessionCard(
    session: SavedSession,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(...) {
        // Session name and date
        // Session info text
        // Progress bar
    }
}
```

#### SessionDetailsDialog()
```kotlin
@Composable
fun SessionDetailsDialog(
    session: SavedSession,
    onDismiss: () -> Unit
) {
    AlertDialog(...) {
        LazyColumn {
            item { Text("Date: ...") }
            item { Text("Crop: ...") }
            item { Text("Area: ...") }
            // ... more info items
        }
    }
}
```

---

## Data Flow Diagram

```
User clicks "Save Data"
        ↓
showSaveSessionDialog = true
        ↓
Save Dialog Opens
        ↓
User enters session name
        ↓
User clicks "Save Session"
        ↓
isSavingSession = true
        ↓
soilDataViewModel.saveCurrentSession() called
        ↓
Creates SavedSession object from current state
        ↓
Converts LatLng coordinates to Pair
        ↓
Adds to savedSessions list
        ↓
Returns SavedSession
        ↓
isSavingSession = false
        ↓
Dialog closes
        ↓
Session now visible in SavedData screen
```

---

## State Management

### GetSoilData Screen
```
┌─ GetSoilData Composable
│
├─ UI State
│  ├─ showSaveSessionDialog
│  ├─ sessionName
│  ├─ isSavingSession
│  └─ sessionSaveSuccess
│
├─ ViewModel (Injected)
│  └─ soilDataViewModel
│     ├─ soilDataStorage (all NPK, pH, temp, moisture)
│     ├─ totalDotsCount
│     └─ savedSessions
│
└─ Navigation
   └─ navController
```

### SavedData Screen
```
┌─ SavedDataScreen Composable
│
├─ UI State
│  ├─ selectedSession
│  ├─ showSessionDetails
│  ├─ showDeleteConfirm
│  └─ sessionToDelete
│
├─ ViewModel Data
│  └─ soilDataViewModel.getAllSavedSessions()
│
└─ Navigation
   └─ navController
```

---

## Error Handling

### In Save Dialog
```kotlin
try {
    // Save operation
    soilDataViewModel.saveCurrentSession(...)
    
    Log.d("SaveSession", "✓ Session saved: $sessionName")
    
} catch (e: Exception) {
    Log.e("SaveSession", "Error: ${e.message}")
} finally {
    isSavingSession = false
}
```

### Validation
```kotlin
// Session name must not be blank
enabled = sessionName.isNotBlank() && !isSavingSession

// Confirmation before delete
if (showDeleteConfirm) {
    AlertDialog(...)  // User confirms deletion
}
```

---

## UI/UX Features

### Visual Feedback
- Loading spinner during save
- Success/error logging
- Disabled buttons during operations
- Color-coded buttons (orange for save, red for delete)

### User Experience
- Session summary before saving
- Custom naming for organization
- Easy session inspection
- One-click deletion with confirmation

### Progress Tracking
- Progress bars show completion percentage
- Summary shows data collected vs total
- Clear visual indicators

---

## Integration Points

### With Existing Code
1. **SoilDataViewModel**: Extends existing ViewModel with session management
2. **GetSoilData Screen**: Adds button and dialog, doesn't modify map logic
3. **SavedData Screen**: Completely replaced, now functional
4. **Data Models**: Uses existing SoilData class, wraps in SavedSession

### With Navigation
- GetSoilData: User clicks "Save Data" → Dialog opens
- SavedData: User navigates to see saved sessions
- Bidirectional: Can save, view, and delete sessions

---

## Performance Considerations

### Memory
- Sessions stored in RAM (ViewModel)
- No file I/O yet (future enhancement)
- Efficient LazyColumn for large session lists

### Processing
- Save operation runs in coroutine (non-blocking)
- LatLng to Pair conversion happens during save
- UI updates via Compose state changes

### Scalability
- Supports unlimited sessions (only RAM limited)
- Can be extended with database/file storage
- Efficient list operations with Kotlin stdlib

---

## Testing Scenarios

### Scenario 1: Basic Save
1. Collect data for 3 dots
2. Click "Save Data"
3. Enter name "Test"
4. Click "Save Session"
5. ✓ Session appears in SavedData

### Scenario 2: Multiple Sessions
1. Save 3 different sessions with different names
2. View all 3 in SavedData screen
3. ✓ All appear with correct information

### Scenario 3: Session Details
1. Click session card
2. ✓ Details dialog shows full information
3. Close dialog
4. ✓ Returns to session list

### Scenario 4: Delete Session
1. Click delete button on session
2. ✓ Confirmation dialog appears
3. Click "Delete"
4. ✓ Session removed from list

### Scenario 5: Incomplete Session
1. Collect data for 2 out of 5 dots
2. Save session
3. ✓ Session shows 2/5 completion
4. ✓ Progress bar shows 40%

---

That covers the complete code architecture! 🏗️

