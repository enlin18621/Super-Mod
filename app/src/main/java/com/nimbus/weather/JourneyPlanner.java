package com.nimbus.weather;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

final class JourneyPlanner {
    static final class Route {
        String from="",to="",summary="",error="";
        long leaveMs=0,arriveMs=0,departureMs=0;
        int durationMinutes=0,walkingMinutes=0;
        String leaveClock(){return format(leaveMs);}
        String arriveClock(){return format(arriveMs);}
    }
    static String format(long t) {return t<=0?"--:--":new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(t));}
    static boolean inVbb(double lat,double lon){return lat>=51.2&&lat<=53.9&&lon>=11.2&&lon<=15.8;}
    private static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private static JSONObject lookup(String q)throws Exception{
        JSONArray a=new JSONArray(WeatherApi.getText("https://v6.vbb.transport.rest/locations?query="+enc(q)+"&results=2&language=de"));
        if(a.length()==0)throw new Exception("Ort im VBB nicht gefunden: "+q);
        return a.getJSONObject(0);
    }
    private static String param(String side,JSONObject place,String label)throws Exception{
        String kind=place.optString("type","");
        String id=place.optString("id","");
        if(("stop".equals(kind)||"station".equals(kind))&&!id.isEmpty())return side+"="+enc(id);
        JSONObject point=place.optJSONObject("location");
        if(point==null)point=place;
        double lat=point.optDouble("latitude",Double.NaN),lon=point.optDouble("longitude",Double.NaN);
        if(!Double.isFinite(lat)||!Double.isFinite(lon))throw new Exception("Keine Koordinaten für "+label);
        return side+".latitude="+lat+"&"+side+".longitude="+lon+"&"+side+".address="+enc(label);
    }
    private static long epoch(JSONObject leg,String key,String fallback){
        String value=leg.optString(key,leg.optString(fallback,""));
        if(value.isEmpty()||"null".equals(value))return 0;
        try{return OffsetDateTime.parse(value).toInstant().toEpochMilli();}
        catch(Exception ex){try{return Instant.parse(value).toEpochMilli();}catch(Exception ignored){return 0;}}
    }
    static Route plan(Context context,DayEngine.Event event) {
        Route r=new Route();
        if(event==null||event.startMs<=System.currentTimeMillis()||event.allDay)return r;
        SharedPreferences p=WeatherWidget.prefs(context);
        String destination=event.location.trim();
        String title=event.title.toLowerCase(Locale.ROOT);
        if(destination.isEmpty()&&(title.contains("uni")||title.contains("seminar")||title.contains("vorles")||title.contains("lecture")||title.contains("law")))
            destination=p.getString("uni","").trim();
        if(destination.isEmpty()){r.error="Kein Ziel im Kalendereintrag";return r;}
        try{
            String home=p.getString("home","").trim(),station=p.getString("station","").trim();
            JSONObject origin=null,dest;
            String originLabel="";
            int walk=0;
            if(!home.isEmpty()){
                origin=lookup(home);originLabel=home;
            }else if(!station.isEmpty()){
                origin=lookup(station);originLabel=origin.optString("name",station);
                walk=p.getInt("walk_buffer",7);
            }else if(p.getBoolean("geo_ok",false)){
                double lat=Double.longBitsToDouble(p.getLong("lat",0));
                double lon=Double.longBitsToDouble(p.getLong("lon",0));
                if(!inVbb(lat,lon)){r.error="VBB nur Berlin/Brandenburg";return r;}
                JSONArray nearby=new JSONArray(WeatherApi.getText("https://v6.vbb.transport.rest/locations/nearby?latitude="+lat+"&longitude="+lon+"&results=1&distance=1200&poi=false&language=de"));
                if(nearby.length()==0){r.error="Keine Haltestelle in der Nähe";return r;}
                origin=nearby.getJSONObject(0);originLabel=origin.optString("name","");
                walk=Math.max(3,Math.min(20,(int)Math.ceil(origin.optDouble("distance",500)/75.0)));
            }else {r.error="Startadresse oder Standort erforderlich";return r;}
            dest=lookup(destination);
            r.from=originLabel;r.to=dest.optString("name",destination);
            long arriveTarget=event.startMs-10*60*1000L;
            String iso=DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(arriveTarget));
            String url="https://v6.vbb.transport.rest/journeys?"+param("from",origin,originLabel)+"&"+param("to",dest,destination)+"&arrival="+enc(iso)+"&results=4&language=de";
            JSONArray journeys=WeatherApi.getJson(url).optJSONArray("journeys");
            if(journeys==null||journeys.length()==0){r.error="Keine passende VBB-Verbindung";return r;}
            JSONObject best=null;long bestArrival=0;
            for(int i=0;i<journeys.length();i++){
                JSONObject option=journeys.optJSONObject(i);
                if(option==null)continue;
                JSONArray legs=option.optJSONArray("legs");
                if(legs==null||legs.length()==0)continue;
                long lastArr=epoch(legs.getJSONObject(legs.length()-1),"arrival","plannedArrival");
                if(lastArr>0&&lastArr<=arriveTarget&&lastArr>bestArrival){best=option;bestArrival=lastArr;}
            }
            if(best==null){r.error="Keine Verbindung vor Terminbeginn gefunden";return r;}
            JSONArray legs=best.getJSONArray("legs");
            r.departureMs=epoch(legs.getJSONObject(0),"departure","plannedDeparture");
            r.arriveMs=bestArrival;
            r.leaveMs=r.departureMs-walk*60_000L;
            r.walkingMinutes=walk;
            if(r.leaveMs<=0){r.error="Abfahrtszeit nicht verfügbar";return r;}
            r.durationMinutes=(int)Math.ceil((r.arriveMs-r.leaveMs)/60000.0);
            StringBuilder b=new StringBuilder();
            for(int i=0;i<legs.length();i++){
                JSONObject leg=legs.getJSONObject(i),line=leg.optJSONObject("line");
                String name=line==null?"🚶":line.optString("name","🚶");
                if(b.length()>0)b.append(" › ");
                b.append(name);
            }
            r.summary=b.toString();
        }catch(Exception ex){r.error=ex.getMessage()==null?"VBB derzeit nicht verfügbar":ex.getMessage();}
        return r;
    }
    private JourneyPlanner(){}
}
