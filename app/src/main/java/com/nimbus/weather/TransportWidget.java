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

public final class TransportWidget extends AppWidgetProvider {
 private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
  if(ids.length==0)return;
  Context app=context.getApplicationContext();
  WORK.execute(()->{
   RemoteViews r=new RemoteViews(app.getPackageName(),R.layout.widget_transport);
   SharedPreferences p=WeatherWidget.prefs(app);
   String lang=p.getString("lang","de");
   DayEngine.Snapshot s=DayEngine.load(app);
   if(s.route!=null&&s.route.leaveMs>0){
    r.setTextViewText(R.id.transport_leave,UI.t(lang,"depart")+" "+s.route.leaveClock());
    r.setTextViewText(R.id.transport_route,s.route.summary+" · "+s.route.to);
   }else{
    r.setTextViewText(R.id.transport_leave,s.upcoming()==null?UI.t(lang,"noUpcoming"):s.upcoming().time+" · "+s.upcoming().title);
    r.setTextViewText(R.id.transport_route,s.route!=null&&!s.route.error.isEmpty()?s.route.error:UI.t(lang,"noRoute"));
   }
   r.setTextViewText(R.id.transport_updated,UI.t(lang,"updated")+" "+s.updated);
   r.setOnClickPendingIntent(R.id.transport_root,PendingIntent.getActivity(app,26,new Intent(app,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
   manager.updateAppWidget(new ComponentName(app,TransportWidget.class),r);
  });
 }
}