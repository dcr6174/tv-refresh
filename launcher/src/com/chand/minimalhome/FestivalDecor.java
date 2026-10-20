package com.chand.minimalhome;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Static vector ornaments with no continuous animation or wallpaper decoding. */
final class FestivalDecor extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String forcedTheme;
    FestivalDecor(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    FestivalDecor(Context c,String theme){this(c);forcedTheme=theme;}
    private void ink(int color,int alpha){p.setShader(null);p.setColor(color);p.setAlpha(alpha);p.setStyle(Paint.Style.FILL);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        String selected=forcedTheme!=null?forcedTheme:getContext().getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("theme","Auto");
        String name=ThemePalette.resolve(selected);
        c.save();c.scale(getWidth()/960f,getHeight()/540f);
        if(name.equals("Aurora")){
            p.setShader(new RadialGradient(730,170,520,new int[]{0x55773FCC,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,960,540,p);p.setShader(null);
        }
        if(!ThemePalette.festive(selected)){c.restore();return;}
        if(name.equals("Dussehra")||name.startsWith("Navaratri")){c.restore();return;}
        p.setShader(new RadialGradient(830,60,490,new int[]{0x339C2849,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,960,540,p);p.setShader(null);
        if(name.equals("Sankranti")||name.equals("Kanuma")){
            kite(c,790,130,36,0xFFEF9C45);kite(c,880,210,21,0xFFAD6FD8);
        }else if(name.equals("Deepavali")||name.equals("Karthika Pournami")||name.equals("Bhogi")){
            diya(c,770,445,1);diya(c,885,478,.7f);
            for(int i=0;i<18;i++){ink(0xFFF8CB75,40+(i%3)*15);c.drawCircle(690+(i*41)%240,110+(i*61)%250,1.2f,p);}
        }else{
            for(int i=0;i<17;i++){
                float x=20+i*58,y=12+(float)Math.sin(i*Math.PI/4)*7;
                flower(c,x,y,7,i%2==0?0xFFDCA64A:0xFFAC633E,80);
                ink(0xFF6C9864,80);Path leaf=new Path();leaf.moveTo(x+17,y-4);leaf.quadTo(x+30,y+7,x+19,y+23);leaf.quadTo(x+13,y+8,x+17,y-4);c.drawPath(leaf,p);
            }
        }
        c.save();c.translate(888,486);
        ink(ThemePalette.color(selected),45);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);
        for(int i=0;i<12;i++){c.save();c.rotate(i*30);c.drawOval(-14,-80,14,0,p);c.restore();}
        c.drawCircle(0,0,28,p);c.drawCircle(0,0,88,p);c.restore();
        if(name.contains("Bathukamma")||name.equals("Bonalu")||name.equals("Holi"))
            for(int i=0;i<4;i++)flower(c,815+i*27,165+(i%2)*19,10-i,i%2==0?0xFFD667A6:0xFFDCAD49,95);
        c.restore();
    }
    private void flower(Canvas c,float x,float y,float r,int tint,int alpha){
        ink(tint,alpha);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawCircle(x+(float)Math.cos(a)*r*.6f,y+(float)Math.sin(a)*r*.6f,r*.55f,p);}
        ink(0xFFF3CA72,alpha);c.drawCircle(x,y,r*.4f,p);
    }
    private void kite(Canvas c,float x,float y,float size,int tint){
        ink(tint,110);Path shape=new Path();shape.moveTo(x,y-size);shape.lineTo(x+size*.7f,y);shape.lineTo(x,y+size);shape.lineTo(x-size*.7f,y);shape.close();c.drawPath(shape,p);
        ink(0xFFEAD8AC,70);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);Path tail=new Path();tail.moveTo(x,y+size);tail.cubicTo(x-25,y+size+30,x+20,y+size+45,x-10,y+size+70);c.drawPath(tail,p);
    }
    private void diya(Canvas c,float x,float y,float scale){
        c.save();c.translate(x,y);c.scale(scale,scale);ink(0xFFDC9A43,110);c.drawArc(-21,-5,21,16,0,180,false,p);
        ink(0xFFF5C96F,155);Path flame=new Path();flame.moveTo(0,-29);flame.cubicTo(-13,-11,-8,-2,0,-3);flame.cubicTo(8,-2,13,-11,0,-29);c.drawPath(flame,p);c.restore();
    }
}
