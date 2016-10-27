package ca.uqac.liara.imurecording.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public static int byteArrayToInteger(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] floatToByteArray(float v) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(v).array();
    }

    public static float byteArrayToFloat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }
}
