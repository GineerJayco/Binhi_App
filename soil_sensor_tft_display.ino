/*
   ESP32 SOIL SENSOR DATA DISPLAY

   This sketch displays soil sensor data on a 3.2" TFT LCD (ILI9341)
   with touch support (XPT2046), Bluetooth Serial communication, and
   RS485 NPK soil sensor integration.

   Hardware:
   - ESP32 Dev Module
   - 3.2" SPI TFT LCD (240x320, ILI9341)
   - XPT2046 Touch Controller
   - RS485 NPK Soil Sensor

   Pin Configuration:
   LCD:
   - VCC -> VIN
   - GND -> GND
   - CS -> GPIO 15
   - RST -> GPIO 4
   - DC -> GPIO 2
   - MOSI -> GPIO 23
   - SCK -> GPIO 18
   - MISO -> GPIO 19
   - LED -> 3.3V

   Touch:
   - CLK -> GPIO 18
   - CS -> GPIO 5
   - DIN -> GPIO 23
   - DO -> GPIO 19

   RS485 Sensor (TTL to RS485 Module):
   - DI (TX) -> GPIO 17
   - RO (RX) -> GPIO 16
   - DE (Direction Enable) -> GPIO 21
   - RE (Receiver Enable) -> GPIO 21 (connected with DE)
   - VCC -> 5V or 3.3V
   - GND -> GND

   Libraries Required:
   - Adafruit_GFX
   - Adafruit_ILI9341
   - XPT2046_Touchscreen
   - BluetoothSerial
*/

#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_ILI9341.h>
#include <XPT2046_Touchscreen.h>
#include <BluetoothSerial.h>
#include <HardwareSerial.h>

// ============================================================================
// DISPLAY SETUP (ILI9341)
// ============================================================================

#define TFT_CS   15
#define TFT_RST  4
#define TFT_DC   2
#define TFT_MOSI 23
#define TFT_SCK  18
#define TFT_MISO 19

// Create display object with hardware SPI
Adafruit_ILI9341 tft = Adafruit_ILI9341(TFT_CS, TFT_DC, TFT_RST);

// ============================================================================
// TOUCH SETUP (XPT2046)
// ============================================================================

#define TOUCH_CS 5
#define TOUCH_IRQ 36  // Touch interrupt pin (optional, can use GPIO 36)

// Create touchscreen object
// Parameters: CS pin, IRQ pin (optional)
XPT2046_Touchscreen ts(TOUCH_CS, TOUCH_IRQ);

// Touch calibration values (these will need to be calibrated for your screen)
// Format: {min_x, max_x, min_y, max_y}
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
// RS485 SENSOR SETUP
// ============================================================================

// RS485 Module pins
#define RS485_RX_PIN 16      // RO (Receiver Output) from RS485 module
#define RS485_TX_PIN 17      // DI (Driver Input) from RS485 module
#define RS485_DE_PIN 21      // DE (Driver Enable) for RS485 module
#define RS485_RE_PIN 21      // RE (Receiver Enable) for RS485 module (same as DE)

// RS485 serial communication
HardwareSerial RS485Serial(1);  // Use UART1 on ESP32
#define RS485_BAUD_RATE 4800

// RS485 sensor query command
// Format: {0x01, 0x03, 0x00, 0x00, 0x00, 0x07, 0x04, 0x08}
// Device ID: 0x01, Function: 0x03 (Read Holding Registers), Start: 0x0000, Count: 0x0007, CRC: 0x0408
const byte RS485_QUERY[] = {0x01, 0x03, 0x00, 0x00, 0x00, 0x07, 0x04, 0x08};
const int RS485_QUERY_SIZE = sizeof(RS485_QUERY);
const int RS485_RESPONSE_SIZE = 19;  // Expected response size

// RS485 sensor reading interval
#define RS485_READ_INTERVAL 5000  // Read sensor every 5 seconds (milliseconds)
unsigned long lastSensorReadTime = 0;

// ============================================================================
// SOIL DATA STRUCTURE
// ============================================================================

struct SoilData {
  float nitrogen;      // Nitrogen (N) in mg/kg
  float phosphorus;    // Phosphorus (P) in mg/kg
  float potassium;     // Potassium (K) in mg/kg
  float pH;            // pH level (0-14)
  float temperature;   // Temperature in Celsius
  float moisture;      // Moisture/Humidity in percentage (0-100)
  float conductivity;  // Electrical conductivity in mS/cm
};

SoilData soilData = {
  12.0,    // Nitrogen
  7.0,     // Phosphorus
  9.0,     // Potassium
  6.5,     // pH
  29.4,    // Temperature
  62.0,    // Moisture
  1250.0   // Conductivity
};

// ============================================================================
// UI STATE VARIABLES
// ============================================================================

bool dataReadyToSend = false;
unsigned long lastButtonPressTime = 0;
const unsigned long BUTTON_HIGHLIGHT_DURATION = 300;  // milliseconds

// Button coordinates and dimensions
#define SAVE_BUTTON_X 40
#define SAVE_BUTTON_Y 260
#define SAVE_BUTTON_W 160
#define SAVE_BUTTON_H 50

// Colors
#define COLOR_BACKGROUND ILI9341_BLACK
#define COLOR_TITLE      ILI9341_CYAN
#define COLOR_LABEL      ILI9341_WHITE
#define COLOR_VALUE      ILI9341_YELLOW
#define COLOR_BUTTON     ILI9341_DARKGREEN
#define COLOR_BUTTON_PRESS ILI9341_GREEN
#define COLOR_READY_TEXT ILI9341_MAGENTA

// ============================================================================
// SETUP
// ============================================================================

void setup() {
  // Initialize Serial for debugging
  Serial.begin(115200);
  delay(1000);

  Serial.println("\n\n=== ESP32 Soil Sensor Display ===");

  // Initialize RS485 serial communication
  RS485Serial.begin(RS485_BAUD_RATE, SERIAL_8N1, RS485_RX_PIN, RS485_TX_PIN);
  Serial.println("RS485 Serial initialized at " + String(RS485_BAUD_RATE) + " baud");

  // Setup RS485 control pins
  pinMode(RS485_DE_PIN, OUTPUT);
  pinMode(RS485_RE_PIN, OUTPUT);
  digitalWrite(RS485_DE_PIN, LOW);  // Initially set to receive mode
  digitalWrite(RS485_RE_PIN, LOW);

  // Initialize TFT display
  tft.begin();
  tft.setRotation(0);  // Portrait mode (240x320)
  tft.fillScreen(COLOR_BACKGROUND);

  Serial.println("TFT Display initialized");

  // Initialize touch screen
  if (!ts.begin()) {
    Serial.println("ERROR: Touch screen not found!");
  } else {
    Serial.println("Touch screen initialized");
  }

  // Set touch screen rotation and calibration values
  ts.setRotation(0);

  // Initialize Bluetooth
  SerialBT.begin(DEVICE_NAME);
  Serial.println("Bluetooth initialized: " + String(DEVICE_NAME));

  // Draw initial UI
  drawSoilDataDisplay();

  Serial.println("Setup complete - waiting for input...");
}

// ============================================================================
// MAIN LOOP
// ============================================================================

void loop() {
  // Check for touch input
  handleTouchInput();

  // Check for Bluetooth commands
  handleBluetoothInput();

  // Read sensor data periodically
  if (millis() - lastSensorReadTime >= RS485_READ_INTERVAL) {
    readRS485Sensor();
    lastSensorReadTime = millis();
  }

  // Redraw UI if button was pressed (for highlight effect)
  if (dataReadyToSend && (millis() - lastButtonPressTime) < BUTTON_HIGHLIGHT_DURATION) {
    // Keep showing pressed state
  } else if (dataReadyToSend && (millis() - lastButtonPressTime) >= BUTTON_HIGHLIGHT_DURATION) {
    // Button press effect has finished
    drawSaveButton(false);
  }

  delay(50);  // Non-blocking: small delay for responsiveness
}

// ============================================================================
// RS485 SENSOR READING
// ============================================================================

void readRS485Sensor() {
  // Clear any remaining data in the buffer
  while (RS485Serial.available()) {
    RS485Serial.read();
  }

  // Set RS485 module to transmit mode
  digitalWrite(RS485_DE_PIN, HIGH);
  digitalWrite(RS485_RE_PIN, HIGH);
  delay(10);

  // Send query command to sensor
  RS485Serial.write(RS485_QUERY, RS485_QUERY_SIZE);
  RS485Serial.flush();  // Wait for transmission to complete

  // Switch RS485 module to receive mode
  digitalWrite(RS485_DE_PIN, LOW);
  digitalWrite(RS485_RE_PIN, LOW);
  delay(10);

  // Wait for sensor response
  unsigned long timeout = millis();
  while (RS485Serial.available() < RS485_RESPONSE_SIZE && (millis() - timeout) < 1000) {
    delay(10);
  }

  // Check if we received the expected number of bytes
  if (RS485Serial.available() >= RS485_RESPONSE_SIZE) {
    byte receivedData[RS485_RESPONSE_SIZE];
    RS485Serial.readBytes(receivedData, RS485_RESPONSE_SIZE);

    // Parse the received data and extract sensor values
    parseRS485Data(receivedData);
  } else {
    Serial.println("ERROR: Incomplete RS485 sensor response");
  }
}

void parseRS485Data(byte* data) {
  // Data format from RS485 sensor:
  // Byte 0: Device ID (0x01)
  // Byte 1: Function (0x03)
  // Byte 2: Byte count (0x0E = 14 bytes)
  // Bytes 3-4: Soil Humidity (16-bit)
  // Bytes 5-6: Soil Temperature (16-bit)
  // Bytes 7-8: Soil Conductivity (16-bit)
  // Bytes 9-10: Soil pH (16-bit)
  // Bytes 11-12: Nitrogen (16-bit)
  // Bytes 13-14: Phosphorus (16-bit)
  // Bytes 15-16: Potassium (16-bit)
  // Bytes 17-18: CRC (2 bytes)

  // Extract 16-bit values (MSB first)
  unsigned int rawMoisture = (data[3] << 8) | data[4];
  unsigned int rawTemperature = (data[5] << 8) | data[6];
  unsigned int rawConductivity = (data[7] << 8) | data[8];
  unsigned int rawPH = (data[9] << 8) | data[10];
  unsigned int rawNitrogen = (data[11] << 8) | data[12];
  unsigned int rawPhosphorus = (data[13] << 8) | data[14];
  unsigned int rawPotassium = (data[15] << 8) | data[16];

  // Convert raw values to physical units
  // Humidity: divide by 10
  // Temperature: divide by 10
  // Conductivity: as is (in µS/cm, may need adjustment for mS/cm)
  // pH: divide by 10
  // NPK: as is (in mg/kg)

  soilData.moisture = (float)rawMoisture / 10.0;
  soilData.temperature = (float)rawTemperature / 10.0;
  soilData.conductivity = (float)rawConductivity;
  soilData.pH = (float)rawPH / 10.0;
  soilData.nitrogen = (float)rawNitrogen;
  soilData.phosphorus = (float)rawPhosphorus;
  soilData.potassium = (float)rawPotassium;

  // Redraw display with new data
  drawSoilDataDisplay();

  // Log data to serial
  Serial.println("RS485 Sensor Data Updated:");
  Serial.print("  Moisture: "); Serial.print(soilData.moisture); Serial.println("%");
  Serial.print("  Temperature: "); Serial.print(soilData.temperature); Serial.println("°C");
  Serial.print("  Conductivity: "); Serial.print(soilData.conductivity); Serial.println("µS/cm");
  Serial.print("  pH: "); Serial.println(soilData.pH);
  Serial.print("  N: "); Serial.print(soilData.nitrogen); Serial.println("mg/kg");
  Serial.print("  P: "); Serial.print(soilData.phosphorus); Serial.println("mg/kg");
  Serial.print("  K: "); Serial.print(soilData.potassium); Serial.println("mg/kg");
}

// ============================================================================
// TOUCH HANDLING
// ============================================================================

void handleTouchInput() {
  // Get touch point
  TS_Point p = ts.getPoint();

  // Check if touch is valid
  if (p.z > 0) {
    // Map touch coordinates to screen coordinates
    uint16_t x = map(p.x, TS_MINX, TS_MAXX, 0, tft.width());
    uint16_t y = map(p.y, TS_MINY, TS_MAXY, 0, tft.height());

    Serial.print("Touch detected at: X=");
    Serial.print(x);
    Serial.print(", Y=");
    Serial.println(y);

    // Check if touch is within SAVE DATA button bounds
    if (isTouchInButton(x, y)) {
      handleSaveButtonPress();
    }
  }
}

// ============================================================================
// BUTTON DETECTION
// ============================================================================

bool isTouchInButton(uint16_t x, uint16_t y) {
  return (x >= SAVE_BUTTON_X && x <= (SAVE_BUTTON_X + SAVE_BUTTON_W)) &&
         (y >= SAVE_BUTTON_Y && y <= (SAVE_BUTTON_Y + SAVE_BUTTON_H));
}

void handleSaveButtonPress() {
  Serial.println("SAVE DATA button pressed!");

  // Set flag indicating data is ready to send
  dataReadyToSend = true;
  lastButtonPressTime = millis();

  // Highlight button
  drawSaveButton(true);

  // Display status message
  displayReadyStatus(true);
}

// ============================================================================
// UI DRAWING FUNCTIONS
// ============================================================================

void drawSoilDataDisplay() {
  // Clear screen
  tft.fillScreen(COLOR_BACKGROUND);

  // Draw title
  drawTitle();

  // Draw data fields with smaller spacing for smaller fonts
  int y_offset = 40;
  int line_spacing = 25;

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
  y_offset += line_spacing;

  drawDataField("Conductivity:", soilData.conductivity, y_offset, 7);

  // Draw save button
  drawSaveButton(false);

  // Draw ready status (if applicable)
  if (dataReadyToSend) {
    displayReadyStatus(true);
  }
}

void drawTitle() {
  tft.setFont();  // Use default font
  tft.setTextSize(2);  // Size 2
  tft.setTextColor(COLOR_TITLE);
  tft.setCursor(60, 10);
  tft.println("SOIL DATA");

  // Draw separator line
  tft.drawLine(0, 28, 240, 28, COLOR_LABEL);
}

void drawDataField(const char* label, float value, int y, uint8_t fieldNum) {
  // Set font for label and value - use default font, size 1
  tft.setFont();
  tft.setTextSize(1);

  // Draw label
  tft.setTextColor(COLOR_LABEL);
  tft.setCursor(10, y);
  tft.print(label);

  // Draw value
  tft.setTextColor(COLOR_VALUE);
  tft.setCursor(150, y);

  // Format and print value
  char valueStr[20];
  if (fieldNum == 4) {  // pH level - 1 decimal place
    sprintf(valueStr, "%.1f", value);
  } else if (fieldNum == 5 || fieldNum == 6) {  // Temperature and Moisture - 1 decimal place
    sprintf(valueStr, "%.1f", value);
  } else if (fieldNum == 7) {  // Conductivity - whole number
    sprintf(valueStr, "%.0f", value);
  } else {  // NPK - whole numbers
    sprintf(valueStr, "%.0f", value);
  }
  tft.print(valueStr);
}

void drawSaveButton(bool pressed) {
  // Determine button color based on pressed state
  uint16_t buttonColor = pressed ? COLOR_BUTTON_PRESS : COLOR_BUTTON;
  uint16_t textColor = ILI9341_WHITE;

  // Draw button rectangle
  tft.fillRect(SAVE_BUTTON_X, SAVE_BUTTON_Y, SAVE_BUTTON_W, SAVE_BUTTON_H, buttonColor);

  // Draw button border
  tft.drawRect(SAVE_BUTTON_X, SAVE_BUTTON_Y, SAVE_BUTTON_W, SAVE_BUTTON_H, ILI9341_WHITE);

  // Draw button text
  tft.setFont();
  tft.setTextSize(2);
  tft.setTextColor(textColor);
  tft.setCursor(SAVE_BUTTON_X + 30, SAVE_BUTTON_Y + 15);
  tft.print("SAVE DATA");
}

void displayReadyStatus(bool show) {
  if (show) {
    tft.setFont();
    tft.setTextSize(1);
    tft.setTextColor(COLOR_READY_TEXT);
    tft.setCursor(30, 240);
    tft.print("READY TO SEND");

    Serial.println("Status: READY TO SEND");
  }
}

// ============================================================================
// BLUETOOTH HANDLING
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
      // Unknown command
      SerialBT.println("UNKNOWN_COMMAND");
      Serial.println("Unknown command sent to Bluetooth");
    }
  }
}

void handleReadCommand() {
  // Send soil data immediately when READ command is received
  // No button press required - always send current sensor data

  Serial.println("READ command received - sending current soil data...");
  sendSoilData();

  // Visual feedback: flash button briefly on display
  drawSaveButton(true);
  delay(150);
  drawSaveButton(false);

  Serial.println("Data transmission complete");
}

void sendSoilData() {
  // Format: NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62;COND=1250\n
  char dataStr[120];

  sprintf(dataStr, "NPK=%.0f,%.0f,%.0f;PH=%.1f;TEMP=%.1f;MOIST=%.0f;COND=%.0f",
          soilData.nitrogen,
          soilData.phosphorus,
          soilData.potassium,
          soilData.pH,
          soilData.temperature,
          soilData.moisture,
          soilData.conductivity);

  // Send data with newline
  SerialBT.print(dataStr);
  SerialBT.println();  // Add newline

  Serial.print("Soil data sent: ");
  Serial.println(dataStr);
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

// Function to update soil data (call this from your sensor reading code)
void updateSoilData(float n, float p, float k, float ph, float temp, float moist) {
  soilData.nitrogen = n;
  soilData.phosphorus = p;
  soilData.potassium = k;
  soilData.pH = ph;
  soilData.temperature = temp;
  soilData.moisture = moist;

  // Redraw display with new values
  drawSoilDataDisplay();

  Serial.println("Soil data updated:");
  Serial.print("  N: "); Serial.println(soilData.nitrogen);
  Serial.print("  P: "); Serial.println(soilData.phosphorus);
  Serial.print("  K: "); Serial.println(soilData.potassium);
  Serial.print("  pH: "); Serial.println(soilData.pH);
  Serial.print("  Temp: "); Serial.println(soilData.temperature);
  Serial.print("  Moisture: "); Serial.println(soilData.moisture);
}

// ============================================================================
// END OF CODE
// ============================================================================

