/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.multidex.MultiDex;
import android.util.Base64;
import android.util.Log;

import com.appsgeyser.sdk.AppsgeyserSDK;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.telegram.messenger.config.Config;
import org.telegram.messenger.config.GroupManager;
import org.telegram.messenger.config.ProxyManager;
import org.telegram.messenger.config.ThemeManager;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.Components.ForegroundDetector;

import java.io.File;
import java.util.Set;

public class ApplicationLoader extends Application {

    @SuppressLint("StaticFieldLeak")
    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;
    private static volatile boolean applicationInited = false;
    private static volatile Config config;

    public static volatile boolean isScreenOn = false;
    public static volatile boolean mainInterfacePaused = true;
    public static volatile boolean mainInterfacePausedStageQueue = true;
    public static volatile long mainInterfacePausedStageQueueTime;

    public static File getFilesDirFixed() {
        for (int a = 0; a < 10; a++) {
            File path = ApplicationLoader.applicationContext.getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = applicationContext.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new File("/data/data/org.telegram.messenger/files");
    }

    public static void postInitApplication() {
        if (applicationInited) {
            return;
        }

        applicationInited = true;

        try {
            LocaleController.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            final BroadcastReceiver mReceiver = new ScreenReceiver();
            applicationContext.registerReceiver(mReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            PowerManager pm = (PowerManager)ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            isScreenOn = pm.isScreenOn();
            FileLog.e("screen state = " + isScreenOn);
        } catch (Exception e) {
            FileLog.e(e);
        }

        UserConfig.loadConfig();
        MessagesController.getInstance();
        ConnectionsManager.getInstance();
        if (UserConfig.getCurrentUser() != null) {
            MessagesController.getInstance().putUser(UserConfig.getCurrentUser(), true);
            MessagesController.getInstance().getBlockedUsers(true);
            SendMessagesHelper.getInstance().checkUnsentMessages();

            SharedPreferences configPreference = ApplicationLoader.applicationContext.getSharedPreferences(Config.CONFIG_PREFERENCES, MODE_PRIVATE);
            if(configPreference.getBoolean(GroupManager.ALLOW_SUBSCRIBE, false)) {
                Log.d("subscribe","in");

                final com.appsgeyser.sdk.configuration.Configuration configuration
                        = com.appsgeyser.sdk.configuration.Configuration.getInstance(applicationContext);
                GroupManager.getInstance().requestGroupLinks(applicationContext.getString(R.string.widgetID), configuration.getAppGuid());
            }

        }

        ApplicationLoader app = (ApplicationLoader)ApplicationLoader.applicationContext;
        app.initPlayServices();

        //startProxyService();
        FileLog.e("app initied");

        ContactsController.getInstance().checkAppAccount();
        MediaController.getInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        NativeLoader.initNativeLibs(ApplicationLoader.applicationContext);
        ConnectionsManager.native_setJava(Build.VERSION.SDK_INT == 14 || Build.VERSION.SDK_INT == 15);
        new ForegroundDetector(this);
        AppsgeyserSDK.setApplicationInstance(this);

        applicationHandler = new Handler(applicationContext.getMainLooper());

        startPushService();
        loadConfig();
        createTheme();
        applyProxy();
    }

    /*@Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }*/

    private void loadConfig() {
        config = new Config(getApplicationContext());
    }

    private void createTheme() {
        SharedPreferences preferences = getSharedPreferences(Config.CONFIG_PREFERENCES, MODE_PRIVATE);
        if (!preferences.getBoolean(ThemeManager.THEME_PREF, false)) {
            Log.w("ThemeManager", "ApplyTheme");
            Config config = ApplicationLoader.getConfig();
            ThemeManager.getInstance().createThemes(config.getThemeList(), config.getBackground());
            preferences.edit().putBoolean(ThemeManager.THEME_PREF, true).apply();
        }
    }

    private void applyProxy() {
        SharedPreferences preferences = getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        if (!preferences.getBoolean(Config.ProxySettings.PROXY_PREF, false)) {
            Log.d("ProxySettings", "StartInit");
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("proxy_ip", Config.ProxySettings.ADDRESS);
            editor.putString("proxy_pass", Config.ProxySettings.PASSWORD);
            editor.putString("proxy_user", Config.ProxySettings.USER);
            editor.putInt("proxy_port", Config.ProxySettings.PORT);
            editor.putBoolean("default_proxy", true);
            editor.putBoolean(Config.ProxySettings.PROXY_PREF, true);
            editor.apply();
        }
    }

    public static Config getConfig() {
        return config;
    }

    /*public static void sendRegIdToBackend(final String token) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                UserConfig.pushString = token;
                UserConfig.registeredForPush = false;
                UserConfig.saveConfig(false);
                if (UserConfig.getClientUserId() != 0) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesController.getInstance().registerForPush(token);
                        }
                    });
                }
            }
        });
    }*/

    public static void startPushService() {
        SharedPreferences preferences = applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);

        if (preferences.getBoolean("pushService", true)) {
            applicationContext.startService(new Intent(applicationContext, NotificationsService.class));
        } else {
            stopPushService();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static void startProxyService() {

       /* if(isGooglePlayServicesAvailable()) {
            Log.d("ProxyService", "schedule firebase");

            FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(applicationContext));
            Job myJob = dispatcher.newJobBuilder()
                    .setService(ProxyService.class)
                    .setTag("proxy-check-job")
                    .setRecurring(true)
                    .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                    .setTrigger(Trigger.executionWindow(6 * 60 * 60, 7 * 60 * 60))
                    .setReplaceCurrent(true)
                    .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                    .build();

            dispatcher.mustSchedule(myJob);
        } else {
            Log.d("ProxyService", "schedule support");
            applicationContext.startService(new Intent(applicationContext, ProxySupportService.class));
        }*/
    }

    public static boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(applicationContext);
        return status == ConnectionResult.SUCCESS;
    }

    public static void stopPushService() {
        applicationContext.stopService(new Intent(applicationContext, NotificationsService.class));

        PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
        AlarmManager alarm = (AlarmManager)applicationContext.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pintent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            LocaleController.getInstance().onDeviceConfigurationChange(newConfig);
            AndroidUtilities.checkDisplaySize(applicationContext, newConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayServices() {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (checkPlayServices()) {
                    if (UserConfig.pushString != null && UserConfig.pushString.length() != 0) {
                        FileLog.d("GCM regId = " + UserConfig.pushString);
                    } else {
                        FileLog.d("GCM Registration not found.");
                    }

                    //if (UserConfig.pushString == null || UserConfig.pushString.length() == 0) {
                    Intent intent = new Intent(applicationContext, GcmRegistrationIntentService.class);
                    startService(intent);
                    //} else {
                    //    FileLog.d("GCM regId = " + UserConfig.pushString);
                    //}
                } else {
                    FileLog.d("No valid Google Play Services APK found.");
                }
            }
        }, 1000);
    }

    /*private void initPlayServices() {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (checkPlayServices()) {
                    if (UserConfig.pushString != null && UserConfig.pushString.length() != 0) {
                        FileLog.d("GCM regId = " + UserConfig.pushString);
                    } else {
                        FileLog.d("GCM Registration not found.");
                    }
                    try {
                        if (!FirebaseApp.getApps(ApplicationLoader.applicationContext).isEmpty()) {
                            String token = FirebaseInstanceId.getInstance().getToken();
                            if (token != null) {
                                sendRegIdToBackend(token);
                            }
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                } else {
                    FileLog.d("No valid Google Play Services APK found.");
                }
            }
        }, 2000);
    }*/

    private boolean checkPlayServices() {
        try {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            return resultCode == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return true;

        /*if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("tmessages", "This device is not supported.");
            }
            return false;
        }
        return true;*/
    }
}
