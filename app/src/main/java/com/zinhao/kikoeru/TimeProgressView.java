package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

/**
 * 2021/2/14 0:15
 *
 * @author JIL
 **/
public class TimeProgressView extends View implements View.OnTouchListener{
    private static final int MIN_HEIGHT = 50;
    private static final int MIN_WIDTH = 400;
    private static final float PROGRESS_POINT_WIDTH = 10;
    private static final float PROGRESS_POINT_HEIGHT = 30;
    private int max = 100;
    private int min;
    private int progress = 100;
    private final Paint textPaint;
    private final Paint seekBarPaint;
    private final RectF alreadyPassRect;
    private final RectF remainRect;
    private final RectF progressPointRect;
    private String maxTimeStr ="00:00";
    private static final int SECOND = 60;
    public static final int Millis_SECOND = 60000;
    public int type =Millis_SECOND;
    private float textY;
    private boolean isTouch;
    private float textDistance;

    public TimeProgressView(Context context) {
        this(context,null);
    }

    public TimeProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TimeProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textPaint =new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, context.getResources().getDisplayMetrics()));
        textPaint.setColor(Color.BLACK);
        alreadyPassRect = new RectF();
        progressPointRect =new RectF();
        remainRect = new RectF();
        seekBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        seekBarPaint.setColor(Color.GREEN);
        Paint.FontMetrics fontMetrics =textPaint.getFontMetrics();
        textDistance = (fontMetrics.bottom -fontMetrics.top)/2 -fontMetrics.bottom;
        setOnTouchListener(this);

    }

    public void setColor(int color){
        textPaint.setColor(color);
        seekBarPaint.setColor(color);
        invalidate();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        if(!isTouch)
            invalidate();
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        maxTimeStr = makeMaxTimeStr();
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    private String makeCurrentTimeStr(){
        int minute =progress/type;
        int second =progress%type/(type/60);
        return String.format("%02d:%02d",minute,second);
    }

    @SuppressLint("DefaultLocale")
    private String makeMaxTimeStr(){
        int minute =max/type;
        int second =max%type/(type/60);
        return String.format("%02d:%02d",minute,second);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        int w = widthSpecSize;
        int h = heightSpecSize;

        //处理wrap_content的几种特殊情况
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            w = MIN_WIDTH;  //单位是px
            h = MIN_HEIGHT;
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            //只要宽度布局参数为wrap_content， 宽度给固定值200dp(处理方式不一，按照需求来)
            w = MIN_WIDTH;
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            h = MIN_HEIGHT;
        }
        //给两个字段设置值，完成最终测量
        textY = h/2f + textDistance;
        barStartX =textPaint.measureText("00:00") + PROGRESS_POINT_WIDTH/2;
        barEndX = w - barStartX;
        barTopY = h/5f *2;
        barBottomY = h/5f *3;
        allProgressWidth = barEndX-barStartX;
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    float barStartX;
    float barEndX;
    float barTopY;
    float barBottomY;
    float allProgressWidth;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float progressPe =  Math.min((float) progress/max,1f);

        float nowProgressX = allProgressWidth* progressPe + barStartX-PROGRESS_POINT_WIDTH/2f;
        float progressRectTop = getHeight()/2f -PROGRESS_POINT_HEIGHT/2f;
        float progressRectBottom = progressRectTop +PROGRESS_POINT_HEIGHT;
        // already pass
        alreadyPassRect.set(barStartX,barTopY,nowProgressX+PROGRESS_POINT_WIDTH/2f,barBottomY);
        // remain
        remainRect.set(nowProgressX+PROGRESS_POINT_WIDTH/2f,barTopY,barEndX,barBottomY);
        // oval point
        progressPointRect.set(nowProgressX,progressRectTop,nowProgressX+PROGRESS_POINT_WIDTH,progressRectBottom);

        canvas.drawText(makeCurrentTimeStr(),0,textY,textPaint);
        canvas.drawText(maxTimeStr,barEndX+PROGRESS_POINT_WIDTH/2,textY,textPaint);

        seekBarPaint.setAlpha(255);
        canvas.drawRect(alreadyPassRect,seekBarPaint);

        seekBarPaint.setAlpha(100);
        canvas.drawRect(remainRect,seekBarPaint);

        seekBarPaint.setAlpha(255);
        canvas.drawRect(progressPointRect,seekBarPaint);
//        canvas.drawOval(progressPointRect,seekBarPaint);

    }

    private SeekBar.OnSeekBarChangeListener listener;

    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        float x =event.getX();
        float current,pe;
        switch(action){
            case MotionEvent.ACTION_DOWN:
                if(x<barStartX||x>barEndX){
                    return false;
                }
                isTouch =true;
                current =x- barStartX;
                pe =current/allProgressWidth;
                progress = (int) (max*pe);
                listener.onStartTrackingTouch(null);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if(x<barStartX||x>barEndX){
                    return true;
                }
                current =x- barStartX;
                pe =current/allProgressWidth;
                progress = (int) (max*pe);
                if(listener!=null)
                    listener.onProgressChanged(null,progress,false);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                isTouch =false;
                x=Math.min(barEndX,Math.max(barStartX,x));
                current =x- barStartX;
                pe =current/allProgressWidth;
                progress = (int) (max*pe);
                if(listener!=null)
                    listener.onProgressChanged(null,progress,true);
                invalidate();
                listener.onStopTrackingTouch(null);
                break;
            default:
                break;
        }
        return true;
    }
}
