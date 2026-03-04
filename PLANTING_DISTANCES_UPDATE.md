# Planting Distances Update

## Summary
Updated the application to use the final planting distances for all supported crops. The system now supports separate row and column spacing for more accurate crop positioning.

## Updated Planting Distances

### Crop Specifications
| Crop | Row Spacing (m) | Column Spacing (m) |
|------|-----------------|-------------------|
| Corn | 0.75 | 0.25 |
| Cassava | 1.0 | 0.5 |
| Sweet Potato | 1.0 | 0.5 |
| Banana | 5.0 | 5.0 |
| Mango | 10.0 | 10.0 |

## Changes Made

### 1. **CropData.kt**
- Updated `CropPlanting` data class to include separate `rowSpacing` and `columnSpacing` fields
- Kept `plantingDistance` field for backward compatibility
- Added secondary constructor for legacy code support
- Updated all crop entries in the `CropData.crops` map with new spacing values

**New Data Structure:**
```kotlin
data class CropPlanting(
    val name: String,
    val areaPerPlant: Double,
    val plantingDistance: Double,      // legacy, kept for compatibility
    val rowSpacing: Double,             // distance between rows
    val columnSpacing: Double,          // distance between plants in a row
    val iconResource: Int
)
```

### 2. **VisualizeCQ.kt**
- Updated `calculateCropPositions()` function to use separate `rowSpacing` and `columnSpacing`
- Changed latitude distance calculation to use `rowSpacing`
- Changed longitude distance calculation to use `columnSpacing`

**Updated Calculation:**
```kotlin
val rowSpacing = cropPlanting.rowSpacing
val columnSpacing = cropPlanting.columnSpacing

val latDistance = rowSpacing / 111111.0
val lngDistance = columnSpacing / (111111.0 * cos(Math.toRadians((minLat + maxLat) / 2)))
```

### 3. **VisualizeLA.kt**
- Updated `calculateCropPositions()` function with the same changes as VisualizeCQ.kt
- Now correctly calculates crop positions using row and column spacing

## Impact

### Feature Improvements
- **More Accurate Positioning**: Crops with different row and column spacing will now be positioned correctly
- **Better Visualization**: Maps will show more realistic crop distributions based on actual planting distances
- **Backward Compatibility**: Legacy code using `plantingDistance` still works through the secondary constructor

### Affected Areas
1. **Crop Quantity Visualization**: The maps in both land area and crop quantity visualizations will now show crops with proper spacing
2. **Estimated Quantity Calculation**: The calculation uses the area-per-plant metric, which is separate from spacing
3. **Marker Positioning**: Markers on the map will be distributed according to the new spacing values

## Testing Recommendations

1. **Verify Spacing Accuracy**
   - Open a land area with Corn (0.75m x 0.25m spacing)
   - Verify that plants are closer in column direction than row direction
   - Compare with Mango (5m x 5m) to see square spacing

2. **Test All Crops**
   - Corn: 0.25m x 0.75m
   - Cassava: 1m x 0.5m
   - Sweet Potato: 0.5m x 1m
   - Banana: 5m x 5m
   - Mango: 10m x 10m

3. **Verify Map Rendering**
   - Check that crop markers are distributed correctly on maps
   - Ensure no crashes or visual glitches when visualizing different crops
   - Verify estimated quantities are reasonable for the land area

## Files Modified

1. ✅ `CropData.kt`
2. ✅ `VisualizeCQ.kt`
3. ✅ `VisualizeLA.kt`

## Backward Compatibility

The secondary constructor in `CropPlanting` ensures that any existing code creating `CropPlanting` objects with just `(name, areaPerPlant, plantingDistance, iconResource)` will still work, setting both `rowSpacing` and `columnSpacing` to the `plantingDistance` value.

## Status
✅ **Implementation Complete**

Date: January 30, 2026

