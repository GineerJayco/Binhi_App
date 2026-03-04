# Crop Placement Visual Guide - Center Grid Approach

## How the New Algorithm Works

### Step 1: Define Polygon Bounds
```
                    maxLat
                      ↑
         ┌────────────────────────┐
         │                        │
minLng ←─┤    Red Polygon Box     ├─→ maxLng
         │                        │
         └────────────────────────┘
                      ↓
                   minLat

Bounds:
- minLat = 9.3090, maxLat = 9.3110
- minLng = 123.3075, maxLng = 123.3095
- Width = 0.0020 degrees, Height = 0.0020 degrees
```

### Step 2: Calculate Grid Dimensions
```
Input: 4 crops

cropsPerRow = sqrt(4) = 2
numRows = ceiling(4/2) = 2

Grid: 2 columns × 2 rows
```

### Step 3: Divide Space into Cells
```
         ┌──────────────┬──────────────┐
         │   Cell 0,0   │   Cell 0,1   │
         │              │              │
         ├──────────────┼──────────────┤
         │   Cell 1,0   │   Cell 1,1   │
         │              │              │
         └──────────────┴──────────────┘

effectiveColSpacing = width / cropsPerRow = 0.0020 / 2 = 0.0010
effectiveRowSpacing = height / numRows = 0.0020 / 2 = 0.0010
```

### Step 4: Place Crops at Cell Centers
```
Formula for cell center:
cellCenterLat = minLat + (row + 0.5) * effectiveRowSpacing
cellCenterLng = minLng + (col + 0.5) * effectiveColSpacing

Crop Positions:
─────────────────────────────────────────────────

Row 0, Col 0:
cellCenterLat = 9.3090 + (0 + 0.5) * 0.0010 = 9.3090 + 0.0005 = 9.30905
cellCenterLng = 123.3075 + (0 + 0.5) * 0.0010 = 123.3075 + 0.0005 = 123.30755
Position: (9.30905, 123.30755) ← 🍌

Row 0, Col 1:
cellCenterLat = 9.3090 + (0 + 0.5) * 0.0010 = 9.30905
cellCenterLng = 123.3075 + (1 + 0.5) * 0.0010 = 123.3075 + 0.0015 = 123.30855
Position: (9.30905, 123.30855) ← 🍌

Row 1, Col 0:
cellCenterLat = 9.3090 + (1 + 0.5) * 0.0010 = 9.3090 + 0.0015 = 9.31015
cellCenterLng = 123.3075 + (0 + 0.5) * 0.0010 = 123.30755
Position: (9.31015, 123.30755) ← 🍌

Row 1, Col 1:
cellCenterLat = 9.3090 + (1 + 0.5) * 0.0010 = 9.31015
cellCenterLng = 123.3075 + (1 + 0.5) * 0.0010 = 123.30855
Position: (9.31015, 123.30855) ← 🍌
```

### Step 5: Verify Points Inside Polygon
```
For each position:
if (isPointInsidePolygon(position, polygonPoints)) {
    positions.add(position)
    cropsPlaced++
}

All 4 crops are inside the polygon ✅
```

## Visual Result

### 4 Bananas in 100 sqm (10m × 10m)
```
         ┌────────────┬────────────┐
         │            │            │
         │     🍌      │     🍌      │
         │ (5m, 5m)   │ (5m, 15m)  │
         │            │            │
         ├────────────┼────────────┤
         │            │            │
         │     🍌      │     🍌      │
         │ (15m, 5m)  │ (15m, 15m) │
         │            │            │
         └────────────┴────────────┘

Perfect 2×2 grid with icons at centers!
```

### 6 Crops in Different Arrangements
```
With 6 crops:
cropsPerRow = sqrt(6) = 2
numRows = ceiling(6/2) = 3

Grid: 2 columns × 3 rows

         ┌─────────────┬─────────────┐
         │      🌾      │      🌾      │ Row 0
         │   (cell center)          │
         ├─────────────┼─────────────┤
         │      🌾      │      🌾      │ Row 1
         │              │              │
         ├─────────────┼─────────────┤
         │      🌾      │      🌾      │ Row 2
         │              │              │
         └─────────────┴─────────────┘

All 6 crops positioned at grid cell centers ✅
```

### 9 Crops (Perfect Square)
```
With 9 crops:
cropsPerRow = sqrt(9) = 3
numRows = ceiling(9/3) = 3

Grid: 3 columns × 3 rows

         ┌─────┬─────┬─────┐
         │ 🌾  │ 🌾  │ 🌾  │
         ├─────┼─────┼─────┤
         │ 🌾  │ 🌾  │ 🌾  │
         ├─────┼─────┼─────┤
         │ 🌾  │ 🌾  │ 🌾  │
         └─────┴─────┴─────┘

Perfect 3×3 grid!
```

## Comparison: Old vs New

### Old Approach (While-Loop)
```
     Start
       ↓
   Set initial position
       ↓
   While loop iteration
       ↓
   Check boundaries
       ↓
   May miss center alignment
       ↓
   Increment with spacing
       ↓
   Continue...
    
Result: Approximate positioning
```

### New Approach (Grid-Based)
```
     Start
       ↓
   Calculate grid dimensions
       ↓
   For each grid cell
       ↓
   Calculate exact center
       ↓
   Verify inside polygon
       ↓
   Add position
       ↓
   Continue to next cell
    
Result: Perfect centered positioning
```

## Key Advantages

### 1. Exact Centering
```
Old: position ≈ cell_center (approximate)
New: position = cell_center (exact with 0.5 offset)
```

### 2. Predictable Layout
```
Old: Layout depends on spacing values and boundaries
New: Layout depends only on requested quantity and polygon size
```

### 3. Uniform Distribution
```
Old: May have uneven spacing due to boundary constraints
New: Equal spacing across all grid cells
```

### 4. Cleaner Code
```
Old: Complex while-loops with multiple conditions
New: Simple for-loops with basic arithmetic
```

## Implementation Details

### Grid Calculation
```kotlin
// Determine square-ish grid
cropsPerRow = sqrt(estimatedQuantity).toInt()
numRows = ceiling(estimatedQuantity / cropsPerRow)

// This ensures:
// - Square grids (like 2×2, 3×3) for perfect counts
// - Rectangular grids (like 2×3, 3×4) for non-square counts
// - Minimal empty cells
```

### Cell Center Formula
```kotlin
// The 0.5 offset puts the point at the center of each cell
cellCenterLat = minLat + (row + 0.5) * spacing
cellCenterLng = minLng + (col + 0.5) * spacing

// Example with 2×2 grid:
// Row 0: positions at row_start + 0.5 × spacing (25% into grid)
// Row 1: positions at row_start + 1.5 × spacing (75% into grid)
// Same for columns
```

### Boundary Checking
```kotlin
// Ensure crop is actually inside the polygon
if (isPointInsidePolygon(position, polygonPoints)) {
    positions.add(position)
}

// This handles irregular polygon shapes where some grid
// positions might fall outside the bounds
```

## Real-World Example

### Scenario: 4 Bananas Planted
```
Input:
- Crop: Banana
- Quantity: 4
- Land Area: 100 sqm (10m × 10m)
- Banana Area Per Plant: 25 sqm

Calculation:
- Grid: 2 × 2
- Each cell: 5m × 5m
- Each banana area: 25 sqm

Result in Map View:
         ┌────────────┬────────────┐
         │            │            │
         │     🍌      │     🍌      │  50m North
         │            │            │
         ├────────────┼────────────┤
         │            │            │
         │     🍌      │     🍌      │  50m South
         │            │            │
         └────────────┴────────────┘

   50m West       50m East

Each icon represents the center of a 5m × 5m cell
All 4 bananas fit perfectly in the 100 sqm area ✅
```

## Testing the Centered Grid Placement

To verify the new algorithm is working:

1. **Check Visual Alignment**
   - Icons should be in a regular grid pattern
   - Spacing between icons should be equal
   - No icons at edges of polygon (they're in centers)

2. **Check Quantity**
   - Count the icons
   - Should match the input quantity exactly

3. **Check Distribution**
   - Horizontal spacing should be uniform
   - Vertical spacing should be uniform
   - Grid should be square-ish (close to square dimensions)

4. **Test Different Quantities**
   - 1 crop: 1 icon at center
   - 4 crops: 2×2 grid
   - 9 crops: 3×3 grid
   - 6 crops: 2×3 grid
   - 16 crops: 4×4 grid

All should produce uniform, centered grids! ✅

## Conclusion

The new grid-based centered placement algorithm provides:
- ✅ Exact center positioning for each crop
- ✅ Predictable, uniform distribution
- ✅ Cleaner, simpler code
- ✅ Better visual representation of actual planting layout

