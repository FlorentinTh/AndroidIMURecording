package ca.uqac.liara.imurecording;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.EditText;

import com.github.jorgecastilloprz.FABProgressCircle;

import java.util.HashMap;

import ca.uqac.liara.imurecording.Utils.AndroidUtils;

public class CommunicationActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText hikingNameInput, sensorLocationInput, usernameInput;
    private FABProgressCircle sendButton;

    private SharedPreferences sharedPreferences;

    private HashMap<String, String> data = new HashMap();
    private boolean isStarted = false;

    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        sharedPreferences = this.getSharedPreferences(getResources().getString(R.string.shared_preference_file), Context.MODE_PRIVATE);

        initGUI();
        initListeners();

    }

    private void initGUI() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        hikingNameInput = (EditText) findViewById(R.id.hiking_name_input);
        sensorLocationInput = (EditText) findViewById(R.id.sensor_location_input);
        usernameInput = (EditText)  findViewById(R.id.username_input);
        sendButton = (FABProgressCircle) findViewById(R.id.btn_send);

        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp, null));
        } else {
            toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        }

        try {
            setTitle(String.valueOf(AndroidUtils.readFromSharedPreferences(sharedPreferences, getResources().getString(R.string.device_name_key_value), String.class)));
        } catch (Exception e) {
            Log.e(CommunicationActivity.class.toString(), e.getMessage());
        }
    }

    private void initListeners() {
        toolbar.setNavigationOnClickListener(
                v -> {
                    finish();
                }
        );

        sendButton.setOnClickListener(
                view -> {
                    switch (validateInputs()) {
                        case 0:
                            if (isStarted == false) {
                                sendButton.show();
                                startSendingData();
                            } else {
                                sendButton.hide();
                                stopSendingData();
                            }
                            break;
                        case 1:
                            Snackbar.make(view, getResources().getString(R.string.hiking_name_input_empty), Snackbar.LENGTH_LONG).show();
                            break;
                        case 2:
                            Snackbar.make(view, getResources().getString(R.string.sensor_location_input_empty), Snackbar.LENGTH_LONG).show();
                            break;
                        case 3:
                            Snackbar.make(view, getResources().getString(R.string.username_input_empty), Snackbar.LENGTH_LONG).show();
                            break;
                    }
                }
        );
    }

    private int validateInputs() {
        if (!(hikingNameInput.getText().toString().trim().length() > 0)) {
            return 1;
        }

        if(!(sensorLocationInput.getText().toString().trim().length() > 0)) {
            return 2;
        }

        if (!(usernameInput.getText().toString().trim().length() > 0)) {
            return 3;
        }

        return 0;
    }

    private void startSendingData() {

    }

    private void stopSendingData() {

    }
}
