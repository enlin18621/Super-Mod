package com.nimbus.weather;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Looper;
import java.util.function.Consumer;

final class LocationHelper {
    private LocationHelper() {}
    static boolean permitted(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
          || c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    static boolean isRecent(Context c) {
        long age=System.currentTimeMillis()-WeatherWidget.prefs(c).getLong("location_ms",0);
        return age>=0 && age<3*60*60*1000L;
    }
    static void request(Activity activity, Consumer<Boolean> completed) {
        if (!permitted(activity)) {completed.accept(false);return;}
        LocationManager lm=(LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if(lm==null){completed.accept(false);return;}
        String provider=null;
        try {
            if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))provider=LocationManager.NETWORK_PROVIDER;
            else if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))provider=LocationManager.GPS_PROVIDER;
            else {completed.accept(useLast(lm,activity));return;}
            final String selected=provider;
            Location last=bestLast(lm);
            if(last!=null && System.currentTimeMillis()-last.getTime()<15*60*1000L)store(activity,last);
            if(Build.VERSION.SDK_INT>=30){
                lm.getCurrentLocation(selected,null,activity.getMainExecutor(),loc->{
                    if(loc!=null){store(activity,loc);completed.accept(true);}
                    else completed.accept(useLast(lm,activity));
                });
            }else{
                lm.requestSingleUpdate(selected,new android.location.LocationListener(){
                    @Override public void onLocationChanged(Location loc){store(activity,loc);completed.accept(true);}
                },Looper.getMainLooper());
            }
        }catch(SecurityException|IllegalArgumentException e){completed.accept(false);}
    }
    private static Location bestLast(LocationManager lm) {
        Location result=null;
        for(String p:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER,LocationManager.PASSIVE_PROVIDER}){
            try{Location v=lm.getLastKnownLocation(p);if(v!=null&&(result==null||v.getTime()>result.getTime()))result=v;}
            catch(Exception ignored){}
        }
        return result;
    }
    private static boolean useLast(LocationManager lm,Context context) {
        Location last=bestLast(lm);
        if(last==null || System.currentTimeMillis()-last.getTime()>24*60*60*1000L)return false;
        store(context,last);return true;
    }
    private static void store(Context c,Location l){
        if(!Double.isFinite(l.getLatitude())||!Double.isFinite(l.getLongitude()))return;
        SharedPreferences p=WeatherWidget.prefs(c);
        p.edit().putLong("lat",Double.doubleToRawLongBits(l.getLatitude()))
          .putLong("lon",Double.doubleToRawLongBits(l.getLongitude()))
          .putLong("location_ms",l.getTime()).putBoolean("geo_ok",true).apply();
    }
}
