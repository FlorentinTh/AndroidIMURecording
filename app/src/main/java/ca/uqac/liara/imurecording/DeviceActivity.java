package ca.uqac.liara.imurecording;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.ArrayList;
import java.util.List;

import ca.uqac.liara.imurecording.Adapters.AvailableDeviceAdapter;
import ca.uqac.liara.imurecording.Adapters.PairedDeviceAdapter;
import ca.uqac.liara.imurecording.Utils.StringUtils;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class DeviceActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final int REQUEST_ENABLE_BLUETOOTH = 201;

    private RelativeLayout layout;
    private ListView availableDevicesList;
    private ListView pairedDevicesList;
    private FABProgressCircle scanButton;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private RxBleClient rxBleClient;

    private Subscription scanAvailableDevices;
    private Subscription connectAvailableDevice;

    private AvailableDeviceAdapter availableDeviceAdapter;
    private PairedDeviceAdapter pairedDeviceAdapter;

    private List<RxBleDevice> pairedDevices;
    private List<RxBleDevice> availableDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        rxBleClient = RxBleClient.create(this);

        initGUI();
        initListeners();
        initBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkRuntimePermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBLESCAN();
    }

    private void initGUI() {
        layout = (RelativeLayout) findViewById(R.id.activity_main);
        pairedDevicesList = (ListView) findViewById(R.id.paired_devices);
        availableDevicesList = (ListView) findViewById(R.id.available_devices);
        scanButton = (FABProgressCircle) findViewById(R.id.btn_scan);

        pairedDevices = new ArrayList<>();
        availableDevices = new ArrayList<>();


        pairedDeviceAdapter = new PairedDeviceAdapter(this, pairedDevices);
        pairedDevicesList.setAdapter(pairedDeviceAdapter);
        availableDeviceAdapter = new AvailableDeviceAdapter(this, availableDevices);
        availableDevicesList.setAdapter(availableDeviceAdapter);
    }

    private void initListeners() {
        scanButton.setOnClickListener(
                v -> {
                    if (scanAvailableDevices == null) {
                        startBLEScan();
                    } else {
                        stopBLESCAN();
                    }
                }
        );

        availableDevicesList.setOnItemClickListener(
                (parent, view, position, id) -> {
                    subscribeBLEDevice(view, availableDeviceAdapter.getItem(position));
                }
        );

        pairedDeviceAdapter.setOnUseButtonClickListener(
                position -> {
                    /**
                     * TODO
                     * start new Activity
                     */
                }
        );

        pairedDeviceAdapter.setOnUnpairButtonClickListener(
                position -> {
                    unsubscribeBLEDevice();
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
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alert_location_permission_title);
            builder.setMessage(R.string.alert_location_permission_message);
            builder.setCancelable(false);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(
                    dialog -> {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                    }
            );
            builder.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (requestCode < 0) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.alert_bluetooth_access_denied_title);
                    builder.setMessage(R.string.alert_bluetooth_access_denied_message);
                    builder.setCancelable(true);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(dialog -> {});
                    builder.show();
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
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.alert_location_permission_denied_title);
                    builder.setMessage(R.string.alert_location_permission_denied_message);
                    builder.setCancelable(true);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(dialog -> {});
                    builder.show();
                }
                break;
        }
    }

    private void startBLEScan() {
        scanButton.show();
        scanAvailableDevices = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        rxBleScanResult -> {
                            RxBleDevice device = rxBleScanResult.getBleDevice();
                            String connectionStateValue = StringUtils.getConnectionStateDescription(device.getConnectionState().toString());
                            if (!availableDevices.contains(rxBleScanResult.getBleDevice()) &&
                                    (connectionStateValue != "CONNECTED" || connectionStateValue != "CONNECTING")) {
                                availableDevices.add(device);
                                availableDeviceAdapter.notifyDataSetChanged();
                            }
                        },
                        throwable -> {
                            Snackbar.make(layout, throwable.toString(), Snackbar.LENGTH_LONG);
                        }
                );
    }

    private void stopBLESCAN() {
        if (scanAvailableDevices != null) {
            scanButton.hide();
            scanAvailableDevices.unsubscribe();
            scanAvailableDevices = null;
        }
    }

    private void subscribeBLEDevice(View view, RxBleDevice device) {
        AvailableDeviceAdapter.ViewHolder availableDeviceHolder = (AvailableDeviceAdapter.ViewHolder) view.getTag();

        unsubscribeBLEDevice();

        connectAvailableDevice = device.establishConnection(DeviceActivity.this, false)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> {
                    String connectionStateValue = StringUtils.getConnectionStateDescription(device.getConnectionState().toString());
                    StringUtils.setConnectionStateValue(availableDeviceHolder.connectionState, connectionStateValue);
                })
                .subscribe(
                        rxBleConnection -> {
                            String connectionStateValue = StringUtils.getConnectionStateDescription(device.getConnectionState().toString());
                            StringUtils.setConnectionStateValue(availableDeviceHolder.connectionState, connectionStateValue);
                            pairedDevices.add(device);
                            availableDevices.remove(device);
                            pairedDeviceAdapter.notifyDataSetChanged();
                            availableDeviceAdapter.notifyDataSetChanged();
                        },
                        throwable -> {
                            Snackbar.make(layout, throwable.toString(), Snackbar.LENGTH_LONG);
                        }
                );
    }

    private void unsubscribeBLEDevice() {
        if (connectAvailableDevice != null && availableDeviceAdapter != null) {
            Snackbar.make(layout, R.string.unpair_confirm_text, Snackbar.LENGTH_LONG).show();
            connectAvailableDevice.unsubscribe();
            connectAvailableDevice = null;
            pairedDevices.clear();
            availableDeviceAdapter.notifyDataSetChanged();
            pairedDeviceAdapter.notifyDataSetChanged();
        }
    }
}
