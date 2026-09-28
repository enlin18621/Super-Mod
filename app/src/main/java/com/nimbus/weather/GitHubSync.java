package com.nimbus.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OPTIONAL two-way state bridge through a dedicated PRIVATE GitHub repo.
 * ChatGPT with the user's separately connected GitHub permission can edit
 * nimbus-state.json; this Android app pulls it, and optionally writes
 * nimbus-device.json back.  This does not give ChatGPT autonomous access,
 * scheduled checks or ChatGPT Memory sync by itself.
 */
final class GitHubSync {
    private static final String REMOTE_CONFIG="nimbus-state.json",DEVICE_REPORT="nimbus-device.json";
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    private static final Pattern REPO=Pattern.compile("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+");
    interface Done{void complete(boolean ok,String message);}
    static boolean enabled(Context c){
        SharedPreferences p=Prefs.get(c);
        return p.getBoolean("sync_enabled",false)&&REPO.matcher(p.getString("sync_repo","")).matches()&&
            !GitHubTokenVault.read(c).isEmpty();
    }
    private static String repo(Context c){return Prefs.get(c).getString("sync_repo","");}
    private static String url(Context c,String file){return "https://api.github.com/repos/"+repo(c)+"/contents/"+file;}
    private static HttpURLConnection connect(Context c,String file,String method)throws Exception{
        if(!enabled(c))throw new Exception("Private sync not configured");
        HttpURLConnection conn=(HttpURLConnection)new URL(url(c,file)).openConnection();
        conn.setConnectTimeout(11000);conn.setReadTimeout(14000);conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization","Bearer "+GitHubTokenVault.read(c));
        conn.setRequestProperty("Accept","application/vnd.github+json");
        conn.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        conn.setRequestProperty("User-Agent","NimbusPersonalSync/1.0 Android");
        return conn;
    }
    private static String read(HttpURLConnection conn)throws Exception{
        try(BufferedReader input=new BufferedReader(new InputStreamReader(conn.getInputStream(),StandardCharsets.UTF_8))){
            StringBuilder builder=new StringBuilder();String line;
            while((line=input.readLine())!=null){
                builder.append(line);
                if(builder.length()>180000)throw new Exception("Remote file too large");
            }
            return builder.toString();
        }
    }
    private static JSONObject remoteFile(Context c,String file)throws Exception{
        HttpURLConnection conn=connect(c,file,"GET");
        try{
            int code=conn.getResponseCode();
            if(code==404)return null;
            if(code!=200)throw new Exception("GitHub HTTP "+code);
            return new JSONObject(read(conn));
        }finally{conn.disconnect();}
    }
    private static String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());}
    private static String limit(JSONObject object,String key,int max){
        if(!object.has(key)||object.isNull(key))return null;
        String s=object.optString(key,"");
        return s.substring(0,Math.min(max,s.length()));
    }
    private static boolean apply(Context c,JSONObject remote,String sha)throws Exception{
        SharedPreferences prefs=Prefs.get(c);
        if(sha!=null&&sha.equals(prefs.getString("sync_applied_sha","")))return false;
        SharedPreferences.Editor edit=prefs.edit();
        String city=limit(remote,"city",120);
        if(city!=null&&!city.trim().isEmpty()&&!city.equals(Prefs.city(c))){
            // Geocode on the device; a remote repo can NEVER inject GPS coordinates or arbitrary URLs.
            JSONObject found=WeatherApi.searchCity(city);
            String name=found.getString("name");
            if(!found.optString("country","").isEmpty())name+=", "+found.optString("country");
            edit.putString("city",name).putLong("lat",Double.doubleToRawLongBits(found.getDouble("latitude")))
                .putLong("lon",Double.doubleToRawLongBits(found.getDouble("longitude"))).remove("weather_cache");
        }
        for(String key:new String[]{"home","uni","stop"}){
            String value=limit(remote,key,400);
            if(value!=null)edit.putString(key,value);
        }
        String day=limit(remote,"plans_day",10);
        String plans=limit(remote,"plans",4000);
        if(plans!=null&&day!=null&&day.equals(today()))
            edit.putString("plans",plans).putString("plans_day",day);
        String language=limit(remote,"lang",7);
        if("de".equals(language)||"en".equals(language)||"zh-TW".equals(language))edit.putString("lang",language);
        if(remote.has("fahrenheit"))edit.putBoolean("fahrenheit",remote.optBoolean("fahrenheit",false));
        if(sha!=null)edit.putString("sync_applied_sha",sha);
        edit.putLong("sync_pulled",System.currentTimeMillis()).remove("sync_error").apply();
        return true;
    }
    static void pull(Context c,Done callback){
        if(!enabled(c)){if(callback!=null)callback.complete(false,"Sync not configured");return;}
        Context app=c.getApplicationContext();
        EXEC.execute(()->{
            try{
                JSONObject file=remoteFile(app,REMOTE_CONFIG);
                if(file==null)throw new Exception("Create nimbus-state.json in your PRIVATE sync repository first");
                String encoded=file.optString("content","").replace("\n","");
                byte[] binary=Base64.decode(encoded,Base64.DEFAULT);
                if(binary.length>24000)throw new Exception("Remote configuration too large");
                JSONObject remote=new JSONObject(new String(binary,StandardCharsets.UTF_8));
                if(remote.optInt("version",1)!=1)throw new Exception("Unsupported remote state version");
                boolean changed=apply(app,remote,file.optString("sha",""));
                if(callback!=null)callback.complete(true,changed?"Private settings synchronized":"Already up to date");
            }catch(Exception ex){
                Prefs.get(app).edit().putString("sync_error",ex.getMessage()).apply();
                if(callback!=null)callback.complete(false,ex.getMessage());
            }
        });
    }
    static void push(Context c,DashboardData.Snapshot snapshot){
        if(!enabled(c)||!Prefs.get(c).getBoolean("sync_device_report",false))return;
        Context app=c.getApplicationContext();
        EXEC.execute(()->{
            try{
                SharedPreferences p=Prefs.get(app);
                JSONObject report=new JSONObject().put("version",1).put("observed_at",System.currentTimeMillis())
                    .put("weather_city",Prefs.city(app))
                    .put("weather_checked_at",snapshot.weatherUpdated)
                    .put("transit_checked_at",snapshot.transitUpdated)
                    .put("route_checked_at",snapshot.routeUpdated)
                    .put("calendar_permission",CalendarReader.allowed(app));
                if(p.getBoolean("sync_share_events",false)){
                    JSONArray events=new JSONArray();
                    for(CalendarReader.Event e:snapshot.events)
                        events.put(new JSONObject().put("title",e.title).put("start",e.start)
                            .put("location",p.getBoolean("sync_share_private",false)?e.location:""));
                    report.put("today_events",events);
                }
                if(p.getBoolean("sync_share_private",false)){
                    report.put("home",p.getString("home","")).put("uni",p.getString("uni",""));
                    report.put("plans",today().equals(p.getString("plans_day",""))?p.getString("plans",""):"");
                    android.location.Location loc=GeoRoute.remembered(app);
                    if(loc!=null&&p.getBoolean("sync_share_precise_gps",false))
                        report.put("gps",new JSONObject().put("lat",loc.getLatitude()).put("lon",loc.getLongitude())
                            .put("at",loc.getTime()));
                }
                JSONObject prior=remoteFile(app,DEVICE_REPORT);
                JSONObject body=new JSONObject().put("message","Nimbus device status update")
                    .put("content",Base64.encodeToString(report.toString(2).getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));
                if(prior!=null&&!prior.optString("sha","").isEmpty())body.put("sha",prior.getString("sha"));
                HttpURLConnection conn=connect(app,DEVICE_REPORT,"PUT");
                conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");
                try(OutputStream stream=conn.getOutputStream()){stream.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                int code=conn.getResponseCode();
                if(code!=200&&code!=201)throw new Exception("Device report upload HTTP "+code);
                conn.disconnect();
                p.edit().putLong("sync_pushed",System.currentTimeMillis()).remove("sync_error").apply();
            }catch(Exception ex){Prefs.get(app).edit().putString("sync_error",ex.getMessage()).apply();}
        });
    }
}
