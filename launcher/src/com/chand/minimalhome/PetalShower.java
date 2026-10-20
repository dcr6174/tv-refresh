package com.chand.minimalhome;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Brief festive entrance only; not an always-running particle effect. */
final class PetalShower extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animation;
    private float progress;
    private boolean fireworks;
    PetalShower(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void start(String theme){
        fireworks=theme.equals("Deepavali");
        stop();setVisibility(View.VISIBLE);setAlpha(1);
        animation=ValueAnimator.ofFloat(0,1);animation.setDuration(9000);
        animation.setInterpolator(new android.view.animation.LinearInterpolator());
        animation.addUpdateListener(v->{progress=(float)v.getAnimatedValue();setAlpha(progress>.8f?(1-progress)*5:1);invalidate();});
        animation.start();
    }
    void stop(){if(animation!=null)animation.cancel();setVisibility(View.INVISIBLE);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(fireworks){
            int[] colors={0xFFE9B955,0xFF8EB9EE,0xFFE68BC0,0xFFACD5A6};
            for(int burst=0;burst<6;burst++){
                float age=progress*2.3f-burst*.22f;if(age<0||age>1)continue;
                float cx=(.15f+(burst%4)*.22f)*getWidth(),cy=(.2f+(burst%3)*.13f)*getHeight();
                paint.setColor(colors[burst%4]);paint.setAlpha((int)((1-age)*190));paint.setStrokeWidth(2);
                for(int ray=0;ray<24;ray++){
                    double angle=ray*Math.PI/12;
                    float radius=age*getWidth()*.085f;
                    float x=cx+(float)Math.cos(angle)*radius,y=cy+(float)Math.sin(angle)*radius+age*age*35;
                    c.drawLine(x,y,x-(float)Math.cos(angle)*6,y-(float)Math.sin(angle)*6,paint);
                }
            }
            return;
        }
        for(int i=0;i<22;i++){
            float phase=progress*1.4f-i*.023f;if(phase<0||phase>1.2f)continue;
            float x=((i*137)%960)/960f*getWidth()+(float)Math.sin(phase*8+i)*25;
            float y=phase*getHeight();
            paint.setColor(i%3==0?0xB3D377A4:i%2==0?0xB3E7B255:0x99C87841);
            c.save();c.translate(x,y);c.rotate(phase*230+i*37);c.drawOval(-5,-9,5,9,paint);c.restore();
        }
    }
}
