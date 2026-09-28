package com.nimbus.weather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherWidget extends AppWidgetProvider {
    static final String ACTION_REFRESH = "com.nimbus.weather.REFRESH";
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("nimbus_settings", Context.MODE_PRIVATE); }
    static void updateAll(Context context) { updateAll(context, null); }
    static void updateAll(Context context, PendingResult pending) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        int[] ids = manager.getAppWidgetIds(new ComponentName(app, WeatherWidget.class));
        if (ids.length == 0) { if (pending != null) pending.finish(); return; }
        SharedPreferences p = prefs(app);
        RemoteViews loading = shell(app, p);
        loading.setTextViewText(R.id.condition, "Updating forecast…");
        manager.updateAppWidget(ids, loading);
        EXEC.execute(() -> {
            try {
                JSONObject response = WeatherApi.forecast(
                    Double.longBitsToDouble(p.getLong("lat", Double.doubleToRawLongBits(52.52))),
                    Double.longBitsToDouble(p.getLong("lon", Double.doubleToRawLongBits(13.405))),
                    p.getBoolean("fahrenheit", false));
                JSONObject current = response.getJSONObject("current");
                JSONObject daily = response.getJSONObject("daily");
                String unit = p.getBoolean("fahrenheit", false) ? "°F" : "°C";
                RemoteViews v = shell(app, p);
                v.setTextViewText(R.id.temp, Math.round(current.getDouble("temperature_2m")) + "°");
                v.setTextViewText(R.id.condition, WeatherApi.condition(current.optInt("weather_code", -1)));
                v.setTextViewText(R.id.feels, "Feels like " + Math.round(current.getDouble("apparent_temperature")) + unit);
                JSONArray hi = daily.getJSONArray("temperature_2m_max");
                JSONArray lo = daily.getJSONArray("temperature_2m_min");
                JSONArray rain = daily.optJSONArray("precipitation_probability_max");
                v.setTextViewText(R.id.details, "H " + Math.round(hi.getDouble(0)) + "°  ·  L " + Math.round(lo.getDouble(0)) + "°  ·  Rain " + (rain == null || rain.isNull(0) ? "--" : rain.optInt(0)) + "%  ·  Wind " + Math.round(current.getDouble("wind_speed_10m")) + " km/h");
                String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                v.setTextViewText(R.id.updated, "Updated " + now + " · Yr ↗ · Open-Meteo");
                manager.updateAppWidget(ids, v);
            } catch (Exception ex) {
                RemoteViews error = shell(app, p);
                error.setTextViewText(R.id.condition, "Weather unavailable");
                error.setTextViewText(R.id.updated, "Tap ↻ to try again · Yr ↗");
                manager.updateAppWidget(ids, error);
            } finally { if (pending != null) pending.finish(); }
        });
    }
    static RemoteViews shell(Context c, SharedPreferences p) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_weather);
        v.setTextViewText(R.id.city, p.getString("city", "Berlin").toUpperCase(Locale.getDefault()));
        Intent forecast = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.yr.no/en/search?q=" + Uri.encode(p.getString("city", "Berlin"))));
        forecast.addCategory(Intent.CATEGORY_BROWSABLE);
        v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(c, 1, forecast, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent settings = new Intent(c, MainActivity.class);
        v.setOnClickPendingIntent(R.id.edit, PendingIntent.getActivity(c, 2, settings, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent refresh = new Intent(c, WeatherWidget.class).setAction(ACTION_REFRESH);
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 3, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) { updateAll(c); }
    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action) || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) updateAll(c, goAsync());
        else super.onReceive(c, intent);
    }
}
