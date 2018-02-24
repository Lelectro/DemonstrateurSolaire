package ch.heigvd.huguelet.demonstrateursolaire.fragment;


import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import ch.heigvd.huguelet.demonstrateursolaire.MainActivity;
import ch.heigvd.huguelet.demonstrateursolaire.R;
import ch.heigvd.huguelet.demonstrateursolaire.data.AdafruitData;
import ch.heigvd.huguelet.demonstrateursolaire.data.TabletCommand;
import ch.heigvd.huguelet.demonstrateursolaire.utils.NumerikConvert;
import ch.heigvd.huguelet.demonstrateursolaire.utils.SolarPosition;
import ch.heigvd.huguelet.demonstrateursolaire.utils.TextFormater;

public class ApplicationFragment extends Fragment {

    private static final int AZIMUT_INCREMENT = 200;
    private static final int ZENITH_INCREMENT = 100;

    private static final int SECOND_INCREMENT_SIMUALTION = 30 * NumerikConvert.NB_SEC_IN_MINUTE;

    // Configuration mode
    private Button mCapteurSelect, mGPSSelect, mManualSelect, mSimulation;

    // Information view
    private TextView mode;
    private TextView azimut, zenith;
    private TextView ppv, pch;
    private TextView longitudeValue, latitudeValue;
    private TextView dateTimeTxtView;

    //Views
    View manualLayout, gpsLayout, simulationLayout, capteurPVLayout;

    // Localisation
    private LocationManager locationManager;
    private Timer locTimer;
    private TimerTask locTask;
    private double longitude, latitude;
    private double longitudeGPS, latitudeGPS;
    private double longitudeNetwork, latitudeNetwork;

    // Simultation
    private int timezoneSimulation;
    private double longitudeSimulation, latitudeSimulation;
    private SimpleDateFormat dateSimulation;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private TextView dateSimulationView;
    private boolean isInSimulation;
    private int yearSimulation, monthSimulation, daySimulation;
    private ArrayList<TabletCommand> simulationCommands;
    private ArrayList<String> simulationCommandsText;
    private int iteratorSimulationsCommands = 0;

    // Manual
    private Button btnUp, btnDown, btnLeft, btnRight;
    private Button btnTH, btnTV, btnCapSud, btnRoulement, btnPVFast, btnInit;
    private int currentAzimut, currentZenith;

    // GPS
    private Timer gpsTimer;

    public ApplicationFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_application, container, false);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        mode = view.findViewById(R.id.mode);
        azimut = view.findViewById(R.id.azimut);
        zenith = view.findViewById(R.id.zenith);
        ppv = view.findViewById(R.id.ppv);
        pch = view.findViewById(R.id.pch);
        longitudeValue = view.findViewById(R.id.longitude);
        latitudeValue = view.findViewById(R.id.latitude);
        dateTimeTxtView = view.findViewById(R.id.timelastinfo);

        manualLayout = view.findViewById(R.id.manualView);
        capteurPVLayout = view.findViewById(R.id.capteurPVVIew);
        gpsLayout = view.findViewById(R.id.gpsView);
        simulationLayout = view.findViewById(R.id.simulationView);
        messageListView = view.findViewById(R.id.listMessageSimulation);
        listAdapter = new ArrayAdapter<String>(getContext(), R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        isInSimulation = false;
        simulationCommands = new ArrayList<>();
        simulationCommandsText = new ArrayList<>();


        btnUp = view.findViewById(R.id.manualUp);
        btnDown = view.findViewById(R.id.manualDown);
        btnLeft = view.findViewById(R.id.manualLeft);
        btnRight = view.findViewById(R.id.manualRight);

        btnTH = view.findViewById(R.id.btnth);
        btnTV = view.findViewById(R.id.btntv);
        btnCapSud = view.findViewById(R.id.btnsud);
        btnRoulement = view.findViewById(R.id.btnroulement);
        btnPVFast = view.findViewById(R.id.btnPVFast);
        btnInit = view.findViewById(R.id.btninit);

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "An error occurated", Toast.LENGTH_SHORT).show();
            return view;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 10, locationListenerGPS);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * 1000, 10, locationListenerNetwork);

        locTimer = new Timer();
        locTask = new TimerTask() {
            @Override
            public void run() { getActivity().runOnUiThread(() -> {
                    if (longitudeGPS == 0.0 && latitudeGPS == 0.0) {
                        if (longitudeNetwork == 0.0 && longitudeGPS == 0.0) {

                            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            // longitudeGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
                            // latitudeGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();

                            if (longitudeGPS == 0.0 && latitudeGPS == 0.0) {

                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                longitudeNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude();
                                latitudeNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude();

                                if (longitudeNetwork == 0.0 && longitudeGPS == 0.0) {

                                    longitude = latitude = 0;

                                } else {
                                    longitude = longitudeNetwork;
                                    latitude = latitudeNetwork;
                                }
                            } else {
                                longitude = longitudeGPS;
                                latitude = latitudeGPS;
                            }
                        } else {
                            longitude = longitudeNetwork;
                            latitude = latitudeNetwork;
                        }
                    } else {
                        longitude = longitudeGPS;
                        latitude = latitudeGPS;
                    }

                    if (! isInSimulation) {
                        longitudeValue.setText(longitude + "");
                        latitudeValue.setText(latitude + "");
                    }

                });
            }
        };
        locTimer.scheduleAtFixedRate(locTask, 0, 1000 * 60);

        mCapteurSelect = view.findViewById(R.id.photoresistance);
        mGPSSelect = view.findViewById(R.id.gps);
        mManualSelect = view.findViewById(R.id.manual);
        mSimulation = view.findViewById(R.id.simulation);

        mCapteurSelect.setOnClickListener(view1 -> {
            noModeSelected();
            mCapteurSelect.setBackgroundResource(R.drawable.side_nav_bar);
            capteurPVLayout.setVisibility(View.VISIBLE);
            doCaptorPV();

        });

        mGPSSelect.setOnClickListener(view1 -> {
            noModeSelected();
            mGPSSelect.setBackgroundResource(R.drawable.side_nav_bar);
            gpsLayout.setVisibility(View.VISIBLE);
            doGPS();
        });

        mManualSelect.setOnClickListener(view1 -> {
            noModeSelected();
            mManualSelect.setBackgroundResource(R.drawable.side_nav_bar);
            manualLayout.setVisibility(View.VISIBLE);
        });

        mSimulation.setOnClickListener(view1 -> {
            noModeSelected();
            mSimulation.setBackgroundResource(R.drawable.side_nav_bar);
            simulationLayout.setVisibility(View.VISIBLE);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            final View dialogView = inflater.inflate(R.layout.dialog_simulation, null);
            dialogBuilder.setView(dialogView);

            final EditText dateValue = dialogView.findViewById(R.id.date);
            final EditText longitude = dialogView.findViewById(R.id.longitudeedt);
            final EditText latitude = dialogView.findViewById(R.id.latitudeedt);
            final EditText timezone = dialogView.findViewById(R.id.timezoneedt);

            Calendar myCalendar = Calendar.getInstance();
            DatePickerDialog.OnDateSetListener date = (view2, year, monthOfYear, dayOfMonth) -> {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String myFormat = "dd.MM.yyyy"; //In which you need put here
                dateSimulation = new SimpleDateFormat(myFormat, Locale.FRANCE);
                dateValue.setText(dateSimulation.format(myCalendar.getTime()));


            };


            dateValue.setOnClickListener(v -> new DatePickerDialog(getContext(), date, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show());


            dialogBuilder.setTitle("Paramètre de la simulation");

            dialogBuilder.setPositiveButton("OK", (dialog, whichButton) -> {

                if (Objects.equals(dateValue.getText().toString(), "") ||
                        Objects.equals(longitude.getText().toString(), "") ||
                        Objects.equals(latitude.getText().toString(), "") ||
                        Objects.equals(timezone.getText().toString(), "")) {

                    Toast.makeText(getContext(), "Simulation interrompue, valeurs manquantes", Toast.LENGTH_LONG).show();
                    noModeSelected();

                } else {
                    //dateSimulation already set.
                    longitudeSimulation = Double.valueOf(longitude.getText().toString());
                    latitudeSimulation =  Double.valueOf(latitude.getText().toString());
                    timezoneSimulation = Integer.valueOf(timezone.getText().toString());

                    if ((-180 <= longitudeSimulation && longitudeSimulation <= 180) ||
                            (-90 <= latitudeSimulation && latitudeSimulation <= 90) ||
                            (-12 <= timezoneSimulation && timezoneSimulation <= 12)) {

                        yearSimulation = myCalendar.get(Calendar.YEAR);
                        monthSimulation = myCalendar.get(Calendar.MONTH)+1;
                        daySimulation = myCalendar.get(Calendar.DAY_OF_MONTH);

                        Toast.makeText(getContext(), "Début de la simulation", Toast.LENGTH_LONG).show();
                        doSimulation();

                    } else {
                        Toast.makeText(getContext(), "Simulation interrompue, valeurs saisies incorrectes", Toast.LENGTH_LONG).show();
                        noModeSelected();
                    }
                }
            });
            dialogBuilder.setNegativeButton("Annuler", (dialog, whichButton) -> {

                Toast.makeText(getContext(), "Simulation interrompue", Toast.LENGTH_LONG).show();
                noModeSelected();
            });

            AlertDialog b = dialogBuilder.create();
            b.show();

        });

        btnUp.setOnClickListener(view15 -> doManual(TabletCommand.Mode.MANUAL, currentAzimut, currentZenith + ZENITH_INCREMENT));

        btnLeft.setOnClickListener(view14 -> doManual(TabletCommand.Mode.MANUAL, currentAzimut + AZIMUT_INCREMENT, currentZenith));

        btnDown.setOnClickListener(view12 -> doManual(TabletCommand.Mode.MANUAL, currentAzimut, currentZenith - ZENITH_INCREMENT));

        btnRight.setOnClickListener(view13 -> doManual(TabletCommand.Mode.MANUAL, currentAzimut - AZIMUT_INCREMENT, currentZenith));

        btnTH.setOnClickListener(view16 -> doManual(TabletCommand.Mode.HORIZONTAL_TEST, TabletCommand.HORIZONTAL_POSITION_MIN, TabletCommand.VERTICAL_POSITION_MAX));

        btnTV.setOnClickListener(view19 -> doManual(TabletCommand.Mode.VERTICAL_TEST, TabletCommand.HORIZONTAL_POSITION_MAX/4, TabletCommand.VERTICAL_POSITION_MIN));

        btnCapSud.setOnClickListener(view17 -> doManual(TabletCommand.Mode.CAP_SUD, TabletCommand.HORIZONTAL_POSITION_MAX/2, TabletCommand.VERTICAL_POSITION_MAX/3));

        btnRoulement.setOnClickListener(view18 -> doManual(TabletCommand.Mode.ROULMENT, TabletCommand.HORIZONTAL_POSITION_MIN, TabletCommand.VERTICAL_POSITION_MIN));

        btnPVFast.setOnClickListener(view110 -> doManual(TabletCommand.Mode.DEMO_PV, TabletCommand.HORIZONTAL_POSITION_MIN, TabletCommand.VERTICAL_POSITION_MIN));

        btnInit.setOnClickListener(view111 -> doManual(TabletCommand.Mode.RESET, TabletCommand.HORIZONTAL_POSITION_MIN, TabletCommand.VERTICAL_POSITION_MIN));

        noModeSelected();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locTimer.cancel();
        if (gpsTimer != null)
            gpsTimer.cancel();
        locationManager.removeUpdates(locationListenerGPS);
        locationManager.removeUpdates(locationListenerNetwork);
    }

    public void updateInfo(String dateTime, AdafruitData data) {


        if (isInSimulation) {

            mode.setText("Simulation d'une journée");
            dateTimeTxtView.setText(TextFormater.convertIntoTextWithSpecificSize(2, daySimulation) + "." + TextFormater.convertIntoTextWithSpecificSize(2,monthSimulation) + "." + TextFormater.convertIntoTextWithSpecificSize(4,yearSimulation) + " GMT+" + timezoneSimulation);
            longitudeValue.setText(longitudeSimulation + "");
            latitudeValue.setText(latitudeSimulation + "");
            ppv.setText("-");
            pch.setText("-");
            azimut.setText(data.getHorizontalPositionAngle() + "");
            zenith.setText(data.getVerticalPositionAngle() + "");

            (new Handler()).postDelayed(() -> doSendSimulationCommands(), 4000);

        }
        else {
            currentAzimut = (int) data.getHorizontalPosition();
            currentZenith = (int) data.getVerticalPosition();

            dateTimeTxtView.setText(dateTime);
            mode.setText(data.getMode().getName());
            azimut.setText(data.getHorizontalPositionAngle() + "");
            zenith.setText(data.getVerticalPositionAngle() + "");
            ppv.setText(data.getPowerPannel() + "");
            pch.setText(data.getPowerBatterie() + "");
        }


    }

    private void doSimulation() {

        // locTimer.cancel();


        getActivity().runOnUiThread(() -> {

            isInSimulation = true;
            listAdapter.clear();
            simulationCommands.clear();
            simulationCommandsText.clear();
            iteratorSimulationsCommands = 0;

            for (int myTime = 0; myTime < (NumerikConvert.NB_SEC_IN_DAY); myTime += SECOND_INCREMENT_SIMUALTION) {

                SolarPosition position = new SolarPosition(
                        (float) latitudeSimulation,
                        (float) longitudeSimulation,
                        timezoneSimulation,
                        yearSimulation,
                        monthSimulation,
                        daySimulation,
                        NumerikConvert.getHoursFrom(myTime),
                        NumerikConvert.getMinutesFrom(myTime),
                        NumerikConvert.getSecondsFrom(myTime)
                );

                String addtolist = "[" + TextFormater.convertIntoTextWithSpecificSize(2,NumerikConvert.getHoursFrom(myTime)) +":"+ TextFormater.convertIntoTextWithSpecificSize(2,NumerikConvert.getMinutesFrom(myTime)) +":"+ TextFormater.convertIntoTextWithSpecificSize(2,NumerikConvert.getSecondsFrom(myTime)) + "]";
                addtolist += " azimut = " + TextFormater.convertFloatIntoTextWithSpecificSize(position.getAzimuth());
                addtolist += ", zenith = " + TextFormater.convertFloatIntoTextWithSpecificSize(position.getZenith());
                addtolist += ", status = ";

                TabletCommand command = new TabletCommand(TabletCommand.Mode.MANUAL);
                try {
                    command.setHorizontalPosition((int) NumerikConvert.angleToNbMotorIncrement(position.getAzimuth()));
                    command.setVerticalPosition((int) NumerikConvert.angleToNbMotorIncrement(position.getZenith()));

                    simulationCommands.add(command);

                    addtolist += "1";
                }
                catch (TabletCommand.TabletCommandInvalidValueException e) {
                    simulationCommands.add(null);
                    addtolist += "0";
                }

                simulationCommandsText.add(addtolist);
            }

            doSendSimulationCommands();

        });
    }

    private void doSendSimulationCommands() {

        if (! isInSimulation ) return;

        if (iteratorSimulationsCommands >= simulationCommandsText.size()) return;

        while (iteratorSimulationsCommands < simulationCommandsText.size() && simulationCommands.get(iteratorSimulationsCommands) == null) {
            listAdapter.add(simulationCommandsText.get(iteratorSimulationsCommands));
            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
            iteratorSimulationsCommands++;
        }

        if (iteratorSimulationsCommands >= simulationCommandsText.size()) return;

        if (simulationCommands.get(iteratorSimulationsCommands) != null) {
            sendCommand(simulationCommands.get(iteratorSimulationsCommands));
        }

        listAdapter.add(simulationCommandsText.get(iteratorSimulationsCommands));
        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
        iteratorSimulationsCommands++;
    }

    private void doGPS() {

        gpsTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(() -> {

                    SolarPosition position = new SolarPosition(
                            (float) latitude,
                            (float) longitude,
                            NumerikConvert.getCurrentTimeZone(),
                            NumerikConvert.getCurrentYear(),
                            NumerikConvert.getCurrentMonth(),
                            NumerikConvert.getCurrentDay(),
                            NumerikConvert.getCurrentHours(),
                            NumerikConvert.getCurrentMinutes(),
                            NumerikConvert.getCurrentSeconds()
                    );


                    double azimut = NumerikConvert.angleToNbMotorIncrement(position.getAzimuth());
                    double zenith = NumerikConvert.angleToNbMotorIncrement(position.getZenith());

                    Log.e("GPS", (float) latitude + " | " + (float) longitude + " | " + NumerikConvert.getCurrentTimeZone() + " | " + NumerikConvert.getCurrentYear() + " | " + NumerikConvert.getCurrentMonth() + " | " + NumerikConvert.getCurrentDay() + " | " + NumerikConvert.getCurrentHours() + " | " + NumerikConvert.getCurrentMinutes() + " | " + NumerikConvert.getCurrentSeconds());
                    Log.e("GPS", "azimuth = " + position.getAzimuth() + " - zenith = " + position.getZenith());

                    try {

                        TabletCommand command = new TabletCommand(TabletCommand.Mode.GPS);
                        command.setHorizontalPosition((int) azimut);
                        command.setVerticalPosition((int) zenith);
                        sendCommand(command);
                    } catch (TabletCommand.TabletCommandInvalidValueException e) {
                        Toast.makeText(getContext(), "La position est inatteignable", Toast.LENGTH_LONG).show();
                        noModeSelected();
                    }
                });
            }
        };
        locTimer.scheduleAtFixedRate(task, 0, 1000 * 60 * 30);


        // SolarPosition(float Lat,float Lon, float TimeZone, float A, float M, float J, float HH, float MM, float SS)
        //SolarPosition position = new SolarPosition(46.778, 6.6411, 1, 2017, 12, 21, 12, 0,0);
        //Log.e("GPS", "Azi = 172.2496  ===>  " + position.getAzimuth() + " - " + "Zen = 19.4655 ===>" + position.getZenith());

    }

    private void doManual(TabletCommand.Mode mode, int newAzimut, int newZenith) {

        try {

            TabletCommand command = new TabletCommand(mode);
            command.setHorizontalPosition(newAzimut);
            command.setVerticalPosition(newZenith);
            sendCommand(command);
        } catch (TabletCommand.TabletCommandInvalidValueException e) {
            Toast.makeText(getContext(), "La position est inatteignable", Toast.LENGTH_LONG).show();
        }

    }

    private void doCaptorPV() {
        TabletCommand command = new TabletCommand(TabletCommand.Mode.LUX);
        sendCommand(command);
    }

    private void noModeSelected() {
        isInSimulation = false;
        mCapteurSelect.setBackgroundResource(android.R.drawable.btn_default);
        mGPSSelect.setBackgroundResource(android.R.drawable.btn_default);
        mManualSelect.setBackgroundResource(android.R.drawable.btn_default);
        mSimulation.setBackgroundResource(android.R.drawable.btn_default);

        capteurPVLayout.setVisibility(View.INVISIBLE);
        gpsLayout.setVisibility(View.INVISIBLE);
        manualLayout.setVisibility(View.INVISIBLE);
        simulationLayout.setVisibility(View.INVISIBLE);

        if (gpsTimer != null) {
            gpsTimer.cancel();
        }
    }

    private void sendCommand(TabletCommand command) {
        ((MainActivity) getActivity()).sendDataOverBLE(command.getFullTextCommand());
    }

    private boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", (paramDialogInterface, paramInt) -> {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                })
                .setNegativeButton("Cancel", (paramDialogInterface, paramInt) -> {
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private final LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.d("GPS-WIFI", "Lon = " + location.getLongitude() + " - Lat = " + location.getLatitude());
            longitudeNetwork = location.getLongitude();
            latitudeNetwork = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    private final LocationListener locationListenerGPS = new LocationListener() {

        public void onLocationChanged(Location location) {
            Log.d("GPS-GPS", "Lon = " + location.getLongitude() + " - Lat = " + location.getLatitude());
            longitudeGPS = location.getLongitude();
            latitudeGPS = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

}

