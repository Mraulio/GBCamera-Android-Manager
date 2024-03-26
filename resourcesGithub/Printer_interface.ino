//Game boy Printer interface with Arduino, by Raphaël BOICHOT 2023/04/03

char byte_read;
bool bit_sent, bit_read;
int clk = 2; // clock signal
int TX = 5; // The data signal coming from the Arduino and goind to the printer (Sout on Arduino becomes Sin on the printer)
int RX = 4;// The response bytes coming from printer going to Arduino (Sout from printer becomes Sin on the Arduino)

void setup() {
  pinMode(clk, OUTPUT);
  pinMode(TX, OUTPUT);
  pinMode(RX, INPUT_PULLUP);
  digitalWrite(clk, HIGH);
  digitalWrite(TX, LOW);
  Serial.begin(9600);//9600 WORKS. 19200, 38400,57600 DON'T
}

void loop() {
  if (Serial.available() > 0)
  {
    Serial.write(printing(Serial.read()));
  }
}

char printing(char byte_sent) { // this function prints bytes to the serial
  for (int i = 0; i <= 7; i++) {
    bit_sent = bitRead(byte_sent, 7 - i);
    digitalWrite(clk, LOW);
    digitalWrite(TX, bit_sent);
    delayMicroseconds(30);//double speed mode
    digitalWrite(clk, HIGH);
    bit_read = (digitalRead(RX));
    bitWrite(byte_read, 7 - i, bit_read);
    delayMicroseconds(30);//double speed mode
  }
  delayMicroseconds(0);//optionnal delay between bytes, may me less than 1490 µs
return byte_read;
}
