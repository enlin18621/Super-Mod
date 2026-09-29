package com.nimbus.weather;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.provider.CalendarContract;
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
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST=701;
    private static final int BG=0xFFF6F8FD,WHITE=Color.WHITE,INK=0xFF18243A,MUTED=0xFF6B7890;
    private static final int BLUE=0xFF5269E9,PALE_BLUE=0xFFECF2FF,PALE_RED=0xFFFFEDF0;
    private static final int PALE_MINT=0xFFE8F7F1,PALE_PURPLE=0xFFF2EEFF,TEAL=0xFF138A75;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private SharedPreferences p;
    private String lang="de";
    private DayEngine.Snapshot snapshot;
    private int selectedDay=0,page=0;
    private LinearLayout content,root;
    private EditText nameInput,cityInput,homeInput,uniInput,stationInput,planInput,walkInput;
    private CheckBox cityLocked,fahrenheit,shareHome;
    private Spinner language;
    private boolean loading=false,destroyed=false;
    private String t(String key){return UI.t(lang,key);}
    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    private GradientDrawable bg(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private GradientDrawable outlined(int color,int radius){
        GradientDrawable d=bg(color,radius);
        d.setStroke(dp(1),0xFFE3EAF4);return d;
    }
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String value,int sp,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(sp);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setIncludeFontPadding(true);
        return v;
    }
    private void gap(LinearLayout l,int height){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);}
    private LinearLayout box(String heading){
        LinearLayout c=column();c.setPadding(dp(16),dp(15),dp(16),dp(16));c.setBackground(outlined(WHITE,20));
        if(heading!=null){c.addView(text(heading,15,INK,true));gap(c,10);}
        return c;
    }
    private void addCard(LinearLayout c,int marginBottom){
        LinearLayout.LayoutParams params=lp(-1,-2);params.bottomMargin=dp(marginBottom);content.addView(c,params);
    }
    private TextView action(String caption,Runnable callback){
        TextView v=text(caption,13,BLUE,true);v.setPadding(dp(7),dp(9),dp(7),dp(9));
        v.setOnClickListener(w->callback.run());return v;
    }
    private Button button(LinearLayout parent,String label,Runnable callback){
        Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextColor(WHITE);b.setTextSize(15);
        b.setBackgroundTintList(ColorStateList.valueOf(BLUE));b.setOnClickListener(v->callback.run());
        LinearLayout.LayoutParams l=lp(-1,50);l.topMargin=dp(8);parent.addView(b,l);return b;
    }
    private String todayKey(){
        Calendar day=Calendar.getInstance();day.add(Calendar.DATE,selectedDay);
        return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(day.getTime());
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        p=WeatherWidget.prefs(this);lang=p.getString("lang","de");
        if(!p.contains("city_locked") && p.contains("lat") && !p.getBoolean("geo_ok",false))
            p.edit().putBoolean("city_locked",true).apply();
        draw();
        if(!p.getBoolean("first_permissions_asked",false))permissions(true);
        else requestLocationAndRefresh();
        importFromIntent(getIntent());
    }
    private void permissions(boolean first){
        ArrayList<String> missing=new ArrayList<>();
        if(!LocationHelper.permitted(this)){
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
            missing.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.READ_CALENDAR);
        p.edit().putBoolean("first_permissions_asked",true).apply();
        if(missing.isEmpty()){requestLocationAndRefresh();return;}
        if(first){
            new AlertDialog.Builder(this)
              .setTitle(t("permissionTitle")).setMessage(t("permissionWhy"))
              .setPositiveButton("Weiter / Continue",(dialog,which)->requestPermissions(missing.toArray(new String[0]),PERMISSION_REQUEST))
              .setNegativeButton(t("cancel"),(dialog,which)->refresh())
              .show();
        } else requestPermissions(missing.toArray(new String[0]),PERMISSION_REQUEST);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==PERMISSION_REQUEST)requestLocationAndRefresh();
    }
    private void requestLocationAndRefresh(){
        if(!p.getBoolean("city_locked",false)&&LocationHelper.permitted(this)){
            loading=true;draw();
            LocationHelper.request(this,ok->{
                if(destroyed)return;
                if(!ok)Toast.makeText(this,t("locationUnavailable"),Toast.LENGTH_LONG).show();
                refresh();
            });
        }else refresh();
    }
    private void refresh(){
        if(destroyed)return;
        loading=true;draw();
        final int day=selectedDay;
        io.execute(()->{
            DayEngine.Snapshot result=DayEngine.load(getApplicationContext(),day);
            if(destroyed)return;
            runOnUiThread(()->{
                if(day!=selectedDay){refresh();return;}
                snapshot=result;loading=false;draw();
                if(day==0)WeatherWidget.updateAll(getApplicationContext());
            });
        });
    }
    private void draw(){
        root=column();root.setBackgroundColor(BG);
        setContentView(root);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        content=column();content.setPadding(dp(18),dp(18),dp(18),dp(24));scroll.addView(content);
        header();
        if(page==0)home();else if(page==1)planner();else if(page==2)explore();else settings();
        footer();
    }
    private void header(){
        LinearLayout r=row();
        LinearLayout left=column();
        TextView small=text("NIMBUS / DAILY",11,BLUE,true);small.setLetterSpacing(.13f);left.addView(small);
        Calendar c=Calendar.getInstance();int hour=c.get(Calendar.HOUR_OF_DAY);
        String greeting=t(hour<11?"hello":hour<18?"afternoon":"evening");
        String name=p.getString("name","").trim();
        if(!name.isEmpty())greeting+=", "+name;
        left.addView(text(greeting,24,INK,true));
        left.addView(text(t("overview"),13,MUTED,false));
        r.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        TextView refresh=text("↻",27,BLUE,true);refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(bg(WHITE,30));refresh.setOnClickListener(v->requestLocationAndRefresh());
        r.addView(refresh,lp(46,46));content.addView(r);
        gap(content,12);
        TextView status=text(loading?t("loading"):snapshot==null?t("loading"):t("updated")+" "+snapshot.updated+" · "+t("source"),11,MUTED,false);
        status.setMaxLines(2);content.addView(status);gap(content,16);
    }
    private Locale locale(){return "en".equals(lang)?Locale.ENGLISH:"zh".equals(lang)?Locale.TRADITIONAL_CHINESE:Locale.GERMAN;}
    private void days(){
        LinearLayout strip=row();
        for(int i=0;i<5;i++){
            final int target=i;
            Calendar c=Calendar.getInstance();c.add(Calendar.DATE,i);
            String day=new SimpleDateFormat("EEE",locale()).format(c.getTime());
            String date=new SimpleDateFormat("dd MMM",locale()).format(c.getTime());
            LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(1),dp(13),dp(1),dp(13));
            tile.setBackground(i==selectedDay?bg(BLUE,13):outlined(WHITE,13));
            tile.addView(text(day,12,i==selectedDay?WHITE:INK,true));
            tile.addView(text(date,10,i==selectedDay?WHITE:MUTED,false));
            tile.setOnClickListener(v->{selectedDay=target;snapshot=null;refresh();});
            LinearLayout.LayoutParams param=new LinearLayout.LayoutParams(0,dp(70),1);
            if(i>0)param.leftMargin=dp(6);strip.addView(tile,param);
        }
        content.addView(strip);gap(content,24);
    }
    private LinearLayout section(String title,Runnable seeAll){
        LinearLayout head=row();
        TextView v=text(title,18,INK,true);head.addView(v,new LinearLayout.LayoutParams(0,-2,1));
        if(seeAll!=null)head.addView(action("›",seeAll));
        content.addView(head);gap(content,10);
        return head;
    }
    private String clock(String pattern){return new SimpleDateFormat(pattern,locale()).format(new Date());}
    private void home(){
        days();
        section(t("today"),()->{page=1;draw();});
        if(snapshot==null||loading){
            LinearLayout placeholder=box(null);
            placeholder.addView(text(t("loading"),15,MUTED,false));addCard(placeholder,20);
        }else agenda(content,Math.min(5,snapshot.events.size()),true);
        gap(content,10);
        section(t("next"),()->{page=2;draw();});
        routeCard();
        gap(content,10);
        LinearLayout split=row();split.setGravity(Gravity.TOP);
        LinearLayout weatherCard=box(t("weather"));
        weatherCard.setOnClickListener(v->openWeather());
        renderWeather(weatherCard);
        LinearLayout packingCard=box(t("bring"));
        renderPacking(packingCard,4);
        LinearLayout.LayoutParams w=weight();w.rightMargin=dp(8);
        split.addView(weatherCard,w);split.addView(packingCard,weight());
        content.addView(split);gap(content,16);
        LinearLayout sun=box(null);
        sun.addView(text(t("sunrise")+"  ☀  "+(snapshot==null?"--:--":snapshot.sunrise)+"     "+t("sunset")+"  ☾  "+(snapshot==null?"--:--":snapshot.sunset),13,MUTED,false));
        addCard(sun,12);
    }
    private int categoryColor(DayEngine.Event e){
        String x=e.title.toLowerCase(Locale.ROOT);
        if(x.matches("(?s).*(uni|seminar|vorles|lecture|hochschul|大學).*"))return PALE_BLUE;
        if(x.matches("(?s).*(lunch|essen|restaurant|coffee|café|mittag|午餐).*"))return PALE_RED;
        if(x.matches("(?s).*(work|meeting|arbeit|job|besprech|工作).*"))return PALE_PURPLE;
        if(x.matches("(?s).*(gym|fitness|sport|wander|park|training|健身).*"))return PALE_MINT;
        return WHITE;
    }
    private String iconFor(DayEngine.Event e){
        int col=categoryColor(e);
        return col==PALE_BLUE?"▣":col==PALE_RED?"☕":col==PALE_PURPLE?"▦":col==PALE_MINT?"◈":"●";
    }
    private void agenda(LinearLayout target,int count,boolean compact){
        if(snapshot==null||snapshot.events.isEmpty()){
            LinearLayout c=box(null);c.addView(text(t("noEvents"),14,MUTED,false));
            LinearLayout.LayoutParams l=lp(-1,-2);l.bottomMargin=dp(12);target.addView(c,l);return;
        }
        for(int i=0;i<count;i++){
            DayEngine.Event e=snapshot.events.get(i);
            LinearLayout row=row();row.setGravity(Gravity.CENTER_VERTICAL);
            TextView clock=text(e.time,13,INK,true);clock.setGravity(Gravity.TOP|Gravity.LEFT);
            row.addView(clock,lp(63,-2));
            LinearLayout block=row();block.setPadding(dp(12),dp(12),dp(10),dp(12));
            block.setBackground(bg(categoryColor(e),15));
            TextView symbol=text(iconFor(e),22,categoryColor(e)==PALE_MINT?TEAL:BLUE,true);
            block.addView(symbol,lp(33,-2));
            LinearLayout info=column();info.addView(text(e.title,compact?14:16,INK,true));
            if(e.location!=null&&!e.location.isEmpty())info.addView(text("⌖  "+e.location,12,MUTED,false));
            block.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            if(e.location!=null&&!e.location.isEmpty()){
                block.addView(text("›",23,MUTED,false));
                block.setOnClickListener(v->openPlace(e.location));
            }
            row.addView(block,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams rp=lp(-1,-2);rp.bottomMargin=dp(9);target.addView(row,rp);
        }
    }
    private void routeCard(){
        LinearLayout card=box(null);
        DayEngine.Event next=snapshot==null?null:snapshot.upcoming();
        if(next!=null){
            String target=next.location.isEmpty()?p.getString("uni",""):next.location;
            if(snapshot.route!=null&&snapshot.route.leaveMs>0){
                JourneyPlanner.Route r=snapshot.route;
                card.addView(text("🚆  "+t("depart")+"  "+r.leaveClock(),18,BLUE,true));
                card.addView(text(r.durationMinutes+" min · "+r.to,13,MUTED,false));
                gap(card,7);
                card.addView(text(r.summary,17,INK,true));
                card.addView(text(t("walk")+" "+r.walkingMinutes+" min  ·  "+t("arrive")+" "+r.arriveClock(),12,MUTED,false));
                if(!target.isEmpty())card.setOnClickListener(v->directions(target));
            }else{
                card.addView(text(t("nextEvent")+": "+next.time+"  "+next.title,15,INK,true));
                card.addView(text(snapshot.route!=null&&!snapshot.route.error.isEmpty()?snapshot.route.error:t("noRoute"),12,MUTED,false));
                if(!target.isEmpty())card.addView(action(t("maps")+" ›",()->directions(target)));
            }
        }else card.addView(text(t("noUpcoming"),14,MUTED,false));
        addCard(card,15);
    }
    private void renderWeather(LinearLayout card){
        if(snapshot==null){card.addView(text("—",34,BLUE,true));return;}
        String temp=snapshot.temp;
        card.addView(text(temp,32,INK,true));
        card.addView(text(snapshot.forecastError.isEmpty()?snapshot.condition:snapshot.forecastError,12,MUTED,false));
        gap(card,9);
        card.addView(text(t("high")+" "+snapshot.high+" · "+t("low")+" "+snapshot.low,11,MUTED,false));
        card.addView(text(t("rain")+" "+snapshot.rain+" · UV "+snapshot.uv,11,MUTED,false));
    }
    private void renderPacking(LinearLayout card,int limit){
        if(snapshot==null){card.addView(text("…",14,MUTED,false));return;}
        Set<String> done=p.getStringSet("packed_"+todayKey(),new HashSet<>());
        int n=0;
        for(String item:snapshot.pack){
            if(n++>=limit)break;
            CheckBox check=new CheckBox(this);
            check.setButtonTintList(ColorStateList.valueOf(BLUE));
            check.setText(item);check.setTextColor(INK);check.setTextSize(12);check.setPadding(0,0,0,0);
            check.setChecked(done.contains(item));
            check.setOnCheckedChangeListener((view,checked)->{
                Set<String> updated=new HashSet<>(p.getStringSet("packed_"+todayKey(),new HashSet<>()));
                if(checked)updated.add(item);else updated.remove(item);
                p.edit().putStringSet("packed_"+todayKey(),updated).apply();
            });
            card.addView(check);
        }
        if(snapshot.pack.size()>limit)card.addView(action("+"+(snapshot.pack.size()-limit)+" ›",()->{page=1;draw();}));
    }
    private void planner(){
        days();
        section(t("planner"),()->openCalendar());
        LinearLayout intro=box(null);
        intro.addView(text(t("plannerDesc"),13,MUTED,false));
        addCard(intro,14);
        if(snapshot==null||loading){content.addView(text(t("loading"),14,MUTED,false));}
        else agenda(content,snapshot.events.size(),false);
        gap(content,10);
        LinearLayout buttons=box(null);
        button(buttons,t("editCalendar"),this::openCalendar);
        button(buttons,t("newEvent"),this::newEvent);
        addCard(buttons,15);
        if(snapshot!=null){
            section(t("things"),null);
            LinearLayout packing=box(null);renderPacking(packing,100);addCard(packing,15);
        }
    }
    private void explore(){
        section(t("next"),null);routeCard();
        LinearLayout card=box(t("nearby"));
        String station=p.getString("station","");
        if(station.trim().isEmpty()){
            card.addView(text(t("onlyVbb")+"\n"+t("station")+": "+t("settings"),14,MUTED,false));
        } else card.addView(text(snapshot==null?t("loading"):snapshot.transportError.isEmpty()?
          snapshot.transit.isEmpty()?t("loading"):snapshot.transit:snapshot.transportError,14,INK,false));
        button(card,t("maps"),()->directions(p.getString("uni","")));
        addCard(card,14);
        content.addView(text(t("onlyVbb"),12,MUTED,false));
    }
    private EditText field(LinearLayout parent,String hint,String initial){
        parent.addView(text(hint,13,MUTED,true));
        EditText e=new EditText(this);e.setText(initial);e.setTextColor(INK);e.setHintTextColor(MUTED);
        e.setTextSize(15);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(outlined(BG,11));
        LinearLayout.LayoutParams l=lp(-1,-2);l.bottomMargin=dp(14);l.topMargin=dp(5);parent.addView(e,l);
        return e;
    }
    private void settings(){
        section(t("settings"),null);
        LinearLayout c=box(null);
        String[] labels={"Deutsch","English","繁體中文"};
        language=new Spinner(this);
        ArrayAdapter<String> adapt=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);
        language.setAdapter(adapt);language.setSelection("en".equals(lang)?1:"zh".equals(lang)?2:0);
        c.addView(text(t("language"),13,MUTED,true));c.addView(language);
        gap(c,13);
        nameInput=field(c,t("profile"),p.getString("name",""));
        cityLocked=new CheckBox(this);cityLocked.setText(t("manualMode"));cityLocked.setTextColor(INK);
        cityLocked.setChecked(p.getBoolean("city_locked",false));c.addView(cityLocked);
        c.addView(text(t("autoMode")+" · "+t("yourLocation"),12,MUTED,false));gap(c,9);
        cityInput=field(c,t("manualCity"),p.getString("city",""));
        homeInput=field(c,t("homeAddr"),p.getString("home",""));
        uniInput=field(c,t("dest"),p.getString("uni",""));
        stationInput=field(c,t("station"),p.getString("station",""));
        walkInput=field(c,t("walkBuffer"),String.valueOf(p.getInt("walk_buffer",7)));
        walkInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        planInput=field(c,t("activity"),p.getString("plan",""));
        fahrenheit=new CheckBox(this);fahrenheit.setText(t("fahrenheit"));fahrenheit.setChecked(p.getBoolean("fahrenheit",false));
        fahrenheit.setTextColor(INK);c.addView(fahrenheit);
        button(c,t("save"),this::saveSettings);
        button(c,t("permissions"),()->permissions(false));
        addCard(c,14);
        LinearLayout chat=box(t("chat"));
        chat.addView(text(t("chatDetails"),13,MUTED,false));
        shareHome=new CheckBox(this);shareHome.setText(t("homeAddr")+" → ChatGPT");shareHome.setTextColor(INK);
        chat.addView(shareHome);
        button(chat,t("askChat"),this::askChatGPT);
        button(chat,t("import"),this::importClipboard);
        chat.addView(text(t("importHelp"),12,MUTED,false));
        addCard(chat,14);
        content.addView(text(t("dataLocal"),12,MUTED,false));
    }
    private void saveSettings(){
        String nextLang=language.getSelectedItemPosition()==1?"en":language.getSelectedItemPosition()==2?"zh":"de";
        boolean locked=cityLocked.isChecked();
        String query=cityInput.getText().toString().trim();
        String before=p.getString("city","");
        String newName=nameInput.getText().toString().trim();
        int walk=7;try{walk=Integer.parseInt(walkInput.getText().toString().trim());}catch(Exception ignored){}
        p.edit().putString("name",newName).putString("lang",nextLang).putBoolean("city_locked",locked)
          .putString("home",homeInput.getText().toString().trim())
          .putString("uni",uniInput.getText().toString().trim()).putString("station",stationInput.getText().toString().trim())
          .putString("plan",planInput.getText().toString().trim()).putInt("walk_buffer",Math.max(0,Math.min(45,walk)))
          .putBoolean("fahrenheit",fahrenheit.isChecked()).apply();
        lang=nextLang;
        if(locked && !query.isEmpty() && !query.equals(before)){
            Toast.makeText(this,t("saving"),Toast.LENGTH_SHORT).show();
            io.execute(()->{
                try{
                    JSONObject r=WeatherApi.searchCity(query);
                    String label=r.getString("name");String country=r.optString("country","");
                    if(!country.isEmpty())label+=", "+country;
                    p.edit().putString("city",label)
                      .putLong("lat",Double.doubleToRawLongBits(r.getDouble("latitude")))
                      .putLong("lon",Double.doubleToRawLongBits(r.getDouble("longitude"))).apply();
                    runOnUiThread(()->{Toast.makeText(this,t("saved"),Toast.LENGTH_SHORT).show();requestLocationAndRefresh();});
                }catch(Exception ex){runOnUiThread(()->{Toast.makeText(this,t("cityError"),Toast.LENGTH_LONG).show();requestLocationAndRefresh();});}
            });
        }else{
            if(locked&&!query.isEmpty())p.edit().putString("city",query).apply();
            Toast.makeText(this,t("saved"),Toast.LENGTH_SHORT).show();
            requestLocationAndRefresh();
        }
    }
    private void footer(){
        LinearLayout nav=row();nav.setBackground(outlined(WHITE,0));nav.setPadding(dp(6),dp(5),dp(6),dp(6));
        String[] icons={"⌂","▦","↗","···"};
        String[] names={t("home"),t("planner"),t("explore"),t("more")};
        for(int i=0;i<4;i++){
            final int index=i;LinearLayout item=column();item.setGravity(Gravity.CENTER);
            int color=page==i?BLUE:MUTED;
            TextView ic=text(icons[i],23,color,true);ic.setGravity(Gravity.CENTER);item.addView(ic);
            TextView label=text(names[i],11,color,page==i);label.setGravity(Gravity.CENTER);item.addView(label);
            item.setOnClickListener(v->{page=index;draw();});
            nav.addView(item,new LinearLayout.LayoutParams(0,dp(61),1));
        }
        root.addView(nav,lp(-1,-2));
    }
    private void openPlace(String location){
        if(location==null||location.trim().isEmpty())return;
        navigate(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/?api=1&query="+Uri.encode(location))));
    }
    private void directions(String target){
        String dest=target==null||target.trim().isEmpty()?p.getString("uni",""):target;
        if(dest.trim().isEmpty()){Toast.makeText(this,t("noRoute"),Toast.LENGTH_SHORT).show();return;}
        String url="https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(dest)+"&travelmode=transit";
        String origin=p.getString("home","").trim();
        if(!origin.isEmpty())url+="&origin="+Uri.encode(origin);
        navigate(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
    }
    private void openWeather(){
        if(p.getBoolean("city_locked",false)){
            String q=p.getString("city","Berlin");
            navigate(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.yr.no/en/search?q="+Uri.encode(q))));
        } else if(p.getBoolean("geo_ok",false)){
            double lat=Double.longBitsToDouble(p.getLong("lat",0)),lon=Double.longBitsToDouble(p.getLong("lon",0));
            navigate(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode("weather "+lat+","+lon))));
        }else{page=3;draw();}
    }
    private void navigate(Intent i){
        try{startActivity(i);}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void openCalendar(){navigate(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR));}
    private void newEvent(){navigate(new Intent(Intent.ACTION_INSERT,CalendarContract.Events.CONTENT_URI));}
    private String chatPayload(boolean includeHome){
        StringBuilder b=new StringBuilder();
        b.append("Erstelle mir eine realistische Tagesplanung und Packliste. Sprache: ").append(lang)
         .append(". Nutze nur geprüfte Informationen; suche bei Bedarf aktuelle Verkehrsverbindungen und frage bei Unklarheiten nach.\n");
        if(snapshot!=null){
            b.append("Datum: ").append(todayKey()).append("\nWetter: ").append(snapshot.city).append(" ").append(snapshot.temp)
             .append(", ").append(snapshot.condition).append(", Regen ").append(snapshot.rain)
             .append(", UV ").append(snapshot.uv).append(", Aufgang ").append(snapshot.sunrise).append(", Untergang ").append(snapshot.sunset)
             .append("\nTermine: \n").append(snapshot.agendaText());
            if(snapshot.route!=null&&snapshot.route.leaveMs>0)b.append("\nVBB: Ab Zuhause ").append(snapshot.route.leaveClock())
             .append(", Ankunft ").append(snapshot.route.arriveClock()).append(", Route ").append(snapshot.route.summary);
            b.append("\nPackvorschläge: ").append(snapshot.packText());
        }
        b.append("\nUni/Ziel: ").append(p.getString("uni","")).append("\nSonstige Pläne: ").append(p.getString("plan",""));
        if(includeHome)b.append("\nZuhause: ").append(p.getString("home",""));
        return b.toString();
    }
    private void askChatGPT(){
        if(snapshot==null){Toast.makeText(this,t("loading"),Toast.LENGTH_LONG).show();return;}
        boolean includeHome=shareHome!=null&&shareHome.isChecked();
        String payload=chatPayload(includeHome);
        ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Nimbus Daily",payload));
        Toast.makeText(this,t("copyDone"),Toast.LENGTH_LONG).show();
        navigate(new Intent(Intent.ACTION_VIEW,Uri.parse("https://chatgpt.com/")));
    }
    private void importClipboard(){
        ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        ClipData clip=cm.getPrimaryClip();
        if(clip==null||clip.getItemCount()==0){Toast.makeText(this,t("importEmpty"),Toast.LENGTH_LONG).show();return;}
        CharSequence data=clip.getItemAt(0).coerceToText(this);
        if(data==null){Toast.makeText(this,t("importEmpty"),Toast.LENGTH_LONG).show();return;}
        importText(data.toString());
    }
    private void importFromIntent(Intent intent){
        if(intent==null)return;
        Uri u=intent.getData();
        if(u!=null&&"nimbusdaily".equals(u.getScheme())&&"import".equals(u.getHost())){
            String data=u.getQueryParameter("data");if(data!=null)importText(data);
        }
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);importFromIntent(intent);}
    private void importText(String raw){
        try{
            if(raw.length()>10000||!raw.contains("NIMBUS_V1"))throw new Exception("format");
            int from=raw.indexOf('{'),to=raw.lastIndexOf('}');
            if(from<0||to<=from)throw new Exception("json");
            JSONObject data=new JSONObject(raw.substring(from,to+1));
            String[] fields={"name","home","uni","station","plan","city","lang"};
            StringBuilder summary=new StringBuilder();
            int found=0;
            for(String key:fields)if(data.has(key)&&data.opt(key) instanceof String){
                String value=data.optString(key,"");
                if(value.length()>700)throw new Exception("length");
                summary.append(key).append(": ").append(value).append("\n");found++;
            }
            if(data.has("locked")&&data.opt("locked") instanceof Boolean){summary.append("locked: ").append(data.getBoolean("locked")).append("\n");found++;}
            if(data.has("walk_buffer")&&data.opt("walk_buffer") instanceof Number){summary.append("walk_buffer: ").append(data.optInt("walk_buffer")).append("\n");found++;}
            if(found==0)throw new Exception("empty");
            new AlertDialog.Builder(this).setTitle(t("importReview")).setMessage(summary.toString())
                .setNegativeButton(t("cancel"),null)
                .setPositiveButton(t("apply"),(d,w)->applyImport(data))
                .show();
        }catch(Exception ex){Toast.makeText(this,t("importEmpty"),Toast.LENGTH_LONG).show();}
    }
    private void applyImport(JSONObject data){
        SharedPreferences.Editor editor=p.edit();
        for(String key:new String[]{"name","home","uni","station","plan","lang"}){
            if(data.opt(key) instanceof String){
                String value=data.optString(key,"");if(value.length()<=700)editor.putString(key,value);
            }
        }
        if(data.opt("locked") instanceof Boolean)editor.putBoolean("city_locked",data.optBoolean("locked"));
        if(data.opt("walk_buffer") instanceof Number)editor.putInt("walk_buffer",Math.max(0,Math.min(45,data.optInt("walk_buffer"))));
        editor.apply();
        String city=data.optString("city","");
        if(data.optBoolean("locked",p.getBoolean("city_locked",false))&&!city.isEmpty()){
            io.execute(()->{
                try{JSONObject r=WeatherApi.searchCity(city);
                    String label=r.getString("name");String country=r.optString("country","");
                    if(!country.isEmpty())label+=", "+country;
                    p.edit().putString("city",label).putLong("lat",Double.doubleToRawLongBits(r.getDouble("latitude")))
                      .putLong("lon",Double.doubleToRawLongBits(r.getDouble("longitude"))).apply();
                    runOnUiThread(()->{lang=p.getString("lang","de");requestLocationAndRefresh();});
                }catch(Exception ignored){runOnUiThread(()->{Toast.makeText(this,t("cityError"),Toast.LENGTH_LONG).show();requestLocationAndRefresh();});}
            });
        }else{lang=p.getString("lang","de");requestLocationAndRefresh();}
    }
    @Override protected void onDestroy(){destroyed=true;io.shutdown();super.onDestroy();}
}
