package ca.uqac.liara.imurecording.Utils;

import android.content.SharedPreferences;

/**
 * Created by FlorentinTh on 10/19/2016.
 */

public abstract class AndroidUtils {

    public static Object readFromSharedPreferences(SharedPreferences sharedPreferences, String key, Class returnClass) throws Exception {
        if (returnClass.equals(Integer.class)) {
            Integer result = sharedPreferences.getInt(key, -1);
            return result;
        } else if (returnClass.equals(Float.class)) {
            Float result = sharedPreferences.getFloat(key, -1);
            return result;
        } else if (returnClass.equals(Boolean.class)) {
            Boolean result = sharedPreferences.getBoolean(key, false);
            return result;
        } else if (returnClass.equals(Long.class)) {
            Long result = sharedPreferences.getLong(key, -1);
            return result;
        } else if (returnClass.equals(String.class)) {
            String result = sharedPreferences.getString(key, null);
            return result;
        } else {
            throw new Exception(returnClass + "is incompatible with shared preference types");
        }
    }

    public static void writeSharedPreferences(SharedPreferences sharedPreferences, String key, Object value) throws Exception {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (value.getClass().equals(Integer.class)) {
            editor.putInt(key, (Integer) value);
            editor.commit();
        } else if (value.getClass().equals(Float.class)) {
            editor.putFloat(key, (Float) value);
            editor.commit();
        } else if (value.getClass().equals(Boolean.class)) {
            editor.putBoolean(key, (Boolean) value);
            editor.commit();
        } else if (value.getClass().equals(Long.class)) {
            editor.putLong(key, (Long) value);
            editor.commit();
        } else if (value.getClass().equals(String.class)) {
            editor.putString(key, (String) value);
            editor.commit();
        } else {
            throw new Exception(value.getClass() + "is incompatible with shared preference types");
        }
    }
}
