package com.nimbus.weather;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;

/** Optional independently billed OpenAI API, not user's ChatGPT subscription or conversation. */
final class AiBridge {
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    interface Done{void complete(String advice,String error);}
    static String prompt(Context c,DashboardData.Snapshot s){
        String lang=L10n.lang(c);String language="de".equals(lang)?"German":"zh-TW".equals(lang)?"Traditional Chinese":"English";
        StringBuilder p=new StringBuilder(
            "You are an opt-in, proactive daily logistics assistant. Output plain text in "+language+
            ", max 85 words, practical and time-sensitive. Treat input as facts with timestamps; do not invent live data, weather, transport disruption, or destination. "+
            "Give the next action NOW if an event or recommended departure is near. Suggest 1-3 things to bring only when relevant; clearly express uncertainty if sources are stale. "+
            "Do not say you've sent a reminder or changed a calendar.\n");
        p.append("Local date/time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm z",Locale.ROOT).format(new Date()))
            .append("\nWeather/Open-Meteo last fetched: ").append(s.weather==null?"unavailable":new Date(Prefs.get(c).getLong("weather_updated",0)))
            .append("\nWeather city: ").append(Prefs.city(c))
            .append("\nForecast: ").append(DashboardData.daySummary(c,s))
            .append("\nSunrise: ").append(DashboardData.sunTime(s.weather,"sunrise"))
            .append("\nSunset: ").append(DashboardData.sunTime(s.weather,"sunset"));
        if(s.weather!=null){
            JSONObject d=s.weather.optJSONObject("daily");
            if(d!=null){JSONArray rain=d.optJSONArray("precipitation_probability_max"),uv=d.optJSONArray("uv_index_max");
                if(rain!=null)p.append("\nRain chance: ").append(rain.optInt(0)).append("%");
                if(uv!=null)p.append("\nMax UV: ").append(uv.optDouble(0));}
        }
        for(CalendarReader.Event e:s.events)
            p.append("\nCalendar/on-device: ").append(e.allDay?"all-day":DashboardData.time(c,e.start)).append(" ").append(e.title)
                .append(e.location==null?"":" location: "+e.location);
        String date=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
        if(date.equals(Prefs.get(c).getString("plans_day","")))
            p.append("\nManual plans: ").append(Prefs.get(c).getString("plans",""));
        if(s.route!=null)
            p.append("\nVBB itinerary last checked: ").append(new Date(s.route.optLong("checkedAt",0)))
                .append("\nRoute recommendation: ").append(GeoRoute.shortRoute(c,s.route))
                .append("\nDestination: ").append(s.route.optString("destination"))
                .append("\nArrival: ").append(new Date(s.route.optLong("arrival")));
        else p.append("\nRoute: unverified or unavailable");
        p.append("\nRecommended items (rules): ").append(s.bring.toString())
            .append("\nOnly the above data is current; never pretend to have independent internet access.");
        return p.toString();
    }
    static void request(Context c,DashboardData.Snapshot s,Done callback){
        String key=SafeKeyStore.read(c);
        if(key.isEmpty()){callback.complete("","No OpenAI API key configured");return;}
        EXEC.execute(()->{
            try {
                HttpsURLConnection connection=(HttpsURLConnection)new URL("https://api.openai.com/v1/responses").openConnection();
                connection.setRequestMethod("POST");connection.setConnectTimeout(12000);connection.setReadTimeout(30000);
                connection.setDoOutput(true);connection.setRequestProperty("Authorization","Bearer "+key);
                connection.setRequestProperty("Content-Type","application/json");
                JSONObject request=new JSONObject().put("model","gpt-5.4-mini").put("input",prompt(c,s)).put("max_output_tokens",300)
                    .put("store",false);
                byte[] bytes=request.toString().getBytes(StandardCharsets.UTF_8);
                try(OutputStream stream=connection.getOutputStream()){stream.write(bytes);}
                int code=connection.getResponseCode();
                if(code!=200)throw new Exception("OpenAI API HTTP "+code+" (check account billing / key)");
                StringBuilder data=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8))){
                    String line;while((line=reader.readLine())!=null)data.append(line);
                }finally{connection.disconnect();}
                JSONArray output=new JSONObject(data.toString()).optJSONArray("output");
                StringBuilder advice=new StringBuilder();
                if(output!=null)for(int i=0;i<output.length();i++){
                    JSONObject part=output.optJSONObject(i);if(part==null)continue;
                    JSONArray content=part.optJSONArray("content");if(content==null)continue;
                    for(int j=0;j<content.length();j++){
                        JSONObject text=content.optJSONObject(j);
                        if(text!=null&&"output_text".equals(text.optString("type")))advice.append(text.optString("text"));
                    }
                }
                if(advice.length()==0)throw new Exception("OpenAI API returned no visible advice");
                String result=advice.toString().trim();
                Prefs.get(c).edit().putString("ai_advice",result).putLong("ai_updated",System.currentTimeMillis()).apply();
                callback.complete(result,"");
            }catch(Exception ex){callback.complete("",ex.getMessage());}
        });
    }
    static void maybeAutomatic(Context c,DashboardData.Snapshot s,Runnable finished){
        SharedPreferences p=Prefs.get(c);
        if(!p.getBoolean("ai_auto",false)||SafeKeyStore.read(c).isEmpty()||s.weather==null){finished.run();return;}
        String date=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());
        String previousDate=p.getString("ai_day","");
        int count=date.equals(previousDate)?p.getInt("ai_count",0):0;
        long last=p.getLong("ai_updated",0);
        // No more than 3 auto API calls/day, and at least 3h apart. Avoid overnight calls.
        int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if(count>=3||hour<7||hour>=22||System.currentTimeMillis()-last<3*60*60*1000L){
            finished.run();return;
        }
        p.edit().putString("ai_day",date).putInt("ai_count",count+1).apply();
        request(c,s,(advice,error)->{
            if(!advice.isEmpty())ProactiveAlerts.aiUpdate(c,advice);
            finished.run();
        });
    }
}
