package com.chand.minimalhome;

import android.content.Context;
import android.os.StatFs;
import android.os.Environment;
import java.io.File;
import java.util.regex.*;

/** Disposable cache trim plus normal Recents removal; never force-stops or clears app data. */
final class TvCleaner {
    static LocalAdb connect(Context context,Runnable needsApproval)throws Exception{return new LocalAdb(new File(context.getFilesDir(),"cleaner"),"127.0.0.1",5555,needsApproval);}
    static int closeRecentApps(LocalAdb adb)throws Exception{
        String sessions=adb.shell("dumpsys media_session");
        String focus=adb.shell("dumpsys window windows | grep mCurrentFocus");
        java.util.Set<String> skip=new java.util.HashSet<>();
        Matcher packageName=Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+)+").matcher(focus);
        while(packageName.find())skip.add(packageName.group());
        // Conservatively protect every package in the media-session dump if any session is playing.
        if(sessions.contains("state=3")||sessions.contains("state=6")){
            packageName=Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+)+").matcher(sessions);
            while(packageName.find())skip.add(packageName.group());
        }
        java.util.List<Integer> stacks=RecentsCleaner.eligible(adb.shell("am stack list"),skip);
        int closed=0;
        for(int id:stacks){
            // Re-check each stack just before closing it in case playback or focus changed.
            String current=adb.shell("am stack list");
            if(!RecentsCleaner.eligible(current,skip).contains(id))continue;
            String mediaNow=adb.shell("dumpsys media_session");
            if((mediaNow.contains("state=3")||mediaNow.contains("state=6"))&&!mediaNow.equals(sessions))continue;
            adb.shell("am stack remove "+id);
            for(int attempt=0;attempt<6;attempt++){
                Thread.sleep(120);
                String remaining=adb.shell("am stack list");
                if(!java.util.regex.Pattern.compile("(?m)^Stack id="+id+" ").matcher(remaining).find()){closed++;break;}
            }
        }
        return closed;
    }

    static String clean(Context context,Runnable needsApproval)throws Exception{
        long before=new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes();
        try(LocalAdb adb=connect(context,needsApproval)){
            String closed;
            try{closed=closeRecentApps(adb)+" background app tasks closed";}
            catch(Exception e){closed="Background task cleanup unavailable";}
            boolean trimmed=adb.shell("pm trim-caches 999999999999 >/dev/null 2>&1 && echo CACHE_OK").contains("CACHE_OK");
            long freed=Math.max(0,new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes()-before)/(1024*1024);
            return closed+" · "+(trimmed?"Cache cleaned"+(freed>0?" · "+freed+" MB freed":""):"Cache cleaning unavailable");
        }
    }
}
