package com.nimbus.weather;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DashboardData {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    interface Listener{void onReady(Snapshot snapshot);}
    static final class Snapshot {
        JSONObject weather,transit,route;
        List<CalendarReader.Event> events;
        List<String> bring;
        long updated,weatherUpdated,transitUpdated,routeUpdated;boolean error;
    }
    static Snapshot cached(Context c){
        SharedPreferences p=Prefs.get(c);Snapshot s=new Snapshot();
        try{s.weather=new JSONObject(p.getString("weather_cache","{}"));}catch(Exception ex){}
        try{s.transit=new JSONObject(p.getString("transit_cache","{}"));}catch(Exception ex){}
        try{s.route=new JSONObject(p.getString("route_cache","{}"));if(s.route.length()==0)s.route=null;}catch(Exception ex){}
        s.events=CalendarReader.today(c);
        s.bring=DayPlanner.suggestions(c,s.weather,s.events);
        s.updated=p.getLong("updated",0);
        s.weatherUpdated=p.getLong("weather_updated",0);
        s.transitUpdated=p.getLong("transit_updated",0);
        s.routeUpdated=p.getLong("route_updated",0);
        if(s.route!=null){
            if(s.routeUpdated<System.currentTimeMillis()-12*60*1000L ||
               s.route.optLong("eventStart",0)>0&&s.route.optLong("eventStart",0)<System.currentTimeMillis()-15*60*1000L)
               s.route=null; // never show stale route as real-time advice
        }
        return s;
    }
    static void refresh(Context context,Listener callback) {
        Context c=context.getApplicationContext();
        WORK.execute(()->{
            SharedPreferences p=Prefs.get(c);boolean error=false;
            // Last-known location works without a background-location permission, but can be unavailable/stale.
            GeoRoute.tryLastLocation(c);
            android.location.Location gps=p.getBoolean("use_gps",false)?GeoRoute.remembered(c):null;
            try{
                double lat=gps!=null?gps.getLatitude():Prefs.lat(c),lon=gps!=null?gps.getLongitude():Prefs.lon(c);
                JSONObject w=WeatherApi.forecast(lat,lon,p.getBoolean("fahrenheit",false));
                p.edit().putString("weather_cache",w.toString()).putLong("weather_updated",System.currentTimeMillis())
                    .putBoolean("weather_gps",gps!=null).apply();
            }catch(Exception ex){error=true;}
            String stop=p.getString("stop","").trim();
            try{
                JSONObject transit;
                if(!stop.isEmpty())transit=TransitApi.departures(stop);
                else if(gps!=null)transit=TransitApi.departuresNearest(gps.getLatitude(),gps.getLongitude());
                else transit=null;
                if(transit==null)p.edit().remove("transit_cache").remove("transit_updated").apply();
                else p.edit().putString("transit_cache",transit.toString()).putLong("transit_updated",System.currentTimeMillis()).apply();
            }catch(Exception ex){
                error=true;
                if(System.currentTimeMillis()-p.getLong("transit_updated",0)>3*60*1000L)
                    p.edit().remove("transit_cache").apply();
            }
            List<CalendarReader.Event> events=CalendarReader.today(c);
            try{
                JSONObject route=GeoRoute.route(c,events);
                if(route==null)p.edit().remove("route_cache").remove("route_updated").apply();
                else p.edit().putString("route_cache",route.toString()).putLong("route_updated",System.currentTimeMillis()).apply();
            }catch(Exception ex){
                error=true;p.edit().remove("route_cache").remove("route_updated").apply();
            }
            p.edit().putLong("updated",System.currentTimeMillis()).apply();
            Snapshot s=cached(c);s.error=error;
            if(callback!=null)callback.onReady(s);
        });
    }
    static String time(Context c,long millis){
        return millis<=0?"--:--":new SimpleDateFormat("HH:mm",Locale.forLanguageTag(L10n.lang(c))).format(new Date(millis));
    }
    static String sunTime(JSONObject weather,String key){
        if(weather==null)return "--:--";
        JSONObject daily=weather.optJSONObject("daily");
        JSONArray times=daily==null?null:daily.optJSONArray(key);
        String val=times==null?"":times.optString(0,"");
        return val.length()>=16?val.substring(11,16):"--:--";
    }
    static String degree(double n){return Double.isNaN(n)?"--°":Math.round(n)+"°";}
    static String weatherDescription(Context c,JSONObject weather){
        JSONObject current=weather==null?null:weather.optJSONObject("current");
        return current==null?"—":L10n.cond(c,current.optInt("weather_code",-1));
    }
    static String daySummary(Context c,Snapshot s){
        JSONObject current=s.weather==null?null:s.weather.optJSONObject("current");
        JSONObject daily=s.weather==null?null:s.weather.optJSONObject("daily");
        if(current==null||daily==null)return "—";
        JSONArray hi=daily.optJSONArray("temperature_2m_max"),lo=daily.optJSONArray("temperature_2m_min");
        return degree(current.optDouble("temperature_2m",Double.NaN))+" · "+weatherDescription(c,s.weather)+
            " · "+L10n.t(c,"high")+" "+(hi==null?"--°":degree(hi.optDouble(0,Double.NaN)))+
            " / "+L10n.t(c,"low")+" "+(lo==null?"--°":degree(lo.optDouble(0,Double.NaN)));
    }
}
