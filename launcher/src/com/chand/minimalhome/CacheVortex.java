package com.chand.minimalhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.ViewGroup;

/** Lightweight, finite wisps swooping into the actual cleaner button. */
final class CacheVortex extends View {
    private final Paint paint=new Paint(3);
    private final Path trail=new Path();
    private final ValueAnimator animation=ValueAnimator.ofFloat(0,1);
    private final View target;
    private final int[] origin=new int[2],anchor=new int[2];
    private float progress;
    private android.graphics.drawable.Drawable fan(){return target instanceof android.widget.TextView?((android.widget.TextView)target).getCompoundDrawables()[0]:null;}
    private void spin(float fraction){android.graphics.drawable.Drawable icon=fan();if(icon!=null)icon.setLevel(Math.round(fraction*10000));}
    CacheVortex(Context context,View target){
        super(context);this.target=target;
        setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        animation.setDuration(1900);
        animation.setInterpolator(new android.view.animation.LinearInterpolator());
        animation.addUpdateListener(a->{progress=(float)a.getAnimatedValue();spin(progress);invalidate();});
        animation.addListener(new android.animation.AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(android.animation.Animator a){spin(0);if(getParent() instanceof ViewGroup)((ViewGroup)getParent()).removeView(CacheVortex.this);}
        });
    }
    void start(){animation.start();}
    @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();animation.cancel();}
    private float coordinate(float t,float start,float c1,float c2,float end){
        float u=1-t;return u*u*u*start+3*u*u*t*c1+3*u*t*t*c2+t*t*t*end;
    }
    @Override protected void onDraw(Canvas canvas){
        getLocationInWindow(origin);target.getLocationInWindow(anchor);
        float tx=anchor[0]-origin[0]+target.getWidth()*.5f;
        float ty=anchor[1]-origin[1]+target.getHeight()*.5f;
        float density=getResources().getDisplayMetrics().density;
        float fade=Math.min(1,progress*8)*Math.min(1,(1-progress)*7);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        for(int i=0;i<9;i++){
            float head=Math.max(0,Math.min(1,(progress-i*.022f)/.78f));
            if(head<=0||head>=1)continue;
            float sx=getWidth()*(.08f+(i%5)*.20f),sy=getHeight()*(.68f+(i%3)*.13f);
            float hx=0,hy=0;
            for(int layer=0;layer<2;layer++){
                trail.reset();
                for(int k=0;k<=22;k++){
                    float t=Math.max(0,head-.23f+k*.23f/22),u=1-t;
                    float curl=(float)Math.sin(t*9+i*.6f)*u*42*density;
                    float x=coordinate(t,sx,sx-getWidth()*.23f,tx-getWidth()*.28f,tx)+curl;
                    float y=coordinate(t,sy,sy-getHeight()*.43f,ty+getHeight()*.32f,ty)+curl*.55f;
                    if(k==0)trail.moveTo(x,y);else trail.lineTo(x,y);
                    hx=x;hy=y;
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth((layer==0?8:1.5f)*density*(1-head*.8f));
                paint.setColor(Color.argb((int)((layer==0?24:170)*fade*(1-head*.55f)),110+i%3*40,195,255));
                canvas.drawPath(trail,paint);
            }
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.argb((int)(220*fade*(1-head)),185,225,255));
            canvas.drawCircle(hx,hy,2.3f*density*(1-head),paint);
        }
        // The destination glow hugs the real icon, rather than covering the screen.
        float pulse=(float)Math.sin(progress*Math.PI);
        paint.setStyle(Paint.Style.STROKE);
        for(int j=0;j<3;j++){
            paint.setStrokeWidth((5-j)*density);
            paint.setColor(Color.argb((int)((35-j*9)*fade),95,180,255));
            canvas.drawCircle(tx,ty,(12+4*j+3*pulse)*density,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
