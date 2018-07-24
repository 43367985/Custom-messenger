package org.telegram.messenger.config;

import android.animation.ArgbEvaluator;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.telegram.ui.ActionBar.Theme;

import java.util.List;

/**
 * Created by roma on 29.04.2017.
 */

public class ThemeManager {
    public static final String THEME_PREF = "theme";

    private static volatile ThemeManager Instance = null;

    public static ThemeManager getInstance() {
        ThemeManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (ThemeManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ThemeManager();
                }
            }
        }
        return localInstance;
    }
    //------------------------------------------------------


    public void createThemes(List<CustomTheme> customThemeList, Bitmap background) {
        if (customThemeList != null) {
            for (CustomTheme customTheme : customThemeList) {
                String name = customTheme.getName();
                if (name == null || name.equals("")) {
                    continue;
                }
                Theme.saveCurrentTheme(name, true);

                if (customTheme.getActionBarColor() != null) {
                    Theme.setColor(Theme.key_actionBarDefault, customTheme.getActionBarColor(), false);
                    Theme.setColor(Theme.key_actionBarDefaultSelector, customTheme.getActionBarColor(), false);
                    Theme.setColor(Theme.key_avatar_backgroundActionBarBlue, customTheme.getActionBarColor(), false);
                    Theme.setColor(Theme.key_avatar_actionBarSelectorBlue, customTheme.getActionBarColor(), false);
                }

                if (customTheme.getActionColor() != null) {

                    Theme.setColor(Theme.key_chats_actionBackground, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_dialogButton, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_dialogInputFieldActivated, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_windowBackgroundWhiteInputFieldActivated, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_chat_emojiPanelIconSelected, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_chat_emojiPanelIconSelector, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_chat_messagePanelSend, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_dialogRadioBackgroundChecked, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_groupcreate_cursor, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_windowBackgroundWhiteBlueHeader, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_switchThumb, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_switchThumbChecked, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_avatar_backgroundBlue, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_avatar_backgroundInProfileBlue, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_chats_menuCloudBackgroundCats, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_chat_messagePanelVoiceBackground, customTheme.getActionColor(), false);
                    Theme.setColor(Theme.key_switchTrackChecked, manipulateColor(customTheme.getActionColor(), 1.4f), false);

                }

                if (customTheme.getTextColor() != null) {
                    Theme.setColor(Theme.key_chats_name, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_windowBackgroundWhiteBlackText, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_windowBackgroundWhiteGrayIcon, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_chats_menuItemText, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_chats_menuItemIcon, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_chat_messagePanelText, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_chat_messageTextIn, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_chat_messageTextOut, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_dialogBadgeText, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_dialogTextBlack, customTheme.getTextColor(), false);
                    Theme.setColor(Theme.key_chats_nameIcon, customTheme.getTextColor(), false);
                }

                if (customTheme.getMessageBarColor() != null) {
                    Theme.setColor(Theme.key_chat_messagePanelBackground, customTheme.getMessageBarColor(), false);
                }

                if (customTheme.getReceivedColor() != null) {
                    Theme.setColor(Theme.key_chat_inBubble, customTheme.getReceivedColor(), false);
                    Theme.setColor(Theme.key_chat_outBubble, customTheme.getSentColor(), false);
                    Theme.setColor(Theme.key_chat_inBubbleSelected, manipulateColor(customTheme.getReceivedColor(), 1.3f), false);
                    Theme.setColor(Theme.key_chat_outBubbleSelected, manipulateColor(customTheme.getSentColor(), 1.3f), false);
                    Theme.setColor(Theme.key_chat_selectedBackground, adjustAlpha(customTheme.getBackgroundColor(), 0.6f), false);
                }

                if (customTheme.getBackgroundColor() != null) {
                    Theme.setColor(Theme.key_windowBackgroundWhite, customTheme.getBackgroundColor(), false);
                    Theme.setColor(Theme.key_windowBackgroundGray, manipulateColor(customTheme.getBackgroundColor(), 0.7f), false);
                    Theme.setColor(Theme.key_graySection, manipulateColor(customTheme.getBackgroundColor(), 0.7f), false);
                    Theme.setColor(Theme.key_divider, manipulateColor(customTheme.getBackgroundColor(), 0.7f), false);
                    Theme.setColor(Theme.key_chats_menuBackground, customTheme.getBackgroundColor(), false);
                    Theme.setColor(Theme.key_dialogBackground, customTheme.getBackgroundColor(), false);
                }
                if (background != null) {
                    Theme.setThemeWallpaper(name, background, null);
                }
                Theme.saveCurrentTheme(name, true);

                if (customTheme.isApplyTheme()) {
                    Theme.applyTheme(Theme.getCurrentTheme());
                }
            }
        }
    }

    public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }

    private int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }
}
