package ca.uqac.liara.imurecording.Activities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.List;

import ca.uqac.liara.imurecording.R;
import ca.uqac.liara.imurecording.Services.BluetoothLEService;
import ca.uqac.liara.imurecording.Utils.ByteArrayUtils;

public class SendDataActivity extends AppCompatActivity {

    private RelativeLayout layout;
    private TextInputLayout hikingNameLayout, sensorLocationLayout, usernameLayout;
    private TextInputEditText hikingNameInput, sensorLocationInput, usernameInput;
    private FloatingActionButton sendButton;
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                showAlertDialogFinishOnDismiss(getResources().getString(R.string.popup_titles),
                        getResources().getString(R.string.device_deconnected_message));
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringExtra(BluetoothLEService.EXTRA_UUID)
                        .equals(getResources().getString(R.string.start_record_uuid))) {
                    toggleView(ByteArrayUtils.byteArrayToBoolean(intent.getByteArrayExtra(BluetoothLEService.EXTRA_DATA)));
                }
            }
        }
    };
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
    private BluetoothDevice device;
    private boolean isRecording;
    private HashMap<BluetoothGattCharacteristic, byte[]> data = new HashMap<>();

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
        setContentView(R.layout.activity_send_data);

        getDataFromActivity();
        initGUI();
        initListeners();
        toggleView(isRecording);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(gattUpdateReceiver);
        unbindService(serviceConnection);
        bluetoothLEService = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getDataFromActivity() {
        final Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");
        isRecording = intent.getBooleanExtra("isRecording", false);
    }

    private void initGUI() {
        layout = (RelativeLayout) findViewById(R.id.activity_send_data);

        hikingNameLayout = (TextInputLayout) findViewById(R.id.hiking_name);
        hikingNameLayout.setErrorEnabled(true);
        hikingNameLayout.setError(getResources().getString(R.string.hiking_name_input_invalid));
        hikingNameInput = (TextInputEditText) findViewById(R.id.hiking_name_input);

        sensorLocationLayout = (TextInputLayout) findViewById(R.id.sensor_location);
        sensorLocationLayout.setErrorEnabled(true);
        sensorLocationLayout.setError(getResources().getString(R.string.sensor_location_input_invalid));
        sensorLocationInput = (TextInputEditText) findViewById(R.id.sensor_location_input);

        usernameLayout = (TextInputLayout) findViewById(R.id.username);
        usernameLayout.setErrorEnabled(true);
        usernameInput = (TextInputEditText) findViewById(R.id.username_input);
        usernameLayout.setError(getResources().getString(R.string.username_input_invalid));

        sendButton = (FloatingActionButton) findViewById(R.id.btn_send);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setTitle((device.getName() == null) ? "Unknown" : device.getName());
    }

    private void initListeners() {
        hikingNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs(s, hikingNameLayout);
            }
        });

        hikingNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateInputs(((EditText) v).getText(), hikingNameLayout);
            }
        });

        sensorLocationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs(s, sensorLocationLayout);
            }
        });

        sensorLocationInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateInputs(((EditText) v).getText(), sensorLocationLayout);
            }
        });

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs(s, usernameLayout);
            }
        });

        usernameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateInputs(((EditText) v).getText(), usernameLayout);
            }
        });

        sendButton.setOnClickListener(
                view -> {
                    if (!(hikingNameLayout.isErrorEnabled()) &&
                            !(sensorLocationLayout.isErrorEnabled()) &&
                            !(usernameLayout.isErrorEnabled())) {

                        writeData(
                                hikingNameInput.getText().toString(),
                                sensorLocationInput.getText().toString(),
                                usernameInput.getText().toString()
                        );
                    }
                }
        );
    }

    private void validateInputs(Editable s, TextInputLayout layout) {
        if (TextUtils.isEmpty(s) || TextUtils.getTrimmedLength(s) > 20) {
            if (layout.equals(hikingNameLayout)) {
                hikingNameLayout.setErrorEnabled(true);
                hikingNameLayout.setError(getResources().getString(R.string.hiking_name_input_invalid));
            } else if (layout.equals(sensorLocationLayout)) {
                sensorLocationLayout.setErrorEnabled(true);
                sensorLocationLayout.setError(getResources().getString(R.string.sensor_location_input_invalid));
            } else if (layout.equals(usernameLayout)) {
                usernameLayout.setErrorEnabled(true);
                usernameLayout.setError(getResources().getString(R.string.username_input_invalid));
            }
        } else {
            if (layout.equals(hikingNameLayout)) {
                hikingNameLayout.setError(null);
                hikingNameLayout.setErrorEnabled(false);
            } else if (layout.equals(sensorLocationLayout)) {
                sensorLocationLayout.setError(null);
                sensorLocationLayout.setErrorEnabled(false);
            } else if (layout.equals(usernameLayout)) {
                usernameLayout.setError(null);
                usernameLayout.setErrorEnabled(false);
            }
        }
    }

    private void toggleView(final boolean started) {
        sendButton.setVisibility(View.INVISIBLE);
        if (started) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDelete, null)));
            } else {
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDelete)));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_cross, null));
            } else {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_cross));
            }
            sendButton.setVisibility(View.VISIBLE);

            hikingNameInput.setEnabled(false);
            hikingNameLayout.setErrorEnabled(false);
            sensorLocationInput.setEnabled(false);
            sensorLocationLayout.setErrorEnabled(false);
            usernameInput.setEnabled(false);
            usernameLayout.setErrorEnabled(false);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorSuccess, null)));
            } else {
                sendButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorSuccess)));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send, null));
            } else {
                sendButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_send));
            }
            sendButton.setVisibility(View.VISIBLE);
            hikingNameInput.setEnabled(true);
            hikingNameInput.setText("");
            hikingNameLayout.setErrorEnabled(true);
            sensorLocationInput.setText("");
            sensorLocationInput.setEnabled(true);
            sensorLocationLayout.setErrorEnabled(true);
            usernameInput.setText("");
            usernameInput.setEnabled(true);
            usernameLayout.setErrorEnabled(true);
        }
    }

    private void writeData(final String hikingName, final String sensorLocation, final String username) {
        data.clear();

        if (isRecording) {
            isRecording = false;

            data.put(
                    bluetoothLEService.getCharacteristicFromService(
                            getResources().getString(R.string.metadata_service),
                            getResources().getString(R.string.start_record_uuid)),
                    ByteArrayUtils.booleanToByteArray(isRecording)
            );
        } else {
            isRecording = true;

            final List<BluetoothGattCharacteristic> characteristics =
                    bluetoothLEService.getCharacteristicsFromService(
                            getResources().getString(R.string.metadata_service));

            for (BluetoothGattCharacteristic characteristic : characteristics) {
                String charac = characteristic.getUuid().toString();
                if (charac.equals(getResources().getString(R.string.start_record_uuid))) {
                    data.put(
                            bluetoothLEService.getCharacteristicFromService(
                                    getResources().getString(R.string.metadata_service),
                                    charac),
                            ByteArrayUtils.booleanToByteArray(isRecording)
                    );
                } else if (charac.equals(getResources().getString(R.string.hiking_name_uuid))) {
                    data.put(
                            bluetoothLEService.getCharacteristicFromService(
                                    getResources().getString(R.string.metadata_service),
                                    charac),
                            ByteArrayUtils.stringToByteArray(hikingName)
                    );
                } else if (charac.equals(getResources().getString(R.string.sensor_location_uuid))) {
                    data.put(
                            bluetoothLEService.getCharacteristicFromService(
                                    getResources().getString(R.string.metadata_service),
                                    charac),
                            ByteArrayUtils.stringToByteArray(sensorLocation)
                    );
                } else if (charac.equals(getResources().getString(R.string.username_uuid))) {
                    data.put(
                            bluetoothLEService.getCharacteristicFromService(
                                    getResources().getString(R.string.metadata_service),
                                    charac),
                            ByteArrayUtils.stringToByteArray(username)
                    );
                }
            }
        }
        bluetoothLEService.setCharacteristics(data);
        bluetoothLEService.writeCharacteristics();
    }

    private void showAlertDialogFinishOnDismiss(final String title, final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(dialog -> this.finish());
        builder.show();
    }
}
