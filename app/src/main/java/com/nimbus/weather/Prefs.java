package com.nimbus.weather;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static SharedPreferences get(Context c) {return c.getApplicationContext().getSharedPreferences("nimbus_settings",Context.MODE_PRIVATE);}
    static double lat(Context c) {return Double.longBitsToDouble(get(c).getLong("lat",Double.doubleToRawLongBits(52.52)));}
    static double lon(Context c) {return Double.longBitsToDouble(get(c).getLong("lon",Double.doubleToRawLongBits(13.405)));}
    static String city(Context c) {return get(c).getString("city","Berlin");}
}
