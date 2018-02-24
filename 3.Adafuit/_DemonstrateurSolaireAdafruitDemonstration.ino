/* http://patorjk.com; Big; Smush(R)
  _____       _                 _            _   _
  |_   _|     | |               | |          | | (_)
   | |  _ __ | |_ _ __ ___   __| |_   _  ___| |_ _  ___  _ __
   | | | '_ \| __| '__/ _ \ / _` | | | |/ __| __| |/ _ \| '_ \
  _| |_| | | | |_| | | (_) | (_| | |_| | (__| |_| | (_) | | | |
  |_____|_| |_|\__|_|  \___/ \__,_|\__,_|\___|\__|_|\___/|_| |_|


  Auteur:   Vincent HUGUELET
  Date:     Février 2018
  Projet:   Bachelor, démontrateur solaire
  Section:  Gestion de deux moteurs avec quatres photoResistances,
            système manuel et la communication Bluetooth pour commande
            et enrigstrement des mesures d'un démonstrateur solaire
            (Solair Tracking)


  ____  _ _     _ _       _   _      __
  |  _ \(_) |   | (_)     | | | |     \_\
  | |_) |_| |__ | |_  ___ | |_| |__   ___  __ _ _   _  ___  ___
  |  _ <| | '_ \| | |/ _ \| __| '_ \ / _ \/ _` | | | |/ _ \/ __|
  | |_) | | |_) | | | (_) | |_| | | |  __/ (_| | |_| |  __/\__ \
  |____/|_|_.__/|_|_|\___/ \__|_| |_|\___|\__, |\__,_|\___||___/
                                            | |
                                            |_|
*/
#include <Stepper.h>                    // Moteur pas à pas
#include <Arduino.h>                    // Libraire de base Arduino
#include <TimerOne.h>                   // Permet l'utilisation du timer pour l'interuption
#include <math.h>                       // Afin de gérer les vitesses par calculs
#include <Adafruit_BLE.h>               // Librairie Adafruit bluetooth
#include <Adafruit_BluefruitLE_SPI.h>   // Librairie Adafruit connection SPI
#include <Adafruit_BluefruitLE_UART.h>  // Librairie Adafruit connextion UART
#include "BluefruitConfig.h"            // Librairie Adafruit de configuration
/*
   ____  _     _      _
  / __ \| |   (_)    | |
  | |  | | |__  _  ___| |_ ___
  | |  | | '_ \| |/ _ \ __/ __|
  | |__| | |_) | |  __/ |_\__ \
  \____/|_.__/| |\___|\__|___/
             _/ |
            |__/


     L'ensemble de cete section est donnée par Adafruit pour le bon fonctionnement du bluetooth
     Les paramètre de communications sont définis ici.
*/

// Adafruit, configuration du bluetooth
String BROADCAST_NAME = "Démonstrateur Solaire";
String BROADCAST_CMD = String("AT+GAPDEVNAME=" + BROADCAST_NAME);
Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

// function prototypes over in packetparser.cpp
uint8_t readPacket(Adafruit_BLE *ble, uint16_t timeout);
float parsefloat(uint8_t *buffer);
void printHex(const uint8_t * data, const uint32_t numBytes);
// the packet buffer
extern uint8_t packetbuffer[];
char buf[60];
/*
  __      __        _       _     _
  \ \    / /       (_)     | |   | |
  \ \  / /_ _ _ __ _  __ _| |__ | | ___  ___
   \ \/ / _` | '__| |/ _` | '_ \| |/ _ \/ __|
    \  / (_| | |  | | (_| | |_) | |  __/\__ \
     \/ \__,_|_|  |_|\__,_|_.__/|_|\___||___/


*/
// Mesures
float VRelais1, VRelais2, VRelais3, VRelais4;                 // Entrées du switch
float Vref;                                                   // Tension de référence pour mesures

// Attribution des PINs
int Relais1 = A10, Relais2 = A2, Relais3 = A1, Relais4 = A0;  // Entrées analogique des relais
int MPhoto1 = A11, MPhoto2 = A3, MPhoto3 = A4, MPhoto4 = A5;  // Entrées analogique de photo-résistances.
int RelaisSelect = 9;                                         // Sortie d'état du relais
int Inter = 13;                                               // Position de l'interupteur rotatif
int EnM1 = 1, EnM2 = 0;                                       // Enables des moteurs
int M1A = 2, M1C = 3, M2A = 5, M2C = 6;                       // Sorties sur la commande des moteurs (M1A = 2, M1C = 3, M2A = 5, M2C = 6; )

// Moteur
const int nbrPas = 1;                                         // Nombre de pas des moteurs par appel
int NbrPasMax = 15000;                                        // Nombre de pas d'un moteur afin de faire un tour complet à la roue dentée
int OffsetZenith = 84, OffsetAzimuth = 0;                     // Définition des offset de position en fonction du zéro des butées
int AzimuthMax = NbrPasMax;                                   // Nbr de pas maximum en Azimuth
int ZenithMax = (NbrPasMax / 4);                              // Nbr de pas maximum en Zenith
int AzimuthSud = (NbrPasMax / 2) - OffsetAzimuth;             // Position plein sur en azimuth en nbr de pas
int Zenith60 = ((NbrPasMax / 4) / 3) - OffsetZenith;          // Position Zenith à 60 degrés
int AzimuthRepos = (NbrPasMax / 4) - OffsetAzimuth;           // Position d'Azimuth pour le rangement du module
int ZenithRepos = 1;                                          // Position de Zénith pour le rangement du module
Stepper moteur1(200, M1A, M1C);                               // Pour un moteur de 200 pas par tour (1.8° par pas) et branché sur les pins 2 et 3
Stepper moteur2(200, M2A, M2C);                               // Pour un moteur de 200 pas par tour (1.8° par pas) et branché sur les pins 5 et 6

// Capteur
float Bu1 = 1., Bu2 = 2., Bu3 = 3., Bu4 = 4.;                 // Butées (Float afin de ne pas confondre avec une entrée numérique)

// Calcul généraux
int PhotoFlag = 0;                                            // Flag afin d'effectuer les mesures en mode photorésistance
int adjust = 0;                                               // Flag d'ajustement des mesures en mode photorésistance
int ValArondiAzi = 100, ValArondiZen = 100;                   // Choix de l'arrondi des mesures de potentielle des photorésistances
float HystereseAzi = 10 / ValArondiAzi;                          // Taille de l'hystérèse en azimuth
float HystereseZen = 10 / ValArondiZen;                          // Taille de l'hystérèse en zénith
int NbrControlePhoto = 200;                                   // Nombre de contrôle à faire sur les photorésistance avant d'estimer être stable

// Comunication bluetooth
const int NbrCaract = 13, NbreDonnes = 3;                     // Taille du tableau de donnée et nombre de séprateurs
int tableau [NbrCaract + 1];                                  // Définition du tableau de valeur
int mode = 9, oMode = mode;                                   // Mode de confonctionnement actuel et ancien
long int azimuthMesure = 0, zenithMesure = 0;                 // Mesure en nobre de pas
long int azimuthConsigne = 0, zenithConsigne = 0;             // Consigne de position en nbr de pas
long int oAzimuthConsigne = 0, oZenithConsigne = 0;           // Anciennes consigne de position en nombre de pas

// Temps
int TPauseN = 1800, TPauseD = 20;                             // Temps de pause des modes photorésistance en seconde
int Tinterupt = 10;                                            // Durée de l'interuption en seconde

/*
   _____      _
  / ____|    | |
  | (___   ___| |_ _   _ _ __
  \___ \ / _ \ __| | | | '_ \
  ____) |  __/ |_| |_| | |_) |
  |_____/ \___|\__|\__,_| .__/
                       | |
                       |_|
*/

/*   La fonction "Setup" est appelée une fois au démarage de
     l'Arduino. Elle permet d'effectuée certaines tâche d'initialisation
     qui ont besoin d'être effectuée une seule fois.
*/
void setup() {                                                // the setup routine runs once when you press reset:

  Serial.begin(115200);                                       // Initialize serial communication at 115200 bits per second
  //      Valeur recommandée par Adafruit pour le module bluetooth
  // Moteur
  moteur1.setSpeed(120);                                      // 120 tours par minute
  moteur2.setSpeed(30);                                       // 30 tours par minute

  //Pin mode
  pinMode(EnM1, OUTPUT);
  pinMode(EnM2, OUTPUT);
  pinMode(RelaisSelect, OUTPUT);
  pinMode(M1A, OUTPUT);
  pinMode(M1C, OUTPUT);
  pinMode(M2A, OUTPUT);
  pinMode(M2C, OUTPUT);
  pinMode(Inter, INPUT);
  pinMode(MPhoto1, INPUT);
  pinMode(MPhoto2, INPUT);
  pinMode(MPhoto3, INPUT);
  pinMode(MPhoto4, INPUT);
  pinMode(Relais1, INPUT);
  pinMode(Relais2, INPUT);
  pinMode(Relais3, INPUT);
  pinMode(Relais4, INPUT);

  // Initialisation du Bluetooth
  BLEsetup();                                                 // Fonction provenant d'Adafruit

  // Routine d'interuption
  Timer1.initialize(Tinterupt * 1000000);                     // Initialisation du timer en microseconde (ici  10 [s] = 10 [Hz])
  Timer1.attachInterrupt(timerIsr);                           // Initialisation de la fonction liée à l'interuption
  Timer1.start();                                             // Début de comptage

  // Référence
  Vref = 3.33;                                                // Valeur moyenne du potentiel allimantant l'adafruit

  // Initialsation des moteurs
  initMoteur(&azimuthMesure, &zenithMesure, &azimuthConsigne, &zenithConsigne, &oAzimuthConsigne, &oZenithConsigne);
}
/*
  _
  | |
  | |     ___   ___  _ __
  | |    / _ \ / _ \| '_ \
  | |___| (_) | (_) | |_) |
  |______\___/ \___/| .__/
                   | |
                   |_|
*/

/*   La focntion "loop" est une focntion de type "main", elle
     est appelée en boucle tant que l'arduino fonctionne et que
     la fonction "setup"(si présente) est terminée.
     Seule exeption à cette focntion: Interuption
    Dans cette fonction est appeler la recherche de nouvelle données
    provenant de la tablette. La traitement de ces données est traité
    ici. LE choix du mode defocntionement et l'appel des focntions
    respectives se fait aussi ici.
*/
void loop() {
  // Reception donnée tablette
  receptionBLE(tableau, NbrCaract);                           // Recpetion d'éventuelles données de la Tablette

  // Contrôle bouton
  controleOnOff(&mode);                                       // Contrôle de l'état de l'interupteur rotatif

  // Analyse des données
  if (char (tableau[1]) != ' ') {
    oMode = mode;
    mode = tableau[1];
    if (oMode != mode)
      PhotoFlag = 0;
    else;
    for (int i = 1 ; i <= NbrCaract; i++ ) {
      if (char (tableau[i]) == ' ')
        tableau[i] = 0;
    }
    oAzimuthConsigne = azimuthConsigne;                       // enrigistrement de l'ancienne valeur de consigne
    oZenithConsigne = zenithConsigne;                         // enrigistrement de l'ancienne valeur de consigne
    azimuthConsigne = 10000 * tableau[3] + 1000 * tableau[4] + 100 * tableau[5] + 10 * tableau[6] + tableau[7]; // enrigitrement de la nouvell consigne
    zenithConsigne = 1000 * tableau[9] + 100 * tableau[10] + 10 * tableau[11] + tableau[12]; // enrigitrement de la nouvell consigne
  }
  // Mode Photo-résistance
  if (mode == 0 || mode == 7) {
    if (PhotoFlag == 0) {
      moteurPaPPhoto(&adjust, mode, nbrPas, (lireTension(MPhoto1) - lireTension(MPhoto2)), (lireTension(MPhoto3) - lireTension(MPhoto4)), &azimuthMesure, &zenithMesure, &azimuthConsigne, &zenithConsigne, &PhotoFlag);
      Serial.print(String(lireTension(MPhoto1)));
      Serial.print("    ");
      Serial.println(String(lireTension(MPhoto2)));
      Serial.print("    ");
      Serial.print(String(lireTension(MPhoto3)));
      Serial.print("    ");
      Serial.println(String(lireTension(MPhoto4)));
    }
    else;
  }
  // Mode GPS/horaire
  else if (mode == 1) {
    if ((azimuthConsigne != azimuthMesure) || (zenithConsigne != zenithMesure))
      moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    else;
  }
  // Mode horizontale
  else if (mode == 2) {
    if ((azimuthConsigne != azimuthMesure) || (zenithConsigne != zenithMesure)) {
      moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    }
  }
  // Mode Verticale
  else if (mode == 3) {
    if ((azimuthConsigne != azimuthMesure) || (zenithConsigne != zenithMesure)) {
      moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    }
  }
  // Cap plein Sud
  else if (mode == 4) {
    if ((azimuthConsigne != azimuthMesure) || (zenithConsigne != zenithMesure)) {
      moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    }
  }
  // Mode Manuel
  else if (mode == 5) {
    if ((azimuthConsigne != azimuthMesure) || (zenithConsigne != zenithMesure)) {
      moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    }
  }
  // Mode roulement
  else if (mode == 6) {
    oAzimuthConsigne = azimuthConsigne;
    azimuthConsigne = OffsetAzimuth;
    zenithConsigne = zenithConsigne;
    moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    oZenithConsigne = zenithConsigne;
    azimuthConsigne = azimuthConsigne;
    zenithConsigne = OffsetZenith;
    moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    oAzimuthConsigne = azimuthConsigne;
    azimuthConsigne = AzimuthMax-OffsetAzimuth;
    zenithConsigne = zenithConsigne;
    moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
    oZenithConsigne = zenithConsigne;
    azimuthConsigne = azimuthConsigne;
    zenithConsigne = ZenithMax-OffsetZenith;
    moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
  }
  // Mode rengement
  else if (mode == 8) {
    if ((azimuthConsigne != azimuthMesure) || (zenithConsigne != zenithMesure)) {
      oAzimuthConsigne = azimuthConsigne;
      oZenithConsigne = zenithConsigne;
      azimuthConsigne = AzimuthRepos;
      zenithConsigne = ZenithRepos;
      moteurPAPGPS(nbrPas, azimuthConsigne, zenithConsigne, &azimuthMesure, &zenithMesure, &oAzimuthConsigne, &oZenithConsigne);
      for (int i = 1; i > 0; i++);                              // Attente infinie afin de ne plus toucher l'instalation sans remettre l'interupteur à "1" après coupure du relais tempo.
    }
  }
  // Mode réinitialisation
  else if (mode == 9) {                                       // réinitialisation
    oAzimuthConsigne = azimuthConsigne;
    oZenithConsigne = zenithConsigne;
    initMoteur(&azimuthMesure, &zenithMesure, &azimuthConsigne, &zenithConsigne, &oAzimuthConsigne, &oZenithConsigne);
    mode = 0;
  }
  // Erreur
  else {
    //envoisBLE(float(404), float(404), float(404), float(404) , float(404), float(404), float(404));
    //delay(1000);
    Serial.println("Erreur");
  }
}


/*
  ______                          _     _
  |  ____|                        | |   (_)
  | |__      ___    _ __     ___  | |_   _    ___    _ __    ___
  |  __|    / _ \  | '_ \   / __| | __| | |  / _ \  | '_ \  / __|
  | |      | (_) | | | | | | (__  | |_  | | | (_) | | | | | \__ \
  |_|       \___/  |_| |_|  \___|  \__| |_|  \___/  |_| |_| |___/

*/

/*    La fonction "initMoteur" est appelée lors du "setup". Elle a pour instruction de
      trouver les points zéros (butées) de chacun des moteurs.
      Pour ce faire, il y a plusiqurs étapes:
      1) retour des deux moteur aux butées.
      2) écarter les deux moteur des butées
      3) retour plus lentement des deux moteurs aux butées
      4) mise en plus sud du panneau solaire et activation du Timer

*/
void initMoteur(long int *azimuth, long int *zenith, long int *azimuthConsigne, long int *zenithConsigne, long int *oAzimuthConsigne, long int *oZenithConsigne) {
  int sens = 0;

  Timer1.stop();

  //1er contrôle des butées
  controleButee(Bu1);
  controleButee(Bu2);
  controleButee(Bu3);
  controleButee(Bu4);

  // Step 1; Initialisation grossière
  initZenGros(&*zenith);
  initAziGros(&*azimuth, sens);

  // Step 2; Initialisation fine
  *azimuthConsigne = 300;
  *zenithConsigne = 120;
  moteurPAPGPS(nbrPas, *azimuthConsigne, *zenithConsigne, &*azimuth, &*zenith, &*oAzimuthConsigne, &*oZenithConsigne);
  initZenFin(&*zenith);
  initAziFin(&*azimuth, sens);

  // Step 3; Mise en plein Sud
  *azimuthConsigne = AzimuthSud;
  //*azimuthConsigne = 1000;
  *zenithConsigne = Zenith60;
  moteurPAPGPS(nbrPas, *azimuthConsigne, *zenithConsigne, &*azimuth, &*zenith, oAzimuthConsigne, oZenithConsigne);
  mode = 0;
  Timer1.restart();
}

/*   La fonction "initAziGros" envois le moteur 1 en butée avec un démarage en douceur.
      Cette focntion sert aussi de retour à la butée opposée dans le cas ou le mode
      photoresistance l'impose.
*/
void initAziGros(long int *azi, int Sens) {
  int count = 0, count2 = 0;
  digitalWrite(EnM1, HIGH);
  delay(3);
  if (Sens == 0) {
    while (controleButee(Bu4) == 0) {
      if (count < 400) {
        count = count + 1;
        moteur1.step(nbrPas);
        delay(int(0.0000000002 * pow(float(count), 4) - 0.0000000004 * pow(float(count), 3) - 0.00008 * pow(float(count), 2) + 0.00008 * float(count) + 9));
      }
      else {
        moteur1.step(nbrPas);
        delay(1);
      }
    }
    *azi = 0;
  }
  else {
    while (controleButee(Bu3) == 0) {
      if (count < 400) {                                      // Gestion du démarage
        count = count + 1;
        moteur1.step(-nbrPas);
        delay(int(0.0000000002 * pow(float(count), 4) - 0.0000000004 * pow(float(count), 3) - 0.00008 * pow(float(count), 2) + 0.00008 * float(count) + 9));
      }
      else {
        moteur1.step(-nbrPas);
        count2 = count2 + 1;
        delay(1);
      }
    }
    *azi = count2;
  }
  digitalWrite(EnM1, LOW);
}

/*   La fonction "initZenGros" envois le moteur 2 en butée avec un démarage en douceur.
*/
void initZenGros(long int *zen) {
  int count = 0;
  digitalWrite(EnM2, HIGH);
  delay(3);
  while (controleButee(Bu2) == 0) {
    if (count < 200) {                                        // Gestion du démarage
      count = count + 1;
      moteur2.step(nbrPas);
      delay(int(0.0000000002 * pow(float(count), 4) - 0.0000000004 * pow(float(count), 3) - 0.00008 * pow(float(count), 2) + 0.00008 * float(count) + 9));
    }
    else {
      moteur2.step(nbrPas);
      delay(2);
    }
  }
  digitalWrite(EnM2, LOW);
  *zen = 0;
}

/*   La fonction "initZAziFin" envois le moteur 1 en butée à une vitesse ralentie.
*/
void initAziFin(long int *azi, int S) {
  int count = 0;
  if (S == 0) {
    digitalWrite(EnM1, HIGH);
    delay(3);
    while (controleButee(Bu4) == 0) {
      moteur1.step(nbrPas);
      delay(30);
    }
    digitalWrite(EnM1, LOW);
    *azi = 0;
  }
  else {
    digitalWrite(EnM1, HIGH);
    delay(3);
    while (controleButee(Bu3) == 0) {
      moteur1.step(-nbrPas);
      delay(30);
      count = count + 1;
    }
    digitalWrite(EnM1, LOW);
    *azi = count;
  }
}

/*   La fonction "initZZenFin" envois le moteur 2 en butée à une vitesse ralentie.
*/
void initZenFin(long int *zen) {
  digitalWrite(EnM2, HIGH);
  delay(3);
  while (controleButee(Bu2) == 0) {
    moteur2.step(nbrPas);
    delay(30);
  }
  digitalWrite(EnM2, LOW);
  *zen = 0;
}

/*    La fonction "controleOnOff" vérifie l'état de l'interupteur en deux contrôles
      espacé de 10[ms] (afin luté contre une baisse de tensino parasite).
      Elle défini le mode "renagement" le cas échéant.
*/
void controleOnOff(int *M) {
  if (digitalRead(Inter) == LOW) {
    delay(10);
    if (digitalRead(Inter) == LOW)
      *M = 8;
    else;
  }
  else;
}

/*   La fonction "lireTension" en fortement inspirée des fonction de base Arduino.
     Elle mesure le ptotentielle à la Pin selectionnée et applique la relation
     d'adptation dû au convertisseur AD.
     Pour gagné en précision (au détriment de la durée) plusieur itération peuvent
     être effectuées.
     Elle retourne le potentiel demandé sous forme de valeur analogique en float.

*/
float lireTension(int Ax) {
  int NbrIteration = 2;
  float tension = 0. , sensorValue = 0.;
  delay(3);                                                   // delay in between reads for stability
  for (int i = 0; i < NbrIteration; i++) {
    sensorValue = analogRead(Ax);
    delay(3);                                                 // delay in between reads for stability
    tension = tension + (sensorValue * Vref / 1023.0);        // Convert the analog reading (which goes from 0 - 1023) to a voltage (3.3V):
  }
  tension = tension / NbrIteration;
  return tension;
}

/*   La fonction "controleVitessePAP" permet un démarage/arret en douceur des moteur par application
     de l'équiation suivante: < Attente = 2E-05*position + 4.0539 > infulanceant le temps d'attente
     entre les pas (~0 à 9 [ms] d'attente)
*/
void controleVitessePAP(long int consigne, long int *mesure, long int *oConsigne) {
  int Temp1 = -(consigne - *mesure);                          // Décélération
  int Temp2 = -(*mesure - consigne);                          // Décélération
  int Temp3 = (*oConsigne - *mesure);                         // Accélération
  int Temp4 = (*mesure - *oConsigne);                         // Accélération

  if ( ((consigne >= *mesure) && (-Temp1) < 400) ) {
    delay(int(0.0000000002 * pow(float((Temp1)), 4) - 0.0000000004 * pow(float((Temp1)), 3) - 0.00008 * pow(float((Temp1)), 2) + 0.00008 * float((Temp1)) + 9));
  }
  else if ( ((*mesure >= consigne) && (-Temp2) < 400) ) {
    delay(int(0.0000000002 * pow(float((Temp2)), 4) - 0.0000000004 * pow(float((Temp2)), 3) - 0.00008 * pow(float((Temp2)), 2) + 0.00008 * float((Temp2)) + 9));
  }
  else ;
  if ( ((*oConsigne >= *mesure) && (Temp3) < 400) ) {
    delay(int(0.0000000002 * pow(float((Temp3)), 4) - 0.0000000004 * pow(float((Temp3)), 3) - 0.00008 * pow(float((Temp3)), 2) + 0.00008 * float((Temp3)) + 9));
  }
  else if ( ((*mesure >= *oConsigne) && (Temp4) < 400) ) {
    delay(int(0.0000000002 * pow(float((Temp4)), 4) - 0.0000000004 * pow(float((Temp4)), 3) - 0.00008 * pow(float((Temp4)), 2) + 0.00008 * float((Temp4)) + 9));
  }
  else ;
}

/*  Cette fonction permet aux moteur d'allé à une position souhaitées tout en contr'olant la véracité des
    positions demandée et contrôlant aussi l'état des butées.
    Elle est intimement liée à la fonction "controleVitessePAP" et est appelée à chaque fois qu'un moteur
    doit se rendre à un emplacement connu souhaité.
*/
void moteurPAPGPS(int p1, long int aziC, long int zenC, long int *aziM, long int *zenM, long int *oAziC, long int *oZenC) {
  // Contrôle des offests
  if (mode == 1) {
    if (zenC > 0 && zenC < OffsetZenith)
      zenC = OffsetZenith;
    else;
    if (aziC > 0 && aziC < OffsetAzimuth)
      aziC = OffsetAzimuth;
    else;
  }
  else;

  // Application des consignes
  if (zenC > 0 && zenC <= ZenithMax) {
    while (zenC > *zenM) {
      if (controleButee(Bu1) == 0) {
        Timer1.stop();
        digitalWrite(EnM2, HIGH);
        delay(3);
        controleVitessePAP(zenC, &*zenM, &*oZenC);
        moteur2.step(-p1);
        digitalWrite(EnM2, LOW);
        *zenM = *zenM + p1;
      }
      else{
        if (mode != 9)
          Timer1.restart();
        break;
      }
    }
    while (zenC < *zenM) {
      if (controleButee(Bu2) == 0) {
        Timer1.stop();
        digitalWrite(EnM2, HIGH);
        delay(3);
        controleVitessePAP(zenC, &*zenM, &*oZenC);
        moteur2.step(p1);
        digitalWrite(EnM2, LOW);
        *zenM = *zenM - p1;
      }
      else{
        if (mode != 9)
          Timer1.restart();
        break;
      }
    }
  }

  if ((aziC > 0) && (aziC <= AzimuthMax)) {
    while (aziC > *aziM) {
      if (controleButee(Bu3) == 0) {
        Timer1.stop();
        digitalWrite(EnM1, HIGH);
        delay(3);
        controleVitessePAP(aziC, &*aziM, &*oAziC);
        moteur1.step(-p1);
        digitalWrite(EnM1, LOW);
        *aziM = *aziM + p1;
      }
      else{
        if (mode != 9)
          Timer1.restart();
        break;
      };
    }
    while (aziC < *aziM) {
      if (controleButee(Bu4) == 0) {
        Timer1.stop();
        digitalWrite(EnM1, HIGH);
        delay(3);
        controleVitessePAP(aziC, &*aziM, &*oAziC);
        moteur1.step(p1);
        digitalWrite(EnM1, LOW);
        *aziM = *aziM - p1;
      }
      else{
        if (mode != 9)
          Timer1.restart();
        break;
      }
    }
    if (mode != 9)
      Timer1.restart();
  }
  else;
}

/*   La Fonction "moteurPaPPhoto" est appeler lorsque les mode phtotrésistance sont activé. Elle reçois les
     deux différences de potentielles mesures à chaque paire de photorésistances. Elle reCois aussi le mode
     de focntionnement (démo ou utilisation) ainsi que les flag, nombre de pas par impulsion et l'emplacement
     des moteurs (mesures et consignes)
     Cette focntion effectue un arrondi ds mesure et les compare à une valeur définie dans une hystérèse. Elle
     met à jour l'emplacement mesuré des moteurs et active un flag.
     Une fois à l'équilibre, est encore appelée plusieurs fois afin d'être plus au juste des mesures de
     lumières

*/
void moteurPaPPhoto(int *Adj, int M, int p1, float VMPhoto12, float VMPhoto34, long int *azi, long int *zen, long int *oAziC, long int *oZenC, int *Flag) {
  int FlagAzi = 0, FlagZen = 0, sens = 1;
  // Contrôle pour azimuth
  if ((float(int (VMPhoto12 * float(ValArondiAzi))) / ValArondiAzi) < -HystereseAzi) { // contrôle la valeur soustraite arrondie à 2 chiffre après la virgule
    if (controleButee(Bu3) == 0) {
      digitalWrite(EnM1, HIGH);
      delay(3);
      moteur1.step(-p1);
      *oAziC = *azi;
      *azi = *azi + p1;
      digitalWrite(EnM1, LOW);
    }
    else {
      if (controleButee(Bu1) == 1) {
        sens = 0;
        initAziGros(&*azi, sens);
        digitalWrite(EnM1, HIGH);
        delay(3);
        for (int i = 0; i < 200; i++) {
          moteur1.step(p1);
          delay(1);
        }
        initAziFin(&*azi, sens);
        *oAziC = *azi;
        digitalWrite(EnM1, LOW);
      }
    }
  }
  else if ((float(int (VMPhoto12 * float(ValArondiAzi))) / ValArondiAzi) > HystereseAzi) {
    if (controleButee(Bu4) == 0) {
      digitalWrite(EnM1, HIGH);
      delay(3);
      moteur1.step(p1);
      *oAziC = *azi;
      *azi = *azi - p1;
      digitalWrite(EnM1, LOW);
    }
    else {
      if (controleButee(Bu1) == 1) {
        sens = 1;
        initAziGros(&*azi, sens);
        digitalWrite(EnM1, HIGH);
        delay(3);
        for (int i = 0; i < 200; i++) {
          moteur1.step(p1);
          delay(1);
        }
        initAziFin(&*azi, sens);
        *oAziC = *azi;
        digitalWrite(EnM1, LOW);
      }
    }
  }
  else
    FlagAzi = 1;

  // Contrôle pour le Zénith
  if ((float(int (VMPhoto34 * float(ValArondiZen))) / ValArondiZen) < -HystereseZen) {        // contrôle la valeur soustraite arrondie à 1 chiffre après la virgule
    if (controleButee(Bu2) == 0) {
      digitalWrite(EnM2, HIGH);
      delay(3);
      moteur2.step(p1);
      *oZenC = *zen;
      *zen = *zen - p1;
      digitalWrite(EnM2, LOW);
    }
    else;
  }
  else if ((float(int (VMPhoto34 * float(ValArondiZen))) / ValArondiZen) > HystereseZen) {
    if  (controleButee(Bu1) == 0) {
      digitalWrite(EnM2, HIGH);
      delay(3);
      moteur2.step(-p1);
      *oZenC = *zen;
      *zen = *zen + p1;
      digitalWrite(EnM2, LOW);
    }
    else;
  }
  else
    FlagZen = 1;

  //Multiple contrôle
  if (FlagAzi == 1 && FlagZen == 1) {
    *Adj = *Adj + 1;
    if (*Adj == 200)
      if (M == 0) {
        *Flag = 180;                              // Nbr d'interuption avant la reprise.
        *Adj = 0;
      }
      else {
        *Flag = 2;                                // Nbr d'interuption avant la reprise.
        *Adj = 0;
      }
  }
  else;
}

/*   La Fonction "envoisBLE" est appelée avec les données à transmettre à l'application Android en
     UART. Elle ajoute un séparateur entre chaque données puis appel la focntion Adafruit permettant
     l'envois des données.
*/

void envoisBLE(float data0, float data1, float data2, float data3, float data4, float data5, float data6) {
  String Separateur = ";";
  String ToSend = String (data0);
  ToSend += Separateur;
  ToSend += String(data1);
  ToSend += Separateur;
  ToSend += String(data2);
  ToSend += Separateur;
  ToSend += String(data3);
  ToSend += Separateur;
  ToSend += String(data4);
  ToSend += Separateur;
  ToSend += String(data5);
  ToSend += Separateur;
  ToSend += String(data6);
  ToSend += Separateur;
  ble.println(ToSend);
}

/*   La fonction "receptionBLE" est fotement inspirée des focntions proposée dans les projets
     d'Adafruit. Elle contrôle l'arrivée de nouvelle donnée par charactère uniquement.
     Tant qu'il y a des caractère en "attente" elle enregistre ces caractère dans un tableau dont
     la dimension est pré-définie.
     Afin d'avoir une veuleur, les données reçu sont comparées puis converties selon la table ASCI.
*/
void receptionBLE(int tab[], int Nbr) {
  // Echo received data
  int c = 0, count = 0;
  while (ble.available())
  {
    c = ble.read();
    count = count + 1;
    if (c >= 48 && c <= 57)                                   // Contrôle table ASCII
      tab[count] = c - 48;                                    // Convertion de la valeur "Chr" en "Dec"
    else if (c == 59)                                         // Séprarteur à conserver
      tab[count] = c;
    else if (c == 45)                                         // Trait d'union à conserver
      tab[count] = c;
    else
      tab[count] = 418;                                       // Erreur (Je suis une théière) donnée erronée
  }
  while (count <= Nbr) {                                      // mettre des espace dans les emplacement non
    tab[count] = ' ';                                         //   remplis si envois partiel ou non-envois
    count = count + 1;
  }
}

/*  La Fonction "controleButee" est une fonction allant lire l'état de l'entrée du switch,
    selectrionnée à son appel. Dans ce cas, le commande du relais est au repos.
    Le calcul de potentiel est effectuée. Le résultat est comparé à une valeur intermédiaire.
    En effet, la butée fonctionne en tout ou rien.
    La fonctione retourne en binaire l'état de la butée concernée.
*/
int controleButee (int Num) {
  int EtatButee = 1, entree = 0;
  // Lien entre les butées et leur emplacement sur l'adafruit,
  if (Num == 1.)
    entree = Relais1;
  else if (Num == 2.)
    entree = Relais2;
  else if (Num == 3.)
    entree = Relais3;
  else if (Num == 4.)
    entree = Relais4;
  else;
  digitalWrite(RelaisSelect, LOW);                               // Imposer la position repos au relais
  delay(3); // delay in between reads for stability
  if (analogRead(entree) * Vref / 1023 > 2.5)                 // Contrôle de l'état de la butée avec conversion DA
    EtatButee = 1; // butée activée
  else
    EtatButee = 0; // butée inactive
  digitalWrite(RelaisSelect, LOW);
  return EtatButee;
}

/*
   La fonction timerIsr() est une fonction propre à Arduino. Elle permet l'utilisation d'interuption.
   Plus préciséement, elle effectue ce qui est dans la fonction lors de l'interuption.
   En l'occurence, elle demandera une lecture des mesures en activant préalablement le swtich.
   Elle envera ces mesures à la tablette par Bluetooth.

   Cette focntion sert également de comptage pour le temps d'interuption de mesures lorsque l'appareil
   est en mode "maximum de lumière"

   Cette fonction est appelée automatiquement toute les 10 secondes.

   Attention: lorsque cette fonction est activée, rien d'autre ne fonctionne.
*/
void timerIsr()
{
  float temp = 0;
  digitalWrite(RelaisSelect, HIGH);                            // Switch les position du relais afin d'acceder aux mesures
  delay(200);                                                  

  temp = lireTension(Relais1);
  delay(3);
  //VRelais1 = temp;
  VRelais1 = (-89.28571 * (Vref - (2 * temp))) / Vref;  // Adaptation de mesure tension en courant selon fourniseur, pour la batterie
  temp = lireTension(Relais2);
  delay(3);
  //VRelais2 = temp;
  VRelais2 = temp * (2.191 + 17.90) / 2.191;      // Rapport diviseur de tension pour la batterie
  temp = lireTension(Relais3);
  delay(3);
  //VRelais3 = temp;
  VRelais3 = (73.3 * temp / Vref) - 36.7;         // Adaptation de mesure tension en courant selon fourniseur, pour le PV
  temp = lireTension(Relais4);
  delay(3);
  //VRelais4 = temp;
  VRelais4 = temp * (2.191 + 17.97) / 2.191;      // Rapport diviseur de tension pour le PV
  digitalWrite(RelaisSelect, LOW);                            // Remise au repos du relais

  // Envois des données à la tablette
  envoisBLE(float(mode), float(azimuthMesure), float(zenithMesure), VRelais1, VRelais2, VRelais3, VRelais4);

  // Reception donnée tablette (redondance)
  receptionBLE(tableau, NbrCaract);                           // Recpetion d'éventuelles données de la Tablette

  // Compteur pour les modes photo-résistance
  if ((mode == 0 || mode == 7) && PhotoFlag != 0)
    PhotoFlag = PhotoFlag - 1;

    controleOnOff(&mode);
}

/*   La fonction "BLEsetup" à été determinée grâce à l'analyse de nombreux projet Adafruit OpenSource.
     Elle permet l'instanciation d'une connexion Bluetooth.
*/
//   Déterminer avec des exemples d'instalation adafruit
void BLEsetup() {
  if ( !ble.begin(VERBOSE_MODE) )
  {
    error(F("Couldn't find Bluefruit, make sure it's in CoMmanD mode & check wiring?"));
  }

  /* Perform a factory reset to make sure everything is in a known state */
  if (! ble.factoryReset() ) {
    error(F("Couldn't factory reset"));
  }

  //Convert the name change command to a char array
  BROADCAST_CMD.toCharArray(buf, 60);

  //Change the broadcast device name here!
  if (ble.sendCommandCheckOK(buf)) {
    //name changed
  }
  delay(250);

  //reset to take effect
  if (ble.sendCommandCheckOK("ATZ")) {
    //resetting
  }
  delay(250);

  //Confirm name change
  ble.sendCommandCheckOK("AT+GAPDEVNAME");

  /* Disable command echo from Bluefruit */
  ble.echo(false);
  //Requesting Bluefruit info:

  /* Print Bluefruit information */
  ble.info();
  // Then Enter characters to send to Bluefruit

  ble.verbose(false);  // debug info is a little annoying after this point!

  /* Wait for connection */
  while (! ble.isConnected()) {
    delay(500);
  }
  // Set Bluefruit to DATA mode
  ble.setMode(BLUEFRUIT_MODE_DATA);
}
