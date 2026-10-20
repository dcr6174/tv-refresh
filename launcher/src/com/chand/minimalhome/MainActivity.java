package com.chand.minimalhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Apple TV style home screen: clock top-left, pills top-right, the focused dock app's name over an
 * ambient glow, a dock of pinned apps, and every other app in a grid that scrolls up on Down.
 * Plain framework views only (no AndroidX), with weather refreshed while Home is visible.
 */
public class MainActivity extends Activity {

    // Dock, hidden apps, Live TV and grid width come from res/values/config.xml.
    private String[] dockApps;
    private WeatherWidget weatherWidget;
    private final List<App> orderedApps = new ArrayList<>();
    private final List<App> userHiddenApps = new ArrayList<>();
    private String lastAppPackage, lastFavoritePackage;
    private boolean rebuildingApps;
    private String movingPackage, orderBeforeMove;
    private boolean ignoreMoveRelease;
    private boolean consumeLongPressUp;
    private boolean expandedApps;
    private int returnGridScroll;
    private boolean restoringGrid;
    private final Set<String> hidden = new HashSet<>();
    private String liveTvPkg, inputPickerPkg;
    private int cols;

    static final class App {
        String pkg, label;
        ComponentName component;
        Intent shortcut;
        Drawable art;
        boolean isBanner;
        int color;
    }

    private final Map<String, Integer> colorCache = new HashMap<>();
    private final List<View> dockCards = new ArrayList<>();
    private final List<View> gridCards = new ArrayList<>();
    private final List<View> pills = new ArrayList<>();
    private final List<View> headerPills = new ArrayList<>();
    private TextView headerWifi;
    private TextView cleanupButton;
    private volatile boolean cleanupRunning;
    private PetalShower petals;
    private View pendingConfirm;
    private final Runnable confirmHold=new Runnable(){public void run(){
        View target=pendingConfirm;
        if(target!=null && target.hasFocus() && hasWindowFocus() && target.getTag() instanceof App && inputsPopup==null && movingPackage==null){
            pendingConfirm=null;consumeLongPressUp=true;target.performLongClick();
        }
    }};
    private void cancelConfirm(){handler.removeCallbacks(confirmHold);pendingConfirm=null;}

    private ImageView festivalWallpaper;
    private Bitmap durgaWallpaper;

    private HomeScrollView scroll;
    private LinearLayout content, dockRow;
    private FrameLayout hero, sidebar;
    private boolean sidebarOpen;
    private ConfirmGate sidebarConfirm=new ConfirmGate(false);
    private TextView allAppsPill;
    private FestivalDecor festivalDecor;
    private String renderedTheme = "";
    private LinearLayout grid;
    private View glow;
    private View gridShade;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView greeting, clock, seconds, ampm, date, title, wifiPill, appsLabel, contextHint;
    private String connectedWifiName = "Wi-Fi";
    private boolean clockRunning;
    private final SimpleDateFormat clock12Format = new SimpleDateFormat("h:mm", Locale.getDefault());
    private final SimpleDateFormat clock24Format = new SimpleDateFormat("H:mm", Locale.getDefault());
    private final SimpleDateFormat dayDateFormat = new SimpleDateFormat("EEEE'\n'd MMMM", Locale.getDefault());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (!clockRunning) return;
            updateClock();
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
        }
    };
    private int glowColor = 0xFF3A3A3A;
    private ValueAnimator glowAnim;
    private int lastFocusedId = View.NO_ID;
    private boolean firstShow = true;
    private int packageSeq;
    private MenuOverlay inputsPopup;
    private FrameLayout rootView;

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateClock(); updateWifi(); }
    };
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { buildApps(); }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if(b!=null){expandedApps=b.getBoolean("all_apps_open",false);returnGridScroll=b.getInt("all_apps_scroll",0);}
        FestivalCalendar.init(this);
        if(!getPreferences(MODE_PRIVATE).getBoolean("v2_auto_enabled",false))
            getPreferences(MODE_PRIVATE).edit().putString("theme","Auto").putBoolean("v2_auto_enabled",true).apply();
        if (!getPreferences(MODE_PRIVATE).getBoolean("festival_auto_added", false))
            getPreferences(MODE_PRIVATE).edit().putString("theme", "Auto").putBoolean("festival_auto_added", true).apply();
        lastAppPackage = getPreferences(MODE_PRIVATE).getString("last_app", "shortcut.hdmi1");
        lastFavoritePackage = getPreferences(MODE_PRIVATE).getString("last_favorite", "shortcut.hdmi1");
        String theme = ThemePalette.normalize(getPreferences(MODE_PRIVATE).getString("theme", "Ocean Blue"));
        getPreferences(MODE_PRIVATE).edit().putString("theme", theme).apply();
        dockApps = getResources().getStringArray(R.array.dock_apps);
        hidden.addAll(Arrays.asList(getResources().getStringArray(R.array.hidden_apps)));
        hidden.add(getPackageName());
        liveTvPkg = getString(R.string.live_tv_package);
        inputPickerPkg = getString(R.string.input_picker_package);
        cols = getResources().getInteger(R.integer.columns);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(buildUi());
        buildApps();

        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addDataScheme("package");
        registerReceiver(packages, pf);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tf = new IntentFilter(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        tf.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(tick, tf);
        clockRunning = true;
        handler.removeCallbacks(clockTick);
        clockTick.run();
        updateWifi();
        ScreensaverScheduler.apply(this);
        if(weatherWidget!=null)weatherWidget.start();
        String theme=getPreferences(MODE_PRIVATE).getString("theme","Auto");
        String entrance=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date())+ThemePalette.resolve(theme);
        if(ThemePalette.festive(theme)&&(!theme.equals("Auto")||!FestivalCalendar.today().isEmpty())&&!entrance.equals(getPreferences(MODE_PRIVATE).getString("last_petals",""))){
            getPreferences(MODE_PRIVATE).edit().putString("last_petals",entrance).apply();
            petals.post(()->petals.start(ThemePalette.resolve(theme)));
        }
        boolean returningToGrid=expandedApps;
        int previousScroll=returnGridScroll;
        // Catch installs/removals that happened while another app was in front.
        if (getPackageManager().getChangedPackages(packageSeq) != null) buildApps();
        setAppExpansion(returningToGrid);
        if(returningToGrid)restoreGridSelection(previousScroll);else restoreSelection(false);
        if (firstShow) { firstShow = false; playEntrance(); }
        if(!getPreferences(MODE_PRIVATE).getBoolean("cleaner_setup_requested",false)){
            getPreferences(MODE_PRIVATE).edit().putBoolean("cleaner_setup_requested",true).apply();
            prepareCleaner();
        }
        // New photo on return if the current one has been up for the full interval.

    }

    @Override
    protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);
        out.putBoolean("all_apps_open",expandedApps);
        out.putInt("all_apps_scroll",expandedApps?scroll.getScrollY():0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelConfirm();
        if(expandedApps)returnGridScroll=scroll.getScrollY();
        clockRunning = false;
        handler.removeCallbacks(clockTick);
        if(weatherWidget!=null)weatherWidget.stop();
        if(petals!=null)petals.stop();
        consumeLongPressUp=false;
        unregisterReceiver(tick);
        if (inputsPopup != null) inputsPopup.dismiss();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(clockTick);
        unregisterReceiver(packages);
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && scroll!=null)scroll.post(()->{
            View focused=getCurrentFocus();
            if(inputsPopup==null && (focused==null || !focused.isShown())){
                if(sidebarOpen && !pills.isEmpty())pills.get(0).requestFocus();
                else restoreSelection(expandedApps);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        cancelConfirm();
        returnGridScroll=0;
        if (sidebarOpen) closeSidebar();
        movingPackage = null;
        if (inputsPopup != null) inputsPopup.dismiss();
        consumeLongPressUp=false;ignoreMoveRelease=false;
        setAppExpansion(false);restoreSelection(false);
        scroll.smoothScrollTo(0, 0);
    }

    @Override
    public void onBackPressed() {
        if (sidebarOpen) { closeSidebar(); return; }
        if (movingPackage != null) {
            if (orderBeforeMove == null) getPreferences(MODE_PRIVATE).edit().remove("ordered_apps").apply();
            else getPreferences(MODE_PRIVATE).edit().putString("ordered_apps", orderBeforeMove).apply();
            movingPackage = null;
            contextHint.setVisibility(View.INVISIBLE);
            buildApps();
            android.widget.Toast.makeText(this, "Move cancelled", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if(expandedApps) {
            setAppExpansion(false);
            restoreSelection(false);
            scroll.smoothScrollTo(0,0);
            return;
        }
        // Home has nowhere to go back to; Back from the grid returns to the dock.
        if (scroll.getScrollY() > 0 && !dockCards.isEmpty()) dockCards.get(0).requestFocus();
    }

    // ---------------------------------------------------------------- layout

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        rootView=root;
        root.setBackgroundColor(Color.BLACK);
        festivalWallpaper=new ImageView(this);
        festivalWallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(festivalWallpaper,new FrameLayout.LayoutParams(-1,-1));

        // Static dark background: no image decoding, network requests or wallpaper timers.
        gridShade = new View(this);
        gridShade.setBackgroundColor(0xB3000000);
        gridShade.setAlpha(0f);
        root.addView(gridShade, new FrameLayout.LayoutParams(-1, -1));
        glow = new View(this);
        root.addView(glow, new FrameLayout.LayoutParams(-1, -1));
        festivalDecor = new FestivalDecor(this);
        root.addView(festivalDecor, new FrameLayout.LayoutParams(-1, -1));
        petals=new PetalShower(this);petals.setVisibility(View.INVISIBLE);
        root.addView(petals,new FrameLayout.LayoutParams(-1,-1));
        setGlow(glowColor);

        scroll = new HomeScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));

        // Hero: exactly one screen tall, header on top, title + dock at the bottom.
        hero = new FrameLayout(this);
        hero.setClipChildren(false);
        int screenH = getResources().getDisplayMetrics().heightPixels;
        content.addView(hero, new LinearLayout.LayoutParams(-1, screenH));

        hero.addView(buildHeader(), lp(-1, -2, Gravity.TOP, dp(48), dp(36), dp(48), 0));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setClipChildren(false);
        bottom.setClipToPadding(false);
        hero.addView(bottom, lp(-1, -2, Gravity.BOTTOM, dp(100), 0, dp(100), dp(24)+(int)(screenH*.12f)));

        title = text(30, "sans-serif-light", Color.WHITE);
        title.setShadowLayer(dp(18), 0, 0, 0x66000000);
        title.setSingleLine(true);
        title.setVisibility(View.GONE);
        bottom.addView(title, new LinearLayout.LayoutParams(-2, -2));
        ((LinearLayout.LayoutParams) title.getLayoutParams()).setMargins(dp(8), 0, 0, dp(14));

        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER);
        dockRow.setClipChildren(false);
        dockRow.setClipToPadding(false);
        dockRow.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable band = new GradientDrawable();
        band.setCornerRadius(dp(26));
        band.setColor(0x09FFFFFF);
        band.setStroke(dp(1), 0x0FFFFFFF);
        dockRow.setBackground(band);
        bottom.addView(dockRow, new LinearLayout.LayoutParams(-1, -2));

        appsLabel = text(11, "sans-serif-medium", 0x99FFFFFF);
        appsLabel.setText("ALL APPS");
        appsLabel.setLetterSpacing(0.22f);
        content.addView(appsLabel, llp(-2, -2, dp(120), dp(36), 0, dp(24)));

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        content.addView(grid, llp(-1, -2, dp(120), 0, dp(120), dp(100)));
        allAppsPill=text(11,"sans-serif-medium",0x80FFFFFF);
        allAppsPill.setText("↓  All apps");allAppsPill.setGravity(Gravity.CENTER);
        allAppsPill.setLetterSpacing(.06f);
        allAppsPill.setId(View.generateViewId());allAppsPill.setFocusable(false);allAppsPill.setClickable(true);
        allAppsPill.setContentDescription("All apps. Move down to open.");
        allAppsPill.setAlpha(0);allAppsPill.post(()->allAppsPill.animate().alpha(.7f).setDuration(700).start());
        allAppsPill.setOnClickListener(v->openAllApps());
        hero.addView(allAppsPill,lp(-2,dp(28),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,0,0,dp(48)));
        contextHint = text(11, "sans-serif-medium", 0xB3FFFFFF);
        contextHint.setGravity(Gravity.CENTER);
        contextHint.setVisibility(View.INVISIBLE);
        root.addView(contextHint, lp(-1, dp(28), Gravity.BOTTOM, 0, 0, 0, dp(14)));
        buildSidebar(root);
        return root;
    }

    private View buildHeader() {
        FrameLayout header = new FrameLayout(this);
        header.setClipChildren(false);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        greeting = text(13, "sans-serif-medium", 0xB3FFFFFF);
        greeting.setLetterSpacing(0.25f);
        greeting.setSingleLine(true);
        greeting.setPadding(0, 0, dp(16), 0);
        left.addView(greeting);

        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout time = new LinearLayout(this);
        time.setGravity(Gravity.BOTTOM);
        clockRow.addView(time, new LinearLayout.LayoutParams(dp(160), dp(50)));
        clock = text(36, "sans-serif-light", Color.WHITE);
        clock.setIncludeFontPadding(false);
        clock.setSingleLine(true);
        clock.setFontFeatureSettings("tnum");
        time.addView(clock);
        seconds = text(19, "sans-serif-light", 0xCCFFFFFF);
        seconds.setIncludeFontPadding(false);
        seconds.setSingleLine(true);
        seconds.setFontFeatureSettings("tnum");
        time.addView(seconds, llp(dp(30), -2, dp(2), 0, 0, dp(4)));
        ampm = text(10, "sans-serif-medium", 0xB3FFFFFF);
        ampm.setIncludeFontPadding(false);
        time.addView(ampm, llp(-2, -2, dp(3), 0, 0, dp(8)));
        left.addView(clockRow, llp(-1, dp(54), 0, dp(3), 0, 0));

        date = text(16, "sans-serif-medium", 0xF2FFFFFF);
        date.setFontVariationSettings("'wght' 650");
        date.setIncludeFontPadding(false);
        date.setLines(2);
        date.setLetterSpacing(0.04f);
        date.setLineSpacing(dp(3), 1f);
        View divider = new View(this);
        divider.setBackgroundColor(0x33FFFFFF);
        clockRow.addView(divider, llp(dp(1), dp(34), dp(6), 0, dp(12), 0));



        clockRow.addView(date, new LinearLayout.LayoutParams(dp(132), -2));
        for (TextView t : new TextView[]{greeting, clock, seconds, ampm, date}) t.setShadowLayer(dp(10), 0, dp(1), 0xB3000000);
        header.addView(left, new FrameLayout.LayoutParams(dp(355), -2, Gravity.START | Gravity.TOP));

        LinearLayout right=new LinearLayout(this);right.setGravity(Gravity.CENTER_VERTICAL);right.setClipChildren(false);
        TextView menu=pill("Clean");cleanupButton=menu;menu.setOnClickListener(v->cleanNow());
        menu.setContentDescription("Clear caches and recent app tasks");
        TextView inputs=pill("Inputs");inputs.setOnClickListener(v->showInputs(v));
        headerWifi=pill("Wi-Fi");headerWifi.setText(connectedWifiName);headerWifi.setCompoundDrawablePadding(dp(7));
        headerWifi.setMaxEms(13);headerWifi.setEllipsize(android.text.TextUtils.TruncateAt.END);
        headerWifi.setOnClickListener(v->launch(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        TextView settings=pill("Settings");settings.setOnClickListener(v->showQuickSettings(v));
        TextView themes=pill("Themes");themes.setOnClickListener(v->showThemes(v));
        for(TextView button:new TextView[]{menu,inputs,headerWifi,settings,themes}){
            right.addView(button,llp(button==headerWifi?-2:dp(32),dp(32),dp(6),0,0,0));headerPills.add(button);
        }
        header.addView(right,lp(-2,-2,Gravity.END|Gravity.TOP,0,dp(12),0,0));

        return header;
    }

    private void cleanerApproval(){runOnUiThread(()->android.widget.Toast.makeText(this,"Allow USB debugging on the TV once; select Always allow",android.widget.Toast.LENGTH_LONG).show());}

    private void prepareCleaner(){
        if(cleanupRunning)return;cleanupRunning=true;
        new Thread(()->{
            try(LocalAdb adb=TvCleaner.connect(this,this::cleanerApproval)){
                if(!adb.shell("echo CLEANER_READY").contains("CLEANER_READY"))throw new java.io.IOException("Setup check failed");
                getPreferences(MODE_PRIVATE).edit().putBoolean("cleaner_ready",true).apply();
                runOnUiThread(()->android.widget.Toast.makeText(this,"Fan ready · one tap clears caches and recent apps",android.widget.Toast.LENGTH_LONG).show());
            }catch(Exception e){
                android.util.Log.w("MinimalHome","Cleaner setup pending: "+e.getClass().getSimpleName());
                runOnUiThread(()->android.widget.Toast.makeText(this,"Tap the fan to retry debugging approval",android.widget.Toast.LENGTH_LONG).show());
            }finally{cleanupRunning=false;}
        },"Cleaner-setup").start();
    }

    private void cleanNow(){
        if(cleanupRunning){android.widget.Toast.makeText(this,"Cleanup or initial approval is already in progress",android.widget.Toast.LENGTH_SHORT).show();return;}
        cleanupRunning=true;cleanupButton.setAlpha(.5f);
        CacheVortex vortex=new CacheVortex(this,cleanupButton);rootView.addView(vortex,new FrameLayout.LayoutParams(-1,-1));vortex.start();
        android.widget.Toast.makeText(this,"Clearing caches and recent apps…",android.widget.Toast.LENGTH_SHORT).show();
        new Thread(()->{
            String result;
            try{result=TvCleaner.clean(this,this::cleanerApproval);getPreferences(MODE_PRIVATE).edit().putBoolean("cleaner_ready",true).apply();}
            catch(Exception e){android.util.Log.w("MinimalHome","Cleanup failed: "+e.getClass().getSimpleName());result="Cleanup needs USB debugging approval · tap to retry";}
            final String message=result;
            runOnUiThread(()->{cleanupRunning=false;if(cleanupButton!=null)cleanupButton.setAlpha(1);android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();});
        },"TV-cleanup").start();
    }

    private void buildSidebar(FrameLayout root) {
        sidebar = new FrameLayout(this);
        sidebar.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xFF111B28,0xFC0B111B}));
        sidebar.setElevation(dp(24)); sidebar.setVisibility(View.INVISIBLE);
        sidebar.setTranslationX(-dp(316));
        root.addView(sidebar,new FrameLayout.LayoutParams(dp(300),-1,Gravity.START));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(48),dp(32),dp(20),dp(24));body.setClipChildren(false);body.setClipToPadding(false);sidebar.addView(body,new FrameLayout.LayoutParams(-1,-1));
        TextView label=text(13,"sans-serif-medium",0x99FFFFFF);label.setText("MINIMAL HOME");label.setLetterSpacing(.18f);
        body.addView(label,llp(-1,-2,dp(10),0,0,dp(22)));
        String[] names={"Apps","Inputs","Settings","Themes"};
        String[] details={"Your complete app library","Choose a TV source","Quick controls","Auto, colors and Telugu festivals"};
        int[] icons={R.drawable.ic_apps,R.drawable.ic_inputs,R.drawable.ic_settings,R.drawable.ic_palette};
        for(int i=0;i<names.length;i++) {
            final int item=i;
            View row=sourceCard(names[i],details[i],-1,icons[i],()->{
                if(item==0) {closeSidebar();setAppExpansion(true);if(!gridCards.isEmpty())gridCards.get(0).requestFocus();}
                else if(item==1)showInputs(pills.get(item));
                else if(item==2)showQuickSettings(pills.get(item));
                else showThemes(pills.get(item));
            });
            pills.add(row);body.addView(row,llp(-1,dp(54),0,dp(2),0,dp(2)));
        }
        View line=new View(this);line.setBackgroundColor(0x1AFFFFFF);body.addView(line,llp(-1,dp(1),dp(10),dp(22),dp(10),dp(12)));
        weatherWidget=new WeatherWidget(this);body.addView(weatherWidget,llp(-1,dp(64),dp(10),0,0,dp(12)));
        wifiPill=text(13,"sans-serif-medium",0xB3FFFFFF);wifiPill.setText(connectedWifiName);
        Drawable wifi=getDrawable(R.drawable.ic_wifi).mutate();wifi.setBounds(0,0,dp(17),dp(17));
        wifiPill.setCompoundDrawables(wifi,null,null,null);wifiPill.setCompoundDrawablePadding(dp(10));
        wifiPill.setPadding(dp(12),0,dp(12),0);wifiPill.setGravity(Gravity.CENTER_VERTICAL);
        wifiPill.setId(View.generateViewId());wifiPill.setFocusable(true);wifiPill.setFocusableInTouchMode(true);
        wifiPill.setOnFocusChangeListener((v,has)->{v.setBackgroundColor(has?0x24FFFFFF:Color.TRANSPARENT);});
        wifiPill.setOnClickListener(v->launch(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        pills.add(wifiPill);body.addView(wifiPill,llp(-1,dp(38),0,0,0,0));
        TextView hint=text(10,"sans-serif-light",0x66FFFFFF);hint.setText("→ OR BACK TO CLOSE");hint.setLetterSpacing(.1f);
        body.addView(hint,llp(-1,-2,dp(12),dp(16),0,0));
    }

    private void openSidebar() {
        if(sidebarOpen)return;sidebarOpen=true;sidebarConfirm=new ConfirmGate(false);
        sidebar.animate().cancel();sidebar.setVisibility(View.VISIBLE);
        sidebar.animate().translationX(0).setDuration(210).setInterpolator(new DecelerateInterpolator()).start();
        scroll.animate().alpha(.4f).setDuration(180).start();
        pills.get(0).requestFocus();
    }
    private void closeSidebar() {
        if(!sidebarOpen)return;sidebarOpen=false;
        sidebar.animate().translationX(-dp(316)).setDuration(180).withEndAction(()->{if(!sidebarOpen)sidebar.setVisibility(View.INVISIBLE);}).start();
        scroll.animate().alpha(1).setDuration(180).start();
        restoreSelection(true);
    }

    private TextView pill(String label) {
        TextView p = text(11, "sans-serif-medium", 0xE6FFFFFF);
        p.setText("");
        p.setContentDescription(label);
        p.setSingleLine(true);
        Drawable artwork = getDrawable(label.equals("Clean") ? R.drawable.ic_fan : label.equals("Menu") ? R.drawable.ic_menu : label.equals("Inputs") ? R.drawable.ic_inputs : label.equals("Wi-Fi") ? R.drawable.ic_wifi
                : label.equals("Themes") ? R.drawable.ic_palette : R.drawable.ic_settings).mutate();
        final Drawable icon;
        if(label.equals("Clean")){
            android.graphics.drawable.RotateDrawable fan=new android.graphics.drawable.RotateDrawable();
            fan.setDrawable(artwork);fan.setFromDegrees(0);fan.setToDegrees(1080);
            fan.setPivotXRelative(true);fan.setPivotYRelative(true);fan.setPivotX(.5f);fan.setPivotY(.5f);
            icon=fan;
        }else icon=artwork;
        icon.setBounds(0, 0, dp(14), dp(14));
        p.setCompoundDrawables(icon, null, null, null);
        p.setCompoundDrawablePadding(0);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(9), 0, dp(9), 0);
        p.setFocusable(true);p.setFocusableInTouchMode(true);
        p.setId(View.generateViewId());
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xC010151D);
        bg.setStroke(dp(1), 0x2EFFFFFF);
        p.setBackground(bg);
        p.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0xF2FFFFFF : 0xC010151D);
            icon.setTint(has ? 0xFF111111 : 0xE6FFFFFF);
            ((TextView) v).setTextColor(has ? 0xFF111111 : 0xE6FFFFFF);
            v.animate().scaleX(has ? 1.04f : 1f).scaleY(has ? 1.04f : 1f)
                    .setInterpolator(new OvershootInterpolator(2f)).setDuration(220).start();
            if (contextHint != null) {
                contextHint.setText(label.equals("Wi-Fi") ? "Wi-Fi · " + connectedWifiName
                        : label);
                contextHint.setVisibility(has ? View.VISIBLE : View.INVISIBLE);
            }
            if (has) { lastFocusedId = v.getId(); scroll.smoothScrollTo(0, 0); fadeGlow(1f); }
        });
        return p;
    }

    // ---------------------------------------------------------------- apps

    private void buildApps() {
        rebuildingApps = true;
        PackageManager pm = getPackageManager();
        android.content.pm.ChangedPackages changed = pm.getChangedPackages(packageSeq);
        if (changed != null) packageSeq = changed.getSequenceNumber();
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Map<String, App> byPkg = new HashMap<>();
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            ActivityInfo ai = ri.activityInfo;
            if (hidden.contains(ai.packageName) || byPkg.containsKey(ai.packageName)) continue;
            App a = new App();
            a.pkg = ai.packageName;
            a.label = String.valueOf(ri.loadLabel(pm));
            a.component = new ComponentName(ai.packageName, ai.name);
            Drawable banner = ai.loadBanner(pm);
            if (banner == null) banner = ai.applicationInfo.loadBanner(pm);
            a.isBanner = banner != null;
            a.art = banner != null ? banner : ri.loadIcon(pm);
            a.color = sampleColor(a.pkg, a.art);
            byPkg.put(a.pkg, a);
        }

        List<App> dock = new ArrayList<>();
        App live = new App();
        live.pkg = "shortcut.hdmi1";
        live.label = "Live TV";
        live.art = getDrawable(R.drawable.ic_tv);
        live.color = 0xFF508FCA;
        live.shortcut = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(
                "com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744384"));
        dock.add(live);
        for (String p : dockApps) if (byPkg.containsKey(p)) dock.add(byPkg.remove(p));
        // Ensure the first screen always has launch targets even when preferred apps are absent.
        if (dock.isEmpty()) {
            List<App> fallback = new ArrayList<>(byPkg.values());
            Collections.sort(fallback, (x, y) -> x.label.compareToIgnoreCase(y.label));
            for (int i = 0; i < Math.min(4, fallback.size()); i++) {
                dock.add(fallback.get(i));
                byPkg.remove(fallback.get(i).pkg);
            }
        }
        List<App> rest = new ArrayList<>(byPkg.values());
        Collections.sort(rest, (x, y) -> {
            List<String> priority = Arrays.asList("com.graymatrix.did", "com.sonyliv", "ahaflix.tv", "org.videolan.vlc");
            int xi = priority.indexOf(x.pkg), yi = priority.indexOf(y.pkg);
            if (xi < 0) xi = 100;
            if (yi < 0) yi = 100;
            return xi != yi ? Integer.compare(xi, yi) : x.label.compareToIgnoreCase(y.label);
        });

        orderedApps.clear();
        Map<String, App> available = new HashMap<>();
        for (App app : dock) available.put(app.pkg, app);
        for (App app : rest) available.put(app.pkg, app);
        String savedOrder = getPreferences(MODE_PRIVATE).getString("ordered_apps", "");
        for (String pkg : savedOrder.split(",")) {
            App app = available.remove(pkg);
            if (app != null) orderedApps.add(app);
        }
        for (App app : dock) if (available.remove(app.pkg) != null) orderedApps.add(app);
        for (App app : rest) if (available.remove(app.pkg) != null) orderedApps.add(app);
        Set<String> userHidden = getPreferences(MODE_PRIVATE).getStringSet("user_hidden", Collections.emptySet());
        userHiddenApps.clear();
        for (App app : orderedApps) if (userHidden.contains(app.pkg)) userHiddenApps.add(app);
        orderedApps.removeAll(userHiddenApps);
        dock.clear(); rest.clear();
        for (int i=0; i<orderedApps.size(); i++) {
            (i<5 ? dock : rest).add(orderedApps.get(i));
        }
        List<String> orderLog=new ArrayList<>();for(App app:orderedApps)orderLog.add(app.pkg);
        android.util.Log.i("MinimalHome","Order: "+String.join(",",orderLog));

        dockRow.removeAllViews();
        grid.removeAllViews();
        dockCards.clear();
        gridCards.clear();

        for (int i = 0; i < dock.size(); i++) {
            View tile = tile(dock.get(i), true);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(i == 0 ? 0 : dp(8), 0, i == dock.size() - 1 ? 0 : dp(8), 0);
            dockRow.addView(tile, p);
        }
        for (int i = dock.size(); i < 5; i++) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 1, 1f);
            p.setMargins(i == 0 ? 0 : dp(8), 0, i == 4 ? 0 : dp(8), 0);
            dockRow.addView(new View(this), p);
        }

        LinearLayout row = null;
        for (int i = 0; i < orderedApps.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                grid.addView(row, llp(-1, -2, 0, 0, 0, dp(26)));
            }
            View tile = tile(orderedApps.get(i), false);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.setMargins(i % cols == 0 ? 0 : dp(8), 0, i % cols == cols - 1 ? 0 : dp(8), 0);
            row.addView(tile, p);
        }
        // Pad the last row so tiles keep their width.
        if (row != null) for (int i = orderedApps.size() % cols; i != 0 && i < cols; i++) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 1, 1f);
            p.setMargins(dp(8), 0, i == cols - 1 ? 0 : dp(8), 0);
            row.addView(new View(this), p);
        }
        setAppExpansion(expandedApps);
        wireFocus();

        if (!dock.isEmpty()) { title.setText(dock.get(0).label); setGlow(dock.get(0).color); }
        restoreSelection(true);
        if (movingPackage != null) {
            List<View> cards = new ArrayList<>(dockCards); cards.addAll(gridCards);
            for (View card : cards) if (((App)card.getTag()).pkg.equals(movingPackage)) {
                if(cards.indexOf(card)>=5)setAppExpansion(true);
                card.requestFocus();
                title.setText("Move " + ((App)card.getTag()).label + "  ·  arrows to move  ·  OK to save");
                showMoveHint((App)card.getTag());
                break;
            }
        }
        rebuildingApps = false;
    }

    private void rememberApp(App app, boolean favorite) {
        lastAppPackage = app.pkg;
        if (favorite) lastFavoritePackage = app.pkg;
        if (!app.pkg.equals(getPreferences(MODE_PRIVATE).getString("last_app", ""))) {
            getPreferences(MODE_PRIVATE).edit().putString("last_app", lastAppPackage)
                    .putString("last_favorite", lastFavoritePackage).apply();
        }
    }

    private void restoreGridSelection(int y){
        View selected=null;
        for(View card:gridCards)if(((App)card.getTag()).pkg.equals(lastAppPackage)){selected=card;break;}
        if(selected==null&&!gridCards.isEmpty())selected=gridCards.get(0);
        if(selected!=null){setAppExpansion(true);restoringGrid=true;selected.requestFocus();restoringGrid=false;scroll.post(()->scroll.scrollTo(0,y));}
        else {setAppExpansion(false);restoreSelection(false);}
    }

    private void restoreSelection(boolean includeOtherApps) {
        String wanted = includeOtherApps ? lastAppPackage : lastFavoritePackage;
        if(includeOtherApps&&expandedApps){restoreGridSelection(returnGridScroll);return;}
        List<View> cards = new ArrayList<>(dockCards);
        if (includeOtherApps) cards.addAll(gridCards);
        View selected = null;
        for (View card : cards) if (((App)card.getTag()).pkg.equals(wanted)) { selected = card; break; }
        if (selected == null && !dockCards.isEmpty()) selected = dockCards.get(0);
        if (selected != null) {
            boolean favorite = dockCards.contains(selected);
            setAppExpansion(!favorite);
            selected.requestFocus();
            if (!rebuildingApps) rememberApp((App)selected.getTag(), favorite);
        }
    }

    /** A 16:9 card with the app's banner; the name shows under it while focused. */
    private void openAllApps(){
        if(gridCards.isEmpty())return;
        View selected=gridCards.get(0);
        for(View card:gridCards)if(((App)card.getTag()).pkg.equals(lastFavoritePackage)){selected=card;break;}
        setAppExpansion(true);selected.requestFocus();scroll.scrollTo(0,0);
    }

    private void setAppExpansion(boolean expanded) {
        expandedApps=expanded;
        if(grid!=null)grid.setVisibility(expanded?View.VISIBLE:View.GONE);
        if(appsLabel!=null) { appsLabel.setVisibility(expanded?View.VISIBLE:View.GONE); appsLabel.setText("ALL APPS"); }
        if(hero!=null)hero.setVisibility(expanded?View.GONE:View.VISIBLE);
    }

    private View tile(App app, boolean inDock) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);

        FrameLayout card = new AspectFrame(this);
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setTag(app);
        card.setContentDescription(app.label + ". Hold OK to customize.");
        card.setOnLongClickListener(v -> {
            if(inputsPopup!=null)return true;
            consumeLongPressUp=true;
            showAppMenu(app, v);
            return true;
        });
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(10));
            }
        });
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF151B24);
        card.setBackground(bg);

        ImageView img = new ImageView(this);
        img.setImageDrawable(app.art);
        if (app.isBanner) {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
            img.setPadding(dp(16), dp(10), dp(16), dp(10));
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
            img.setPadding(dp(22), dp(14), dp(22), dp(14));
        }

        // Focus ring and sheen, drawn above the art.
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(10));
        ring.setStroke(dp(2), ThemePalette.highlight(getPreferences(MODE_PRIVATE).getString("theme", "Ocean Blue")));
        ring.setOrientation(GradientDrawable.Orientation.TL_BR);
        ring.setColors(new int[]{0x26FFFFFF, 0x00FFFFFF, 0x00FFFFFF});
        View shine = new View(this);
        shine.setBackground(ring);
        shine.setAlpha(0f);
        card.addView(shine, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout surface = new AspectFrame(this);
        surface.setClipChildren(false);
        surface.setClipToPadding(false);
        View halo = new SelectionGlow(this, ThemePalette.color(getPreferences(MODE_PRIVATE).getString("theme", "Ocean Blue")));
        halo.setAlpha(0f);
        FrameLayout.LayoutParams haloLayout = new FrameLayout.LayoutParams(-1, -1);
        haloLayout.setMargins(-dp(18), -dp(18), -dp(18), -dp(18));
        surface.addView(halo, haloLayout);
        surface.addView(card, new FrameLayout.LayoutParams(-1, -2));
        box.addView(surface, new LinearLayout.LayoutParams(-1, -2));

        TextView label = text(18, "sans-serif-medium", Color.WHITE);
        label.setFontVariationSettings("'wght' 650");
        label.setShadowLayer(dp(4),0,dp(2),0xFF000000);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setText(app.label);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        box.addView(label, llp(-1, -2, 0, dp(6), 0, 0));

        card.setOnClickListener(v -> launch(app.shortcut != null ? app.shortcut : new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)));
        card.setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.10f : 1f;
            v.animate().cancel();
            v.animate().scaleX(s).scaleY(s).translationY(has ? -dp(4) : 0).translationZ(has ? dp(14) : 0)
                    .setInterpolator(has ? new DecelerateInterpolator(1.6f) : new DecelerateInterpolator())
                    .setDuration(has ? 130 : 100).start();
            shine.animate().alpha(has ? 1f : 0f).setDuration(80).start();
            halo.animate().alpha(has ? 1f : 0f).scaleX(has ? 1.07f : 1f).scaleY(has ? 1.07f : 1f)
                    .translationY(has ? -dp(4) : 0).setDuration(120).start();
            label.animate().alpha(has ? 1f : 0f).setDuration(80).start();
            if (!has) return;
            if (contextHint != null) contextHint.setVisibility(View.INVISIBLE);
            if (!rebuildingApps) rememberApp(app, inDock);
            if (movingPackage != null) showMoveHint(app);
            lastFocusedId = v.getId();
            if (inDock) {
                if (movingPackage == null) setAppExpansion(false);
                title.setText(app.label);
                title.animate().alpha(1f).setDuration(200).start();
                animateGlow(app.color);
                fadeGlow(1f);
                if(scroll.getScrollY()!=0)scroll.scrollTo(0,0);
            } else {
                fadeGlow(0.75f);
                title.setText(app.label);
                if(!restoringGrid)scrollToRow(box);
            }
        });

        (inDock ? dockCards : gridCards).add(card);
        return box;
    }

    /** A small cached shadow behind the selected card; no continuously running effect. */
    private final class SelectionGlow extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        SelectionGlow(Context context, int color) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            paint.setColor(withAlpha(color, 0.07f));
            paint.setShadowLayer(dp(12), 0, dp(2), withAlpha(color, 0.55f));
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = dp(16);
            canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, dp(12), dp(12), paint);
        }
    }

    /** Explicit D-pad targets: one row per press, ends of rows and of the top bar don't wrap. */
    private void wireFocus() {
        int d = dockCards.size(), g = gridCards.size();
        for (int i = 0; i < pills.size(); i++) {
            View p = pills.get(i);
            p.setNextFocusUpId(pills.get(Math.max(0,i-1)).getId());
            p.setNextFocusDownId(pills.get(Math.min(pills.size()-1,i+1)).getId());
            p.setNextFocusLeftId(p.getId()); p.setNextFocusRightId(p.getId());
        }
        for(int i=0;i<headerPills.size();i++){
            View p=headerPills.get(i);p.setNextFocusLeftId(headerPills.get(Math.max(0,i-1)).getId());
            p.setNextFocusRightId(headerPills.get(Math.min(headerPills.size()-1,i+1)).getId());
            p.setNextFocusUpId(p.getId());p.setNextFocusDownId(d>0?dockCards.get(Math.min(d-1,i)).getId():p.getId());
        }
        int topId = !headerPills.isEmpty()?headerPills.get(0).getId():d > 0 ? dockCards.get(0).getId() : View.NO_ID;
        for (int i = 0; i < d; i++) {
            View c = dockCards.get(i);
            c.setNextFocusUpId(headerPills.get(Math.min(i,headerPills.size()-1)).getId());
            c.setNextFocusLeftId(i == 0 ? c.getId() : dockCards.get(i - 1).getId());
            c.setNextFocusRightId(i == d - 1 ? c.getId() : dockCards.get(i + 1).getId());
            c.setNextFocusDownId(g > 0 ? gridCards.get(Math.min(i, g - 1)).getId() : c.getId());
        }
        int lastRow = (g - 1) / cols;
        for (int j = 0; j < g; j++) {
            View c = gridCards.get(j);
            int r = j / cols, col = j % cols;
            c.setNextFocusUpId(r == 0
                    ? (d > 0 ? dockCards.get(Math.min(col, d - 1)).getId() : topId)
                    : gridCards.get(j - cols).getId());
            c.setNextFocusDownId(j + cols < g ? gridCards.get(j + cols).getId()
                    : r < lastRow ? gridCards.get(g - 1).getId() : c.getId());
            c.setNextFocusLeftId(col == 0 ? c.getId() : gridCards.get(j - 1).getId());
            c.setNextFocusRightId(col == cols - 1 || j == g - 1 ? c.getId() : gridCards.get(j + 1).getId());
        }
    }

    private void scrollToRow(View tileBox) {
        grid.post(() -> {
            if (!expandedApps) return;
            View row = (View) tileBox.getParent();
            int y = grid.getTop() + row.getTop();
            int screenH = getResources().getDisplayMetrics().heightPixels;
            int target = y - (int)(screenH * (grid.indexOfChild(row) == 0 ? 0.72f : 0.42f));
            int max = Math.max(0, content.getHeight() - scroll.getHeight());
            scroll.smoothScrollTo(0, Math.max(0, Math.min(target, max)));
        });
    }

    // ---------------------------------------------------------------- inputs

    private static final class MenuAction {
        final String name, detail;
        final int icon;
        final Runnable action;
        MenuAction(String name, String detail, int icon, Runnable action) {
            this.name = name; this.detail = detail; this.icon = icon; this.action = action;
        }
    }

    private final class MenuOverlay {
        final FrameLayout shade;
        final View anchor;
        final List<View> rows=new ArrayList<>();
        final ConfirmGate confirm=new ConfirmGate(consumeLongPressUp);
        int selected;
        MenuOverlay(FrameLayout shade,View anchor){this.shade=shade;this.anchor=anchor;}
        void dismiss(){
            rootView.removeView(shade);
            if(inputsPopup==this)inputsPopup=null;
            consumeLongPressUp=false;
            if(anchor!=null && anchor.isShown())anchor.requestFocus();
            else if(sidebarOpen && !pills.isEmpty())pills.get(0).requestFocus();
            else restoreSelection(true);
        }
        void move(int delta){
            int current=rows.indexOf(getCurrentFocus());if(current>=0)selected=current;
            selected=Math.max(0,Math.min(rows.size()-1,selected+delta));
            if(!rows.isEmpty())rows.get(selected).requestFocus();
        }
        void select(){
            int current=rows.indexOf(getCurrentFocus());if(current>=0)selected=current;
            if(!rows.isEmpty())rows.get(selected).performClick();
        }
    }

    private void showOptions(View anchor, String heading, List<MenuAction> options) {
        if(inputsPopup!=null)inputsPopup.dismiss();
        FrameLayout shade=new FrameLayout(this);shade.setBackgroundColor(0x66000000);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16),dp(18),dp(16),dp(16));panel.setClipChildren(false);panel.setClipToPadding(false);
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF202B39,0xFF111923});
        background.setCornerRadius(dp(20));background.setStroke(dp(1),0x38FFFFFF);panel.setBackground(background);
        TextView eyebrow=text(9,"sans-serif-medium",0x88FFFFFF);eyebrow.setText("MINIMAL HOME");eyebrow.setLetterSpacing(.18f);
        panel.addView(eyebrow,llp(-1,-2,dp(12),0,0,dp(6)));
        TextView caption=text(22,"sans-serif-medium",0xF2FFFFFF);caption.setText(heading);
        caption.setMaxLines(2);panel.addView(caption,llp(-1,-2,dp(12),0,dp(12),dp(12)));
        android.widget.ScrollView listScroll=new android.widget.ScrollView(this);
        listScroll.setVerticalScrollBarEnabled(false);listScroll.setClipToPadding(true);
        listScroll.setPadding(dp(4),dp(4),dp(4),dp(4));
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setClipChildren(false);list.setClipToPadding(false);
        listScroll.addView(list);
        panel.addView(listScroll,new LinearLayout.LayoutParams(-1,0,1f));
        MenuOverlay popup=new MenuOverlay(shade,anchor);
        for(MenuAction option:options){
            View row=sourceCard(option.name,option.detail,-1,option.icon,()->{popup.dismiss();option.action.run();});
            list.addView(row,llp(-1,dp(60),0,dp(3),0,dp(3)));popup.rows.add(row);
        }
        int safeHeight=Math.max(dp(200),getResources().getDisplayMetrics().heightPixels-dp(96));
        int panelHeight=Math.min(safeHeight,dp(112)+options.size()*dp(66));
        int panelWidth=Math.min(dp(350),getResources().getDisplayMetrics().widthPixels-dp(96));
        shade.setClipChildren(true);shade.setClipToPadding(true);
        shade.addView(panel,lp(panelWidth,panelHeight,Gravity.END|Gravity.CENTER_VERTICAL,dp(48),dp(48),dp(48),dp(48)));
        shade.setOnClickListener(v->popup.dismiss());panel.setClickable(true);
        inputsPopup=popup;rootView.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        panel.setAlpha(0);panel.setTranslationX(dp(12));panel.animate().alpha(1).translationX(0).setDuration(160).start();
        if(!popup.rows.isEmpty())popup.rows.get(0).requestFocus();
        shade.post(()->{if(inputsPopup==popup&&!popup.rows.isEmpty())popup.rows.get(0).requestFocus();});
        android.util.Log.i("MinimalHome","Panel opened: "+heading);
    }

    private void showMoveHint(App app) {
        contextHint.setText("Move " + app.label + " · Arrows to move · OK saves · Back cancels");
        contextHint.setVisibility(View.VISIBLE);
    }

    private void persistOrder() {
        List<String> packages = new ArrayList<>();
        for (App app : orderedApps) packages.add(app.pkg);
        getPreferences(MODE_PRIVATE).edit().putString("ordered_apps", String.join(",", packages)).apply();
    }

    private void showAppMenu(App app, View anchor) {
        List<MenuAction> options = new ArrayList<>();
        options.add(new MenuAction("Move", "Reorder with the arrow buttons", R.drawable.ic_move, () -> {
            movingPackage = app.pkg;
            orderBeforeMove = getPreferences(MODE_PRIVATE).getString("ordered_apps", null);
            ignoreMoveRelease = false;
            anchor.requestFocus();
            showMoveHint(app);
            android.util.Log.i("MinimalHome", "Reorder started: " + app.pkg);
        }));
        if (orderedApps.indexOf(app) >= 5) {
            options.add(new MenuAction("Add to favorites", "Choose a favorite to replace", R.drawable.ic_star, () -> {
                List<MenuAction> slots = new ArrayList<>();
                for (int i = 0; i < Math.min(5, orderedApps.size()); i++) {
                    App favorite = orderedApps.get(i);
                    slots.add(new MenuAction(favorite.label, "Replace in the favorites row", R.drawable.ic_star,
                            () -> swapFavorite(app, favorite)));
                }
                showOptions(anchor, "Add " + app.label + " to favorites", slots);
            }));
        } else {
            options.add(new MenuAction("Replace favorite", "Choose another installed app", R.drawable.ic_star, () -> {
                List<MenuAction> choices = new ArrayList<>();
                for (int i = 5; i < orderedApps.size(); i++) {
                    App other = orderedApps.get(i);
                    choices.add(new MenuAction(other.label, "Add to this favorite slot", R.drawable.ic_star,
                            () -> swapFavorite(other, app)));
                }
                if (choices.isEmpty()) android.widget.Toast.makeText(this, "No other visible apps", android.widget.Toast.LENGTH_SHORT).show();
                else showOptions(anchor, "Replace " + app.label, choices);
            }));
        }
        options.add(new MenuAction("Hide", "Hide this tile; keep the app installed", R.drawable.ic_hide, () -> {
            if (orderedApps.size() <= 1) {
                android.widget.Toast.makeText(this, "Keep at least one visible app", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Set<String> hiddenApps = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("user_hidden", Collections.emptySet()));
            hiddenApps.add(app.pkg);
            getPreferences(MODE_PRIVATE).edit().putStringSet("user_hidden", hiddenApps).apply();
            buildApps();
            android.util.Log.i("MinimalHome", "Hidden: " + app.pkg);
        }));
        options.add(new MenuAction("Hidden apps", userHiddenApps.isEmpty() ? "No apps hidden" : "Restore a hidden app",
                R.drawable.ic_restore, () -> {
            List<MenuAction> choices = new ArrayList<>();
            for (App hiddenApp : userHiddenApps) {
                choices.add(new MenuAction(hiddenApp.label, "Show this app again", R.drawable.ic_restore, () -> {
                    Set<String> hiddenApps = new HashSet<>(getPreferences(MODE_PRIVATE).getStringSet("user_hidden", Collections.emptySet()));
                    hiddenApps.remove(hiddenApp.pkg);
                    getPreferences(MODE_PRIVATE).edit().putStringSet("user_hidden", hiddenApps).apply();
                    lastAppPackage = hiddenApp.pkg;
                    buildApps();
                    android.util.Log.i("MinimalHome", "Restored: " + hiddenApp.pkg);
                }));
            }
            if (choices.isEmpty()) android.widget.Toast.makeText(this, "No apps are hidden", android.widget.Toast.LENGTH_SHORT).show();
            else showOptions(anchor, "Hidden apps", choices);
        }));
        showOptions(anchor, app.label, options);
    }

    private void swapFavorite(App added, App replaced) {
        int from = orderedApps.indexOf(added), to = orderedApps.indexOf(replaced);
        if (from < 0 || to < 0) return;
        Collections.swap(orderedApps, from, to);
        persistOrder();
        lastAppPackage = added.pkg;
        lastFavoritePackage = added.pkg;
        buildApps();
        rememberApp(added, true);
        android.util.Log.i("MinimalHome", "Favorite added: " + added.pkg);
    }

    private void applyTheme(String theme) {
        getPreferences(MODE_PRIVATE).edit().putString("theme",theme).apply();
        setGlow(ThemePalette.color(theme));
        buildApps();
        if(ThemePalette.festive(theme)&&(!theme.equals("Auto")||!FestivalCalendar.today().isEmpty()))petals.start(ThemePalette.resolve(theme));
        if(sidebarOpen && !pills.isEmpty())pills.get(3).requestFocus();
        android.widget.Toast.makeText(this, theme.equals("Auto") ? "Auto · "+ThemePalette.resolve(theme) : theme, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void showThemes(View anchor) {
        String current=getPreferences(MODE_PRIVATE).getString("theme","Auto");
        List<MenuAction> options=new ArrayList<>();
        options.add(new MenuAction("Auto"+(current.equals("Auto")?"  ✓":""),
            "Now: "+ThemePalette.resolve("Auto")+" · festivals + time of day",R.drawable.ic_palette,()->applyTheme("Auto")));
        options.add(new MenuAction("Color themes", "Five vivid colors + Aurora", R.drawable.ic_palette, ()->{
            List<MenuAction> colors=new ArrayList<>();
            for(String name:ThemePalette.NAMES)if(!name.equals("Auto"))
                colors.add(new MenuAction(name+(current.equals(name)?"  ✓":""),"Ambient color and matching focus glow",R.drawable.ic_palette,()->applyTheme(name)));
            showOptions(anchor,"Color themes",colors);
        }));
        options.add(new MenuAction("Telugu festivals", "Choose a festive look at any time", R.drawable.ic_star, ()->{
            List<MenuAction> festivals=new ArrayList<>();
            for(String name:ThemePalette.FESTIVALS)
                festivals.add(new MenuAction(name+(current.equals(name)?"  ✓":""),"Festive details · original app logos",R.drawable.ic_star,()->applyTheme(name)));
            showOptions(anchor,"Festival themes",festivals);
        }));
        showOptions(anchor,"Themes",options);
    }

    private void showScreensavers(View anchor) {
        List<MenuAction> options=new ArrayList<>();
        String current=ScreensaverScheduler.mode(this);
        options.add(new MenuAction("Auto"+(current.equals("Auto")?"  ✓":""),"Aerial day · aurora/stars evening · clock night",R.drawable.ic_palette,()->ScreensaverScheduler.select(this,"Auto")));
        options.add(new MenuAction("Aerial videos"+(current.equals("Aerial")?"  ✓":""),"Quiet landscapes · starts after 5 idle minutes",R.drawable.ic_tv,()->ScreensaverScheduler.select(this,"Aerial")));
        options.add(new MenuAction("Drifting clock"+(current.equals("Clock")?"  ✓":""),"Dim clock · festival colors",R.drawable.ic_home,()->ScreensaverScheduler.select(this,"Clock")));
        options.add(new MenuAction("Soft aurora"+(current.equals("Aurora")?"  ✓":""),"Slow teal and violet ambient light",R.drawable.ic_palette,()->ScreensaverScheduler.select(this,"Aurora")));
        options.add(new MenuAction("Quiet starfield"+(current.equals("Stars")?"  ✓":""),"Dim stars and a drifting clock",R.drawable.ic_star,()->ScreensaverScheduler.select(this,"Stars")));
        options.add(new MenuAction("Festival ambience"+(current.equals("Festival")?"  ✓":""),"Current festival · warm lamps and rangoli",R.drawable.ic_star,()->ScreensaverScheduler.select(this,"Festival")));
        options.add(new MenuAction("Aerial Views settings","Video categories and overlays",R.drawable.ic_settings,()->openPackage("com.neilturner.aerialviews")));
        showOptions(anchor,"Screensavers",options);
    }

    private void showQuickSettings(View anchor) {
        List<MenuAction> options=new ArrayList<>();
        options.add(new MenuAction("TV settings","Picture, sound and system",R.drawable.ic_settings,()->launch(new Intent(Settings.ACTION_SETTINGS))));
        options.add(new MenuAction("Network",connectedWifiName,R.drawable.ic_wifi,()->launch(new Intent(Settings.ACTION_WIFI_SETTINGS))));
        options.add(new MenuAction("Screensavers","Auto · aerial video and drifting clock",R.drawable.ic_tv,()->showScreensavers(anchor)));
        showOptions(anchor,"Settings",options);
    }

    private void showInputs(View anchor) {
        Intent source = new Intent("com.android.tv.action.VIEW_INPUTS")
                .setComponent(new ComponentName("com.tcl.tv", "com.tcl.sourcemenu.sourcemanager.MainActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!launch(source)) openPackage(liveTvPkg);
    }
    private View sourceCard(String name, String subtitle, int state, int iconResource, Runnable action) {
        final int accent = ThemePalette.highlight(getPreferences(MODE_PRIVATE).getString("theme", "Ocean Blue"));
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12),dp(4),dp(12),dp(4));
        row.setId(View.generateViewId()); row.setFocusable(true); row.setFocusableInTouchMode(true); row.setClickable(true);
        row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        GradientDrawable background=new GradientDrawable();
        background.setCornerRadius(dp(12)); background.setColor(0x05FFFFFF);
        background.setStroke(dp(1), 0x10FFFFFF);
        row.setBackground(background);
        ImageView icon=new ImageView(this);
        icon.setImageResource(iconResource); icon.setColorFilter(0xCCFFFFFF);
        row.addView(icon,new LinearLayout.LayoutParams(dp(23),dp(23)));
        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        TextView titleText=text(14,"sans-serif-medium",Color.WHITE);
        titleText.setText(name); titleText.setSingleLine(true);titleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView detail=text(10,"sans-serif",0x80FFFFFF); detail.setText(subtitle);detail.setSingleLine(true);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        names.addView(titleText); names.addView(detail);
        LinearLayout.LayoutParams namesParams=new LinearLayout.LayoutParams(0,-2,1f);
        namesParams.setMargins(dp(12),0,dp(8),0); row.addView(names,namesParams);
        TextView status=text(9,"sans-serif-medium",state==TvInputManager.INPUT_STATE_CONNECTED?0xFF8ACCB1:0x7FFFFFFF);
        status.setText(state==TvInputManager.INPUT_STATE_CONNECTED?"CONNECTED":state==TvInputManager.INPUT_STATE_DISCONNECTED?"NO SIGNAL":state>=0?"STANDBY":"›");
        if(state<0)status.setTextSize(20);
        row.addView(status);
        row.setOnFocusChangeListener((view,focused) -> {
            background.setColor(focused?withAlpha(accent,.13f):0x05FFFFFF);
            background.setStroke(dp(1),focused?withAlpha(accent,.8f):0x10FFFFFF);
            titleText.setTextColor(Color.WHITE);
            detail.setTextColor(focused?0xC7DDE9F7:0x88FFFFFF);
            icon.setColorFilter(focused?accent:0xB3FFFFFF);
            status.setTextColor(focused?accent:0x66FFFFFF);
            view.setTranslationX(0);
        });
        row.setOnClickListener(view -> action.run());
        return row;
    }

    // ---------------------------------------------------------------- status

    private void updateClock() {
        Calendar now = Calendar.getInstance();
        int h = now.get(Calendar.HOUR_OF_DAY);
        String selectedTheme=getPreferences(MODE_PRIVATE).getString("theme","Auto");
        String resolved=ThemePalette.resolve(selectedTheme);
        if(!resolved.equals(renderedTheme)) {
            boolean initial=renderedTheme.isEmpty();
            renderedTheme=resolved; setGlow(ThemePalette.color(selectedTheme));
            if(!initial && !rebuildingApps && !dockCards.isEmpty())buildApps();
        }
        setTextIfChanged(greeting, ThemePalette.festive(selectedTheme) ? "HAPPY "+resolved.toUpperCase(Locale.getDefault()) : h < 5 ? "GOOD NIGHT" : h < 12 ? "GOOD MORNING" : h < 17 ? "GOOD AFTERNOON" : "GOOD EVENING");
        boolean is24 = android.text.format.DateFormat.is24HourFormat(this);
        SimpleDateFormat timeFormat = is24 ? clock24Format : clock12Format;
        timeFormat.setTimeZone(now.getTimeZone());
        dayDateFormat.setTimeZone(now.getTimeZone());
        setTextIfChanged(clock, timeFormat.format(now.getTime()));
        int second = now.get(Calendar.SECOND);
        setTextIfChanged(seconds, ":" + (second < 10 ? "0" : "") + second);
        setTextIfChanged(ampm, is24 ? "" : (h < 12 ? "AM" : "PM"));
        setTextIfChanged(date, dayDateFormat.format(now.getTime()).toUpperCase(Locale.getDefault()));
    }

    private static void setTextIfChanged(TextView view, String value) {
        if (!value.contentEquals(view.getText())) view.setText(value);
    }

    private void updateWifi() {
        String name = "Wi-Fi";
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null && wm.getConnectionInfo() != null) {
                String s = wm.getConnectionInfo().getSSID();
                if (s != null && !s.contains("unknown ssid")) name = s.replace("\"", "");
                int networkId=wm.getConnectionInfo().getNetworkId();
                if (name.equals("Wi-Fi") && networkId>=0) {
                    List<android.net.wifi.WifiConfiguration> networks=wm.getConfiguredNetworks();
                    if(networks!=null)for(android.net.wifi.WifiConfiguration network:networks)
                        if(network.networkId==networkId && network.SSID!=null)name=network.SSID.replace("\"", "");
                }
                if(name.equals("Wi-Fi")) {
                    android.net.ConnectivityManager connectivity=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
                    android.net.NetworkInfo info=connectivity.getActiveNetworkInfo();
                    if(info!=null && info.getExtraInfo()!=null && !info.getExtraInfo().contains("unknown"))name=info.getExtraInfo().replace("\"", "");
                }
                if (networkId == -1) name = "Offline";
            }
        } catch (SecurityException ignored) { }
        connectedWifiName = name;
        if(headerWifi!=null){headerWifi.setText(name);headerWifi.setContentDescription("Wi-Fi: "+name);}
        if(wifiPill==null)return;
        wifiPill.setText(name);
        wifiPill.setContentDescription("Wi-Fi: " + name);
        if (wifiPill.hasFocus() && contextHint != null) contextHint.setText("Wi-Fi · " + name);
    }

    // ---------------------------------------------------------------- glow

    private void setGlow(int color) {
        String theme=getPreferences(MODE_PRIVATE).getString("theme","Auto");
        String name=ThemePalette.resolve(theme);
        boolean durga=name.equals("Dussehra") || name.startsWith("Navaratri");
        if(festivalWallpaper!=null){
            if(!durga && durgaWallpaper!=null){
                festivalWallpaper.setImageDrawable(null);durgaWallpaper=null;
            }
            if(durga && durgaWallpaper==null){
                try(java.io.InputStream stream=getAssets().open("durga-wallpaper.png")){
                    BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.RGB_565;
                    durgaWallpaper=BitmapFactory.decodeStream(stream,null,options);
                    festivalWallpaper.setImageBitmap(durgaWallpaper);
                }catch(Exception e){android.util.Log.w("MinimalHome","Wallpaper unavailable",e);}
            }
            festivalWallpaper.setVisibility(durga && durgaWallpaper!=null?View.VISIBLE:View.GONE);
        }
        color = ThemePalette.color(theme);
        if(theme.equals("Auto")&&!ThemePalette.festive(theme)){
            glowColor=color;glow.setBackgroundColor(0xFF080D15);
            if(festivalDecor!=null)festivalDecor.invalidate();
            return;
        }
        glowColor = color;
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientCenter(0.32f, 0.30f);
        g.setGradientRadius(getResources().getDisplayMetrics().widthPixels * 0.72f);
        g.setColors(new int[]{withAlpha(color, durga?0.18f:0.62f), withAlpha(color, durga?0.04f:0.19f), Color.TRANSPARENT});
        glow.setBackground(g);
        if(festivalDecor!=null)festivalDecor.invalidate();
    }

    private void animateGlow(int to) {
        if (glowAnim != null) glowAnim.cancel();
        // Focus does not rebuild a full-screen gradient or touch wallpaper decoding.
        int chosen=ThemePalette.color(getPreferences(MODE_PRIVATE).getString("theme","Auto"));
        if(glowColor!=chosen)setGlow(chosen);
    }

    private void fadeGlow(float alpha) {
        glow.animate().alpha(alpha).setDuration(300).start();
        // The grid sits over the photo's busiest middle part, so dim it while browsing apps.
        gridShade.animate().alpha(alpha < 1f ? 1f : 0f).setDuration(300).start();
    }

    /** Average the banner's vivid pixels, then push saturation so the glow reads as a colour. */
    private int sampleColor(String pkg, Drawable d) {
        Integer cached = colorCache.get(pkg);
        if (cached != null) return cached;
        int color = 0xFF3A3A3A;
        try {
            Bitmap bm = Bitmap.createBitmap(24, 14, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            d.setBounds(0, 0, 24, 14);
            d.draw(c);
            float[] hsv = new float[3];
            double r = 0, g = 0, b = 0, w = 0;
            for (int y = 0; y < 14; y++) for (int x = 0; x < 24; x++) {
                int p = bm.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                Color.colorToHSV(p, hsv);
                double weight = 0.05 + hsv[1] * hsv[2];
                r += Color.red(p) * weight; g += Color.green(p) * weight; b += Color.blue(p) * weight; w += weight;
            }
            bm.recycle();
            if (w > 0) {
                Color.colorToHSV(Color.rgb((int) (r / w), (int) (g / w), (int) (b / w)), hsv);
                if (hsv[1] > 0.12f) { hsv[1] = Math.max(hsv[1], 0.75f); hsv[2] = 0.85f; }
                else { hsv[1] = 0f; hsv[2] = 0.45f; }
                color = Color.HSVToColor(hsv);
            }
        } catch (RuntimeException ignored) { }
        colorCache.put(pkg, color);
        return color;
    }

    // ---------------------------------------------------------------- entrance

    private void playEntrance() {
        View header = hero.getChildAt(0);
        header.setAlpha(0f);
        header.setTranslationY(-dp(12));
        header.animate().alpha(1f).translationY(0).setDuration(500).setStartDelay(80).start();
        title.setAlpha(0f);
        title.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            View t = dockRow.getChildAt(i);
            t.setAlpha(0f);
            t.setTranslationY(dp(24));
            t.animate().alpha(1f).translationY(0).setInterpolator(new DecelerateInterpolator(2f))
                    .setDuration(450).setStartDelay(160 + i * 60L).start();
        }
    }

    // ---------------------------------------------------------------- helpers

    private boolean launch(Intent i) {
        try {
            if (!i.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isInstalled(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openPackage(String pkg) {
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) launch(i);
    }

    private TextView text(float sp, String family, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(Typeface.create(getResources().getFont(R.font.outfit), family.contains("thin") ? 200 : family.contains("light") ? 300 : family.contains("medium") ? 500 : 400, false));
        t.setFontVariationSettings("'wght' "+(family.contains("thin")?200:family.contains("light")?300:family.contains("medium")?500:400));
        t.setTextColor(color);
        return t;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static FrameLayout.LayoutParams lp(int w, int h, int gravity, int l, int t, int r, int b) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h, gravity);
        p.setMargins(l, t, r, b);
        return p;
    }

    private static LinearLayout.LayoutParams llp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private static int withAlpha(int c, float a) {
        return Color.argb(Math.round(a * 255), Color.red(c), Color.green(c), Color.blue(c));
    }

    private static int darken(int c, float f) {
        return Color.rgb((int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int navigationKey=e.getKeyCode();
        boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
        boolean menuSelect=navigationKey==KeyEvent.KEYCODE_DPAD_CENTER||navigationKey==KeyEvent.KEYCODE_ENTER;
        if(inputsPopup!=null){
            if(navigationKey==KeyEvent.KEYCODE_BACK||navigationKey==KeyEvent.KEYCODE_MENU){if(!down)inputsPopup.dismiss();return true;}
            if(navigationKey==KeyEvent.KEYCODE_DPAD_UP||navigationKey==KeyEvent.KEYCODE_DPAD_DOWN){
                if(down)inputsPopup.move(navigationKey==KeyEvent.KEYCODE_DPAD_UP?-1:1);return true;
            }
            if(navigationKey==KeyEvent.KEYCODE_DPAD_LEFT||navigationKey==KeyEvent.KEYCODE_DPAD_RIGHT)return true;
            if(menuSelect){
                MenuOverlay menu=inputsPopup;
                boolean activate=menu.confirm.event(down,e.getRepeatCount());
                if(!down)consumeLongPressUp=false;
                if(activate)menu.select();
                return true;
            }
        }
        if(sidebarOpen && movingPackage==null){
            if(navigationKey==KeyEvent.KEYCODE_BACK||navigationKey==KeyEvent.KEYCODE_DPAD_RIGHT){if(!down)closeSidebar();return true;}
            if(navigationKey==KeyEvent.KEYCODE_DPAD_LEFT)return true;
            if(navigationKey==KeyEvent.KEYCODE_DPAD_UP||navigationKey==KeyEvent.KEYCODE_DPAD_DOWN){
                if(down){int i=pills.indexOf(getCurrentFocus());if(i<0)i=0;
                    pills.get(Math.max(0,Math.min(pills.size()-1,i+(navigationKey==KeyEvent.KEYCODE_DPAD_UP?-1:1)))).requestFocus();}
                return true;
            }
            if(menuSelect){if(sidebarConfirm.event(down,e.getRepeatCount())){View focus=getCurrentFocus();if(!pills.contains(focus))focus=pills.get(0);focus.performClick();}return true;}
        }
        if(movingPackage==null && down && navigationKey>=KeyEvent.KEYCODE_DPAD_UP && navigationKey<=KeyEvent.KEYCODE_DPAD_RIGHT){
            cancelConfirm();
            View focus=getCurrentFocus();
            if(focus==null||!focus.isShown()){restoreSelection(true);focus=getCurrentFocus();}
            int i=dockCards.indexOf(focus),g=gridCards.indexOf(focus),h=headerPills.indexOf(focus);
            if(expandedApps && g>=0){
                int target=g;
                if(navigationKey==KeyEvent.KEYCODE_DPAD_LEFT){if(g%cols==0){openSidebar();return true;}target=g-1;}
                if(navigationKey==KeyEvent.KEYCODE_DPAD_RIGHT)target=g%cols==cols-1?g:Math.min(gridCards.size()-1,g+1);
                if(navigationKey==KeyEvent.KEYCODE_DPAD_UP){if(g<cols){setAppExpansion(false);restoreSelection(false);scroll.smoothScrollTo(0,0);return true;}target=g-cols;}
                if(navigationKey==KeyEvent.KEYCODE_DPAD_DOWN)target=Math.min(gridCards.size()-1,g+cols);
                gridCards.get(target).requestFocus();return true;
            }
            if(i>=0){
                if(navigationKey==KeyEvent.KEYCODE_DPAD_LEFT){if(i==0)openSidebar();else dockCards.get(i-1).requestFocus();}
                if(navigationKey==KeyEvent.KEYCODE_DPAD_RIGHT)dockCards.get(Math.min(dockCards.size()-1,i+1)).requestFocus();
                if(navigationKey==KeyEvent.KEYCODE_DPAD_UP)headerPills.get(Math.min(i,headerPills.size()-1)).requestFocus();
                if(navigationKey==KeyEvent.KEYCODE_DPAD_DOWN&&!gridCards.isEmpty())openAllApps();
                return true;
            }
            if(h>=0){
                if(navigationKey==KeyEvent.KEYCODE_DPAD_LEFT)headerPills.get(Math.max(0,h-1)).requestFocus();
                if(navigationKey==KeyEvent.KEYCODE_DPAD_RIGHT)headerPills.get(Math.min(headerPills.size()-1,h+1)).requestFocus();
                if(navigationKey==KeyEvent.KEYCODE_DPAD_DOWN)restoreSelection(false);
                return true;
            }
        }
        if(menuSelect && movingPackage==null){
            if(down){
                if(e.getRepeatCount()==0 && pendingConfirm==null){
                    View focus=getCurrentFocus();
                    if(focus==null||!focus.isShown()){restoreSelection(false);focus=getCurrentFocus();}
                    pendingConfirm=focus;handler.postDelayed(confirmHold,600);
                }
            }else{
                View target=pendingConfirm;cancelConfirm();
                if(consumeLongPressUp)consumeLongPressUp=false;
                else if(target!=null && target.isShown() && target.hasFocus())target.performClick();
            }
            return true;
        }
        if (movingPackage != null) {
            int key=e.getKeyCode();
            boolean select=key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER;
            boolean arrow=key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT
                    || key==KeyEvent.KEYCODE_DPAD_UP || key==KeyEvent.KEYCODE_DPAD_DOWN;
            if (select) {
                if (e.getAction()==KeyEvent.ACTION_UP) {
                    if (ignoreMoveRelease) ignoreMoveRelease=false;
                    else {
                        movingPackage=null;
                        contextHint.setVisibility(View.INVISIBLE);
                        android.util.Log.i("MinimalHome","Reorder saved");
                        View focus=getCurrentFocus();
                        if (focus!=null && focus.getTag() instanceof App) title.setText(((App)focus.getTag()).label);
                        android.widget.Toast.makeText(this,"Order saved",android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            if (arrow) {
                if (e.getAction()==KeyEvent.ACTION_DOWN && e.getRepeatCount()==0) {
                    ignoreMoveRelease=false;
                    int from=-1;
                    for(int i=0;i<orderedApps.size();i++) if(orderedApps.get(i).pkg.equals(movingPackage)) from=i;
                    int to=from;
                    if(key==KeyEvent.KEYCODE_DPAD_LEFT && from%5>0) to=from-1;
                    if(key==KeyEvent.KEYCODE_DPAD_RIGHT && from%5<4) to=from+1;
                    if(key==KeyEvent.KEYCODE_DPAD_UP) to=from-5;
                    if(key==KeyEvent.KEYCODE_DPAD_DOWN) to=from+5;
                    if(from>=0 && to>=0 && to<orderedApps.size() && to!=from) {
                        Collections.swap(orderedApps,from,to);
                        List<String> packages=new ArrayList<>();
                        for(App app:orderedApps) packages.add(app.pkg);
                        getPreferences(MODE_PRIVATE).edit().putString("ordered_apps",String.join(",",packages)).apply();
                        buildApps();
                    }
                }
                return true;
            }
        }
        // Menu key on the remote opens the Inputs dropdown.
        if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP && !pills.isEmpty()) {
            if(sidebarOpen)closeSidebar();else openSidebar();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    /** 16:9 container. */
    static final class AspectFrame extends FrameLayout {
        AspectFrame(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY));
        }
    }

    /** ScrollView that never auto-scrolls to the focused child; the activity scrolls it by row. */
    static final class HomeScrollView extends android.widget.ScrollView {
        HomeScrollView(Context c) { super(c); }
        @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) { return 0; }
    }
}










