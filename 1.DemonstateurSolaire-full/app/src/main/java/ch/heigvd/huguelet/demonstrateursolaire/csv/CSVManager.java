package ch.heigvd.huguelet.demonstrateursolaire.csv;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import ch.heigvd.huguelet.demonstrateursolaire.utils.TextFormater;

public class CSVManager {

    static public final String TAG = "CSV";

    private static CSVManager ourInstance = null;

    public static CSVManager getInstance() {
        return ourInstance;
    }

    public static CSVManager newInstance(String filename) {
        ourInstance = new CSVManager(filename);
        return ourInstance;
    }

    private File file;
    private boolean isFileEmpty;

    private CSVManager(String filename) {

        // Get the directory for the user's public pictures directory.
        String path = Environment.getExternalStorageDirectory() + File.separator + "DemonstrateurSolaire";

        File folder = new File(path);
        if (! folder.exists())
            folder.mkdirs();

        // Create the file.
        file = new File(folder, filename + "_" +TextFormater.getDateTimeFileName() +".csv");

        isFileEmpty = true;

    }

    public void write (ICSVManager content) throws IOException {

        if (isFileEmpty) {
            isFileEmpty = false;
            write(content.getCSVTitleFormated());
        }

        write(content.getCSVDataFormated());
    }

    private void write(String content) throws IOException {

        FileOutputStream fOut = new FileOutputStream(file, true);
        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
        myOutWriter.append(content);
        myOutWriter.append("\n");
        myOutWriter.flush();
        myOutWriter.close();
        fOut.close();
    }

    public String getFilename() {
        return file.getName();
    }

    public interface ICSVManager {
        String CSV_SEPARATOR = ";";
        String getCSVDataFormated();
        String getCSVTitleFormated();
    }
}
