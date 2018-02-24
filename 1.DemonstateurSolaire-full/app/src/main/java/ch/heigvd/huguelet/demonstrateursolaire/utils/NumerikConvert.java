package ch.heigvd.huguelet.demonstrateursolaire.utils;

import java.util.Calendar;

public class NumerikConvert {

    public static double nbMotorIncrementToAngle(double increment) {
        return increment * 0.024;
    }

    public static double angleToNbMotorIncrement(double angle) {
        return angle / 0.024;
    }

    public static long getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static long getCurrentMonth() {
        // January value is 0.
        return Calendar.getInstance().get(Calendar.MONTH)+1;
    }

    public static long getCurrentDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    public static long getCurrentTimeZone() {
        // Calendar.ZONE_OFFSET is un millisecond
        return Calendar.getInstance().get(Calendar.ZONE_OFFSET)/(1000*60*60);
    }

    public static long getCurrentHours() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    public static long getCurrentMinutes() {
        return Calendar.getInstance().get(Calendar.MINUTE);
    }

    public static long getCurrentSeconds() {
        return Calendar.getInstance().get(Calendar.SECOND);
    }

    public static final int NB_SEC_IN_MINUTE = 60;
    public static final int NB_SEC_IN_HOURS  = 60 * 60;
    public static final int NB_SEC_IN_DAY    = 60 * 60 * 24;
    public static final int NB_MIN_IN_HOURS  = 60;
    public static final int NB_HOURS_IN_DAY  = 24;

    public static int getHoursFrom(int timeInSecond) {
        return timeInSecond / NB_SEC_IN_HOURS;
    }

    public static int getMinutesFrom(int timeInSecond) {
        return (timeInSecond % NB_SEC_IN_HOURS) / NB_SEC_IN_MINUTE;
    }

    public static int getSecondsFrom(int timeInSecond) {
        return (timeInSecond % NB_SEC_IN_HOURS) % NB_SEC_IN_MINUTE;
    }



}
