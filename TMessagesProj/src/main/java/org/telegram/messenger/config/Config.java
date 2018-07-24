package org.telegram.messenger.config;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by roma on 27.04.2017.
 */

public class Config {
    public static final String CONFIG_PREFERENCES = "ConfigPref";

    private String appName;
    private boolean useDefaultWelcome;
    private Drawable icon;
    private Bitmap background;
    private String faqUrl;
    private String inviteUrl;
    private List<String> stickerList;
    private List<String> groupList;
    private List<CustomTheme> themeList;
    private CustomTheme defaultTheme;
    JSONObject settings;

    public Config(Context context) {

        try {
            settings = new JSONObject(loadSettings(context));
            //appName = settings.getString("name");
            appName = context.getString(R.string.app_name);
            faqUrl = settings.getString("faqUrl");
            inviteUrl = settings.getString("inviteUrl");
            useDefaultWelcome = settings.getBoolean("useDefaultWelcome");
            stickerList = readStickerList(settings);
            groupList = readGroupsList(settings);
            themeList = readThemeList(settings);
            defaultTheme = readTheme(settings, true);
            themeList.add(defaultTheme);

            String iconUrl = settings.getString("icon");
            if (iconUrl != null && !iconUrl.equals("")) {
                Bitmap b = BitmapFactory.decodeStream(context.getAssets().open(iconUrl));
                b.setDensity(Bitmap.DENSITY_NONE);
                icon = new BitmapDrawable(context.getResources(), b);
            }
            String backgroundUrl = settings.getString("backgroundImage");
            if (backgroundUrl != null && !backgroundUrl.equals("")) {
                background = BitmapFactory.decodeStream(context.getAssets().open(backgroundUrl));
            }

        } catch (JSONException e) {
            Log.e("Config", "Json parse error: " + e.getMessage());
        } catch (IOException e) {
            Log.e("Config", "Json read error: " + e.getMessage());
        }
    }
    //------------------------------------------------------

    private List<CustomTheme> readThemeList(JSONObject settings) throws JSONException {
        List<CustomTheme> customThemeList = new ArrayList<>();
        for (int i = 0; i < settings.getJSONArray("theme").length(); i++) {
            customThemeList.add(readTheme(settings.getJSONArray("theme").getJSONObject(i), false));
        }
        return customThemeList;
    }

    private CustomTheme readTheme(JSONObject jsonTheme, boolean defaultTheme) throws JSONException {
        CustomTheme customTheme = new CustomTheme();
        if (defaultTheme) {
            customTheme.setName(getAppName());
            customTheme.setApplyTheme(true);
        } else {
            customTheme.setName(jsonTheme.getString("themeName"));
        }
        customTheme.setActionBarColor(readColor(jsonTheme, "actionBarColor"));
        customTheme.setBackgroundColor(readColor(jsonTheme, "backgroundColor"));
        customTheme.setMessageBarColor(readColor(jsonTheme, "messageBarColor"));
        customTheme.setReceivedColor(readColor(jsonTheme, "receivedColor"));
        customTheme.setSentColor(readColor(jsonTheme, "sentColor"));
        customTheme.setTextColor(readColor(jsonTheme, "textColor"));
        customTheme.setActionColor(readColor(jsonTheme, "actionColor"));
        return customTheme;
    }

    private Integer readColor(JSONObject jsonTheme, String name) throws JSONException {
        String color = jsonTheme.getString(name);
        if (color == null || color.equals("")) {
            return null;
        }
        return Color.parseColor("#" + color);
    }

    private List<String> readStickerList(JSONObject settings) throws JSONException {
        JSONArray jArray = settings.getJSONArray("stickers");
        List<String> resultList = new ArrayList<>();
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                resultList.add(jArray.getJSONObject(i).getString("stickerUrl"));
            }
        }
        return resultList;
    }

    private List<String> readGroupsList(JSONObject settings) throws JSONException {
        JSONArray jArray = settings.getJSONArray("groups");
        List<String> resultList = new ArrayList<>();
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                resultList.add(jArray.getJSONObject(i).getString("groupsUrl"));
            }
        }
        return resultList;
    }

    public Bitmap getBackground() {
        return background;
    }

    public String loadSettings(Context context) throws IOException {
        String json = null;
        try {
            InputStream is = context.getAssets().open("settings.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public List<IntroPage> getIntroPages(Context context){
        List<IntroPage> list = new ArrayList<>();
        try {
            JSONArray jsonArray = settings.getJSONArray("welcomeSteps");
            for (int i=0; i < jsonArray.length(); i++){
                JSONObject object = jsonArray.getJSONObject(i);
                String iconUrl = object.getString("stepImage");
                Drawable icon = null;
                if (iconUrl != null && !iconUrl.equals("")) {
                    Bitmap b = BitmapFactory.decodeStream(context.getAssets().open(iconUrl));
                    b.setDensity(Bitmap.DENSITY_NONE);
                    icon = new BitmapDrawable(context.getResources(), b);
                }
                list.add(new IntroPage(icon, object.getString("stepTitle"), object.getString("stepText")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    //------------------------------------------------------

    public List<String> getGroupList() {
        return groupList;
    }

    public List<CustomTheme> getThemeList() {
        return themeList;
    }

    public List<String> getStickerList() {
        return stickerList;
    }

    public String getFaqUrl() {
        return faqUrl;
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isUseDefaultWelcome() {
        return useDefaultWelcome;
    }

    public CustomTheme getDefaultTheme() {
        return defaultTheme;
    }

    //------------------------------------------------------

    public static class ProxySettings{
        public static final String[] TELEGRAM_HOSTS = {
                "https://telegram.me/",
                "https://telegram.org/",
                "https://core.telegram.org/",
                "https://desktop.telegram.org/",
                "https://macos.telegram.org/",
                "https://web.telegram.org/",
                "https://venus.web.telegram.org/",
                "https://pluto.web.telegram.org/",
                "https://flora.web.telegram.org/",
                "https://flora-1.web.telegram.org/"
        };

        public static final String PROXY_PREF ="proxy_applied";
        public static final String ADDRESS ="prx.appioapp.com";
        public static final  String USER = "teleuser1";
        public static final  String PASSWORD = "hop7oogeiboK";
        public static final int PORT = 1081;
    }
}
