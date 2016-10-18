package ca.uqac.liara.imurecording.Utils;

import android.graphics.Color;
import android.widget.TextView;

/**
 * Created by FlorentinTh on 10/14/2016.
 */

public abstract class StringUtils {

    public static String getConnectionStateDescription(String input) {
        return input.substring(input.indexOf("{") + 1, input.indexOf("}"));
    }

    public static void setConnectionStateValue(TextView textView, String value) {
        textView.setText(value);
        switch (value) {
            case "DISCONNECTED" :
                textView.setTextColor(Color.RED);
                break;
            case "DISCONNECTING":
                textView.setTextColor(Color.RED);
                break;
            case "CONNECTED" :
                textView.setTextColor(Color.GREEN);
                break;
            case "CONNECTING":
                textView.setTextColor(Color.GREEN);
                break;
        }
    }
}
