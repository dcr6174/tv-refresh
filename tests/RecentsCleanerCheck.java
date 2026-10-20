package com.chand.minimalhome;
import java.util.*;
public class RecentsCleanerCheck{
 public static void main(String[] args){
  String app="Stack id=6 bounds=[]\n mActivityType=standard\n taskId=1: com.phlox.tvwebbrowser/.Main userId=0 visible=false topActivity=ComponentInfo{com.phlox.tvwebbrowser/.Main}\n";
  if(!RecentsCleaner.eligible(app,Collections.emptySet()).equals(Arrays.asList(6)))throw new AssertionError("ordinary hidden task");
  if(!RecentsCleaner.eligible(app.replace("visible=false","visible=true"),Collections.emptySet()).isEmpty())throw new AssertionError("visible task");
  if(!RecentsCleaner.eligible(app,new HashSet<>(Arrays.asList("com.phlox.tvwebbrowser"))).isEmpty())throw new AssertionError("active media");
  if(!RecentsCleaner.eligible(app.replace("com.phlox.tvwebbrowser","com.android.systemui"),Collections.emptySet()).isEmpty())throw new AssertionError("system");
  if(!RecentsCleaner.eligible(app+" taskId=2: com.tcl.tv/.TV visible=false\n",Collections.emptySet()).isEmpty())throw new AssertionError("mixed stack");
  if(!RecentsCleaner.eligible(app.replace("mActivityType=standard","mActivityType=home"),Collections.emptySet()).isEmpty())throw new AssertionError("home");
  System.out.println("PASS: background, foreground, playback, system, mixed stack and home protections");
 }
}
