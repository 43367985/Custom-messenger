package org.telegram.messenger.config;

/**
 * Created by roma on 29.04.2017.
 */

public class CustomTheme {
    private String name;
    private Integer actionBarColor;
    private Integer backgroundColor;
    private Integer messageBarColor;
    private Integer receivedColor;
    private Integer sentColor;
    private Integer textColor;
    private Integer actionColor;
    private boolean applyTheme;
    //------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getActionBarColor() {
        return actionBarColor;
    }

    public void setActionBarColor(Integer actionBarColor) {
        this.actionBarColor = actionBarColor;
    }

    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Integer backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Integer getMessageBarColor() {
        return messageBarColor;
    }

    public void setMessageBarColor(Integer messageBarColor) {
        this.messageBarColor = messageBarColor;
    }

    public Integer getReceivedColor() {
        return receivedColor;
    }

    public void setReceivedColor(Integer receivedColor) {
        this.receivedColor = receivedColor;
    }

    public Integer getSentColor() {
        return sentColor;
    }

    public void setSentColor(Integer sentColor) {
        this.sentColor = sentColor;
    }

    public Integer getTextColor() {
        return textColor;
    }

    public void setTextColor(Integer textColor) {
        this.textColor = textColor;
    }

    public Integer getActionColor() {
        return actionColor;
    }

    public void setActionColor(Integer actionColor) {
        this.actionColor = actionColor;
    }

    public boolean isApplyTheme() {
        return applyTheme;
    }

    public void setApplyTheme(boolean applyTheme) {
        this.applyTheme = applyTheme;
    }
}
