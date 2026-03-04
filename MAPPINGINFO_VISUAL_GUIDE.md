# MappingInfo Screen - Visual Guide

## Screen Layout

```
┌─────────────────────────────────────────────┐
│  ← Soil Sample Mapping Data           [TOP] │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─ Data Collection Summary ────────────┐  │
│  │                                       │  │
│  │   Total Samples: 12    Completion: 100% │
│  │                                       │  │
│  │   Click on any sample to view...     │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  ┌─ Sample Location 1 ───────────────────┐ │
│  │ 📍 Sample Location                     │ │
│  │    09° 18' 35.4800" N                 │ │
│  │    123° 18' 28.8000" E                │ │
│  │    Decimal: 9.309867, 123.307999    │ │
│  │                                       │ │
│  │ Soil Parameters                       │ │
│  │ ┌──────────┐ ┌──────────┐ ┌────────┐ │ │
│  │ │Nitrogen │ │Phosph.  │ │Potassium│ │ │
│  │ │   85    │ │   45    │ │   120   │ │ │
│  │ │ mg/kg   │ │ mg/kg   │ │ mg/kg   │ │ │
│  │ └──────────┘ └──────────┘ └────────┘ │ │
│  │ ┌──────────┐ ┌──────────┐ ┌────────┐ │ │
│  │ │pH Level │ │Temperature│ │Moisture│ │ │
│  │ │  6.82   │ │   28.5   │ │   65   │ │ │
│  │ │   pH    │ │   °C     │ │   %    │ │ │
│  │ └──────────┘ └──────────┘ └────────┘ │ │
│  │                                       │ │
│  │ Collection Time                       │ │
│  │ Dec 28, 2024 10:45:32         [DELETE]│ │
│  └───────────────────────────────────────┘ │
│                                             │
│  ┌─ Sample Location 2 ───────────────────┐ │
│  │ [Similar layout with different data]  │ │
│  └───────────────────────────────────────┘ │
│                                             │
│  [More samples below, scroll to see]       │
│                                             │
└─────────────────────────────────────────────┘
```

## Empty State

```
┌─────────────────────────────────────────────┐
│  ← Soil Sample Mapping Data           [TOP] │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─ Data Collection Summary ────────────┐  │
│  │ Total Samples: 0       Completion: 0% │  │
│  │ No soil data collected yet...        │  │
│  └───────────────────────────────────────┘  │
│                                             │
│                   📍                        │
│                                             │
│            No Samples Collected             │
│                                             │
│     Start collecting soil samples in        │
│            GetSoilData                      │
│                                             │
└─────────────────────────────────────────────┘
```

## Delete Confirmation Dialog

```
┌──────────────────────────────────┐
│                                  │
│           🗑️                      │
│                                  │
│      Delete Sample?              │
│                                  │
│  This will permanently remove    │
│  this soil sample data.          │
│                                  │
│  [  Cancel  ]  [  Delete  ]     │
│                                  │
└──────────────────────────────────┘
```

## Color Scheme Reference

| Parameter | Color | Hex Code | Usage |
|-----------|-------|----------|-------|
| Nitrogen | Green | #4CAF50 | N parameter box background |
| Phosphorus | Amber | #FFC107 | P parameter box background |
| Potassium | Purple | #9C27B0 | K parameter box background |
| pH Level | Cyan | #00BCD4 | pH parameter box background |
| Temperature | Orange | #FF5722 | Temperature parameter box |
| Moisture | Light Blue | #03A9F4 | Moisture parameter box |
| Primary | Blue | #2196F3 | Header, location box |
| Error | Red | #F44336 | Delete button, error states |
| Background | Light Gray | #F5F5F5 | Screen background |
| Surface | White | #FFFFFF | Card backgrounds |

## Data Display Examples

### Example 1: Healthy Soil
```
Sample Location
Latitude: 09° 18' 35.4800" N
Longitude: 123° 18' 28.8000" E
Decimal: 9.309867, 123.307999

Soil Parameters:
- Nitrogen: 85 mg/kg (Optimal)
- Phosphorus: 45 mg/kg (Good)
- Potassium: 120 mg/kg (Good)
- pH Level: 6.82 (Neutral)
- Temperature: 28.5°C
- Moisture: 65%

Collection Time: Dec 28, 2024 10:45:32
```

### Example 2: Deficient Soil
```
Sample Location
Latitude: 09° 18' 40.2000" N
Longitude: 123° 18' 32.5000" E
Decimal: 9.310611, 123.308700

Soil Parameters:
- Nitrogen: 25 mg/kg (Low)
- Phosphorus: 15 mg/kg (Low)
- Potassium: 60 mg/kg (Low)
- pH Level: 7.25 (Slightly Alkaline)
- Temperature: 26.5°C
- Moisture: 45%

Collection Time: Dec 28, 2024 11:20:15
```

## DMS Coordinate Format Explanation

Example: `09° 18' 35.4800" N`

- `09°` = 9 Degrees
- `18'` = 18 Minutes (1 degree = 60 minutes)
- `35.4800"` = 35.48 Seconds (1 minute = 60 seconds)
- `N` = North (for latitude) or E (for longitude)

Cardinal Directions:
- Latitude: N (North) or S (South)
- Longitude: E (East) or W (West)

## Navigation Flow

```
GetSoilData
    ↓
[Click "Get Crop Recommendation"]
    ↓
MappingInfo (this screen)
    ↓
[View/Delete samples]
    ↓
[Click Back]
    ↓
Returns to GetSoilData
```

## User Interactions

### View Data
1. Navigate to MappingInfo from GetSoilData
2. Scroll through all collected samples
3. Review soil parameters in color-coded boxes
4. Check GPS coordinates in DMS format

### Delete Sample
1. Click DELETE button on sample card
2. Confirm deletion in dialog
3. Sample removed from view and storage
4. Summary statistics update automatically

### Return to Previous Screen
1. Click back arrow at top-left
2. Returns to GetSoilData screen
3. Data is preserved in ViewModel storage

## Performance Considerations

- LazyColumn used for efficient rendering of many samples
- Sorted locations (by latitude, then longitude) for consistent ordering
- Mutable state properly managed with Compose state
- Images loaded efficiently with Material Icons

