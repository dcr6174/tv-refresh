package com.chand.minimalhome;
final class ConfirmGateCheck {
    static void require(boolean condition,String description){if(!condition)throw new AssertionError(description);}
    public static void main(String[] args){
        ConfirmGate held=new ConfirmGate(true);
        require(!held.event(true,1),"Held repeat must not select");
        require(!held.event(false,0),"Opening long-press release must not select");
        require(!held.event(false,0),"Duplicate release must not select");
        require(!held.event(true,0),"A new down must only arm");
        require(held.event(false,0),"Fresh press/release must select");
        require(!held.event(false,0),"One press must select at most once");
        ConfirmGate ordinary=new ConfirmGate(false);
        require(!ordinary.event(false,0),"Unpaired release must not select");
        ordinary.event(true,0);ordinary.event(true,3);
        require(ordinary.event(false,0),"Held fresh press must select once on release");
        System.out.println("PASS: opening long press, repeats, duplicate/unpaired releases, and fresh confirmation");
    }
}
