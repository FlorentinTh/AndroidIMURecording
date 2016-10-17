package ca.uqac.liara.imurecording;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout layout;
    private ListView listView;
    private Button scanButton;

    private RxBleClient rxBleClient;
    private Subscription scanSubscription;

    private DeviceAdapter adapter;
    private List<RxBleDevice> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        devices = new ArrayList<>();
        rxBleClient = rxBleClient.create(this);

        layout = (RelativeLayout) findViewById(R.id.activity_main);
        listView = (ListView) findViewById(R.id.list);
        scanButton = (Button) findViewById(R.id.btn_scan);

        scanButton.setText("START SCAN");

        adapter = new DeviceAdapter(this, devices);
        listView.setAdapter(adapter);

        scanButton.setOnClickListener(
                v -> {
                    if (scanSubscription == null) {
                        scanButton.setText("STOP SCAN");
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
                    } else {
                        scanButton.setText("START SCAN");
                        scanSubscription.unsubscribe();
                        scanSubscription = null;
                    }
                }
        );
    }
}
