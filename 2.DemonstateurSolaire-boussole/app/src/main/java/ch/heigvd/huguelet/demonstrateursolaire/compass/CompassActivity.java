package ch.heigvd.huguelet.demonstrateursolaire.compass;

import android.app.Activity;
import android.app.AlertDialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import ch.heigvd.huguelet.demonstrateursolaire.R;

public class CompassActivity extends Activity implements SensorEventListener {

    // define the display assembly compass picture
    private ImageView image;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    private TextView angleX, angleZ;
    private double x = 0, z = 0;


    private Button btnCancel, btnOK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        image = findViewById(R.id.imageViewCompass);
        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        angleX = findViewById(R.id.angleAxeX);
        angleZ = findViewById(R.id.angleAxeZ);

        btnCancel = findViewById(R.id.btnCompassCancel);
        btnOK     = findViewById(R.id.btnCompassOk);


        btnCancel.setOnClickListener(v -> {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.dialog_config, null);
            dialogBuilder.setView(dialogView);

            final TextView azimut = dialogView.findViewById(R.id.azimut);
            final TextView zenith = dialogView.findViewById(R.id.zenith);


            azimut.setText("0.0");
            zenith.setText("0.0");

            dialogBuilder.setTitle("Calibrage annulé");
            dialogBuilder.setMessage("Les valeurs enregistrés sont : ");

            dialogBuilder.setPositiveButton("OK", (dialog, whichButton) -> {

            });
            AlertDialog b = dialogBuilder.create();
            b.show();

        });
        btnOK.setOnClickListener(v -> {


            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.dialog_config, null);
            dialogBuilder.setView(dialogView);

            final TextView azimut = dialogView.findViewById(R.id.azimut);
            final TextView zenith = dialogView.findViewById(R.id.zenith);


            azimut.setText(x + "");
            zenith.setText(z + "");

            dialogBuilder.setTitle("Calibrage validé");
            dialogBuilder.setMessage("Les valeurs enregistrés sont : ");

            dialogBuilder.setPositiveButton("OK", (dialog, whichButton) -> {

            });
            AlertDialog b = dialogBuilder.create();
            b.show();



        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);
        degree += 90;
        degree %= 360;

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

        x = event.values[2];
        z = event.values[1];

        angleX.setText(Float.toString(event.values[2]));
        angleZ.setText(Float.toString(event.values[1]));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }
}