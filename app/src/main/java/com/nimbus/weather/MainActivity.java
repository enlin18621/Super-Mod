package com.nimbus.weather;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final int BG=Color.rgb(13,23,38), CARD=Color.rgb(23,39,57), TEXT=Color.WHITE, MUTED=Color.rgb(173,194,206), MINT=Color.rgb(108,224,205);
    private LinearLayout main;
    private EditText city,station,home,uni,plan;
    private Spinner language;
    private CheckBox fahrenheit, shareHome;
    private TextView weather,calendar,transit,sun,pack,status;
    private DayEngine.Snapshot snapshot;
    private String lang="de";
    private SharedPreferences p;
    private String t(String k){return L10n.t(lang,k);}
    @Override public void onCreate(Bundle savedState){
        super.onCreate(savedState);p=WeatherWidget.prefs(this);lang=p.getString("lang","de");buildUI();renderEmpty();
        if(checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED)refresh();
    }
    private GradientDrawable shape(int color,int radius) {GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String value,int sp,int color){TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(sp);v.setPadding(0,5,0,5);return v;}
    private void addGap(LinearLayout l,int dp){View gap=new View(this);l.addView(gap,new LinearLayout.LayoutParams(1,dp));}
    private void heading(LinearLayout l,String title){TextView v=text(title,13,MINT);v.setLetterSpacing(.13f);v.setTypeface(null,Typeface.BOLD);l.addView(v);}
    private LinearLayout card(String title){
        LinearLayout l=col();l.setPadding(20,16,20,18);l.setBackground(shape(CARD,30));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=16;main.addView(l,lp);heading(l,title);return l;
    }
    private EditText field(LinearLayout l,String hint,String value){
        EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(false);e.setMaxLines(2);e.setText(value);e.setTextSize(15);e.setTextColor(TEXT);e.setHintTextColor(MUTED);
        e.setPadding(6,14,6,14);l.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
    }
    private Button btn(LinearLayout l,String caption,Runnable action) {
        Button b=new Button(this);b.setText(caption);b.setAllCaps(false);b.setTextColor(BG);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(MINT));b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=8;l.addView(b,lp);return b;
    }
    private void buildUI(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(BG);setContentView(sc);
        main=col();main.setPadding(20,30,20,42);sc.addView(main);
        TextView title=text("NIMBUS  /  DAILY",15,MINT);title.setLetterSpacing(.22f);title.setTypeface(null,Typeface.BOLD);main.addView(title);
        TextView subtitle=text(t("subtitle"),29,TEXT);subtitle.setTypeface(null,Typeface.BOLD);main.addView(subtitle);addGap(main,22);
        LinearLayout hero=card(t("today"));weather=text("",28,TEXT);hero.addView(weather);sun=text("",15,MUTED);hero.addView(sun);
        LinearLayout agenda=card(t("agenda"));calendar=text("",15,TEXT);agenda.addView(calendar);
        LinearLayout tr=card(t("transport"));transit=text("",15,TEXT);tr.addView(transit);
        LinearLayout packing=card(t("pack"));pack=text("",17,TEXT);packing.addView(pack);
        LinearLayout settings=card("⚙ "+t("language")+" / "+t("city"));
        language=new Spinner(this);String[] opts={"Deutsch","English","繁體中文"};
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts);language.setAdapter(adapter);
        language.setSelection("en".equals(lang)?1:"zh".equals(lang)?2:0);settings.addView(language);
        city=field(settings,t("city"),p.getString("city","Berlin"));
        station=field(settings,t("station"),p.getString("station",""));
        home=field(settings,t("home"),p.getString("home",""));
        uni=field(settings,t("university"),p.getString("uni",""));
        plan=field(settings,t("plans"),p.getString("plan",""));
        fahrenheit=new CheckBox(this);fahrenheit.setText(t("fahrenheit"));fahrenheit.setTextColor(TEXT);fahrenheit.setChecked(p.getBoolean("fahrenheit",false));settings.addView(fahrenheit);
        btn(settings,t("save"),this::save);
        LinearLayout tools=card("TOOLS");
        btn(tools,t("refresh"),this::refresh);
        btn(tools,t("permission"),this::calendarPermission);
        btn(tools,t("maps"),this::maps);
        btn(tools,t("calendarApp"),this::openCalendar);
        btn(tools,t("newEvent"),this::newEvent);
        btn(tools,t("chatgpt"),this::askChatGPT);
        shareHome=new CheckBox(this);shareHome.setText("ChatGPT: Share home address / Adresse teilen / 分享住家地址");shareHome.setTextColor(MUTED);shareHome.setChecked(false);tools.addView(shareHome);
        status=text(t("waiting"),14,MUTED);main.addView(status);main.addView(text(t("source")+" · "+t("updated")+" ↻",12,MUTED));
    }
    private void renderEmpty(){weather.setText(t("waiting"));calendar.setText(t("noEvents"));transit.setText(t("noStop"));sun.setText(t("notReady"));pack.setText(t("noPack"));}
    private void save(){
        String newLang=language.getSelectedItemPosition()==1?"en":language.getSelectedItemPosition()==2?"zh":"de";
        lang=newLang;
        String query=city.getText().toString().trim(), current=p.getString("city","Berlin");
        p.edit().putString("lang",newLang).putString("station",station.getText().toString().trim())
            .putString("home",home.getText().toString().trim()).putString("uni",uni.getText().toString().trim())
            .putString("plan",plan.getText().toString().trim()).putBoolean("fahrenheit",fahrenheit.isChecked()).apply();
        status.setText(t("saved"));
        if(query.isEmpty()||query.equals(current)){buildUI();renderEmpty();refresh();return;}
        executor.execute(()->{
            try {JSONObject r=WeatherApi.searchCity(query);
                String label=r.getString("name");String country=r.optString("country","");
                if(!country.isEmpty())label+=", "+country;
                p.edit().putString("city",label).putLong("lat",Double.doubleToRawLongBits(r.getDouble("latitude")))
                  .putLong("lon",Double.doubleToRawLongBits(r.getDouble("longitude"))).apply();
                runOnUiThread(()->{buildUI();renderEmpty();refresh();});
            }catch(Exception ex){runOnUiThread(()->status.setText(t("errorCity")+ex.getMessage()));}
        });
    }
    private void refresh(){
        status.setText("↻ "+t("updated")+" …");
        executor.execute(()->{
            DayEngine.Snapshot s=DayEngine.load(getApplicationContext());
            runOnUiThread(()->{snapshot=s;render(s);WeatherWidget.updateAll(getApplicationContext());});
        });
    }
    private void render(DayEngine.Snapshot s){
        String unit=p.getBoolean("fahrenheit",false)?"°F":"°C";
        weather.setText(s.city+"\n"+s.temp+"  "+s.condition+"\n"+t("feels")+" "+s.feels
          +"\n"+t("high")+" "+s.high+"   "+t("low")+" "+s.low+"\n"+t("rain")+" "+s.rain+"  ·  "+t("wind")+" "+s.wind+"  ·  "+t("uv")+" "+s.uv
          +(s.forecastError.isEmpty()?"":"\n⚠ "+s.forecastError));
        sun.setText("☀ "+t("sunrise")+" "+s.sunrise+"     ☾ "+t("sunset")+" "+s.sunset);
        calendar.setText(s.events.isEmpty()?t("noEvents"):s.agendaText());
        transit.setText(!s.transportError.isEmpty()?"⚠ "+s.transportError:s.transit.isEmpty()?t("noStop"):s.transit);
        pack.setText(s.packText());status.setText(t("updated")+" "+s.updated);
    }
    private void calendarPermission(){
        if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},101);
        else Toast.makeText(this,t("calendarOK"),Toast.LENGTH_SHORT).show();
    }
    @Override public void onRequestPermissionsResult(int code,String[] perms,int[] results){
        super.onRequestPermissionsResult(code,perms,results);
        if(code==101){Toast.makeText(this,results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED?t("calendarOK"):t("calendarMissing"),Toast.LENGTH_LONG).show();refresh();}
    }
    private void navigate(Intent intent) {
        try{startActivity(intent);}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void maps() {
        String dest=p.getString("uni","").trim();if(dest.isEmpty())dest="Freie Universität Berlin";
        String url="https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(dest)+"&travelmode=transit";
        String orig=p.getString("home","").trim();if(!orig.isEmpty())url+="&origin="+Uri.encode(orig);
        navigate(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
    }
    private void openCalendar(){
        Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR);
        navigate(Intent.createChooser(i,t("calendarApp")));
    }
    private void newEvent(){Intent i=new Intent(Intent.ACTION_INSERT,CalendarContract.Events.CONTENT_URI);navigate(i);}
    private void askChatGPT(){
        if(snapshot==null){Toast.makeText(this,t("waiting"),Toast.LENGTH_SHORT).show();return;}
        String address=shareHome.isChecked()?"\nHome: "+p.getString("home",""):"";
        String payload=t("chatIntro")+"\n\n"+snapshot.city+": "+snapshot.temp+" "+snapshot.condition+
          "\nRain "+snapshot.rain+"; UV "+snapshot.uv+"; sunrise "+snapshot.sunrise+"; sunset "+snapshot.sunset+
          "\nCalendar:\n"+snapshot.agendaText()+"\nTransit:\n"+snapshot.transit+
          "\nOther plans: "+p.getString("plan","")+"\nPacking suggestions: "+snapshot.packText()+address;
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Nimbus Daily",payload));
        Toast.makeText(this,t("copied"),Toast.LENGTH_LONG).show();
        navigate(new Intent(Intent.ACTION_VIEW,Uri.parse("https://chatgpt.com/")));
    }
    @Override protected void onDestroy(){executor.shutdown();super.onDestroy();}
}