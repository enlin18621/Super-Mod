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
        long startMs;
        boolean allDay;
        Event(String t,String l,String display,long epoch,boolean all){title=t;location=l;time=display;startMs=epoch;allDay=all;}
    }
    static final class Snapshot {
        String city="",condition="",temp="--°",feels="--",high="--",low="--",rain="--",wind="--",uv="--";
        String sunrise="--:--",sunset="--:--",transit="",forecastError="",transportError="",updated="";
        String extraPlan="",source="";
        int rainPercent=-1,dayOffset=0;
        double celsius=15,uvValue=0,windValue=0;
        long generatedAt=0;
        List<Event> events=new ArrayList<>();
        List<String> pack=new ArrayList<>();
        JourneyPlanner.Route route;
        String lang="de";
        Event upcoming(){
            long now=System.currentTimeMillis();
            for(Event e:events)if(!e.allDay&&e.startMs>now)return e;
            return null;
        }
        String agendaText(){
            StringBuilder b=new StringBuilder();
            for(Event e:events){if(b.length()>0)b.append("\n");b.append(e.time).append("  ").append(e.title);if(!e.location.isEmpty())b.append(" · ").append(e.location);}
            if(!extraPlan.trim().isEmpty())b.append("\n").append(extraPlan.trim());
            return b.toString();
        }
        String packText(){return android.text.TextUtils.join(" · ",pack);}
    }
    static Snapshot load(Context c){return load(c,0);}
    static Snapshot load(Context c,int offset){
        SharedPreferences p=WeatherWidget.prefs(c);
        Snapshot s=new Snapshot();s.lang=p.getString("lang","de");
        s.dayOffset=Math.max(0,Math.min(4,offset));
        s.events=calendar(c,s.dayOffset);
        s.extraPlan=s.dayOffset==0?p.getString("plan",""):"";
        boolean locked=p.getBoolean("city_locked",p.contains("lat")&&!p.contains("geo_ok"));
        s.city=locked?p.getString("city","Berlin"):UI.t(s.lang,"yourLocation");
        boolean hasGeo=locked||p.getBoolean("geo_ok",false);
        if(hasGeo){
            double lat=Double.longBitsToDouble(p.getLong("lat",Double.doubleToRawLongBits(52.52)));
            double lon=Double.longBitsToDouble(p.getLong("lon",Double.doubleToRawLongBits(13.405)));
            boolean f=p.getBoolean("fahrenheit",false);
            try {
                JSONObject obj=WeatherApi.forecast(lat,lon,f);
                JSONObject cur=obj.getJSONObject("current"),d=obj.getJSONObject("daily");
                int index=s.dayOffset;
                String unit=f?"°F":"°C";
                s.high=Math.round(d.getJSONArray("temperature_2m_max").getDouble(index))+unit;
                s.low=Math.round(d.getJSONArray("temperature_2m_min").getDouble(index))+unit;
                s.temp=index==0?Math.round(cur.getDouble("temperature_2m"))+"°":s.high;
                s.feels=index==0?Math.round(cur.getDouble("apparent_temperature"))+unit:"—";
                s.condition=WeatherApi.condition(index==0?cur.optInt("weather_code",-1):d.getJSONArray("weather_code").optInt(index,-1),s.lang);
                s.rainPercent=d.getJSONArray("precipitation_probability_max").optInt(index,-1);
                s.rain=s.rainPercent<0?"--":s.rainPercent+"%";
                s.windValue=cur.optDouble("wind_speed_10m",0);
                s.wind=Math.round(s.windValue)+" km/h";
                s.uvValue=d.getJSONArray("uv_index_max").optDouble(index,0);
                s.uv=String.format(Locale.US,"%.1f",s.uvValue);
                s.sunrise=clock(d.getJSONArray("sunrise").getString(index));
                s.sunset=clock(d.getJSONArray("sunset").getString(index));
                s.celsius=f?(cur.optDouble("temperature_2m",60)-32)*5/9:cur.optDouble("temperature_2m",15);
            }catch(Exception ex){s.forecastError=ex.getMessage()==null?"Nicht verfügbar":ex.getMessage();}
        } else s.forecastError=UI.t(s.lang,"locationDenied");
        if(s.dayOffset==0){
            Event next=s.upcoming();
            if(next!=null)s.route=JourneyPlanner.plan(c,next);
            String station=p.getString("station","").trim();
            if(!station.isEmpty()){
                try{s.transit=transit(station,s.lang);}
                catch(Exception ex){s.transportError=ex.getMessage()==null?"Nicht verfügbar":ex.getMessage();}
            }
        }
        s.pack=packing(s,s.extraPlan);
        s.generatedAt=System.currentTimeMillis();
        s.updated=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(s.generatedAt));
        return s;
    }
    static String clock(String iso){
        int t=iso.indexOf('T');
        return t<0?iso:iso.substring(t+1,Math.min(t+6,iso.length()));
    }
    static List<Event> calendar(Context c,int offset){
        List<Event> out=new ArrayList<>();
        if(c.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)return out;
        Calendar day=Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
        day.add(Calendar.DATE,offset);
        long start=day.getTimeInMillis();day.add(Calendar.DATE,1);long end=day.getTimeInMillis();
        Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uri,start);ContentUris.appendId(uri,end);
        String[] columns={CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.EVENT_LOCATION,CalendarContract.Instances.ALL_DAY};
        try(Cursor cur=c.getContentResolver().query(uri.build(),columns,null,null,CalendarContract.Instances.BEGIN+" ASC")){
            if(cur!=null)while(cur.moveToNext()&&out.size()<24){
                String title=cur.getString(0);long begin=cur.getLong(1);
                String place=cur.getString(2);boolean allDay=cur.getInt(3)==1;
                if(title==null)title="Termin";
                String display=allDay?UI.t(WeatherWidget.prefs(c).getString("lang","de"),"allDay"):new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(begin));
                out.add(new Event(title,place==null?"":place,display,begin,allDay));
            }
        }catch(Exception ignored){}
        return out;
    }
    static String transit(String station,String lang)throws Exception{
        String url="https://v6.vbb.transport.rest/locations?addresses=false&poi=false&results=1&query="+URLEncoder.encode(station,"UTF-8");
        JSONArray matches=new JSONArray(WeatherApi.getText(url));
        if(matches.length()==0)throw new Exception("Haltestelle nicht gefunden");
        JSONObject match=matches.getJSONObject(0);String id=match.getString("id");
        JSONObject departures=WeatherApi.getJson("https://v6.vbb.transport.rest/stops/"+URLEncoder.encode(id,"UTF-8")+"/departures?duration=50&results=6&language=de");
        JSONArray rows=departures.optJSONArray("departures");
        if(rows==null||rows.length()==0)return match.getString("name")+" · —";
        StringBuilder b=new StringBuilder(match.getString("name")).append("\n");
        for(int i=0;i<rows.length()&&i<6;i++){
            JSONObject d=rows.getJSONObject(i),line=d.optJSONObject("line");
            String when=d.optString("when",d.optString("plannedWhen",""));
            b.append(line==null?"?":line.optString("name","?")).append(" → ").append(d.optString("direction","?")).append(" · ").append(clock(when));
            int delay=d.optInt("delay",0);if(delay>=60)b.append(" (+").append(Math.round(delay/60.0)).append("m)");
            if(i<rows.length()-1&&i<5)b.append("\n");
        }
        return b.toString();
    }
    static List<String> packing(Snapshot s,String extra){
        Set<String> bag=new LinkedHashSet<>();String lang=s.lang;
        bag.add(L10n.t(lang,"water"));
        bag.add(L10n.t(lang,"keys"));
        if(s.rainPercent>=35)bag.add(L10n.t(lang,"umbrella"));
        if(s.forecastError.isEmpty()&&s.celsius<=14)bag.add(L10n.t(lang,"jacket"));
        if(s.uvValue>=5){bag.add(L10n.t(lang,"sunScreen"));bag.add(L10n.t(lang,"sunglasses"));}
        StringBuilder text=new StringBuilder(extra==null?"":extra);
        for(Event e:s.events)text.append(" ").append(e.title).append(" ").append(e.location);
        String all=text.toString().toLowerCase(Locale.ROOT);
        if(all.matches("(?s).*(uni|university|universität|vorlesung|seminar|lecture|campus|hochschule|大學|上課).*")){
            bag.add(L10n.t(lang,"laptop"));bag.add(L10n.t(lang,"notebook"));bag.add(L10n.t(lang,"studentId"));
        }
        if(all.matches("(?s).*(hiking|wander|outdoor|walk|spazier|park|健行|登山|散步|戶外).*")){
            bag.add(L10n.t(lang,"walking"));bag.add(L10n.t(lang,"snack"));
        }
        if(all.matches("(?s).*(fitness|gym|training|sport|fitnessstudio|健身|運動).*")){
            bag.add(L10n.t(lang,"gymKit"));
        }
        if(all.matches("(?s).*(flight|airport|train|flug|reise|bahn|機場|飛機|旅行|火車).*")){
            bag.add(L10n.t(lang,"tickets"));
            if(all.matches("(?s).*(flight|airport|flug|機場|飛機).*"))bag.add(L10n.t(lang,"passport"));
        }
        return new ArrayList<>(bag);
    }
    private DayEngine(){}
}
