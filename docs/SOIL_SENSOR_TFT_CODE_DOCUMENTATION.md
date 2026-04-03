# ESP32 Soil Sensor TFT - Code Documentation & Integration Examples

## File: soil_sensor_tft_display.ino

This document provides detailed explanation of the code structure and integration examples.

---

## Code Architecture Overview

```
┌─────────────────────────────────────────────┐
│  HARDWARE INITIALIZATION (setup)            │
│  - Display (ILI9341)                        │
│  - Touch (XPT2046)                          │
│  - Bluetooth Serial                         │
│  - Initial UI Draw                          │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  MAIN LOOP (loop function)                  │
│  - 50ms polling interval                    │
│  - Non-blocking operations                  │
└────┬────────────────────────────┬───────────┘
     │                            │
     ↓                            ↓
┌──────────────────────┐  ┌──────────────────────┐
│  Touch Handler       │  │  Bluetooth Handler   │
│  - Detect press      │  │  - Receive commands  │
│  - Check button area │  │  - Send data         │
│  - Set flags         │  │  - Reset states      │
└──────────────────────┘  └──────────────────────┘
     │                            │
     └────────────┬───────────────┘
                  ↓
        ┌──────────────────────┐
        │  UI Rendering        │
        │  - Draw button state │
        │  - Display values    │
        │  - Show status text  │
        └──────────────────────┘
```

---

## Key Code Sections Explained

### 1. Header Includes & Library Setup
```cpp
#include <SPI.h>                      // Serial Peripheral Interface
#include <Adafruit_GFX.h>             // Graphics library (base)
#include <Adafruit_ILI9341.h>         // Display driver for ILI9341
#include <XPT2046_Touchscreen.h>      // Touch controller driver
#include <BluetoothSerial.h>          // Bluetooth for ESP32
```

**Why each library:**
- **SPI.h:** Hardware communication with display and touch
- **Adafruit_GFX.h:** Drawing functions (rectangles, text, etc.)
- **Adafruit_ILI9341.h:** Display-specific control
- **XPT2046_Touchscreen.h:** Touch input handling
- **BluetoothSerial.h:** Wireless communication

---

### 2. Hardware Initialization Objects
```cpp
// Create display object with hardware SPI
Adafruit_ILI9341 tft = Adafruit_ILI9341(TFT_CS, TFT_DC, TFT_RST);

// Create touchscreen object
XPT2046_Touchscreen ts(TOUCH_CS, TOUCH_IRQ);

// Create Bluetooth object
BluetoothSerial SerialBT;
```

These objects manage communication with each hardware component.

---

### 3. Data Structure
```cpp
struct SoilData {
  float nitrogen;      // NPK = Nitrogen, Phosphorus, Potassium
  float phosphorus;    // Essential nutrients for plant growth
  float potassium;
  float pH;            // Soil acidity (5.5-8.0 is optimal)
  float temperature;   // Affects nutrient availability
  float moisture;      // Water content (critical for growth)
};

SoilData soilData = {
  12.0,    // Initial values for testing
  7.0,
  9.0,
  6.5,
  29.4,
  62.0
};
```

This structure stores all sensor readings in one place. Access values like:
```cpp
soilData.nitrogen      // Get nitrogen value
soilData.pH = 7.0      // Set pH value
```

---

### 4. State Management
```cpp
bool dataReadyToSend = false;  // Flag: can data be sent via Bluetooth?
unsigned long lastButtonPressTime = 0;  // Timestamp for button highlight
```

**dataReadyToSend:**
- Set to `true` when user presses "SAVE DATA" button
- Stays `true` until Android sends "READ" command
- Ensures data is only sent when intentionally saved

---

## Function Reference

### setup()
**Purpose:** Initialize all hardware and draw initial screen

**What it does:**
1. Initialize Serial (115200 baud) for debugging
2. Initialize TFT display
3. Initialize touch screen
4. Initialize Bluetooth with device name
5. Draw initial UI with default values

**Call:** Automatic, runs once at startup

---

### loop()
**Purpose:** Main program loop, handles all real-time operations

**What it does:**
1. Check for touch input
2. Check for Bluetooth commands
3. Handle button highlight effect (if pressed)
4. Small delay (50ms) for responsiveness

**Non-blocking design:**
- Doesn't wait for input
- Continues looping even if nothing happens
- Touch/Bluetooth handlers are called each cycle

---

### handleTouchInput()
**Purpose:** Detect and process touch screen input

**How it works:**
```cpp
TS_Point p = ts.getPoint();        // Get current touch point
if (p.z > 0) {                     // If pressure > 0, screen was touched
  uint16_t x = map(p.x, ...);     // Map raw coordinates to screen coords
  uint16_t y = map(p.y, ...);
  
  if (isTouchInButton(x, y)) {    // Check if touch is on button
    handleSaveButtonPress();       // Button was pressed!
  }
}
```

**Parameters:**
- `p.x, p.y`: Raw touch coordinates from XPT2046
- `p.z`: Pressure/touch strength (>0 means touched)

**Mapping:**
Touch controller returns 0-3900 range, which is mapped to 0-240/320 screen pixels

---

### isTouchInButton(x, y)
**Purpose:** Check if touch coordinates are within button rectangle

**Logic:**
```cpp
bool isTouchInButton(uint16_t x, uint16_t y) {
  return (x >= SAVE_BUTTON_X && x <= (SAVE_BUTTON_X + SAVE_BUTTON_W)) &&
         (y >= SAVE_BUTTON_Y && y <= (SAVE_BUTTON_Y + SAVE_BUTTON_H));
}
```

**Example:**
```
Button: X=50, Y=280, Width=140, Height=30
Touch at X=120, Y=295
  → X in range [50, 190]? YES
  → Y in range [280, 310]? YES
  → Result: TRUE (button pressed!)
```

---

### handleSaveButtonPress()
**Purpose:** Execute code when button is pressed

**What it does:**
1. Set `dataReadyToSend = true` (Bluetooth can now send data)
2. Record current time for highlight effect
3. Draw button in pressed state (green)
4. Display "READY TO SEND" text
5. Print to Serial Monitor for debugging

---

### drawSoilDataDisplay()
**Purpose:** Redraw entire screen with current soil data

**What it draws:**
1. Title: "SOIL DATA"
2. Six data fields with labels and values:
   - N (Nitrogen)
   - P (Phosphorus)
   - K (Potassium)
   - pH Level
   - Temp (Temperature)
   - Moisture
3. "SAVE DATA" button
4. "READY TO SEND" text (if data is ready)

---

### drawTitle()
**Purpose:** Draw the title and separator line

```cpp
void drawTitle() {
  tft.setFont(&FreeSansBold24pt7b);  // Large bold font
  tft.setTextColor(COLOR_TITLE);     // Cyan color
  tft.setCursor(10, 35);             // Position (10 pixels from left)
  tft.println("SOIL DATA");
  
  tft.drawLine(0, 45, 240, 45, COLOR_LABEL);  // Horizontal line
}
```

---

### drawDataField(label, value, y, fieldNum)
**Purpose:** Draw one data field (label + value)

**Parameters:**
- `label`: Text like "N (mg/kg):"
- `value`: Number to display
- `y`: Vertical position
- `fieldNum`: Field number (determines decimal places)

**Logic:**
```cpp
void drawDataField(const char* label, float value, int y, uint8_t fieldNum) {
  // Print label in white
  tft.setTextColor(COLOR_LABEL);
  tft.setCursor(20, y);
  tft.print(label);
  
  // Print value in yellow at position 160
  tft.setTextColor(COLOR_VALUE);
  tft.setCursor(160, y);
  
  // Determine decimal places
  if (fieldNum == 4) {          // pH - 1 decimal
    sprintf(valueStr, "%.1f", value);
  } else {                       // Others based on field
    sprintf(valueStr, "%.0f", value);
  }
  tft.print(valueStr);
}
```

---

### drawSaveButton(pressed)
**Purpose:** Draw the interactive button

**Parameters:**
- `pressed = true`: Button is being pressed (green highlight)
- `pressed = false`: Button is normal state (dark green)

**What it draws:**
```
┌─────────────────────┐
│   SAVE DATA         │  ← White text
│                     │  ← Green/Dark green background
└─────────────────────┘  ← White border
```

---

### handleBluetoothInput()
**Purpose:** Check for incoming Bluetooth commands

**Logic:**
```cpp
if (SerialBT.available()) {           // Is there data waiting?
  String command = SerialBT.readStringUntil('\n');  // Read until newline
  command.trim();                      // Remove extra spaces
  
  if (command == "READ") {
    handleReadCommand();               // Process the command
  }
}
```

**Supported Commands:**
- `"READ"` → Send soil data (if ready) or "NO_DATA"
- Anything else → Send "UNKNOWN_COMMAND"

---

### handleReadCommand()
**Purpose:** Process the "READ" Bluetooth command

**Logic:**
```cpp
if (dataReadyToSend) {
  sendSoilData();              // Send the data
  dataReadyToSend = false;     // Reset flag
  // Clear status text from display
}else {
  SerialBT.println("NO_DATA");  // No data ready to send
}
```

---

### sendSoilData()
**Purpose:** Format and send soil data via Bluetooth

**Format:**
```
NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

**Breaking it down:**
- `NPK=` : Section header
- `12,7,9` : N, P, K values separated by commas
- `;` : Section separator
- `PH=6.5` : pH value with 1 decimal place
- `;` : Section separator
- `TEMP=29.4` : Temperature with 1 decimal place
- `;` : Section separator
- `MOIST=62` : Moisture percentage
- `\n` : Newline character (line ending)

**Code:**
```cpp
sprintf(dataStr, "NPK=%.0f,%.0f,%.0f;PH=%.1f;TEMP=%.1f;MOIST=%.0f",
        soilData.nitrogen,
        soilData.phosphorus,
        soilData.potassium,
        soilData.pH,
        soilData.temperature,
        soilData.moisture);

SerialBT.print(dataStr);
SerialBT.println();  // Add newline
```

**Format specifiers:**
- `%.0f` : Float with 0 decimal places (12.7 becomes 12)
- `%.1f` : Float with 1 decimal place (6.54 becomes 6.5)

---

### updateSoilData(n, p, k, ph, temp, moist)
**Purpose:** Update soil data and refresh display

**This is the function you call from your sensor code!**

**Example usage:**
```cpp
// Read sensors
float nitrogen = analogRead(pin_N) / 409.6;  // Convert 0-4095 to 0-10
float phosphorus = analogRead(pin_P) / 409.6;
float potassium = analogRead(pin_K) / 409.6;
// ... etc for other sensors

// Update display
updateSoilData(nitrogen, phosphorus, potassium, pH, temperature, moisture);
```

**What it does:**
1. Updates internal SoilData structure
2. Redraws entire display with new values
3. Prints values to Serial Monitor for debugging

---

## Integration Examples

### Example 1: Reading from Analog Sensors

```cpp
// Pin definitions for your sensors
#define PIN_NITROGEN A0
#define PIN_PHOSPHORUS A3
#define PIN_POTASSIUM A6
#define PIN_MOISTURE A7
#define PIN_TEMPERATURE A4
#define PIN_PH A5

// In your setup() or loop():
float nitrogen = analogRead(PIN_NITROGEN) * (255.0 / 4095.0);  // Convert to 0-255
float phosphorus = analogRead(PIN_PHOSPHORUS) * (200.0 / 4095.0);  // Convert to 0-200
float potassium = analogRead(PIN_POTASSIUM) * (300.0 / 4095.0);    // Convert to 0-300
float pH = analogRead(PIN_PH) * (14.0 / 4095.0);              // Convert to 0-14
float temperature = (analogRead(PIN_TEMPERATURE) * 3.3 / 4095.0) * 100;  // LM35: 10mV per °C
float moisture = analogRead(PIN_MOISTURE) * (100.0 / 4095.0); // Convert to 0-100%

// Update display
updateSoilData(nitrogen, phosphorus, potassium, pH, temperature, moisture);
```

---

### Example 2: Reading from I2C Sensor

```cpp
// Add to includes
#include <Wire.h>

// Suppose you have a soil NPK sensor on I2C
#define NPK_SENSOR_ADDRESS 0x60

void readNPKSensor() {
  Wire.beginTransmission(NPK_SENSOR_ADDRESS);
  Wire.write(0x03);  // Read NPK command
  Wire.endTransmission();
  
  Wire.requestFrom(NPK_SENSOR_ADDRESS, 6);  // Request 6 bytes
  
  uint8_t n = Wire.read();
  uint8_t p = Wire.read();
  uint8_t k = Wire.read();
  uint8_t temp = Wire.read();
  uint8_t moist = Wire.read();
  uint8_t pH_raw = Wire.read();
  
  float pH = pH_raw / 10.0;  // Convert 65 to 6.5
  
  updateSoilData(n, p, k, pH, temp, moist);
}

// Call in loop():
// readNPKSensor();
```

---

### Example 3: Scheduled Updates (Every 5 seconds)

```cpp
unsigned long lastUpdate = 0;
const unsigned long UPDATE_INTERVAL = 5000;  // 5 seconds

void loop() {
  // Existing code...
  handleTouchInput();
  handleBluetoothInput();
  
  // Update sensors every 5 seconds
  if (millis() - lastUpdate >= UPDATE_INTERVAL) {
    lastUpdate = millis();
    
    // Read sensors (your code here)
    float n = readNitrogen();
    float p = readPhosphorus();
    // ... etc
    
    updateSoilData(n, p, k, pH, temp, moist);
  }
  
  delay(50);
}
```

---

### Example 4: Button to Trigger Sensor Read

```cpp
// Modify handleSaveButtonPress():
void handleSaveButtonPress() {
  Serial.println("SAVE DATA button pressed!");
  
  // Read sensors when button is pressed
  float n = readNitrogen();
  float p = readPhosphorus();
  float k = readPotassium();
  float pH = readPH();
  float temp = readTemperature();
  float moist = readMoisture();
  
  // Update display with fresh data
  updateSoilData(n, p, k, pH, temp, moist);
  
  // Set flag for Bluetooth sending
  dataReadyToSend = true;
  lastButtonPressTime = millis();
  
  // Visual feedback
  drawSaveButton(true);
  displayReadyStatus(true);
}
```

---

### Example 5: Log Data to SPIFFS (Flash Storage)

```cpp
#include <SPIFFS.h>

void logSoilData() {
  // Create/open file
  File file = SPIFFS.open("/soil_log.txt", "a");  // Append mode
  
  if (!file) {
    Serial.println("Failed to open log file");
    return;
  }
  
  // Write timestamp and data
  char logEntry[150];
  sprintf(logEntry, "%lu,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f\n",
          millis(),
          soilData.nitrogen,
          soilData.phosphorus,
          soilData.potassium,
          soilData.pH,
          soilData.temperature,
          soilData.moisture);
  
  file.print(logEntry);
  file.close();
  
  Serial.println("Data logged");
}

// Call in loop() or in handleSaveButtonPress()
```

---

## Debugging Tips

### Serial Monitor
```cpp
// Print values for debugging
Serial.print("Touch: X=");
Serial.print(x);
Serial.print(", Y=");
Serial.println(y);

// Print data structure
Serial.print("Current N: ");
Serial.println(soilData.nitrogen);
```

### Check Bluetooth Communication
```cpp
// Add to handleBluetoothInput()
if (SerialBT.available()) {
  String command = SerialBT.readStringUntil('\n');
  
  // Print what was received
  Serial.print("BT Command: '");
  Serial.print(command);
  Serial.println("'");
}
```

### Monitor Touch Coordinates
```cpp
// Add to handleTouchInput() for debugging
if (p.z > 0) {
  Serial.print("Raw Touch: ");
  Serial.print(p.x);
  Serial.print(", ");
  Serial.println(p.y);
  
  Serial.print("Mapped Touch: ");
  Serial.print(x);
  Serial.print(", ");
  Serial.println(y);
}
```

---

## Memory Usage

**Approximate RAM usage:**
- Display object: ~5KB
- Touch object: ~2KB
- Bluetooth: ~10KB
- SoilData struct: 24 bytes
- Other variables: ~1KB
- **Total: ~18KB of 320KB available**

**Plenty of room for sensor code and additional features!**

---

## Performance Timing

| Operation | Time | Notes |
|-----------|------|-------|
| Touch sampling | 5-10ms | Per cycle |
| Display redraw | 30-50ms | All text and buttons |
| Bluetooth check | 1-2ms | Per cycle |
| Serial print | 5-10ms | Debug only |
| Total loop cycle | ~50-70ms | Comfortable responsiveness |

---

## Next Steps

1. **Copy the main code** to your Arduino IDE
2. **Install all libraries** (GFX, ILI9341, XPT2046, ESP32 board)
3. **Verify pins** match your wiring
4. **Upload and test** display and touch
5. **Implement sensor reading** using examples above
6. **Test Bluetooth** communication
7. **Integrate with Android app**

---

**Last Updated:** January 4, 2026
**Code Version:** 1.0

