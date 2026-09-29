package com.nimbus.weather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CompactWeatherWidget extends AppWidgetProvider {
 private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  if(ids.length==0)return;
  Context app=context.getApplicationContext();
  SharedPreferences p=WeatherWidget.prefs(app);
  WORK.execute(()->{
   RemoteViews r=new RemoteViews(app.getPackageName(),R.layout.widget_weather_compact);
   String lang=p.getString("lang","de");
   boolean locked=p.getBoolean("city_locked",false),known=p.getBoolean("geo_ok",false);
   r.setTextViewText(R.id.compact_city,locked?p.getString("city","Berlin"):UI.t(lang,"yourLocation"));
   if(locked||known){
    try{
     double lat=Double.longBitsToDouble(p.getLong("lat",Double.doubleToRawLongBits(52.52)));
     double lon=Double.longBitsToDouble(p.getLong("lon",Double.doubleToRawLongBits(13.405)));
     JSONObject w=WeatherApi.forecast(lat,lon,p.getBoolean("fahrenheit",false));
     JSONObject cur=w.getJSONObject("current"),day=w.getJSONObject("daily");
     r.setTextViewText(R.id.compact_temp,Math.round(cur.getDouble("temperature_2m"))+"°");
     r.setTextViewText(R.id.compact_condition,WeatherApi.condition(cur.optInt("weather_code",-1),lang));
     r.setTextViewText(R.id.compact_detail,UI.t(lang,"high")+" "+Math.round(day.getJSONArray("temperature_2m_max").getDouble(0))+"°  ·  "+UI.t(lang,"low")+" "+Math.round(day.getJSONArray("temperature_2m_min").getDouble(0))+"°");
    }catch(Exception ex){r.setTextViewText(R.id.compact_condition,UI.t(lang,"error"));}
   }else r.setTextViewText(R.id.compact_condition,UI.t(lang,"locationDenied"));
   r.setOnClickPendingIntent(R.id.compact_root,PendingIntent.getActivity(app,25,new Intent(app,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   manager.updateAppWidget(new ComponentName(app,CompactWeatherWidget.class),r);
  });
 }
}