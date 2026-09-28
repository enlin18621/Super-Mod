package com.nimbus.weather;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

final class TransitApi {
    static JSONObject departures(String stop) throws Exception{
        JSONArray found=WeatherApi.getJsonArray("https://v6.vbb.transport.rest/locations?query="+Uri.encode(stop)+"&poi=false&addresses=false&results=3");
        JSONObject station=null;
        for(int i=0;i<found.length();i++){
            JSONObject x=found.optJSONObject(i);if(x==null)continue;
            if("stop".equals(x.optString("type"))||"station".equals(x.optString("type"))){station=x;break;}
        }
        if(station==null)throw new Exception("VBB stop not found");
        JSONObject departures=WeatherApi.getJson("https://v6.vbb.transport.rest/stops/"+
            Uri.encode(station.getString("id"))+"/departures?duration=60&results=6&language=en");
        departures.put("selectedStop",station.optString("name",stop));return departures;
    }
    static JSONObject departuresNearest(double lat,double lon)throws Exception{
        JSONArray nearby=WeatherApi.getJsonArray(String.format(Locale.US,
            "https://v6.vbb.transport.rest/locations/nearby?latitude=%.6f&longitude=%.6f&results=3&distance=1200&poi=false&stops=true",lat,lon));
        JSONObject nearest=nearby.optJSONObject(0);
        if(nearest==null||nearest.optString("id").isEmpty())throw new Exception("No nearby VBB stops");
        JSONObject departures=WeatherApi.getJson("https://v6.vbb.transport.rest/stops/"+
            Uri.encode(nearest.optString("id"))+"/departures?duration=60&results=6&language=en");
        departures.put("selectedStop",nearest.optString("name",""));
        return departures;
    }
}
