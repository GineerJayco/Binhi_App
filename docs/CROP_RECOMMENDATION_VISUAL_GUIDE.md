# Crop Recommendation Feature - Visual Guide

## Screen Layout

### Before All Dots Are Saved
```
┌─────────────────────────────────────────────────┐
│                    MAP SCREEN                   │
├─────────────────────────────────────────────────┤
│                                                 │
│        [Google Map with Dots]                   │
│        ┌──────────────────┐                     │
│        │  🔵 Blue Dots    │  Unsaved            │
│        │  🟢 Green Dots   │  Saved              │
│        │  🔴 Red Polygon  │  Land Area          │
│        │                  │                     │
│        │  [Only 3/10 done]                      │
│        │                  │                     │
│        └──────────────────┘                     │
│                                                 │
│                                                 │
│     ← MAP CONTROLS                              │
│                                                 │
│     [+] [-] [↑] [↓] [←] [→]                     │
│     [My Location] [Satellite]                   │
│                                                 │
│     [NO BUTTON HERE]                            │
│     ✅ Feature working: button hidden           │
│                                                 │
└─────────────────────────────────────────────────┘
```

### After All Dots Are Saved
```
┌─────────────────────────────────────────────────┐
│                    MAP SCREEN                   │
├─────────────────────────────────────────────────┤
│                                                 │
│        [Google Map with Dots]                   │
│        ┌──────────────────┐                     │
│        │  🔵 Blue Dots    │  (none - all saved) │
│        │  🟢 Green Dots   │  All 10 saved!      │
│        │  🔴 Red Polygon  │  Land Area          │
│        │                  │                     │
│        │  [10/10 done ✓]                        │
│        │                  │                     │
│        └──────────────────┘                     │
│                                                 │
│                                                 │
│     ← MAP CONTROLS                              │
│                                                 │
│     [+] [-] [↑] [↓] [←] [→]                     │
│     [My Location] [Satellite]                   │
│                                                 │
│     ┌─────────────────────────────────────┐    │
│     │  🌾 Get Crop Recommendation        │    │ ← NEW!
│     └─────────────────────────────────────┘    │
│   All 10 sampling points have been collected   │
│     ✅ Feature working: button visible         │
│                                                 │
└─────────────────────────────────────────────────┘
```

## State Diagram

```
                       START
                         │
                         ▼
                 ┌──────────────┐
                 │ Map Loaded   │
                 │ Dots = 0     │
                 └──────────────┘
                         │
                         │ User defines area & crop
                         │ Dots calculated
                         ▼
                 ┌──────────────┐
                 │ totalDotsCount│ = 10
                 │ saved count = 0│
                 │ allDotsComplete│ = FALSE
                 └──────────────┘
                         │
                         │ Button: HIDDEN ❌
                         │
        ┌────────────────┼────────────────┐
        │                │                │
    [Save Dot 1]   [Save Dot 2]   [Save Dot N]
        │                │                │
        ▼                ▼                ▼
    ┌──────┐         ┌──────┐         ┌──────┐
    │saved │         │saved │         │saved │
    │ = 1  │         │ = 2  │         │ = 9  │
    │FALSE │         │FALSE │         │FALSE │
    └──────┘         └──────┘         └──────┘
        │                │                │
        │ Button: HIDDEN ❌ (7 more needed)
        │
        └─────────────────────────────────────┐
                                              │
                                      [Save Dot 10]
                                              │
                                              ▼
                                    ┌──────────────┐
                                    │ saved = 10   │
                                    │ total = 10   │
                                    │ allDotsComplete│
                                    │ = TRUE       │
                                    └──────────────┘
                                              │
                                              │ Button: VISIBLE ✅
                                              │
                                    ┌──────────────────┐
                                    │ User can tap:    │
                                    │ "Get Crop        │
                                    │  Recommendation" │
                                    └──────────────────┘
```

## Component Hierarchy

```
GetSoilData (Main Composable)
├── Box (Container for map and controls)
│   ├── GoogleMap
│   │   ├── Polygon (Land area boundary)
│   │   └── Markers (Soil sampling dots)
│   │       ├── 🟢 Green markers (saved data)
│   │       └── 🔵 Blue markers (unsaved)
│   │
│   ├── TopAppBar (Back button)
│   │
│   ├── FloatingActionButton (My Location)
│   │
│   ├── Column (Right side controls)
│   │   ├── Rotate buttons
│   │   ├── Move buttons (↑↓←→)
│   │   ├── My Location toggle
│   │   ├── Satellite/Normal toggle
│   │   └── Switch
│   │
│   ├── MapScaleBar (Bottom left)
│   │
│   └── ✨ NEW: Column (Bottom center)
│       │   Conditional: visible only when allDotsComplete
│       │
│       ├── Button("Get Crop Recommendation")
│       │   └── onClick → TODO: Navigation/API call
│       │
│       └── Text("All 10 sampling points...")
│
├── Dialog (Sample Location)
│   ├── Location coordinates
│   ├── Stored soil data (if exists)
│   ├── "Receive Data" button → Bluetooth
│   └── "Close" button
│
├── Dialog (Bluetooth Response)
│   ├── Received soil data display
│   ├── "Save Data" button
│   └── "Close" button
│
└── Dialog (Success Message)
    ├── Check icon
    ├── "Data Saved Successfully!"
    └── Auto-close after 2 seconds
```

## State Flow Diagram

```
USER INTERACTION                  VIEWMODEL STATE
─────────────────────────────────────────────────

Input area, length, width
        │
        ▼
Dots calculated in remember
        │
        ▼
LaunchedEffect(dots.size)
        │
        ├─→ setTotalDotsCount(dots.size)
        │           │
        │           ▼
        │   totalDotsCount: State = 10
        │           │
        │           ▼
        │   allDotsComplete: DerivedState
        │   { totalDotsCount > 0 && 
        │     storage.size == totalDotsCount }
        │           │
        │           ├─→ Check: 10 > 0 ✓
        │           ├─→ Check: 0 == 10 ✗
        │           │
        │           ▼
        │   Result: FALSE
        │           │
        ▼           ▼
Compose detects change  if (allDotsComplete) → FALSE
        │               Button NOT rendered
        │
        ├─→ Recompose UI (conditional render)
        │   Button hidden ✅

Tap dot → Dialog shown
        │
Receive Bluetooth data
        │
Save data via saveSoilData(location, data)
        │           │
        │           ▼
        │   storage[location] = data
        │   storage.size = 1
        │           │
        │           ▼
        │   allDotsComplete re-evaluates:
        │   { 10 > 0 ✓ && 1 == 10 ✗ }
        │   Result: FALSE
        │
        ▼
Recompose (still hidden)


[Repeat for dots 2-10]


Save 10th data
        │           │
        │           ▼
        │   storage.size = 10
        │           │
        │           ▼
        │   allDotsComplete re-evaluates:
        │   { 10 > 0 ✓ && 10 == 10 ✓ }
        │   Result: TRUE
        │
        ▼
Compose detects change
        │
        ├─→ Recompose UI (conditional render)
        │   if (allDotsComplete) → TRUE
        │   Button RENDERED ✅
        │
        ▼
Button appears at bottom center
Text: "All 10 sampling points collected"


User taps button
        │
        ▼
onClick handler executes
TODO: Navigate/API call
```

## Completion Percentage Visualization

```
Dots: [🔵🔵🔵🔵🔵🟢🟢🟢🟢🟢]
      [Unsaved]  [Saved]
      0%████████████████████████100%
      
Step 1: [🟢🔵🔵🔵🔵🔵🔵🔵🔵🔵] → 10%
Step 2: [🟢🟢🔵🔵🔵🔵🔵🔵🔵🔵] → 20%
Step 3: [🟢🟢🟢🔵🔵🔵🔵🔵🔵🔵] → 30%
Step 4: [🟢🟢🟢🟢🔵🔵🔵🔵🔵🔵] → 40%
Step 5: [🟢🟢🟢🟢🟢🔵🔵🔵🔵🔵] → 50%
Step 6: [🟢🟢🟢🟢🟢🟢🔵🔵🔵🔵] → 60%
Step 7: [🟢🟢🟢🟢🟢🟢🟢🔵🔵🔵] → 70%
Step 8: [🟢🟢🟢🟢🟢🟢🟢🟢🔵🔵] → 80%
Step 9: [🟢🟢🟢🟢🟢🟢🟢🟢🟢🔵] → 90%
Step 10:[🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢] → 100% ✅ BUTTON APPEARS!
```

## Button Styling Details

```
┌──────────────────────────────────────────┐
│  🌾 Get Crop Recommendation              │  ← 56dp height
├──────────────────────────────────────────┤
│ Color: Material Blue (#2196F3)           │
│ Icon: Agriculture (from Material Icons)  │
│ Icon Size: 24dp                          │
│ Icon Color: White                        │
│ Text: "Get Crop Recommendation"          │
│ Text Size: 16sp                          │
│ Text Color: White                        │
│ Shape: RoundedCornerShape(12.dp)         │
│ Width: 90% of screen                     │
│ Margin Bottom: 32dp                      │
└──────────────────────────────────────────┘

Below button:
All 10 sampling points have been collected
(12sp, white, centered, padded)
```

## Data Storage Visualization

### Before Any Data Saved
```
soilDataStorage: Map<LatLng, SoilData> {
    // Empty
}

totalDotsCount = 10
allDotsComplete = false  // 0 != 10
getCompletionPercentage() = 0%
```

### During Collection
```
soilDataStorage: Map<LatLng, SoilData> {
    LatLng(9.3093, 123.308) → SoilData(N:50, P:30, K:20, pH:7.0, ...)
    LatLng(9.3095, 123.310) → SoilData(N:48, P:32, K:22, pH:6.8, ...)
    LatLng(9.3097, 123.312) → SoilData(N:52, P:28, K:18, pH:7.1, ...)
    // ... 3 more entries ...
}

totalDotsCount = 10
allDotsComplete = false  // 6 != 10
getCompletionPercentage() = 60%
```

### After All Data Saved
```
soilDataStorage: Map<LatLng, SoilData> {
    LatLng(9.3093, 123.308) → SoilData(N:50, P:30, K:20, pH:7.0, ...)
    LatLng(9.3095, 123.310) → SoilData(N:48, P:32, K:22, pH:6.8, ...)
    LatLng(9.3097, 123.312) → SoilData(N:52, P:28, K:18, pH:7.1, ...)
    LatLng(9.3099, 123.314) → SoilData(N:49, P:31, K:21, pH:6.9, ...)
    LatLng(9.3101, 123.316) → SoilData(N:51, P:29, K:19, pH:7.2, ...)
    LatLng(9.3103, 123.318) → SoilData(N:47, P:33, K:23, pH:6.7, ...)
    LatLng(9.3105, 123.320) → SoilData(N:53, P:27, K:17, pH:7.3, ...)
    LatLng(9.3107, 123.322) → SoilData(N:46, P:34, K:24, pH:6.6, ...)
    LatLng(9.3109, 123.324) → SoilData(N:54, P:26, K:16, pH:7.4, ...)
    LatLng(9.3111, 123.326) → SoilData(N:45, P:35, K:25, pH:6.5, ...)
}

totalDotsCount = 10
allDotsComplete = true   // 10 == 10 ✓
getCompletionPercentage() = 100%

→ BUTTON APPEARS ✅
```

## Color Scheme

```
Unsaved Dots:     🔵 Blue (#2196F3)
Saved Dots:       🟢 Green (#4CAF50)
Land Area:        🔴 Red (#F44336)
Button:           🔵 Blue (#2196F3)
Button Text:      ⚪ White
Completion Text:  ⚪ White
Map Background:   ⚫ Dark/Satellite
```

## Timeline Example: 10 Dots, 3 Samples Per Minute

```
Timeline:          Action                    State
─────────────────────────────────────────────────────
0:00               Start collection
                   Dots generated = 10
                   totalDotsCount = 10
                   saved = 0
                   Button: HIDDEN ❌

0:20               Save dot 1 → Green
                   saved = 1 / 10
                   Completion: 10%
                   Button: HIDDEN ❌

0:40               Save dot 2 → Green
                   saved = 2 / 10
                   Completion: 20%
                   Button: HIDDEN ❌

1:00               Save dot 3 → Green
                   saved = 3 / 10
                   Completion: 30%
                   Button: HIDDEN ❌

1:20               Save dot 4 → Green
                   saved = 4 / 10
                   Completion: 40%
                   Button: HIDDEN ❌

1:40               Save dot 5 → Green
                   saved = 5 / 10
                   Completion: 50%
                   Button: HIDDEN ❌

2:00               Save dot 6 → Green
                   saved = 6 / 10
                   Completion: 60%
                   Button: HIDDEN ❌

2:20               Save dot 7 → Green
                   saved = 7 / 10
                   Completion: 70%
                   Button: HIDDEN ❌

2:40               Save dot 8 → Green
                   saved = 8 / 10
                   Completion: 80%
                   Button: HIDDEN ❌

3:00               Save dot 9 → Green
                   saved = 9 / 10
                   Completion: 90%
                   Button: HIDDEN ❌

3:20               Save dot 10 → Green
                   saved = 10 / 10
                   Completion: 100%
                   Button: VISIBLE ✅
                   
                   "All 10 sampling points
                    have been collected"

3:20 - 3:30       User taps button
                   → TODO: Recommendation logic
```

## Troubleshooting Visual Guide

```
ISSUE: Button never appears
┌─────────────────────────────────┐
│ Check List:                     │
├─────────────────────────────────┤
│ □ Dots being generated?         │
│   └─ Watch dots count in logs   │
│ □ LaunchedEffect triggered?     │
│   └─ Verify setTotalDotsCount() │
│ □ Data being saved?             │
│   └─ Check soilDataStorage size │
│ □ Completion logic correct?     │
│   └─ totalDotsCount > 0 ✓       │
│   └─ storage.size == total ✓    │
└─────────────────────────────────┘

ISSUE: Button appears, then disappears
┌─────────────────────────────────┐
│ Likely Cause:                   │
├─────────────────────────────────┤
│ Data is being deleted or        │
│ soilDataStorage is being cleared│
│                                 │
│ Check for calls to:             │
│ • deleteSoilData()              │
│ • clearAllData()                │
└─────────────────────────────────┘

ISSUE: Recomposes too frequently
┌─────────────────────────────────┐
│ NOT using derivedStateOf:       │
├─────────────────────────────────┤
│ ❌ var complete = remember {    │
│      mutableStateOf(...)        │
│    }                            │
│                                 │
│ ✅ val complete by             │
│      derivedStateOf { ... }     │
└─────────────────────────────────┘
```

---

## Quick Reference Checklist

- [ ] Dots appear on map
- [ ] Blue dots = unsaved
- [ ] Green dots = saved
- [ ] Button hidden when < 100%
- [ ] Button appears when 100%
- [ ] Button shows correct count
- [ ] Button can be tapped
- [ ] Button disappears if data deleted
- [ ] Logcat shows debug message
- [ ] No recomposition spam


