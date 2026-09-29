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
    static String getText(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "NimbusDaily/2.0 (+local Android app)");
        try {
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            try (InputStream input = c.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder all = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) all.append(line);
                return all.toString();
            }
        } finally { c.disconnect(); }
    }
    static JSONObject getJson(String url) throws Exception { return new JSONObject(getText(url)); }
    static JSONObject searchCity(String city) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(city.trim(), "UTF-8") + "&count=1&language=en&format=json";
        JSONArray results = getJson(url).optJSONArray("results");
        if (results == null || results.length() == 0) throw new Exception("City not found");
        return results.getJSONObject(0);
    }
    static JSONObject forecast(double lat, double lon, boolean fahrenheit) throws Exception {
        String url = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,sunrise,sunset,weather_code&timezone=auto&forecast_days=5&temperature_unit=%s", lat, lon, fahrenheit ? "fahrenheit" : "celsius");
        return getJson(url);
    }
    static String condition(int code, String lang) {
        String key = code == 0 ? "clear" : code == 1 ? "mostlyClear" : code == 2 ? "partly" : code == 3 ? "cloudy"
           : code == 45 || code == 48 ? "fog" : code >= 51 && code <= 57 ? "drizzle"
           : code >= 61 && code <= 67 ? "rain" : code >= 71 && code <= 77 ? "snow"
           : code >= 80 && code <= 82 ? "showers" : code == 85 || code == 86 ? "snow"
           : code >= 95 ? "storm" : "weather";
        return L10n.t(lang,key);
    }
}