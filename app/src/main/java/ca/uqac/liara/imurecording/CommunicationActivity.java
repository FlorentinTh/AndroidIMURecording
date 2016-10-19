package ca.uqac.liara.imurecording;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import ca.uqac.liara.imurecording.Utils.AndroidUtils;

public class CommunicationActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private FloatingActionButton sendButton;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        sharedPreferences = this.getSharedPreferences(getResources().getString(R.string.shared_preference_file), Context.MODE_PRIVATE);

        initGUI();
        initListeners();
    }

    private void initGUI() {
        sendButton = (FloatingActionButton) findViewById(R.id.btn_send);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

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
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
        );
    }
}
