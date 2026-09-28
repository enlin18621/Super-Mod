package com.nimbus.weather;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;

final class TransitApi {
    static JSONObject departures(String stop) throws Exception {
        JSONArray found = WeatherApi.getJsonArray("https://v6.vbb.transport.rest/locations?query="+Uri.encode(stop)+"&poi=false&addresses=false&results=3");
        String id=null;
        for(int i=0;i<found.length();i++){
            JSONObject item=found.getJSONObject(i);
            if("stop".equals(item.optString("type")) || "station".equals(item.optString("type"))){ id=item.optString("id");break; }
        }
        if(id==null||id.isEmpty()) throw new Exception("VBB stop not found");
        JSONObject departures=WeatherApi.getJson("https://v6.vbb.transport.rest/stops/"+Uri.encode(id)+"/departures?duration=60&results=5&language=en");
        departures.put("selectedStop",found.optJSONObject(0)==null?stop:found.getJSONObject(0).optString("name",stop));
        return departures;
    }
}
