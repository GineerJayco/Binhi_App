# Soil Data Storage - Visual Reference Guide

## User Interaction Flow

### Scenario 1: Receiving and Saving Data

```
┌─────────────────────────────────────────────────────────────┐
│ Map View with Soil Sample Points                            │
│                                                              │
│  [Blue Dot] [Blue Dot] [Blue Dot]  ← Unsaved points       │
│                                                              │
│  Blue = No data stored yet                                  │
│  Green = Data already saved                                 │
└──────────────────┬────────────────────────────────────────┘
                   │
                   │ User clicks Blue dot
                   ↓
        ┌──────────────────────────────┐
        │ Sample Location Dialog        │
        ├──────────────────────────────┤
        │ Latitude: 9°18'36.2" N        │
        │ Longitude: 123°18'45.6" E     │
        ├──────────────────────────────┤
        │ ❌ No Data Stored             │
        │                              │
        │ [Receive Data Button]        │
        │ [Close Button]               │
        └──────────────────────────────┘
                   │
                   │ User clicks "Receive Data"
                   │ (sends Bluetooth command)
                   ↓
        ┌──────────────────────────────┐
        │ Loading State                 │
        ├──────────────────────────────┤
        │ ⟳ Receiving...               │
        │ (waiting for ESP32)          │
        └──────────────────────────────┘
                   │
                   │ Bluetooth data received
                   ↓
        ┌──────────────────────────────┐
        │ Received Soil Data Dialog     │
        ├──────────────────────────────┤
        │ ✓ All Values Received        │
        │                              │
        │ Nitrogen         12          │
        │ Phosphorus       7           │
        │ Potassium        9           │
        │ pH Level         6.50        │
        │ Temperature      29.4°C      │
        │ Moisture         62%         │
        │                              │
        │ Raw: NPK=12,7,9;...         │
        │                              │
        │ [💾 Save Data] [Close]      │
        └──────────────────────────────┘
                   │
                   │ User clicks "Save Data"
                   ↓
        ┌──────────────────────────────┐
        │ Success Message (2 sec)      │
        ├──────────────────────────────┤
        │         ✓                    │
        │ Data Saved Successfully!     │
        │ Marker color changed to      │
        │ green                        │
        └──────────────────────────────┘
                   │
                   ↓
        ┌──────────────────────────────┐
        │ Map View Updated             │
        │                              │
        │  [Blue] [Green] [Blue]       │
        │            ↑                 │
        │      Just saved!            │
        └──────────────────────────────┘
```

### Scenario 2: Loading Saved Data

```
┌─────────────────────────────────────────────────────────────┐
│ Map View with Mixed Markers                                  │
│                                                              │
│  [Blue] [Green] [Green] [Blue] [Green]                     │
│           ↓                      ↓                          │
│      Has data              Has data                         │
└──────────────────────────────────────────────────────────────┘
                   │
                   │ User clicks Green marker
                   │ (has stored data)
                   ↓
        LaunchedEffect detects click
        checks: hasSoilData(location)
        result: TRUE
                   │
                   ↓
        Automatically loads data from
        SoilDataViewModel in O(1) time
                   │
                   ↓
        ┌──────────────────────────────┐
        │ Sample Location Dialog        │
        ├──────────────────────────────┤
        │ Latitude: 9°18'36.2" N        │
        │ Longitude: 123°18'45.6" E     │
        ├──────────────────────────────┤
        │ ✅ Stored Soil Data (GREEN)  │
        │                              │
        │ Nitrogen         12          │
        │ Phosphorus       7           │
        │ Potassium        9           │
        │ pH Level         6.50        │
        │ Temperature      29.4°C      │
        │ Moisture         62%         │
        │                              │
        │ [Receive Data] [Close]       │
        └──────────────────────────────┘
                   │
                   │ User can:
                   ├─ Close dialog
                   └─ Receive new data to update
```

## Data Structure Hierarchy

```
ESP32 Transmission
│
└─ Raw String: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
   │
   └─ SoilSensorData.fromResponse()
      │
      └─ SoilSensorData {
         │   nitrogen: 12
         │   phosphorus: 7
         │   potassium: 9
         │   phLevel: 6.5f
         │   temperature: 29.4f
         │   moisture: 62
         │   rawData: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
         │   isError: false
         │   errorMessage: ""
         │
         └─ .toSoilData()
            │
            └─ SoilData {
               │   nitrogen: 12
               │   phosphorus: 7
               │   potassium: 9
               │   phLevel: 6.5f
               │   temperature: 29.4f
               │   moisture: 62
               │   timestamp: 1703840400000
               │
               └─ .isValid() → true
                  │
                  └─ SoilDataViewModel.saveSoilData(
                        location: LatLng(9.3093, 123.308),
                        data: SoilData
                     )
                     │
                     └─ Map<LatLng, SoilData> {
                        LatLng(9.3093, 123.308) → SoilData(12,7,9,6.5,29.4,62)
                     }
```

## State Management Flowchart

```
┌────────────────────────────────────────────────┐
│ GetSoilData Composable State Variables         │
└────────────────────────────────────────────────┘
            │
            ├─ var selectedDot: LatLng?
            │   ├─ null: no selection
            │   └─ LatLng: dot is selected
            │        │
            │        ↓
            │   LaunchedEffect triggered
            │   Check: hasSoilData(dot)?
            │   │
            │   ├─ YES → Load via getSoilData()
            │   │         Set currentSoilData
            │   │
            │   └─ NO → Keep currentSoilData null
            │
            ├─ var showDialog: Boolean
            │   └─ Dialog visibility
            │
            ├─ var bluetoothResponse: SoilSensorData?
            │   └─ Result from ESP32
            │
            ├─ var showBluetoothDialog: Boolean
            │   └─ Response dialog visibility
            │
            ├─ var currentSoilData: SoilData?
            │   └─ Loaded from ViewModel
            │
            ├─ var showSaveSuccessMessage: Boolean
            │   └─ Success notification
            │
            ├─ var isBluetoothLoading: Boolean
            │   └─ Receiving state (shows spinner)
            │
            └─ var hasBluetoothPermission: Boolean
                └─ Permission check result
```

## Marker Color Logic

```
┌─────────────────────────────────────────────┐
│ For Each Dot on Map:                        │
└─────────────────────────────────────────────┘
    │
    ├─ soilDataViewModel.hasSoilData(dot)?
    │   │
    │   ├─ YES (true)
    │   │   │
    │   │   └─ markerColor = HUE_GREEN (0.0f)
    │   │       │
    │   │       └─ Marker shows as GREEN
    │   │           User can click to load data
    │   │
    │   └─ NO (false)
    │       │
    │       └─ markerColor = HUE_BLUE (240.0f)
    │           │
    │           └─ Marker shows as BLUE
    │               User can click to receive data
    │
    └─ Update happens immediately on save:
       soilDataViewModel.saveSoilData(dot, data)
       │
       └─ Next recomposition
          │
          └─ Marker color recalculated
             │
             └─ Marker UI updates to GREEN
```

## Dialog Stack Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Map (Base Layer)                            │
│            [Blue Dots] [Green Dots]                          │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ Dialog System (Layered)
                        │
            ┌───────────┴───────────┐
            │                       │
    ┌───────▼────────┐      ┌───────▼────────┐
    │ Sample Location│      │  Bluetooth     │
    │    Dialog      │      │  Response      │
    │ (Always below) │      │  Dialog        │
    │                │      │ (Mid-level)    │
    │ [Coordinates]  │      │                │
    │ [Data if any]  │      │ [All 6 Fields] │
    │ [Receive Data] │      │ [Save Button]  │
    └────────────────┘      └────────────────┘
                                    │
                                    │ Over this
                                    ↓
                            ┌──────────────────┐
                            │ Success Message  │
                            │ Dialog           │
                            │ (Top-level)      │
                            │                  │
                            │ [✓ Saved!]       │
                            │ (Auto-close)     │
                            └──────────────────┘
```

## Data Validation Pipeline

```
SoilSensorData
    │
    ├─ nitrogen: Int → Must be ≥ 0
    │
    ├─ phosphorus: Int → Must be ≥ 0
    │
    ├─ potassium: Int → Must be ≥ 0
    │
    ├─ phLevel: Float → Must be 0 < pH ≤ 14
    │   └─ Valid: 1.0f to 14.0f
    │   └─ Example valid: 6.5f (acidic)
    │   └─ Example invalid: 15.0f (too high)
    │
    ├─ temperature: Float → Must be -40f ≤ temp ≤ 80f
    │   └─ Valid: -40°C to 80°C
    │   └─ Example valid: 29.4f (tropical)
    │   └─ Example invalid: 100.0f (too hot)
    │
    └─ moisture: Int → Must be 0 ≤ moisture ≤ 100
        └─ Valid: 0% to 100%
        └─ Example valid: 62% (humid)
        └─ Example invalid: 150% (impossible)

        │
        ↓ Call: data.isValid()
        │
    ┌───▼────┐
    │ CHECK  │
    └────────┘
        │
        ├─ ALL fields valid? YES → Save to ViewModel
        │
        └─ ANY field invalid? NO → Reject silently
                                    (no success message)
```

## Bluetooth Message Parsing

```
Raw from ESP32:
"NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"

                    │
                    ↓ Regex Parsing
                    
    ┌───────────────┬──────────────┬─────────────┬──────────────┐
    │               │              │             │              │
    ↓               ↓              ↓             ↓              ↓
NPK=12,7,9      PH=6.5         TEMP=29.4    MOIST=62        (end)

Extract:           Extract:       Extract:     Extract:
N=12               pH=6.5         T=29.4       M=62
P=7                (Float)        (Float)      (Int)
K=9                │              │            │
│                  │              │            │
├─ Nitrogen: 12    └─ phLevel:    └─Temperature└─ moisture:
├─ Phosphorus: 7     6.5f:          29.4f:       62%
└─ Potassium: 9      acidic pH      hot zone

                    │
                    ↓ Create SoilSensorData
                    │
    SoilSensorData(
        nitrogen = 12,
        phosphorus = 7,
        potassium = 9,
        phLevel = 6.5f,
        temperature = 29.4f,
        moisture = 62,
        rawData = "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62",
        isError = false
    )
```

## Memory Usage Profile

```
Storage per Location:
┌─────────────────────────────────────┐
│ LatLng (key)         ~32 bytes      │
│ ├─ latitude: Double  8 bytes        │
│ └─ longitude: Double 8 bytes        │
│                                      │
│ SoilData (value)     ~40 bytes      │
│ ├─ nitrogen: Int     4 bytes        │
│ ├─ phosphorus: Int   4 bytes        │
│ ├─ potassium: Int    4 bytes        │
│ ├─ phLevel: Float    4 bytes        │
│ ├─ temperature: Float 4 bytes       │
│ ├─ moisture: Int     4 bytes        │
│ └─ timestamp: Long   8 bytes        │
│                                      │
│ Total per dot: ~72 bytes            │
└─────────────────────────────────────┘

Example Scenarios:
- 100 dots with data → ~7.2 KB
- 1000 dots with data → ~72 KB
- 10000 dots with data → ~720 KB

Lookup Performance:
HashMap<LatLng, SoilData>
- Average case: O(1)
- Worst case: O(n)
- Typical usage: < 1ms for any lookup
```

## Event Timeline

```
Time    Event                                   State Change
────────────────────────────────────────────────────────────────
T+0s    User clicks Blue dot                   selectedDot set
        Sample Location Dialog opens           showDialog = true
        LaunchedEffect runs
        hasSoilData() → false
        currentSoilData remains null           "No Data Stored"

T+1s    User clicks "Receive Data"             isBluetoothLoading = true
        Bluetooth command sent to ESP32        "Receiving..."

T+3s    ESP32 responds                         bluetoothResponse set
        Response Dialog opens                  showBluetoothDialog = true
        isBluetoothLoading = false
        Dialog shows all 6 fields

T+4s    User clicks "Save Data"                saveSoilData() called
        Validation passes                      hasSoilData() → true
        Data stored in ViewModel               Save button hidden
        Success Dialog appears                 showSaveSuccessMessage = true
        Marker color changes BLUE → GREEN

T+6s    Success Dialog auto-closes             showSaveSuccessMessage = false
        All dialogs close automatically        showDialog = false
        Map refreshes                          Marker stays GREEN

T+10s   User clicks GREEN marker               selectedDot set
        LaunchedEffect runs                    
        hasSoilData() → true                   
        currentSoilData loads from ViewModel
        Sample Location Dialog opens           Shows stored data
```

## File Dependencies

```
GetSoilData.kt (UI Layer)
    ├─ imports: SoilDataViewModel
    │   └─ manages data storage
    ├─ imports: SoilSensorData
    │   └─ parses & converts Bluetooth data
    ├─ imports: SoilData
    │   └─ used by ViewModel storage
    └─ creates markers based on hasSoilData()

SoilDataViewModel.kt (State Layer)
    └─ imports: SoilData
        └─ stored in Map<LatLng, SoilData>

SoilSensorData.kt (Data Layer - Transient)
    └─ imports: SoilData
        └─ converts via toSoilData()

SoilData.kt (Data Layer - Persistent)
    └─ imported by both ViewModel and UI layer
        └─ shared data structure
```

This visual guide should help you understand exactly how the soil data storage system works at every level!

