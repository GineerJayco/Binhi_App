# Save Data Feature - Visual Flow & Diagrams

## 1. User Journey

```
┌──────────────────────────────────────────────────────────────────┐
│                    BINHI APP - SAVE DATA FLOW                    │
└──────────────────────────────────────────────────────────────────┘

START
  │
  ├─→ [GetSoilData Screen]
  │   │
  │   ├─→ User navigates map
  │   │
  │   ├─→ User clicks dots on map
  │   │
  │   ├─→ User receives sensor data (Bluetooth)
  │   │
  │   ├─→ Data is saved to dots (Blue → Green markers)
  │   │
  │   └─→ User has collected soil data ✓
  │
  ├─→ [Save Data Button Click]
  │   │
  │   └─→ Orange "Save Data" button at bottom
  │
  ├─→ [Save Session Dialog Opens]
  │   │
  │   ├─ Display session summary
  │   │  ├ Crop type
  │   │  ├ Land area
  │   │  ├ Field dimensions
  │   │  ├ Total dots
  │   │  ├ Data collected
  │   │  └ Completion %
  │   │
  │   ├─ Text input for session name
  │   │
  │   └─ Buttons: [Save Session] [Cancel]
  │
  ├─→ [User Names Session]
  │   │
  │   ├─ Types descriptive name
  │   │
  │   └─ Button becomes enabled ✓
  │
  ├─→ [User Clicks Save Session]
  │   │
  │   ├─ Loading spinner appears
  │   │
  │   └─ Data is processed
  │
  ├─→ [ViewModel Saves Session]
  │   │
  │   ├─ Create SavedSession object
  │   │
  │   ├─ Convert coordinates
  │   │
  │   ├─ Capture all parameters
  │   │
  │   └─ Add to savedSessions list
  │
  ├─→ [Dialog Closes]
  │   │
  │   └─ Returns to map view
  │
  ├─→ [Session Saved Successfully] ✓
  │   │
  │   └─ Data available in SavedData screen
  │
  ├─→ [User Navigates to SavedData]
  │   │
  │   ├─→ [Saved Sessions List]
  │   │   │
  │   │   ├─→ [Session Card 1]
  │   │   │   ├─ Name: "Field A - January 2026"
  │   │   │   ├─ Date: 02/18/2026 10:30
  │   │   │   ├─ Info: Crop | Area | Dots
  │   │   │   ├─ Progress: ████████░░ 80%
  │   │   │   └─ Delete button
  │   │   │
  │   │   ├─→ [Session Card 2]
  │   │   │   └─ ...
  │   │   │
  │   │   └─→ [Session Card 3]
  │   │       └─ ...
  │   │
  │   ├─→ [User Clicks Card]
  │   │   │
  │   │   └─ Details dialog shows all info
  │   │
  │   └─→ [User Clicks Delete]
  │       │
  │       ├─ Confirmation dialog appears
  │       │
  │       └─ User confirms deletion
  │
  └─→ END

```

---

## 2. Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                      ARCHITECTURE                              │
└────────────────────────────────────────────────────────────────┘

USER INTERFACE LAYER
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌─────────────────┐  ┌──────────────────────────────────┐ │
│  │ GetSoilData     │  │ SavedData                        │ │
│  ├─────────────────┤  ├──────────────────────────────────┤ │
│  │ • Map View      │  │ • Session List (LazyColumn)     │ │
│  │ • Save Button   │◄─┤ • Session Cards                 │ │
│  │ • Save Dialog   │  │ • Details Dialog                │ │
│  │ • Markers       │  │ • Delete Confirmation           │ │
│  └─────────────────┘  └──────────────────────────────────┘ │
│         ▲                            ▲                      │
│         │                            │                      │
└─────────┼────────────────────────────┼──────────────────────┘
          │                            │
          │ Compose State Updates      │
          │                            │
VIEWMODEL LAYER
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      SoilDataViewModel                               │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ Mutable State:                                       │  │
│  │  • soilDataStorage: Map<LatLng, SoilData>           │  │
│  │  • totalDotsCount: Int                              │  │
│  │  • savedSessions: List<SavedSession>               │  │
│  │                                                      │  │
│  │ Methods:                                            │  │
│  │  • saveSoilData()                                   │  │
│  │  • saveCurrentSession()  ◄── NEW                    │  │
│  │  • loadSession()          ◄── NEW                   │  │
│  │  • getAllSavedSessions()  ◄── NEW                   │  │
│  │  • deleteSavedSession()   ◄── NEW                   │  │
│  │  • getSoilData()                                    │  │
│  │  • hasSoilData()                                    │  │
│  └──────────────────────────────────────────────────────┘  │
│         ▲                            ▲                      │
└─────────┼────────────────────────────┼──────────────────────┘
          │                            │
          │ Create/Read/Delete         │
          │                            │
DATA MODEL LAYER
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌──────────────────┐      ┌────────────────────────────┐  │
│  │   SoilData       │      │   SavedSession             │  │
│  ├──────────────────┤      ├────────────────────────────┤  │
│  │ • nitrogen       │◄─────┤ • id: String               │  │
│  │ • phosphorus     │◄─────┤ • sessionName: String      │  │
│  │ • potassium      │◄─────┤ • landArea: Double         │  │
│  │ • phLevel        │◄─────┤ • crop: String             │  │
│  │ • temperature    │◄─────┤ • soilDataPoints: Map      │  │
│  │ • moisture       │◄─────┤ • polygonCenter: Pair      │  │
│  │ • timestamp      │◄─────┤ • rotation: Float          │  │
│  └──────────────────┘      │ • totalDots: Int           │  │
│                            │ • timestamp: Long          │  │
│                            └────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

```

---

## 3. Data Flow - Save Operation

```
┌─────────────────────────────────────────────────────────────┐
│         SAVE OPERATION DATA FLOW                            │
└─────────────────────────────────────────────────────────────┘

User clicks "Save Data"
        │
        ▼
┌──────────────────────┐
│ showSaveSessionDialog │
│   = true             │
└──────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Dialog Composable Rendered       │
│  • Shows session summary         │
│  • Input field for name          │
│  • Save/Cancel buttons           │
└──────────────────────────────────┘
        │
        ▼ (User enters name)
┌──────────────────────────────────┐
│ sessionName = "Field A - Jan"    │
│                                  │
│ Button enabled = true            │
└──────────────────────────────────┘
        │
        ▼ (User clicks Save Session)
┌──────────────────────────────────┐
│ isSavingSession = true           │
│ Loading spinner shows            │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────┐
│ Launch Coroutine                            │
│  coroutineScope.launch {                    │
│    // Run on IO thread                      │
│  }                                          │
└──────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────┐
│ Convert Current State              │
│                                    │
│ mapTypeStr = if (SATELLITE)        │
│              "SATELLITE" else      │
│              "NORMAL"              │
│                                    │
│ landArea = landArea.toDouble()     │
│ length = length.toDouble()         │
│ width = width.toDouble()           │
│ crop = crop ?: "Unknown"           │
└────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│ Call ViewModel Method               │
│                                      │
│ soilDataViewModel.saveCurrentSession(│
│   sessionName = "Field A - Jan"     │
│   landArea = 500.0                 │
│   length = 25.0                    │
│   width = 20.0                     │
│   crop = "Corn"                    │
│   polygonCenter = LatLng(9.3, 123)  │
│   rotation = 45.5f                 │
│   mapType = "SATELLITE"            │
│ )                                   │
└──────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────┐
│ ViewModel Processing               │
│                                    │
│ 1. Convert LatLng to Pair:         │
│    (9.3, 123.308)                  │
│                                    │
│ 2. Create SavedSession object:     │
│    SavedSession(                   │
│      id = UUID.random()            │
│      sessionName = "Field A - Jan" │
│      landArea = 500.0              │
│      ...all parameters...          │
│      soilDataPoints = Map(...)     │
│      timestamp = now()             │
│    )                               │
│                                    │
│ 3. Add to list:                    │
│    savedSessions = savedSessions   │
│                   + newSession     │
│                                    │
│ 4. Return session object           │
└────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Back to Composable               │
│                                  │
│ Log.d("SaveSession",             │
│       "✓ Session saved: ...")    │
│                                  │
│ sessionSaveSuccess = true        │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Delay 1500ms                     │
│                                  │
│ (Allows user to see              │
│  confirmation if desired)        │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Clean Up                         │
│                                  │
│ showSaveSessionDialog = false   │
│ sessionName = ""                │
│ sessionSaveSuccess = false      │
│ isSavingSession = false         │
└──────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────┐
│ Dialog Closes                    │
│ Return to Map View               │
│ Session Saved ✓                 │
└──────────────────────────────────┘

```

---

## 4. Session Display Structure

```
SavedData Screen Structure
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─ Column (Main Container)
│
├─ Row (Top Bar)
│  └─ [◀] Back Button
│
├─ Text "Saved Sessions" (Title)
│
├─ LazyColumn (Session List)
│  │
│  ├─ SessionCard #1
│  │  └─ Card(
│  │     ├─ Row (Header)
│  │     │  ├─ Column (Info)
│  │     │  │  ├─ Text "Field A - January 2026"
│  │     │  │  └─ Text "02/18/2026 10:30:45"
│  │     │  └─ IconButton (Delete)
│  │     │     └─ Icon(Delete, Red)
│  │     │
│  │     ├─ Divider
│  │     │
│  │     ├─ Text "Crop: Corn | Area: 500..."
│  │     │
│  │     └─ LinearProgressIndicator (80%)
│  │        └─ Green progress bar
│  │
│  ├─ SessionCard #2
│  │  └─ ... (similar structure)
│  │
│  └─ SessionCard #3
│     └─ ... (similar structure)
│
├─ Dialog: SessionDetailsDialog
│  └─ AlertDialog(
│     ├─ Title: Session Name
│     ├─ LazyColumn(
│     │  ├─ Text "Session Details"
│     │  ├─ Text "Date: ..."
│     │  ├─ Text "Crop: ..."
│     │  ├─ Text "Area: ..."
│     │  ├─ Text "Dots: ..."
│     │  ├─ Text "Data Points: ..."
│     │  ├─ Text "Completion: ..."
│     │  ├─ Text "Map Type: ..."
│     │  └─ Text "Rotation: ..."
│     └─ Button "Close"
│
└─ Dialog: DeleteConfirmationDialog
   └─ AlertDialog(
      ├─ Title: "Delete Session?"
      ├─ Text: "Are you sure..."
      ├─ Button "Delete" (Red)
      └─ Button "Cancel"
```

---

## 5. State Management Diagram

```
GetSoilData Composable State
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Local Mutable State:
┌─────────────────────────────┐
│ showSaveSessionDialog: false │  ──→ Controls dialog visibility
│ sessionName: ""              │  ──→ User input for name
│ isSavingSession: false       │  ──→ Loading state
│ sessionSaveSuccess: false    │  ──→ Success feedback
└─────────────────────────────┘


SavedData Composable State
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Local Mutable State:
┌───────────────────────────────────┐
│ selectedSession: SavedSession? = null │  ──→ Currently viewed session
│ showSessionDetails: false          │  ──→ Details dialog visibility
│ showDeleteConfirm: false           │  ──→ Delete confirmation visibility
│ sessionToDelete: SavedSession? = null  ──→ Session pending deletion
└───────────────────────────────────┘


SoilDataViewModel Persistent State
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Observable State:
┌───────────────────────────────────┐
│ soilDataStorage: Map<LatLng, SoilData> │  ──→ Collected measurements
│ totalDotsCount: Int               │  ──→ Grid size
│ savedSessions: List<SavedSession> │  ──→ All saved sessions
└───────────────────────────────────┘
     ▲
     │ Updates flow back to UI
     │
┌────────────────────────────────────┐
│ Any changes trigger recomposition  │
│ UI automatically refreshes         │
└────────────────────────────────────┘
```

---

## 6. Component Hierarchy

```
GetSoilData
  │
  ├─ Box (Main Container)
  │  │
  │  ├─ GoogleMap
  │  │  ├─ Polygon (Field boundary)
  │  │  └─ Markers (Soil collection points)
  │  │
  │  ├─ MapScaleBar
  │  │
  │  ├─ TopAppBar
  │  │
  │  ├─ FloatingActionButton (Location)
  │  │
  │  ├─ Column (Controls)
  │  │  ├─ RotateRight Button
  │  │  ├─ RotateLeft Button
  │  │  ├─ Movement Buttons
  │  │  ├─ Location Toggle
  │  │  └─ Map Type Switch
  │  │
  │  └─ Row (Bottom Buttons)
  │     ├─ Save Data Button ◄── NEW
  │     └─ Get Crop Rec Button
  │
  ├─ Dialog (Location)
  │  └─ Card
  │     └─ Column
  │        ├─ Coordinates
  │        ├─ Receive Data Button
  │        └─ Close Button
  │
  ├─ Dialog (Bluetooth Response)
  │  └─ Card
  │     └─ Column
  │        ├─ Soil Data Display
  │        ├─ Save Button
  │        └─ Close Button
  │
  ├─ Dialog (Success Message)
  │  └─ Card
  │     └─ Column
  │        ├─ Success Icon
  │        └─ Success Text
  │
  └─ Dialog (Save Session) ◄── NEW
     └─ Card
        └─ Column
           ├─ Title
           ├─ Session Summary
           ├─ Name Input
           ├─ Save Button
           └─ Cancel Button


SavedData
  │
  └─ Column
     │
     ├─ IconButton (Back)
     │
     ├─ Text (Title)
     │
     ├─ LazyColumn (Session List)
     │  │
     │  └─ SessionCard (Multiple)
     │     └─ Card
     │        ├─ Row (Header)
     │        ├─ Divider
     │        ├─ Info Text
     │        └─ Progress Bar
     │
     ├─ Dialog (Session Details)
     │  └─ AlertDialog
     │     └─ LazyColumn
     │
     └─ Dialog (Delete Confirmation)
        └─ AlertDialog
```

---

## 7. Color & Icon Reference

```
UI Elements
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

BUTTONS:
┌─────────────────────────────────┐
│ Save Data Button                │
│ Color: Orange (#FF9800)         │
│ Icon: Save                      │
│ State: Always Enabled           │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ Get Crop Recommendation Button  │
│ Color: Blue (#2196F3)           │
│ Icon: Agriculture               │
│ State: Conditional (all dots)   │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ Delete Button (Session)         │
│ Color: Red (#FF0000)            │
│ Icon: Delete                    │
│ State: Always Visible           │
└─────────────────────────────────┘

MARKERS:
┌─────────────────────────────────┐
│ Unsaved Data                    │
│ Color: Blue                     │
│ Icon: Default Marker            │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ Saved Data                      │
│ Color: Green                    │
│ Icon: Default Marker (Green)    │
└─────────────────────────────────┘

INDICATORS:
┌─────────────────────────────────┐
│ Progress Bar (Complete)         │
│ Color: Green (#4CAF50)          │
│ Shows: Data collection progress │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ Loading Spinner                 │
│ Color: Matches button color     │
│ Shows: During save operation    │
└─────────────────────────────────┘
```

---

Great! This provides comprehensive visual documentation of the save data feature. 🎉


