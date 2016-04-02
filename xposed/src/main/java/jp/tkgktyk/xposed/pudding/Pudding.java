package jp.tkgktyk.xposed.pudding;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import jp.tkgktyk.xposed.pudding.app.util.ActionInfo;

/**
 * Created by tkgktyk on 2/6/16.
 */
public class Pudding {
    public static final String PACKAGE_NAME = Pudding.class.getPackage().getName();
    public static final String NAME = Pudding.class.getSimpleName();
    public static final String PREFIX_ACTION = PACKAGE_NAME + ".intent.action.";
    public static final String PREFIX_EXTRA = PACKAGE_NAME + ".intent.extra.";

    // key actions
    public static final String ACTION_BACK = PREFIX_ACTION + "BACK";
    public static final String ACTION_HOME = PREFIX_ACTION + "HOME";
    public static final String ACTION_RECENTS = PREFIX_ACTION + "RECENTS";
    public static final String ACTION_LEFT = PREFIX_ACTION + "LEFT";
    public static final String ACTION_RIGHT = PREFIX_ACTION + "RIGHT";
    public static final String ACTION_REFRESH = PREFIX_ACTION + "REFRESH";
    public static final String ACTION_MOVE_HOME = PREFIX_ACTION + "SCROLL_HOME";
    public static final String ACTION_MOVE_END = PREFIX_ACTION + "SCROLL_END";
    public static final String ACTION_LOCK_SCREEN = PREFIX_ACTION + "LOCK_SCREEN";
    // status bar
    public static final String ACTION_NOTIFICATIONS = PREFIX_ACTION + "NOTIFICATIONS";
    public static final String ACTION_QUICK_SETTINGS = PREFIX_ACTION + "QUICK_SETTINGS";
    // other internal functions
    public static final String ACTION_KILL = PREFIX_ACTION + "KILL";
    public static final String ACTION_POWER_MENU = PREFIX_ACTION + "POWER_MENU";

    public static final IntentFilter INTERNAL_ACTION_FILTER;

    private static class Entry {
        final int nameId;
        final int iconId;

        Entry(@StringRes int nameId, @DrawableRes int iconId) {
            this.nameId = nameId;
            this.iconId = iconId;
        }
    }

    private static final Map<String, Entry> ENTRIES = Maps.newHashMap();

    /**
     * Entry actions
     */
    static {
        //
        // key action
        //
        ENTRIES.put(ACTION_BACK, new Entry(R.string.action_back, R.drawable.ic_arrow_back_black_24dp));
        ENTRIES.put(ACTION_HOME, new Entry(R.string.action_home, R.drawable.ic_home_black_24dp));
        ENTRIES.put(ACTION_RECENTS, new Entry(R.string.action_recents, R.drawable.ic_history_black_24dp));
        ENTRIES.put(ACTION_LEFT, new Entry(R.string.action_left, R.drawable.ic_arrow_back_black_24dp));
        ENTRIES.put(ACTION_RIGHT, new Entry(R.string.action_right, R.drawable.ic_arrow_forward_black_24dp));
        ENTRIES.put(ACTION_REFRESH, new Entry(R.string.action_refresh, R.drawable.ic_refresh_black_24dp));
        ENTRIES.put(ACTION_MOVE_HOME, new Entry(R.string.action_move_home, R.drawable.ic_vertical_align_top_black_24dp));
        ENTRIES.put(ACTION_MOVE_END, new Entry(R.string.action_move_end, R.drawable.ic_vertical_align_bottom_black_24dp));
        ENTRIES.put(ACTION_LOCK_SCREEN, new Entry(R.string.action_lock_screen, R.drawable.ic_lock_black_24dp));
        //
        // status bar
        //
        ENTRIES.put(ACTION_NOTIFICATIONS, new Entry(R.string.action_notifications, R.drawable.ic_notifications_black_24dp));
        ENTRIES.put(ACTION_QUICK_SETTINGS, new Entry(R.string.action_quick_settings, R.drawable.ic_settings_black_24dp));
        //
        // other internal functions
        //
        ENTRIES.put(ACTION_KILL, new Entry(R.string.action_kill, R.drawable.ic_close_black_24dp));
        ENTRIES.put(ACTION_POWER_MENU, new Entry(R.string.action_power_menu, R.drawable.ic_power_settings_new_black_24dp));
    }

    /**
     * IntentFilters initialization
     */
    static {
        INTERNAL_ACTION_FILTER = new IntentFilter();
        // key action
        INTERNAL_ACTION_FILTER.addAction(ACTION_BACK);
        INTERNAL_ACTION_FILTER.addAction(ACTION_HOME);
        INTERNAL_ACTION_FILTER.addAction(ACTION_RECENTS);
        INTERNAL_ACTION_FILTER.addAction(ACTION_LEFT);
        INTERNAL_ACTION_FILTER.addAction(ACTION_RIGHT);
        INTERNAL_ACTION_FILTER.addAction(ACTION_REFRESH);
        INTERNAL_ACTION_FILTER.addAction(ACTION_MOVE_HOME);
        INTERNAL_ACTION_FILTER.addAction(ACTION_MOVE_END);
        INTERNAL_ACTION_FILTER.addAction(ACTION_LOCK_SCREEN);
        // status bar
        INTERNAL_ACTION_FILTER.addAction(ACTION_NOTIFICATIONS);
        INTERNAL_ACTION_FILTER.addAction(ACTION_QUICK_SETTINGS);
        // other internal
        INTERNAL_ACTION_FILTER.addAction(ACTION_KILL);
        INTERNAL_ACTION_FILTER.addAction(ACTION_POWER_MENU);
    }

    private static Context mModContext;

    public static Context getModContext(Context context) {
        if (mModContext != null) {
            return mModContext;
        }
        try {
            if (context.getPackageName().equals(Pudding.PACKAGE_NAME)) {
                mModContext = context;
            } else {
                mModContext = context.createPackageContext(
                        Pudding.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            }
        } catch (Throwable t) {
        }
        return mModContext;
    }

    @NonNull
    public static String getActionName(Context context, String action) {
        Entry entry = ENTRIES.get(action);
        if (entry != null) {
            Context mod = getModContext(context);
            return mod.getString(ENTRIES.get(action).nameId);
        }
        return "";
    }

    @DrawableRes
    public static int getActionIconResource(String action) {
        Entry entry = ENTRIES.get(action);
        if (entry != null) {
            return entry.iconId;
        }
        return 0;
    }


    public static boolean performAction(@NonNull View view,
                                        @NonNull ActionInfo actionInfo) {
        return actionInfo.launch(getModContext(view.getContext()));
    }

    /**
     * for Settings UI
     *
     * @param context
     * @return
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    public static SharedPreferences getSharedPreferences(Context context) {
        return getModContext(context).getSharedPreferences(PACKAGE_NAME + "_preferences", Context.MODE_WORLD_READABLE);
    }

    /**
     * for Settings UI
     *
     * @param context
     * @return
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    public static SharedPreferences getSharedPreferences(Context context, String name) {
        return getModContext(context).getSharedPreferences(name, Context.MODE_WORLD_READABLE);
    }

    public static class Settings implements Serializable {
        static final long serialVersionUID = 1L;

        // General
        public final boolean whitelistMode;
        public final Set<String> blacklist;
        public final boolean vibrate;
        public final boolean marginForDrawer;
        public final boolean singleTouch;
        public final boolean workaround1;

        // Overscroll Action
        public final Actions actions;

        public Settings(SharedPreferences prefs) {
            this(prefs, prefs);
        }

        public Settings(SharedPreferences prefs, SharedPreferences actionPrefs) {

            // General
            // Detector
            whitelistMode = prefs.getBoolean("key_whitelist_mode", false);
            blacklist = prefs.getStringSet("key_blacklist", Sets.<String>newHashSet());
            vibrate = prefs.getBoolean("key_vibration", true);
            marginForDrawer = prefs.getBoolean("key_margin_for_drawer", true);
            singleTouch = prefs.getBoolean("key_single_touch", true);
            workaround1 = prefs.getBoolean("key_workaround1", true);

            // Overscroll Action
            actions = new Actions(actionPrefs);
        }

        private String getStringToParse(SharedPreferences prefs, String key, String defValue) {
            String str = prefs.getString(key, defValue);
            if (Strings.isNullOrEmpty(str)) {
                str = defValue;
            }
            return str;
        }

        public static class Actions implements Serializable {
            static final long serialVersionUID = 1L;

            public ActionInfo.Record top;
            public ActionInfo.Record left;
            public ActionInfo.Record bottom;
            public ActionInfo.Record right;

            public Actions(SharedPreferences prefs) {
                top = getActionRecord(prefs, "key_action_top");
                left = getActionRecord(prefs, "key_action_left");
                bottom = getActionRecord(prefs, "key_action_bottom");
                right = getActionRecord(prefs, "key_action_right");
            }

            private ActionInfo.Record getActionRecord(SharedPreferences prefs, String key) {
                return ActionInfo.Record.fromPreference(prefs.getString(key, ""));
            }

            public void save(SharedPreferences prefs) {
                prefs.edit()
                        .putString("key_action_top", top.toStringForPreference())
                        .putString("key_action_left", left.toStringForPreference())
                        .putString("key_action_bottom", bottom.toStringForPreference())
                        .putString("key_action_right", right.toStringForPreference())
                        .apply();
            }
        }
    }
}
