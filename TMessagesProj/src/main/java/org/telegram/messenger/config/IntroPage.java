package org.telegram.messenger.config;

import android.graphics.drawable.Drawable;

/**
 * Created by roma on 06.01.2018.
 */

public class IntroPage {

    private Drawable image;
    private String title;
    private String message;

    public IntroPage(Drawable image, String title, String message) {
        this.image = image;
        this.title = title;
        this.message = message;
    }

    public Drawable getImage() {
        return image;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
