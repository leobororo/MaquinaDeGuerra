//Autor : Leandro e Leonardo
 
//Definicoes pinos Arduino ligados a entrada da Ponte H
int IN1 = 4;
int IN2 = 5;
int IN3 = 6;
int IN4 = 7;

//Pino analógico em que o sensor está conectado.
int sensor = 0;      
 
//variável usada para ler o valor do sensor em tempo real.
int valorSensor = 0;
  
void setup()
{
  //Define os pinos como saida  
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
 
  //Ativando o serial monitor que exibirá os valores lidos no sensor.
  Serial.begin(9600);    
}
  
void loop()
{
  
  if (Serial.available()) {
    char letra = Serial.read();
    
    if(letra == 'F') //Se o caractere recebido for a letra 'F'
    {
      //Move o carrinho para frente
      digitalWrite(IN1, HIGH);
      digitalWrite(IN2, LOW);
      digitalWrite(IN3, HIGH);
      digitalWrite(IN4, LOW);
    }
    else if (letra == 'B') //Se o caractere recebido for a letra 'B'
    {
      //Move o carrinho para trás
      digitalWrite(IN2, HIGH);
      digitalWrite(IN1, LOW);
      digitalWrite(IN4, HIGH);
      digitalWrite(IN3, LOW);
    }
    else if (letra == 'R') //Se o caractere recebido for a letra 'R'
    {
      //Move o carrinho para direita
      digitalWrite(IN1, HIGH);
      digitalWrite(IN2, LOW);
      digitalWrite(IN4, LOW);
      digitalWrite(IN3, LOW);
    }
    else if (letra == 'L') //Se o caractere recebido for a letra 'L'
    {
      //Move o carrinho para esquerda
      digitalWrite(IN1, LOW);
      digitalWrite(IN2, LOW);
      digitalWrite(IN4, HIGH);
      digitalWrite(IN3, LOW);
    } 
    else if (letra == 'S') //Se o caractere recebido for a letra 'S'
    {
    //Para o carrinho
      digitalWrite(IN1, LOW);
      digitalWrite(IN2, LOW);
      digitalWrite(IN4, LOW);
      digitalWrite(IN3, LOW);
    }
  }    
  
   //Lendo o valor do sensor.
  int valorSensor = analogRead(sensor);
   
  //Exibindo o valor do sensor no serial monitor.
  Serial.println(valorSensor);
   
  delay(500);
}
