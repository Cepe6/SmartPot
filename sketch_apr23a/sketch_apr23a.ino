#include <DallasTemperature.h>
#include <OneWire.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <dht.h>

//#define MATRIX_DI 2
//#define MATRIX_CLK 3
//#define MATRIX_LAT 4

#define LCD_ADDRESS 0x3F
#define DHT_PIN 10
#define SOIL_TEMPERATURE_PIN 9
#define SOIL_MOISTURE_PIN A0

#define THRESHOLD_UP 8
#define THRESHOLD_DOWN 7

//#define BASE_MATRIX_PIN 3

//#define SAD_MOUTH_PIN 4
//#define HAPPY_MOUTH_PIN 5

//#define RED_COLOR_PIN 11
//#define GREEN_COLOR_PIN 12

#define RELAY 6

LiquidCrystal_I2C lcd(LCD_ADDRESS, 16, 4);
dht DHT;

OneWire ds(SOIL_TEMPERATURE_PIN);
DallasTemperature sensors(&ds);

int soil_moisture = 0;
int soil_temperature = 0;
int air_temperature = 0;
int air_humidity = 0;

int last_soil_moisture = 0;
int last_soil_temperature = 0;
int last_air_temperature = 0;
int last_air_humidity = 0;

int threshold = 50;

int sad_face_data[] =   {0x5A, 0xBD, 0x52,  0xFF, 0xFF, 0xFF, 0xC3, 0xBD};
int happy_face_data[] = {0x00, 0x42, 0x14A, 0x00, 0x00, 0x00, 0x42, 0x3C};
bool last_state;

char blueToothVal;
char lastValue;

bool auto_watering = true;
bool watering = false;

void read_air();
void read_soil_temperature();
void read_soil_moisture();
void read_threshold_buttons();
void check_threshold();

void setup()
{
  lcd.init(); // initialize the lcd
  lcd.backlight();
  lcd.setCursor(0, 0);
  lcd.print("A:00C 00% Water");
  lcd.setCursor(0, 1);
  lcd.print("S:00C 00% at:50");

  Serial.begin(9600);
  sensors.begin();

  pinMode(THRESHOLD_UP, INPUT);
  pinMode(THRESHOLD_DOWN, INPUT);

  pinMode(RELAY, OUTPUT);
  //pinMode(MATRIX_DI, OUTPUT);
  //pinMode(MATRIX_CLK, OUTPUT);
  //pinMode(MATRIX_LAT, OUTPUT);

  //pinMode(11, OUTPUT);
  pinMode(13, OUTPUT);
}

void loop()
{
  if (Serial.available())
  { //if there is data being recieved
    blueToothVal = Serial.read(); //read it
    Serial.println(blueToothVal);
    if(blueToothVal == 'a') {
      blueToothVal = Serial.read();
      if(blueToothVal == 'n') {
        Serial.println("autoOn");
        auto_watering = true;
      } else if(blueToothVal == 'f') {
        Serial.println("autoOff");
        lcd.setCursor(13, 1);
        lcd.print("OFF");
        auto_watering = false;
      }
    } else if(blueToothVal == 'w') {
       if(watering)
          Serial.println("Already watering!");
       else {
         blueToothVal = Serial.read();
         int seconds = blueToothVal - '0';
         water(seconds);
         Serial.println("Began watering");
       }
    } else if(blueToothVal == 't') {
        if(!auto_watering) Serial.println("Auto watering is DISABLED!");
        else {
          int new_threshold = 0;
          while((blueToothVal = Serial.read()) != 'e') {
            new_threshold = new_threshold * 10 + blueToothVal - '0';
          }
  
          threshold = new_threshold;
          lcd.setCursor(13, 1);
          lcd.print(threshold);
          lcd.print("%");
          Serial.println("Set new threshold: ");
          Serial.println(new_threshold);
        }
      }
  }
  

  read_air();
  read_soil_temperature();
  read_soil_moisture();
  if(auto_watering) {
    read_threshold_buttons();
    if(threshold > soil_moisture && !watering) {
      water(2000);
    }
  }
  //delay(1000);
}

void read_soil_temperature() {
  sensors.requestTemperatures(); //Read Soil Temperature
  soil_temperature = (int)sensors.getTempCByIndex(0);

  lcd.setCursor(2, 1);
  if (soil_temperature < 10 && soil_temperature > 0) {
    lcd.print(" ");
  }
  lcd.print(soil_temperature);
  lcd.print("C");

  if (soil_temperature < 100) {
    lcd.print(" ");
  }

  if(soil_temperature != last_soil_temperature) {
    last_soil_temperature = soil_temperature;
    Serial.println("st");
    Serial.println(soil_temperature);
  }
}

void read_soil_moisture() {
  soil_moisture = (int)100 - analogRead(SOIL_MOISTURE_PIN) / 10;

  lcd.setCursor(6, 1);
  if (soil_moisture < 10 && soil_moisture > 0) {
    lcd.print(" ");
  }
  lcd.print(soil_moisture);
  lcd.print("%");

  if (soil_moisture < 100) {
    lcd.print(" ");
  }

  if(soil_moisture != last_soil_moisture) {
    last_soil_moisture = soil_moisture;
    Serial.println("sm");
    Serial.println(soil_moisture);
  }
}

void read_threshold_buttons() {
  if (digitalRead(THRESHOLD_UP) == HIGH && threshold < 100) {
    threshold += 5;
  }
  else if (digitalRead(THRESHOLD_DOWN) == HIGH && threshold > 0) {
    threshold -= 5;
  }

  lcd.setCursor(13, 1);
  lcd.print(threshold);
  lcd.print("%");

  Serial.println("tn");
  Serial.println(threshold);
}

void water(int seconds) {
  watering = true;
  digitalWrite(RELAY, HIGH);
  delay(seconds * 1000);
  digitalWrite(RELAY, LOW);
  watering = false;
}

void read_air() {
  int chk = DHT.read11(DHT_PIN);
  air_temperature = (int)DHT.temperature;
  air_humidity = (int)DHT.humidity;

  lcd.setCursor(2, 0);
  if (air_temperature < 10 && air_temperature > 0) {
    lcd.print(" ");
  }
  lcd.print(air_temperature);
  lcd.print("C");

  if (air_temperature < 100) {
    lcd.print(" ");
  }

  lcd.setCursor(6, 0);
  if (air_humidity < 10 && air_humidity > 0) {
    lcd.print(" ");
  }
  lcd.print(air_humidity);
  lcd.print("%");

  if (air_humidity < 100) {
    lcd.print(" ");
  }

  if(air_temperature != last_air_temperature) {
    last_air_temperature = air_temperature;
    Serial.println("at");
    Serial.println(air_temperature);
  }

  if(air_humidity != last_air_humidity) {
    last_air_humidity = air_humidity;
    Serial.println("ah");
    Serial.println(air_humidity);
  }
}


