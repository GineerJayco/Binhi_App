# RS485 Soil Sensor - Quick Start & Testing Guide

## ⚡ Quick Setup (5 Minutes)

### 1️⃣ Wiring Check
```
ESP32          RS485 Module      Soil Sensor
GPIO 17  ----> DI               
GPIO 16  ----> RO               
GPIO 21  ----> DE, RE
5V/3.3V  ----> VCC
GND      ----> GND
                         -----> A (Yellow)
                         -----> B (Blue)
                              VCC (Brown)
                              GND (Black)
```

### 2️⃣ Upload Code
- Copy `soil_sensor_tft_display.ino` to Arduino IDE
- Select Board: **ESP32 Dev Module**
- Select Port: Your ESP32 COM port
- Click **Upload**

### 3️⃣ Open Serial Monitor
- Tools → Serial Monitor
- Baud Rate: **115200**
- Look for these messages:

```
=== ESP32 Soil Sensor Display ===
RS485 Serial initialized at 4800 baud
TFT Display initialized
Touch screen initialized
Bluetooth initialized: ESP32_SOIL_SENSOR
Setup complete - waiting for input...

RS485 Sensor Data Updated:
  Moisture: 62.0%
  Temperature: 29.4°C
  ...
```

---

## 🧪 Testing Procedures

### Test 1: Sensor Communication
**What to Check:** Serial Monitor shows "RS485 Sensor Data Updated" every 5 seconds

**If it fails:**
1. Check Pin Configuration:
   - GPIO 17 → DI ✓
   - GPIO 16 → RO ✓
   - GPIO 21 → DE & RE ✓
2. Check Power:
   - RS485 module receiving 5V/3.3V ✓
   - Soil sensor receiving power ✓
3. Check RS485 Cables:
   - A (Yellow) and B (Blue) connected ✓

---

### Test 2: TFT Display
**What to Check:** 3.2" TFT shows all 7 parameters

**Expected Display:**
```
SOIL DATA
─────────────────────
N (mg/kg):           12
P (mg/kg):            7
K (mg/kg):            9
pH Level:           6.5
Temp (°C):         29.4
Moisture (%):       62.0
Conductivity:       1250

[     SAVE DATA    ]
```

**If display is blank:**
1. Check TFT power (VCC/GND)
2. Check SPI pins (CS, DC, RST, MOSI, SCK, MISO)
3. Verify display rotation is 0

---

### Test 3: Bluetooth Data Transmission
**What to Check:** Bluetooth sends complete sensor data

**Steps:**
1. Pair ESP32 with phone:
   - Bluetooth Name: `ESP32_SOIL_SENSOR`
   - PIN: `1234` (default)

2. Open Bluetooth Serial App (Android) or similar

3. Send command: `READ` (include newline)

4. Expected response:
   ```
   NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62;COND=1250
   ```

**If Bluetooth not responding:**
1. Check pairing: Device shows in Bluetooth list
2. Verify app is connected to correct COM port
3. Check Serial Monitor for "Bluetooth received: READ"

---

### Test 4: Live Data Updates
**What to Check:** Data changes every 5 seconds

**Serial Monitor Check:**
- Look for timestamp changes in "RS485 Sensor Data Updated"
- Sensor values should change periodically

**TFT Display Check:**
- Watch numbers update in real-time
- Moisture and Temperature should change slightly

**If data not updating:**
1. Check sensor is powered and connected
2. Verify RS485 termination (if needed by sensor)
3. Increase `RS485_READ_INTERVAL` if timeouts occur:
   ```cpp
   #define RS485_READ_INTERVAL 10000  // 10 seconds instead of 5
   ```

---

## 🔧 Common Fixes

### Issue: "ERROR: Incomplete RS485 sensor response"

**Solution 1: Check Cable Connections**
```
Verify:
- All wires firmly inserted
- No loose connections
- Correct color wires to correct pins
```

**Solution 2: Add Delay**
```cpp
// In readRS485Sensor() function, change:
delay(10);  // Change to delay(50);
```

**Solution 3: Check Sensor Power**
```
Measure voltage:
- Brown wire: Should show 5V or 3.3V
- Black wire: Should show 0V (Ground)
- Between A & B: Should show ~100-200mV signal
```

---

### Issue: Sensor Data Looks Wrong

**Example Bad Data:**
```
Moisture: 65535.0%      ❌ (max 100%)
Temperature: 655.0°C    ❌ (unrealistic)
```

**Possible Causes:**
1. **Byte Order Issue**: Data parsed in wrong order
   - Check if MSB/LSB is correct in parseRS485Data()

2. **Sensor Timeout**: Sensor not responding in time
   - Increase wait time in readRS485Sensor()

3. **CRC Error**: Response data corrupted
   - Data may still be valid, but verify with manual reading

---

### Issue: Touch Button Not Working

**Test:**
1. Touch the SAVE DATA button on TFT screen
2. Serial Monitor should show: "SAVE DATA button pressed!"
3. Button should change color briefly

**If not working:**
1. Verify touch screen power
2. Check touch calibration values:
   ```cpp
   #define TS_MINX 150
   #define TS_MAXX 3900
   #define TS_MINY 150
   #define TS_MAXY 3900
   ```
   (These may need adjustment for your specific screen)

---

## 📊 Expected Sensor Ranges

| Parameter | Min | Max | Typical |
|-----------|-----|-----|---------|
| Nitrogen (N) | 0 | 1000 | 20-40 mg/kg |
| Phosphorus (P) | 0 | 1000 | 10-30 mg/kg |
| Potassium (K) | 0 | 1000 | 30-60 mg/kg |
| pH | 4 | 9 | 6-7.5 |
| Temperature | -10 | 50 | 15-35 °C |
| Moisture | 0 | 100 | 20-60 % |
| Conductivity | 0 | 3000 | 500-2000 µS/cm |

If values are outside these ranges, check:
- Sensor is properly calibrated
- Soil conditions are being read correctly
- No sensor malfunction

---

## 🛠️ Debug Checklist

### Before Testing
- [ ] Code uploaded successfully (no upload errors)
- [ ] Serial Monitor shows "Setup complete"
- [ ] No warnings in Arduino IDE

### Hardware Check
- [ ] All wires connected firmly
- [ ] Power supply stable (check multimeter)
- [ ] No bent pins or damaged connectors

### Software Check
- [ ] Baud rate correct: 4800 (sensor) & 115200 (debug)
- [ ] Pin definitions match hardware:
  - RS485_RX_PIN = 16
  - RS485_TX_PIN = 17
  - RS485_DE_PIN = 21
- [ ] Display rotation set to 0 (portrait)

### Functionality Check
- [ ] Serial Monitor shows sensor data every 5 seconds
- [ ] TFT display updates with new values
- [ ] Bluetooth sends data with READ command
- [ ] Button highlights when touched

---

## 📈 Performance Tips

### Faster Sensor Reading
```cpp
#define RS485_READ_INTERVAL 2000  // Read every 2 seconds instead of 5
```

### Slower for Reliability
```cpp
#define RS485_READ_INTERVAL 10000  // Read every 10 seconds (more stable)
```

### Increase Serial Timeout
```cpp
// In readRS485Sensor(), increase timeout:
while (RS485Serial.available() < RS485_RESPONSE_SIZE && (millis() - timeout) < 2000) {
  delay(10);  // Changed from 1000ms to 2000ms
}
```

---

## 📱 Bluetooth Testing App Recommendations

### Android
- **Serial Bluetooth Terminal** (free)
- **Bluetooth Terminal** (free)
- **SSA Bluetooth Control** (free)

### iOS
- **Serial Bluetooth Terminal** (paid)
- **Bluetooth Electronics** (paid)

### Windows/Mac
- **Arduino IDE** → Tools → Serial Monitor
- **PuTTY** (free, serial over Bluetooth)

---

## ✅ Success Criteria

Your integration is **WORKING** when:
1. ✅ Serial Monitor shows sensor data updates every 5 seconds
2. ✅ TFT display shows all 7 parameters
3. ✅ Values are within expected ranges (see table above)
4. ✅ Bluetooth READ command returns formatted data
5. ✅ No error messages in Serial Monitor

---

## 🆘 Still Having Issues?

### Step 1: Isolate the Problem
```cpp
// Add this to setup() to test just the sensor:
void testRS485Sensor() {
  readRS485Sensor();
  delay(1000);
}

// Then in loop(), replace everything with:
void loop() {
  testRS485Sensor();
}
```

### Step 2: Check Sensor Response
```cpp
// Add this in readRS485Sensor() to print raw bytes:
if (RS485Serial.available() >= RS485_RESPONSE_SIZE) {
  byte receivedData[RS485_RESPONSE_SIZE];
  RS485Serial.readBytes(receivedData, RS485_RESPONSE_SIZE);
  
  // Print raw data
  Serial.print("Raw: ");
  for (int i = 0; i < RS485_RESPONSE_SIZE; i++) {
    Serial.print("0x");
    Serial.print(receivedData[i], HEX);
    Serial.print(" ");
  }
  Serial.println();
  
  // Then parse
  parseRS485Data(receivedData);
}
```

### Step 3: Contact Support
Prepare:
- [ ] Copy of Serial Monitor output
- [ ] Photo of wiring connections
- [ ] Sensor model/part number
- [ ] RS485 module model/part number

---

**Last Updated:** February 7, 2026  
**Version:** 1.0 - Complete Integration

