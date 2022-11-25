package com.zinhao.kikoeru;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagsView<T> extends View {
    private static final String TAG = "TagsView";
    private T tags;
    private List<RectF> tagsRectFs;
    private final Paint textPaint;
    private int MIN_WIDTH = 0;
    private int MIN_HEIGHT = 0;
    private float textPadding = 20;
    private float rectFH = 0;
    private float textDistance;
    private Drawable tagBg;
    private TextGet textGet;
    private TagClickListener tagClickListener;
    private GestureDetector gestureDetector;
    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener;

    public void setTagClickListener(TagClickListener<?> tagClickListener) {
        this.tagClickListener = tagClickListener;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onGenericMotionEvent(event);
    }

    public void setTagBackgroundResource(int resid) {
        tagBg = ContextCompat.getDrawable(getContext(), resid);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    public static class JsonTextGet implements TextGet<JSONObject> {
        private String key;

        public JsonTextGet(String key) {
            this.key = key;
        }

        @Override
        public String onGetText(JSONObject t) {
            try {
                return t.getString(key);
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
            return "";
        }

        public JsonTextGet setKey(String key) {
            this.key = key;
            return this;
        }

        public String getKey() {
            return key;
        }
    }

    public static final JsonTextGet JSON_TEXT_GET = new JsonTextGet("name");
    public static final TextGet<String> STRING_TEXT_GET = new TextGet<String>() {
        @Override
        public String onGetText(String t) {
            return t;
        }
    };

    public TagsView(Context context) {
        this(context, null);
    }

    public TagsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        MIN_WIDTH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, context.getResources().getDisplayMetrics());
        MIN_HEIGHT = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, context.getResources().getDisplayMetrics());
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setTextSize(12);
        textPaint.setColor(Color.WHITE);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        textDistance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
        rectFH = fontMetrics.bottom - fontMetrics.top;
        setPadding(10, 7, 0, 7);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TagsView);
        tagBg = array.getDrawable(R.styleable.TagsView_tagBackground);
        String tagStr = array.getString(R.styleable.TagsView_tags);
        if (tagStr != null && !tagStr.isEmpty()) {
            String[] tagStrs = tagStr.split(" ");
            if (tagStrs.length != 0) {
                List<String> tags = Arrays.asList(tagStrs);
                this.tags = (T) tags;
                textGet = STRING_TEXT_GET;
            }
        }
        array.recycle();
        tagsRectFs = new ArrayList<>();
        for (int i = 0; i < getTagsLen(); i++) {
            tagsRectFs.add(new RectF());
        }
        simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onContextClick(MotionEvent e) {
                Log.d(TAG, "onContextClick: ");
                if (tagClickListener == null)
                    return false;
                for (int i = 0; i < tagsRectFs.size(); i++) {
                    if (tagsRectFs.get(i).contains(e.getX(), e.getY())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d(TAG, "onSingleTapUp: ");
                if (tagClickListener == null)
                    return false;
                for (int i = 0; i < tagsRectFs.size(); i++) {
                    if (tagsRectFs.get(i).contains(e.getX(), e.getY())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.d(TAG, "onLongPress: ");
            }

            @Override
            public boolean onDown(MotionEvent e) {
                Log.d(TAG, "onDown: ");
                if (tagClickListener == null)
                    return false;
                for (int i = 0; i < tagsRectFs.size(); i++) {
                    if (tagsRectFs.get(i).contains(e.getX(), e.getY())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.d(TAG, "onSingleTapConfirmed: ");
                if (tagClickListener == null)
                    return false;
                for (int i = 0; i < tagsRectFs.size(); i++) {
                    if (tagsRectFs.get(i).contains(e.getX(), e.getY())) {
                        tagClickListener.onTagClick(getTagByIndex(i));
                        return true;
                    }
                }
                return false;
            }
        };
        gestureDetector = new GestureDetector(getContext(), simpleOnGestureListener);
    }

    public int getTagsLen() {
        if (tags instanceof List) {
            return ((List<?>) tags).size();
        } else if (tags instanceof JSONArray) {
            return ((JSONArray) tags).length();
        }
        return 0;
    }

    public String getTagText(int index) {
        Object o = null;
        if (tags instanceof List) {
            List<String> strings = (List<String>) tags;
            o = strings.get(index);
        } else if (tags instanceof JSONArray) {
            try {
                o = ((JSONArray) tags).get(index);
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }
        if (o != null && textGet != null) {
            return textGet.onGetText(o);
        }
        return "";
    }

    public Object getTagByIndex(int index) {
        Object o = null;
        if (tags instanceof List) {
            List<String> strings = (List<String>) tags;
            o = strings.get(index);
        } else if (tags instanceof JSONArray) {
            try {
                o = ((JSONArray) tags).getJSONObject(index);
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }
        return o;
    }

    public interface TextGet<T> {
        String onGetText(T t);
    }

    public interface TagClickListener<T> {
        void onTagClick(T t);
    }

    public void setTags(T tags, TextGet<?> textGet) {
        this.tags = tags;
        this.textGet = textGet;
        tagsRectFs.clear();
        setMeasuredDimension(getWidth(), makeTagsRectF(getWidth()));
        if (!isInLayout()) {
            requestLayout();
        } else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isInLayout()) {
                        requestLayout();
                    }
                }
            }, 500);
        }
    }

    /**
     * @param size sp
     */
    public void setTextSize(int size) {
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, getContext().getResources().getDisplayMetrics()));
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
        setMeasuredDimension(w, makeTagsRectF(w));
    }

    private int makeTagsRectF(int w) {
        for (int i = 0; i < getTagsLen(); i++) {
            tagsRectFs.add(new RectF());
        }
        int h = 0;
        float x = 0, y = getPaddingTop();
        for (int i = 0; i < getTagsLen(); i++) {
            float textW = 0;
            String text = getTagText(i);
            textW = textPaint.measureText(text);
            float rectW = textW + textPadding * 2;
            float rectH = rectFH + textPadding;

            if (x + rectW > w) {
                y += rectH + getPaddingTop() + getPaddingBottom();
                x = 0;
            }
            tagsRectFs.get(i).set(x, y, x + rectW, y + rectH);
            x += rectW + getPaddingRight() + getPaddingLeft();
            h = (int) (y + rectH) + getPaddingBottom();
        }
        if (getTagsLen() == 0)
            h = 0;
        return h;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < getTagsLen(); i++) {
            RectF tagRectF = tagsRectFs.get(i);
            if (tagBg != null) {
                tagBg.setBounds((int) tagRectF.left, (int) tagRectF.top, (int) tagRectF.right, (int) tagRectF.bottom);
                tagBg.draw(canvas);
            }
            textPaint.setColor(Color.WHITE);
            textPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(getTagText(i), tagsRectFs.get(i).centerX(), tagsRectFs.get(i).centerY() + textDistance, textPaint);
        }
    }
}
