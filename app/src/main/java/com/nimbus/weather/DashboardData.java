package com.nimbus.weather;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DashboardData {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    interface Listener {void onReady(Snapshot s);}
    static final class Snapshot {
        JSONObject weather,transit; List<CalendarReader.Event> events; List<String> bring;
        long updated; boolean error;
    }
    static Snapshot cached(Context c){
        Snapshot s=new Snapshot();SharedPreferences p=Prefs.get(c);
        try{s.weather=new JSONObject(p.getString("weather_cache","{}"));}catch(Exception ignored){}
        try{s.transit=new JSONObject(p.getString("transit_cache","{}"));}catch(Exception ignored){}
        s.events=CalendarReader.today(c);s.bring=DayPlanner.suggestions(c,s.weather,s.events);
        s.updated=p.getLong("updated",0);
        return s;
    }
    static void refresh(Context context,Listener listener){
        Context c=context.getApplicationContext();
        WORK.execute(()->{
            SharedPreferences p=Prefs.get(c);boolean error=false;
            try{
                JSONObject w=WeatherApi.forecast(Prefs.lat(c),Prefs.lon(c),p.getBoolean("fahrenheit",false));
                p.edit().putString("weather_cache",w.toString()).apply();
            }catch(Exception ex){error=true;}
            String stop=p.getString("stop","").trim();
            if(!stop.isEmpty()){
                try{JSONObject transit=TransitApi.departures(stop);p.edit().putString("transit_cache",transit.toString()).apply();}
                catch(Exception ex){error=true;p.edit().remove("transit_cache").apply();}
            }else p.edit().remove("transit_cache").apply();
            p.edit().putLong("updated",System.currentTimeMillis()).apply();
            Snapshot s=cached(c);s.error=error;
            if(listener!=null)listener.onReady(s);
        });
    }
    static String time(Context c,long millis){return new SimpleDateFormat("HH:mm",Locale.forLanguageTag(L10n.lang(c))).format(new Date(millis));}
    static String sun(JSONArrayCompat array) {return array.value;}
    static String sunTime(JSONObject weather,String key){
        if(weather==null)return "--:--";
        JSONObject daily=weather.optJSONObject("daily");
        String value=daily==null?"":daily.optJSONArray(key)==null?"":daily.optJSONArray(key).optString(0,"");
        return value.length()>=16?value.substring(11,16):"--:--";
    }
    static String degree(double value){return Double.isNaN(value)?"--°":Math.round(value)+"°";}
    static String weatherDescription(Context c,JSONObject weather){
        if(weather==null)return "—";
        JSONObject current=weather.optJSONObject("current");if(current==null)return "—";
        return L10n.cond(c,current.optInt("weather_code",-1));
    }
    static String daySummary(Context c,Snapshot s){
        if(s==null||s.weather==null)return "—";
        JSONObject current=s.weather.optJSONObject("current");JSONObject daily=s.weather.optJSONObject("daily");
        if(current==null||daily==null)return "—";
        double temp=current.optDouble("temperature_2m",Double.NaN);
        org.json.JSONArray max=daily.optJSONArray("temperature_2m_max"),min=daily.optJSONArray("temperature_2m_min");
        return degree(temp)+" · "+weatherDescription(c,s.weather)+" · "+L10n.t(c,"high")+" "+
           (max==null?"--°":degree(max.optDouble(0,Double.NaN)))+" / "+L10n.t(c,"low")+" "+
           (min==null?"--°":degree(min.optDouble(0,Double.NaN)));
    }
    // Internal static class avoided Android dependencies in callers.
    private static final class JSONArrayCompat {String value;}
}
