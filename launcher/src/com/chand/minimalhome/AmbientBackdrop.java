package com.chand.minimalhome;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import java.util.Random;

/** Static scene artwork drifts as a composited layer; no video decoder or per-frame bitmap work. */
final class AmbientBackdrop extends View {
    private final String scene;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] stars=new float[180];
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active, reverse;
    private final Runnable drift=new Runnable(){public void run(){
        if(!active)return;
        reverse=!reverse;
        animate().translationX(reverse?18:-18).translationY(reverse?-10:10).setDuration(60000).setInterpolator(new android.view.animation.LinearInterpolator()).start();
        handler.postDelayed(this,60000);
    }};
    AmbientBackdrop(Context context,String scene){
        super(context);this.scene=scene;Random random=new Random(715);
        for(int i=0;i<stars.length;i++)stars[i]=random.nextFloat();
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void start(){active=true;handler.removeCallbacks(drift);handler.post(drift);}
    void stop(){active=false;handler.removeCallbacks(drift);animate().cancel();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight();
        if(scene.equals("Stars")){
            paint.setShader(new RadialGradient(w*.7f,h*.35f,w*.65f,new int[]{0xFF101B37,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint);paint.setShader(null);
            for(int i=0;i<stars.length;i+=3){paint.setColor(Color.argb((int)(45+stars[i+2]*100),180,199,230));c.drawCircle(stars[i]*w,stars[i+1]*h,.7f+stars[i+2]*1.2f,paint);}
        }else{
            paint.setShader(new RadialGradient(w*.23f,h*.3f,w*.7f,new int[]{0xFF124139,0x330D282B,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint);
            paint.setShader(new RadialGradient(w*.79f,h*.5f,w*.65f,new int[]{0xFF2D1E4B,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,paint);paint.setShader(null);
        }
    }
}
