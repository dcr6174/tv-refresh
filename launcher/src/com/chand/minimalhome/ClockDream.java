package com.chand.minimalhome;

import android.service.dreams.DreamService;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/** System-managed idle clock. No network, wakelock or always-running service. */
public class ClockDream extends DreamService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private FrameLayout root;
    private LinearLayout face;
    private TextView greeting, clock, date;
    private boolean running;
    private int minute = -1;
    private long nextMove;
    private AmbientBackdrop ambient;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            Calendar now = Calendar.getInstance();
            if (minute != now.get(Calendar.MINUTE)) {
                minute = now.get(Calendar.MINUTE);
                boolean is24 = android.text.format.DateFormat.is24HourFormat(ClockDream.this);
                clock.setText(new SimpleDateFormat(is24 ? "H:mm" : "h:mm a", Locale.getDefault()).format(now.getTime()));
                date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now.getTime()));
                int h = now.get(Calendar.HOUR_OF_DAY);
                String theme=getSharedPreferences("MainActivity",MODE_PRIVATE).getString("theme","Auto");
                greeting.setText(ThemePalette.festive(theme)?"HAPPY "+ThemePalette.resolve(theme).toUpperCase(Locale.getDefault()):h < 5 ? "GOOD NIGHT" : h < 12 ? "GOOD MORNING" : h < 17 ? "GOOD AFTERNOON" : "GOOD EVENING");
            }
            if (System.currentTimeMillis() >= nextMove && root.getWidth() > 0 && face.getWidth() > 0) {
                float dx = Math.max(0, (root.getWidth() - face.getWidth()) / 2f - dp(72));
                float dy = Math.max(0, (root.getHeight() - face.getHeight()) / 2f - dp(72));
                face.animate().translationX((random.nextFloat()*2-1)*dx)
                    .translationY((random.nextFloat()*2-1)*dy).setDuration(9000).start();
                nextMove = System.currentTimeMillis()+45000;
            }
            handler.postDelayed(this, 1000);
        }
    };
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView label(float size, String family, int color) {
        TextView view = new TextView(this);
        view.setTextSize(size);
        view.setTypeface(Typeface.create(getResources().getFont(R.font.outfit),family.contains("thin")?200:family.contains("medium")?500:300,false));
        view.setFontVariationSettings("'wght' "+(family.contains("thin")?200:family.contains("medium")?500:300));
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        return view;
    }
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FestivalCalendar.init(this);
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        root = new FrameLayout(this);
        String theme=getSharedPreferences("MainActivity",MODE_PRIVATE).getString("theme","Ocean Blue");
        int vivid=ThemePalette.color(theme);
        int accent=Color.rgb(Color.red(vivid)/6,Color.green(vivid)/6,Color.blue(vivid)/6);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF03070C, accent, Color.BLACK}));
        String scene=getSharedPreferences("MainActivity",MODE_PRIVATE).getString("active_screensaver_scene","Clock");
        if(scene.equals("Aurora")||scene.equals("Stars")){
            root.setBackgroundColor(Color.BLACK);
            ambient=new AmbientBackdrop(this,scene);
            root.addView(ambient,new FrameLayout.LayoutParams(-1,-1));
        }else if(scene.equals("Festival")){
            root.addView(new FestivalDecor(this,FestivalCalendar.today().isEmpty()?"Deepavali":FestivalCalendar.today()),new FrameLayout.LayoutParams(-1,-1));
        }
        face = new LinearLayout(this);
        face.setOrientation(LinearLayout.VERTICAL);
        face.setGravity(Gravity.CENTER);
        greeting = label(11, "sans-serif-medium", 0xFF8CACC5);
        greeting.setLetterSpacing(.3f);
        clock = label(100, "sans-serif-thin", 0xFFE7EFF5);
        clock.setIncludeFontPadding(false);
        date = label(17, "sans-serif-light", 0xFF8E9DAA);
        date.setLetterSpacing(.08f);
        face.addView(greeting);
        face.addView(clock);
        face.addView(date);
        root.addView(face, new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));
        setContentView(root);
    }
    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        running = true;
        minute = -1;
        nextMove = System.currentTimeMillis()+4000;
        handler.post(tick);
        if(ambient!=null)ambient.start();
    }
    @Override public void onDreamingStopped() {
        running = false;
        handler.removeCallbacks(tick);
        if(ambient!=null)ambient.stop();
        if (face != null) face.animate().cancel();
        super.onDreamingStopped();
    }
    @Override public void onDetachedFromWindow() {
        running = false;
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }
}
