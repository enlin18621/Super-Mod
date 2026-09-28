package com.nimbus.weather;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int CALENDAR_PERMISSION=41;
    private static final int BACKGROUND=0xff101d2d, PANEL=0xff1a2c40, WHITE=0xfff4f8fd, MINT=0xff78ddcf, MUTED=0xffb0c5d8;
    private static final String[] LANGS={"de","en","zh-TW"};
    private static final ExecutorService SAVE_EXECUTOR=Executors.newSingleThreadExecutor();
    private LinearLayout root;private TextView weatherText,sunText,eventsText,transitText,bringText,statusText;
    private EditText cityInput,homeInput,uniInput,stopInput,plansInput;
    private Spinner langSpinner;private CheckBox fahrenheit,shareAddress;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);buildUi();render(DashboardData.cached(this));handleSharedText(getIntent());refreshAll();
    }
    @Override protected void onNewIntent(Intent incoming){
        super.onNewIntent(incoming);setIntent(incoming);handleSharedText(incoming);
    }
    private static String todayStamp(){return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());}
    private int dp(float value){return (int)(getResources().getDisplayMetrics().density*value+.5f);}
    private GradientDrawable background(int color,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);
        v.setLineSpacing(dp(2),1f);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private void addSpace(LinearLayout parent,int dp){
        View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(dp)));
    }
    private LinearLayout card(String title){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(16),dp(18),dp(17));
        panel.setBackground(background(PANEL,19));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,0,0,dp(13));root.addView(panel,params);
        TextView header=text(title,18,WHITE,true);panel.addView(header);addSpace(panel,11);return panel;
    }
    private TextView content(LinearLayout parent){
        TextView v=text("—",15,MUTED,false);parent.addView(v);return v;
    }
    private Button button(LinearLayout parent,String label,Runnable run){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(BACKGROUND);
        b.setBackgroundTintList(ColorStateList.valueOf(MINT));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(47));lp.setMargins(0,dp(8),0,0);parent.addView(b,lp);
        b.setOnClickListener(v->run.run());return b;
    }
    private EditText input(LinearLayout parent,String label,String value,boolean multiline){
        TextView header=text(label,13,MINT,true);LinearLayout.LayoutParams headParams=new LinearLayout.LayoutParams(-1,-2);headParams.setMargins(0,dp(13),0,dp(2));
        parent.addView(header,headParams);
        EditText e=new EditText(this);e.setText(value);e.setTextSize(15);e.setTextColor(WHITE);
        e.setHintTextColor(MUTED);e.setBackgroundTintList(ColorStateList.valueOf(MUTED));
        if(multiline){e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setMinLines(3);e.setMaxLines(6);}
        else{e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);e.setSingleLine(true);}
        parent.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
    }
    private void buildUi(){
        SharedPreferences p=Prefs.get(this);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BACKGROUND);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(27),dp(18),dp(35));
        scroll.addView(root);setContentView(scroll);
        TextView brand=text(L10n.t(this,"app"),14,MINT,true);brand.setLetterSpacing(.11f);root.addView(brand);
        addSpace(root,7);
        TextView title=text(new SimpleDateFormat("EEEE · d MMMM",Locale.forLanguageTag(L10n.lang(this))).format(new Date()),25,WHITE,true);root.addView(title);
        addSpace(root,14);

        LinearLayout forecastCard=card("☀  "+L10n.t(this,"weather")+" · "+Prefs.city(this));
        weatherText=text("—",22,WHITE,true);forecastCard.addView(weatherText);
        addSpace(forecastCard,6);sunText=content(forecastCard);
        button(forecastCard,L10n.t(this,"yr"),()->{
            Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.yr.no/en/search?q="+Uri.encode(Prefs.city(this))));
            startActivity(i);
        });

        LinearLayout calendarCard=card("▣  "+L10n.t(this,"calendar"));
        eventsText=content(calendarCard);
        button(calendarCard,L10n.t(this,"calpermission"),()->{
            if(!CalendarReader.allowed(this))requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},CALENDAR_PERMISSION);
            else refreshAll();
        });

        LinearLayout transitCard=card("↗  "+L10n.t(this,"departures"));
        transitText=content(transitCard);
        button(transitCard,L10n.t(this,"map"),this::openMaps);

        LinearLayout briefCard=card("✓  "+L10n.t(this,"brief"));
        bringText=content(briefCard);addSpace(briefCard,7);
        briefCard.addView(text(L10n.t(this,"ruleDisclaimer"),12,MUTED,false));

        LinearLayout actions=card(L10n.t(this,"today"));
        button(actions,L10n.t(this,"refresh"),this::refreshAll);
        button(actions,L10n.t(this,"chatgpt"),this::shareToChatGpt);
        button(actions,L10n.t(this,"paste"),this::pasteUpdate);
        actions.addView(text(L10n.t(this,"chatnote"),12,MUTED,false));
        statusText=text("",13,MINT,false);LinearLayout.LayoutParams statusParams=new LinearLayout.LayoutParams(-1,-2);
        statusParams.setMargins(0,dp(13),0,0);actions.addView(statusText,statusParams);

        LinearLayout settings=card("⚙  "+L10n.t(this,"edit"));
        settings.addView(text(L10n.t(this,"settingsHint"),13,MUTED,false));
        cityInput=input(settings,L10n.t(this,"city"),p.getString("pendingCity",Prefs.city(this)),false);
        homeInput=input(settings,L10n.t(this,"home"),p.getString("home",""),false);
        uniInput=input(settings,L10n.t(this,"uni"),p.getString("uni",""),false);
        stopInput=input(settings,L10n.t(this,"stop"),p.getString("stop",""),false);
        plansInput=input(settings,L10n.t(this,"plans"),todayStamp().equals(p.getString("plans_day",""))?p.getString("plans",""):"",true);
        TextView language=text(L10n.t(this,"lang"),13,MINT,true);
        LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(-1,-2);labelParams.setMargins(0,dp(14),0,dp(6));settings.addView(language,labelParams);
        langSpinner=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"Deutsch","English","繁體中文"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);langSpinner.setAdapter(adapter);
        String lang=L10n.lang(this);langSpinner.setSelection("en".equals(lang)?1:("zh-TW".equals(lang)?2:0));
        settings.addView(langSpinner);
        fahrenheit=new CheckBox(this);fahrenheit.setText(L10n.t(this,"fahrenheit"));fahrenheit.setTextColor(WHITE);
        fahrenheit.setChecked(p.getBoolean("fahrenheit",false));settings.addView(fahrenheit);
        shareAddress=new CheckBox(this);shareAddress.setText(L10n.t(this,"shareaddress"));shareAddress.setTextColor(WHITE);
        shareAddress.setChecked(p.getBoolean("share_address",false));settings.addView(shareAddress);
        button(settings,L10n.t(this,"save"),this::saveSettings);
    }
    private void render(DashboardData.Snapshot s){
        if(weatherText==null)return;
        if(s.weather!=null){
            JSONObject current=s.weather.optJSONObject("current");
            JSONObject daily=s.weather.optJSONObject("daily");
            weatherText.setText(DashboardData.daySummary(this,s));
            String unit=Prefs.get(this).getBoolean("fahrenheit",false)?"°F":"°C";
            StringBuilder more=new StringBuilder();
            if(current!=null){
                more.append(L10n.t(this,"feels")).append(" ").append(DashboardData.degree(current.optDouble("apparent_temperature",Double.NaN))).append("  ·  ")
                    .append(L10n.t(this,"humidity")).append(" ").append(current.optInt("relative_humidity_2m",0)).append("%\n")
                    .append(L10n.t(this,"wind")).append(" ").append(Math.round(current.optDouble("wind_speed_10m",0))).append(" km/h");
            }
            if(daily!=null){
                JSONArray rain=daily.optJSONArray("precipitation_probability_max"),uv=daily.optJSONArray("uv_index_max");
                if(rain!=null)more.append("  ·  ").append(L10n.t(this,"rain")).append(" ").append(rain.optInt(0)).append("%");
                if(uv!=null)more.append("  ·  ").append(L10n.t(this,"uv")).append(" ").append(Math.round(uv.optDouble(0,0)));
            }
            more.append("\n").append(L10n.t(this,"sunrise")).append(" ↑ ").append(DashboardData.sunTime(s.weather,"sunrise"))
                .append("    ").append(L10n.t(this,"sunset")).append(" ↓ ").append(DashboardData.sunTime(s.weather,"sunset"));
            sunText.setText(more.toString());
        } else {weatherText.setText("—");sunText.setText(L10n.t(this,"refresh"));}
        if(!CalendarReader.allowed(this)){
            eventsText.setText(L10n.t(this,"permission"));
        }else if(s.events.isEmpty())eventsText.setText(L10n.t(this,"calempty"));
        else{
            StringBuilder events=new StringBuilder();
            for(CalendarReader.Event e:s.events){
                events.append(e.allDay?L10n.t(this,"allday"):DashboardData.time(this,e.start))
                    .append("   ").append(e.title==null?"—":e.title);
                if(e.location!=null&&!e.location.isEmpty())events.append("\n      ↳ ").append(e.location);
                events.append("\n\n");
            }
            eventsText.setText(events.toString().trim());
        }
        String stop=Prefs.get(this).getString("stop","").trim();
        if(stop.isEmpty())transitText.setText(L10n.t(this,"stopmissing"));
        else if(s.transit==null||s.transit.optJSONArray("departures")==null)transitText.setText(L10n.t(this,"departuresempty"));
        else{
            JSONArray departures=s.transit.optJSONArray("departures");
            StringBuilder result=new StringBuilder(stop).append("\n\n");
            for(int i=0;i<Math.min(4,departures.length());i++){
                JSONObject d=departures.optJSONObject(i);if(d==null)continue;
                JSONObject line=d.optJSONObject("line");
                String time=d.optString("when",d.optString("plannedWhen",""));
                result.append(time.length()>15?time.substring(11,16):"--:--").append("    ")
                    .append(line==null?"":line.optString("name","")).append("  →  ").append(d.optString("direction",""));
                if(!d.isNull("delay")&&d.optInt("delay",0)>0)result.append("  +").append(d.optInt("delay")/60).append(" min");
                result.append("\n");
            }
            transitText.setText(result.toString().trim());
        }
        StringBuilder items=new StringBuilder();
        for(String item:s.bring)items.append("•  ").append(item).append("\n");
        bringText.setText(items.toString().trim());
        if(s.updated>0)statusText.setText(L10n.t(this,"updated")+" "+new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(s.updated))+(s.error?" · "+L10n.t(this,"error"):""));
    }
    private void refreshAll(){
        statusText.setText(L10n.t(this,"updating"));
        DashboardData.refresh(this,s->{
            WeatherWidget.render(this,s);
            runOnUiThread(()->render(s));
        });
    }
    private void saveSettings(){
        SharedPreferences p=Prefs.get(this);String oldCity=Prefs.city(this),oldLang=L10n.lang(this);
        String requestedCity=cityInput.getText().toString().trim();
        SharedPreferences.Editor edit=p.edit().putString("home",homeInput.getText().toString().trim())
            .putString("uni",uniInput.getText().toString().trim()).putString("stop",stopInput.getText().toString().trim())
            .putString("plans",plansInput.getText().toString()).putString("plans_day",todayStamp()).putString("lang",LANGS[langSpinner.getSelectedItemPosition()])
            .putBoolean("fahrenheit",fahrenheit.isChecked()).putBoolean("share_address",shareAddress.isChecked());
        edit.apply();boolean languageChanged=!oldLang.equals(L10n.lang(this));
        if(!requestedCity.isEmpty()&&!requestedCity.equals(oldCity)){
            statusText.setText(L10n.t(this,"updating"));
            SAVE_EXECUTOR.execute(()->{
                try{
                    JSONObject city=WeatherApi.searchCity(requestedCity);
                    String label=city.getString("name");
                    if(!city.optString("country","").isEmpty())label+=", "+city.optString("country");
                    p.edit().putString("city",label)
                        .putLong("lat",Double.doubleToRawLongBits(city.getDouble("latitude")))
                        .putLong("lon",Double.doubleToRawLongBits(city.getDouble("longitude")))
                        .remove("weather_cache").remove("pendingCity").apply();
                    runOnUiThread(()->{buildUi();refreshAll();});
                }catch(Exception ex){
                    runOnUiThread(()->statusText.setText(L10n.t(this,"error")+" "+ex.getMessage()));
                }
            });
        }else{
            p.edit().remove("pendingCity").apply();
            if(languageChanged)buildUi();
            statusText.setText(L10n.t(this,"saved"));
            refreshAll();
        }
    }
    private void openMaps(){
        SharedPreferences p=Prefs.get(this);String destination=p.getString("uni","");
        if(destination.trim().isEmpty()){statusText.setText(L10n.t(this,"uni"));return;}
        Uri.Builder uri=Uri.parse("https://www.google.com/maps/dir/").buildUpon()
            .appendQueryParameter("api","1").appendQueryParameter("destination",destination).appendQueryParameter("travelmode","transit");
        String home=p.getString("home","");if(!home.trim().isEmpty())uri.appendQueryParameter("origin",home);
        startActivity(new Intent(Intent.ACTION_VIEW,uri.build()));
    }
    private void shareToChatGpt(){
        DashboardData.Snapshot s=DashboardData.cached(this);
        StringBuilder prompt=new StringBuilder("Please help me plan today based on the following up-to-date dashboard information. Reply in ")
            .append("zh-TW".equals(L10n.lang(this))?"Traditional Chinese":("en".equals(L10n.lang(this))?"English":"German"))
            .append(". Suggest what to bring, when to leave and any practical schedule considerations. Do not assume missing data is verified.\n");
        prompt.append("Date/time: ").append(new Date()).append("\nWeather location: ").append(Prefs.city(this))
            .append("\nForecast: ").append(DashboardData.daySummary(this,s))
            .append("\nSunrise: ").append(DashboardData.sunTime(s.weather,"sunrise"))
            .append("\nSunset: ").append(DashboardData.sunTime(s.weather,"sunset"));
        for(CalendarReader.Event e:s.events)prompt.append("\nCalendar: ").append(e.allDay?"All day":DashboardData.time(this,e.start))
            .append(" ").append(e.title).append(e.location==null?"":" @ "+e.location);
        prompt.append("\nPlans: ").append(todayStamp().equals(Prefs.get(this).getString("plans_day",""))?Prefs.get(this).getString("plans",""):"(not set today)")
            .append("\nVBB stop: ").append(Prefs.get(this).getString("stop",""));
        if(s.transit!=null){
            JSONArray departures=s.transit.optJSONArray("departures");
            if(departures!=null)for(int i=0;i<Math.min(3,departures.length());i++){
                JSONObject d=departures.optJSONObject(i);if(d==null)continue;
                String when=d.optString("when","");
                prompt.append("\nDeparture: ").append(when.length()>15?when.substring(11,16):"—").append(" ");
                JSONObject line=d.optJSONObject("line");
                prompt.append(line==null?"":line.optString("name")).append(" → ").append(d.optString("direction"));
            }
        }
        if(Prefs.get(this).getBoolean("share_address",false)){
            prompt.append("\nHome address: ").append(Prefs.get(this).getString("home",""))
                .append("\nUniversity/destination: ").append(Prefs.get(this).getString("uni",""));
        }
        prompt.append("\nTo propose changes that I can import into Nimbus, include one line beginning NIMBUS_UPDATE: followed by valid JSON with only these supported keys: city, home, uni, stop, plans, lang (de/en/zh-TW), fahrenheit. I'll review and confirm before importing it via Android Share. Never claim you can push data directly to my phone.");
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,prompt.toString());
        Intent chat=new Intent(i);chat.setPackage("com.openai.chatgpt");
        try{startActivity(chat);}catch(ActivityNotFoundException ex){startActivity(Intent.createChooser(i,L10n.t(this,"chatgpt")));}
    }
    private void pasteUpdate(){
        ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data=clipboard==null?null:clipboard.getPrimaryClip();
        CharSequence content=data!=null&&data.getItemCount()>0?data.getItemAt(0).coerceToText(this):null;
        if(content==null||!content.toString().contains("NIMBUS_UPDATE:")){
            statusText.setText(L10n.t(this,"pasteEmpty"));return;
        }
        Intent i=new Intent(Intent.ACTION_SEND);i.putExtra(Intent.EXTRA_TEXT,content.toString());
        handleSharedText(i);
    }
    private void handleSharedText(Intent incoming){
        if(incoming==null||!Intent.ACTION_SEND.equals(incoming.getAction()))return;
        String text=incoming.getStringExtra(Intent.EXTRA_TEXT);
        if(text==null)return;
        int index=text.indexOf("NIMBUS_UPDATE:");
        if(index<0)return;
        String candidate=text.substring(index+"NIMBUS_UPDATE:".length()).trim();
        int first=candidate.indexOf('{'),last=candidate.lastIndexOf('}');
        if(first<0||last<first)return;
        try{
            JSONObject obj=new JSONObject(candidate.substring(first,last+1));
            new AlertDialog.Builder(this).setTitle(L10n.t(this,"importTitle")).setMessage(obj.toString(2))
                .setNegativeButton(L10n.t(this,"cancel"),(d,w)->{})
                .setPositiveButton(L10n.t(this,"import"),(d,w)->importJson(obj)).show();
        }catch(Exception ex){statusText.setText("Invalid Nimbus update JSON.");}
    }
    private void importJson(JSONObject json){
        SharedPreferences.Editor edit=Prefs.get(this).edit();
        String[] textFields={"home","uni","stop","plans"};
        for(String key:textFields)if(json.has(key)&&!json.isNull(key))edit.putString(key,json.optString(key).substring(0,Math.min(4000,json.optString(key).length())));
        if(json.has("plans"))edit.putString("plans_day",todayStamp());
        if(json.has("city")&&!json.isNull("city"))edit.putString("pendingCity",json.optString("city").substring(0,Math.min(150,json.optString("city").length())));
        if(json.has("fahrenheit"))edit.putBoolean("fahrenheit",json.optBoolean("fahrenheit",false));
        String lang=json.optString("lang","");if(lang.equals("de")||lang.equals("en")||lang.equals("zh-TW"))edit.putString("lang",lang);
        edit.apply();buildUi();render(DashboardData.cached(this));
        statusText.setText(L10n.t(this,"imported"));
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==CALENDAR_PERMISSION)refreshAll();
    }
}
