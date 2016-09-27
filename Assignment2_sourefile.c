/*******************************************************************************
* Description :
*     Robot calculates its position with constant pwm.
*     Written for IAR compiler, using Normal DLIB Library.
* Limitations :
*     Written for C language (not C++).
*     This program assumes the robot starts at the origin
*     and is pointing along the x axis.
* Notes:
*     This is an incomplete program for the Introductory Assignment.
*     The code that needs to be completed is shown in bold comments
*     in this PDF document (Robot Intro Code.pdf).
*     This is sample code. You can change it any way you like. 
*******************************************************************************/

/*******************************************************************************
* INCLUDES
*******************************************************************************/
#include <iom128.h>
#include <intrinsics.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

/*******************************************************************************
* DEFINES
*******************************************************************************/
#define PI 3.141592653589793238462643 // from CLIB math.h

#define RIGHT_SPEED 1023
#define LEFT_SPEED  1023
#define BUF_MAX     1000

#define USE_SERIAL_INTERRUPTS 1

/*******************************************************************************
* STRUCTURES
*******************************************************************************/
struct Position
{
    float x; // x and y position coordinates in metres
    float y;
    float h; // heading angle in radians, anti-clockwise from the x-axis
};

struct targetLocation{
  
    //bluetooth variables
    int cmd; //type of command
    int speed; //max speed ; can be set by user
  
    float xT; //x and y positions for target location (meters)
    float yT;
    float hT; //target heading angle in rads, anti-clockwise from x-axis
    float intergalSum; //some of integral that is used in PID calculations
};
/*******************************************************************************
* FUNCTION PROTOTYPES
*******************************************************************************/
void Setup(); // ATmega128 initialisation for this program
void CalcPosition(struct Position *pos); // calculate robot position from wheel pulse counts
void OutputString(char* str); // put string in serial port 0 transmits buffer


//****************************************
//(own Methods)
void TargetHeadingCalc(struct Position *pos, struct targetLocation *target); //calculates the target heading in relation to robots heading
void PID(float error, struct targetLocation *target);	//function that does PID control

//Bluetooth Function
void GetCommand(struct Position *pos, struct targetLocation *target); //function to get commmand and value sent by the user
/*******************************************************************************
* GLOBAL VARIABLES
*******************************************************************************/
volatile unsigned int leftPulseCount, rightPulseCount; // wheel pulse counts


//(Assignment2) *** variables 
volatile char gotCmd; // got command flag
volatile char command[40]; // received command string
enum cmdStates{GO_CMD, TURN_CMD, STOP_CMD}; //the values that the cmd could be

//(own)********* flag for PID calc, occurs every 100ms
volatile unsigned char PID_interuptCounter;

#if USE_SERIAL_INTERRUPTS == 1
// serial port 0 transmit buffer
char buffer[BUF_MAX]; // buffer (queue) data
volatile unsigned int head, tail, count; // buffer (queue) indexes 
#endif

/*******************************************************************************
* MAIN FUNCTION
*******************************************************************************/
void main (void)
{
    Setup(); // initialise ATmega128
    struct Position pos; // current position from CalcPosition		
    struct targetLocation target; //target values of robot
    
    //reset all the position (robot and target) values to zero before the programs starts
    pos.x = 0.; //robot values
    pos.y = 0.;
    pos.h = 0.;
    target.xT = 0.; //target values
    target.yT = 0.;
    target.hT = 0.;
    
   //variables***********************************
    enum allStates{START_STATE, TURN_STATE, PID_STATE, STOP_STATE}; //all states that control robot movement 
    float error, distance; //distance - the robot has to travel
                          //error - the error value used to calculate PID. is the difference between robot angle & target angle
    char robotState = START_STATE;
    target.speed = 1023; //set default speed, safe guarding incase user doesnt set speed in GUI
    char cmdFlag; //flag to store gotcmd value
    static char str[40]; // serial output string
    // display the wheel pwm settings
    sprintf(str, "SPEED,%d,%d\r\n", LEFT_SPEED, RIGHT_SPEED);
    OutputString(str);
    //*******************************************
    while (1) // loop forever
    {
      
      CalcPosition(&pos); //calculates position of robot 
      
      if(target.cmd != TURN_CMD)        //only calculate targetheading if not in turn command
        TargetHeadingCalc(&pos, &target);	//sets targets heading
      
      __disable_interrupt(); //disable interrupts and access the gotCmd variable 
      cmdFlag = gotCmd;
      gotCmd = 0;
      __enable_interrupt(); //enable interrupt again
      
      //get if user has sent command
      if(cmdFlag == 1){
              GetCommand(&pos, &target); //get the command, stores in global cmd variable
              
              if(target.cmd == GO_CMD)  //if GO, start normal robot motor control
                robotState = START_STATE;
              
              if(target.cmd == STOP_CMD) //if STOP, go to the stop motor state
                robotState = STOP_STATE;
              
              if(target.cmd == TURN_CMD)//if TURN, go to the turn motor state
                robotState = TURN_STATE;
              
              cmdFlag = 0;} //reset the gotCommand flag so new data can be stored 
      
     
      error = pos.h - target.hT; //calculation for error values, used in PID
      
	  //check sums to keeps error within +-180 degrees
       if(error>PI)
          error -= (2*PI);
  
      if(error<(-1*PI))
          error += (2*PI); 
      
	  //calculates distance from robot to target position
      distance = sqrt(pow((target.xT-pos.x),2) + pow((target.yT-pos.y),2));
      
	  //state machine 
      switch(robotState)
      { 
        case START_STATE:
          //OutputString("\r\nState: Start\r\n");
          if (distance <= 0.050) // metres
            robotState = STOP_STATE;
          else{
            robotState = TURN_STATE;
            OutputString("\r\nState: TURN_STATE\r\n"); }
        break;
        
        case TURN_STATE:
          if(distance <= 0.050 && target.cmd != TURN_CMD){ //meters for distance, and only go stop state if not turn Command as in turn distance == 0
            robotState = STOP_STATE;
            OutputString("\r\nState: STOP_STATE\r\n"); }
          else if(error < 0.20 && error > -0.20) //checks to see if error is within a 12 degree range of target
          {     
              
            if(target.cmd != TURN_CMD){ //Don't go into PID if turn command, as PID is not needed
              robotState = PID_STATE;
              OutputString("\r\nState: PID_STATE\r\n"); }
            else
              robotState = STOP_STATE;
            
              
              target.intergalSum = 0;      //reset sum when going into PID       
          }
          
          else if(error < 0){ 	//turn right
              OCR1B = 350;      //set to 350 for a slow turn speed
              OCR1A = 0;
          }
          
          else if(error > 0){	//turn left
              OCR1A = 350; 
              OCR1B = 0; 
          }

        break;
        
        case PID_STATE:
          if (distance <= 0.050){ // metres
            robotState = STOP_STATE;
            OutputString("\r\nState: STOP_STATE\r\n"); }
          else if (error > 0.5 || error < -0.5){ // radians, check if error is too big 
            robotState = TURN_STATE;
            OutputString("\r\nState: TURN_STATE\r\n"); }
          else
            PID(error, &target); // call separate PID function
         break;
          
        case STOP_STATE:            
            OCR1A = 0;
            OCR1B = 0;
        break;
      }
    }
}

/*******************************************************************************
* OTHER FUNCTIONS
*******************************************************************************/
void CalcPosition(struct Position *pos) // calculate the robot position
{
  
    int leftCount, rightCount; // number of wheel pulses
    static char str[100]; // serial output string
    
    // get the pulse counts
    __disable_interrupt();
    leftCount = leftPulseCount; // copy the pulse counts
    rightCount = rightPulseCount;
    leftPulseCount = 0; // reset the pulse counts to zero
    rightPulseCount = 0;
    __enable_interrupt();
    float dx, dy,dh = 0;
    
    // if there were any pulses, calculate the new position
    if (leftCount || rightCount)
    {
      dh = (rightCount - leftCount)*0.06686;
      
      if(leftCount == rightCount){
        dx = 0.001774*rightCount*cos(pos->h);
        dy = 0.001774*rightCount*sin(pos->h);  
      }else {
        dx = 0.0532*((float)((rightCount+leftCount)/(rightCount-leftCount)))*(sin(pos->h+dh)-sin(pos->h));
        dy = 0.0532*((float)((rightCount+leftCount)/(rightCount-leftCount)))*(cos(pos->h)-cos(pos->h+dh));  
      }
        // do the position calculation
      pos->h = pos->h + dh;
      pos->x = pos->x + dx;
      pos->y = pos->y + dy;
      
      if(pos->h>PI)
		pos->h -= (2*PI);
  
      if(pos->h<(-1*PI))
		pos->h += (2*PI);
      
        // display the new position (convert heading to degrees)
        sprintf(str, "POS,%7.3f,%7.3f,%7.1f, %3d,%3d\r\n", pos->x, pos->y,
            pos->h * 180. / PI, leftCount, rightCount);
        OutputString(str);
    }
}

/******************************************************************************
*                               BLUETOOTH
******************************************************************************/


void GetCommand(struct Position *pos, struct targetLocation *target){
  
  int n;
  float f1, f2;
  //int s;
  
  
  //************* GO (X,Y) *************
  if (strncmp((char*)command, "GO", 2) == 0)
  {
    // get x and y values
    n = sscanf((char*)command + 2, "%f,%f", &f1, &f2);
    if (n == 2) // got both numbers
    {
        // set command type for the Navigation state machine
        target->cmd = GO_CMD;
        target->xT = f1; // change the target coordinates
        target->yT = f2;
    }
  }

  //************* SPEED (S) *************
  if (strncmp((char*)command, "SPEED", 5) == 0)
  {
    // get speed value
    n = sscanf((char*)command + 5, "%f", &f1);
    if (n == 1) // got speed value
    {
        //check if speed is within the PWN suitable range
        if(f1 > 0 && f1 < 1023)
          target->speed = (int)f1;
        else 
          target->speed = 1023; //if not, set to max
    }
  }
  
  //************* TURN (h) *************
  if (strncmp((char*)command, "TURN", 4) == 0)
  {
    // get angle value
    n = sscanf((char*)command + 4, "%f", &f1);
    if (n == 1) // got turn value
    {
        // set command type for the Navigation state machine
        target->cmd = TURN_CMD;
        target->xT = pos->x; //set target location, to current robot position, as doesn't need to finish current command if there is one
        target->yT = pos->y;
        f2 = (f1*PI)/180; //converting degrees to rads
        target->hT = f2; //set target heading to user set value
     }
  }
              
  //************* STOP  *************             
  if (strncmp((char*)command, "STOP", 4) == 0)
  {
      // set command type for the Navigation state machine
        target->cmd = STOP_CMD;
        target->xT = pos->x; //set target location, to current robot position, as doesn't need to finish current command if there is one
        target->yT = pos->y;
  }
  
} 


 //****************************************************************
void TargetHeadingCalc(struct Position *pos, struct targetLocation *target)
{
  target->hT = atan2((target->yT-pos->y),(target->xT-pos->x)); //calculate target heading
  
  //checksum to keep within +-180 degree range
   if(target->hT>PI)
	target->hT -= (2*PI);
  
   if(target->hT<(-1*PI))
        target->hT += (2*PI);
}

void PID(float error, struct targetLocation *target){
  
    static float errorPrevious = 0;
    char PID_Flag;
	
	//get timer3 flag from interrupt
    __disable_interrupt();
    PID_Flag = PID_interuptCounter;
    PID_interuptCounter = 0;	//reset flag
    __enable_interrupt();
    
  if(PID_Flag == 1){	//do the PID control calculations
    
    float F_PID;
    
    target->intergalSum = target->intergalSum + error + errorPrevious; //calculated integral sum for PID
    
    if(target->intergalSum > 10)	//Keep sum within suitable range
      target->intergalSum = 10;
    
	//calculation of PID value
    F_PID = (2000*error) + (100*target->intergalSum) +
                          (750*(error - errorPrevious));
    
    //checksum to keep PID value within +-1023 range
    if(F_PID > target->speed)
       F_PID = target->speed;
    
    if(F_PID < (-1*target->speed))
      F_PID = (-1*target->speed);
    
    errorPrevious = error;	//set previous error to current error
    
    PID_Flag = 0;	//reset PID flag
    
    //control robot wheels depending on PID value
	//******************************
    if(F_PID >= 0){
      OCR1A = target->speed; //max value can only be the set speed, or 1023 if not set by user
      OCR1B = target->speed - (int)(F_PID);
    }
    else if(F_PID <= 0){
      OCR1A = target->speed + (int)(F_PID);
      OCR1B = target->speed;
    }
    else if(F_PID == 0){
      OCR1A = target->speed;
      OCR1B = target->speed;
    }
	//*******************************
  }
}
//******************************************************************************
//Transmitting data function
//******************************************************************************
#if USE_SERIAL_INTERRUPTS == 1
// transmit serial string USING INTERRUPTS
void OutputString(char* str)
{
    int length = strlen(str);
    UCSR0B_UDRIE0 = 0; // disable serial port 0 UDRE interrupt
    // check for too many chars
    if (count + length >= BUF_MAX)
    {
        UCSR0B_UDRIE0 = 1; // enable serial port 0 UDRE interrupt
        return;
    }
    // write the characters into the buffer
    for (int n = 0; n < length; n++)
    {
        buffer[tail] = str[n];
        tail++;
        if (tail >= BUF_MAX)
        {
          tail = 0;
        }
    }
    count += length;
    UCSR0B_UDRIE0 = 1; // enable serial port 0 UDRE interrupt
}
#else
// transmit serial string NOT USING INTERRUPTS
void OutputString(char* str)
{
    int length = strlen(str);
    // for each character in the string
    for (int n = 0; n < length; n++)
    {
        // wait while the serial port is busy
        while (!UCSR0A_UDRE0);
        // transmit the character
        UDR0 = str[n];
    }
}
#endif

/*******************************************************************************
* INTERRUPT FUNCTIONS
*******************************************************************************/

#pragma vector = TIMER3_COMPA_vect 
__interrupt void Timer3Interrput(void) //set PID flag every 100ms
{
  PID_interuptCounter = 1;
}

#pragma vector = INT0_vect
__interrupt void LeftCounterISR(void) // left wheel pulse counter
{
    leftPulseCount++;
}

#pragma vector = INT1_vect
__interrupt void RightCounterISR(void) // right wheel pulse counter
{
    rightPulseCount++;
}
//******************************************************************************


#if USE_SERIAL_INTERRUPTS == 1
#pragma vector = USART0_UDRE_vect
__interrupt void Serial0UDREmptyISR(void) // serial DRE (transmit) interrupt
{
    if (count > 0) // if there are more characters
    {
        UDR0 = buffer[head]; // transmit the next character
        // adjust the buffer variables
        head++;
        if (head > BUF_MAX)
        {
            head = 0;
        }
        count--;
    }
    if (count == 0) // if there are no more characters
    {
        UCSR0B_UDRIE0 = 0; // then disable serial port 0 UDRE interrupt
    }
}
#endif


#pragma vector = USART0_RXC_vect
__interrupt void SerialReceiveISR(void)
{
  char ch;
  static char buffer[40]; // receiving command
  static int index = 0; // number of command chars received
  ch = UDR0;
  // only store ASCII characters
  if (ch >= ' ' && ch <= '~' && index < 39)
  {
    buffer[index] = ch;
    index++;
   }
  if (ch == '\r') // end of command
  {
    buffer[index] = 0; // null character code
  // make sure the previous command has been used
    if (!gotCmd)
      {
        strcpy((char*)command, buffer);
        gotCmd = 1;
      }
        index = 0;
  }
} 
//******************************************************************************

void Setup() // ATmega128 setup
{
    // timer1 setup for pwm: enable pwm outputs, mode 7 prescale=256
    TCCR1A = 0xA3;
    TCCR1B = 0x0C;
    TCCR1C = 0x00;
    OCR1A = 0; // motors off
    OCR1B = 0;
    
    //(own) timer3 for interput: 10Hz, CTC mode, interupt every 100ms
    TCCR3A = 0x00;
    TCCR3B = 0x0C;
    TCCR3C = 0x00;
    OCR3A = 3124; 
    
    // timer3 interupt on channel A on compare match
    ETIMSK_Bit4 = 1;  
    // digital input/output  
    // enable motor outputs
    DDRB_Bit5 = 1; //output
    DDRB_Bit6 = 1;
    // enable motor direction outputs, motors forward
    DDRA_Bit6 = 1;
    DDRA_Bit7 = 1;
    //PORTA_Bit6 = 0;
    // enable the wheel pulse generator electronics
    DDRC_Bit3 = 1;
    PORTC_Bit3 = 1;
    // serial output: enable receive and transmit
    UCSR0B_RXEN0 = 1;
    UCSR0B_TXEN0 = 1;
    
    //enable receive interrupt 
    UCSR0B_RXCIE0 = 1;
    
        // 8 data bits, no parity, 1 stop
        // set the baud rate
    UCSR0C_UCSZ01 = 1;
    UCSR0C_UCSZ00 = 1;
    UBRR0L = 12;
        // do NOT enable the transmit interrupt here
    // give the electronic hardware time to settle
    __delay_cycles(4000000); // 500ms
    // enable external interrupts for the wheel pulses (INT0, INT1)
    EIMSK = 0x03;
    EICRA = 0x0F;
    // set initial robot location at origin
    //pos.x = 0.;
   // pos.y = 0.;
   // pos.h = 0.;
    
	//setting target position
    //target.xT = 1;
    //target.yT = 1;
    
    
    #if USE_SERIAL_INTERRUPTS == 1
    // initialise serial output buffer
    head = 0;
    tail = 0;
    count = 0;
    #endif
    __enable_interrupt(); // enable interrupts last    
    // display started on serial port
    OutputString("\r\nSTARTING\r\n");
}