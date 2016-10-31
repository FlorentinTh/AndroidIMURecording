package ca.uqac.liara.imurecording.Services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by FlorentinTh on 10/27/2016.
 */

public class BluetoothLEService extends Service {

    public final static String ACTION_GATT_CONNECTED = "ca.uqac.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ca.uqac.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ca.uqac.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ca.uqac.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_UUID = "ca.uqac.bluetooth.le.EXTRA_UUID";
    public final static String EXTRA_DATA = "ca.uqac.bluetooth.le.EXTRA_DATA";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private final IBinder binder = new LocalBinder();
    private BluetoothGatt bluetoothGatt;
    private String deviceAddress;
    private int connectionState = STATE_DISCONNECTED;
    private HashMap<BluetoothGattCharacteristic, byte[]> characteristics;
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(getClass().getSimpleName(), "Connected to GATT server.");
                Log.i(getClass().getSimpleName(), "Start discovering services.");
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = STATE_DISCONNECTED;
                Log.i(getClass().getSimpleName(), "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.i(getClass().getSimpleName(), "Services discovered.");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(getClass().getSimpleName(), "Characteristic: " + characteristic.getUuid() + " read.");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(getClass().getSimpleName(), "Characteristic:" + characteristic.getUuid() + " changed.");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(getClass().getSimpleName(), "Characteristic:" + characteristic.getUuid() + " written.");

                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

                characteristics.remove(characteristic);

                if (characteristics.size() > 0) {
                    writeCharacteristics();
                }
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());

        final byte[] data = characteristic.getValue();

        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA, data);
        }

        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        return super.onUnbind(intent);
    }

    public boolean connect(final BluetoothDevice device) {

        final String address = device.getAddress();

        if (address == null) {
            Log.w(getClass().getSimpleName(), "Unspecified device address.");
            return false;
        }

        if (deviceAddress != null && address.equals(deviceAddress) && bluetoothGatt != null) {
            Log.d(getClass().getSimpleName(), "Trying to use an existing mBluetoothGatt for connection.");

            if (bluetoothGatt.connect()) {
                connectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        if (device == null) {
            Log.w(getClass().getSimpleName(), "Device not found. Unable to connect.");
            return false;
        }

        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        Log.d(getClass().getSimpleName(), "Trying to create a new connection.");
        deviceAddress = address;
        connectionState = STATE_CONNECTING;

        return true;
    }

    public void disconnect() {
        if (bluetoothGatt == null) {
            Log.w(getClass().getSimpleName(), "bluetoothGatt not initialized");
            return;
        }

        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        deviceAddress = null;
        bluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w(getClass().getSimpleName(), "BluetoothAdapter not initialized");
            return;
        }

        bluetoothGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w(getClass().getSimpleName(), "BluetoothAdapter not initialized");
            return;
        }

        characteristic.setValue(this.characteristics.get(characteristic));
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    public void setCharacteristics(HashMap<BluetoothGattCharacteristic, byte[]> characteristics) {
        this.characteristics = characteristics;
    }

    public void writeCharacteristics() {
        if (bluetoothGatt == null) {
            Log.w(getClass().getSimpleName(), "BluetoothAdapter not initialized");
            return;
        }

        writeCharacteristic((BluetoothGattCharacteristic) this.characteristics.keySet().toArray()[this.characteristics.size() - 1]);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w(getClass().getSimpleName(), "BluetoothAdapter not initialized");
            return;
        }

        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)
                == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
            if (bluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
            }
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) {
            return null;
        }

        return bluetoothGatt.getServices();
    }

    public List<BluetoothGattCharacteristic> getCharacteristicsFromService(String uuidService) {
        if (bluetoothGatt == null) {
            return null;
        }

        final List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
        characteristics.addAll(bluetoothGatt.getService(UUID.fromString(uuidService)).getCharacteristics());
        return characteristics;
    }

    public BluetoothGattCharacteristic getCharacteristicFromService(String uuidService, String uuidCharacteristic) {
        if (bluetoothGatt == null) {
            return null;
        }

        return bluetoothGatt.getService(UUID.fromString(uuidService)).getCharacteristic(UUID.fromString(uuidCharacteristic));
    }

    public class LocalBinder extends Binder {
        public BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }
}
