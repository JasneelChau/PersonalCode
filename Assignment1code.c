
/*
 * 
 *  Baby Water Sensor
 *  Embedded Systems Assignment 1
 *  ID: 1385494
 */

#define F_CPU 8000000UL
#include <avr/io.h>

#define heaterSw (PINA & (1<<0))
#define heaterOn (PORTB |= (1<<7))
#define heaterOFF (PORTB &=~ (1<<7))
#define tempDisplay PORTC
#define adcReading ADCW

void setup(void);
char TWIwrite(char address, char reg_addr, char data);
char convertHextoDec(char tempHex);	//method to convert hex value to decimal

int main(void)
{
	setup();
	char realTemp;
    while(1)
    {
			
			realTemp = adcReading/10; //converting ADC reading to temperature
			//	Working:
			//	ADC is 10bit, ref value is 5v
			//  5/1023 = 0.004887
			//  ADC from schematic is 50mV per 1 degree Celsius therefore: 50mV/5mV = 10 
			
			tempDisplay = convertHextoDec(realTemp); //converting temperature to decimal and displaying value
			
			if(heaterSw)	//checking if heater switch is on or off 
				heaterOn;
			else
				heaterOFF;
			
			if(realTemp <= 36)	//checking if water is too cool
				TWIwrite(0b11001100, 0b00000011, 0b00110000); //turns on Blue LEDS
			else if (realTemp > 36 && realTemp < 39)	//checking if water in ideal temperature
				TWIwrite(0b11001100, 0b00000011, 0b00000011); //turn on Green LEDS
			else	//else statement means water is too hot
				TWIwrite(0b11001100, 0b00000011, 0b00001100); //turn on Red LEDS
		
    }
}

void setup(void)
{
	DDRC = 0xFF; //setting out pin for output
	PORTC = 0x00;
	DDRA = 0x00;
	DDRB = 0b10000000;	//setting bit7 for output
	ADCSRA = 0b11100111; //enable, free running, 128 prescale
	ADMUX = 0b01000011;	// AVCC with external capacitor at AREF PIN, set to read ADC3 Channel
	
	TWBR = 0x20; //clock set to 333kHz
	TWIwrite(0b11001100, 0b00000000, 0b00001111); //turn on Blue to 16mA
	TWIwrite(0b11001100, 0b00000001, 0b00001111); //turn on Red to 16mA
	TWIwrite(0b11001100, 0b00000010, 0b00001111); //turn on Green to 16mA
}

char convertHextoDec(char tempHex){	
	
	char ten = tempHex/10;
	char ones = tempHex%10;
	char final = (ten<<4)+(ones);
	return final;
}


char TWIwrite(char address, char reg_addr, char data)
{
	char x, error = 0;
	
	TWCR = (1<<TWINT)|(1<<TWSTA)|(1<<TWEN); //enable TWI, send start, clear flag
	while(!(TWCR & (1<<TWINT))); //wait for start bit to be sent
	if((TWSR & 0xF8) != 0x08) // status code for successful start is 0x18
		error = 1;
	
	else
	{
		TWDR = address; //send slave address and write
		TWCR = (1<<TWINT)|(1<<TWEN); //clear the flag
		while(!(TWCR & (1<<TWINT)));
		if((TWSR & 0xF8) != 0x18)
			error = 1;
		else
		{
			TWDR = reg_addr; //send slave address and write
			TWCR = (1<<TWINT)|(1<<TWEN); //clear the flag
			while(!(TWCR & (1<<TWINT)));
			if((TWSR & 0xF8) != 0x28)
			error = 1;
			else
			{
				TWDR = data;
				TWCR = (1<<TWINT)|(1<<TWEN);
				while(!(TWCR & (1<<TWINT)));
				
				if((TWSR & 0xF8) != 0x28)
				error = 1;
				else
				{
					TWCR = (1<<TWINT)|(1<<TWEN)|(1<<TWSTO);
					for(x = 0; x < 50; x++)
						asm("NOP");					
				}
			}
		
	}
}
return(error);
}


