package com.nimbus.weather;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);
        EditText city = findViewById(R.id.cityInput);
        TextView status = findViewById(R.id.status);
        Button search = findViewById(R.id.searchButton);
        CheckBox fahrenheit = findViewById(R.id.fahrenheit);
        city.setText(WeatherWidget.prefs(this).getString("city", "Berlin"));
        fahrenheit.setChecked(WeatherWidget.prefs(this).getBoolean("fahrenheit", false));
        fahrenheit.setOnCheckedChangeListener((buttonView, checked) -> {
            WeatherWidget.prefs(this).edit().putBoolean("fahrenheit", checked).apply();
            WeatherWidget.updateAll(this);
        });
        search.setOnClickListener(v -> {
            String query = city.getText().toString().trim();
            if (query.isEmpty()) { status.setText("Enter a city name."); return; }
            search.setEnabled(false); status.setText("Searching for " + query + "…");
            executor.execute(() -> {
                try {
                    JSONObject result = WeatherApi.searchCity(query);
                    String label = result.getString("name");
                    String country = result.optString("country", "");
                    if (!country.isEmpty()) label += ", " + country;
                    double lat = result.getDouble("latitude"); double lon = result.getDouble("longitude");
                    WeatherWidget.prefs(this).edit().putString("city", label)
                        .putLong("lat", Double.doubleToRawLongBits(lat))
                        .putLong("lon", Double.doubleToRawLongBits(lon)).apply();
                    WeatherWidget.updateAll(this);
                    String resultLabel = label;
                    runOnUiThread(() -> {status.setText("Saved " + resultLabel + ". Widget is updating."); city.setText(resultLabel); search.setEnabled(true);});
                } catch (Exception error) {
                    runOnUiThread(() -> {status.setText("Could not find city: " + error.getMessage());search.setEnabled(true);});
                }
            });
        });
        findViewById(R.id.refreshButton).setOnClickListener(v -> {status.setText("Refreshing…");WeatherWidget.updateAll(this);});
        findViewById(R.id.yrButton).setOnClickListener(v -> {
            String query = WeatherWidget.prefs(this).getString("city", "Berlin");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.yr.no/en/search?q=" + Uri.encode(query))));
        });
    }
    @Override public void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
