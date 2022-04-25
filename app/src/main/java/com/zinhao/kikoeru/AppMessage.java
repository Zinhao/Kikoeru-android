package com.zinhao.kikoeru;

public class AppMessage extends Exception{
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
        return title;
    }

    public Runnable getAction() {
        return action;
    }

    public String getActionName() {
        return actionName;
    }
}
