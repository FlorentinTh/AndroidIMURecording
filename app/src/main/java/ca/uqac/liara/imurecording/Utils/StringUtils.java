package ca.uqac.liara.imurecording.Utils;

/**
 * Created by FlorentinTh on 10/14/2016.
 */

public abstract class StringUtils {

    public static String getConnectionStateDescription(String input) {
        return input.substring(input.indexOf("{") + 1, input.indexOf("}"));
    }
}
