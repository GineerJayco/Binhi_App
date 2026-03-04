# ESP32 Soil Sensor TFT - Quick Reference & Testing Guide

## Quick Start Checklist

### Before Uploading Code:
- [ ] Install all 4 required libraries (GFX, ILI9341, XPT2046, ESP32 board)
- [ ] Verify all hardware connections match pin configuration
- [ ] Select "ESP32 Dev Module" board in Arduino IDE
- [ ] Select correct COM port
- [ ] Close any other Serial Monitor windows

### After Uploading:
- [ ] Open Serial Monitor (115200 baud)
- [ ] See initialization messages
- [ ] Display shows "SOIL DATA" title
- [ ] Values are visible on screen

---

## Pin Quick Reference

```
ESP32 PIN    | Connected To         | Purpose
─────────────┼──────────────────────┼─────────────────
GPIO 2       | Display DC           | Data/Command
GPIO 4       | Display RST          | Reset
GPIO 5       | Touch CS             | Touch Chip Select
GPIO 15      | Display CS           | Display Chip Select
GPIO 18      | Display SCK + Touch CLK | SPI Clock
GPIO 19      | Display MISO + Touch DO | SPI Data In
GPIO 23      | Display MOSI + Touch DIN | SPI Data Out
VIN          | Display VCC          | Power
3.3V         | Display LED, Touch VCC | Power
GND          | All GND              | Ground
```

---

## Testing Procedures

### Test 1: Serial Communication
**Expected Output in Serial Monitor:**
```
=== ESP32 Soil Sensor Display ===
TFT Display initialized
Touch screen initialized
Bluetooth initialized: ESP32_SOIL_SENSOR
Setup complete - waiting for input...
```

**If you don't see this:**
- Check USB cable and COM port
- Try pressing ESP32 reset button
- Verify baud rate is 115200

---

### Test 2: Display Visibility
**What you should see on screen:**
```
SOIL DATA
─────────────────────
N (mg/kg):       12
P (mg/kg):       7
K (mg/kg):       9
pH Level:       6.5
Temp (°C):     29.4
Moisture (%):   62
  ┌────────────────┐
  │  SAVE DATA     │
  └────────────────┘
```

**If display is blank/garbled:**
1. Check power connections (VCC, GND)
2. Check CS pin (GPIO 15) - try different GPIO if needed
3. Check RST pin (GPIO 4) - verify connection
4. Try a shorter SPI cable
5. Reduce SPI clock speed if garbled

**If display is wrong color:**
- This is normal initially - it's a display mode/rotation issue
- Try changing rotation: `tft.setRotation(0)` to `tft.setRotation(1)` or `(3)`

---

### Test 3: Touch Functionality
**What to do:**
1. Press the "SAVE DATA" button on the screen
2. Button should highlight in green
3. "READY TO SEND" text should appear below button
4. Serial Monitor should print: "SAVE DATA button pressed!"

**If touch not working:**
1. Check Touch CS pin (GPIO 5) connection
2. Verify SPI connections for touch (GPIO 18, 23, 19)
3. Serial Monitor will print touch coordinates - verify they're within screen bounds
4. May need to recalibrate touch (see Troubleshooting)

**Touch Calibration Values:**
If coordinates are inverted or off, adjust these in code:
```cpp
#define TS_MINX 150    // Adjust if X coordinates are reversed
#define TS_MAXX 3900
#define TS_MINY 150    // Adjust if Y coordinates are reversed
#define TS_MAXY 3900
```

---

### Test 4: Bluetooth Communication
**On Android Device:**
1. Open Bluetooth settings, pair with "ESP32_SOIL_SENSOR"
2. Use a Bluetooth Serial app (like "Bluetooth Terminal" or "Serial Bluetooth Terminal")
3. Connect to the device
4. Send: `READ` (with newline)
5. Receive: `NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62`

**Expected Responses:**
```
Send:      "READ\n"
Response:  "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62\n"
           (if button was pressed)

Send:      "READ\n" 
Response:  "NO_DATA\n"
           (if button wasn't pressed)
```

**Serial Monitor will also show:**
```
Bluetooth received: READ
Data sent via Bluetooth
Soil data sent: NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

**If Bluetooth not working:**
1. Check Serial Monitor shows "Bluetooth initialized"
2. Phone Bluetooth might need restart
3. Try re-pairing device
4. Check your Bluetooth app has permission to read/write

---

## Code Modification Guide

### To Update Displayed Values:
Find in your code (or in loop/interrupt handler):
```cpp
updateSoilData(
  12.0,   // Nitrogen (change this)
  7.0,    // Phosphorus (change this)
  9.0,    // Potassium (change this)
  6.5,    // pH (change this)
  29.4,   // Temperature (change this)
  62.0    // Moisture (change this)
);
```

The display will automatically refresh with new values.

### To Change Button Position:
```cpp
#define SAVE_BUTTON_X 50      // Horizontal position
#define SAVE_BUTTON_Y 280     // Vertical position
#define SAVE_BUTTON_W 140     // Width
#define SAVE_BUTTON_H 30      // Height
```

### To Change Button Highlight Duration:
```cpp
const unsigned long BUTTON_HIGHLIGHT_DURATION = 300;  // milliseconds
```

### To Change Device Name:
```cpp
#define DEVICE_NAME "Your_Custom_Name"
```

---

## Common Issues & Solutions

### Issue: "TFT Display not found!"
**Solution:**
- Check TFT_CS (GPIO 15) and TFT_DC (GPIO 2) connections
- Verify voltage: display may need 5V on VCC instead of 3.3V
- Try without display first, verify SPI is working

### Issue: "Touch screen not found!"
**Solution:**
- Check TOUCH_CS (GPIO 5) connection
- Verify touch shares same SPI bus (GPIO 18, 23, 19)
- Try touching screen and checking Serial Monitor for coordinates

### Issue: Button press doesn't work
**Solution:**
- Serial Monitor will show touch coordinates when pressed
- If no coordinates printed, check Touch CS pin
- If coordinates shown but button doesn't respond, recalibrate TS_MIN/MAX values

### Issue: Bluetooth sends data multiple times
**Solution:**
- This happens if you send multiple "READ" commands
- Each "READ" only sends if dataReadyToSend is true
- Button must be pressed first to enable data sending

### Issue: Bluetooth connects but no response
**Solution:**
- Try sending command with explicit newline character
- In app, make sure you're sending "READ\n" not just "READ"
- Check Serial Monitor for "Bluetooth received:" message

---

## Data Format Reference

### Data Structure (in code):
```cpp
struct SoilData {
  float nitrogen;      // mg/kg (0-300 typical)
  float phosphorus;    // mg/kg (0-200 typical)
  float potassium;     // mg/kg (0-300 typical)
  float pH;            // 0-14 scale (optimal 5.5-8.0)
  float temperature;   // Celsius (-20 to 50°C typical)
  float moisture;      // Percentage (0-100%)
};
```

### Bluetooth Transmission Format:
```
NPK=N_value,P_value,K_value;PH=pH_value;TEMP=temp_value;MOIST=moisture_value

Example:
NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62

Breakdown:
- NPK section: Three comma-separated integers or floats
- PH section: Float with 1 decimal place
- TEMP section: Float with 1 decimal place
- MOIST section: Integer or float
```

---

## Performance Expectations

| Operation | Response Time | Notes |
|-----------|---------------|-------|
| Display Refresh | ~50ms | Non-blocking |
| Touch Detection | ~50ms | Polling interval |
| Bluetooth Receive | <100ms | After data sent |
| Button Highlight | 300ms | Automatic timeout |

---

## File Structure Overview

Your workspace now has:
```
soil_sensor_tft_display.ino          ← Main code file (UPLOAD THIS)
SOIL_SENSOR_TFT_SETUP_GUIDE.md       ← Detailed setup guide
SOIL_SENSOR_TFT_QUICK_REFERENCE.md   ← This file
```

---

## Next Steps for Integration

1. **Sensor Integration:**
   - Connect your soil sensors to ESP32 ADC pins
   - Read analog values in setup/loop
   - Call `updateSoilData()` with real sensor values

2. **Android App Integration:**
   - Implement Bluetooth Serial connection
   - Send "READ\n" command when needed
   - Parse received data string
   - Display NPK, pH, Temperature, Moisture values

3. **Data Logging:**
   - Consider SPIFFS/LittleFS for local storage
   - Log Bluetooth communication history
   - Add timestamp to data logs

---

## Useful Arduino Functions Reference

```cpp
// Update display with new values
updateSoilData(n, p, k, ph, temp, moist);

// Send data via Bluetooth (called automatically)
sendSoilData();

// Check if button is in touch area
bool inButton = isTouchInButton(x, y);

// Handle touch input
handleTouchInput();

// Handle Bluetooth commands
handleBluetoothInput();

// Serial debugging
Serial.println("Message");
SerialBT.println("Message");  // Send via Bluetooth
```

---

**Last Updated:** January 4, 2026
**Version:** 1.0
**Difficulty Level:** Intermediate

