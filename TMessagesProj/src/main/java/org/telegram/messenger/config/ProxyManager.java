package org.telegram.messenger.config;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.ConnectionsManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by roma on 24.07.2017.
 */

public class ProxyManager {

    private static volatile ProxyManager Instance = null;

    public static ProxyManager getInstance() {
        ProxyManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (ProxyManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ProxyManager();
                }
            }
        }
        return localInstance;
    }

    public void checkProxy(){
        Log.d("ProxyService", "check proxy");

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);

        boolean enableAutomatically = preferences.getBoolean("proxy_enable_automatically", true);
        if (enableAutomatically && ConnectionsManager.isNetworkOnline()) {
            for (String host : Config.ProxySettings.TELEGRAM_HOSTS) {
                if (!isServerReachable(ApplicationLoader.applicationContext, host)) {
                    Log.d("ProxyService", "Unavailable host: " + host);
                    setProxyEnabled(true);
                    return;
                }
                Log.d("ProxyService", "available host: " + host);
            };
            setProxyEnabled(false);
        }
    }

    private boolean isServerReachable(Context context, String url) {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            try {
                URL urlServer = new URL(url);
                HttpURLConnection urlConn = (HttpURLConnection) urlServer.openConnection();
                urlConn.setConnectTimeout(1000); //<- 1Seconds Timeout
                urlConn.connect();
                return true;
            } catch (MalformedURLException e1) {
                Log.d("ProxyService", "exception host: " + url + e1.getMessage());
                return false;
            } catch (IOException e) {
                Log.d("ProxyService", "exception host: " + url + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public void setProxyEnabled(boolean enabled) {
        Log.d("ProxyService", "set proxy enabled: " + enabled);
        SharedPreferences sharedPreferences  = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("proxy_enabled", enabled);
        editor.putBoolean("proxy_enabled_calls", enabled);
        editor.commit();
        if (enabled) {
            ConnectionsManager.native_setProxySettings(
                    sharedPreferences.getString("proxy_ip", ""),
                    sharedPreferences.getInt("proxy_port", 0),
                    sharedPreferences.getString("proxy_user", ""),
                    sharedPreferences.getString("proxy_pass", ""));
        } else {
            ConnectionsManager.native_setProxySettings("", 0, "", "");
        }
    }
}
