package com.nimbus.weather;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class DayPlanner {
    static List<String> suggestions(Context c, JSONObject weather, List<CalendarReader.Event> events) {
        LinkedHashSet<String> items=new LinkedHashSet<>();
        items.add(L10n.t(c,"bringBasics"));
        JSONObject current=weather==null?null:weather.optJSONObject("current");
        JSONObject daily=weather==null?null:weather.optJSONObject("daily");
        if(current!=null){
            double feels=current.optDouble("apparent_temperature",16);
            if(Prefs.get(c).getBoolean("fahrenheit",false))feels=(feels-32)*5/9;
            if(feels<11)items.add(L10n.t(c,"bringCold"));
            if(feels<2)items.add(L10n.t(c,"bringGloves"));
        }
        if(daily!=null){
            JSONArray rain=daily.optJSONArray("precipitation_probability_max");
            if(rain!=null&&rain.optInt(0,0)>=40)items.add(L10n.t(c,"bringRain"));
            JSONArray uv=daily.optJSONArray("uv_index_max");
            if(uv!=null&&uv.optDouble(0,0)>=5)items.add(L10n.t(c,"bringSun"));
        }
        StringBuilder activities=new StringBuilder(Prefs.get(c).getString("plans",""));
        for(CalendarReader.Event event:events)activities.append(' ').append(event.title).append(' ').append(event.location);
        String all=activities.toString().toLowerCase(Locale.ROOT);
        if(matches(all,"uni","campus","jura","lecture","vorlesung","seminar","tutorial","exam","prüfung","university","大學","大学","上課","考試"))
            items.add(L10n.t(c,"bringUni"));
        if(matches(all,"sport","gym","fitness","training","laufen","jogging","workout","schwimmen","運動","健身","跑步"))
            items.add(L10n.t(c,"bringSports"));
        if(matches(all,"outdoor","wandern","hiking","park","radfahren","biking","ausflug","spaziergang","登山","健行","戶外","騎車"))
            items.add(L10n.t(c,"bringOutdoor"));
        if(matches(all,"travel","reise","flight","flug","airport","bahn","train","zug","trip","機場","飛機","火車","旅行"))
            items.add(L10n.t(c,"bringTravel"));
        if(activities.length()>50||events.size()>=3)items.add(L10n.t(c,"bringBattery"));
        return new ArrayList<>(items);
    }
    private static boolean matches(String s,String... words){for(String w:words)if(s.contains(w))return true;return false;}
}
