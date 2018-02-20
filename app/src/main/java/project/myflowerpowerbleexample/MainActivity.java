package project.myflowerpowerbleexample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TargetApi(21)
public class MainActivity extends Activity {
    private final String TAG = "MainActivity";
    private final long SCAN_PERIOD = 30000; // 30 seconds
    private int REQUEST_ENABLE_BT = 1;

    private final String TARGET_MACADDRESS = "A0:14:3D:09:1B:41";
    private final UUID mTargetService = UUID.fromString("39e1fa00-84a8-11e2-afba-0002a5d5c51b");
    private final UUID mTargetCharacteristic = UUID.fromString("39e1fa04-84a8-11e2-afba-0002a5d5c51b");

    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private BluetoothLeScanCallback mScanCallback;
    private BluetoothGatt mGatt;

    private TextView viewOutput;
    private boolean mScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewOutput = (TextView)findViewById(R.id.output);

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE Not Supported...quitting app!", Toast.LENGTH_SHORT).show();
            finish();
        }
        BluetoothManager btm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = btm.getAdapter();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        viewOutput.append("Bluetooth LE supported\n");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        disconnectGatt();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onButtonClickConnect(View v) {
        startScan();
    }

    private void disconnectGatt() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
        }
    }

    private boolean bluetoothAvailable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private void startScan() {
        if (bluetoothAvailable()) {
            disconnectGatt();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(TARGET_MACADDRESS).build();
            filters.add(filter);

            mScanCallback = new BluetoothLeScanCallback();

            mLEScanner.startScan(filters, settings, mScanCallback);
            mScanning = true;
            Log.i(TAG, "Bluetooth LE scanner started...");
            viewOutput.append("Bluetooth LE scanner started...\n");

            // Stopping the scan after SCAN_PERIOD seconds
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, SCAN_PERIOD);
        }
    }

    public void stopScan() {
        if (bluetoothAvailable() && mScanning) {
            mLEScanner.stopScan(mScanCallback);
            viewOutput.append("Bluetooth LE scanner stopped!\n");
        }

        mScanCallback = null;
        mScanning = false;
    }

    private class BluetoothLeScanCallback extends ScanCallback {
        private final String TAG = "BtleScanCallback";

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // Stopping scan to prevent this method being called multiple times
            stopScan();

            Log.i(TAG, "Scan result: " + result.toString());
            final BluetoothDevice bluetoothDevice = result.getDevice();
            final String deviceAddress = bluetoothDevice.getAddress();
            Log.i(TAG, "Device found: " + deviceAddress);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewOutput.append("Device found: " + deviceAddress + "\n");
                }
            });

            mGatt = bluetoothDevice.connectGatt(MainActivity.this, false, mGattCallback);
        }

        // When using SCAN_MODE_LOW_POWER onBatchScanResults is used instead
        // @Override
        // public void onBatchScanResults(List<ScanResult> results) {
        //     for (ScanResult result : results) {
        //         doSomething();
        //     }
        // }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Bluetooth LE scan failed");
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        private final String TAG = "BluetoothGattCallback";

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange - Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(TAG, "STATE_DISCONNECTED");
                    break;
                default:
                    gatt.discoverServices();
                    Log.e(TAG, "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered failed with status " + Integer.toString(status));
                return;
            }
            List<BluetoothGattService> services = gatt.getServices();
            Log.i(TAG, "onServicesDiscovered: " + services.toString());

            BluetoothGattService service = gatt.getService(mTargetService);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(mTargetCharacteristic);
            gatt.readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i(TAG, "onCharacteristicRead: " + characteristic.toString());

            // The Flower Power returns a 2 byte array that needs to be converted into
            // an 16bit integer using Little Endian format.
            // Since Java uses signed bytes we have to convert them to unsigned with b & 0xFF

            int val = ((characteristic.getValue()[1] & 0xFF) << 8) +
                    (characteristic.getValue()[0] & 0xFF);

            // Some magic calculation I extracted from here
            // https://github.com/Parrot-Developers/node-flower-power/blob/master/index.js

            final double temp = 0.00000003044 * Math.pow(val, 3.0) - 0.00008038 *
                    Math.pow(val, 2.0) + val * 0.1149 - 30.449999999999999;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewOutput.append("Temperature reading: " + Double.toString(temp) + "\n");
                }
            });

            gatt.disconnect();
        }
    };
}