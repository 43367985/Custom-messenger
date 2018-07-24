package org.telegram.messenger.config;

import android.net.Uri;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.query.StickersQuery;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.List;

/**
 * Created by roma on 27.04.2017.
 */

public class StickerManager {
    public static final String STICKERS_PREF = "stickers";

    private static volatile StickerManager Instance = null;

    public static StickerManager getInstance() {
        StickerManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (StickerManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new StickerManager();
                }
            }
        }
        return localInstance;
    }
    //------------------------------------------------------

    public void addStickerSets(List<String> stickerUrlList) {
        for (String url : stickerUrlList) {
            addStickerSet(url);
        }
    }

    private void addStickerSet(String url) {
        Uri data = Uri.parse(url);
        String sticker = null;
        if (data != null) {
            String scheme = data.getScheme();
            if (scheme != null) {
                if ((scheme.equals("http") || scheme.equals("https"))) {
                    String host = data.getHost().toLowerCase();
                    if (host.equals("telegram.me") || host.equals("telegram.dog") || host.equals("tlgrm.ru")) {
                        String path = data.getPath();
                        if (path != null && path.length() > 1) {
                            path = path.substring(1);

                            if (path.startsWith("addstickers/")) {
                                sticker = path.replace("addstickers/", "");
                            } else if (path.startsWith("stickers/")) {
                                sticker = path.replace("stickers/", "");
                            }
                        }
                    }
                } else if (scheme.equals("tg")) {
                    if (url.startsWith("tg:addstickers") || url.startsWith("tg://addstickers")) {
                        url = url.replace("tg:addstickers", "tg://telegram.org").replace("tg://addstickers", "tg://telegram.org");
                        data = Uri.parse(url);
                        sticker = data.getQueryParameter("set");
                    }
                }
            }
        }
        if (sticker != null) {
            TLRPC.InputStickerSet inputStickerSet = new TLRPC.TL_inputStickerSetShortName();
            inputStickerSet.short_name = sticker;
            TLRPC.TL_messages_installStickerSet req = new TLRPC.TL_messages_installStickerSet();
            req.stickerset = inputStickerSet;
            final String finalSticker = sticker;
            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(final TLObject response, final TLRPC.TL_error error) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (error == null) {
                                    Log.d("StickerManager", "Sticker added: " + finalSticker);
                                } else {
                                    Log.w("StickerManager", "ErrorOccurred: " + finalSticker);
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            StickersQuery.loadStickers(StickersQuery.TYPE_IMAGE, false, true);
                        }
                    });
                }
            });
        }
    }
}
