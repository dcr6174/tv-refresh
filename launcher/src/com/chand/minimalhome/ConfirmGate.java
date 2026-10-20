package com.chand.minimalhome;

/** A menu accepts a fresh press/release, never the release that opened it. */
final class ConfirmGate {
    private boolean blocked, armed;
    ConfirmGate(boolean openingKeyHeld){blocked=openingKeyHeld;}
    boolean event(boolean down,int repeatCount){
        if(blocked){if(!down)blocked=false;armed=false;return false;}
        if(down){if(repeatCount==0)armed=true;return false;}
        boolean selected=armed;armed=false;return selected;
    }
}
