package ch.heigvd.huguelet.demonstrateursolaire.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TextFormater {

    public static final String DEFAULT_DELIM = ";";

    public static String[] split (String fullText, String delim) {
        return fullText.split(delim);
    }

    public static String convertIntoTextWithSpecificSize(int size, int value){
        return String.format("%0" +size+ "d", value);
    }


    public static String convertFloatIntoTextWithSpecificSize (double value) {
        return String.format("%4.6f", value);
    }

    public static String getDateTimeCSV() {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }

    public static String getDateTimeFileName() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }

    public static String getTimeFormated() {
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }

    public static String getDateTime() {
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }
}
