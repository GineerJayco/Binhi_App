/*
   ESP32 SOIL SENSOR DISPLAY - COMPLETE EXAMPLE WITH SENSOR INTEGRATION

   This is an extended version showing how to integrate actual sensors.
   Includes analog sensor reading and timing examples.

   Sensor Setup Example:
   - NPK values: Analog sensor on ADC pins
   - Temperature: LM35 analog sensor
   - Moisture: Capacitive soil moisture sensor
   - pH: pH sensor with analog output
*/

#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_ILI9341.h>
#include <XPT2046_Touchscreen.h>
#include <BluetoothSerial.h>
#include <Fonts/FreeSansBold24pt7b.h>
#include <Fonts/FreeSansBold12pt7b.h>

// ============================================================================
// DISPLAY & TOUCH SETUP (Same as before)
// ============================================================================

#define TFT_CS   15
#define TFT_RST  4
#define TFT_DC   2
#define TFT_MOSI 23
#define TFT_SCK  18
#define TFT_MISO 19

Adafruit_ILI9341 tft = Adafruit_ILI9341(TFT_CS, TFT_DC, TFT_RST);

#define TOUCH_CS 5
#define TOUCH_IRQ 36

XPT2046_Touchscreen ts(TOUCH_CS, TOUCH_IRQ);

#define TS_MINX 150
#define TS_MAXX 3900
#define TS_MINY 150
#define TS_MAXY 3900

// ============================================================================
// BLUETOOTH SETUP
// ============================================================================

BluetoothSerial SerialBT;
#define DEVICE_NAME "ESP32_SOIL_SENSOR"

// ============================================================================
// SOIL DATA STRUCTURE
// ============================================================================

struct SoilData {
  float nitrogen;      // mg/kg
  float phosphorus;    // mg/kg
  float potassium;     // mg/kg
  float pH;            // 0-14
  float temperature;   // °C
  float moisture;      // % (0-100)
};

SoilData soilData = {
  12.0, 7.0, 9.0, 6.5, 29.4, 62.0
};

// ============================================================================
// SENSOR PIN DEFINITIONS (CUSTOMIZE THESE)
// ============================================================================

// Analog input pins for sensors
#define SENSOR_NITROGEN A0      // Pin 36 (ADC1_0)
#define SENSOR_PHOSPHORUS A3    // Pin 39 (ADC1_3)
#define SENSOR_POTASSIUM A6     // Pin 34 (ADC1_6)
#define SENSOR_PH A5            // Pin 35 (ADC1_5)
#define SENSOR_TEMPERATURE A4   // Pin 32 (ADC1_4) - LM35 sensor
#define SENSOR_MOISTURE A7      // Pin 33 (ADC1_7)

// ============================================================================
// SENSOR CALIBRATION VALUES (TUNE THESE FOR YOUR SENSORS)
// ============================================================================

// NPK Sensor calibration
// These values convert raw 0-4095 ADC to mg/kg
#define NPK_CALIBRATION_FACTOR 0.06250  // 255 / 4095
#define NPK_MAX_VALUE 300               // Maximum expected mg/kg

// Temperature sensor (LM35)
// Output: 10mV per °C
#define TEMP_VOLTAGE_DIVIDER (3.3 / 4095.0)  // ADC to voltage
#define TEMP_LM35_FACTOR 100.0               // mV to °C conversion

// pH Sensor calibration
// Typical: 0-14 pH maps to 0-3.3V
#define PH_CALIBRATION_FACTOR (14.0 / 4095.0)
#define PH_OFFSET 0.0  // Adjust if sensor has offset

// Moisture sensor calibration
// Capacitive sensor: drier = higher reading, wet = lower reading
#define MOISTURE_DRY_READING 4095   // Reading when completely dry
#define MOISTURE_WET_READING 2000   // Reading when completely wet
#define MOISTURE_MAX 100.0          // Maximum moisture percentage

// ============================================================================
// UI STATE
// ============================================================================

bool dataReadyToSend = false;
unsigned long lastButtonPressTime = 0;
const unsigned long BUTTON_HIGHLIGHT_DURATION = 300;

// Button layout
#define SAVE_BUTTON_X 50
#define SAVE_BUTTON_Y 280
#define SAVE_BUTTON_W 140
#define SAVE_BUTTON_H 30

// Colors
#define COLOR_BACKGROUND ILI9341_BLACK
#define COLOR_TITLE      ILI9341_CYAN
#define COLOR_LABEL      ILI9341_WHITE
#define COLOR_VALUE      ILI9341_YELLOW
#define COLOR_BUTTON     ILI9341_DARKGREEN
#define COLOR_BUTTON_PRESS ILI9341_GREEN
#define COLOR_READY_TEXT ILI9341_MAGENTA

// ============================================================================
// SENSOR READING TIMING
// ============================================================================

unsigned long lastSensorReadTime = 0;
const unsigned long SENSOR_READ_INTERVAL = 2000;  // Read sensors every 2 seconds

// Moving average for smoothing sensor readings
#define MOVING_AVG_SIZE 5
float nitro_avg[MOVING_AVG_SIZE] = {0};
float phos_avg[MOVING_AVG_SIZE] = {0};
float pot_avg[MOVING_AVG_SIZE] = {0};
float ph_avg[MOVING_AVG_SIZE] = {0};
float temp_avg[MOVING_AVG_SIZE] = {0};
float moist_avg[MOVING_AVG_SIZE] = {0};
uint8_t avg_index = 0;

// ============================================================================
// SETUP
// ============================================================================

void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println("\n\n=== ESP32 Soil Sensor Display (With Sensor Integration) ===");

  // Configure analog read resolution
  // ESP32 ADC is 12-bit (0-4095) by default
  analogSetAttenuation(ADC_11db);  // Full range: 0-3.3V

  // Initialize TFT display
  tft.begin();
  tft.setRotation(0);
  tft.fillScreen(COLOR_BACKGROUND);

  Serial.println("TFT Display initialized");

  // Initialize touch screen
  if (!ts.begin()) {
    Serial.println("ERROR: Touch screen not found!");
  } else {
    Serial.println("Touch screen initialized");
  }

  ts.setRotation(0);

  // Initialize Bluetooth
  SerialBT.begin(DEVICE_NAME);
  Serial.println("Bluetooth initialized: " + String(DEVICE_NAME));

  // Draw initial UI
  drawSoilDataDisplay();

  Serial.println("Setup complete - reading sensors...");
}

// ============================================================================
// MAIN LOOP
// ============================================================================

void loop() {
  // Check for touch input
  handleTouchInput();

  // Check for Bluetooth commands
  handleBluetoothInput();

  // Update sensors at regular intervals (non-blocking)
  if (millis() - lastSensorReadTime >= SENSOR_READ_INTERVAL) {
    lastSensorReadTime = millis();
    readAllSensors();  // Read sensors and update display
  }

  // Handle button highlight timing
  if (dataReadyToSend && (millis() - lastButtonPressTime) < BUTTON_HIGHLIGHT_DURATION) {
    // Keep pressed state
  } else if (dataReadyToSend && (millis() - lastButtonPressTime) >= BUTTON_HIGHLIGHT_DURATION) {
    drawSaveButton(false);
  }

  delay(50);
}

// ============================================================================
// SENSOR READING FUNCTIONS
// ============================================================================

void readAllSensors() {
  Serial.println("\n--- Reading Sensors ---");

  // Read each sensor
  float n = readNitrogen();
  float p = readPhosphorus();
  float k = readPotassium();
  float ph = readPH();
  float temp = readTemperature();
  float moist = readMoisture();

  // Update display with new values
  updateSoilData(n, p, k, ph, temp, moist);

  Serial.println("--- Sensors Updated ---\n");
}

// ============================================================================
// INDIVIDUAL SENSOR READING FUNCTIONS
// ============================================================================

float readNitrogen() {
  /*
    Read nitrogen value from analog sensor.

    Calibration:
    - Raw ADC range: 0-4095
    - Nitrogen range: 0-300 mg/kg
    - Linear conversion
  */

  uint16_t raw = analogRead(SENSOR_NITROGEN);

  // Apply moving average (smoothing)
  nitro_avg[avg_index] = (float)raw * NPK_CALIBRATION_FACTOR;

  // Calculate average
  float average = 0;
  for (int i = 0; i < MOVING_AVG_SIZE; i++) {
    average += nitro_avg[i];
  }
  average /= MOVING_AVG_SIZE;

  // Constrain to valid range
  average = constrain(average, 0, NPK_MAX_VALUE);

  Serial.print("Nitrogen: Raw=");
  Serial.print(raw);
  Serial.print(" → Value=");
  Serial.print(average);
  Serial.println(" mg/kg");

  return average;
}

float readPhosphorus() {
  /*
    Read phosphorus value from analog sensor.
    Same calibration as nitrogen.
  */

  uint16_t raw = analogRead(SENSOR_PHOSPHORUS);

  phos_avg[avg_index] = (float)raw * NPK_CALIBRATION_FACTOR;

  float average = 0;
  for (int i = 0; i < MOVING_AVG_SIZE; i++) {
    average += phos_avg[i];
  }
  average /= MOVING_AVG_SIZE;

  average = constrain(average, 0, NPK_MAX_VALUE);

  Serial.print("Phosphorus: Raw=");
  Serial.print(raw);
  Serial.print(" → Value=");
  Serial.print(average);
  Serial.println(" mg/kg");

  return average;
}

float readPotassium() {
  /*
    Read potassium value from analog sensor.
    Same calibration as nitrogen.
  */

  uint16_t raw = analogRead(SENSOR_POTASSIUM);

  pot_avg[avg_index] = (float)raw * NPK_CALIBRATION_FACTOR;

  float average = 0;
  for (int i = 0; i < MOVING_AVG_SIZE; i++) {
    average += pot_avg[i];
  }
  average /= MOVING_AVG_SIZE;

  average = constrain(average, 0, NPK_MAX_VALUE);

  Serial.print("Potassium: Raw=");
  Serial.print(raw);
  Serial.print(" → Value=");
  Serial.print(average);
  Serial.println(" mg/kg");

  return average;
}

float readTemperature() {
  /*
    Read temperature from LM35 analog sensor.

    LM35 output: 10mV per °C
    Example: 250mV = 25°C

    Calibration:
    - Raw ADC range: 0-4095 (0-3.3V)
    - LM35 output: 10mV per °C
    - Formula: Temp = (ADC * 3.3 / 4095) * 100
  */

  uint16_t raw = analogRead(SENSOR_TEMPERATURE);

  // Convert ADC to voltage
  float voltage = raw * TEMP_VOLTAGE_DIVIDER;

  // Convert voltage to temperature (10mV per °C)
  float temp = voltage * 100.0;

  // Apply moving average
  temp_avg[avg_index] = temp;

  float average = 0;
  for (int i = 0; i < MOVING_AVG_SIZE; i++) {
    average += temp_avg[i];
  }
  average /= MOVING_AVG_SIZE;

  // Constrain to reasonable range
  average = constrain(average, -20, 60);

  Serial.print("Temperature: Raw=");
  Serial.print(raw);
  Serial.print(" (");
  Serial.print(voltage);
  Serial.print("V) → Value=");
  Serial.print(average);
  Serial.println(" °C");

  return average;
}

float readPH() {
  /*
    Read pH from analog pH sensor.

    Typical calibration:
    - pH 0 → 0V
    - pH 7 → 1.65V (midpoint)
    - pH 14 → 3.3V

    Linear conversion: pH = ADC * (14 / 4095)

    Adjust PH_OFFSET if your sensor has calibration offset.
  */

  uint16_t raw = analogRead(SENSOR_PH);

  // Linear conversion
  float pH = (float)raw * PH_CALIBRATION_FACTOR + PH_OFFSET;

  // Apply moving average
  ph_avg[avg_index] = pH;

  float average = 0;
  for (int i = 0; i < MOVING_AVG_SIZE; i++) {
    average += ph_avg[i];
  }
  average /= MOVING_AVG_SIZE;

  // Constrain to valid pH range
  average = constrain(average, 0, 14);

  Serial.print("pH: Raw=");
  Serial.print(raw);
  Serial.print(" → Value=");
  Serial.println(average, 1);

  return average;
}

float readMoisture() {
  /*
    Read soil moisture from capacitive sensor.

    Capacitive sensors work opposite to resistive:
    - Dry soil: Higher ADC reading (~4095)
    - Wet soil: Lower ADC reading (~2000)

    Conversion: Moisture% = 100 - ((ADC - WET) / (DRY - WET)) * 100
    Simplified: Moisture% = ((DRY - ADC) / (DRY - WET)) * 100
  */

  uint16_t raw = analogRead(SENSOR_MOISTURE);

  // Convert to percentage
  // Formula: (DRY_READING - current) / (DRY - WET) * 100
  float moisture = ((float)(MOISTURE_DRY_READING - raw) /
                    (MOISTURE_DRY_READING - MOISTURE_WET_READING)) * 100.0;

  // Constrain to 0-100%
  moisture = constrain(moisture, 0, 100);

  // Apply moving average
  moist_avg[avg_index] = moisture;

  float average = 0;
  for (int i = 0; i < MOVING_AVG_SIZE; i++) {
    average += moist_avg[i];
  }
  average /= MOVING_AVG_SIZE;

  // Ensure within valid range
  average = constrain(average, 0, 100);

  Serial.print("Moisture: Raw=");
  Serial.print(raw);
  Serial.print(" → Value=");
  Serial.print(average);
  Serial.println(" %");

  return average;
}

// ============================================================================
// UPDATE MOVING AVERAGE INDEX
// ============================================================================

void updateAverageIndex() {
  avg_index++;
  if (avg_index >= MOVING_AVG_SIZE) {
    avg_index = 0;
  }
}

// ============================================================================
// TOUCH HANDLING (Same as before)
// ============================================================================

void handleTouchInput() {
  TS_Point p = ts.getPoint();

  if (p.z > 0) {
    uint16_t x = map(p.x, TS_MINX, TS_MAXX, 0, tft.width());
    uint16_t y = map(p.y, TS_MINY, TS_MAXY, 0, tft.height());

    Serial.print("Touch: X=");
    Serial.print(x);
    Serial.print(", Y=");
    Serial.println(y);

    if (isTouchInButton(x, y)) {
      handleSaveButtonPress();
    }
  }
}

bool isTouchInButton(uint16_t x, uint16_t y) {
  return (x >= SAVE_BUTTON_X && x <= (SAVE_BUTTON_X + SAVE_BUTTON_W)) &&
         (y >= SAVE_BUTTON_Y && y <= (SAVE_BUTTON_Y + SAVE_BUTTON_H));
}

void handleSaveButtonPress() {
  Serial.println("SAVE DATA button pressed!");

  dataReadyToSend = true;
  lastButtonPressTime = millis();

  drawSaveButton(true);
  displayReadyStatus(true);
}

// ============================================================================
// UI DRAWING (Same as before)
// ============================================================================

void drawSoilDataDisplay() {
  tft.fillScreen(COLOR_BACKGROUND);
  drawTitle();

  int y_offset = 50;
  int line_spacing = 38;

  drawDataField("N (mg/kg):", soilData.nitrogen, y_offset, 1);
  y_offset += line_spacing;

  drawDataField("P (mg/kg):", soilData.phosphorus, y_offset, 2);
  y_offset += line_spacing;

  drawDataField("K (mg/kg):", soilData.potassium, y_offset, 3);
  y_offset += line_spacing;

  drawDataField("pH Level:", soilData.pH, y_offset, 4);
  y_offset += line_spacing;

  drawDataField("Temp (°C):", soilData.temperature, y_offset, 5);
  y_offset += line_spacing;

  drawDataField("Moisture (%):", soilData.moisture, y_offset, 6);

  drawSaveButton(false);

  if (dataReadyToSend) {
    displayReadyStatus(true);
  }
}

void drawTitle() {
  tft.setFont(&FreeSansBold24pt7b);
  tft.setTextColor(COLOR_TITLE);
  tft.setCursor(10, 35);
  tft.println("SOIL DATA");
  tft.drawLine(0, 45, 240, 45, COLOR_LABEL);
}

void drawDataField(const char* label, float value, int y, uint8_t fieldNum) {
  tft.setFont(&FreeSansBold12pt7b);

  tft.setTextColor(COLOR_LABEL);
  tft.setCursor(20, y);
  tft.print(label);

  tft.setTextColor(COLOR_VALUE);
  tft.setCursor(160, y);

  char valueStr[20];
  if (fieldNum == 4) {
    sprintf(valueStr, "%.1f", value);
  } else if (fieldNum == 5 || fieldNum == 6) {
    sprintf(valueStr, "%.1f", value);
  } else {
    sprintf(valueStr, "%.0f", value);
  }
  tft.print(valueStr);
}

void drawSaveButton(bool pressed) {
  uint16_t buttonColor = pressed ? COLOR_BUTTON_PRESS : COLOR_BUTTON;

  tft.fillRect(SAVE_BUTTON_X, SAVE_BUTTON_Y, SAVE_BUTTON_W, SAVE_BUTTON_H, buttonColor);
  tft.drawRect(SAVE_BUTTON_X, SAVE_BUTTON_Y, SAVE_BUTTON_W, SAVE_BUTTON_H, ILI9341_WHITE);

  tft.setFont(&FreeSansBold12pt7b);
  tft.setTextColor(ILI9341_WHITE);
  tft.setCursor(SAVE_BUTTON_X + 18, SAVE_BUTTON_Y + 22);
  tft.print("SAVE DATA");
}

void displayReadyStatus(bool show) {
  if (show) {
    tft.setFont(&FreeSansBold12pt7b);
    tft.setTextColor(COLOR_READY_TEXT);
    tft.setCursor(40, 250);
    tft.print("READY TO SEND");
    Serial.println("Status: READY TO SEND");
  }
}

// ============================================================================
// BLUETOOTH HANDLING (Same as before)
// ============================================================================

void handleBluetoothInput() {
  if (SerialBT.available()) {
    String command = SerialBT.readStringUntil('\n');
    command.trim();

    Serial.print("Bluetooth received: ");
    Serial.println(command);

    if (command == "READ") {
      handleReadCommand();
    } else {
      SerialBT.println("UNKNOWN_COMMAND");
    }
  }
}

void handleReadCommand() {
  if (dataReadyToSend) {
    sendSoilData();
    dataReadyToSend = false;
    tft.fillRect(30, 245, 180, 30, COLOR_BACKGROUND);
    drawSaveButton(false);
    Serial.println("Data sent via Bluetooth");
  } else {
    SerialBT.println("NO_DATA");
    Serial.println("No data ready");
  }
}

void sendSoilData() {
  char dataStr[100];

  sprintf(dataStr, "NPK=%.0f,%.0f,%.0f;PH=%.1f;TEMP=%.1f;MOIST=%.0f",
          soilData.nitrogen,
          soilData.phosphorus,
          soilData.potassium,
          soilData.pH,
          soilData.temperature,
          soilData.moisture);

  SerialBT.print(dataStr);
  SerialBT.println();

  Serial.print("Sent: ");
  Serial.println(dataStr);
}

// ============================================================================
// UPDATE SOIL DATA (Called from sensor reading)
// ============================================================================

void updateSoilData(float n, float p, float k, float ph, float temp, float moist) {
  soilData.nitrogen = n;
  soilData.phosphorus = p;
  soilData.potassium = k;
  soilData.pH = ph;
  soilData.temperature = temp;
  soilData.moisture = moist;

  drawSoilDataDisplay();

  // Update moving average index
  updateAverageIndex();
}

// ============================================================================
// END OF CODE
// ============================================================================

