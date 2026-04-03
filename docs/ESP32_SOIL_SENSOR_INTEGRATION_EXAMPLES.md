# ESP32 Soil Sensor Integration Examples

This document provides code examples for integrating real sensors with your ESP32 Soil Sensor UI.

---

## Overview

The main code uses hardcoded test values. This document shows how to:
1. Read from actual soil sensors
2. Integrate multiple sensor types
3. Handle sensor data acquisition
4. Update the display with real readings

---

## Example 1: Analog Soil Moisture Sensor

### Hardware
- Capacitive Soil Moisture Sensor (e.g., DFRobot)
- Output: Analog voltage (0-3.3V)
- Pin: GPIO 35 (ADC)

### Code Integration

Add this to your sketch:

```cpp
// ============================================================================
// SOIL MOISTURE SENSOR - Analog Input
// ============================================================================

#define MOISTURE_SENSOR_PIN 35  // ADC1_CH7

/**
 * Read soil moisture from analog sensor
 * Returns percentage (0-100%)
 */
float readMoistureSensor() {
  // Read analog value (0-4095 for 12-bit ADC)
  int rawValue = analogRead(MOISTURE_SENSOR_PIN);
  
  // Convert to percentage
  // Adjust these values based on your sensor calibration:
  // DRY (4095) = 0%
  // WET (0) = 100%
  int dryValue = 4095;    // Value when completely dry
  int wetValue = 1300;    // Value when saturated with water
  
  float moisture = map(rawValue, dryValue, wetValue, 0, 100);
  
  // Constrain to 0-100%
  moisture = constrain(moisture, 0, 100);
  
  Serial.print("Moisture raw: ");
  Serial.print(rawValue);
  Serial.print(" | %: ");
  Serial.println(moisture);
  
  return moisture;
}

// In loop(), update every 10 seconds:
unsigned long lastMoistureRead = 0;
void loop() {
  // ... existing code ...
  
  if (millis() - lastMoistureRead > 10000) {
    soilData.moisture = readMoistureSensor();
    drawUI();  // Update display
    lastMoistureRead = millis();
  }
}
```

### Calibration

To calibrate your sensor:
1. Place sensor in completely dry soil, record analog value (dryValue)
2. Place sensor in wet soil, record analog value (wetValue)
3. Update the code with these values

---

## Example 2: DHT22 Temperature & Humidity Sensor

### Hardware
- DHT22 Temperature/Humidity Sensor
- Pin: GPIO 14
- Library: DHT sensor library (Adafruit)

### Installation

Install in Arduino IDE:
Sketch → Include Library → Manage Libraries → Search "DHT sensor library" → Install

### Code Integration

```cpp
#include <DHT.h>

// ============================================================================
// DHT22 TEMPERATURE SENSOR
// ============================================================================

#define DHT_PIN 14
#define DHT_TYPE DHT22  // DHT22 sensor type
DHT dht(DHT_PIN, DHT_TYPE);

/**
 * Initialize DHT sensor
 */
void initDHT() {
  dht.begin();
  Serial.println("DHT22 sensor initialized");
}

/**
 * Read temperature from DHT22
 * Returns temperature in Celsius
 */
float readTemperature() {
  float temp = dht.readTemperature();  // Celsius
  
  // Check if read was successful
  if (isnan(temp)) {
    Serial.println("DHT22 temperature read failed!");
    return soilData.temperature;  // Return previous value
  }
  
  Serial.print("Temperature: ");
  Serial.print(temp);
  Serial.println("C");
  
  return temp;
}

/**
 * Read humidity from DHT22
 * (Optional - not required for current UI)
 */
float readHumidity() {
  float humidity = dht.readHumidity();
  
  if (isnan(humidity)) {
    Serial.println("DHT22 humidity read failed!");
    return 0.0;
  }
  
  Serial.print("Humidity: ");
  Serial.print(humidity);
  Serial.println("%");
  
  return humidity;
}

// Add to setup():
void setup() {
  // ... existing setup code ...
  initDHT();
}

// Add to loop():
unsigned long lastTempRead = 0;
void loop() {
  // ... existing code ...
  
  if (millis() - lastTempRead > 10000) {  // Read every 10 seconds
    soilData.temperature = readTemperature();
    drawUI();
    lastTempRead = millis();
  }
}
```

---

## Example 3: NPK (Nitrogen, Phosphorus, Potassium) Sensor with Serial Interface

### Hardware
- Serial NPK Sensor (e.g., Gravity NPK sensor)
- Pins: RX=GPIO 16, TX=GPIO 17
- Baud: 9600

### Code Integration

```cpp
#include <HardwareSerial.h>

// ============================================================================
// NPK SENSOR - Serial Interface
// ============================================================================

// Create second serial port for sensor (ESP32 has 3 UART ports)
HardwareSerial sensorSerial(1);  // UART1

#define NPK_RX_PIN 16
#define NPK_TX_PIN 17
#define NPK_BAUD 9600

/**
 * Initialize NPK sensor communication
 */
void initNPKSensor() {
  sensorSerial.begin(NPK_BAUD, SERIAL_8N1, NPK_RX_PIN, NPK_TX_PIN);
  Serial.println("NPK sensor initialized");
}

/**
 * Request data from NPK sensor and parse response
 * Typical response format: 0x01 0x03 0x00 0x1E 0x00 0x03 ...
 */
void readNPKData() {
  // Command to request NPK values (varies by sensor model)
  byte command[] = {0x01, 0x03, 0x00, 0x1E, 0x00, 0x03, 0xE4, 0x04};
  
  // Clear buffer
  while (sensorSerial.available()) {
    sensorSerial.read();
  }
  
  // Send command
  sensorSerial.write(command, sizeof(command));
  
  // Wait for response
  delay(100);
  
  // Read response
  if (sensorSerial.available()) {
    byte data[11] = {0};
    int index = 0;
    
    while (sensorSerial.available() && index < 11) {
      data[index++] = sensorSerial.read();
    }
    
    // Parse NPK values (this format is for gravity NPK sensor)
    // Check documentation for your specific sensor
    if (index >= 9) {
      soilData.nitrogen = (data[3] << 8) | data[4];
      soilData.phosphorus = (data[5] << 8) | data[6];
      soilData.potassium = (data[7] << 8) | data[8];
      
      Serial.print("NPK read: N=");
      Serial.print(soilData.nitrogen);
      Serial.print(" P=");
      Serial.print(soilData.phosphorus);
      Serial.print(" K=");
      Serial.println(soilData.potassium);
    }
  } else {
    Serial.println("NPK sensor: No response");
  }
}

// Add to setup():
void setup() {
  // ... existing setup code ...
  initNPKSensor();
}

// Add to loop():
unsigned long lastNPKRead = 0;
void loop() {
  // ... existing code ...
  
  if (millis() - lastNPKRead > 30000) {  // Read every 30 seconds
    readNPKData();
    drawUI();
    lastNPKRead = millis();
  }
}
```

---

## Example 4: Analog pH Sensor

### Hardware
- Analog pH Sensor (0-3.3V output)
- Pin: GPIO 34 (ADC)
- Sensor outputs voltage proportional to pH

### Code Integration

```cpp
// ============================================================================
// PH SENSOR - Analog Input
// ============================================================================

#define PH_SENSOR_PIN 34  // ADC1_CH6

/**
 * Read pH from analog sensor
 * 
 * Calibration:
 * - Sensor output at pH 7.0: typically ~1.65V (2048 ADC)
 * - Sensor output changes ~59mV per pH unit
 */
float readPHSensor() {
  // Read analog value
  int rawValue = analogRead(PH_SENSOR_PIN);
  
  // Convert ADC value to voltage (0-3.3V = 0-4095)
  float voltage = (rawValue / 4095.0) * 3.3;
  
  // Convert voltage to pH
  // Typical sensor: pH = 7 + (1.65 - voltage) / 0.059
  // Adjust calibration point and slope based on your sensor
  float calibrationVoltage = 1.65;  // Voltage at pH 7.0
  float slope = 0.059;               // mV per pH unit
  
  float pH = 7.0 + (calibrationVoltage - voltage) / slope;
  
  // Typical pH range: 0-14
  pH = constrain(pH, 0, 14);
  
  Serial.print("pH raw ADC: ");
  Serial.print(rawValue);
  Serial.print(" | Voltage: ");
  Serial.print(voltage);
  Serial.print("V | pH: ");
  Serial.println(pH, 2);
  
  return pH;
}

// Add to loop():
unsigned long lastPHRead = 0;
void loop() {
  // ... existing code ...
  
  if (millis() - lastPHRead > 10000) {
    soilData.pH = readPHSensor();
    drawUI();
    lastPHRead = millis();
  }
}
```

### pH Sensor Calibration

To calibrate:
1. Use pH 7.0 buffer solution, record voltage
2. Use pH 4.0 buffer solution, record voltage
3. Calculate slope: (voltage_pH7 - voltage_pH4) / (7 - 4)
4. Update calibrationVoltage and slope in code

---

## Example 5: Complete Sensor Integration (All Sensors Combined)

### Full Example Code

Add this complete sensor integration to your main sketch:

```cpp
// ============================================================================
// COMPLETE SENSOR INTEGRATION
// ============================================================================

// Sensor read intervals (in milliseconds)
#define MOISTURE_INTERVAL 10000    // 10 seconds
#define TEMP_INTERVAL 10000        // 10 seconds
#define NPK_INTERVAL 30000         // 30 seconds
#define PH_INTERVAL 10000          // 10 seconds

// Last read times
unsigned long lastMoistureRead = 0;
unsigned long lastTempRead = 0;
unsigned long lastNPKRead = 0;
unsigned long lastPHRead = 0;

/**
 * Initialize all sensors
 */
void initAllSensors() {
  // Configure ADC for better accuracy
  analogSetWidth(12);        // 12-bit ADC resolution
  analogSetAttenuation(ADC_11db);  // 3.3V range
  
  Serial.println("=== Initializing All Sensors ===");
  
  // Initialize sensors
  initDHT();
  initNPKSensor();
  
  Serial.println("All sensors initialized");
}

/**
 * Main sensor update function
 * Call from loop() to update all sensors with proper intervals
 */
void updateAllSensors() {
  unsigned long currentTime = millis();
  
  // Update moisture sensor
  if (currentTime - lastMoistureRead > MOISTURE_INTERVAL) {
    soilData.moisture = readMoistureSensor();
    lastMoistureRead = currentTime;
  }
  
  // Update temperature sensor
  if (currentTime - lastTempRead > TEMP_INTERVAL) {
    soilData.temperature = readTemperature();
    lastTempRead = currentTime;
  }
  
  // Update pH sensor
  if (currentTime - lastPHRead > PH_INTERVAL) {
    soilData.pH = readPHSensor();
    lastPHRead = currentTime;
  }
  
  // Update NPK sensor (less frequently)
  if (currentTime - lastNPKRead > NPK_INTERVAL) {
    readNPKData();
    lastNPKRead = currentTime;
  }
  
  // Redraw display when any sensor updates
  drawUI();
}

// Modify setup():
void setup() {
  Serial.begin(115200);
  delay(1000);
  
  Serial.println("\n\n=== ESP32 Soil Sensor UI Starting ===");
  
  // Initialize display
  tft.begin();
  tft.setRotation(0);
  tft.fillScreen(BLACK);
  
  // Initialize touch
  if (!ts.begin()) {
    Serial.println("ERROR: Touch screen initialization failed!");
  }
  ts.setRotation(0);
  
  // Initialize Bluetooth
  if (!SerialBT.begin("ESP32_SOIL_SENSOR")) {
    Serial.println("ERROR: Bluetooth initialization failed!");
  }
  
  // Initialize all sensors
  initAllSensors();
  
  // Draw initial UI
  drawUI();
  
  Serial.println("=== Setup Complete ===\n");
}

// Modify loop():
void loop() {
  // Handle Bluetooth
  handleBluetooth();
  
  // Handle touch
  handleTouchInput();
  
  // Update button highlight
  updateButtonHighlight();
  
  // Update all sensors
  updateAllSensors();
  
  delay(50);
}
```

---

## Example 6: I2C Sensors (BME680 - Temperature, Humidity, Pressure)

### Hardware
- BME680 Sensor
- SDA: GPIO 21
- SCL: GPIO 22
- Library: Adafruit BME680

### Code Integration

```cpp
#include <Adafruit_BME680.h>

// ============================================================================
// BME680 SENSOR - I2C Interface
// ============================================================================

Adafruit_BME680 bme680;  // I2C address 0x77

/**
 * Initialize BME680 sensor
 */
void initBME680() {
  if (!bme680.begin(0x77)) {  // Default I2C address
    Serial.println("ERROR: BME680 sensor not found!");
    return;
  }
  
  // Set up BME680 for soil monitoring
  bme680.setTemperatureOversampling(BME680_OS_8X);
  bme680.setHumidityOversampling(BME680_OS_2X);
  bme680.setPressureOversampling(BME680_OS_4X);
  bme680.setIIRFilterSize(BME680_FILTER_SIZE_3);
  bme680.setGasHeater(320, 150);  // 320°C for 150ms
  
  Serial.println("BME680 sensor initialized");
}

/**
 * Read data from BME680
 */
void readBME680() {
  if (!bme680.performReading()) {
    Serial.println("BME680: Failed to perform reading");
    return;
  }
  
  // Update temperature
  soilData.temperature = bme680.temperature;
  
  Serial.print("Temperature: ");
  Serial.print(soilData.temperature);
  Serial.print("°C | Humidity: ");
  Serial.print(bme680.humidity);
  Serial.print("% | Pressure: ");
  Serial.print(bme680.pressure / 100.0);
  Serial.println("hPa");
}
```

---

## Example 7: Sensor Error Handling

### Robust Sensor Reading with Error Handling

```cpp
/**
 * Read sensor with error handling and fallback
 */
float readSensorWithFallback(
    float (*sensorFunction)(),
    float fallbackValue,
    const char* sensorName) {
  
  float value = sensorFunction();
  
  // Check for invalid readings
  if (isnan(value) || isinf(value)) {
    Serial.print(sensorName);
    Serial.println(" returned invalid value!");
    return fallbackValue;  // Use last known good value
  }
  
  return value;
}

// Usage in loop:
soilData.temperature = readSensorWithFallback(
  readTemperature,
  soilData.temperature,  // Keep previous value if read fails
  "Temperature Sensor"
);
```

---

## Example 8: Averaging Multiple Readings

### Smooth Sensor Data with Averaging

```cpp
/**
 * Simple moving average filter
 */
class SensorFilter {
private:
  float readings[5];
  int index;
  
public:
  SensorFilter() : index(0) {
    for (int i = 0; i < 5; i++) {
      readings[i] = 0;
    }
  }
  
  float addReading(float value) {
    readings[index] = value;
    index = (index + 1) % 5;
    
    float sum = 0;
    for (int i = 0; i < 5; i++) {
      sum += readings[i];
    }
    return sum / 5.0;  // Return average
  }
};

// Create filters for each sensor
SensorFilter moistureFilter;
SensorFilter tempFilter;
SensorFilter pHFilter;

// Use in loop:
float rawMoisture = readMoistureSensor();
soilData.moisture = moistureFilter.addReading(rawMoisture);
```

---

## Example 9: EEPROM Data Logging

### Save sensor readings to EEPROM

```cpp
#include <EEPROM.h>

#define EEPROM_SIZE 512
#define DATA_START_ADDR 0

/**
 * Save current soil data to EEPROM
 */
void saveDataToEEPROM() {
  EEPROM.putFloat(DATA_START_ADDR, soilData.nitrogen);
  EEPROM.putFloat(DATA_START_ADDR + 4, soilData.phosphorus);
  EEPROM.putFloat(DATA_START_ADDR + 8, soilData.potassium);
  EEPROM.putFloat(DATA_START_ADDR + 12, soilData.pH);
  EEPROM.putFloat(DATA_START_ADDR + 16, soilData.temperature);
  EEPROM.putFloat(DATA_START_ADDR + 20, soilData.moisture);
  EEPROM.commit();
  
  Serial.println("Data saved to EEPROM");
}

/**
 * Load soil data from EEPROM
 */
void loadDataFromEEPROM() {
  soilData.nitrogen = EEPROM.getFloat(DATA_START_ADDR);
  soilData.phosphorus = EEPROM.getFloat(DATA_START_ADDR + 4);
  soilData.potassium = EEPROM.getFloat(DATA_START_ADDR + 8);
  soilData.pH = EEPROM.getFloat(DATA_START_ADDR + 12);
  soilData.temperature = EEPROM.getFloat(DATA_START_ADDR + 16);
  soilData.moisture = EEPROM.getFloat(DATA_START_ADDR + 20);
  
  Serial.println("Data loaded from EEPROM");
}

// In setup():
EEPROM.begin(EEPROM_SIZE);
loadDataFromEEPROM();

// When saving button is pressed:
void handleButtonPress() {
  if (!buttonPressed) {
    buttonPressed = true;
    buttonPressTime = millis();
    dataReadyToSend = true;
    
    // Save data to EEPROM
    saveDataToEEPROM();
    
    drawUI();
  }
}
```

---

## Example 10: ADC Calibration for Analog Sensors

### Precise ADC Reading Calibration

```cpp
/**
 * Calibrate analog sensor readings
 * Takes readings while pairing them with known reference values
 */
class ADCCalibrator {
public:
  float point1_raw, point1_value;
  float point2_raw, point2_value;
  
  ADCCalibrator() : point1_raw(0), point1_value(0),
                    point2_raw(0), point2_value(0) {}
  
  void calibratePoint1(int raw, float value) {
    point1_raw = raw;
    point1_value = value;
    Serial.println("Point 1 calibrated");
  }
  
  void calibratePoint2(int raw, float value) {
    point2_raw = raw;
    point2_value = value;
    Serial.println("Point 2 calibrated");
  }
  
  float convert(int rawValue) {
    if (point1_raw == point2_raw) return 0;  // Prevent division by zero
    
    float slope = (point2_value - point1_value) / (point2_raw - point1_raw);
    float intercept = point1_value - slope * point1_raw;
    
    return slope * rawValue + intercept;
  }
};

// Usage:
ADCCalibrator moistureCalibrator;

void setup() {
  // After hardware is ready, calibrate:
  // 1. Place sensor in dry soil, call:
  //    moistureCalibrator.calibratePoint1(analogRead(MOISTURE_SENSOR_PIN), 0);
  
  // 2. Place sensor in wet soil, call:
  //    moistureCalibrator.calibratePoint2(analogRead(MOISTURE_SENSOR_PIN), 100);
}

// Then use in readings:
float readMoistureSensor() {
  int rawValue = analogRead(MOISTURE_SENSOR_PIN);
  return moistureCalibrator.convert(rawValue);
}
```

---

## Wiring Diagram for All Sensors

```
ESP32              Sensors
================   ===================================
GND ────────────── GND (all sensors)
3.3V ───────────── VCC (all sensors)

GPIO 35 ←────────── Moisture Sensor (analog)
GPIO 34 ←────────── pH Sensor (analog)

GPIO 14 ←────────── DHT22 (data line)

GPIO 16 ←────────── NPK Sensor RX
GPIO 17 ────────→─ NPK Sensor TX

GPIO 21 ←────────── BME680 SDA (I2C)
GPIO 22 ────────→─ BME680 SCL (I2C)

(LCD and Touch connections remain as before)
```

---

## Testing Sensor Integration

### Step 1: Test Each Sensor Individually
1. Comment out all but one sensor read function
2. Verify readings appear in Serial Monitor
3. Check values are realistic

### Step 2: Test with updateAllSensors()
1. Uncomment all sensor read functions
2. Call updateAllSensors() in loop()
3. Verify all values update with correct intervals

### Step 3: Verify Bluetooth Transmission
1. Press button on display
2. Send "READ" via Bluetooth
3. Verify response includes updated sensor values

### Step 4: Monitor for Data Consistency
1. Run for 1+ hour
2. Check Serial Monitor for errors
3. Verify display updates smoothly
4. Ensure no crashes or hangs

---

## Troubleshooting Sensor Issues

| Sensor | Problem | Solution |
|--------|---------|----------|
| DHT22 | Always returns NaN | Check wiring, pull-up resistor, try different GPIO |
| Moisture | Wrong percentage | Calibrate dry/wet values |
| pH | Drifting values | Calibrate with buffer solutions |
| NPK (Serial) | No response | Check baud rate, RX/TX pins |
| BME680 | Not detected | Check I2C address (0x77 or 0x76) |
| Any analog | Noisy readings | Add 10μF capacitor to sensor output |

---

## Additional Resources

- **Sensor Datasheets**: Check manufacturer documentation for specific pin requirements
- **Library Documentation**: Refer to Arduino library documentation for examples
- **Calibration**: Most analog sensors require calibration for accuracy
- **Power Supply**: Use regulated 3.3V supply for stability

---

Version: 1.0
Last Updated: December 29, 2025

