package com.zinhao.kikoeru;

import java.util.ArrayList;
import java.util.List;

public class Text {
    public static final Text NONE = new Text("");
    private String text;
    private static final String TAG = "Text";
    private List<TextRow> textRows;

    private int currentIndex = -1;

    public Text(String text) {
        this.text = text;
        textRows = new ArrayList<>();
        String[] rows = text.split("\n");
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            TextRow textRow = new TextRow(row);
            textRows.add(textRow);
        }
        for (int i = 1; i < textRows.size() - 1; i++) {
            TextRow textRow = textRows.get(i);
            textRow.upRow = textRows.get(i - 1);
            textRow.nextRow = textRows.get(i + 1);
        }
    }

    public String getText() {
        return text;
    }

    public List<TextRow> getLrcRows() {
        return textRows;
    }

    public static class TextRow {
        public static final TextRow NONE = new TextRow("");
        public TextRow upRow = NONE;
        public TextRow nextRow = NONE;
        public String content;

        public TextRow(String content) {
            this.content = content;
        }

        public TextRow getUpRow() {
            if (upRow == null)
                return NONE;
            return upRow;
        }

        public TextRow getNextRow() {
            if (nextRow == null) {
                return NONE;
            }
            return nextRow;
        }
    }

}
