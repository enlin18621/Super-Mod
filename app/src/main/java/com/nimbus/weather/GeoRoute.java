package com.nimbus.weather;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Location and VBB route estimates. Location is optional and never committed to GitHub. */
final class GeoRoute {
    private static final ExecutorService GEO=Executors.newSingleThreadExecutor();
    static final long MAX_LOCATION_AGE=3*60*60*1000L;
    interface Callback {void onLocation(boolean found);}
    static boolean permitted(Context c){
        return c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
               c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    static Location remembered(Context c) {
        SharedPreferences p=Prefs.get(c);
        long at=p.getLong("gps_at",0);
        if(at==0||System.currentTimeMillis()-at>MAX_LOCATION_AGE)return null;
        Location loc=new Location("cached");
        loc.setLatitude(Double.longBitsToDouble(p.getLong("gps_lat",0)));
        loc.setLongitude(Double.longBitsToDouble(p.getLong("gps_lon",0)));
        loc.setTime(at);return loc;
    }
    static void remember(Context c,Location l){
        if(l==null||l.getLatitude()<-90||l.getLatitude()>90||l.getLongitude()<-180||l.getLongitude()>180)return;
        Prefs.get(c).edit().putLong("gps_lat",Double.doubleToRawLongBits(l.getLatitude()))
            .putLong("gps_lon",Double.doubleToRawLongBits(l.getLongitude()))
            .putLong("gps_at",System.currentTimeMillis()).apply();
    }
    static void tryLastLocation(Context c) {
        if(!permitted(c)||!Prefs.get(c).getBoolean("use_gps",false))return;
        try{
            LocationManager lm=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
            Location best=null;
            for(String provider:lm.getProviders(true)){
                Location sample=lm.getLastKnownLocation(provider);
                if(sample!=null&&System.currentTimeMillis()-sample.getTime()<MAX_LOCATION_AGE &&
                   (best==null||sample.getTime()>best.getTime()))best=sample;
            }
            if(best!=null)remember(c,best);
        }catch(SecurityException|RuntimeException ignored){}
    }
    static void requestCurrent(Context c,Callback callback){
        if(!permitted(c)||!Prefs.get(c).getBoolean("use_gps",false)){callback.onLocation(false);return;}
        try{
            LocationManager lm=(LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
            String provider=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?
                LocationManager.NETWORK_PROVIDER:
                (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)?LocationManager.GPS_PROVIDER:null);
            if(provider==null){tryLastLocation(c);callback.onLocation(remembered(c)!=null);return;}
            if(Build.VERSION.SDK_INT>=30){
                lm.getCurrentLocation(provider,null,c.getMainExecutor(),loc->{
                    if(loc!=null)remember(c,loc);else tryLastLocation(c);
                    callback.onLocation(remembered(c)!=null);
                });
            }else{
                lm.requestSingleUpdate(provider,new LocationListener(){
                    @Override public void onLocationChanged(Location loc){remember(c,loc);callback.onLocation(remembered(c)!=null);}
                    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
                    @Override public void onProviderEnabled(String provider){}
                    @Override public void onProviderDisabled(String provider){}
                },android.os.Looper.getMainLooper());
            }
        }catch(SecurityException|RuntimeException e){tryLastLocation(c);callback.onLocation(remembered(c)!=null);}
    }
    private static JSONObject findAddress(String text)throws Exception{
        JSONArray arr=WeatherApi.getJsonArray("https://v6.vbb.transport.rest/locations?query="+Uri.encode(text)+"&results=3&poi=true&addresses=true&stops=true");
        for(int i=0;i<arr.length();i++){
            JSONObject found=arr.optJSONObject(i);
            if(found!=null&&found.optJSONObject("location")!=null)return found;
        }
        throw new Exception("Destination not found in VBB");
    }
    static JSONObject route(Context c,List<CalendarReader.Event> events)throws Exception {
        SharedPreferences p=Prefs.get(c);
        boolean gps=p.getBoolean("use_gps",false);
        Location origin=gps?remembered(c):null;
        String home=p.getString("home","").trim();
        if(origin==null&&home.isEmpty())return null;
        long now=System.currentTimeMillis();
        CalendarReader.Event next=null;
        for(CalendarReader.Event e:events){
            if(!e.allDay && e.start>=now-10*60*1000L && e.start<now+16*60*60*1000L){
                if(e.location!=null&&!e.location.trim().isEmpty() ||
                    e.title!=null&&e.title.toLowerCase(Locale.ROOT).matches(".*(uni|jura|vorlesung|lecture|seminar|campus|university|大學|大学).*")){
                    next=e;break;
                }
            }
        }
        String destination="";
        if(next!=null){
            destination=next.location==null?"":next.location.trim();
            if(destination.isEmpty())destination=p.getString("uni","");
        } else destination=p.getString("uni","").trim();
        if(destination.isEmpty())return null;
        String url="https://v6.vbb.transport.rest/journeys?";
        if(origin!=null){
            url+="from.latitude="+origin.getLatitude()+"&from.longitude="+origin.getLongitude()+"&from.address="+Uri.encode("Aktueller Standort");
        }else{
            JSONObject from=findAddress(home);
            JSONObject loc=from.optJSONObject("location");
            url+="from.latitude="+loc.getDouble("latitude")+"&from.longitude="+loc.getDouble("longitude")+"&from.address="+Uri.encode(home);
        }
        JSONObject to=findAddress(destination);
        JSONObject coords=to.getJSONObject("location");
        url+="&to.latitude="+coords.getDouble("latitude")+"&to.longitude="+coords.getDouble("longitude")+"&to.address="+Uri.encode(destination);
        url+="&results=5&language=en&stopovers=false&polylines=false";
        if(next!=null){
            long deadline=next.start-10*60*1000L;
            url+="&arrival="+URLEncoder.encode(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                java.time.Instant.ofEpochMilli(deadline).atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime()),"UTF-8");
        }
        JSONObject results=WeatherApi.getJson(url);
        JSONArray journeys=results.optJSONArray("journeys");
        if(journeys==null||journeys.length()==0)return null;
        JSONObject chosen=null;long departure=0,arrival=0;
        for(int i=0;i<journeys.length();i++){
            JSONObject journey=journeys.optJSONObject(i);if(journey==null)continue;
            JSONArray legs=journey.optJSONArray("legs");if(legs==null||legs.length()==0)continue;
            JSONObject first=legs.optJSONObject(0),last=legs.optJSONObject(legs.length()-1);
            if(first==null||last==null)continue;
            long from=parseTime(first.optString("departure",first.optString("plannedDeparture","")));
            long toAt=parseTime(last.optString("arrival",last.optString("plannedArrival","")));
            if(from==0||toAt==0)continue;
            if(next!=null){
                if(toAt>next.start-9*60*1000L||from<now-4*60*1000L)continue;
                if(chosen==null||from>departure){chosen=journey;departure=from;arrival=toAt;}
            }else if(chosen==null||from<departure){chosen=journey;departure=from;arrival=toAt;}
        }
        if(chosen==null)return null;
        JSONObject result=new JSONObject();
        result.put("destination",destination);result.put("event",next==null?"":next.title);
        result.put("eventStart",next==null?0:next.start);
        result.put("departure",departure);
        result.put("arrival",arrival);
        result.put("leaveBy",departure-5*60*1000L); // 5 minute margin
        result.put("journey",chosen);
        result.put("checkedAt",System.currentTimeMillis());
        result.put("gpsOrigin",origin!=null);
        return result;
    }
    static long parseTime(String iso) {
        try{return OffsetDateTime.parse(iso).toInstant().toEpochMilli();}
        catch(Exception ex){return 0;}
    }
    static String shortRoute(Context c,JSONObject route){
        if(route==null)return L10n.t(c,"noRoute");
        long start=route.optLong("leaveBy");
        if(route.optLong("eventStart",0)==0)
            return L10n.t(c,"routeDuration")+": "+Math.max(0,(route.optLong("arrival")-route.optLong("departure"))/60000)+" min";
        if(start<=System.currentTimeMillis()+3*60*1000L)return L10n.t(c,"leaveNow");
        return L10n.t(c,"leaveAt")+" "+DashboardData.time(c,start);
    }
}
