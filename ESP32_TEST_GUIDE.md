# ESP32 Quick Test Guide

## 🚀 FASTEST WAY TO TEST

### Step 1: Upload Code (5 minutes)
```
1. Open Arduino IDE
2. Copy ESP32_SIMPLE_TEST.ino code
3. Tools → Board → ESP32 Dev Module
4. Tools → Port → Select COM port
5. Click Upload
6. Wait for "Done uploading"
```

### Step 2: Verify Upload (1 minute)
```
1. Open Serial Monitor (115200 baud)
2. You should see:
   "Starting Bluetooth..."
   "Bluetooth initialized!"
   "Device name: ESP32_SOIL_SENSOR"
```

### Step 3: Pair with Android (2 minutes)
```
1. Android Settings → Bluetooth → ON
2. Find "ESP32_SOIL_SENSOR"
3. Tap to pair
4. Serial Monitor shows:
   "Android connected!" (or similar)
```

### Step 4: Test in App (1 minute)
```
1. Open Binhi app
2. Long-click map point
3. Click "Receive Data"
4. Dialog shows: N=150, P=120, K=180 ✅
```

---

## 📊 TEST DATA SENT

When Android app clicks "Receive Data", ESP32 sends:
```
"NPK=150,120,180"
```

The app automatically parses this as:
- Nitrogen: 150
- Phosphorus: 120
- Potassium: 180

---

## 🔧 TEST COMMANDS

### Via Serial Monitor
Type these commands in Serial Monitor to test:

```
READ
  → ESP32 responds: NPK=150,120,180

TEST
  → ESP32 responds: NPK=100,80,120

STATUS
  → ESP32 responds: STATUS=OK
```

---

## ✅ WHAT YOU'LL SEE

### Serial Monitor Output
```
Starting Bluetooth...
Bluetooth initialized!
Device name: ESP32_SOIL_SENSOR
Waiting for Android to connect...
Received: READ
Sent: NPK=150,120,180
```

### Android App Dialog
```
┌─────────────────────────┐
│   Soil Data             │
│                         │
│  Nitrogen: 150          │
│  Phosphorus: 120        │
│  Potassium: 180         │
│                         │
│     [Close]             │
└─────────────────────────┘
```

---

## 🎯 TROUBLESHOOTING

### Problem: No startup messages
**Solution**: Check USB cable, select correct COM port

### Problem: Device not in Bluetooth scan
**Solution**: Wait 10 seconds after upload, restart ESP32

### Problem: Connection fails
**Solution**: Unpair, restart both devices, pair again

### Problem: No response in app
**Solution**: 
1. Check Serial Monitor shows "Received: READ"
2. If you see received but no response, check app has permission
3. Send "TEST" command via Serial Monitor to verify

---

## 📝 CODE EXPLANATION

```cpp
// Name the device - Android will see this name
SerialBT.begin("ESP32_SOIL_SENSOR");

// Wait for Android to send data
if (SerialBT.available()) {
    // Read what Android sends
    String command = SerialBT.readStringUntil('\n');
    
    // If "READ", respond with NPK data
    if (command == "READ") {
        SerialBT.println("NPK=150,120,180");
    }
}
```

That's it! Very simple.

---

## 🎊 SUCCESS!

When you see this in your app dialog:
```
Nitrogen: 150
Phosphorus: 120
Potassium: 180
```

Your Bluetooth integration is working! ✅

Then you can:
1. Replace test data with real sensor readings
2. Connect actual NPK sensors to GPIO 34, 35, 32
3. Use ESP32_COMPLETE_CODE.ino for full implementation

---

**Total time: 10 minutes to working Bluetooth! 🚀**

