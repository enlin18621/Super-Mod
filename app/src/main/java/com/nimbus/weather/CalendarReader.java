package com.nimbus.weather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

final class CalendarReader {
    static final class Event {
        final String title,location; final long start; final boolean allDay;
        Event(String title,String location,long start,boolean allDay){this.title=title;this.location=location;this.start=start;this.allDay=allDay;}
    }
    static boolean allowed(Context c){return c.checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED;}
    static List<Event> today(Context c) {
        List<Event> result=new ArrayList<>();
        if(!allowed(c))return result;
        Calendar start=Calendar.getInstance(); start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
        Calendar end=(Calendar)start.clone();end.add(Calendar.DAY_OF_YEAR,1);
        String[] projection={CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.EVENT_LOCATION,CalendarContract.Instances.ALL_DAY};
        try(Cursor cursor=CalendarContract.Instances.query(c.getContentResolver(),projection,start.getTimeInMillis(),end.getTimeInMillis())){
            if(cursor!=null)while(cursor.moveToNext()&&result.size()<12)
                result.add(new Event(cursor.getString(0),cursor.getString(2),cursor.getLong(1),cursor.getInt(3)!=0));
        }catch(Exception ignored){}
        result.sort((a,b)->Long.compare(a.start,b.start));
        return result;
    }
}
