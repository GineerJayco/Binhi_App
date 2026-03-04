# Saved Session Screenshot Feature

## Overview
You now have the ability to save a **complete snapshot** of your GetSoilData session - including all UI elements, map state, data points, and configurations. When you load a saved session, you'll see a live, interactive map view of exactly what you had when you saved it.

## What Gets Saved
When you click the "Save Session" button on GetSoilData.kt, the following information is captured:

### Map & UI State
- **Camera Position**: Exact zoom level where you saved
- **Polygon Center**: The center location of your field
- **Rotation**: Current rotation angle of your field layout
- **Map Type**: Whether you were using SATELLITE or NORMAL map view
- **All Dots**: The exact positions of all sampling points

### Session Data
- **Crop Information**: The crop type you selected
- **Land Area**: Total area in square meters
- **Field Dimensions**: Length and width in meters
- **Soil Data Points**: All collected sensor data for each location
- **Timestamp**: Date and time of when the session was saved

## How to Use

### Saving a Session
1. Click the **"Save Session"** button (orange button) in GetSoilData
2. Enter a descriptive session name (e.g., "Rice Field - North Section")
3. Click **"Save Session"** - all your data and UI state is captured

### Viewing Saved Sessions
1. Navigate to **"Saved Data"** from the main menu
2. You'll see a list of all your saved sessions
3. Click on any session to see an **interactive map view**

### Exploring a Saved Session
When you click on a saved session, you'll see:

- **Interactive Map View**: The exact same map with:
  - Your field polygon (red outline)
  - All sampling points (green markers)
  - Same camera zoom level as when saved
  - Same map type (Satellite or Normal)
  
- **Session Information Panel** (at bottom):
  - Crop name
  - Land area
  - Total dots and data points collected
  - Rotation angle
  - Map type
  - Camera zoom level
  - Save date/time
  - Completion progress bar

### Interacting with Saved Data

#### View Soil Data for a Specific Location
1. Click on any green marker on the map
2. A dialog opens showing:
   - Exact latitude/longitude coordinates
   - All stored soil measurements:
     - Nitrogen level
     - Phosphorus level
     - Potassium level
     - pH Level
     - Temperature
     - Moisture percentage

#### Rotate the Map
Use the standard Google Maps gestures to:
- Pinch to zoom in/out
- Rotate by using two-finger rotation gesture
- Pan by dragging

#### Switch Map Types
The map displays in the same type as when saved (Satellite or Normal view)

### Going Back
Click the **Back arrow** button to return to the session list at any time.

## Technical Details

### Data Storage Structure
Each saved session contains:
```
SavedSession {
  id: UUID
  sessionName: String
  landArea: Double
  length: Double
  width: Double
  crop: String
  polygonCenter: Pair<Lat, Lon>
  rotation: Float
  mapType: String (SATELLITE/NORMAL)
  cameraZoom: Float ← NEW! Captures exact zoom level
  totalDots: Int
  soilDataPoints: Map<Coordinates, SoilData>
  timestamp: Long
}
```

### What This Means
- **Complete Visual Restoration**: When you open a saved session, you see the exact same view as when you saved it
- **No Data Loss**: Every single data point and setting is preserved
- **Live & Interactive**: It's not just a screenshot - it's a fully interactive map
- **Easy Navigation**: Seamlessly switch between sessions and explore your data

## Example Workflow

1. **Day 1**: You map a rice field, place 100 dots, collect 50 samples
   - Click "Save Session"
   - Name it: "Rice Field - North - Day 1"
   - All 50 data points are saved with their exact positions

2. **Day 2**: You open SavedData, click on your saved session
   - You see the exact same field layout
   - Same map zoom, same rotation, same field polygon
   - All 50 green markers showing where you collected data
   - You can click any marker to review the soil measurements

3. **Later**: You want to add more data to the same field
   - You could use this as a reference
   - Or export this data for analysis
   - Or compare it with new samples from a different day

## Benefits

✅ **Complete State Preservation**: Map view, camera zoom, rotation - everything is saved
✅ **Visual Reference**: See exactly where you collected each sample
✅ **Data Exploration**: Click on any dot to view the complete soil measurements
✅ **Comparison Ready**: Save multiple sessions to compare different fields or dates
✅ **No Manual Documentation**: Automatic visual snapshot of your entire session
✅ **Mobile-Friendly**: Works seamlessly on tablets and phones for field work

## Future Enhancements (Potential)

- Export sessions as PDF with map and data
- Compare two sessions side-by-side
- Annotate saved sessions with notes
- Share sessions with other users
- Continue data collection from a saved session
- Generate reports from saved sessions

