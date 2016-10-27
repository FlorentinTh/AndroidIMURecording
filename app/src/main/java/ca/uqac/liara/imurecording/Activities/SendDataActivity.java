package ca.uqac.liara.imurecording.Activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RelativeLayout;

import ca.uqac.liara.imurecording.R;

public class SendDataActivity extends AppCompatActivity {

    private RelativeLayout layout;
    private TextInputLayout hikingNameLayout, sensorLocationLayout, usernameLayout;
    private EditText hikingNameInput, sensorLocationInput, usernameInput;
    private FloatingActionButton sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);

        initGUI();
        initListeners();
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

    private void initGUI() {
        layout = (RelativeLayout) findViewById(R.id.activity_send_data);
        hikingNameLayout = (TextInputLayout) findViewById(R.id.hiking_name);
        hikingNameLayout.setErrorEnabled(true);
        hikingNameInput = (EditText) findViewById(R.id.hiking_name_input);
        sensorLocationLayout = (TextInputLayout) findViewById(R.id.sensor_location);
        sensorLocationLayout.setErrorEnabled(true);
        sensorLocationInput = (EditText) findViewById(R.id.sensor_location_input);
        usernameLayout = (TextInputLayout) findViewById(R.id.username);
        usernameLayout.setErrorEnabled(true);
        usernameInput = (EditText) findViewById(R.id.username_input);
        sendButton = (FloatingActionButton) findViewById(R.id.btn_send);

        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initListeners() {
        hikingNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
                        Snackbar.make(layout, "OK", Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(layout, "PAS OK", Snackbar.LENGTH_LONG).show();
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
}
