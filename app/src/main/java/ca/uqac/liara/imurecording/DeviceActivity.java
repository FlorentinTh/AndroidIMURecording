package ca.uqac.liara.imurecording;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.github.jorgecastilloprz.FABProgressCircle;

import java.util.ArrayList;
import java.util.List;

import ca.uqac.liara.imurecording.Adapters.AvailableDeviceAdapter;
import ca.uqac.liara.imurecording.Adapters.PairedDeviceAdapter;
import ca.uqac.liara.imurecording.Utils.AndroidUtils;

public class DeviceActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final int REQUEST_ENABLE_BLUETOOTH = 201;
    private static final long SCAN_PERIOD_TIMEOUT = 10000;

    private RelativeLayout layout;
    private ListView availableDevicesList;
    private ListView pairedDevicesList;
    private FABProgressCircle scanButton;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;


    private AvailableDeviceAdapter availableDeviceAdapter;
    private PairedDeviceAdapter pairedDeviceAdapter;

    private List<BluetoothDevice> pairedDevice;
    private List<BluetoothDevice> availableDevices;

    private SharedPreferences sharedPreferences;

    private Handler handler;

    private boolean isSacnning;

    @Override
    protected void onResume() {
        super.onResume();

        initBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkRuntimePermission();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        handler = new Handler();
        sharedPreferences = this.getSharedPreferences(getResources().getString(R.string.shared_preference_file), Context.MODE_PRIVATE);

        initGUI();
        initListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        scan(false);
    }

    private void initGUI() {
        layout = (RelativeLayout) findViewById(R.id.activity_main);
        pairedDevicesList = (ListView) findViewById(R.id.paired_devices);
        availableDevicesList = (ListView) findViewById(R.id.available_devices);
        scanButton = (FABProgressCircle) findViewById(R.id.btn_scan);

        pairedDevice = new ArrayList<>();
        availableDevices = new ArrayList<>();

        pairedDeviceAdapter = new PairedDeviceAdapter(this, pairedDevice);
        pairedDevicesList.setAdapter(pairedDeviceAdapter);
        availableDeviceAdapter = new AvailableDeviceAdapter(this, availableDevices);
        availableDevicesList.setAdapter(availableDeviceAdapter);
    }

    private void initListeners() {
        scanButton.setOnClickListener(
                v -> {
                    if (!isSacnning) {
                        scan(true);
                    } else {
                        scan(false);
                    }
                }
        );

        availableDevicesList.setOnItemClickListener(
                (parent, view, position, id) -> {
                    BluetoothDevice device = availableDeviceAdapter.getDevice(position);
                    pair(true, device);
                }
        );

        pairedDeviceAdapter.setOnUseButtonClickListener(
                position -> {
                    try {
                        AndroidUtils.writeSharedPreferences(sharedPreferences, getResources().getString(R.string.device_name_key_value), pairedDeviceAdapter.getDevice(position).getName());
                        AndroidUtils.writeSharedPreferences(sharedPreferences, getResources().getString(R.string.device_mac_address_key_value), pairedDeviceAdapter.getDevice(position).getAddress());
                    } catch (Exception e) {
                        Log.e(DeviceActivity.class.toString(), e.getMessage());
                    }

                    startActivity(new Intent(getApplicationContext(), CommunicationActivity.class));
                }
        );

        pairedDeviceAdapter.setOnUnpairButtonClickListener(
                position -> {
                    BluetoothDevice device = pairedDeviceAdapter.getDevice(position);
                    pair(false, device);
                }
        );
    }

    private void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkRuntimePermission() {
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (requestCode == Activity.RESULT_CANCELED) {
                    showAlertDialog(getResources().getString(R.string.alert_bluetooth_access_denied_title),
                            getResources().getString(R.string.alert_bluetooth_access_denied_message));
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showAlertDialog(getResources().getString(R.string.alert_location_permission_denied_title),
                            getResources().getString(R.string.alert_location_permission_denied_message));
                }
                break;
        }
    }

    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(dialog -> {
        });
        builder.show();
    }

    private void scan(final boolean enable) {
        if (enable) {
            availableDeviceAdapter.clear();
            scanButton.show();

            handler.postDelayed(
                    () -> {
                        scanButton.hide();
                        isSacnning = false;
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    },
                    SCAN_PERIOD_TIMEOUT
            );

            isSacnning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            scanButton.hide();
            isSacnning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {
        runOnUiThread(
                () -> {
                    availableDeviceAdapter.addDevice(device);
                    availableDeviceAdapter.notifyDataSetChanged();
                }
        );
    };

    private void pair(final boolean enable, BluetoothDevice device) {
        if (enable) {

            if (pairedDeviceAdapter.getCount() > 0) {
                Snackbar.make(layout, getResources().getString(R.string.device_already_paired), Snackbar.LENGTH_LONG).show();
                return;
            }

            pairedDeviceAdapter.addDevice(device);
            availableDeviceAdapter.removeDevice(device);
            availableDeviceAdapter.setPairedDevice(device);

            pairedDeviceAdapter.notifyDataSetChanged();
            availableDeviceAdapter.notifyDataSetChanged();

        } else {
            pairedDeviceAdapter.clear();
            pairedDeviceAdapter.notifyDataSetChanged();
            availableDeviceAdapter.setPairedDevice(null);
        }
    }
}
