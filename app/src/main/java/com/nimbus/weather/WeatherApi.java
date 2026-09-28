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
    static JSONObject getJson(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(9000); connection.setReadTimeout(9000);
        connection.setRequestProperty("User-Agent", "NimbusWeatherAndroid/1.0");
        try {
            if (connection.getResponseCode() != 200) throw new Exception("Weather service HTTP " + connection.getResponseCode());
            try (InputStream input = connection.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder all = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) all.append(line);
                return new JSONObject(all.toString());
            }
        } finally { connection.disconnect(); }
    }
    static JSONObject searchCity(String city) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(city.trim(), "UTF-8") + "&count=1&language=en&format=json";
        JSONArray results = getJson(url).optJSONArray("results");
        if (results == null || results.length() == 0) throw new Exception("City not found. Try a larger nearby city.");
        return results.getJSONObject(0);
    }
    static JSONObject forecast(double lat, double lon, boolean fahrenheit) throws Exception {
        String url = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=1&temperature_unit=%s", lat, lon, fahrenheit ? "fahrenheit" : "celsius");
        return getJson(url);
    }
    static String condition(int code) {
        if (code == 0) return "☀ Clear sky";
        if (code == 1) return "🌤 Mainly clear";
        if (code == 2) return "⛅ Partly cloudy";
        if (code == 3) return "☁ Overcast";
        if (code == 45 || code == 48) return "🌫 Foggy";
        if (code >= 51 && code <= 57) return "☂ Drizzle";
        if (code >= 61 && code <= 67) return "🌧 Rainy";
        if (code >= 71 && code <= 77) return "❄ Snow";
        if (code >= 80 && code <= 82) return "🌦 Showers";
        if (code == 85 || code == 86) return "🌨 Snow showers";
        if (code >= 95) return "⛈ Thunderstorms";
        return "Current conditions";
    }
}
