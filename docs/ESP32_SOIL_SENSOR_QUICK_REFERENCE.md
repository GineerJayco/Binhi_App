# ESP32 Soil Sensor UI - Quick Reference Card

## Pin Configuration at a Glance

### LCD Display Pins
```
VCC    -> VIN      GND  -> GND
CS     -> GPIO 15  RST  -> GPIO 4
DC     -> GPIO 2   LED  -> 3.3V
MOSI   -> GPIO 23  SCK  -> GPIO 18
MISO   -> GPIO 19
```

### Touch Controller Pins
```
CLK    -> GPIO 18  (shared with LCD SCK)
CS     -> GPIO 5
DIN    -> GPIO 23  (shared with LCD MOSI)
DO     -> GPIO 19  (shared with LCD MISO)
GND    -> GND      VCC  -> 3.3V
```

---

## Display Information

**Resolution:** 240x320 pixels (portrait)
**Colors:** RGB565 format
**Rotation:** Portrait (0 degrees)

### Display Layout
```
┌──────────────────────────────────┐
│          SOIL DATA               │  (Cyan text)
├──────────────────────────────────┤
│                                  │
│  N: 12.0 mg/kg                  │  (White/Yellow)
│  P: 7.0 mg/kg                   │
│  K: 9.0 mg/kg                   │
│  pH: 6.5                         │
│  TEMP: 29.4C                     │
│  MOIST: 62%                      │
│                                  │
│                                  │
│  ┌──────────────────┐            │
│  │   SAVE DATA      │ ← Button    │
│  └──────────────────┘            │
└──────────────────────────────────┘

Button Location: X: 40-200, Y: 280-320
Button Color: Dark Green (normal), Light Green (pressed)
Ready Text: "READY TO SEND" (when dataReadyToSend = true)
```

---

## Bluetooth Communication Protocol

### Device Details
- **Name:** ESP32_SOIL_SENSOR
- **Protocol:** Bluetooth Classic (SPP)
- **Baud Rate:** 115200 (internally handled)

### Message Format

**Android sends:**
```
READ\n
```

**ESP32 responds (if button was pressed):**
```
NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62\n
```

**ESP32 responds (if button was NOT pressed):**
```
NO_DATA\n
```

### Response Format Breakdown
```
NPK=12,7,9          ← Nitrogen, Phosphorus, Potassium (no decimals)
PH=6.5              ← pH Level (1 decimal)
TEMP=29.4           ← Temperature °C (1 decimal)
MOIST=62            ← Moisture % (no decimals)
```

---

## Bluetooth Pairing Steps

### Android Device:
1. Settings → Bluetooth → ON
2. Scan for devices
3. Select "ESP32_SOIL_SENSOR"
4. Confirm pairing (no PIN needed)

### Terminal App:
1. Download "Serial Bluetooth Terminal" from Play Store
2. Open app → Select "ESP32_SOIL_SENSOR"
3. Send: `READ` and press Enter
4. View response

---

## Key Functions Reference

### Display Functions
```cpp
drawUI()                    // Redraw complete UI
drawTitle()                 // Draw "SOIL DATA" title
drawSoilData()             // Draw all sensor values
drawSaveButton(bool)       // Draw button (normal/highlighted)
drawReadyToSendText()      // Draw "READY TO SEND" message
```

### Data Functions
```cpp
updateSoilData(n,p,k,ph,temp,moisture)  // Update display with new values
getSoilData()              // Get current soil data struct
```

### Bluetooth Functions
```cpp
handleBluetooth()          // Check and process incoming BT messages
handleReadCommand()        // Process "READ" command
sendSoilData()             // Send data via Bluetooth
```

### Touch Functions
```cpp
handleTouchInput()         // Check for screen touches
isTouchOnButton(x,y)      // Check if touch is on button
handleButtonPress()        // Process button press
```

---

## Color Codes (RGB565)

| Color       | Hex Value | Usage |
|------------|-----------|-------|
| BLACK      | 0x0000    | Background |
| WHITE      | 0xFFFF    | Text, borders |
| CYAN       | 0x07FF    | Title |
| YELLOW     | 0xFFE0    | Data values |
| GREEN      | 0x07E0    | Button highlight |
| DARK_GREEN | 0x0340    | Button normal |
| RED        | 0xF800    | Errors |
| BLUE       | 0x001F    | Alternative accent |
| GRAY       | 0xC618    | Secondary text |

---

## Common Modifications

### Change Button Position
```cpp
#define BUTTON_X 40        // Left edge (0-240)
#define BUTTON_Y 280       // Top edge (0-320)
#define BUTTON_WIDTH 160   // Button width
#define BUTTON_HEIGHT 40   // Button height
```

### Change Bluetooth Name
```cpp
SerialBT.begin("YOUR_CUSTOM_NAME")
```

### Change Text Size
```cpp
tft.setTextSize(2)  // 1=small, 2=medium, 3=large
```

### Change Colors
```cpp
tft.setTextColor(YELLOW, BLACK)  // Text color, Background color
```

### Update Soil Data
```cpp
updateSoilData(15.5, 8.2, 10.1, 6.8, 28.5, 65.0);
//              N    P    K     pH   TEMP  MOIST
```

---

## Debugging Tips

### Serial Monitor Setup
- **Baud Rate:** 115200
- **Shortcut:** Ctrl+Shift+M

### Check Touch Coordinates
Touch screen prints coordinates to Serial Monitor:
```
Touch detected: X=120, Y=300
```
Use this to verify button area is correct.

### Verify Button Press
When button is pressed, you'll see:
```
Button pressed! Data ready to send.
```

### Monitor Bluetooth Messages
```
BT Message received: READ
Processing READ command...
Sent soil data: NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

---

## Global Variables

| Variable | Type | Default | Purpose |
|----------|------|---------|---------|
| soilData | struct | See default values | Stores soil sensor readings |
| dataReadyToSend | bool | false | Flag for data transmission |
| buttonPressed | bool | false | Track button press state |
| buttonPressTime | unsigned long | 0 | Timestamp of button press |

---

## Timing Constants

```cpp
BUTTON_HIGHLIGHT_DURATION = 500  // How long button stays highlighted (ms)
```

---

## Important Notes

⚠️ **Touch Calibration**: The touch coordinates must be calibrated for your specific screen. Default values may not work. See Setup Guide section 5.

⚠️ **SPI Bus**: LCD and Touch share SCK, MOSI, MISO lines. Each has separate CS pin.

⚠️ **Voltage**: Ensure LCD module is 3.3V compatible. No 5V power!

⚠️ **Startup Time**: Display initialization takes ~2 seconds. Be patient after reset.

⚠️ **Buffer Size**: Ensure USB cable is high-quality for reliable uploads.

---

## Quick Test Commands

### Serial Monitor Commands (for testing):
```
Watch Serial Monitor at 115200 baud
Press RST on ESP32 to see startup sequence
Touch the button and watch for:
  - "Touch detected: X=..."
  - "Button pressed! Data ready to send."
```

### Bluetooth Commands (via Terminal App):
```
Send: READ
Expect: NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
        (if button was pressed)

Send: READ
Expect: NO_DATA
        (if button was NOT pressed)
```

---

## Error Messages & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| TFT display initialized (fails) | Display not connected | Check VCC, GND, CS, DC, MOSI, SCK, MISO pins |
| Touch screen initialized (fails) | Touch not connected | Check CS (GPIO 5) and shared SPI pins |
| Bluetooth initialized (fails) | Rare - ESP32 issue | Restart ESP32 or check USB connection |
| Touch not responding | Wrong calibration | Calibrate touch as per Setup Guide |
| Button doesn't work | Button area wrong | Check BUTTON_X, Y, WIDTH, HEIGHT values |
| Bluetooth not pairing | Not discoverable | Power cycle ESP32 and restart Bluetooth on phone |

---

## File Structure

```
ESP32_SOIL_SENSOR_UI.ino           ← Main sketch file
ESP32_SOIL_SENSOR_SETUP_GUIDE.md   ← Detailed setup instructions
ESP32_SOIL_SENSOR_QUICK_REFERENCE.md ← This file
ESP32_SOIL_SENSOR_TESTING_GUIDE.md ← Testing procedures
```

---

## Board Configuration (Tools Menu)

```
Board:           ESP32 Dev Module
Upload Speed:    921600
Flash Mode:      DIO
Flash Frequency: 40MHz
CPU Frequency:   240MHz
Core Debug Level: None
Partition Scheme: Default 4MB with spiffs
Port:            COMX (your port)
```

---

Version: 1.0
Last Updated: December 29, 2025

