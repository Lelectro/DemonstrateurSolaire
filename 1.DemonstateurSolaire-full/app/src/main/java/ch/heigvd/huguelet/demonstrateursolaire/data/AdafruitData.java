package ch.heigvd.huguelet.demonstrateursolaire.data;

import android.util.Log;

import ch.heigvd.huguelet.demonstrateursolaire.csv.CSVManager;
import ch.heigvd.huguelet.demonstrateursolaire.utils.NumerikConvert;
import ch.heigvd.huguelet.demonstrateursolaire.utils.TextFormater;

/**
 * AdafruitData
 * - Tout est en float avec deux nombre après la virgule
 */
public class AdafruitData implements CSVManager.ICSVManager {

    /*
     * Mode de fonctionnement
     */
    final ch.heigvd.huguelet.demonstrateursolaire.data.TabletCommand.Mode mode;

    /*
     * Position horizontale en nbr de pas depuis le zéro fixé à la butée.
     */
    final float horizontalPosition;

    /*
     * Position Verticale en nbr de pas depuis le zéro fixé à la butée.
     */
    final float verticalPosition;

    /*
     * Courant du panneau solaire.
     */
    final float pannelCurrent;

    /*
     * Tension du panneau solaire.
     */
    final float pannelVoltage;

    /*
     * Courant de sortie de la batterie.
     */
    final float batteryCurrent;

    /*
     * Tension de la batterie.
     */
    final float batteryVoltage;

    /*
     * Timestamp des infos reçu
     */
    final String dateTime;


    public AdafruitData(String dateTime, String fullText) {
        String[] values = TextFormater.split(fullText, TextFormater.DEFAULT_DELIM);

        mode = TabletCommand.Mode.values()[Float.valueOf(values[0]).intValue()];
        horizontalPosition = Float.valueOf(values[1]);
        verticalPosition = Float.valueOf(values[2]);
        pannelCurrent = Float.valueOf(values[3]);
        pannelVoltage = Float.valueOf(values[4]);
        batteryCurrent = Float.valueOf(values[5]);
        batteryVoltage = Float.valueOf(values[6]);
        this.dateTime = dateTime;
    }

    public TabletCommand.Mode getMode() {
        return mode;
    }

    public float getHorizontalPosition() {
        return horizontalPosition;
    }

    public float getVerticalPosition() {
        return verticalPosition;
    }

    public float getPannelCurrent() {
        return pannelCurrent;
    }

    public float getPannelVoltage() {
        return pannelVoltage;
    }

    public float getBatteryCurrent() {
        return batteryCurrent;
    }

    public float getBatteryVoltage() {
        return batteryVoltage;
    }

    public float getPowerPannel() {
        return pannelCurrent * pannelVoltage;
    }

    public float getPowerBatterie() {
        return batteryCurrent * batteryVoltage;
    }

    public double getHorizontalPositionAngle() {
        return NumerikConvert.nbMotorIncrementToAngle(horizontalPosition);
    }

    public double getVerticalPositionAngle() {
        return NumerikConvert.nbMotorIncrementToAngle(verticalPosition);
    }

    public String getFullTextFormat() {
        return mode.getValue() + TextFormater.DEFAULT_DELIM
                + horizontalPosition + TextFormater.DEFAULT_DELIM
                + verticalPosition + TextFormater.DEFAULT_DELIM
                + pannelCurrent + TextFormater.DEFAULT_DELIM
                + pannelVoltage + TextFormater.DEFAULT_DELIM
                + batteryCurrent + TextFormater.DEFAULT_DELIM
                + batteryVoltage + TextFormater.DEFAULT_DELIM;
    }

    @Override
    public String getCSVDataFormated() {
        return dateTime + CSV_SEPARATOR
                + mode.getValue() + CSV_SEPARATOR
                + getHorizontalPositionAngle() + CSV_SEPARATOR
                + getVerticalPositionAngle() + CSV_SEPARATOR
                + getPowerPannel() + CSV_SEPARATOR
                + getPowerBatterie() + CSV_SEPARATOR;
    }

    @Override
    public String getCSVTitleFormated() {
        return "timestamp" + CSV_SEPARATOR
                + "mode" + CSV_SEPARATOR
                + "angle horizontale [°]" + CSV_SEPARATOR
                + "angle verticale [°]" + CSV_SEPARATOR
                + "puissance photovoltaique [W]" + CSV_SEPARATOR
                + "puissance de charge [W]" + CSV_SEPARATOR;
    }
}

