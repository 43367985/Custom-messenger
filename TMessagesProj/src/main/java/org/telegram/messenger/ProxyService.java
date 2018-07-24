/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.telegram.messenger.config.Config;
import org.telegram.messenger.config.ProxyManager;
import org.telegram.tgnet.ConnectionsManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class ProxyService extends JobService {

    @Override
    public boolean onStartJob(JobParameters job) {
        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                ProxyManager.getInstance().checkProxy();
                return null;
            }
        };
        asyncTask.execute();

        Log.d("ProxyService", "onStart");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Log.d("ProxyService", "onStop");

        return true;
    }
}