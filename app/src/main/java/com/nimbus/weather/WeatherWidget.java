package com.nimbus.weather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Locale;

public class WeatherWidget extends AppWidgetProvider {
    static final String ACTION_REFRESH="com.nimbus.weather.REFRESH";
    static void updateAll(Context c){updateAll(c,null);}
    static void updateAll(Context c,PendingResult pending){
        Context app=c.getApplicationContext();
        int[] ids=AppWidgetManager.getInstance(app).getAppWidgetIds(new ComponentName(app,WeatherWidget.class));
        if(ids.length==0){if(pending!=null)pending.finish();return;}
        try{
            render(app,DashboardData.cached(app));
            if(GitHubSync.enabled(app)){
                GitHubSync.pull(app,(ok,message)->refreshWidget(app,pending));
            }else refreshWidget(app,pending);
        }catch(Exception e){if(pending!=null)pending.finish();}
    }
    private static void refreshWidget(Context app,PendingResult pending){
        DashboardData.refresh(app,s->{
            try{
                render(app,s);
                ProactiveAlerts.evaluate(app,s);
                GitHubSync.push(app,s);
                AiBridge.maybeAutomatic(app,s,()->render(app,DashboardData.cached(app)));
            }finally{if(pending!=null)pending.finish();}
        });
    }
    static void render(Context c,DashboardData.Snapshot snapshot){
        Context app=c.getApplicationContext();
        AppWidgetManager manager=AppWidgetManager.getInstance(app);
        int[] ids=manager.getAppWidgetIds(new ComponentName(app,WeatherWidget.class));
        if(ids.length==0)return;
        RemoteViews v=new RemoteViews(app.getPackageName(),R.layout.widget_weather);
        v.setTextViewText(R.id.city,Prefs.get(app).getBoolean("weather_gps",false)&&GeoRoute.remembered(app)!=null?
            L10n.t(app,"gpsWeather").toUpperCase(Locale.ROOT):Prefs.city(app).toUpperCase(Locale.ROOT));
        JSONObject current=snapshot.weather==null?null:snapshot.weather.optJSONObject("current");
        JSONObject daily=snapshot.weather==null?null:snapshot.weather.optJSONObject("daily");
        String unit=Prefs.get(app).getBoolean("fahrenheit",false)?"°F":"°C";
        if(current!=null){
            v.setTextViewText(R.id.temp,DashboardData.degree(current.optDouble("temperature_2m",Double.NaN)));
            v.setTextViewText(R.id.condition,L10n.cond(app,current.optInt("weather_code",-1)));
            v.setTextViewText(R.id.feels,L10n.t(app,"feels")+": "+DashboardData.degree(current.optDouble("apparent_temperature",Double.NaN)));
        }else {
            v.setTextViewText(R.id.temp,"--°");v.setTextViewText(R.id.condition,"—");
            v.setTextViewText(R.id.feels,L10n.t(app,"refresh"));
        }
        if(daily!=null){
            JSONArray hi=daily.optJSONArray("temperature_2m_max"),lo=daily.optJSONArray("temperature_2m_min"),rain=daily.optJSONArray("precipitation_probability_max");
            v.setTextViewText(R.id.details,L10n.t(app,"high")+" "+(hi==null?"--°":DashboardData.degree(hi.optDouble(0,Double.NaN)))+
                "  ·  "+L10n.t(app,"low")+" "+(lo==null?"--°":DashboardData.degree(lo.optDouble(0,Double.NaN)))+
                "  ·  ☂ "+(rain==null?"--":rain.optInt(0))+"%");
            v.setTextViewText(R.id.sun,"↑ "+DashboardData.sunTime(snapshot.weather,"sunrise")+"     ↓ "+DashboardData.sunTime(snapshot.weather,"sunset"));
        }else{v.setTextViewText(R.id.details,"—");v.setTextViewText(R.id.sun,"—");}
        String upcoming=L10n.t(app,"calempty");
        long now=System.currentTimeMillis()-15*60*1000L;
        for(CalendarReader.Event e:snapshot.events){if(e.start>=now||e.allDay){upcoming=(e.allDay?L10n.t(app,"allday"):DashboardData.time(app,e.start))+" · "+e.title;break;}}
        if(!CalendarReader.allowed(app))upcoming=L10n.t(app,"calpermission");
        v.setTextViewText(R.id.nextEvent,"◷  "+upcoming);
        String departure=L10n.t(app,"stopmissing");
        JSONObject transit=snapshot.transit;
        if(transit!=null){
            JSONArray arr=transit.optJSONArray("departures");
            if(arr!=null&&arr.length()>0){
                JSONObject first=arr.optJSONObject(0);
                if(first!=null){
                    JSONObject line=first.optJSONObject("line");
                    String when=first.optString("when",first.optString("plannedWhen",""));
                    departure=(line==null?"":line.optString("name")+"  ")+
                      (when.length()>15?when.substring(11,16):"—")+"  ·  "+first.optString("direction","");
                }
            }
        }
        v.setTextViewText(R.id.departure,"↗  "+departure);
        v.setTextViewText(R.id.route,snapshot.route==null?L10n.t(app,"noRoute"):
            GeoRoute.shortRoute(app,snapshot.route)+" · "+snapshot.route.optString("destination",""));
        String ai=Prefs.get(app).getString("ai_advice","");
        long aiAt=Prefs.get(app).getLong("ai_updated",0);
        v.setTextViewText(R.id.ai,
            !ai.isEmpty()&&System.currentTimeMillis()-aiAt<12*60*60*1000L?"✧  "+ai:L10n.t(app,"brief"));
        v.setTextViewText(R.id.bring,"✓  "+(snapshot.bring.isEmpty()?"—":snapshot.bring.get(snapshot.bring.size()>1?1:0)));
        v.setTextViewText(R.id.updated,(snapshot.updated>0?L10n.t(app,"updated")+" "+DashboardData.time(app,snapshot.updated)+" · ":"")+"Open-Meteo · VBB");
        Intent openApp=new Intent(app,MainActivity.class);
        v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(app,61,openApp,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.edit,PendingIntent.getActivity(app,62,openApp,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        Intent refresh=new Intent(app,WeatherWidget.class).setAction(ACTION_REFRESH);
        v.setOnClickPendingIntent(R.id.refresh,PendingIntent.getBroadcast(app,63,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        Intent yr=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.yr.no/en/search?q="+Uri.encode(Prefs.city(app))));
        v.setOnClickPendingIntent(R.id.yr,PendingIntent.getActivity(app,64,yr,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(ids,v);
    }
    @Override public void onReceive(Context c,Intent intent){
        String action=intent.getAction();
        if(ACTION_REFRESH.equals(action)||AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action))
            updateAll(c,goAsync());
        else super.onReceive(c,intent);
    }
}
