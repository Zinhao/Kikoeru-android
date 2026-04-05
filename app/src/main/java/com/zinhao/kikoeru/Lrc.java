package com.zinhao.kikoeru;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
        if(text.startsWith("WEBVTT")){
            initVtt(text);
        }else{
            initLrc(text);
        }
    }

    private void initVtt(String text){
        String[] rows = text.split("\n");
        int r = 0;
        while (r < rows.length) {
            if ("WEBVTT".equals(rows[r])) {
                r++;
                continue;
            }
            if (rows[r].isEmpty()) {
                LrcRow l = paserRow(rows, r + 1);
                System.out.println(String.format("[%s] %s", l.strTime, l.content));
                lrcRows.add(l);
            }
            r += 4;
        }
        for (int i = 0; i < lrcRows.size() - 1; i++) {
            LrcRow lrcRow = lrcRows.get(i);
            if(i!=0)
                lrcRow.upRow = lrcRows.get(i - 1);
            lrcRow.nextRow = lrcRows.get(i + 1);
        }
    }

    private void initLrc(String text){
        String[] rows = text.split("\n");
        for (int i = 0; i < rows.length; i++) {
            // [00:02.92]欢迎回来
            String row = rows[i];
            int timeStart = row.indexOf('[');
            int timeEnd = row.indexOf(']');
            if (timeStart != -1 && timeEnd != -1) {
                String timeStr = row.substring(timeStart + 1, timeEnd);
                try {
                    long timeLong = transToLong(timeStr);
                    String content = row.substring(timeEnd + 1);
                    if (!content.trim().isEmpty()) {
                        LrcRow lrcRow = new LrcRow(timeStr, timeLong, content);
                        lrcRows.add(lrcRow);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lrc: err lrc row:" + timeStr);
                }
            }
        }
        for (int i = 1; i < lrcRows.size() - 1; i++) {
            LrcRow lrcRow = lrcRows.get(i);
            lrcRow.upRow = lrcRows.get(i - 1);
            lrcRow.nextRow = lrcRows.get(i + 1);
        }
    }

    public String getText() {
        return text;
    }

    public LrcRow getCurrent() {
        if (current == null) {
            return LrcRow.NONE;
        }
        return current;
    }

    public LrcRow update(long seek) {
        if (currentIndex == lrcRows.size() - 1)
            return current;

        if (current == null) {
            current = lrcRows.get(0);
            currentIndex = 0;
            return current;
        }

        if (current.time <= seek) {
            LrcRow nextLrcRow = lrcRows.get(currentIndex+1);
            if (nextLrcRow.time - seek < 300) {
                currentIndex++;
                current = nextLrcRow;
            }
        } else {
            // user scroll seek
            for (int i = currentIndex; i > 0; i--) {
                current = lrcRows.get(i);
                currentIndex = i;
                if(current.getUpRow().time < seek &&
                        current.time > seek){
                    break;
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

    public static LrcRow paserRow(String[] text, int offset) {
        int position = Integer.parseInt(text[offset]);
        String timeStr = text[offset + 1];
        if (!timeStr.contains("-->")) {
            System.out.println("time str err");
            return LrcRow.NONE;
        }
        String[] times = timeStr.split("-->");
        String startTimeStr = times[0].trim();
        String endTimeStr = times[1].trim();

        long startTime = transToLong(startTimeStr);
        long endTime = transToLong(endTimeStr);

        String content = text[offset + 2];
        return new LrcRow(startTimeStr, startTime, content);
    }

    /**
     * @param timeStr 00:00:04.980
     * @return 4980
     */
    private static long transToLong(String timeStr) {
        if(!timeStr.contains(":")){
            return Math.round(Float.parseFloat(timeStr));
        }
        String[] strings = timeStr.split(":");
        String h = "0";
        String m = "0";
        String s = "0";
        String ms = "0";
        if (strings.length == 3) {
            h = strings[0];
            m = strings[1];
            if (strings[2].contains(".")) {
                String[] sWithMs = strings[2].split("\\.");
                s = sWithMs[0];
                ms = sWithMs[1];
            } else {
                s = strings[2];
            }
        } else if (strings.length == 2) {
            m = strings[0];
            if (strings[1].contains(".")) {
                String[] sWithMs = strings[1].split("\\.");
                s = sWithMs[0];
                ms = sWithMs[1];
            } else {
                s = strings[1];
            }
        }
        return Long.parseLong(h) * 60 * 1000 * 60 +
                Long.parseLong(m) * 60 * 1000 +
                Long.parseLong(s) * 1000 +
                Long.parseLong(ms);
    }

    public static class LrcRow {
        public static final LrcRow NONE = new LrcRow("无歌词", 0, "");
        public LrcRow upRow = NONE;
        public LrcRow nextRow = NONE;
        public String strTime;
        public long time;
        public String content;

        public LrcRow(String strTime, long time, String content) {
            this.strTime = strTime;
            this.time = time;
            this.content = content;
        }

        public LrcRow getUpRow() {
            if (upRow == null)
                return NONE;
            return upRow;
        }

        public LrcRow getNextRow() {
            if (nextRow == null) {
                return NONE;
            }
            return nextRow;
        }
    }

}
