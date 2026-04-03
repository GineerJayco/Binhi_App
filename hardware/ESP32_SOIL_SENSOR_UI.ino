/*
  ============================================================================
  ESP32 Soil Sensor Data Display with Touch Control & Bluetooth
  ============================================================================

  Hardware Configuration:
  - ESP32 Dev Module
  - 3.2" SPI TFT LCD (240x320, ILI9341 compatible)
  - XPT2046 Touch Controller
  - Bluetooth Classic (SPP)

  Pin Connections:
  LCD Display Pins:
    VCC -> VIN
    GND -> GND
    CS  -> GPIO 15
    RST -> GPIO 4
    DC  -> GPIO 2
    MOSI-> GPIO 23
    SCK -> GPIO 18
    MISO-> GPIO 19
    LED -> 3.3V (always on)

  Touch Controller Pins:
    CLK -> GPIO 18 (shared with LCD SCK)
    CS  -> GPIO 5
    DIN -> GPIO 23 (shared with LCD MOSI)
    DO  -> GPIO 19 (shared with LCD MISO)

  Required Libraries:
    - Adafruit_GFX
    - Adafruit_ILI9341
    - XPT2046_Touchscreen
    - BluetoothSerial (built-in)

  ============================================================================
*/

// Include required libraries
#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_ILI9341.h>
#include <XPT2046_Touchscreen.h>
#include <BluetoothSerial.h>

// ============================================================================
// PIN DEFINITIONS
// ============================================================================

// LCD Display Pins
#define TFT_CS   15   // Chip Select
#define TFT_RST  4    // Reset
#define TFT_DC   2    // Data/Command
#define TFT_MOSI 23   // SPI MOSI
#define TFT_SCK  18   // SPI Clock
#define TFT_MISO 19   // SPI MISO

// Touch Controller Pins
#define TOUCH_CS 5    // Touch Chip Select

// ============================================================================
// DISPLAY COLORS (RGB565 format)
// ============================================================================

#define BLACK       0x0000
#define WHITE       0xFFFF
#define RED         0xF800
#define GREEN       0x07E0
#define BLUE        0x001F
#define CYAN        0x07FF
#define MAGENTA     0xF81F
#define YELLOW      0xFFE0
#define DARK_GRAY   0x39E7
#define LIGHT_GRAY  0xC618
#define DARK_GREEN  0x0340
#define LIGHT_GREEN 0x9FE0

// ============================================================================
// OBJECT DECLARATIONS
// ============================================================================

// SPI pins: CLK=18, MOSI=23, MISO=19
Adafruit_ILI9341 tft = Adafruit_ILI9341(TFT_CS, TFT_DC, TFT_MOSI, TFT_SCK, TFT_RST, TFT_MISO);

// Touch screen initialization
XPT2046_Touchscreen ts(TOUCH_CS);

// Bluetooth Serial object
BluetoothSerial SerialBT;

// ============================================================================
// GLOBAL VARIABLES
// ============================================================================

// Soil data variables
struct SoilData {
  float nitrogen;      // N (mg/kg)
  float phosphorus;    // P (mg/kg)
  float potassium;     // K (mg/kg)
  float pH;            // pH level
  float temperature;   // Temperature in Celsius
  float moisture;      // Soil moisture in percentage
};

SoilData soilData = {12.0, 7.0, 9.0, 6.5, 29.4, 62.0};

// Button state and flags
bool dataReadyToSend = false;
bool buttonPressed = false;
unsigned long buttonPressTime = 0;
const unsigned long BUTTON_HIGHLIGHT_DURATION = 500;  // 500ms highlight

// Touch calibration values (these may need adjustment for your specific screen)
// These are approximate values - you may need to calibrate for best results
#define TOUCH_MIN_X 200
#define TOUCH_MAX_X 3900
#define TOUCH_MIN_Y 200
#define TOUCH_MAX_Y 3900

// Button dimensions (in display coordinates)
#define BUTTON_X 40
#define BUTTON_Y 280
#define BUTTON_WIDTH 160
#define BUTTON_HEIGHT 40

// ============================================================================
// SETUP FUNCTION
// ============================================================================

void setup() {
  // Initialize Serial for debugging
  Serial.begin(115200);
  delay(1000);

  Serial.println("\n\n=== ESP32 Soil Sensor UI Starting ===");

  // Initialize TFT display
  tft.begin();
  tft.setRotation(0);  // Portrait mode (240x320)
  tft.fillScreen(BLACK);

  Serial.println("TFT display initialized");

  // Initialize touch screen
  if (!ts.begin()) {
    Serial.println("ERROR: Touch screen initialization failed!");
    tft.setTextColor(RED, BLACK);
    tft.setTextSize(2);
    tft.setCursor(10, 150);
    tft.println("TOUCH INIT FAIL");
  } else {
    Serial.println("Touch screen initialized");
  }

  // Set touch screen rotation to match display
  ts.setRotation(0);

  // Initialize Bluetooth Serial
  if (!SerialBT.begin("ESP32_SOIL_SENSOR")) {
    Serial.println("ERROR: Bluetooth initialization failed!");
  } else {
    Serial.println("Bluetooth initialized as 'ESP32_SOIL_SENSOR'");
  }

  // Draw initial UI
  drawUI();

  Serial.println("=== Setup Complete ===\n");
}

// ============================================================================
// MAIN LOOP
// ============================================================================

void loop() {
  // Handle Bluetooth communication (non-blocking)
  handleBluetooth();

  // Handle touch input (non-blocking)
  handleTouchInput();

  // Update button highlight if pressed
  updateButtonHighlight();

  // Small delay to prevent overwhelming the processor
  delay(50);
}

// ============================================================================
// DISPLAY FUNCTIONS
// ============================================================================

/**
 * Draw the complete UI with all soil data and button
 */
void drawUI() {
  // Clear screen
  tft.fillScreen(BLACK);

  // Draw title
  drawTitle();

  // Draw soil data values
  drawSoilData();

  // Draw "SAVE DATA" button
  drawSaveButton(false);

  // Draw status text if data is ready
  if (dataReadyToSend) {
    drawReadyToSendText();
  }
}

/**
 * Draw the title "SOIL DATA" at the top
 */
void drawTitle() {
  tft.setTextColor(CYAN, BLACK);
  tft.setTextSize(3);
  tft.setCursor(30, 10);
  tft.println("SOIL DATA");

  // Draw a line separator
  tft.drawLine(0, 50, 240, 50, CYAN);
}

/**
 * Draw all soil data values on the screen
 */
void drawSoilData() {
  int startY = 70;
  int lineSpacing = 40;

  tft.setTextColor(WHITE, BLACK);
  tft.setTextSize(2);

  // Nitrogen (N)
  tft.setCursor(20, startY);
  tft.print("N: ");
  tft.setTextColor(YELLOW, BLACK);
  tft.print(soilData.nitrogen, 1);
  tft.print(" mg/kg");

  // Phosphorus (P)
  tft.setTextColor(WHITE, BLACK);
  tft.setCursor(20, startY + lineSpacing);
  tft.print("P: ");
  tft.setTextColor(YELLOW, BLACK);
  tft.print(soilData.phosphorus, 1);
  tft.print(" mg/kg");

  // Potassium (K)
  tft.setTextColor(WHITE, BLACK);
  tft.setCursor(20, startY + lineSpacing * 2);
  tft.print("K: ");
  tft.setTextColor(YELLOW, BLACK);
  tft.print(soilData.potassium, 1);
  tft.print(" mg/kg");

  // pH Level
  tft.setTextColor(WHITE, BLACK);
  tft.setCursor(20, startY + lineSpacing * 3);
  tft.print("pH: ");
  tft.setTextColor(YELLOW, BLACK);
  tft.print(soilData.pH, 1);

  // Temperature
  tft.setTextColor(WHITE, BLACK);
  tft.setCursor(20, startY + lineSpacing * 4);
  tft.print("TEMP: ");
  tft.setTextColor(YELLOW, BLACK);
  tft.print(soilData.temperature, 1);
  tft.print("C");

  // Moisture
  tft.setTextColor(WHITE, BLACK);
  tft.setCursor(20, startY + lineSpacing * 5);
  tft.print("MOIST: ");
  tft.setTextColor(YELLOW, BLACK);
  tft.print(soilData.moisture, 0);
  tft.print("%");
}

/**
 * Draw the "SAVE DATA" button
 * @param highlighted true if button should be highlighted
 */
void drawSaveButton(bool highlighted) {
  uint16_t bgColor = highlighted ? GREEN : DARK_GREEN;
  uint16_t textColor = BLACK;

  // Draw button rectangle
  tft.fillRect(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, bgColor);

  // Draw button border
  tft.drawRect(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE);

  // Draw button text
  tft.setTextColor(textColor, bgColor);
  tft.setTextSize(2);
  tft.setCursor(BUTTON_X + 20, BUTTON_Y + 12);
  tft.println("SAVE DATA");
}

/**
 * Draw "READY TO SEND" status message
 */
void drawReadyToSendText() {
  tft.setTextColor(GREEN, BLACK);
  tft.setTextSize(2);
  tft.setCursor(20, 250);
  tft.println("READY TO SEND");
}

// ============================================================================
// TOUCH INPUT HANDLING
// ============================================================================

/**
 * Check for touch input and handle button press
 */
void handleTouchInput() {
  // Check if screen was touched
  if (ts.touched()) {
    // Get touch point
    TS_Point p = ts.getPoint();

    // Convert touch coordinates to display coordinates
    // Note: You may need to adjust these mappings based on your screen calibration
    int touchX = map(p.x, TOUCH_MIN_X, TOUCH_MAX_X, 0, 240);
    int touchY = map(p.y, TOUCH_MIN_Y, TOUCH_MAX_Y, 0, 320);

    Serial.print("Touch detected: X=");
    Serial.print(touchX);
    Serial.print(", Y=");
    Serial.println(touchY);

    // Check if touch is within button bounds
    if (isTouchOnButton(touchX, touchY)) {
      handleButtonPress();
    }
  }
}

/**
 * Check if touch coordinates are within the button area
 * @param x touch X coordinate
 * @param y touch Y coordinate
 * @return true if touch is on button
 */
bool isTouchOnButton(int x, int y) {
  return (x >= BUTTON_X && x <= (BUTTON_X + BUTTON_WIDTH) &&
          y >= BUTTON_Y && y <= (BUTTON_Y + BUTTON_HEIGHT));
}

/**
 * Handle "SAVE DATA" button press
 */
void handleButtonPress() {
  if (!buttonPressed) {
    buttonPressed = true;
    buttonPressTime = millis();

    // Set the flag indicating data is ready to send
    dataReadyToSend = true;

    Serial.println("Button pressed! Data ready to send.");

    // Redraw UI to show button highlight and "READY TO SEND" text
    drawUI();
  }
}

/**
 * Update button highlight (returns to normal after duration expires)
 */
void updateButtonHighlight() {
  if (buttonPressed) {
    unsigned long currentTime = millis();
    unsigned long elapsedTime = currentTime - buttonPressTime;

    // If highlight duration has passed, reset button state
    if (elapsedTime > BUTTON_HIGHLIGHT_DURATION) {
      buttonPressed = false;
      // Optionally redraw UI to remove highlight
      // drawUI();  // Uncomment to remove highlight automatically
    }
  }
}

// ============================================================================
// BLUETOOTH COMMUNICATION
// ============================================================================

/**
 * Handle incoming Bluetooth messages
 */
void handleBluetooth() {
  // Check if there's data available from Bluetooth
  if (SerialBT.available()) {
    // Read the incoming message
    String message = SerialBT.readStringUntil('\n');
    message.trim();  // Remove any whitespace

    Serial.print("BT Message received: ");
    Serial.println(message);

    // Check if message is "READ"
    if (message == "READ") {
      handleReadCommand();
    }
  }
}

/**
 * Handle the READ command from Android app
 */
void handleReadCommand() {
  Serial.println("Processing READ command...");

  if (dataReadyToSend) {
    // Send soil data
    sendSoilData();

    // Reset the flag
    dataReadyToSend = false;

    Serial.println("Data sent! Flag reset.");
  } else {
    // Send "NO_DATA" response
    SerialBT.println("NO_DATA");
    Serial.println("No data available. Sent: NO_DATA");
  }
}

/**
 * Send soil data via Bluetooth in the specified format
 */
void sendSoilData() {
  // Format: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62\n"
  String dataMessage = "";

  // Build the data message
  dataMessage += "NPK=";
  dataMessage += String(soilData.nitrogen, 0);    // N without decimals
  dataMessage += ",";
  dataMessage += String(soilData.phosphorus, 0);  // P without decimals
  dataMessage += ",";
  dataMessage += String(soilData.potassium, 0);   // K without decimals
  dataMessage += ";PH=";
  dataMessage += String(soilData.pH, 1);          // pH with 1 decimal
  dataMessage += ";TEMP=";
  dataMessage += String(soilData.temperature, 1); // Temperature with 1 decimal
  dataMessage += ";MOIST=";
  dataMessage += String(soilData.moisture, 0);    // Moisture without decimals

  // Send the message
  SerialBT.println(dataMessage);

  Serial.print("Sent soil data: ");
  Serial.println(dataMessage);
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Update soil data (for testing or when receiving from sensors)
 * @param n nitrogen value
 * @param p phosphorus value
 * @param k potassium value
 * @param ph pH value
 * @param temp temperature value
 * @param moisture moisture percentage
 */
void updateSoilData(float n, float p, float k, float ph, float temp, float moisture) {
  soilData.nitrogen = n;
  soilData.phosphorus = p;
  soilData.potassium = k;
  soilData.pH = ph;
  soilData.temperature = temp;
  soilData.moisture = moisture;

  // Redraw the display with new data
  drawUI();

  Serial.println("Soil data updated");
}

/**
 * Get current soil data
 * @return SoilData structure with current values
 */
SoilData getSoilData() {
  return soilData;
}

// ============================================================================
// END OF CODE
// ============================================================================

