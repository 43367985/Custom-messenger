/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.config.Config;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

import java.net.URLEncoder;
import java.util.ArrayList;

public class ProxySettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private final static int FIELD_IP = 0;
    private final static int FIELD_PORT = 1;
    private final static int FIELD_USER = 2;
    private final static int FIELD_PASSWORD = 3;

    private ActionBarMenuItem shareItem;
    private EditText[] inputFields;
    private ScrollView scrollView;
    private LinearLayout linearLayout2;
    private HeaderCell headerCell;
    private ArrayList<View> dividers = new ArrayList<>();
    private ShadowSectionCell sectionCell;
    private TextInfoPrivacyCell bottomCell;
    private TextInfoPrivacyCell useForCallsInfoCell;
    private TextCheckCell checkCell1;
    private TextCheckCell checkCellAutoProxy;
    private TextCheckCell useForCallsCell;

    private boolean enableProxyAutomatically;
    private boolean useProxySettings;
    private boolean useProxyForCalls;

    private boolean ignoreOnTextChange;

    private static final int share_item = 1;
    private boolean reseting;
    private boolean defaultSettings;
    private TextWatcher textWatcher;

    @Override
    public void onResume() {
        super.onResume();
        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
    }

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.proxySettingsChanged);
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).edit();
        editor.putBoolean("proxy_enabled", useProxySettings);
        editor.putBoolean("proxy_enabled_calls", useProxyForCalls);
        editor.putBoolean("proxy_enable_automatically", enableProxyAutomatically);
        String address = defaultSettings ? Config.ProxySettings.ADDRESS : inputFields[FIELD_IP].getText().toString();
        String password = defaultSettings ? Config.ProxySettings.PASSWORD : inputFields[FIELD_PASSWORD].getText().toString();
        String user = defaultSettings ? Config.ProxySettings.USER : inputFields[FIELD_USER].getText().toString();
        int port = defaultSettings ? Config.ProxySettings.PORT : Utilities.parseInt(inputFields[FIELD_PORT].getText().toString());
        editor.putString("proxy_ip", address);
        editor.putString("proxy_pass", password);
        editor.putString("proxy_user", user);
        editor.putInt("proxy_port", port);
        editor.putBoolean("default_proxy", defaultSettings);
        editor.commit();
        if (useProxySettings) {
            ConnectionsManager.native_setProxySettings(address, port, user, password);
        } else {
            ConnectionsManager.native_setProxySettings("", 0, "", "");
        }
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        enableProxyAutomatically = preferences.getBoolean("proxy_enable_automatically", true);
        useProxySettings = preferences.getBoolean("proxy_enabled", false);
        useProxyForCalls = preferences.getBoolean("proxy_enabled_calls", false);

        defaultSettings = preferences.getBoolean("default_proxy", false);

        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (defaultSettings && inputFields[3] != null && inputFields[2] != null && inputFields[1] != null && inputFields[0] != null) {
                    defaultSettings = false;
                    inputFields[0].setText("");
                    inputFields[1].setText("0");
                    inputFields[2].setText("");
                    inputFields[3].setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        actionBar.setTitle(LocaleController.getString("ProxySettings", R.string.ProxySettings));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == share_item) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    StringBuilder params = new StringBuilder("");
                    String address = inputFields[FIELD_IP].getText().toString();
                    String password = inputFields[FIELD_PASSWORD].getText().toString();
                    String user = inputFields[FIELD_USER].getText().toString();
                    String port = inputFields[FIELD_PORT].getText().toString();
                    try {
                        if (!TextUtils.isEmpty(address)) {
                            params.append("server=").append(URLEncoder.encode(address, "UTF-8"));
                        }
                        if (!TextUtils.isEmpty(port)) {
                            if (params.length() != 0) {
                                params.append("&");
                            }
                            params.append("port=").append(URLEncoder.encode(port, "UTF-8"));
                        }
                        if (!TextUtils.isEmpty(user)) {
                            if (params.length() != 0) {
                                params.append("&");
                            }
                            params.append("user=").append(URLEncoder.encode(user, "UTF-8"));
                        }
                        if (!TextUtils.isEmpty(password)) {
                            if (params.length() != 0) {
                                params.append("&");
                            }
                            params.append("pass=").append(URLEncoder.encode(password, "UTF-8"));
                        }
                    } catch (Exception ignore) {
                        return;
                    }
                    if (params.length() == 0) {
                        return;
                    }
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "https://t.me/socks?" + params.toString());
                    Intent chooserIntent = Intent.createChooser(shareIntent, LocaleController.getString("ShareLink", R.string.ShareLink));
                    chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getParentActivity().startActivity(chooserIntent);
                }
            }
        });

        shareItem = actionBar.createMenu().addItem(share_item, R.drawable.abc_ic_menu_share_mtrl_alpha);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        AndroidUtilities.setScrollViewEdgeEffectColor(scrollView, Theme.getColor(Theme.key_actionBarDefault));
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout2, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        checkCellAutoProxy = new TextCheckCell(context);
        checkCellAutoProxy.setBackgroundDrawable(Theme.getSelectorDrawable(true));
        checkCellAutoProxy.setTextAndCheck(LocaleController.getString("EnableProxyAutomatically", R.string.EnableProxyAutomatically), enableProxyAutomatically, false);
        linearLayout2.addView(checkCellAutoProxy, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        checkCellAutoProxy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableProxyAutomatically = !enableProxyAutomatically;
                checkCellAutoProxy.setChecked(enableProxyAutomatically);
            }
        });

        checkCell1 = new TextCheckCell(context);
        checkCell1.setBackgroundDrawable(Theme.getSelectorDrawable(true));
        checkCell1.setTextAndCheck(LocaleController.getString("UseProxySettings", R.string.UseProxySettings), useProxySettings, false);
        linearLayout2.addView(checkCell1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        checkCell1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useProxySettings = !useProxySettings;
                checkCell1.setChecked(useProxySettings);
                if (!useProxySettings)
                    useForCallsCell.setChecked(false);
                useForCallsCell.setEnabled(useProxySettings);
            }
        });

        sectionCell = new ShadowSectionCell(context);
        linearLayout2.addView(sectionCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        /*headerCell = new HeaderCell(context);
        headerCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        headerCell.setText(LocaleController.getString("PaymentShippingAddress", R.string.PaymentShippingAddress));
        linearLayout2.addView(headerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));*/

        inputFields = new EditText[4];
        for (int a = 0; a < 4; a++) {
            FrameLayout container = new FrameLayout(context);
            linearLayout2.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            container.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            boolean allowDivider = a != FIELD_PASSWORD;
            if (allowDivider) {
                View divider = new View(context);
                dividers.add(divider);
                divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
                container.addView(divider, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.LEFT | Gravity.BOTTOM));
            }

            inputFields[a] = new EditText(context);
            inputFields[a].setTag(a);
            inputFields[a].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            inputFields[a].setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
            inputFields[a].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            inputFields[a].setBackgroundDrawable(null);
            AndroidUtilities.clearCursorDrawable(inputFields[a]);

            if (a == FIELD_IP) {
                inputFields[a].addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        checkShareButton();
                    }
                });
            } else if (a == FIELD_PORT) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_NUMBER);
                inputFields[a].addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (ignoreOnTextChange) {
                            return;
                        }
                        EditText phoneField = inputFields[FIELD_PORT];
                        int start = phoneField.getSelectionStart();
                        String chars = "0123456789";
                        String str = phoneField.getText().toString();
                        StringBuilder builder = new StringBuilder(str.length());
                        for (int a = 0; a < str.length(); a++) {
                            String ch = str.substring(a, a + 1);
                            if (chars.contains(ch)) {
                                builder.append(ch);
                            }
                        }
                        ignoreOnTextChange = true;
                        boolean changed;
                        int port = Utilities.parseInt(builder.toString());
                        if (port < 0 || port > 65535 || !str.equals(builder.toString())) {
                            if (port < 0) {
                                phoneField.setText("0");
                            } else if (port > 65535) {
                                phoneField.setText("65535");
                            } else {
                                phoneField.setText(builder.toString());
                            }
                        } else {
                            if (start >= 0) {
                                phoneField.setSelection(start <= phoneField.length() ? start : phoneField.length());
                            }
                        }
                        ignoreOnTextChange = false;
                        checkShareButton();
                    }
                });
            } else if (a == FIELD_PASSWORD) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                inputFields[a].setTypeface(Typeface.DEFAULT);
                inputFields[a].setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT);
            }
            inputFields[a].setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            switch (a) {
                case FIELD_IP:
                    inputFields[a].setHint(LocaleController.getString("UseProxyAddress", R.string.UseProxyAddress));
                    inputFields[a].setText(defaultSettings ? "default_proxy" : preferences.getString("proxy_ip", ""));

                    break;
                case FIELD_PASSWORD:
                    inputFields[a].setHint(LocaleController.getString("UseProxyPassword", R.string.UseProxyPassword));
                    inputFields[a].setText(defaultSettings ? "default_pass" : preferences.getString("proxy_pass", ""));
                    break;
                case FIELD_PORT:
                    inputFields[a].setHint(LocaleController.getString("UseProxyPort", R.string.UseProxyPort));
                    inputFields[a].setText(defaultSettings ? "0" : "" + preferences.getInt("proxy_port", 1080));
                    break;
                case FIELD_USER:
                    inputFields[a].setHint(LocaleController.getString("UseProxyUsername", R.string.UseProxyUsername));
                    inputFields[a].setText(defaultSettings ? "default_username" : preferences.getString("proxy_user", ""));
                    break;
            }
            inputFields[a].setSelection(inputFields[a].length());


            inputFields[a].setPadding(0, 0, 0, AndroidUtilities.dp(6));
            inputFields[a].setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            container.addView(inputFields[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 17, 12, 17, 6));

            inputFields[a].setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_NEXT) {
                        int num = (Integer) textView.getTag();
                        if (num + 1 < inputFields.length) {
                            num++;
                            inputFields[num].requestFocus();
                        }
                        return true;
                    } else if (i == EditorInfo.IME_ACTION_DONE) {
                        finishFragment();
                        return true;
                    }
                    return false;
                }
            });
            inputFields[a].addTextChangedListener(textWatcher);
        }

        bottomCell = new TextInfoPrivacyCell(context);
        bottomCell.setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        bottomCell.setText(LocaleController.getString("UseProxyInfo", R.string.UseProxyInfo));
        linearLayout2.addView(bottomCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        useForCallsCell = new TextCheckCell(context);
        useForCallsCell.setBackgroundDrawable(Theme.getSelectorDrawable(true));
        useForCallsCell.setTextAndCheck(LocaleController.getString("UseProxyForCalls", R.string.UseProxyForCalls), useProxyForCalls, false);
        useForCallsCell.setEnabled(useProxySettings);
        linearLayout2.addView(useForCallsCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        useForCallsCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useProxyForCalls = !useProxyForCalls;
                useForCallsCell.setChecked(useProxyForCalls);
            }
        });

        useForCallsInfoCell = new TextInfoPrivacyCell(context);
        useForCallsInfoCell.setBackgroundDrawable(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        useForCallsInfoCell.setText(LocaleController.getString("UseProxyForCallsInfo", R.string.UseProxyForCallsInfo));
        linearLayout2.addView(useForCallsInfoCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextSettingsCell settingsCell = new TextSettingsCell(context);
        settingsCell.setText(LocaleController.getString("ResetAllProxy", R.string.ResetAllProxy), false);
        settingsCell.setBackgroundDrawable(Theme.getSelectorDrawable(true));
        settingsCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (reseting) {
                    return;
                }
                reseting = true;
                for (int i =0; i < 4; i++){
                    inputFields[i].removeTextChangedListener(textWatcher);
                }
                inputFields[FIELD_IP].setText("default_proxy");
                inputFields[FIELD_PORT].setText("0");
                inputFields[FIELD_USER].setText("default_username");
                inputFields[FIELD_PASSWORD].setText("default_pass");
                for (int i =0; i < 4; i++){
                    inputFields[i].addTextChangedListener(textWatcher);
                }
                defaultSettings = true;
                checkCellAutoProxy.setChecked(true);
                enableProxyAutomatically = true;
                if (getParentActivity() != null) {
                    Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("ResetProxyText", R.string.ResetProxyText), Toast.LENGTH_SHORT);
                    toast.show();
                }
                Log.d("ProxySettings", "reset");
                reseting = false;
            }
        });
        linearLayout2.addView(settingsCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        checkShareButton();

        return fragmentView;
    }

    private void checkShareButton() {
        if (inputFields[FIELD_IP] == null || inputFields[FIELD_PORT] == null) {
            return;
        }
        if (inputFields[FIELD_IP].length() != 0 && Utilities.parseInt(inputFields[FIELD_PORT].getText().toString()) != 0) {
            shareItem.setAlpha(1.0f);
            shareItem.setEnabled(true);
        } else {
            shareItem.setAlpha(0.5f);
            shareItem.setEnabled(false);
        }
    }

    @Override
    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && !backward) {
            inputFields[FIELD_IP].requestFocus();
            AndroidUtilities.showKeyboard(inputFields[FIELD_IP]);
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.proxySettingsChanged) {
            if (checkCell1 == null) {
                return;
            }
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
            useProxySettings = preferences.getBoolean("proxy_enabled", false);
            if (!useProxySettings) {
                checkCell1.setChecked(false);
            } else {
                checkCell1.setChecked(true);
                for (int a = 0; a < 4; a++) {
                    switch (a) {
                        case FIELD_IP:
                            inputFields[a].setText(preferences.getString("proxy_ip", ""));
                            break;
                        case FIELD_PASSWORD:
                            inputFields[a].setText(preferences.getString("proxy_pass", ""));
                            break;
                        case FIELD_PORT:
                            inputFields[a].setText("" + preferences.getInt("proxy_port", 1080));
                            break;
                        case FIELD_USER:
                            inputFields[a].setText(preferences.getString("proxy_user", ""));
                            break;
                    }
                }
            }
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(scrollView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));
        arrayList.add(new ThemeDescription(linearLayout2, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        if (inputFields != null) {
            for (int a = 0; a < inputFields.length; a++) {
                arrayList.add(new ThemeDescription((View) inputFields[a].getParent(), ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
            }
        } else {
            arrayList.add(new ThemeDescription(null, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(null, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        }
        arrayList.add(new ThemeDescription(headerCell, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(headerCell, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        arrayList.add(new ThemeDescription(sectionCell, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        arrayList.add(new ThemeDescription(bottomCell, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        arrayList.add(new ThemeDescription(bottomCell, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
        arrayList.add(new ThemeDescription(bottomCell, ThemeDescription.FLAG_LINKCOLOR, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));
        for (int a = 0; a < dividers.size(); a++) {
            arrayList.add(new ThemeDescription(dividers.get(a), ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_divider));
        }

        arrayList.add(new ThemeDescription(checkCell1, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(checkCell1, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchThumb));
        arrayList.add(new ThemeDescription(checkCell1, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        arrayList.add(new ThemeDescription(checkCell1, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchThumbChecked));
        arrayList.add(new ThemeDescription(checkCell1, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));
        arrayList.add(new ThemeDescription(checkCell1, ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(checkCell1, ThemeDescription.FLAG_SELECTORWHITE, null, null, null, null, Theme.key_listSelector));

        return arrayList.toArray(new ThemeDescription[arrayList.size()]);
    }
}
