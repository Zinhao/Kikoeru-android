package com.zinhao.kikoeru;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private void initVtt(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // 1. 兼容 \r\n 和 \n 的换行拆分
        String[] rows = text.split("\\r?\\n");

        // 正则表达式：匹配 VTT 时间戳行，并捕获起始时间，例如 "00:00:01.000 --> 00:00:04.000"
        // 支持 "00:02.000 --> ..." 或 "00:00:02.000 --> ..." 两种常见标准格式
        Pattern timePattern = Pattern.compile("^(([0-9]{2}:)?[0-9]{2}:[0-9]{2}\\.[0-9]{3})\\s-->.*");

        int r = 0;
        while (r < rows.length) {
            String line = rows[r].trim();

            // 跳过文件头和空行
            if (line.isEmpty() || "WEBVTT".equals(line)) {
                r++;
                continue;
            }

            // 匹配到时间戳行
            Matcher matcher = timePattern.matcher(line);
            if (matcher.matches()) {
                // 提取起始时间字符串，例如 "00:00:01.000"
                String strTime = matcher.group(1);
                // 将字符串时间转换为 long 型毫秒数
                long timeMs = parseTimeToMs(strTime);

                StringBuilder contentBuilder = new StringBuilder();

                // 收集接下来的歌词文本，直到遇到空行或下一段的开始
                r++;
                while (r < rows.length && !rows[r].trim().isEmpty()) {
                    if (timePattern.matcher(rows[r].trim()).matches()) {
                        break; // 异常防御：如果下一行直接又是时间戳，跳出文本收集
                    }
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append("\n"); // 多行歌词换行连接
                    }
                    contentBuilder.append(rows[r].trim());
                    r++;
                }

                String content = contentBuilder.toString();

                // 使用你的构造函数实例化 LrcRow
                LrcRow l = new LrcRow(strTime, timeMs, content);

//                System.out.println(String.format("[%s / %dms] %s", l.strTime, l.time, l.content));
                lrcRows.add(l);
            } else {
                // 如果既不是空行也不是时间戳，可能是字幕 ID（如数字 1, 2），直接跳过
                r++;
            }
        }

        // 2. 建立双向链表关系（修复了原代码最后一行无法建立 upRow 的 Bug）
        int size = lrcRows.size();
        for (int i = 0; i < size; i++) {
            LrcRow current = lrcRows.get(i);
            if (i > 0) {
                current.upRow = lrcRows.get(i - 1);
            }
            if (i < size - 1) {
                current.nextRow = lrcRows.get(i + 1);
            }
        }
    }

    /**
     * 辅助方法：将 WebVTT 时间戳字符串转换为毫秒数
     * 支持 "MM:SS.mmm" 和 "HH:MM:SS.mmm" 两种格式
     */
    private long parseTimeToMs(String timeStr) {
        try {
            String[] mainParts = timeStr.split("\\.");
            String[] timeParts = mainParts[0].split(":");

            long ms = Long.parseLong(mainParts[1]); // 毫秒部分

            if (timeParts.length == 3) {
                // HH:MM:SS 格式
                long hours = Long.parseLong(timeParts[0]);
                long minutes = Long.parseLong(timeParts[1]);
                long seconds = Long.parseLong(timeParts[2]);
                return (hours * 3600 + minutes * 60 + seconds) * 1000 + ms;
            } else if (timeParts.length == 2) {
                // MM:SS 格式
                long minutes = Long.parseLong(timeParts[0]);
                long seconds = Long.parseLong(timeParts[1]);
                return (minutes * 60 + seconds) * 1000 + ms;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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
