package com.zinhao.kikoeru;

public class AppMessage extends Exception {
    String title;
    Runnable action;
    String actionName;

    public AppMessage(String title, String message, Runnable action, String actionName) {
        super(message);
        this.title = title;
        this.action = action;
        this.actionName = actionName;
    }

    public String getTitle() {
        if (title == null)
            return "";
        return title;
    }

    public Runnable NONE = new Runnable() {
        @Override
        public void run() {
        }
    };

    public Runnable getAction() {
        if (action == null) {
            return NONE;
        }
        return action;
    }

    public String getActionName() {
        if (title == null)
            return "no name";
        return actionName;
    }
}
