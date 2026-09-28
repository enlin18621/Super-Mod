package com.nimbus.weather;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class ProactiveAlerts {
    private static final String CHANNEL="nimbus_day_alerts";
    private static final int GROUP=930;
    static void evaluate(Context c,DashboardData.Snapshot s){
        if(!Prefs.get(c).getBoolean("auto_alerts",false))return;
        JSONObject route=s.route;
        if(route!=null&&route.optLong("eventStart")>System.currentTimeMillis()){
            long until=route.optLong("leaveBy")-System.currentTimeMillis();
            if(until<=20*60*1000L&&until>=-8*60*1000L){
                String event=route.optString("event","");
                String key="leave:"+route.optLong("eventStart")+":"+route.optLong("departure");
                notifyOnce(c,key,L10n.t(c,"leaveAlert"),GeoRoute.shortRoute(c,route)+" · "+event);
            }
        }
        if(s.weather!=null){
            JSONObject daily=s.weather.optJSONObject("daily");
            JSONArray rain=daily==null?null:daily.optJSONArray("precipitation_probability_max");
            String today=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
            if(rain!=null&&rain.optInt(0)>=75)
                notifyOnce(c,"rain:"+today,L10n.t(c,"rainAlert"),L10n.t(c,"bringRain")+" · "+rain.optInt(0)+"%");
        }
    }
    static void aiUpdate(Context c,String advice){
        if(!Prefs.get(c).getBoolean("auto_alerts",false) || advice==null||advice.isEmpty())return;
        String date=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
        notifyOnce(c,"ai:"+date+":"+advice.hashCode(),L10n.t(c,"aiAlert"),advice);
    }
    private static void notifyOnce(Context c,String key,String title,String body){
        String k="alert_"+Integer.toHexString(key.hashCode());
        if(Prefs.get(c).getBoolean(k,false))return;
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(CHANNEL,"Nimbus daily advice",NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Departure, weather and opt-in AI advice");manager.createNotificationChannel(channel);
        }
        PendingIntent open=PendingIntent.getActivity(c,951,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
        Notification n=builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title)
            .setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).build();
        manager.notify(GROUP+(key.hashCode()&0x7fff),n);
        Prefs.get(c).edit().putBoolean(k,true).apply();
    }
}
