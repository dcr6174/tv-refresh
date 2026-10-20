package com.chand.minimalhome;
import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Dated local calendar. Lunar holidays never repeat on a fixed Gregorian date. */
final class FestivalCalendar {
    private static JSONObject dates = new JSONObject();
    private static boolean loaded;
    static synchronized void init(Context context) {
        if (loaded) return;
        loaded = true;
        try (InputStream in = context.getAssets().open("festivals.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            dates = new JSONObject(bytes.toString("UTF-8")).getJSONObject("dates");
        } catch (Exception e) { android.util.Log.w("MinimalHome", "Festival calendar unavailable", e); }
    }
    static String today() {
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        date.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        return dates.optString(date.format(new Date()), "");
    }
}
