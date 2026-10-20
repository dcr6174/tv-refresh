package com.chand.minimalhome;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import java.util.Calendar;

/** Changes the system's next idle screensaver with a non-wakeup alarm. */
public class ScreensaverScheduler extends BroadcastReceiver {
    static final String AERIAL = "com.neilturner.aerialviews/.ui.screensaver.DreamActivity";
    static final String CLOCK = "com.chand.minimalhome/.ClockDream";
    static String mode(Context context) {
        return context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getString("screensaver_mode", "Auto");
    }
    static void select(Context context, String mode) {
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().putString("screensaver_mode", mode).apply();
        apply(context);
    }
    static void apply(Context context) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        String mode = mode(context);
        String scene=mode;
        if(mode.equals("Auto")) {
            if(hour>=22 || hour<7)scene="Clock";
            else if(hour<17)scene="Aerial";
            else if(hour<19)scene="Aurora";
            else if(hour<21)scene="Stars";
            else {FestivalCalendar.init(context);scene=FestivalCalendar.today().isEmpty()?"Aurora":"Festival";}
        }
        context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).edit().putString("active_screensaver_scene",scene).apply();
        String component = scene.equals("Aerial") ? AERIAL : CLOCK;
        try { context.getPackageManager().getPackageInfo("com.neilturner.aerialviews", 0); }
        catch (Exception e) { component = CLOCK; }
        try {
            Settings.Secure.putString(context.getContentResolver(), "screensaver_components", component);
            Settings.Secure.putInt(context.getContentResolver(), "screensaver_enabled", 1);
            Settings.Secure.putInt(context.getContentResolver(), "screensaver_activate_on_sleep", 1);
            Settings.Secure.putLong(context.getContentResolver(), "sleep_timeout", -1);
        } catch (SecurityException e) { android.util.Log.w("MinimalHome", "Screensaver permission missing", e); }
        Intent event = new Intent(context, ScreensaverScheduler.class).setAction("com.chand.minimalhome.SCREENSAVER_SCHEDULE");
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, event, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pending);
        if (!mode.equals("Auto")) return;
        Calendar next = (Calendar)now.clone();
        next.set(Calendar.MINUTE, 0); next.set(Calendar.SECOND, 0); next.set(Calendar.MILLISECOND, 0);
        if (hour < 7) next.set(Calendar.HOUR_OF_DAY, 7);
        else if (hour < 17) next.set(Calendar.HOUR_OF_DAY, 17);
        else if (hour < 19) next.set(Calendar.HOUR_OF_DAY, 19);
        else if (hour < 21) next.set(Calendar.HOUR_OF_DAY, 21);
        else if (hour < 22) next.set(Calendar.HOUR_OF_DAY, 22);
        else { next.add(Calendar.DATE, 1); next.set(Calendar.HOUR_OF_DAY, 7); }
        alarm.setExact(AlarmManager.RTC, next.getTimeInMillis(), pending);
    }
    @Override public void onReceive(Context context, Intent intent) { apply(context); }
}
