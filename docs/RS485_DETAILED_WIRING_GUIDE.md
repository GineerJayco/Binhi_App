# RS485 Soil Sensor - Detailed Wiring Guide

## 🎯 Complete Pin Mapping

### ESP32 Pin Assignments

```
ESP32 DEV MODULE
┌─────────────────────────────┐
│                             │
│ GND  ─────────────────────[GND_A]
│ D23  ─────────────────────[TFT_MOSI]
│ D22  
│ D21  ─────────────────────[RS485_DE/RE] ← IMPORTANT!
│ D20  
│ D19  ─────────────────────[TFT_MISO]
│ D18  ─────────────────────[TFT_SCK]
│ D17  ─────────────────────[RS485_TX_PIN]
│ D16  ─────────────────────[RS485_RX_PIN]
│ D15  ─────────────────────[TFT_CS]
│ D4   ─────────────────────[TFT_RST]
│ D2   ─────────────────────[TFT_DC]
│ D5   ─────────────────────[TOUCH_CS]
│ D36  ─────────────────────[TOUCH_IRQ] (optional)
│ 3.3V ─────────────────────[TFT_LED]
│ 5V   ─────────────────────[RS485_VCC]
│ GND  ─────────────────────[GND_B]
│                             │
└─────────────────────────────┘
```

### Detailed RS485 Configuration

```cpp
// These are the PIN definitions in your code:

// RS485 Module Communication
#define RS485_RX_PIN 16      // Connect to RO (Receiver Output)
#define RS485_TX_PIN 17      // Connect to DI (Driver Input)
#define RS485_DE_PIN 21      // Connect to both DE and RE
#define RS485_RE_PIN 21      // Same pin as DE_PIN (combined)

// Serial Configuration
HardwareSerial RS485Serial(1);  // UART1 on ESP32
#define RS485_BAUD_RATE 4800   // Soil sensor baud rate
```

---

## 📦 Component Connections

### 1. RS485 TTL Module (TTL-to-RS485 Converter)

**Module Diagram:**
```
      TTL-RS485 MODULE
    ┌──────────────────┐
    │   TTL    RS485   │
    │  ┌────┐ ┌─────┐  │
    │  │    │ │     │  │
    │  GND──GND─────GND │
    │  VCC──VCC─────+5V │
    │  RO   TX           │
    │  DI   RX           │
    │  DE   DE           │
    │  RE   RE           │
    │                    │
    └──────────────────┘

    Functional:
    - RO (Receiver Out)  → From RS485 bus, to ESP32
    - DI (Driver Input)  → From ESP32, to RS485 bus
    - DE (Driver Enable) → Direction control
    - RE (Receiver Enable)→ Direction control
```

**Pin Connections:**
| Module Pin | Signal | ESP32 Pin | Purpose |
|-----------|--------|-----------|---------|
| RO | RX Data | GPIO 16 | Receive from Sensor |
| DI | TX Data | GPIO 17 | Send to Sensor |
| DE | Dir Enable | GPIO 21 | Control TX mode |
| RE | Rcv Enable | GPIO 21 | Control RX mode |
| VCC | Power | 5V or 3.3V | Module Power |
| GND | Ground | GND | Common Ground |

---

### 2. Soil Sensor NPK (RS485 Terminal)

**Sensor Connections:**
```
   SOIL SENSOR (RS485 End)
   ┌──────────────────┐
   │   MODBUS RTU     │
   │  RS485 Terminal  │
   │                  │
   │  A (Yellow)  ←───┼─→ TO RS485 Module A Terminal
   │  B (Blue)    ←───┼─→ TO RS485 Module B Terminal
   │  VCC (Brown) ←───┼─→ TO Power Supply (5V/3.3V)
   │  GND (Black) ←───┼─→ TO Ground
   │                  │
   └──────────────────┘

   SENSOR PROBE
   ┌────────────┐
   │            │
   │  EC Sensor │
   │  pH Sensor │
   │  Temp SNS  │
   │  NPK SNS   │
   │            │
   └────────────┘
```

**Wire Color Guide:**
| Wire Color | Function | Connection |
|-----------|----------|------------|
| Yellow | RS485-A | RS485 Module A Terminal |
| Blue | RS485-B | RS485 Module B Terminal |
| Brown | VCC (Power) | +5V or +3.3V Supply |
| Black | GND (Ground) | Ground |

---

## 🔌 Complete Wiring Diagram

```
                          ┌─────────────────────────────────────┐
                          │      3.2" TFT LCD (ILI9341)         │
                          │                                      │
                          │  (Pin connections already            │
                          │   configured in existing code)       │
                          │                                      │
                          └─────────────────────────────────────┘
                                      ▲
                                      │
        ┌─────────────────────────────┴──────────────────────────────┐
        │                             │                              │
    ┌───▼────┐                    ┌───▼────┐                    ┌────┴───┐
    │ ESP32  │                    │ XPT2046│                    │POWER   │
    │        │                    │ TOUCH  │                    │        │
    │ 5V ────┼─────────────────┐  │        │                    │ 5V  ───┼──► +5V SUPPLY
    │ GND ───┼─┬────────────┐  │  └────────┘                    │ GND ───┼──► GND
    │        │ │            │  │                                │        │
    │ GPIO17 ├─┤ TX ────────┼──┬──────────────────┐            └────────┘
    │ GPIO16 ├─┤ RX ────────┼──┤    ┌──────────┐  │
    │ GPIO21 ├─┤ DE/RE ─────┼──┤    │TTL-RS485 │  │
    │        │ │            │  │    │ MODULE   │  │
    │ 3.3V ──┼─┼─TFT_LED    │  │    │          │  │
    │        │ │            │  │ A ─┼──────────┼──┼────────► Yellow Wire ──┐
    │        │ │            │  │ B ─┼──────────┼──┼────────► Blue Wire    ├─→ SOIL SENSOR
    │ GPIO15─┼─┤ TFT_CS     │  │ +5V┼──────────┼──┼────────► Brown Wire   │   RS485 BUS
    │ GPIO4 ─┼─┤ TFT_RST    │  │ GND┼──────────┼──┼────────► Black Wire ──┘
    │ GPIO2 ─┼─┤ TFT_DC     │  │    │          │  │
    │ GPIO23─┼─┤ TFT_MOSI   │  │    └──────────┘  │
    │ GPIO18─┼─┤ TFT_SCK    │  │                  │
    │ GPIO19─┼─┤ TFT_MISO   │  │                  │
    │ GPIO5 ─┼─┤ TOUCH_CS   │  │                  │
    │ GPIO36─┼─┤ TOUCH_IRQ  │  │                  │
    │        │ │            │  │                  │
    └────────┘ └────────────┘  └──────────────────┘
```

---

## ⚙️ Setup Instructions

### Step 1: Prepare Cables

**For Soil Sensor Connection:**
```
1. Take Yellow wire (A) from sensor
2. Take Blue wire (B) from sensor
3. Connect Yellow → RS485 Module Terminal A
4. Connect Blue → RS485 Module Terminal B
```

**For Power:**
```
1. Brown wire (VCC) → 5V or 3.3V power supply
2. Black wire (GND) → Ground (common with ESP32)
```

### Step 2: RS485 Module Setup

```
TTL-RS485 Module physical setup:

    Pin View:
    ┌─ VCC ─────→ 5V (or 3.3V)
    ├─ GND ─────→ Ground
    ├─ RO ──────→ ESP32 GPIO 16 (RX)
    ├─ DI ──────→ ESP32 GPIO 17 (TX)
    ├─ DE ──────→ ESP32 GPIO 21
    ├─ RE ──────→ ESP32 GPIO 21 (same wire as DE)
    │
    │ RS485 Terminals (A/B):
    ├─ A ───────→ Sensor Yellow Wire
    └─ B ───────→ Sensor Blue Wire
```

### Step 3: ESP32 UART1 Pin Assignment

```cpp
// Your code uses UART1 (Hardware Serial 1):
HardwareSerial RS485Serial(1);  // UART1
RS485Serial.begin(RS485_BAUD_RATE, SERIAL_8N1, RS485_RX_PIN, RS485_TX_PIN);

// This automatically assigns:
// GPIO 16 → RX1 (Receive)
// GPIO 17 → TX1 (Transmit)
```

### Step 4: Verify Power Distribution

**Power Supply Requirements:**
```
Component          Voltage    Current    Note
──────────────────────────────────────────────
ESP32 Dev         5V         500mA      USB Power OK
TFT LCD           3.3V       200mA      From ESP32 3.3V
Touch Controller  3.3V       50mA       From ESP32 3.3V
RS485 Module      5V         100mA      External 5V recommended
Soil Sensor       5V         200mA      From RS485 module VCC

Total Needed: ~1.0-1.5A at 5V
Solution: Use external 5V power supply (USB adapter or dedicated)
```

---

## 🔍 Connection Verification Checklist

### Before Power-Up

- [ ] All cables firmly inserted
- [ ] No loose connections
- [ ] Yellow wire on RS485 Module A terminal
- [ ] Blue wire on RS485 Module B terminal
- [ ] Brown & Black wires to correct power/GND
- [ ] GPIO 21 connected to BOTH DE and RE pins
- [ ] GPIO 17 connected to DI pin
- [ ] GPIO 16 connected to RO pin
- [ ] RS485 Module has 5V power
- [ ] Soil Sensor has power (5V from brown wire)
- [ ] All GND connections are common

### After Power-Up

- [ ] No visible smoke or burning smell
- [ ] LED on RS485 module (if present) is lit
- [ ] Serial Monitor shows startup messages
- [ ] TFT display lights up
- [ ] No error messages in Serial Monitor

### After Running Code

- [ ] Serial Monitor shows "RS485 Sensor Data Updated"
- [ ] Data values are within reasonable ranges:
  - Moisture: 0-100%
  - Temperature: 0-50°C
  - pH: 4-9
  - NPK: 0-1000 mg/kg
  - Conductivity: 0-3000 µS/cm
- [ ] Values change every 5 seconds
- [ ] TFT display updates in real-time

---

## 🧪 Testing with Multimeter

### Verify Power Supply

```
1. Power supply output (before RS485 module):
   Expected: 5V ± 0.5V DC

2. RS485 Module VCC pin:
   Expected: 5V ± 0.2V DC

3. Soil Sensor VCC wire (Brown):
   Expected: 5V ± 0.2V DC

4. All GND points:
   Expected: 0V DC (reference)
```

### Verify RS485 Bus

```
Between A (Yellow) and B (Blue) terminals:
- At Rest (idle): ~100-500mV
- During Communication: Various levels, but changing

If reading 5V or 0V consistently:
→ Check for loose connections
→ Check for damaged RS485 termination (if sensor requires it)
```

### Verify GPIO Outputs

```
GPIO 21 (DE/RE):
- Idle: 0V (Low - receive mode)
- During TX: 5V (High - transmit mode)
- Should toggle every 5 seconds during sensor read

GPIO 17 (TX):
- Idle: 3.3V
- During TX: 0V-3.3V (signal transitions)

GPIO 16 (RX):
- Should show signal activity during sensor response
```

---

## 🚨 Troubleshooting Physical Connections

### Problem: No Communication

**Step 1: Check Wires**
```
Use multimeter Continuity mode:
1. GPIO 17 → DI pin on RS485 (should beep)
2. GPIO 16 → RO pin on RS485 (should beep)
3. GPIO 21 → DE pin on RS485 (should beep)
4. GPIO 21 → RE pin on RS485 (should beep)
5. Yellow → A terminal (should beep)
6. Blue → B terminal (should beep)
```

**Step 2: Check Power**
```
All voltages should be stable:
1. RS485 VCC = 5V (not 0V, not 12V)
2. Sensor VCC = 5V (Brown wire)
3. No GND difference (all points at 0V)
```

**Step 3: Check Sensor**
```
1. Is sensor powered (LED on)?
2. Sensor working with original example code?
3. Sensor has 4-pin connector properly seated?
```

### Problem: Garbled Data

**Step 1: Check Baud Rate**
```
Serial Monitor should show exactly:
RS485 Serial initialized at 4800 baud

If NOT shown, check:
- Code compiles without errors
- Board type is ESP32 Dev Module
```

**Step 2: Check Cable Shielding**
```
RS485 bus should be as short as possible:
- Ideal: < 1 meter
- Use shielded cable if available
- Keep away from power cables and motors
```

**Step 3: Check RS485 Termination**
```
Some sensors need 120Ω resistor between A & B:
- Check sensor manual
- If needed, add 120Ω resistor in parallel
  across A and B terminals (last device only)
```

---

## 📐 Cable Length Recommendations

```
Component              Max Length    Recommended
──────────────────────────────────────────────────
GPIO to RS485 Module   0.5m          0.2m
RS485 Module to Sensor 10m (with 120Ω resistor)
Power Supply Cable     2m            1m
                       
Note: Longer cables → use shielded RS485 cable
```

---

## 🔐 Final Safety Check

Before running the system:

1. **Electrical Safety**
   - [ ] All connections are soldered/crimped (not just touching)
   - [ ] No loose wires that could short
   - [ ] Power supply is proper voltage (5V, not 12V)
   - [ ] Current draw is within supply capacity

2. **Physical Safety**
   - [ ] No sharp wires exposed
   - [ ] Connectors are fully seated
   - [ ] No strain on wires at connection points
   - [ ] System is on non-flammable surface

3. **Component Safety**
   - [ ] All ICs have heat dissipation
   - [ ] No components are too hot to touch
   - [ ] Sensor probe is isolated from live electrical contacts
   - [ ] RS485 module is not exposed to moisture

---

## 💡 Pro Tips

1. **Use Breadboard for RS485 Module**
   ```
   Makes connections easier and more reliable
   than loose wires
   ```

2. **Color-Code Your Wires**
   ```
   Red   = 5V Power
   Black = Ground
   Green = RS485 A
   Yellow= RS485 B
   Blue  = GPIO connections
   ```

3. **Label Everything**
   ```
   Use masking tape to label:
   - Wire ends
   - Sensor location
   - RS485 Module A/B terminals
   ```

4. **Keep Documentation**
   ```
   Take photos of your wiring setup
   for troubleshooting later
   ```

---

## ✅ Verification Diagram

```
Does your setup match this?

ESP32                           Soil Sensor
│                              │
├─ GPIO17 ──DI──┐             │
├─ GPIO16 ──RO──┤ RS485 ──A───┼──(Yellow)
├─ GPIO21 ──DE──┤ Module ──B───┼──(Blue)
├─ GPIO21 ──RE──┤             │
├─ 5V ─────VCC──┤             │
└─ GND ────GND──┴─────GND─────┼──(Black)
                               │
                        (Brown) = 5V from module VCC
```

**If YES** ✅ → Proceed to code upload  
**If NO** ❌ → Check wiring again

---

**Last Updated:** February 7, 2026  
**Version:** 1.0 - Complete Wiring Guide

