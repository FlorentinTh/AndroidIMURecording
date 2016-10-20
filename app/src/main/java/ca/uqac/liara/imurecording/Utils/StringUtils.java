package ca.uqac.liara.imurecording.Utils;

/**
 * Created by FlorentinTh on 10/14/2016.
 */

public abstract class StringUtils {

    private final static char[] stringArray = "0123456789ABCDEF".toCharArray();

    public static byte[] StringToBytes(String input) {
        int length = input.length();
        byte[] data = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(input.charAt(i), 16) << 4) +
                    Character.digit(input.charAt(i + 1), 16));
        }

        return data;
    }

    public static String BytesToString(byte[] input) {
        char[] stringChars = new char[input.length * 2];

        for (int i = 0; i < input.length; i++) {
            int val = input[i] & 0xFF;
            stringChars[i * 2] = stringArray[val >> 4];
            stringChars[i * 2 + 1] = stringArray[val & 0x0F];
        }

        return new String(stringChars);
    }
}
