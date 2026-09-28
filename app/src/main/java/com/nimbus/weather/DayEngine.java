package com.nimbus.weather;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DayEngine {
    static final class Event {
        String title,location,time;
        Event(String t,String l,String d){title=t;location=l;time=d;}
    }
    static final class Snapshot {
        String city="",condition="",temp="--°",feels="",high="",low="",rain="",wind="",uv="",sunrise="--:--",sunset="--:--",transit="",forecastError="",transportError="",updated="";
        int rainPercent=-1; double celsius=15, uvValue=0, windValue=0;
        List<Event> events=new ArrayList<>();
        List<String> pack=new ArrayList<>();
        String lang="de";
        String agendaText() {
            StringBuilder b=new StringBuilder();
            for(Event e:events){if(b.length()>0)b.append("\n");b.append(e.time).append("  ").append(e.title);if(!e.location.isEmpty())b.append(" · ").append(e.location);}
            return b.toString();
        }
        String packText(){return android.text.TextUtils.join(" · ",pack);}
    }
    static Snapshot load(Context c) {
        SharedPreferences p=WeatherWidget.prefs(c);Snapshot s=new Snapshot();s.lang=p.getString("lang","de");
        s.city=p.getString("city","Berlin");
        s.events=calendar(c);
        double lat=Double.longBitsToDouble(p.getLong("lat",Double.doubleToRawLongBits(52.52)));
        double lon=Double.longBitsToDouble(p.getLong("lon",Double.doubleToRawLongBits(13.405)));
        boolean f=p.getBoolean("fahrenheit",false);
        try {
            JSONObject obj=WeatherApi.forecast(lat,lon,f);
            JSONObject cur=obj.getJSONObject("current"),d=obj.getJSONObject("daily");
            String unit=f?"°F":"°C";
            s.temp=Math.round(cur.getDouble("temperature_2m"))+"°";
            s.feels=Math.round(cur.getDouble("apparent_temperature"))+unit;
            s.condition=WeatherApi.condition(cur.optInt("weather_code",-1),s.lang);
            s.high=Math.round(d.getJSONArray("temperature_2m_max").getDouble(0))+unit;
            s.low=Math.round(d.getJSONArray("temperature_2m_min").getDouble(0))+unit;
            s.rainPercent=d.getJSONArray("precipitation_probability_max").optInt(0,-1);
            s.rain=s.rainPercent<0?"--":s.rainPercent+"%";
            s.windValue=cur.optDouble("wind_speed_10m",0);
            s.wind=Math.round(s.windValue)+" km/h";
            s.uvValue=d.getJSONArray("uv_index_max").optDouble(0,0);
            s.uv=String.format(Locale.US,"%.1f",s.uvValue);
            s.sunrise=clock(d.getJSONArray("sunrise").getString(0));
            s.sunset=clock(d.getJSONArray("sunset").getString(0));
            s.celsius=f?(cur.optDouble("temperature_2m",60)-32)*5/9:cur.optDouble("temperature_2m",15);
        } catch(Exception ex){s.forecastError=ex.getMessage()==null?"Unavailable":ex.getMessage();}
        String station=p.getString("station","").trim();
        if(!station.isEmpty()){
            try { s.transit=transit(station); }
            catch(Exception ex){s.transportError=ex.getMessage()==null?"Unavailable":ex.getMessage();}
        }
        s.pack=packing(s,p.getString("plan",""));
        s.updated=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date());
        return s;
    }
    static String clock(String iso) {int t=iso.indexOf('T');return t<0?iso:iso.substring(t+1,Math.min(t+6,iso.length()));}
    static List<Event> calendar(Context c){
        List<Event> out=new ArrayList<>();
        if(c.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)return out;
        Calendar now=Calendar.getInstance();now.set(Calendar.HOUR_OF_DAY,0);now.set(Calendar.MINUTE,0);now.set(Calendar.SECOND,0);now.set(Calendar.MILLISECOND,0);
        long start=now.getTimeInMillis();now.add(Calendar.DATE,2);long end=now.getTimeInMillis();
        Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uri,start);ContentUris.appendId(uri,end);
        String[] columns={CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.EVENT_LOCATION,CalendarContract.Instances.ALL_DAY};
        try(Cursor cur=c.getContentResolver().query(uri.build(),columns,null,null,CalendarContract.Instances.BEGIN+" ASC")){
            if(cur!=null){while(cur.moveToNext()&&out.size()<12){
                String title=cur.getString(0);long time=cur.getLong(1);String location=cur.getString(2);boolean allDay=cur.getInt(3)==1;
                if(title==null)title="Termin";
                String fmt=allDay?"—":new SimpleDateFormat("EEE HH:mm",Locale.getDefault()).format(new Date(time));
                out.add(new Event(title,location==null?"":location,fmt));
            }}
        }catch(Exception ignored){}
        return out;
    }
    static String transit(String station)throws Exception {
        String url="https://v6.vbb.transport.rest/locations?addresses=false&poi=false&results=1&query="+URLEncoder.encode(station,"UTF-8");
        JSONArray matches=new JSONArray(WeatherApi.getText(url));
        if(matches.length()==0)throw new Exception("Station not found");
        JSONObject match=matches.getJSONObject(0);String id=match.getString("id");
        JSONObject departures=WeatherApi.getJson("https://v6.vbb.transport.rest/stops/"+URLEncoder.encode(id,"UTF-8")+"/departures?duration=35&results=5&language=en");
        JSONArray rows=departures.optJSONArray("departures");
        if(rows==null||rows.length()==0)return match.getString("name")+": keine Abfahrten / no departures";
        StringBuilder b=new StringBuilder(match.getString("name")).append("\n");
        for(int i=0;i<rows.length()&&i<5;i++){
            JSONObject d=rows.getJSONObject(i),line=d.optJSONObject("line");
            String when=d.optString("when",d.optString("plannedWhen",""));
            b.append(line==null?"?":line.optString("name","?")).append(" → ").append(d.optString("direction","?")).append(" · ").append(clock(when));
            int delay=d.optInt("delay",0);if(delay>=60)b.append(" (+").append(Math.round(delay/60.0)).append("m)");
            if(i<rows.length()-1 && i<4)b.append("\n");
        }
        return b.toString();
    }
    static List<String> packing(Snapshot s,String extra){
        Set<String> bag=new LinkedHashSet<>();String lang=s.lang;
        bag.add(L10n.t(lang,"water"));
        if(s.rainPercent>=35)bag.add(L10n.t(lang,"umbrella"));
        if(s.forecastError.isEmpty()&&s.celsius<=14)bag.add(L10n.t(lang,"jacket"));
        if(s.uvValue>=5)bag.add(L10n.t(lang,"sunScreen"));
        StringBuilder text=new StringBuilder(extra==null?"":extra);
        for(Event e:s.events){text.append(" ").append(e.title).append(" ").append(e.location);}
        String all=text.toString().toLowerCase(Locale.ROOT);
        if(all.matches("(?s).*(uni|university|universität|vorlesung|seminar|lecture|campus|hochschule|大學|上課).*")){
            bag.add(L10n.t(lang,"laptop"));bag.add(L10n.t(lang,"notebook"));bag.add(L10n.t(lang,"studentId"));
        }
        if(all.matches("(?s).*(hiking|wander|outdoor|walk|sport|spazier|park|健行|登山|散步|戶外).*"))bag.add(L10n.t(lang,"walking"));
        if(all.matches("(?s).*(flight|airport|train|flug|reise|bahn|機場|飛機|旅行|火車).*")){
            bag.add(L10n.t(lang,"tickets"));if(all.matches("(?s).*(flight|airport|flug|機場|飛機).*"))bag.add(L10n.t(lang,"passport"));
        }
        return new ArrayList<>(bag);
    }
}