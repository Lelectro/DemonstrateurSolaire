package ch.heigvd.huguelet.demonstrateursolaire;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import ch.heigvd.huguelet.demonstrateursolaire.ble.IBLEManagement;
import ch.heigvd.huguelet.demonstrateursolaire.ble.LoginActivity;
import ch.heigvd.huguelet.demonstrateursolaire.ble.UartService;
import ch.heigvd.huguelet.demonstrateursolaire.compass.CompassActivity;
import ch.heigvd.huguelet.demonstrateursolaire.csv.CSVManager;
import ch.heigvd.huguelet.demonstrateursolaire.data.AdafruitData;
import ch.heigvd.huguelet.demonstrateursolaire.fragment.ApplicationFragment;
import ch.heigvd.huguelet.demonstrateursolaire.utils.TextFormater;

public class MainActivity extends AppCompatActivity
        implements  NavigationView.OnNavigationItemSelectedListener, IBLEManagement {

    private static final String TAG = "MainActivity";

    // Constant value for BLE Connection
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    // BLE Management
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private String mRXMessage = "";

    // Menu
    DrawerLayout mDrawer;

    // Fragements
    // private ConsoleFragment mConsole;
    private ApplicationFragment mConfiguration;

    // UI Élement
    private TextView deviceName;
    private TextView statusName;

    /*************************************************************************************************************/
    /*  BLE Callback, code to implement                                                                          */
    /*************************************************************************************************************/

    @Override
    public void doOnBLEConnected() {
        runOnUiThread(() -> {
            Log.d(TAG, "UART_CONNECT_MSG");
            deviceName.setText(mDevice.getName());
            statusName.setText("Initialisation en cours...");
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
            findViewById(R.id.contentPanel).setAlpha(0.2f);
            showCompass();
            // mConsole.write(TextFormater.getTimeFormated(), "", "Connecté à "+ mDevice.getName());
            mState = UART_PROFILE_CONNECTED;
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        });
    }

    @Override
    public void doOnBLEDataReceived(final byte[] txValue) {
        runOnUiThread(() -> {
            try {

                mRXMessage += new String(txValue, "UTF-8");

                if (txValue[txValue.length-2] != 13) {
                    Log.e("DATA RECEIVED", "rx partiel = " +mRXMessage);
                }
                else
                {

                    Log.e("DATA RECEIVED", "rx = " + mRXMessage);
                    AdafruitData dataReceive = new AdafruitData(TextFormater.getDateTimeCSV(), mRXMessage);
                    Log.e("DATA RECEIVED", "Adafruit = " + dataReceive.getFullTextFormat());

                    statusName.setText("Prêt");
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    findViewById(R.id.contentPanel).setAlpha(1);

                    // mConsole.write(TextFormater.getTimeFormated(), "RX", dataReceive.getFullTextFormat());
                    mConfiguration.updateInfo(TextFormater.getDateTime(), dataReceive);
                    CSVManager.getInstance().write(dataReceive);
                    mRXMessage = "";

                }
            }
            catch (Exception e) {
                mRXMessage = "";
                e.printStackTrace();
            }
        });
    }

    @Override
    public void doOnBLEDisconnected() {
        runOnUiThread(() -> {
            Log.d(TAG, "UART_DISCONNECT_MSG");
            deviceName.setText("-");
            statusName.setText("-");
            // mConsole.write(TextFormater.getTimeFormated(), "", "Déconnexion de "+ mDevice.getName());
            mState = UART_PROFILE_DISCONNECTED;
            mService.close();
            mDevice = null;
            showDeviceList();
        });
    }

    @Override
    public void sendDataOverBLE(String message) {
        writeRXCharacteristic(message);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        Toast.makeText(this, "Commande vers l'arduino envoyée", Toast.LENGTH_LONG).show();
        Log.e("DATA SEND", "tx = " + message);

        findViewById(R.id.contentPanel).setAlpha(0.2f);
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        statusName.setText("En attente de réception de donnée...");
    }

    /*************************************************************************************************************/
    /*  Android life cicle                                                                                       */
    /*************************************************************************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Chargement des éléments de l'UI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        deviceName = findViewById(R.id.deviceName);
        statusName = findViewById(R.id.statusName);

        // Lancement du Bluetooth
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth n'est pas disponible", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        service_init();


        // Charge la fenêtre pour sélectionner le device à se connecter.
        showDeviceList();


        // Chargement du fragment
        /*mConsole = new ConsoleFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.content_fragment, mConsole)
                .commit();*/

        // Chargement du fragment
        mConfiguration = new ApplicationFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.content_fragment, mConfiguration)
                .commit();

        // Log des valeurs par défaut dans le CSV.
        CSVManager.newInstance("default");

    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /*************************************************************************************************************/
    /*  Manage choice BLE Device to connect                                                                      */
    /*************************************************************************************************************/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    deviceName.setText(mDevice.getName()+ " - connecté");
                    mService.connect(deviceAddress);
                }
                else {
                    showDeviceList();
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth a été allumé", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problème à l'activation du Bleutooth", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            default:
                Log.e(TAG, "Mauvais code");
                break;
        }
    }

    private void showDeviceList() {
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else if (mDevice == null) {

            Intent newIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        }
    }

    private void showCompass() {

        Intent intent = new Intent(this, CompassActivity.class);
        startActivity(intent);
    }


    /*************************************************************************************************************/
    /*  Left Menu management                                                                                     */
    /*************************************************************************************************************/

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_view_data:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_fragment, mConfiguration)
                        .commit();
                break;

            case R.id.nav_about:
                doShowAboutMe();
                break;

            case R.id.nav_export:
                doCSVNewFile();
                break;

            case R.id.nav_disconnect:

                mService.disconnect();
                mState = UART_PROFILE_DISCONNECTED;
                mService.close();
                mDevice = null;
                showDeviceList();
                break;

            case R.id.nav_calibration:
                //Toast.makeText(this,"Cette fonctionnalité n'est pas encore implémentée !", Toast.LENGTH_SHORT).show();
                showCompass();
                break;

            /*case R.id.nav_view_console:
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_fragment, mConsole)
                        .commit();
                break;

            case R.id.nav_change_mod:
                Toast.makeText(this,"Cette fonctionnalité n'est pas encore implémentée !", Toast.LENGTH_SHORT).show();
                break;


            /*case R.id.nav_wiki:
                Toast.makeText(this,"Cette fonctionnalité n'est pas encore implémentée !", Toast.LENGTH_SHORT).show();
                break;*/
        }

        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /*************************************************************************************************************/
    /*  CSV Manager                                                                                              */
    /*************************************************************************************************************/
    public void doCSVNewFile() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_new_csv_file, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = dialogView.findViewById(R.id.edit1);

        dialogBuilder.setTitle("Nouveau document CSV");
        dialogBuilder.setMessage("Saisir le nouveau pour le fichier CSV");

        dialogBuilder.setPositiveButton("OK", (dialog, whichButton) -> {

            CSVManager.newInstance(edt.getText().toString());
            Toast.makeText(MainActivity.this, "Fichier " + CSVManager.getInstance().getFilename() + " créé avec succès", Toast.LENGTH_LONG).show();

        });
        dialogBuilder.setNegativeButton("Annuler", (dialog, whichButton) -> Toast.makeText(MainActivity.this, "Création de fichier interrompu", Toast.LENGTH_LONG).show());
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    /*************************************************************************************************************/
    /*  About me Manager                                                                                         */
    /*************************************************************************************************************/
    public void doShowAboutMe() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_about_me, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("A propos de Démonstrateur Solaire");
        dialogBuilder.setPositiveButton("Close", (dialog, whichButton) -> {/*do nothing*/});
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    /*************************************************************************************************************/
    /* BLE/UART code don't touch                                                                                 */
    /* Copyright (c) 2015, Nordic Semiconductor                                                                  */
    /* https://github.com/NordicPlayground/Android-nRF-UART                                                      */
    /*************************************************************************************************************/

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
            showDeviceList();
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                doOnBLEConnected();
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                doOnBLEDisconnected();
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                doOnBLEDataReceived(intent.getByteArrayExtra(UartService.EXTRA_DATA));
            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                mService.disconnect();
            }
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    private void writeRXCharacteristic (String message ) {

        //send data to service
        try {
            mService.writeRXCharacteristic(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}