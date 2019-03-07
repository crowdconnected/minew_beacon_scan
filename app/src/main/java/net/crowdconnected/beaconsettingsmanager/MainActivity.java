package net.crowdconnected.beaconsettingsmanager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.minew.beaconplus.sdk.MTCentralManager;
import com.minew.beaconplus.sdk.MTConnectionHandler;
import com.minew.beaconplus.sdk.MTPeripheral;
import com.minew.beaconplus.sdk.enums.BluetoothState;
import com.minew.beaconplus.sdk.enums.ConnectionStatus;
import com.minew.beaconplus.sdk.enums.FrameType;
import com.minew.beaconplus.sdk.exception.MTException;
import com.minew.beaconplus.sdk.frames.DeviceInfoFrame;
import com.minew.beaconplus.sdk.frames.IBeaconFrame;
import com.minew.beaconplus.sdk.frames.MinewFrame;
import com.minew.beaconplus.sdk.interfaces.ConnectionStatueListener;
import com.minew.beaconplus.sdk.interfaces.GetPasswordListener;
import com.minew.beaconplus.sdk.interfaces.MTCentralManagerListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements Handler.Callback {

    private MTCentralManager mtCentralManager;
    private ConnectionStatueListener connectionStatusListener;
    private Handler handler;
    private Set<MTPeripheral> mtPeripherals = new HashSet<>();
    private int peripheralCounter = 0;
    private List<MTPeripheral> peripheralsList = new ArrayList<>();
    private List<String> beaconInfo = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        handler = new Handler(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        mtCentralManager = MTCentralManager.getInstance(this);
        mtCentralManager.startService();
        Button scanButton = findViewById(R.id.scanButton);
        connectionStatusListener = new ConnectionStatusListener();

        scanButton.setOnClickListener(new ScanClickListener());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void beaconScan() {

        mtCentralManager.setMTCentralManagerListener(new CentralManagerListener());

        mtCentralManager.startScan();

        long delay = 20000L;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                System.out.println("Handler rec");
                mtCentralManager.stopScan();
                peripheralsList = new ArrayList(mtPeripherals);
                mtCentralManager.connect(peripheralsList.get(peripheralCounter), connectionStatusListener);

            }

        }, delay);


    }

    @Override
    public boolean handleMessage(Message message) {
        System.out.println("Handler message");
        return true;
    }

    private void managePeripherals() {

        MTConnectionHandler mtConnectionHandler = peripheralsList.get(peripheralCounter).mMTConnectionHandler;

        ArrayList<MinewFrame> allFrames = mtConnectionHandler.allFrames;
        IBeaconFrame iBeaconFrame = null;
        DeviceInfoFrame deviceInfoFrame = null;

        for (MinewFrame minewFrame : allFrames) {
            FrameType frameType = minewFrame.getFrameType();
            switch (frameType) {
                case FrameiBeacon:
                    iBeaconFrame = (IBeaconFrame) minewFrame;
                    break;
                case FrameDeviceInfo:
                    deviceInfoFrame = (DeviceInfoFrame) minewFrame;
                    break;
            }

        }
        if (iBeaconFrame != null && deviceInfoFrame != null) {
            String info = iBeaconFrame.getUuid() + ":" + iBeaconFrame.getMajor() + ":" + iBeaconFrame.getMinor() + ","
                    + deviceInfoFrame.getRadiotxPower() + "," + deviceInfoFrame.getAdvtxPower()
                    + "," + deviceInfoFrame.getAdvInterval() + "\n";

            beaconInfo.add(info);
        }
    }

    private void saveToFile() {
        System.out.println(peripheralsList.size());
        try {
            File testFile = new File(this.getExternalFilesDir(null), "Beacons-" +
                    System.currentTimeMillis() + ".csv");
            boolean firstCreate = false;
            if (!testFile.exists()) {
                testFile.createNewFile();
                firstCreate = true;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true));
            if (firstCreate) {
                writer.write("UUID:Major:Minor,RadiotxPower,AdvtxPower,AdvInternal\n");
            }
            for (String string : beaconInfo) {
                writer.write(string);
            }
            writer.close();

            MediaScannerConnection.scanFile(this,
                    new String[]{testFile.toString()},
                    null,
                    null);
        } catch (IOException e) {
            Log.e("ReadWriteFile", "Unable to write to the file.");
        }
    }


    private class ConnectionStatusListener implements ConnectionStatueListener {


        @Override
        public void onUpdateConnectionStatus(final ConnectionStatus connectionStatus, final GetPasswordListener getPasswordListener) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (connectionStatus) {
                        case CONNECTING:
                            Log.i("Connecting", "Peripheral Connecting");
                            break;
                        case CONNECTED:
                            Log.i("Connected", "Peripheral Connected");
                            break;
                        case READINGINFO:
                            Log.i("Reading Info", "Peripheral Reading info");
                            break;
                        case DEVICEVALIDATING:
                            Log.i("Device Validating", "Peripheral Device being validated");
                            break;
                        case PASSWORDVALIDATING:
                            String password = "otoqtogp";
                            getPasswordListener.getPassword(password);
                            Log.i("Password Validating ", "Password being validated");
                            break;
                        case SYNCHRONIZINGTIME:
                            Log.i("Synchronizing", "Synchronizing time");
                            break;
                        case READINGCONNECTABLE:
                            Log.i("Reading", "Reading Connectable");
                            break;
                        case READINGFEATURE:
                            Log.i("Reading", "Reading Feature");
                            break;
                        case READINGFRAMES:
                            Log.i("Reading", "Reading Frames");
                            break;
                        case READINGTRIGGERS:
                            Log.i("Reading", "Reading Triggers");
                            break;
                        case READINGSENSORS:
                            Log.i("Reading", "Reading Sensors");
                            break;
                        case COMPLETED:
                            Log.i("Completed", "Connecting to peripheral completed");
                            managePeripherals();
                            mtCentralManager.disconnect(peripheralsList.get(peripheralCounter));
                            peripheralCounter += 1;
                            if (peripheralCounter < peripheralsList.size()) {
                                try {
                                    Thread.sleep(5000L);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.i("Mac & Name", peripheralsList.get(peripheralCounter).mMTFrameHandler.getMac()
                                        + ":" + peripheralsList.get(peripheralCounter).mMTFrameHandler.getName());
                                mtCentralManager.connect(peripheralsList.get(peripheralCounter), connectionStatusListener);
                            } else {
                                saveToFile();
                            }
                            break;
                        case CONNECTFAILED:
                            Log.i("Failed", "Connect to peripheral failed");
                            peripheralCounter += 1;
                            if (peripheralCounter < peripheralsList.size()) {
                                Log.i("Mac & Name", peripheralsList.get(peripheralCounter).mMTFrameHandler.getMac()
                                        + ":" + peripheralsList.get(peripheralCounter).mMTFrameHandler.getName());
                                mtCentralManager.connect(peripheralsList.get(peripheralCounter), connectionStatusListener);
                            } else {
                                saveToFile();
                            }
                            break;
                        case DISCONNECTED:
                            Log.i("Disconnected", "Disconnected from the peripheral");
                            break;
                    }
                }
            });
        }


        @Override
        public void onError(MTException e) {

        }
    }

    private class ScanClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (mtCentralManager.getBluetoothState(getApplicationContext()).equals(BluetoothState.BluetoothStatePowerOn)) {
                beaconScan();
            }
        }
    }

    private class CentralManagerListener implements MTCentralManagerListener {

        @Override
        public void onScanedPeripheral(List<MTPeripheral> peripherals) {

            mtPeripherals.addAll(peripherals);

        }
    }

}


