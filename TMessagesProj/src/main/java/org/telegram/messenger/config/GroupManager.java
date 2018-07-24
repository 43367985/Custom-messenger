package org.telegram.messenger.config;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.query.DraftQuery;
import org.telegram.messenger.query.StickersQuery;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ArticleViewer;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.JoinGroupAlert;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PhotoViewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by roma on 27.04.2017.
 */

public class GroupManager {
    public static final String GROUPS_PREF = "groups";
    public static final String ALLOW_SUBSCRIBE = "allowSubscribe";
    public static final String PREVIOUS_LINKS_SET = "prevLinksSet";

    private static volatile GroupManager Instance = null;
    private Lock lock;

    private GroupManager() {
        this.lock = new ReentrantLock();
    }

    private String botUser = null;

    public static GroupManager getInstance() {
        GroupManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (GroupManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new GroupManager();
                }
            }
        }
        return localInstance;
    }
    //------------------------------------------------------

    public void requestGroupLinks(String widgetId, String guid) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://remote.appsgeyser.com/?widgetid="+widgetId +"&guid="+guid)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w("requestGroupLinks", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(Config.CONFIG_PREFERENCES, MODE_PRIVATE);
                    Set<String> previousLinks = preferences.getStringSet(PREVIOUS_LINKS_SET, new HashSet<String>());
                    Log.d("subscribe", previousLinks.toString());
                    JSONArray linkArray = new JSONArray(response.body().string());
                    for (int i=0; i < linkArray.length(); i++){
                        if(!previousLinks.contains(linkArray.getString(i))) {
                            parseGroupUrl(linkArray.getString(i));
                            previousLinks.add(linkArray.getString(i));
                        }
                    }
                    preferences.edit().putStringSet(PREVIOUS_LINKS_SET, previousLinks).apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void joinGroups(List<String> groupUrlList) {
        for (String url : groupUrlList) {
            parseGroupUrl(url);
        }
    }

    private void parseGroupUrl(String url) {
        Log.d("GroupManager", "Group url parse" + url);

        final String initialUrl = url;
        Uri data = Uri.parse(url);

        String username = null;
        String group = null;
        String sticker = null;
        Integer messageId = null;
        String botChat = null;
        String message = null;
        boolean hasUrl = false;

        if (data != null) {
            String scheme = data.getScheme();
            if (scheme != null) {
                if ((scheme.equals("http") || scheme.equals("https"))) {
                    String host = data.getHost().toLowerCase();
                    if (host.equals("telegram.me") || host.equals("t.me") || host.equals("telegram.dog")) {
                        String path = data.getPath();
                        if (path != null && path.length() > 1) {
                            path = path.substring(1);
                            if (path.startsWith("joinchat/")) {
                                group = path.replace("joinchat/", "");
                            } else if (path.startsWith("addstickers/")) {
                                sticker = path.replace("addstickers/", "");
                            } else if (path.startsWith("msg/") || path.startsWith("share/")) {
                                message = data.getQueryParameter("url");
                                if (message == null) {
                                    message = "";
                                }
                                if (data.getQueryParameter("text") != null) {
                                    if (message.length() > 0) {
                                        hasUrl = true;
                                        message += "\n";
                                    }
                                    message += data.getQueryParameter("text");
                                }
                            } else if (path.length() >= 1) {
                                List<String> segments = data.getPathSegments();
                                if (segments.size() > 0) {
                                    username = segments.get(0);
                                    if (segments.size() > 1) {
                                        messageId = Utilities.parseInt(segments.get(1));
                                        if (messageId == 0) {
                                            messageId = null;
                                        }
                                    }
                                }
                                botUser = data.getQueryParameter("start");
                                botChat = data.getQueryParameter("startgroup");
                            }
                        }
                    }
                } else if (scheme.equals("tg")) {
                    if (url.startsWith("tg:resolve") || url.startsWith("tg://resolve")) {
                        url = url.replace("tg:resolve", "tg://telegram.org").replace("tg://resolve", "tg://telegram.org");
                        data = Uri.parse(url);
                        username = data.getQueryParameter("domain");
                        botUser = data.getQueryParameter("start");
                        botChat = data.getQueryParameter("startgroup");
                        messageId = Utilities.parseInt(data.getQueryParameter("post"));
                        if (messageId == 0) {
                            messageId = null;
                        }
                    } else if (url.startsWith("tg:join") || url.startsWith("tg://join")) {
                        url = url.replace("tg:join", "tg://telegram.org").replace("tg://join", "tg://telegram.org");
                        data = Uri.parse(url);
                        group = data.getQueryParameter("invite");
                    } else if (url.startsWith("tg:addstickers") || url.startsWith("tg://addstickers")) {
                        url = url.replace("tg:addstickers", "tg://telegram.org").replace("tg://addstickers", "tg://telegram.org");
                        data = Uri.parse(url);
                        sticker = data.getQueryParameter("set");
                    } else if (url.startsWith("tg:msg") || url.startsWith("tg://msg") || url.startsWith("tg://share") || url.startsWith("tg:share")) {
                        url = url.replace("tg:msg", "tg://telegram.org").replace("tg://msg", "tg://telegram.org").replace("tg://share", "tg://telegram.org").replace("tg:share", "tg://telegram.org");
                        data = Uri.parse(url);
                        message = data.getQueryParameter("url");
                        if (message == null) {
                            message = "";
                        }
                        if (data.getQueryParameter("text") != null) {
                            if (message.length() > 0) {
                                hasUrl = true;
                                message += "\n";
                            }
                            message += data.getQueryParameter("text");
                        }
                    }
                }
            } else {
                if (url.startsWith("@")) {
                    username = url.substring(1);
                }
            }
        }

        if (group != null && !(group.length() == 0)) {
            TLRPC.TL_messages_importChatInvite req = new TLRPC.TL_messages_importChatInvite();
            req.hash = group;
            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(final TLObject response, final TLRPC.TL_error error) {
                    if (error == null) {
                        TLRPC.Updates updates = (TLRPC.Updates) response;
                        MessagesController.getInstance().processUpdates(updates, false);
                        Log.d("GroupManager", "Group added");
                    }else {
                        Log.w("GroupManager", "Group adding error: " + error.text);
                    }
                    TLRPC.Updates updates = (TLRPC.Updates) response;
                    if (updates != null && updates.chats != null && !updates.chats.isEmpty()) {
                        TLRPC.Chat chat = updates.chats.get(0);
                        MessagesController.getInstance().addUserToChat(chat.id, UserConfig.getCurrentUser(), null, 0, null, null);
                    }
                }
            }, ConnectionsManager.RequestFlagFailOnServerErrors);

        }

        if (username != null) {
            final TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = username;
            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(final TLObject response, final TLRPC.TL_error error) {
                    if (error == null) {
                        lock.lock();
                        final TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                        MessagesController.getInstance().putUsers(res.users, false);
                        MessagesController.getInstance().putChats(res.chats, false);
                        MessagesStorage.getInstance().putUsersAndChats(res.users, res.chats, false, true);

                        if (!res.chats.isEmpty()) {
                            int chatId = res.chats.get(0).id;
                            MessagesController.getInstance().addUserToChat(chatId, UserConfig.getCurrentUser(), null, 0, null, null);
                        }
                        if (!res.users.isEmpty()) {
                            TLRPC.User user = res.users.get(0);
                            Log.d("GroupManager", "User "+ user.username);

                            if (user.bot) {
                                MessagesController.getInstance().sendBotStart(user, botUser!= null ? botUser : "start");
                            }
                        }
                        Log.d("GroupManager", "Bot added");
                        lock.unlock();
                    }else {
                        Log.w("GroupManager", "Bot adding error: " + error.text);
                    }
                }
            });
        }
    }
}
