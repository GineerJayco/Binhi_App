# ESP32 Soil Sensor UI - Testing Guide

## Complete Testing Checklist

Follow this guide to verify every aspect of your ESP32 Soil Sensor system is working correctly.

---

## PHASE 1: Hardware Verification (Pre-Upload)

### Visual Inspection Checklist
- [ ] All components are undamaged
- [ ] All pin connections are made
- [ ] No loose wires or cold solder joints
- [ ] Power connections (VIN, GND, 3.3V) are secure
- [ ] SPI pins (MOSI, SCK, MISO) are connected
- [ ] Separate CS pins for LCD (GPIO 15) and Touch (GPIO 5)
- [ ] RST and DC pins connected correctly
- [ ] LCD module is 3.3V compatible (no 5V components)

### Continuity Testing (Optional, with multimeter)
- [ ] VIN to ESP32 VIN
- [ ] GND to all GND connections
- [ ] Each data line is connected (CS, DC, MOSI, SCK, MISO, RST)

---

## PHASE 2: Arduino IDE Setup Verification

### Software Installation Checklist
- [ ] Arduino IDE installed (latest version)
- [ ] ESP32 board support installed (Tools → Boards Manager → "esp32")
- [ ] Adafruit GFX library installed (Manage Libraries)
- [ ] Adafruit ILI9341 library installed
- [ ] XPT2046_Touchscreen library installed
- [ ] BluetoothSerial available (built-in with ESP32)

### Board Configuration Checklist
- [ ] Tools → Board: "ESP32 Dev Module" selected
- [ ] Tools → Upload Speed: 921600
- [ ] Tools → Flash Mode: DIO
- [ ] Tools → Flash Frequency: 40MHz
- [ ] Tools → Port: Correct COM port selected
- [ ] Code compiles without errors (Ctrl+R)

---

## PHASE 3: Initial Upload & Startup Test

### Upload Procedure
1. Connect ESP32 via USB cable
2. Open `ESP32_SOIL_SENSOR_UI.ino`
3. Verify code (Ctrl+R) - should show "Compilation complete"
4. Click Upload (Ctrl+U)
5. Wait for "Upload complete" message
6. If upload fails:
   - Press EN button on ESP32
   - Try upload again
   - Try lower baud rate (115200) if persistent issues

### Serial Monitor Test
1. Open Serial Monitor (Ctrl+Shift+M)
2. Set baud rate to **115200**
3. Press RST button on ESP32
4. Verify you see startup messages:

**Expected Output:**
```
=== ESP32 Soil Sensor UI Starting ===
TFT display initialized
Touch screen initialized
Bluetooth initialized as 'ESP32_SOIL_SENSOR'
=== Setup Complete ===
```

**Troubleshooting:**
- No output? → Check USB cable, try different COM port
- "TFT display initialized (fails)" → Check LCD power (VIN, GND) and SPI pins
- "Touch screen initialized (fails)" → Check Touch CS (GPIO 5) and shared SPI lines
- "Bluetooth initialized (fails)" → Rare issue, restart ESP32

### Result:
- [ ] Startup messages appear in Serial Monitor
- [ ] No error messages

---

## PHASE 4: Display Test

### Visual Display Check
After startup, the LCD should display:

**Expected Display:**
```
┌──────────────────────────────────┐
│          SOIL DATA               │  ← Cyan text
├──────────────────────────────────┤
│  N: 12.0 mg/kg                  │  ← White/Yellow text
│  P: 7.0 mg/kg                   │
│  K: 9.0 mg/kg                   │
│  pH: 6.5                         │
│  TEMP: 29.4C                     │
│  MOIST: 62%                      │
│                                  │
│  ┌──────────────────┐            │
│  │   SAVE DATA      │            │
│  └──────────────────┘            │
└──────────────────────────────────┘
```

### Display Verification Checklist
- [ ] Screen has black background (no white/garbage data)
- [ ] "SOIL DATA" title appears in cyan at top
- [ ] Separator line under title is visible
- [ ] All 6 soil data values display clearly:
  - [ ] N: 12.0 mg/kg
  - [ ] P: 7.0 mg/kg
  - [ ] K: 9.0 mg/kg
  - [ ] pH: 6.5
  - [ ] TEMP: 29.4C
  - [ ] MOIST: 62%
- [ ] "SAVE DATA" button is visible at bottom
- [ ] Button is dark green colored
- [ ] Text is readable (adjust text size if too small)

### Brightness Test
- [ ] Display is clearly visible in normal room lighting
- [ ] If too dim, backlight may not be connected to 3.3V
- [ ] If too bright, can't be adjusted (backlight is always-on)

### Result:
- [ ] All display elements visible and readable
- [ ] Colors match specifications

---

## PHASE 5: Touch Input Test

### Touch Detection Test

**Steps:**
1. Keep Serial Monitor open (115200 baud)
2. Touch the center of the LCD screen
3. Observe Serial Monitor for output

**Expected Output (for any touch):**
```
Touch detected: X=120, Y=150
```
(Exact numbers depend on touch location)

### Touch Coordinate Range Test

**Test 1: Touch top-left corner**
- Expected: Low X value, Low Y value
- Check Serial Monitor for coordinates

**Test 2: Touch bottom-right corner**
- Expected: High X value, High Y value
- Check Serial Monitor for coordinates

**Test 3: Touch center**
- Expected: Middle X and Y values
- Check Serial Monitor

### Button Area Verification

**Current button location (in code):**
```
X range: 40 to 200
Y range: 280 to 320
```

**Test:**
1. Touch the "SAVE DATA" button area
2. Verify coordinates print in Serial Monitor
3. If coordinates don't match button area:
   - Perform touch calibration (see Setup Guide, Section 5)
   - Update TOUCH_MIN_X, TOUCH_MAX_X, TOUCH_MIN_Y, TOUCH_MAX_Y

### Result:
- [ ] Touch events detected and printed to Serial Monitor
- [ ] Touch coordinates roughly match display positions
- [ ] Calibration values are noted for later adjustment if needed

---

## PHASE 6: Button Press Test

### Button Press Detection

**Steps:**
1. Keep Serial Monitor open
2. Press the "SAVE DATA" button on screen (use finger)
3. Watch for changes:

**Expected Behavior:**
```
Serial Monitor output:
Touch detected: X=120, Y=300
Button pressed! Data ready to send.

Display changes:
- Button color changes from dark green to bright green
- Text "READY TO SEND" appears below the button
- Display reverts to normal after 0.5 seconds
```

### Button Highlight Duration Test
- [ ] Button highlights when pressed
- [ ] Highlight stays for approximately 0.5 seconds
- [ ] Highlight disappears automatically (button becomes dark green again)

### Repeated Button Press Test
1. Press button
2. Verify it highlights and "READY TO SEND" appears
3. Press button again immediately
4. Verify same behavior occurs

### Result:
- [ ] Button presses are detected
- [ ] Button highlights (color change)
- [ ] "READY TO SEND" text displays
- [ ] Behavior repeats consistently

---

## PHASE 7: Bluetooth Pairing Test

### Android Device Bluetooth Setup

**Steps:**
1. On Android device: Settings → Bluetooth
2. Turn on Bluetooth
3. Tap "Pair new device" or "Scan for devices"
4. Look for "ESP32_SOIL_SENSOR" in the list
5. Tap to pair

**Expected Result:**
```
Device name: ESP32_SOIL_SENSOR
Status: Paired
```

**Troubleshooting:**
- [ ] Device doesn't appear in Bluetooth list
  - Power cycle ESP32 (disconnect USB and reconnect)
  - Try scanning again
  - Check Bluetooth is enabled on ESP32 (verify Serial Monitor shows Bluetooth init message)

- [ ] Pairing fails
  - Try "Unpair" and then "Pair" again
  - Restart Bluetooth on phone
  - Try a different Android device

### Result:
- [ ] ESP32_SOIL_SENSOR appears in Bluetooth devices
- [ ] Successfully paired with Android device

---

## PHASE 8: Bluetooth Communication Test

### Install Terminal App

**Recommended Apps:**
- "Serial Bluetooth Terminal" (Google Play)
- "Bluetooth Terminal" 
- "BLE Scanner" (for Bluetooth Classic)

### Test WITHOUT Button Press (No Data)

**Steps:**
1. Open Bluetooth Terminal app on Android
2. Connect to "ESP32_SOIL_SENSOR"
3. Type the command: `READ`
4. Press Enter/Send

**Expected Result:**
```
Sent:     READ
Received: NO_DATA
```

**Serial Monitor shows:**
```
BT Message received: READ
Processing READ command...
No data available. Sent: NO_DATA
```

### Test WITH Button Press (Data Ready)

**Steps:**
1. On ESP32 display, press the "SAVE DATA" button
2. Watch for "READY TO SEND" message on screen
3. In Bluetooth Terminal, send: `READ`
4. Press Enter/Send

**Expected Result:**
```
Sent:     READ
Received: NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

**Serial Monitor shows:**
```
BT Message received: READ
Processing READ command...
Data sent! Flag reset.
Sent soil data: NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

### Data Format Verification

The response should be exactly:
```
NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62
```

Verify each part:
- [ ] `NPK=12,7,9` (nitrogen, phosphorus, potassium - no decimals)
- [ ] `PH=6.5` (pH with 1 decimal)
- [ ] `TEMP=29.4` (temperature with 1 decimal)
- [ ] `MOIST=62` (moisture - no decimals)

### Repeated Test Sequence

**Test:**
1. Press button on display → "READY TO SEND" appears
2. Send "READ" via Bluetooth → Receive soil data
3. Send "READ" again (without pressing button) → Receive "NO_DATA"
4. Press button again → "READY TO SEND" appears
5. Send "READ" again → Receive soil data

**Expected:**
- Data is sent only after button press
- Flag resets after sending
- Must press button again to send data again

### Result:
- [ ] NO_DATA sent when button not pressed
- [ ] Soil data sent when button was pressed
- [ ] Data format is correct
- [ ] Flag resets properly between transmissions

---

## PHASE 9: Data Update Test (Optional)

### Testing updateSoilData() Function

**To test (open Arduino IDE Serial Monitor connected to ESP32):**

The `updateSoilData()` function is available to update displayed values:

```cpp
updateSoilData(15.5, 8.2, 10.1, 6.8, 28.5, 65.0);
//              N    P    K     pH   TEMP  MOIST
```

**How to test (advanced):**
1. Modify your code to call this function in loop() with different values
2. Recompile and upload
3. Watch display update with new values
4. Send "READ" via Bluetooth and verify new data is transmitted

### Result:
- [ ] Display updates with new values
- [ ] Bluetooth transmits updated data

---

## PHASE 10: Full Integration Test

### Complete Workflow Test

**Scenario: Multiple data acquisitions and transmissions**

**Steps:**
1. Display shows initial soil data ✓
2. Press button on display → "READY TO SEND" appears ✓
3. Send "READ" via Bluetooth → Receive soil data ✓
4. Send "READ" again → Receive "NO_DATA" ✓
5. Press button again → "READY TO SEND" appears ✓
6. Send "READ" → Receive soil data (same values since data hasn't changed) ✓
7. Update soil data values (if sensor integration done) ✓
8. Press button → "READY TO SEND" appears ✓
9. Send "READ" → Receive new soil data ✓

### Result:
- [ ] All steps complete successfully
- [ ] No errors in Serial Monitor
- [ ] Display, touch, and Bluetooth all working together

---

## PHASE 11: Extended Operation Test

### Stability Test

**Run for at least 10 minutes:**
1. Repeatedly press button and send READ commands
2. Vary time between actions (sometimes immediately, sometimes wait 30 seconds)
3. Observe for any crashes, hangs, or unexpected behavior

**Monitor for:**
- [ ] No Serial Monitor errors
- [ ] Display remains responsive
- [ ] Touch still works
- [ ] Bluetooth still responds
- [ ] No memory leaks (watch for increasing garbage in Serial Monitor)

### Result:
- [ ] System runs stably for extended period
- [ ] No crashes or resets

---

## PHASE 12: Error Scenarios Test

### What to do if you encounter errors:

#### Scenario 1: "NO_DATA" always sent
**Cause:** Button press not detected or flag not set
**Fix:**
1. Verify button press shows in Serial Monitor: "Button pressed! Data ready to send."
2. Check touch calibration
3. Verify touch hits button area (coordinates in range 40-200 X, 280-320 Y)

#### Scenario 2: Button doesn't highlight
**Cause:** Display not updating or button code issue
**Fix:**
1. Verify display works (shows data correctly)
2. Check button coordinates in code
3. Ensure drawUI() is being called

#### Scenario 3: Bluetooth doesn't connect
**Cause:** Bluetooth not initialized or not discoverable
**Fix:**
1. Check Serial Monitor shows Bluetooth initialization message
2. Power cycle ESP32
3. Clear Bluetooth cache on Android and try again

#### Scenario 4: Display shows garbage or partial data
**Cause:** SPI connection issue or display not initialized
**Fix:**
1. Double-check CS, DC, MOSI, SCK, MISO connections
2. Verify power connections
3. Try Adafruit ILI9341 library example separately
4. Check for loose wires

#### Scenario 5: Touch not responding to any input
**Cause:** Touch not initialized or wrong CS pin
**Fix:**
1. Verify GPIO 5 (touch CS) is connected
2. Check shared SPI lines (SCK, DIN/MOSI, DO/MISO)
3. Verify XPT2046_Touchscreen library is installed

---

## PHASE 13: Final Sign-Off

### Functional Requirements Checklist

**Display & UI:**
- [ ] "SOIL DATA" title displays at top
- [ ] All 6 soil values display with correct data
- [ ] "SAVE DATA" button visible at bottom
- [ ] Colors are correct (cyan title, white/yellow text, green button)

**Touch Control:**
- [ ] Touch screen responds to input
- [ ] Button press detected correctly
- [ ] Button highlights when pressed
- [ ] "READY TO SEND" text appears on button press

**Bluetooth:**
- [ ] Device pairs with Android device
- [ ] "READ" command receives data or "NO_DATA"
- [ ] Data format is correct
- [ ] Flag resets properly between transmissions

**System Stability:**
- [ ] No crashes during extended operation
- [ ] Serial Monitor shows expected messages
- [ ] All components remain responsive

---

## Test Results Summary

**Date:** _____________
**Tester:** _____________

| Test Phase | Status | Notes |
|-----------|--------|-------|
| Hardware Verification | ☐ PASS ☐ FAIL | _____________ |
| Arduino IDE Setup | ☐ PASS ☐ FAIL | _____________ |
| Initial Upload | ☐ PASS ☐ FAIL | _____________ |
| Display Test | ☐ PASS ☐ FAIL | _____________ |
| Touch Input Test | ☐ PASS ☐ FAIL | _____________ |
| Button Press Test | ☐ PASS ☐ FAIL | _____________ |
| Bluetooth Pairing | ☐ PASS ☐ FAIL | _____________ |
| Bluetooth Communication | ☐ PASS ☐ FAIL | _____________ |
| Data Update Test | ☐ PASS ☐ FAIL | _____________ |
| Full Integration | ☐ PASS ☐ FAIL | _____________ |
| Extended Operation | ☐ PASS ☐ FAIL | _____________ |

**Overall System Status:** ☐ FULLY FUNCTIONAL ☐ NEEDS ADJUSTMENT

**Known Issues (if any):**
```
_________________________________________________________________________
_________________________________________________________________________
_________________________________________________________________________
```

**Next Steps:**
- [ ] System ready for sensor integration
- [ ] System ready for field deployment
- [ ] Needs additional development (describe below)
```
_________________________________________________________________________
_________________________________________________________________________
```

---

## Quick Troubleshooting Matrix

| Problem | Check | Solution |
|---------|-------|----------|
| Display blank | Power (VIN/GND), SPI pins | Verify connections, test with Adafruit example |
| Touch not working | GPIO 5 (CS), shared SPI | Verify connections, calibrate |
| Button doesn't work | Touch coordinates, button area | Calibrate touch screen |
| No Bluetooth | Initialization message | Restart ESP32, check Serial Monitor |
| Wrong data format | sendSoilData() function | Verify data values and formatting |
| Bluetooth always "NO_DATA" | Button press detection | Check touch coordinates, verify button area |

---

## Support Resources

If tests fail and you need help:

1. **Check Serial Monitor messages** - they often indicate what's wrong
2. **Review Setup Guide** - detailed instructions for each component
3. **Check Quick Reference** - pin configurations and function details
4. **Test individual components separately** with library examples:
   - Adafruit ILI9341 example for display
   - XPT2046_Touchscreen example for touch
   - BluetoothSerial example for Bluetooth

---

Version: 1.0
Last Updated: December 29, 2025
Complete your testing and enjoy your ESP32 Soil Sensor System!

