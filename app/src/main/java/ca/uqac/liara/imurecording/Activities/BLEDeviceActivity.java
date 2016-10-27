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

import ca.uqac.liara.imurecording.Adapters.AvailableDeviceAdapter;
import ca.uqac.liara.imurecording.Adapters.PairedDeviceAdapter;
import ca.uqac.liara.imurecording.R;
import ca.uqac.liara.imurecording.Services.BluetoothLEService;

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

//    private RxBleClient rxBleClient;
//    private RxBleDevice rxDevice;
//    private Subscription connectionSubscription;
//    private Observable<RxBleConnection> connectionObservable;
//    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
//    private Observable<List<GattService>> serviceObservable;


    private AvailableDeviceAdapter availableDeviceAdapter;
    private PairedDeviceAdapter pairedDeviceAdapter;
    private List<BluetoothDevice> pairedDevice;
    private List<BluetoothDevice> availableDevices;

    private Handler handler;
    private boolean isScanning;

//    private HashMap<UUID, byte[]> characteristics = new HashMap<>();

    @Override
    protected void onResume() {
        super.onResume();

        initBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkRuntimePermission();
        }

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_device);

        handler = new Handler();

        initGUI();
        initListeners();
//        initServices();

        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        disconnect();
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
//                    pair(true, device, view);

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
                    sendDataActivity(device);
//                    subscribeCharacteristics();
//                    readCharacteristic(UUID.fromString(getResources().getString(R.string.start_record_uuid)));
                }
        );

        pairedDeviceAdapter.setOnUnpairButtonClickListener(
                position -> {
//                    pair(false, device, null);
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

//        rxBleClient = RxBleClient.create(this);
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
                            getResources().getString(R.string.alert_work_message));
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
                            getResources().getString(R.string.alert_work_message));
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
        builder.setOnDismissListener(dialog -> {});
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

    private final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {
        runOnUiThread(
                () -> {
                    availableDeviceAdapter.addDevice(device);
                    availableDeviceAdapter.notifyDataSetChanged();
                }
        );
    };

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
                subscribeGattCharacteristics(bluetoothLEService.getSupportedGattServices());
            }
        }
    };

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

    private void subscribeGattCharacteristics(List<BluetoothGattService> gattServices) {
        final List<BluetoothGattCharacteristic> gattCharacteristics = new ArrayList<>();

        if (gattServices.size() > 0) {
            for (BluetoothGattService gattService : gattServices.subList(2, gattServices.size())) {
                List<BluetoothGattCharacteristic> gattServiceCharacteristics = gattService.getCharacteristics();
                gattCharacteristics.addAll(gattServiceCharacteristics);
            }
        }

        if (gattCharacteristics.size() > 0) {
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                bluetoothLEService.setCharacteristicNotification(gattCharacteristic, true);
            }
        }
    }

    private void sendDataActivity(BluetoothDevice device) {
        Intent intent = new Intent(this, SendDataActivity.class);
        intent.putExtra("device", device);
        startActivity(intent);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

//    private void pair(final boolean enable, BluetoothDevice device, View view) {
//        if (enable) {
//            AvailableDeviceAdapter.ViewHolder holder = (AvailableDeviceAdapter.ViewHolder) view.getTag();
//
//            if (pairedDeviceAdapter.getCount() > 0) {
//                final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.device_already_paired), Snackbar.LENGTH_LONG);
//                snackbar.show();
//                return;
//            }
//
//            try {
//                rxDevice = rxBleClient.getBleDevice(device.getAddress());
//            } catch (Exception e) {
//                final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.device_unreachable), Snackbar.LENGTH_LONG);
//                snackbar.setAction(getResources().getString(R.string.action_retry), v -> pair(true, device, view));
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    snackbar.setActionTextColor(getResources().getColor(R.color.colorDelete, null));
//                } else {
//                    snackbar.setActionTextColor(getResources().getColor(R.color.colorDelete));
//                }
//
//                snackbar.show();
//            }
//
//            holder.connectionState.setVisibility(View.INVISIBLE);
//            holder.progress.setVisibility(View.VISIBLE);
//
//            connectionObservable = rxDevice
//                    .establishConnection(this, false)
//                    .takeUntil(disconnectTriggerSubject)
//                    .doOnUnsubscribe(() -> connectionObservable = null)
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .compose(new ConnectionSharingAdapter());
//
//            connectionSubscription = connectionObservable
//                    .subscribe(
//                            rxBleConnection -> {
//                                holder.connectionState.setVisibility(View.VISIBLE);
//                                holder.progress.setVisibility(View.GONE);
//
//                                pairedDeviceAdapter.addDevice(device);
//                                availableDeviceAdapter.removeDevice(device);
//                                availableDeviceAdapter.setPairedDevice(device);
//                                pairedDeviceAdapter.notifyDataSetChanged();
//                                availableDeviceAdapter.notifyDataSetChanged();
//
//                                initServices();
//
//
//                            }, throwable -> {
//                                holder.connectionState.setVisibility(View.VISIBLE);
//                                holder.progress.setVisibility(View.GONE);
//
//                                final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.device_connection_failed), Snackbar.LENGTH_LONG);
//                                snackbar.setAction(getResources().getString(R.string.action_retry), v -> pair(true, device, view));
//                                snackbar.setActionTextColor(getResources().getColor(R.color.colorDelete));
//                                snackbar.show();
//                            }
//                    );
//        } else {
//            disconnect();
//
//            pairedDeviceAdapter.clear();
//            pairedDeviceAdapter.notifyDataSetChanged();
//            availableDeviceAdapter.setPairedDevice(null);
//
//            final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.unpair_confirm_text), Snackbar.LENGTH_LONG);
//            snackbar.show();
//        }
//    }

//    private boolean isConnected() {
//        return rxDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
//    }

//    private void disconnect() {
//        if (connectionSubscription != null &&
//                !connectionSubscription.isUnsubscribed()) {
//            connectionObservable = null;
//            connectionSubscription.unsubscribe();
//        }
//    }

//    private void initServices() {
//        serviceObservable = Observable.create(
//                subscriber -> {
//                    try {
//                        List<GattService> services = new ArrayList<>();
//                        services.add(new GattService(UUID.fromString(getResources().getString(R.string.gps_service))));
//                        services.add(new GattService(UUID.fromString(getResources().getString(R.string.weather_service))));
//                        services.add(new GattService(UUID.fromString(getResources().getString(R.string.metadata_service))));
//                        services.add(new GattService(UUID.fromString(getResources().getString(R.string.imu_service))));
//
//                        subscriber.onNext(services);
//                        subscriber.onCompleted();
//
//                    } catch (Exception e) {
//                        Log.e(getClass().getSimpleName(), "Error occurs : " + e.getMessage());
//                    }
//                }
//        );
//    }

//    private void  subscribeCharacteristics() {
//        if (isConnected()) {
//            connectionObservable.flatMap(
//                    rxBleConnection ->
//                            rxBleConnection.discoverServices()
//                                    .flatMap(rxBleDeviceServices ->
//                                            serviceObservable
//                                                    .flatMapIterable(services -> services)
//                                                    .flatMap(service ->
//                                                            rxBleDeviceServices.getService(service.getUuid())
//                                                                    .map(BluetoothGattService::getCharacteristics)))
//                                    .flatMap(Observable::from)
//                                    .flatMap(characteristic ->
//                                            rxBleConnection.setupNotification(characteristic).flatMap(
//                                                    observable ->
//                                                            observable
//                                            ),
//                                            Pair::new
//                                    )
//            ).subscribe(
//                    observablePair -> {
//                        characteristics.put(
//                                observablePair.first.getUuid(),
//                                observablePair.second
//                        );
//                    }, throwable -> {
//                        Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());
//
//                    }
//            );
//        }
//    }

//    private void readCharacteristic(UUID uuid) {
//        if (isConnected()){
//            connectionObservable.flatMap(
//                    rxBleConnection ->
//                            rxBleConnection.readCharacteristic(uuid)
//            ).subscribe(
//                    bytes -> {
//                        characteristics.put(uuid, bytes);
//                        runOnUiThread(() -> toggleActionButton(ByteArrayUtils.byteArrayToBoolean(bytes)));
//                    },
//                    throwable -> {
//                        Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());
//                    }
//            );
//        }
//    }

//    private void toggleActionButton(final boolean b) {
//        final boolean isVisible = sendButton.getVisibility() == View.VISIBLE;
//
//        if (!isVisible) {
//            sendButton.setVisibility(View.VISIBLE);
//            YoYo.with(Techniques.SlideInRight).duration(500).playOn(mainLayout.findViewById(R.id.btn_send));
//        } else {
//            YoYo.with(Techniques.ZoomOut).duration(500).playOn(mainLayout.findViewById(R.id.btn_send));
//        }
//
//        if (b) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_cross, null));
//                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDelete, null)));
//            } else {
//                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_cross));
//                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDelete)));
//            }
//        } else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send, null));
//                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary, null)));
//            } else {
//                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send));
//                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
//            }
//        }
//
//        if (isVisible) {
//            YoYo.with(Techniques.ZoomIn).delay(500).duration(500).playOn(mainLayout.findViewById(R.id.btn_send));
//        }
//    }

//    private void test() {
//        final UUID uuidStartStop = UUID.fromString(getResources().getString(R.string.start_record_uuid));
//        final boolean isStarted = ByteArrayUtils.byteArrayToBoolean(characteristics.get(uuidStartStop));
//
//        if (isStarted) {
//            if (isConnected()) {
//                connectionObservable.flatMap(
//                        rxBleConnection ->
//                                rxBleConnection.writeCharacteristic(
//                                        UUID.fromString(getResources().getString(R.string.start_record_uuid)),
//                                        ByteArrayUtils.booleanToByteArray(false)
//                                )
//                ).subscribe(
//                        bytes -> {
//                            Snackbar.make(mainLayout, "Record correctly stopped",
//                                    Snackbar.LENGTH_LONG).show();
//                            runOnUiThread(() -> toggleActionButton(ByteArrayUtils.byteArrayToBoolean(bytes)));
//                            Log.e("TEST", "" + characteristics.get(UUID.fromString(getResources().getString(R.string.start_record_uuid))));
//                        },
//                        throwable -> {
//                            Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());
//                            Snackbar.make(mainLayout, "Error while trying to stop the record",
//                                    Snackbar.LENGTH_LONG).show();
//                        }
//                );
//            }
//        } else {
//            if (isConnected()) {
//                connectionObservable.flatMap(
//                        rxBleConnection ->
//                                rxBleConnection.writeCharacteristic(
//                                        UUID.fromString(getResources().getString(R.string.start_record_uuid)),
//                                        ByteArrayUtils.booleanToByteArray(true)
//                                )
//                ).subscribe(
//                        bytes -> {
//                            Snackbar.make(mainLayout, "Record correctly started",
//                                    Snackbar.LENGTH_LONG).show();
//                            runOnUiThread(() -> toggleActionButton(ByteArrayUtils.byteArrayToBoolean(bytes)));
//                            Log.e("TEST", "" + characteristics.get(UUID.fromString(getResources().getString(R.string.start_record_uuid))));
//                        },
//                        throwable -> {
//                            Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());
//                            Snackbar.make(mainLayout, "Error while trying to stop the record",
//                                    Snackbar.LENGTH_LONG).show();
//                        }
//                );
//            }
//        }
//    }

//    private void writeCharacteristics() {
//        final UUID uuidStartStop = UUID.fromString(getResources().getString(R.string.start_record_uuid));
//        final boolean isStarted = characteristics.get(uuidStartStop).equals(ByteArrayUtils.booleanToByteArray(true));
//
//        if (isConnected()){
//            if (isStarted) {
//                connectionObservable.flatMap(
//                        rxBleConnection ->
//                                rxBleConnection.writeCharacteristic(
//                                        UUID.fromString(getResources().getString(R.string.start_record_uuid)),
//                                        ByteArrayUtils.booleanToByteArray(false)
//                                )
//                ).subscribe();
//            } else {
//                for (Map.Entry entry : characteristics.entrySet()) {
//
//                }
//            }
//        }
//    }
}
