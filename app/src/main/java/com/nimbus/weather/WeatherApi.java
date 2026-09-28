package com.nimbus.weather;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class WeatherApi {
    private static String request(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent","NimbusDashboard/2.0 personal Android client");
        try {
            int status=c.getResponseCode();
            if(status!=200)throw new Exception("HTTP "+status);
            try(InputStream input=c.getInputStream();BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
                StringBuilder all=new StringBuilder();String line;
                while((line=reader.readLine())!=null)all.append(line);
                return all.toString();
            }
        }finally{c.disconnect();}
    }
    static JSONObject getJson(String url)throws Exception{return new JSONObject(request(url));}
    static JSONArray getJsonArray(String url)throws Exception{return new JSONArray(request(url));}
    static JSONObject searchCity(String city)throws Exception {
        String url="https://geocoding-api.open-meteo.com/v1/search?name="+URLEncoder.encode(city.trim(),"UTF-8")+"&count=1&language=en&format=json";
        JSONArray found=getJson(url).optJSONArray("results");
        if(found==null||found.length()==0)throw new Exception("City not found: "+city);
        return found.getJSONObject(0);
    }
    static JSONObject forecast(double lat,double lon,boolean fahrenheit)throws Exception{
        String url=String.format(Locale.US,"https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,precipitation,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,sunrise,sunset&timezone=auto&forecast_days=2&temperature_unit=%s",lat,lon,fahrenheit?"fahrenheit":"celsius");
        return getJson(url);
    }
}
