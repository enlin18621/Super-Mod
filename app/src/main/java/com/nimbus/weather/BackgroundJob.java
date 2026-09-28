package com.nimbus.weather;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

public class BackgroundJob extends JobService {
    private static final int JOB_ID=9200;
    /** Android controls exact execution time. Uses internet; 30-minute interval is a request, not a guarantee. */
    static void updateSchedule(Context c){
        JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(!Prefs.get(c).getBoolean("auto_alerts",false)&&!Prefs.get(c).getBoolean("sync_enabled",false)){
            scheduler.cancel(JOB_ID);return;
        }
        JobInfo info=new JobInfo.Builder(JOB_ID,new ComponentName(c,BackgroundJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(30*60*1000L,10*60*1000L)
            .setPersisted(true)
            .build();
        scheduler.schedule(info);
    }
    @Override public boolean onStartJob(JobParameters params){
        if(GitHubSync.enabled(this)){
            GitHubSync.pull(this,(ok,result)->refreshAndFinish(params));
        }else refreshAndFinish(params);
        return true;
    }
    private void refreshAndFinish(JobParameters params){
        GeoRoute.tryLastLocation(this);
        DashboardData.refresh(this,s->{
            WeatherWidget.render(this,s);
            ProactiveAlerts.evaluate(this,s);
            GitHubSync.push(this,s);
            AiBridge.maybeAutomatic(this,s,()->jobFinished(params,false));
        });
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
