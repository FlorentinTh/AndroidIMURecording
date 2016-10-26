package ca.uqac.liara.imurecording.Utils;

/**
 * Created by FlorentinTh on 10/25/2016.
 */

public abstract class ByteArrayUtils {

    public static byte[] booleanToByteArray(boolean b) {
        return new byte[]{(byte) (b ? 1 : 0)};
    }

    public static boolean byteArrayToBoolean(byte[] bytes) {
        return bytes[0] != 0;
    }

    public static byte[] stringToByteArray(String s) {
        return s.getBytes();
    }

    public static String byteArrayToString(byte[] bytes) {
        return new String(bytes);
    }

    public static byte[] integerToByteArray(int i) {
        return new byte[] {
                (byte) (i >>> 24),
                (byte) (i >>> 16),
                (byte) (i >>> 8),
                (byte) i
        };
    }

    public static int byteArrayToInteger(byte[] bytes) {
        return  (bytes[0] << 24) +
                ((bytes[1] & 0xFF) << 16) +
                ((bytes[2] & 0xFF) << 8) +
                (bytes[3] & 0xFF);
    }
}
