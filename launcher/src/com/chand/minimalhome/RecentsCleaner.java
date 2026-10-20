package com.chand.minimalhome;

import java.util.*;
import java.util.regex.*;

/** Select only hidden ordinary app stacks; system, playback and visible tasks are excluded. */
final class RecentsCleaner {
    private static final Set<String> APPS=new HashSet<>(Arrays.asList("com.google.android.youtube.tv","com.netflix.ninja","com.amazon.amazonvideo.livingroom","in.startv.hotstar","com.graymatrix.did","com.sonyliv","ahaflix.tv","org.videolan.vlc","com.etvwin.androidtv","com.jio.media.jiotvplus","com.suntv.sunnxt","com.stremio.one","tv.airtel.xstream.tvapp","com.phlox.tvwebbrowser","pl.solidexplorer2","com.yablio.sendfilestotv"));
    static List<Integer> eligible(String dump,Set<String> protectedApps){
        List<Integer> result=new ArrayList<>();
        for(String block:dump.split("(?m)(?=^Stack id=)")){
            Matcher id=Pattern.compile("^Stack id=([0-9]+)").matcher(block);
            if(!id.find()||!block.contains("mActivityType=standard")||block.contains("visible=true"))continue;
            Matcher task=Pattern.compile("taskId=[0-9]+: ([a-zA-Z0-9_.]+)/").matcher(block);
            boolean found=false,safe=true;
            while(task.find()){
                found=true;String pkg=task.group(1);
                if(!APPS.contains(pkg)||protectedApps.contains(pkg))safe=false;
            }
            Matcher top=Pattern.compile("topActivity=ComponentInfo\\{([a-zA-Z0-9_.]+)/").matcher(block);
            while(top.find())if(!APPS.contains(top.group(1))||protectedApps.contains(top.group(1)))safe=false;
            if(found&&safe)result.add(Integer.parseInt(id.group(1)));
        }
        return result;
    }
}
