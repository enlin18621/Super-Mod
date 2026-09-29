package com.nimbus.weather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherWidget extends AppWidgetProvider {
    static final String ACTION_REFRESH="com.nimbus.weather.REFRESH";
    private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("nimbus_settings",Context.MODE_PRIVATE);}
    static void updateAll(Context c){updateAll(c,null);}
    static void updateAll(Context c,PendingResult pending){
        Context app=c.getApplicationContext();AppWidgetManager manager=AppWidgetManager.getInstance(app);
        int[] ids=manager.getAppWidgetIds(new ComponentName(app,WeatherWidget.class));
        if(ids.length==0){if(pending!=null)pending.finish();return;}
        SharedPreferences p=prefs(app);
        RemoteViews loading=shell(app,p);
        loading.setTextViewText(R.id.condition,"↻");
        manager.updateAppWidget(ids,loading);
        EXEC.execute(()->{
            try{
                DayEngine.Snapshot s=DayEngine.load(app);
                RemoteViews v=shell(app,p);
                String lang=p.getString("lang","de");
                v.setTextViewText(R.id.city, ("de".equals(lang)?"HEUTE": "en".equals(lang)?"TODAY":"今天")+" · "+s.city);
                v.setTextViewText(R.id.temp,s.temp);
                v.setTextViewText(R.id.condition,s.condition);
                v.setTextViewText(R.id.feels,L10n.t(lang,"feels")+" "+s.feels);
                v.setTextViewText(R.id.details,UI.t(lang,"rain")+" "+s.rain);
                StringBuilder agenda=new StringBuilder();
                for(int j=0;j<Math.min(3,s.events.size());j++){DayEngine.Event ev=s.events.get(j);if(j>0)agenda.append("\\n");agenda.append(ev.time).append(" ").append(ev.title);}
                v.setTextViewText(R.id.agenda,s.events.isEmpty()?UI.t(lang,"noEvents"):agenda.toString());
                StringBuilder packing=new StringBuilder();
                for(int j=0;j<Math.min(3,s.pack.size());j++){if(j>0)packing.append("\\n");packing.append("☐ ").append(s.pack.get(j));}
                v.setTextViewText(R.id.pack,packing.toString());
                if(s.route!=null&&s.route.leaveMs>0)v.setTextViewText(R.id.next,UI.t(lang,"depart")+" "+s.route.leaveClock()+" · "+s.route.summary);
                else if(s.upcoming()!=null)v.setTextViewText(R.id.next,s.upcoming().time+" · "+s.upcoming().title);
                else v.setTextViewText(R.id.next,UI.t(lang,"noUpcoming"));
                v.setTextViewText(R.id.updated,L10n.t(lang,"updated")+" "+s.updated);
                manager.updateAppWidget(ids,v);
            }catch(Exception ex){
                RemoteViews err=shell(app,p);
                err.setTextViewText(R.id.condition,"⚠ "+ex.getMessage());
                manager.updateAppWidget(ids,err);
            }finally{if(pending!=null)pending.finish();}
        });
    }
    private static RemoteViews shell(Context c,SharedPreferences p){
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_weather);
        v.setTextViewText(R.id.city,p.getString("city","Berlin").toUpperCase(java.util.Locale.ROOT));
        Intent open=new Intent(c,MainActivity.class);
        v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.edit,PendingIntent.getActivity(c,2,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        Intent refresh=new Intent(c,WeatherWidget.class).setAction(ACTION_REFRESH);
        v.setOnClickPendingIntent(R.id.refresh,PendingIntent.getBroadcast(c,3,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){updateAll(c);}
    @Override public void onReceive(Context c,Intent i){
        String action=i.getAction();
        if(ACTION_REFRESH.equals(action)||AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action))updateAll(c,goAsync());else super.onReceive(c,i);
    }
}