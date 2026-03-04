# ESP32 Soil Sensor UI - Complete Setup & Configuration Guide

## Overview
This guide walks you through setting up the ESP32 Soil Sensor Display with touch control and Bluetooth communication.

---

## 1. HARDWARE SETUP

### Components Required
- ESP32 Dev Module (ESP32-DEVKIT-V1)
- 3.2" SPI TFT LCD Display (240x320 pixels, ILI9341 controller)
- XPT2046 Touch Controller (usually integrated on the LCD module)
- Micro USB cable for programming
- Jumper wires
- Optional: USB-to-TTL converter for serial debugging

### Pin Connections

#### LCD Display Connections
```
LCD Pin    ->  ESP32 Pin    Color (typical)
VCC        ->  VIN          Red
GND        ->  GND          Black
CS         ->  GPIO 15      White
RST        ->  GPIO 4       Blue
DC         ->  GPIO 2       Green
MOSI       ->  GPIO 23      Yellow
SCK        ->  GPIO 18      Purple
MISO       ->  GPIO 19      Orange
LED (Backlight) -> 3.3V     (constant)
```

#### Touch Controller Connections
```
Touch Pin   ->  ESP32 Pin    Function
CLK         ->  GPIO 18      Shared with LCD SCK
CS          ->  GPIO 5       Touch Chip Select
DIN         ->  GPIO 23      Shared with LCD MOSI
DO          ->  GPIO 19      Shared with LCD MISO
GND         ->  GND
VCC         ->  3.3V
```

### Wiring Notes
- **Shared SPI Bus**: LCD and Touch share SCK, MOSI, and MISO lines but have separate CS pins
- **GND**: Make sure all GND pins are connected together
- **3.3V**: The display backlight (LED) can be connected directly to 3.3V for constant brightness
- **Voltage**: ESP32 operates at 3.3V - ensure your LCD module is 3.3V compatible

---

## 2. ARDUINO IDE SETUP

### Step 1: Install Arduino IDE
- Download from: https://www.arduino.cc/en/software

### Step 2: Add ESP32 Board Support
1. Open Arduino IDE
2. Go to **File → Preferences**
3. In "Additional Boards Manager URLs", add:
   ```
   https://dl.espressif.com/dl/package_esp32_index.json
   ```
4. Click **OK**
5. Go to **Tools → Board → Boards Manager**
6. Search for "ESP32" and install "esp32 by Espressif Systems"
7. Go to **Tools → Board** and select "ESP32 Dev Module"

### Step 3: Configure Board Settings
In Arduino IDE, set:
- **Board**: ESP32 Dev Module
- **Flash Mode**: DIO
- **Flash Frequency**: 40MHz
- **Upload Speed**: 921600 (or lower if connection is unstable)
- **Port**: Select your COM port (e.g., COM3)

---

## 3. INSTALL REQUIRED LIBRARIES

Install the following libraries via **Sketch → Include Library → Manage Libraries**:

### Required Libraries:
1. **Adafruit GFX Library** (v1.11.9 or newer)
   - Search: "Adafruit GFX Library"
   - Author: Adafruit

2. **Adafruit ILI9341** (v1.6.0 or newer)
   - Search: "Adafruit ILI9341"
   - Author: Adafruit

3. **XPT2046_Touchscreen** (v0.1.4 or newer)
   - Search: "XPT2046_Touchscreen"
   - Author: Paul Stoffregen

4. **BluetoothSerial** (Built-in with ESP32 core)
   - No additional installation needed

### Installation Steps:
1. Open Arduino IDE
2. Sketch → Include Library → Manage Libraries (Ctrl+Shift+I)
3. Search for each library name
4. Click **Install** on the version recommended above
5. Restart Arduino IDE after installing all libraries

---

## 4. CODE UPLOAD

### Step 1: Open the Sketch
- Open `ESP32_SOIL_SENSOR_UI.ino` in Arduino IDE

### Step 2: Verify the Code
- Click **Sketch → Verify/Compile** (Ctrl+R)
- Wait for "Compilation complete" message

### Step 3: Upload to ESP32
1. Connect ESP32 to computer via Micro USB cable
2. Ensure correct port is selected (**Tools → Port → COMX**)
3. Click **Upload** (Ctrl+U)
4. Wait for "Upload complete" message
5. Press the **EN (Enable)** button on the ESP32 if upload doesn't start

### Step 4: Monitor Serial Output
- Open **Tools → Serial Monitor** (Ctrl+Shift+M)
- Set baud rate to **115200**
- Press the **RST (Reset)** button on ESP32
- You should see startup messages

Expected startup output:
```
=== ESP32 Soil Sensor UI Starting ===
TFT display initialized
Touch screen initialized
Bluetooth initialized as 'ESP32_SOIL_SENSOR'
=== Setup Complete ===
```

---

## 5. DISPLAY CALIBRATION

The touch screen coordinates need to be mapped to display coordinates. If touches aren't accurate:

### Touch Calibration Steps:

1. In the code, find these lines (around line 85-88):
   ```cpp
   #define TOUCH_MIN_X 200
   #define TOUCH_MAX_X 3900
   #define TOUCH_MIN_Y 200
   #define TOUCH_MAX_Y 3900
   ```

2. Open **Serial Monitor** (Ctrl+Shift+M, baud 115200)

3. Touch the top-left corner and note X and Y values printed
4. Touch the bottom-right corner and note X and Y values printed

5. Update the calibration values:
   - `TOUCH_MIN_X` = X value from top-left touch
   - `TOUCH_MIN_Y` = Y value from top-left touch
   - `TOUCH_MAX_X` = X value from bottom-right touch
   - `TOUCH_MAX_Y` = Y value from bottom-right touch

6. Re-upload the code

Alternatively, you can use a touchscreen calibration sketch to be more precise.

---

## 6. BLUETOOTH PAIRING (Android)

### Step 1: Enable Bluetooth on Android Device
- Open Settings → Bluetooth
- Turn on Bluetooth

### Step 2: Pair with ESP32
- In Bluetooth settings, tap "Pair new device"
- Select "ESP32_SOIL_SENSOR" from the list
- No PIN is required (default is 1234 if prompted)

### Step 3: Install Android App
- Install an app that can communicate via Bluetooth (e.g., "Serial Bluetooth Terminal")
- Connect to "ESP32_SOIL_SENSOR"

### Step 4: Test Communication
- In the app, send: `READ\n`
- ESP32 should respond with one of:
  - `NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62\n` (if button was pressed)
  - `NO_DATA\n` (if button wasn't pressed)

---

## 7. OPERATION GUIDE

### On the ESP32 Display:
1. **Title**: Shows "SOIL DATA" at top
2. **Soil Data Values**: Displays current sensor readings
   - N (Nitrogen)
   - P (Phosphorus)
   - K (Potassium)
   - pH Level
   - Temperature (°C)
   - Moisture (%)

3. **SAVE DATA Button**: 
   - Located at bottom of screen
   - Press with finger to trigger data save
   - Button highlights green when pressed
   - Sets `dataReadyToSend = true`
   - "READY TO SEND" text appears

### Via Bluetooth:
1. Open Bluetooth terminal app on Android
2. Send command: `READ\n`
3. Receive response:
   - If button was pressed: Full soil data
   - If button wasn't pressed: `NO_DATA`

### Data Format:
```
NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```
- NPK values: No decimals (integers)
- PH: 1 decimal place
- TEMP: 1 decimal place
- MOIST: No decimals (percentage)

---

## 8. UPDATING SENSOR DATA

### Method 1: Hardcoded Values (for testing)
Edit these lines in `setup()`:
```cpp
SoilData soilData = {12.0, 7.0, 9.0, 6.5, 29.4, 62.0};
//                   N     P     K     pH   TEMP  MOIST
```

### Method 2: Use updateSoilData() Function
Call this function to update display with new values:
```cpp
updateSoilData(15.5, 8.2, 10.1, 6.8, 28.5, 65.0);
```

### Method 3: Read from Real Sensors
Replace the hardcoded values with actual sensor readings in the `loop()` function.

---

## 9. TROUBLESHOOTING

### Display doesn't show anything
- Check all power connections (VIN, GND, 3.3V)
- Check SPI pin connections (MOSI, SCK, MISO)
- Verify CS and DC pins are correct
- Try uploading the Adafruit example to test the display alone

### Touch is unresponsive
- Verify touch CS pin (GPIO 5) connection
- Check that touch controller shares SCK, MOSI, MISO with display
- Perform touch calibration (see section 5)
- Try the XPT2046_Touchscreen library example

### Bluetooth not connecting
- Check that "ESP32_SOIL_SENSOR" appears in Android Bluetooth devices
- Try forgetting and re-pairing
- Check serial monitor for Bluetooth init errors
- Verify BluetoothSerial library is included (built-in)

### Button doesn't respond
- Check touch calibration values
- Serial monitor will print coordinates when touched - verify they're in button range
- Button bounds: X: 40-200, Y: 280-320
- Try touching the center of the button first

### Data sends as "NO_DATA" when it shouldn't
- Ensure button was pressed on display (watch for green highlight)
- Check that button press sets `dataReadyToSend = true`
- Verify `handleReadCommand()` receives "READ" message correctly

---

## 10. CUSTOMIZATION

### Change Bluetooth Device Name
Find this line:
```cpp
if (!SerialBT.begin("ESP32_SOIL_SENSOR")) {
```
Replace `"ESP32_SOIL_SENSOR"` with your desired name.

### Change Display Colors
Find the color definitions (around line 45):
```cpp
#define CYAN        0x07FF
#define YELLOW      0xFFE0
// ... etc
```
Use online RGB565 color pickers to find hex values.

### Modify Button Position/Size
Find these lines (around line 90):
```cpp
#define BUTTON_X 40
#define BUTTON_Y 280
#define BUTTON_WIDTH 160
#define BUTTON_HEIGHT 40
```

### Adjust Text Sizes
In the drawing functions, change `tft.setTextSize()` value (larger = bigger text).

### Change Data Format
Modify the `sendSoilData()` function to use different formatting.

---

## 11. SERIAL MONITOR MESSAGES

### Expected Messages During Operation:

**Startup:**
```
=== ESP32 Soil Sensor UI Starting ===
TFT display initialized
Touch screen initialized
Bluetooth initialized as 'ESP32_SOIL_SENSOR'
=== Setup Complete ===
```

**Button Press:**
```
Touch detected: X=120, Y=300
Button pressed! Data ready to send.
```

**Bluetooth Read Command:**
```
BT Message received: READ
Processing READ command...
Sent soil data: NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

**No Data Ready:**
```
BT Message received: READ
Processing READ command...
No data available. Sent: NO_DATA
```

---

## 12. POWER CONSUMPTION

- **ESP32**: ~80mA (average)
- **LCD Backlight**: ~50mA
- **Touch Controller**: ~5mA
- **Bluetooth**: ~20mA (average)

**Total typical**: ~150mA

For battery-powered operation, consider:
- Using a Li-Po battery (3.7V) with USB charging module
- Implementing sleep modes between sensor readings
- Dimming LCD backlight during idle periods

---

## 13. NEXT STEPS

1. **Integrate Real Sensors**: Connect soil moisture, temperature, and NPK sensors
2. **Add Data Logging**: Save data to SD card or SPIFFS
3. **Enhance UI**: Add charts, graphs, or additional screens
4. **Improve Bluetooth**: Create a companion Android app
5. **Battery Management**: Implement low-power modes

---

## 14. SUPPORT RESOURCES

- **Adafruit GFX Documentation**: https://learn.adafruit.com/adafruit-gfx-graphics-library
- **ILI9341 Library**: https://github.com/adafruit/Adafruit_ILI9341
- **XPT2046 Library**: https://github.com/PaulStoffregen/XPT2046_Touchscreen
- **ESP32 Documentation**: https://docs.espressif.com/projects/esp-idf/en/latest/esp32/
- **Bluetooth on ESP32**: https://github.com/espressif/arduino-esp32/tree/master/libraries/BluetoothSerial

---

## Quick Reference: Testing Checklist

- [ ] All hardware connections verified
- [ ] Arduino IDE with ESP32 support installed
- [ ] All required libraries installed
- [ ] Code compiles without errors
- [ ] Code uploads successfully
- [ ] Startup messages appear in Serial Monitor
- [ ] Display shows "SOIL DATA" title and values
- [ ] Touch responds (coordinates print in Serial Monitor)
- [ ] Button press highlights and shows "READY TO SEND"
- [ ] Bluetooth pairs successfully with Android device
- [ ] "READ" command returns soil data or "NO_DATA"

---

Generated: December 29, 2025

