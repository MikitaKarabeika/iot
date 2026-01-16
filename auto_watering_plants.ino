#include <dht11.h>

#define DHT11_PIN    9
#define MOISTURE_PIN A2
#define PUMP_PIN1    5
#define PUMP_PIN2    6

unsigned long lastUpdate = 0;
const long interval = 2000; 

int airHumidity, airTemperature, soilHumidity;
int setHumidity = 50;
bool isPumpOn = false;
int operatingMode = 0; // 0:AUTO, 1:ON, 2:OFF

dht11 DHT;

void setup() {
  Serial.begin(9600);
  pinMode(PUMP_PIN1, OUTPUT);
  pinMode(PUMP_PIN2, OUTPUT);
  pumpOff();
}

void loop() {
  if (Serial.available() > 0) {
    String command = Serial.readStringUntil('\n');
    command.trim();

    if (command.startsWith("SET_HUM:")) {
      setHumidity = command.substring(8).toInt();
      Serial.print("CONFIRM|Prog ustawiony na ");
      Serial.println(setHumidity);
    } 
    else if (command == "MODE_AUTO") {
      operatingMode = 0;
      Serial.println("CONFIRM|Tryb AUTO");
    } 
    else if (command == "MODE_ON") {
      operatingMode = 1;
      Serial.println("CONFIRM|Wymuszone WL");
    } 
    else if (command == "MODE_OFF") {
      operatingMode = 2;
      Serial.println("CONFIRM|Wymuszone WYL");
    }
  }

  unsigned long currentMillis = millis();
  if (currentMillis - lastUpdate >= interval) {
    lastUpdate = currentMillis;
    readSensors();
    
    if (operatingMode == 0) checkWatering();
    else if (operatingMode == 1) pumpOn();
    else pumpOff();
    
    sendDataToSerial();
  }
}

void readSensors() {
  DHT.read(DHT11_PIN);
  airHumidity = DHT.humidity;
  airTemperature = DHT.temperature;
  soilHumidity = map(analogRead(MOISTURE_PIN), 0, 1023, 0, 100);
}

void sendDataToSerial() {
  Serial.print("DATA|");
  Serial.print(airTemperature);   Serial.print("|");
  Serial.print(airHumidity);      Serial.print("|");
  Serial.print(soilHumidity);     Serial.print("|");
  Serial.print(setHumidity);      Serial.print("|");
  Serial.print(isPumpOn ? "WL" : "WYL"); Serial.print("|");
  
  if(operatingMode == 0) Serial.println("AUTO");
  else if(operatingMode == 1) Serial.println("RECZNY-WL");
  else Serial.println("RECZNY-WYL");
}

void checkWatering() {
  if (soilHumidity < setHumidity) pumpOn();
  else pumpOff();
}

void pumpOn() { digitalWrite(PUMP_PIN1, HIGH); digitalWrite(PUMP_PIN2, HIGH); isPumpOn = true; }
void pumpOff() { digitalWrite(PUMP_PIN1, LOW); digitalWrite(PUMP_PIN2, LOW); isPumpOn = false; }