package ch.heigvd.huguelet.demonstrateursolaire.utils;

import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.abs;
import static java.lang.Math.tan;

/*
Cette fonction dÈtermine la position du soleil en fonction des coordonÈes
GPS, de la date et l'heure. Elle retourne la position du soleil vu
depuise l'observateur.
Elle a besoin en paramËtres :[ Latitude
                               Longitude
                               Fuseau horaire (heure d'hiver)
                               AnnÈe
                               Mois
                               Jour
                               Heure
                               Minutes
                               Seconde]
Elle retourne :               [Angle de la hauteur de soleil
                               Azimuth]
*/
public class SolarPosition {

    private double AngHauSolVrai = 0, Azimuth = 0;

    public SolarPosition(double Lat, double Lon, double TimeZone, double A, double M, double J, double HH, double MM, double SS) {

        double JJul = 0;                       // Jour Julien
        double SJul = 0;                       // Siècle Julien
        double SolMoyLon = 0;                  // Centre géométrique moyen du soleil
        double SolMoyAno = 0;                  // Anomalie moyenne du soleil
        double ExeOrb = 0;                     // Excentricité orbitale de la terre
        double EquCent = 0;                    // Equation du centre
        double SolLonVrai = 0;                 // Longitude vrai du soleil
        double SolAnoVrai = 0;                 // Anomalie vraie du soleil
        double DistSol = 0;                    // Distance au soleil en [AU]
        double AngSolApp = 0;                  // Angle solaire
        double EliMoy = 0;                     // Moyenne de l'elipse oblique
        double OblCor = 0;                     // Correction oblique
        double AscSol = 0;                     // Ascention solaire
        double DecSol = 0;                     // Déclinaison slaire
        double ValSim = 0;                     // Varialbe afin d'allégéer l'affichage d'équation
        double EqTemp = 0;                     // Equation temporel en minutes
        double AngHor = 0;                     // Angle horaire
        double TempSid = 0;                    // Temps sidérale
        double LevSol = 0;                     // Heure du levé de soleil local
        double CouSol = 0;                     // Heure du couché de soleil local
        double DurJou = 0;                     // Durée du jour en minutes
        double HeuSolVrai = 0;                 // Vrai heure solaire
        double AngHorVrai = 0;                 // Angle horaire vrai
        double Zenith = 0;                     // Zénith solaire
        double AngHauSol = 0;                  // Angle d'élévation solaire
        double AngRefAtm = 0;                  // Angle de réfraction du à l'ahtmosphère
        double temp = 0;                          // Valeur temporaire pour calcul

        // Calculs du jour en canlendrier Julien
        if (M == 1 || M == 2) { //Contrôle du mois car le calendrier Julien en a 14
            A = A - 1;
            M = M + 12;
        }
        double C = (int) (A / 100);
        double B = 2 - C + (int) (C / 4);
        double T = HH / 24 + MM / 1440 + SS / 86400;
        temp = 365.25 * (A + 4716);
        double JJ = (int)temp;
        temp = 30.6001 * (M + 1);
        JJ = JJ +(int)temp;
        JJ = JJ + J + T + B - 1524.5;

        // Calculs de la position du soleil
        SJul = (JJul - 2451545) / 36525;
        SolMoyLon = mod(280.46646 + SJul * (36000.76983 + SJul * 0.0003032), 360);
        SolMoyAno = 357.52911 + SJul * (35999.05029 - 0.0001537 * SJul);
        ExeOrb = 0.016708634 - SJul * (0.000042037 + 0.0000001267 * SJul);
        EquCent = sin((SolMoyAno) * pi / 180) * (1.914602 - SJul * (0.004817 + 0.000014 * SJul)) + sin((2 * SolMoyAno) * pi / 180) * (0.019993 - 0.000101 * SJul) + sin((3 * SolMoyAno) * pi / 180) * 0.000289;
        SolLonVrai = SolMoyLon + ExeOrb;
        SolAnoVrai = SolMoyAno + EquCent;
        DistSol = (1.000001018 * (1 - ExeOrb * ExeOrb)) / (1 + ExeOrb * cos((SolAnoVrai) * pi / 180));
        AngSolApp = SolLonVrai - 0.00569 - 0.00478 * sin((125.04 - 1934.136 * SJul) * pi / 180);
        EliMoy = 23 + (26 + ((21.448 - SJul * (46.815 + SJul * (0.00059 - SJul * 0.001813)))) / 60) / 60;
        OblCor = EliMoy + 0.00256 * cos((125.04 - 1934.136 * SJul) * pi / 180);
        AscSol = (atan2(cos((OblCor) * pi / 180) * sin((AngSolApp) * pi / 180), cos((AngSolApp) * pi / 180))) * 180 / pi;
        DecSol = (asin(sin((OblCor) * pi / 180) * sin((AngSolApp) * pi / 180))) * 180 / pi;
        ValSim = tan((OblCor / 2) * pi / 180) * tan((OblCor / 2) * pi / 180);
        EqTemp = 4 * (ValSim * sin(2 * (SolMoyLon) * pi / 180) - 2 * ExeOrb * sin((SolMoyAno) * pi / 180) + 4 * ExeOrb * ValSim * sin((SolMoyAno) * pi / 180) * cos(2 * (SolMoyLon) * pi / 180) - 0.5 * ValSim * ValSim * sin(4 * (SolMoyLon) * pi / 180) - 1.25 * ExeOrb * ExeOrb * sin(2 * (SolMoyAno) * pi / 180)) * 180 / pi;
        AngHor = (acos(cos((90.833) * pi / 180) / (cos((Lat) * pi / 180) * cos((DecSol) * pi / 180)) - tan((Lat) * pi / 180) * tan((DecSol) * pi / 180))) * 180 / pi;


        double datenum = ((HH*60 + MM + SS/60)* 6.944444444 * Math.pow(10, -4));
        HeuSolVrai = (datenum*1440+EqTemp+4*Lon-60*TimeZone) % 1440;


        if (HeuSolVrai / 4 < 0) {
            AngHorVrai = HeuSolVrai / 4 + 180;
        }
        else {
            AngHorVrai = HeuSolVrai / 4 - 180;
        }


        Zenith = (acos(sin((Lat) * pi / 180) * sin((DecSol) * pi / 180) + cos((Lat) * pi / 180) * cos((DecSol) * pi / 180) * cos((AngHorVrai) * pi / 180))) * 180 / pi;
        AngHauSol = 90 - Zenith;


        if (AngHauSol > 85) {
            AngRefAtm = 0;
        }
        else {
            if (AngHauSol > 5) {
                AngRefAtm = (58.1 / tan((AngHauSol) * pi / 180) - 0.07 / Math.pow(tan((AngHauSol) * pi / 180),3) + 0.000086 / Math.pow(tan((AngHauSol) * pi / 180),5)) / 3600;
            }
            else {
                if (AngHauSol > -0.575) {
                    AngRefAtm = 1735 + AngHauSol * (-518.2 + AngHauSol * (103.4 + AngHauSol * (-12.79 + AngHauSol * 0.711)));
                } else {
                    AngRefAtm = (-20.772 / tan((AngHauSol) * pi / 180)) / 3600;
                }
            }
        }

        AngHauSolVrai = AngHauSol + AngRefAtm;

        if (AngHorVrai > 0) {
            Azimuth = mod(abs(acos(((sin((Lat) * pi / 180) * cos((Zenith) * pi / 180)) - sin((DecSol) * pi / 180)) / (cos((Lat) * pi / 180) * sin((Zenith) * pi / 180))) * 180 / pi + 180), 360);
        }
        else {
            Azimuth = mod(abs((540 - (acos(((sin((Lat) * pi / 180) * cos((Zenith) * pi / 180)) - sin((DecSol) * pi / 180)) / (cos((Lat) * pi / 180) * sin((Zenith) * pi / 180)))) * 180 / pi)), 360);
        }
    }

    public double getZenith() {
        return AngHauSolVrai;
    }

    public double getAzimuth() {
        return Azimuth;
    }


    private static double pi = Math.PI;

    private double mod(double x, double y) {
        return x % y;
    }
}