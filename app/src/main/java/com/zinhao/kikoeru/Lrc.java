package com.zinhao.kikoeru;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Lrc {
    public static final Lrc NONE = new Lrc("");
    private String text;
    private static final String TAG = "Lrc";
    private List<LrcRow> lrcRows;

    private int currentIndex = -1;
    private LrcRow current;

    public Lrc(String text) {
        this.text = text;
        lrcRows = new ArrayList<>();
        String[] rows =text.split("\n");
        for (int i = 0; i < rows.length; i++) {
            // [00:02.92]欢迎回来
            String row = rows[i];
            int timeStart = row.indexOf('[');
            int timeEnd = row.indexOf(']');
            if(timeStart != -1 && timeEnd != -1){
                String timeStr = row.substring(timeStart+1,timeEnd);
                try{
                    long timeLong = transToLong(timeStr);
                    String content = row.substring(timeEnd+1);
                    LrcRow lrcRow = new LrcRow(timeStr,timeLong,content);
                    lrcRows.add(lrcRow);
                }catch (Exception e){
                    Log.e(TAG, "Lrc: err lrc row:" + timeStr);
                    continue;
                }
            }
        }
        for (int i = 1; i < lrcRows.size() - 1; i++) {
            LrcRow lrcRow = lrcRows.get(i);
            lrcRow.upRow = lrcRows.get(i-1);
            lrcRow.nextRow = lrcRows.get(i+1);
        }
    }

    public String getText() {
        return text;
    }

    public LrcRow getCurrent() {
        if(current == null){
            return LrcRow.NONE;
        }
        return current;
    }

    private static long transToLong(String timeStr){
        String m = timeStr.substring(0,timeStr.indexOf(':'));
        String s = timeStr.substring(timeStr.indexOf(':')+1,timeStr.indexOf('.'));
        String ms = timeStr.substring(timeStr.indexOf(".") +1);
        return Long.parseLong(m) *60*1000 + Long.parseLong(s)*1000 + Long.parseLong(ms);
    }

    public LrcRow update(long seek){
        if(currentIndex == lrcRows.size()-1)
            return current;

        if(current == null){
            current = lrcRows.get(0);
            currentIndex = 0;
        }

        if(current.time <= seek){
            for (int i = currentIndex+1; i < lrcRows.size() - 1; i++) {
                LrcRow lrcRow = lrcRows.get(i);
                if(lrcRow.time > seek && current.time <= seek){
                    break;
                }else {
                    currentIndex = i;
                    current = lrcRow;
                }
            }
        }else {
            for (int i = currentIndex; i > 0; i--) {
                LrcRow lrcRow = lrcRows.get(i);
                if(lrcRow.time > seek && current.time < seek){
                    break;
                }else {
                    currentIndex = i;
                    current = lrcRow;
                }
            }
        }
        return current;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<LrcRow> getLrcRows() {
        return lrcRows;
    }

    public static class LrcRow{
        public static final LrcRow NONE = new LrcRow("00:00",0,"无歌词");
        public LrcRow upRow = NONE;
        public LrcRow nextRow = NONE;
        public String strTime;
        public long time ;
        public String content ;

        public LrcRow(String strTime, long time, String content) {
            this.strTime = strTime;
            this.time = time;
            this.content = content;
        }

        public LrcRow getUpRow() {
            if(upRow == null)
                return NONE;
            return upRow;
        }

        public LrcRow getNextRow() {
            if(nextRow == null){
                return NONE;
            }
            return nextRow;
        }
    }

}
