package ca.uqac.liara.imurecording.Activities;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.github.jorgecastilloprz.FABProgressCircle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ca.uqac.liara.imurecording.Adapters.AvailableDeviceAdapter;
import ca.uqac.liara.imurecording.Adapters.PairedDeviceAdapter;
import ca.uqac.liara.imurecording.R;
import ca.uqac.liara.imurecording.Services.BluetoothLEService;
import ca.uqac.liara.imurecording.Utils.ByteArrayUtils;

public class BLEDeviceActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final int REQUEST_ENABLE_BLUETOOTH = 201;
    private static final long SCAN_PERIOD_TIMEOUT = 10000;

    private View itemView;
    private RelativeLayout layout;

    private ListView availableDevicesList, pairedDevicesList;
    private FABProgressCircle scanButton;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothLEService bluetoothLEService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothLEService = null;
        }
    };
    private AvailableDeviceAdapter availableDeviceAdapter;
    private final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {
        runOnUiThread(
                () -> {
                    availableDeviceAdapter.addDevice(device);
                    availableDeviceAdapter.notifyDataSetChanged();
                }
        );
    };
    private PairedDeviceAdapter pairedDeviceAdapter;
    private List<BluetoothDevice> pairedDevice;
    private List<BluetoothDevice> availableDevices;
    private Handler handler;
    private boolean isScanning;
    private boolean isRecording = false;
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                updateViewOnPair(false, true);
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateViewOnPair(false, false);
                final Snackbar snackbar = Snackbar.make(layout,
                        getResources().getString(R.string.unpair_confirm_text),
                        Snackbar.LENGTH_LONG);
                snackbar.show();
            } else if (BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                subscribeGattCharacteristics(bluetoothLEService.getSupportedGattServices());
                readCharacteristic(UUID.fromString(getResources().getString(R.string.metadata_service)),
                        UUID.fromString(getResources().getString(R.string.start_record_uuid)));
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                setIsRecording(intent.getStringExtra(BluetoothLEService.EXTRA_UUID), intent.getByteArrayExtra(BluetoothLEService.EXTRA_DATA));
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_device);

        handler = new Handler();

        initGUI();
        initListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onResume() {
        super.onResume();

        initBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkRuntimePermission();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(gattUpdateReceiver);
        unbindService(serviceConnection);
        bluetoothLEService = null;
    }

    private void initGUI() {
        layout = (RelativeLayout) findViewById(R.id.activity_ble_devices);

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
                    if (bluetoothAdapter == null) {
                        Snackbar.make(layout, "Bluetooth was not initialized", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    if (!isScanning) {
                        scan(true);
                    } else {
                        scan(false);
                    }
                }
        );

        availableDevicesList.setOnItemClickListener(
                (parent, view, position, id) -> {
                    device = availableDeviceAdapter.getDevice(position);

                    if (pairedDeviceAdapter.getCount() > 0) {
                        final Snackbar snackbar = Snackbar.make(layout,
                                getResources().getString(R.string.device_already_paired),
                                Snackbar.LENGTH_LONG);
                        snackbar.show();
                        return;
                    }

                    itemView = view;
                    updateViewOnPair(true, null);
                    bluetoothLEService.connect(device);
                }
        );

        pairedDeviceAdapter.setOnUseButtonClickListener(
                position -> {
                    device = pairedDeviceAdapter.getDevice(position);
                    sendDataToActivity(device, isRecording);
                }
        );

        pairedDeviceAdapter.setOnUnpairButtonClickListener(
                position -> {
                    updateViewOnPair(false, false);
                    bluetoothLEService.disconnect();
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
                    showAlertDialog(getResources().getString(R.string.popup_titles),
                            getResources().getString(R.string.alert_bluetooth_message));
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
                    showAlertDialog(getResources().getString(R.string.popup_titles),
                            getResources().getString(R.string.alert_location_permission_denied_message));
                }
                break;
        }
    }

    private void showAlertDialog(final String title, final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                        isScanning = false;
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    },
                    SCAN_PERIOD_TIMEOUT
            );

            isScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            scanButton.hide();
            isScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }

//    private void subscribeGattCharacteristics(List<BluetoothGattService> gattServices) {
//        final List<BluetoothGattCharacteristic> gattCharacteristics = new ArrayList<>();
//
//        if (gattServices.size() > 0) {
//            for (BluetoothGattService gattService : gattServices.subList(2, gattServices.size())) {
//                List<BluetoothGattCharacteristic> gattServiceCharacteristics = gattService.getCharacteristics();
//                gattCharacteristics.addAll(gattServiceCharacteristics);
//            }
//        }
//
//        if (gattCharacteristics.size() > 0) {
//            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                bluetoothLEService.setCharacteristicNotification(gattCharacteristic, true);
//            }
//        }
//    }

    private void updateViewOnPair(final Boolean init, final Boolean enable) {

        final AvailableDeviceAdapter.ViewHolder holder = (AvailableDeviceAdapter.ViewHolder) itemView.getTag();

        runOnUiThread(
                () -> {
                    if (init && enable == null) {
                        holder.connectionState.setVisibility(View.INVISIBLE);
                        holder.progress.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (enable) {
                        holder.connectionState.setVisibility(View.VISIBLE);
                        holder.progress.setVisibility(View.INVISIBLE);

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
        );
    }

    private void readCharacteristic(UUID uuidService, UUID uuidCharacteristic) {
        final List<BluetoothGattService> services = bluetoothLEService.getSupportedGattServices();

        BluetoothGattService service = null;
        BluetoothGattCharacteristic characteristic = null;

        for (BluetoothGattService s : services) {
            if (s.getUuid().equals(uuidService)) {
                service = s;
            }
        }

        if (service != null) {
            characteristic = service.getCharacteristic(uuidCharacteristic);
        }

        if (characteristic != null) {
            int properties = characteristic.getProperties();
            if (((properties & BluetoothGattCharacteristic.PROPERTY_READ) ==
                    BluetoothGattCharacteristic.PROPERTY_READ)) {
                bluetoothLEService.readCharacteristic(characteristic);
            }
        }
    }

    private void setIsRecording(String uuid, byte[] data) {
        if (uuid.equals(getResources().getString(R.string.start_record_uuid))) {
            final boolean started = ByteArrayUtils.byteArrayToBoolean(data);

            isRecording = started;

            bluetoothLEService.setCharacteristicNotification(
                    bluetoothLEService.getCharacteristicFromService(
                            getResources().getString(R.string.metadata_service),
                            getResources().getString(R.string.start_record_uuid)), true);
        }
    }

    private void sendDataToActivity(BluetoothDevice device, boolean b) {
        Intent intent = new Intent(this, SendDataActivity.class);
        Bundle bundle = new Bundle();

        bundle.putParcelable("device", device);
        bundle.putBoolean("isRecording", b);

        intent.putExtras(bundle);
        startActivity(intent);
    }
}
