/*
 * Simple ESP32 Bluetooth Test Code
 *
 * This is a minimal test version to verify Bluetooth communication
 * with the Binhi Android app.
 *
 * Just upload this, pair "ESP32_SOIL_SENSOR", and test!
 */

#include <BluetoothSerial.h>

BluetoothSerial SerialBT;

void setup() {
    Serial.begin(115200);
    delay(1000);

    Serial.println("Starting Bluetooth...");
    SerialBT.begin("ESP32_SOIL_SENSOR");  // Device name

    Serial.println("Bluetooth initialized!");
    Serial.println("Device name: ESP32_SOIL_SENSOR");
    Serial.println("Waiting for Android to connect...");
}

void loop() {
    // Check for incoming data from Android
    if (SerialBT.available()) {
        String command = SerialBT.readStringUntil('\n');
        command.trim();

        Serial.print("Received: ");
        Serial.println(command);

        // Handle READ command (what the app sends)
        if (command == "READ") {
            // Send test NPK data
            String response = "NPK=150,120,180";
            SerialBT.println(response);
            Serial.print("Sent: ");
            Serial.println(response);
        }
        // Handle TEST command
        else if (command == "TEST") {
            SerialBT.println("NPK=100,80,120");
            Serial.println("Sent test data");
        }
        // Handle STATUS command
        else if (command == "STATUS") {
            SerialBT.println("STATUS=OK");
            Serial.println("Sent status");
        }
        else {
            Serial.println("Unknown command");
        }
    }

    delay(100);
}

