# ESP32 Soil Sensor TFT Display - Implementation Guide

## Overview
This document provides step-by-step instructions for setting up the ESP32 soil sensor display with touch-enabled TFT LCD and Bluetooth communication.

## Prerequisites
- ESP32 Development Board
- 3.2" ILI9341 TFT LCD Display (240x320 pixels)
- XPT2046 Touch Controller
- Required Libraries (install via Arduino IDE Library Manager)

## Library Installation

### 1. Required Libraries
Install these libraries via **Arduino IDE → Sketch → Include Library → Manage Libraries**:

```
1. Adafruit GFX Library
   - Search: "Adafruit GFX"
   - Author: Adafruit
   - Install latest version

2. Adafruit ILI9341
   - Search: "Adafruit ILI9341"
   - Author: Adafruit
   - Install latest version

3. XPT2046_Touchscreen
   - Search: "XPT2046_Touchscreen"
   - Author: Paul Stoffregen
   - Install latest version

4. BluetoothSerial
   - Included with ESP32 core (no separate install needed)
```

### 2. ESP32 Board Configuration
1. Go to **Arduino IDE → Preferences**
2. Add this URL to "Additional Board Manager URLs":
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
3. Go to **Tools → Board → Board Manager**
4. Search for "ESP32" and install "esp32 by Espressif Systems"

## Hardware Pin Configuration

### Display (ILI9341)
```
Display Pin    ESP32 Pin    Function
─────────────────────────────────────
VCC            VIN          Power (5V or 3.3V)
GND            GND          Ground
CS             GPIO 15      Chip Select
RST            GPIO 4       Reset
DC             GPIO 2       Data/Command
MOSI           GPIO 23      SPI Master Out Slave In
SCK            GPIO 18      SPI Clock
MISO           GPIO 19      SPI Master In Slave Out
LED            3.3V         Backlight (always on)
```

### Touch Controller (XPT2046)
```
Touch Pin      ESP32 Pin    Function
──────────────────────────────────────
CLK            GPIO 18      SPI Clock (shared)
CS             GPIO 5       Chip Select
DIN            GPIO 23      SPI Data (shared with Display)
DO             GPIO 19      SPI Data In (shared with Display)
VCC            3.3V         Power
GND            GND          Ground
```

**Note:** Touch and Display share the same SPI bus (GPIO 18, 23, 19) but have separate CS pins (GPIO 15 for display, GPIO 5 for touch).

## Software Configuration

### 1. Verify Pin Definitions
Open `soil_sensor_tft_display.ino` and check that pins match your wiring:

```cpp
// Display pins
#define TFT_CS   15
#define TFT_RST  4
#define TFT_DC   2
#define TFT_MOSI 23
#define TFT_SCK  18
#define TFT_MISO 19

// Touch pins
#define TOUCH_CS 5
#define TOUCH_IRQ 36  // Optional interrupt pin
```

### 2. Touch Calibration
The default calibration values are:
```cpp
#define TS_MINX 150
#define TS_MAXX 3900
#define TS_MINY 150
#define TS_MAXY 3900
```

**To calibrate:**
1. Uncomment the touch calibration code (if available)
2. Or manually test by:
   - Pressing the corners of the screen
   - Noting the printed values in Serial Monitor
   - Adjusting TS_MIN/MAX values accordingly

### 3. Bluetooth Device Name
Default name: `ESP32_SOIL_SENSOR`

To change:
```cpp
#define DEVICE_NAME "Your_Device_Name"
```

## Initial Setup Steps

### Step 1: Hardware Assembly
1. Connect all wires according to the pin configuration above
2. Double-check all connections, especially GND and VCC
3. Ensure no short circuits

### Step 2: Upload Code
1. Open `soil_sensor_tft_display.ino` in Arduino IDE
2. Select **Tools → Board → ESP32 Dev Module**
3. Select the correct **COM port**
4. Click **Upload**

### Step 3: Monitor Serial Output
1. Open **Tools → Serial Monitor**
2. Set baud rate to **115200**
3. You should see initialization messages:
   ```
   === ESP32 Soil Sensor Display ===
   TFT Display initialized
   Touch screen initialized
   Bluetooth initialized: ESP32_SOIL_SENSOR
   Setup complete - waiting for input...
   ```

## Features Overview

### Display Screen Layout
```
┌─────────────────────────────────────┐
│         SOIL DATA                   │
├─────────────────────────────────────┤
│                                     │
│ N (mg/kg):           12             │
│ P (mg/kg):           7              │
│ K (mg/kg):           9              │
│ pH Level:            6.5            │
│ Temp (°C):           29.4           │
│ Moisture (%):        62             │
│                                     │
│         ┌────────────────┐          │
│         │   SAVE DATA    │          │
│         └────────────────┘          │
│                                     │
│       READY TO SEND                 │
└─────────────────────────────────────┘
```

### Touch Behavior
- **Touch the "SAVE DATA" button:**
  - Button highlights in green
  - "READY TO SEND" text appears
  - `dataReadyToSend` flag is set to `true`
  - Highlight effect lasts 300ms

### Bluetooth Protocol

#### Android → ESP32
```
Command: "READ\n"
Example: Send "READ" followed by newline character
```

#### ESP32 → Android (if `dataReadyToSend` is `true`)
```
Response: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62\n"

Format explanation:
- NPK=N,P,K (nitrogen, phosphorus, potassium values)
- PH=value (pH level)
- TEMP=value (temperature in Celsius)
- MOIST=value (moisture percentage)
- Ends with newline character (\n)
```

#### ESP32 → Android (if `dataReadyToSend` is `false`)
```
Response: "NO_DATA\n"
```

## Using the Code

### Updating Soil Data Programmatically
To update the display with new sensor readings:

```cpp
// Call this function with your sensor values
updateSoilData(
  12.0,   // Nitrogen (mg/kg)
  7.0,    // Phosphorus (mg/kg)
  9.0,    // Potassium (mg/kg)
  6.5,    // pH level
  29.4,   // Temperature (°C)
  62.0    // Moisture (%)
);
```

This function:
1. Updates the internal data structure
2. Redraws the display with new values
3. Prints data to Serial Monitor for debugging

### Bluetooth Testing
Using a Bluetooth Serial app on Android:
1. Pair device with `ESP32_SOIL_SENSOR`
2. Connect in the app
3. Send command: `READ`
4. Receive response: `NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62`

## Troubleshooting

### Display Not Showing
- **Check:** VCC and GND connections
- **Check:** CS, DC, RST pins are correctly connected
- **Solution:** Try resetting ESP32 (press EN button)

### Touch Not Working
- **Check:** CS pin for touch (GPIO 5) is connected
- **Check:** SPI bus connections (CLK, DIN, DO)
- **Solution:** Run touch calibration (see Touch Calibration section)

### Bluetooth Not Connecting
- **Check:** BluetoothSerial is initialized (check Serial Monitor)
- **Check:** Device name matches what you're looking for
- **Solution:** Reset ESP32, wait 5 seconds, then try pairing again

### Display Garbled or Flickering
- **Check:** SPI clock speed (should be acceptable for ILI9341)
- **Solution:** Add a delay in the loop() function:
  ```cpp
  delay(50);  // Increase if still flickering
  ```

### Button Touch Not Responsive
- **Check:** Touch calibration values (TS_MINX, TS_MAXX, etc.)
- **Solution:** Calibrate touch by testing coordinates
- **Debug:** Serial Monitor will print touch coordinates when pressed

## Color Definitions
The sketch uses standard color constants:
```cpp
COLOR_BACKGROUND      = Black
COLOR_TITLE           = Cyan
COLOR_LABEL           = White
COLOR_VALUE           = Yellow
COLOR_BUTTON          = Dark Green
COLOR_BUTTON_PRESS    = Green (brighter)
COLOR_READY_TEXT      = Magenta
```

## Performance Notes
- **Display refresh:** Optimized to avoid flickering
- **Touch response:** ~50ms polling interval
- **Bluetooth:** Non-blocking, handles commands in main loop
- **Memory:** Efficient struct-based data storage

## Next Steps
1. Upload the code and verify Serial Monitor output
2. Test the display by viewing soil data
3. Test touch by pressing "SAVE DATA" button
4. Test Bluetooth by sending "READ" command from Android
5. Integrate with your actual sensor readings using `updateSoilData()`

## Support
For issues with individual libraries:
- **Adafruit GFX:** https://github.com/adafruit/Adafruit-GFX-Library
- **Adafruit ILI9341:** https://github.com/adafruit/Adafruit_ILI9341
- **XPT2046_Touchscreen:** https://github.com/PaulStoffregen/XPT2046_Touchscreen
- **ESP32:** https://github.com/espressif/arduino-esp32

---
**Last Updated:** January 4, 2026
**Version:** 1.0

