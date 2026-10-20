package com.chand.minimalhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Fixed-location weather; requests only while Home is visible, at 15-minute intervals. */
public class WeatherWidget extends LinearLayout {
    private static final long INTERVAL=15*60*1000L;
    private static final String ENDPOINT="https://api.open-meteo.com/v1/forecast?latitude=17.94675&longitude=78.89559&current=temperature_2m,weather_code,is_day&timezone=Asia%2FKolkata&forecast_days=1";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final SharedPreferences cache;
    private final TextView conditions, attribution;
    private final WeatherIcon icon;
    private boolean active;
    private volatile boolean fetching;
    private long lastAttempt;
    private final Runnable refresh=new Runnable(){
        @Override public void run(){
            if(!active)return;
            showCached();
            long age=System.currentTimeMillis()-cache.getLong("updated",0);
            if(!fetching && (age>=INTERVAL || age<0) && SystemClock.elapsedRealtime()-lastAttempt>60000) fetch();
            handler.postDelayed(this,60000);
        }
    };
    public WeatherWidget(Context context){
        super(context);
        setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
        cache=context.getSharedPreferences("weather",Context.MODE_PRIVATE);
        icon=new WeatherIcon(context);
        addView(icon,new LayoutParams(dp(42),dp(42)));
        LinearLayout text=new LinearLayout(context);text.setOrientation(VERTICAL);
        TextView place=label(14,0xE6FFFFFF,600);
        place.setText("GOURAIPALLY");place.setLetterSpacing(.08f);
        conditions=label(18,0xFFF1F6FA,400);conditions.setText("Weather loading");
        attribution=label(7,0x70FFFFFF,400);attribution.setText("Open-Meteo");
        text.addView(place);text.addView(conditions);text.addView(attribution);
        LayoutParams layout=new LayoutParams(-2,-2);layout.setMargins(dp(9),0,0,0);addView(text,layout);
        setContentDescription("Gouraipally weather, provided by Open-Meteo");
        showCached();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(float size,int color,int weight){
        TextView t=new TextView(getContext());t.setTextSize(size);t.setTextColor(color);t.setSingleLine(true);
        t.setTypeface(getResources().getFont(R.font.outfit));t.setFontVariationSettings("'wght' "+weight);return t;
    }
    public void start(){
        active=true;icon.animating=true;icon.invalidate();handler.removeCallbacks(refresh);
        showCached();
        long age=System.currentTimeMillis()-cache.getLong("updated",0);
        if((age>=INTERVAL || age<0)&&!fetching)fetch();
        handler.postDelayed(refresh,60000);
    }
    public void stop(){active=false;icon.animating=false;handler.removeCallbacks(refresh);}
    private void showCached(){
        long updated=cache.getLong("updated",0);
        if(updated==0)return;
        int code=cache.getInt("code",0);
        int temp=Math.round(cache.getFloat("temperature",0));
        boolean stale=System.currentTimeMillis()-updated>2*INTERVAL;
        conditions.setText(temp+"°  "+description(code));
        attribution.setText("Open-Meteo · "+(stale?"saved ":"updated ")+new SimpleDateFormat("h:mm a",Locale.getDefault()).format(new Date(updated)));
        icon.code=code;icon.day=cache.getBoolean("day",true);icon.invalidate();
        setContentDescription("Gouraipally, "+temp+" degrees Celsius, "+description(code)+(stale?", saved weather":""));
    }
    private void fetch(){
        fetching=true;lastAttempt=SystemClock.elapsedRealtime();
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL(ENDPOINT).openConnection();
                connection.setConnectTimeout(8000);connection.setReadTimeout(8000);
                connection.setRequestProperty("User-Agent","MinimalHome/1.2 (personal Android TV)");
                if(connection.getResponseCode()!=200)throw new java.io.IOException("HTTP "+connection.getResponseCode());
                String json;
                try(InputStream in=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[2048];int length;
                    while((length=in.read(buffer))!=-1){bytes.write(buffer,0,length);if(bytes.size()>32768)throw new java.io.IOException("Response too large");}
                    json=bytes.toString("UTF-8");
                }
                JSONObject current=new JSONObject(json).getJSONObject("current");
                float temperature=(float)current.getDouble("temperature_2m");
                int code=current.getInt("weather_code");boolean day=current.getInt("is_day")==1;
                if(!Float.isFinite(temperature)||temperature < -90 || temperature>65)throw new java.io.IOException("Invalid temperature");
                cache.edit().putFloat("temperature",temperature).putInt("code",code).putBoolean("day",day)
                    .putLong("updated",System.currentTimeMillis()).apply();
                Log.i("MinimalWeather","Gouraipally updated: "+temperature+" C, WMO "+code);
                handler.post(()->{if(active)showCached();});
            }catch(Exception error){
                Log.w("MinimalWeather","Weather refresh failed",error);
                handler.post(()->{if(active){showCached();if(cache.getLong("updated",0)==0){conditions.setText("Weather unavailable");attribution.setText("Open-Meteo · retrying");}}});
            }finally{fetching=false;if(connection!=null)connection.disconnect();}
        },"weather-refresh").start();
    }
    private static String description(int code){
        if(code==0)return "Clear";if(code<=2)return "Partly cloudy";if(code==3)return "Cloudy";
        if(code==45||code==48)return "Fog";if(code>=95)return "Thunderstorms";
        if(code>=71&&code<=77||code==85||code==86)return "Snow";
        if(code>=51&&code<=57)return "Drizzle";if(code>=61)return "Rain";return "Cloudy";
    }
    private static class WeatherIcon extends View{
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);int code=-1;boolean day=true,animating;
        WeatherIcon(Context c){super(c);}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/64f,getHeight()/64f);
            float phase=(SystemClock.elapsedRealtime()%12000)/12000f;
            paint.setStyle(Paint.Style.FILL);
            boolean cloudy=code<0||code>0;
            if(code<=2&&code>=0){
                if(day){
                    canvas.save();canvas.rotate(phase*30,cloudy?23:32,cloudy?24:32);
                    paint.setColor(0xFFFFD57A);float cx=cloudy?23:32,cy=cloudy?24:32;
                    canvas.drawCircle(cx,cy,cloudy?10:12,paint);
                    paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);
                    for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(cx+(float)Math.cos(a)*17,cy+(float)Math.sin(a)*17,cx+(float)Math.cos(a)*21,cy+(float)Math.sin(a)*21,paint);}
                    canvas.restore();
                }else{
                    paint.setColor(0xFFD5E8FF);canvas.drawCircle(29,28,15,paint);
                    paint.setColor(0xFF101923);canvas.drawCircle(35,22,14,paint);
                }
            }
            if(cloudy){
                canvas.save();canvas.translate((float)Math.sin(phase*Math.PI*2)*1.5f,0);
                paint.setColor(0xFFCCDBE8);canvas.drawRoundRect(10,31,57,47,8,8,paint);
                canvas.drawCircle(24,31,11,paint);canvas.drawCircle(39,28,14,paint);canvas.drawCircle(49,36,8,paint);
                canvas.restore();
            }
            if(code>=51&&code<80||code>=80&&code<=82){
                paint.setColor(0xFF77BDEB);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);
                for(int i=0;i<3;i++){float y=48+((phase*45+i*4)%11);canvas.drawLine(23+i*11,y,20+i*11,y+5,paint);}
            }
            if(code>=95){
                paint.setColor(0xFFFFD57A);Path bolt=new Path();bolt.moveTo(34,42);bolt.lineTo(27,54);bolt.lineTo(34,53);bolt.lineTo(30,63);bolt.lineTo(44,48);bolt.lineTo(36,49);bolt.close();canvas.drawPath(bolt,paint);
            }
            canvas.restore();if(animating&&isShown())postInvalidateDelayed(80);
        }
    }
}
