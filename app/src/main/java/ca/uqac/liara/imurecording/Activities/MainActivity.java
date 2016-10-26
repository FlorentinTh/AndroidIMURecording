package ca.uqac.liara.imurecording.Activities;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.github.jorgecastilloprz.FABProgressCircle;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import ca.uqac.liara.imurecording.Classes.BLEService;
import ca.uqac.liara.imurecording.Adapters.AvailableDeviceAdapter;
import ca.uqac.liara.imurecording.Adapters.PairedDeviceAdapter;
import ca.uqac.liara.imurecording.R;
import ca.uqac.liara.imurecording.Utils.ByteArrayUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final int REQUEST_ENABLE_BLUETOOTH = 201;
    private static final long SCAN_PERIOD_TIMEOUT = 10000;

    private Toolbar toolbar;

    private RelativeLayout mainLayout, deviceLayout, communicationLayout;

    private ListView availableDevicesList, pairedDevicesList;
    private FABProgressCircle scanButton;

    private EditText hikingNameInput, sensorLocationInput, usernameInput;
    private FloatingActionButton sendButton;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private RxBleClient rxBleClient;
    private RxBleDevice rxDevice;
    private Subscription connectionSubscription;
    private Observable<RxBleConnection> connectionObservable;
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<List<BLEService>> serviceObservable;

    private AvailableDeviceAdapter availableDeviceAdapter;
    private PairedDeviceAdapter pairedDeviceAdapter;

    private List<BluetoothDevice> pairedDevice;
    private List<BluetoothDevice> availableDevices;

    private Handler handler;

    private boolean isScanning;
    private HashMap<UUID, byte[]> characteristics = new HashMap<>();

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
        setContentView(R.layout.activity_main);

        handler = new Handler();

        initGUI();
        initListeners();
        initServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    private void initGUI() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mainLayout = (RelativeLayout) findViewById(R.id.activity_main);
        deviceLayout = (RelativeLayout) findViewById(R.id.layout_device);
        communicationLayout = (RelativeLayout) findViewById(R.id.layout_communication);

        pairedDevicesList = (ListView) findViewById(R.id.paired_devices);
        availableDevicesList = (ListView) findViewById(R.id.available_devices);
        scanButton = (FABProgressCircle) findViewById(R.id.btn_scan);

        hikingNameInput = (EditText) findViewById(R.id.hiking_name_input);
        sensorLocationInput = (EditText) findViewById(R.id.sensor_location_input);
        usernameInput = (EditText) findViewById(R.id.username_input);
        sendButton = (FloatingActionButton) findViewById(R.id.btn_send);

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
                    if (!isScanning) {
                        scan(true);
                    } else {
                        scan(false);
                    }
                }
        );

        availableDevicesList.setOnItemClickListener(
                (parent, view, position, id) -> {
                    final BluetoothDevice device = availableDeviceAdapter.getDevice(position);
                    pair(true, device, view);
                }
        );

        pairedDeviceAdapter.setOnUseButtonClickListener(
                position -> {
                    toggleViews(false);
                    subscribeCharacteristics();
                    readCharacteristic(UUID.fromString(getResources().getString(R.string.start_record_uuid)));
                }
        );

        pairedDeviceAdapter.setOnUnpairButtonClickListener(
                position -> {
                    final BluetoothDevice device = pairedDeviceAdapter.getDevice(position);
                    pair(false, device, null);
                }
        );

        toolbar.setNavigationOnClickListener(v -> toggleViews(true));

        sendButton.setOnClickListener(
                view -> {
                    switch (validateInputs()) {
                        case 0:
                            test();
                            break;
                        case 1:
                            Snackbar.make(mainLayout,
                                    getResources().getString(R.string.hiking_name_input_invalid),
                                    Snackbar.LENGTH_LONG).show();
                            break;
                        case 2:
                            Snackbar.make(mainLayout,
                                    getResources().getString(R.string.sensor_location_input_invalid),
                                    Snackbar.LENGTH_LONG).show();
                            break;
                        case 3:
                            Snackbar.make(mainLayout,
                                    getResources().getString(R.string.username_input_invalid),
                                    Snackbar.LENGTH_LONG).show();
                            break;
                    }
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

        rxBleClient = RxBleClient.create(this);
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

    private void toggleViews(final boolean first) {
        if (first) {
            deviceLayout.setVisibility(View.VISIBLE);
            communicationLayout.setVisibility(View.INVISIBLE);
            YoYo.with(Techniques.FadeIn).duration(500).playOn(deviceLayout);
            toolbar.setNavigationIcon(null);

        } else {
            deviceLayout.setVisibility(View.INVISIBLE);
            communicationLayout.setVisibility(View.VISIBLE);
            YoYo.with(Techniques.FadeIn).duration(500).playOn(communicationLayout);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp, null));
            } else {
                toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
            }
        }
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

    private BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {
        runOnUiThread(
                () -> {
                    availableDeviceAdapter.addDevice(device);
                    availableDeviceAdapter.notifyDataSetChanged();
                }
        );
    };

    private void pair(final boolean enable, BluetoothDevice device, View view) {
        if (enable) {
            AvailableDeviceAdapter.ViewHolder holder = (AvailableDeviceAdapter.ViewHolder) view.getTag();

            if (pairedDeviceAdapter.getCount() > 0) {
                final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.device_already_paired), Snackbar.LENGTH_LONG);
                snackbar.show();
                return;
            }

            try {
                rxDevice = rxBleClient.getBleDevice(device.getAddress());
            } catch (Exception e) {
                final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.device_unreachable), Snackbar.LENGTH_LONG);
                snackbar.setAction(getResources().getString(R.string.action_retry), v -> pair(true, device, view));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    snackbar.setActionTextColor(getResources().getColor(R.color.colorDelete, null));
                } else {
                    snackbar.setActionTextColor(getResources().getColor(R.color.colorDelete));
                }

                snackbar.show();
            }

            holder.connectionState.setVisibility(View.INVISIBLE);
            holder.progress.setVisibility(View.VISIBLE);

            connectionObservable = rxDevice
                    .establishConnection(this, false)
                    .takeUntil(disconnectTriggerSubject)
                    .doOnUnsubscribe(() -> connectionObservable = null)
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(new ConnectionSharingAdapter());

            connectionSubscription = connectionObservable
                    .subscribe(
                            rxBleConnection -> {
                                holder.connectionState.setVisibility(View.VISIBLE);
                                holder.progress.setVisibility(View.GONE);

                                pairedDeviceAdapter.addDevice(device);
                                availableDeviceAdapter.removeDevice(device);
                                availableDeviceAdapter.setPairedDevice(device);
                                pairedDeviceAdapter.notifyDataSetChanged();
                                availableDeviceAdapter.notifyDataSetChanged();

                                initServices();


                            }, throwable -> {
                                holder.connectionState.setVisibility(View.VISIBLE);
                                holder.progress.setVisibility(View.GONE);

                                final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.device_connection_failed), Snackbar.LENGTH_LONG);
                                snackbar.setAction(getResources().getString(R.string.action_retry), v -> pair(true, device, view));
                                snackbar.setActionTextColor(getResources().getColor(R.color.colorDelete));
                                snackbar.show();
                            }
                    );
        } else {
            disconnect();

            pairedDeviceAdapter.clear();
            pairedDeviceAdapter.notifyDataSetChanged();
            availableDeviceAdapter.setPairedDevice(null);

            final Snackbar snackbar = Snackbar.make(mainLayout, getResources().getString(R.string.unpair_confirm_text), Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    private boolean isConnected() {
        return rxDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void disconnect() {
        if (connectionSubscription != null &&
                !connectionSubscription.isUnsubscribed()) {
            connectionObservable = null;
            connectionSubscription.unsubscribe();
        }
    }

    private void initServices() {
        serviceObservable = Observable.create(
                subscriber -> {
                    try {
                        List<BLEService> services = new ArrayList<>();
                        services.add(new BLEService(UUID.fromString(getResources().getString(R.string.gps_service))));
                        services.add(new BLEService(UUID.fromString(getResources().getString(R.string.weather_service))));
                        services.add(new BLEService(UUID.fromString(getResources().getString(R.string.metadata_service))));
                        services.add(new BLEService(UUID.fromString(getResources().getString(R.string.imu_service))));

                        subscriber.onNext(services);
                        subscriber.onCompleted();

                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), "Error occurs : " + e.getMessage());
                    }
                }
        );
    }

    private void  subscribeCharacteristics() {
        if (isConnected()) {
            connectionObservable.flatMap(
                    rxBleConnection ->
                            rxBleConnection.discoverServices()
                                    .flatMap(rxBleDeviceServices ->
                                            serviceObservable
                                                    .flatMapIterable(services -> services)
                                                    .flatMap(service ->
                                                            rxBleDeviceServices.getService(service.getUuid())
                                                                    .map(BluetoothGattService::getCharacteristics)))
                                    .flatMap(Observable::from)
                                    .flatMap(characteristic ->
                                            rxBleConnection.setupNotification(characteristic).flatMap(
                                                    observable ->
                                                            observable
                                            ),
                                            Pair::new
                                    )
            ).subscribe(
                    observablePair -> {
                        characteristics.put(
                                observablePair.first.getUuid(),
                                observablePair.second
                        );
                    }, throwable -> {
                        Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());

                    }
            );
        }
    }

    private int validateInputs() {
        final int hikingNameInputLength = hikingNameInput.getText().toString().trim().length();
        final int sensorLocationInputLength = sensorLocationInput.getText().toString().trim().length();
        final int usernameInputLength = usernameInput.getText().toString().trim().length();

        if (hikingNameInputLength == 0 ||
                hikingNameInputLength > 20) {
            return 1;
        }

        if (sensorLocationInputLength == 0 ||
                sensorLocationInputLength > 20) {
            return 2;
        }

        if (usernameInputLength == 0 ||
                usernameInputLength > 20) {
            return 3;
        }
        return 0;
    }

    private void readCharacteristic(UUID uuid) {
        if (isConnected()){
            connectionObservable.flatMap(
                    rxBleConnection ->
                            rxBleConnection.readCharacteristic(uuid)
            ).subscribe(
                    bytes -> {
                        characteristics.put(uuid, bytes);
                        runOnUiThread(() -> toggleActionButton(ByteArrayUtils.byteArrayToBoolean(bytes)));
                    },
                    throwable -> {
                        Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());
                    }
            );
        }
    }

    private void toggleActionButton(final boolean b) {
        final boolean isVisible = sendButton.getVisibility() == View.VISIBLE;

        if (!isVisible) {
            sendButton.setVisibility(View.VISIBLE);
            YoYo.with(Techniques.SlideInRight).duration(500).playOn(mainLayout.findViewById(R.id.btn_send));
        } else {
            YoYo.with(Techniques.ZoomOut).duration(500).playOn(mainLayout.findViewById(R.id.btn_send));
        }

        if (b) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_cross, null));
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDelete, null)));
            } else {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_cross));
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDelete)));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send, null));
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary, null)));
            } else {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send));
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
            }
        }

        if (isVisible) {
            YoYo.with(Techniques.ZoomIn).delay(500).duration(500).playOn(mainLayout.findViewById(R.id.btn_send));
        }
    }

    private void test() {
        final UUID uuidStartStop = UUID.fromString(getResources().getString(R.string.start_record_uuid));
        final boolean isStarted = ByteArrayUtils.byteArrayToBoolean(characteristics.get(uuidStartStop));

        if (isStarted) {
            if (isConnected()) {
                connectionObservable.flatMap(
                        rxBleConnection ->
                                rxBleConnection.writeCharacteristic(
                                        UUID.fromString(getResources().getString(R.string.start_record_uuid)),
                                        ByteArrayUtils.booleanToByteArray(false)
                                )
                ).subscribe(
                        bytes -> {
                            Snackbar.make(mainLayout, "Record correctly stopped",
                                    Snackbar.LENGTH_LONG).show();
                            runOnUiThread(() -> toggleActionButton(ByteArrayUtils.byteArrayToBoolean(bytes)));
                            Log.e("TEST", "" + characteristics.get(UUID.fromString(getResources().getString(R.string.start_record_uuid))));
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());
                            Snackbar.make(mainLayout, "Error while trying to stop the record",
                                    Snackbar.LENGTH_LONG).show();
                        }
                );
            }
        } else {
            if (isConnected()) {
                connectionObservable.flatMap(
                        rxBleConnection ->
                                rxBleConnection.writeCharacteristic(
                                        UUID.fromString(getResources().getString(R.string.start_record_uuid)),
                                        ByteArrayUtils.booleanToByteArray(true)
                                )
                ).subscribe(
                        bytes -> {
                            Snackbar.make(mainLayout, "Record correctly started",
                                    Snackbar.LENGTH_LONG).show();
                            runOnUiThread(() -> toggleActionButton(ByteArrayUtils.byteArrayToBoolean(bytes)));
                            Log.e("TEST", "" + characteristics.get(UUID.fromString(getResources().getString(R.string.start_record_uuid))));
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "Error occurs : " + throwable.toString());
                            Snackbar.make(mainLayout, "Error while trying to stop the record",
                                    Snackbar.LENGTH_LONG).show();
                        }
                );
            }
        }
    }

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
