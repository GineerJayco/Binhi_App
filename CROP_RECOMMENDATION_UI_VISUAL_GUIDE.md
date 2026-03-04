# Crop Recommendation UI - Visual Design Guide

## Overall Layout Structure

```
┌─────────────────────────────────────────┐
│  Top App Bar (Blue #2196F3)             │ ← Header with back button
├─────────────────────────────────────────┤
│  [Loading / Error / Results / Empty]    │
│                                         │
│  Main Content Area (Light Gray)         │ ← Scrollable content
│  - Summary Card                         │
│  - Results List (LazyColumn)            │
│                                         │
└─────────────────────────────────────────┘
```

## Color Palette

### Primary Colors
| Color | Hex | Usage |
|-------|-----|-------|
| Blue | `#2196F3` | Top app bar, accent text |
| Light Gray | `#F5F5F5` | Background |
| White | `#FFFFFF` | Cards, panels |
| Dark Gray | `#757575` | Secondary text |
| Green | `#4CAF50` | Statistics, success |
| Red | `#FF5722` | Errors |

### Crop-Specific Colors
| Crop | Color | Hex | Notes |
|------|-------|-----|-------|
| Banana | Gold | `#FFD700` | Warm, tropical feel |
| Cassava | Tan | `#D2B48C` | Earthy, natural |
| Sweet Potato | Salmon | `#FF8C69` | Warm, inviting |
| Corn | Gold | `#FFD700` | Matches Banana |
| Mango | Tomato Red | `#FF6347` | Bold, vibrant |

## State Screens

### 1. Loading State
```
┌─────────────────────────────────────┐
│         🔄 CIRCULAR SPINNER         │
│                                     │
│     Analyzing Soil Data...          │
│                                     │
│    Running ML model inference       │
└─────────────────────────────────────┘
```

### 2. Error State
```
┌─────────────────────────────────────┐
│             ❌ ERROR ICON            │
│                                     │
│   Error Loading Recommendations     │
│                                     │
│   [Error message text here]         │
│                                     │
│         [Try Again Button]          │
└─────────────────────────────────────┘
```

### 3. Empty State
```
┌─────────────────────────────────────┐
│             ℹ️ INFO ICON             │
│                                     │
│   No Recommendations Available      │
│                                     │
│    Please collect soil samples      │
│            first                    │
└─────────────────────────────────────┘
```

### 4. Results State
```
┌─────────────────────────────────────┐
│     TOP RECOMMENDATION CARD          │
│  ┌─────────────────────────────────┐ │
│  │                                 │ │
│  │        🍌 (48sp emoji)          │ │
│  │        Banana                   │ │
│  │       85% Match                 │ │
│  │  (Excellent match - highly      │ │
│  │   recommended)                  │ │
│  │                                 │ │
│  │  5 Options   |  89% Avg Conf.   │ │
│  └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│    All Recommendations              │
├─────────────────────────────────────┤
│  ┌─────────────────────────────────┐│
│  │  🍌  Banana          85%        ││
│  │      ████████░░░░░░░░░░░░░     ││
│  │      ✓ Excellent match...      ││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │  🌳  Cassava          72%       ││
│  │      ██████░░░░░░░░░░░░░░░░   ││
│  │      ✓ Good match...           ││
│  └─────────────────────────────────┘│
│  [More cards below...]             │
└─────────────────────────────────────┘
```

## Component Details

### Top App Bar
```
Height: 56.dp (Material standard)
Background: Color(0xFF2196F3)
Text Color: White
Font Weight: Bold
Title: "Crop Recommendation"
Navigation Icon: Back arrow (left side)

┌──────────────────────────────────────┐
│ ◄  Crop Recommendation               │
└──────────────────────────────────────┘
```

### Summary Card (Top Recommendation)
```
Width: Full - 32.dp margin (16.dp each side)
Background: White
Elevation: 4.dp
Border Radius: 12.dp

┌────────────────────────────────────┐
│  Top Recommendation (gray text)     │
│                                    │
│           🍌 (48sp)                │
│           Banana                   │
│          85% Match (blue)          │
│                                    │
│  Excellent match - highly          │
│  recommended (gray)                │
│                                    │
│  ─────────────────────────────────│
│                                    │
│  5 Options (blue)    89% Avg (green)
│   Options            Confidence    │
└────────────────────────────────────┘

Spacing:
- Top padding: 16.dp
- Bottom padding: 16.dp
- Left/Right padding: 16.dp
- Icon size: 48.sp
- Headline text: headlineSmall
- Body text: bodySmall
- Title text: titleMedium
```

### Crop Prediction Card
```
Width: Full - 32.dp margin
Height: Wrap content
Background: White
Elevation: 2.dp
Border Radius: 12.dp

┌────────────────────────────────────┐
│ ┌────┐                             │
│ │ 🍌 │  Banana              85%   │
│ │ (Gold)                         │
│ │ 56dp │  ████████░░░░░░░░░░     │
│ └────┘                             │
│      ✓ Excellent match -          │
│        highly recommend...        │
│                                   │
└────────────────────────────────────┘

Layout Details:
- Icon Box: 56.dp × 56.dp
- Border Radius: 12.dp
- Background Alpha: 20% (0.2f)
- Spacing between icon & details: 12.dp
- Title Font: titleMedium, Bold
- Percentage Font: bodySmall, Bold, 16.sp
- Progress Bar Height: 6.dp
- Progress Bar Radius: 3.dp
- Reasoning Row Background Alpha: 10% (0.1f)
- Reasoning Row Padding: 8.dp
```

### Progress Bar
```
Full Width - Percentage
Height: 6.dp
Background: LightGray
Progress Height: 6.dp
Progress Gradient: Color → Color with 70% alpha
Border Radius: 3.dp

Example for 85% confidence:
████████░░░░░░░░░░░░░░░
0%     25%    50%    75%   100%
       ↑ Progress at 85%
```

### Reasoning Badge
```
Width: Full
Background: Crop color @ 10% alpha
Border Radius: 6.dp
Padding: 8.dp
Content: Icon (16.dp) + Text

┌─────────────────────────────────┐
│ ✓ Excellent match - highly      │
│   recommended                   │
└─────────────────────────────────┘
```

## Typography

### Text Styles Used

| Type | Material Style | Font Weight | Size |
|------|---|---|---|
| Header | headlineSmall | Bold | 28.sp |
| Card Title | headlineSmall | Bold | 28.sp |
| Section Title | titleMedium | Bold | 16.sp |
| Crop Name | titleMedium | Bold | 16.sp |
| Percentage | bodySmall | Bold | 16.sp |
| Body Text | bodyMedium | Normal | 14.sp |
| Small Text | bodySmall | Normal | 12.sp |
| Label | labelSmall | Normal | 10.sp |
| Reasoning | labelSmall | Normal | 10.sp |

All use default Material 3 typography definitions.

## Spacing Guide

### Standard Measurements
```
Base Unit: 4.dp

- Horizontal margins: 16.dp (4×)
- Vertical spacing: 8.dp (2×)
- Card spacing: 12.dp (3×)
- Component padding: 16.dp (4×)
- Icon size: 56.dp (14×)
- Progress bar: 6.dp
- Emoji size: 32-48sp
```

### Card Spacing
```
Screen Edge
    ↓
16.dp margin
    ↓
Card Content
    ↓
16.dp padding inside card
```

## Animations

### Loading State
- Circular Progress: Rotating animation (built-in)
- Opacity: Fade in/out on state changes
- Type: `AnimatedVisibility` with `fadeIn()` and `fadeOut()`

### Transitions
- State changes: Smooth fade animations
- No scroll animations (scrolling is native)
- No ripple effects on cards (but available on buttons)

## Responsive Design

### Screen Size Handling
```
Small (< 400dp width):
- Single column layout
- Full-width cards
- Readable at all sizes

Medium (400-600dp):
- Full-width cards work well
- LazyColumn handles scrolling

Large (> 600dp):
- Cards still full-width (good for readability)
- Could add side panels in future
```

## Icon Specifications

### Crop Icons (Emoji)
- Size: 32.sp (in icon boxes), 48.sp (top recommendation)
- Font: System emoji font
- Color: Full color (no tinting)

### Material Icons
- Size: 64.dp (error/info screens), 48.dp (dialog), 16.dp (reasoning)
- Color: Theme-specific (Red for error, Gray for info, Color for checkmark)
- Weight: Regular (no bold)

## Button Styles

### Primary Button
```
Background: Color(0xFF2196F3)
Text Color: White
Height: 44.dp (if in MappingInfo)
Font Weight: Bold
Corner Radius: 4.dp (default)
```

### Secondary Button
```
Background: Color(0xFF4CAF50)
Text Color: White
Height: 44.dp
Font Weight: Bold
```

## Dialog Layout

### Error Dialog
```
Width: 85% of screen
Background: White
Elevation: 8.dp

┌─────────────────────────────┐
│      ❌ ERROR ICON (48.dp) │
│                             │
│        Error                │
│     (headlineSmall, bold)   │
│                             │
│    [Error message]          │
│    (bodyMedium)             │
│                             │
│    [Go Back Button]         │
│    (fill width)             │
└─────────────────────────────┘
```

## Dark Mode Considerations

Current colors should work in dark mode with these adjustments (future):
- Background: `#F5F5F5` → `#121212`
- Card: `#FFFFFF` → `#1E1E1E`
- Text: `#000000` → `#FFFFFF`
- Gray Text: `#757575` → `#BDBDBD`

## Accessibility Features

- ✅ Sufficient color contrast (WCAG AA)
- ✅ Icons have contentDescription
- ✅ Text sizing is readable
- ✅ Touch targets are at least 48.dp
- ✅ Color is not the only differentiator (icons + text)
- ✅ Buttons are properly labeled

## Visual Hierarchy

1. **Top Level**: App bar - user knows where they are
2. **Primary**: Top recommendation card - what matters most
3. **Secondary**: Summary stats - quick metrics
4. **Tertiary**: Individual crop cards - detailed options
5. **Supporting**: Colors, icons, progress bars - visual interest

## Design System Compliance

✅ Material 3 Design System
✅ Jetpack Compose component library
✅ Standard elevation system
✅ Color contrast accessibility
✅ Standard typography scale
✅ Rounded corners (12dp for major, 3dp for minor)

---

**Last Updated**: February 18, 2026  
**Design System**: Material 3  
**Framework**: Jetpack Compose

