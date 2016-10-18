package ca.uqac.liara.imurecording;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.ArrayList;
import java.util.List;

import ca.uqac.liara.imurecording.Adapters.DeviceAdapter;
import ca.uqac.liara.imurecording.Utils.StringUtils;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private static final int REQUEST_ENABLE_BLUETOOTH = 201;

    private RelativeLayout layout;
    private ListView listView;
    private FABProgressCircle scanButton;

    private RxBleClient rxBleClient;
    private Subscription scanSubscription;
    private Subscription connectionSubscription;

    private DeviceAdapter adapter;
    private List<RxBleDevice> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGUI();

        scanButton.setOnClickListener(
                v -> {
                    if (scanSubscription == null) {
                        startBLEScan();
                    } else {
                        stopBLESCAN();
                    }
                }
        );

        listView.setOnItemClickListener(
                (parent, view, position, id) -> subscribeBLEDevice(view, adapter.getItem(position))
        );
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopBLESCAN();
        unsubscribeBLEDevice();
    }

    private void initGUI() {
        layout = (RelativeLayout) findViewById(R.id.activity_main);
        listView = (ListView) findViewById(R.id.list);
        scanButton = (FABProgressCircle) findViewById(R.id.fabProgressCircle);

        devices = new ArrayList<>();
        rxBleClient = rxBleClient.create(this);

        adapter = new DeviceAdapter(this, devices);
        listView.setAdapter(adapter);
    }

    private void startBLEScan() {
        scanButton.show();
        scanSubscription = rxBleClient.scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        rxBleScanResult -> {
                            if (!devices.contains(rxBleScanResult.getBleDevice())) {
                                devices.add(rxBleScanResult.getBleDevice());
                                adapter.notifyDataSetChanged();
                            }
                        },
                        throwable -> {
                            Snackbar.make(layout, throwable.toString(), Snackbar.LENGTH_LONG);
                        }
                );
    }

    private void stopBLESCAN() {
        if (scanSubscription != null) {
            scanButton.hide();
            scanSubscription.unsubscribe();
            scanSubscription = null;
        }
    }

    private void subscribeBLEDevice(View view, RxBleDevice device) {
        DeviceAdapter.ViewHolder holder = (DeviceAdapter.ViewHolder) view.getTag();

        connectionSubscription = device.establishConnection(MainActivity.this, false)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(() -> {
                    String connectionStateValue = StringUtils.getConnectionStateDescription(device.getConnectionState().toString());
                    StringUtils.setConnectionStateValue(holder.connectionState, connectionStateValue);
                })
                .subscribe(
                        rxBleConnection -> {
                            String connectionStateValue = StringUtils.getConnectionStateDescription(device.getConnectionState().toString());
                            StringUtils.setConnectionStateValue(holder.connectionState, connectionStateValue);
                        },
                        throwable -> {
                            Snackbar.make(layout, throwable.toString(), Snackbar.LENGTH_LONG);
                        }
                );
    }

    private void unsubscribeBLEDevice() {
        if (connectionSubscription != null && adapter != null) {
            connectionSubscription.unsubscribe();
            adapter.notifyDataSetChanged();
        }
    }
}
