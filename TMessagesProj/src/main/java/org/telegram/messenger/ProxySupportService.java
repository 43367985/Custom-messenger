/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.telegram.messenger.config.Config;
import org.telegram.messenger.config.ProxyManager;
import org.telegram.tgnet.ConnectionsManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class ProxySupportService extends Service {

    private final static long PROXY_SCAN_PERIOD = 6 * 60 * 60 * 1000;

    private Timer timer;

    @Override
    public void onCreate() {
        Log.d("ProxyService", "service started");
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new ProxyTask(), PROXY_SCAN_PERIOD, PROXY_SCAN_PERIOD);
    }

    private class ProxyTask extends TimerTask {

        @Override
        public void run() {
            Log.d("ProxyService", "check");
            ProxyManager.getInstance().checkProxy();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}