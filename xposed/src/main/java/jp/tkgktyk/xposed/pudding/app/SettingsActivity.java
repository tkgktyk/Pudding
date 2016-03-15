package jp.tkgktyk.xposed.pudding.app;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.tkgktyk.xposed.pudding.Pudding;
import jp.tkgktyk.xposed.pudding.R;
import jp.tkgktyk.xposed.pudding.app.util.ActionInfo;
import jp.tkgktyk.xposed.pudding.app.util.SimpleToast;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || ActionPreferenceFragment.class.getName().equals(fragmentName);
    }

    public static class BasePreferenceFragment extends PreferenceFragment {
        private SimpleToast mToast = new SimpleToast();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        public Preference findPreference(@StringRes int keyId) {
            return findPreference(getString(keyId));
        }

        protected void showToast(@StringRes int textId) {
            mToast.show(getActivity(), textId);
        }

        protected void showToast(String text) {
            mToast.show(getActivity(), text);
        }

        protected void openActivity(@StringRes int id, final Class<?> cls) {
            openActivity(id, cls, null);
        }

        protected void openActivity(@StringRes int id, final Class<?> cls, final ExtraPutter putter) {
            Preference pref = findPreference(id);
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent activity = new Intent(preference.getContext(), cls);
                    if (putter != null) {
                        putter.putExtras(preference, activity);
                    }
                    startActivity(activity);
                    return true;
                }
            });
        }

        protected void openActivityForResult(@StringRes int id, final Class<?> cls,
                                             int requestCode) {
            openActivityForResult(id, cls, requestCode, null);
        }

        protected void openActivityForResult(@StringRes int id, final Class<?> cls,
                                             final int requestCode, final ExtraPutter putter) {
            Preference pref = findPreference(id);
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent activity = new Intent(preference.getContext(), cls);
                    if (putter != null) {
                        putter.putExtras(preference, activity);
                    }
                    startActivityForResult(activity, requestCode);
                    return true;
                }
            });
        }

        protected interface ExtraPutter {
            void putExtras(Preference preference, Intent activityIntent);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends BasePreferenceFragment {
        private static final int REQUEST_BLACKLIST = 1;

        private String mPrefKey;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            openActivityForResult(R.string.key_blacklist, AppSelectActivity.class,
                    REQUEST_BLACKLIST, new ExtraPutter() {
                        @Override
                        public void putExtras(Preference preference, Intent activityIntent) {
                            mPrefKey = preference.getKey();
                            Set<String> blacklist = preference.getSharedPreferences()
                                    .getStringSet(mPrefKey, Collections.<String>emptySet());
                            AppSelectActivity.putExtras(activityIntent,
                                    preference.getTitle(), blacklist);
                        }
                    });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_BLACKLIST:
                    if (resultCode == RESULT_OK) {
                        Set<String> blacklist = (Set<String>) data.getSerializableExtra(
                                AppSelectActivity.EXTRA_SELECTED_HASH_SET);
                        getPreferenceManager().getSharedPreferences()
                                .edit()
                                .putStringSet(mPrefKey, blacklist)
                                .apply();
                        showToast(R.string.saved);
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ActionPreferenceFragment extends Fragment {
        private static final int REQUEST_PICK_APP = 1;

        private MyAdapter mMyAdapter;
        private ArrayList<TargetApp> mTargetList = Lists.newArrayList();

        @Bind(R.id.recycler_view)
        RecyclerView mRecyclerView;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_target_list, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ButterKnife.bind(this, view);

            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mMyAdapter = new MyAdapter();
            mRecyclerView.setAdapter(mMyAdapter);
            ItemTouchHelper helper = new ItemTouchHelper(
                    new ItemTouchHelper.SimpleCallback(0,
                            ItemTouchHelper.START | ItemTouchHelper.END) {
                        @Override
                        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                            if (viewHolder.getAdapterPosition() == 0) {
                                return 0;
                            }
                            return super.getMovementFlags(recyclerView, viewHolder);
                        }

                        @Override
                        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                            return false;
                        }

                        @Override
                        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
                            final int pos = viewHolder.getAdapterPosition();
                            mTargetList.get(pos).delete(getActivity());
                            mTargetList.remove(pos);
                            mMyAdapter.notifyItemRemoved(pos);
                        }
                    });
            helper.attachToRecyclerView(mRecyclerView);

            loadTargetList();
        }

        void loadTargetList() {
            Context context = getActivity();
            mTargetList.clear();
            mTargetList.add(new TargetApp(context));

            File prefsdir = new File(getActivity().getApplicationInfo().dataDir, "shared_prefs");

            if (prefsdir.exists() && prefsdir.isDirectory()) {
                String[] xmls = prefsdir.list();
                for (String xml : xmls) {
                    if (xml.startsWith(Pudding.PACKAGE_NAME)) {
                        // ignore
                    } else {
                        String packageName = xml.substring(0, xml.lastIndexOf("."));
                        mTargetList.add(new TargetApp(context, packageName));
                    }
                }
            }

            mMyAdapter.notifyDataSetChanged();
        }

        @OnClick(R.id.add_fab)
        void onAddClicked(FloatingActionButton button) {
            Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER));
            intent.putExtra(Intent.EXTRA_TITLE, getActivity().getTitle());

            startActivityForResult(intent, REQUEST_PICK_APP);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_PICK_APP:
                    if (RESULT_OK == resultCode) {
                        ActionInfo actionInfo = new ActionInfo(getActivity(), data,
                                ActionInfo.TYPE_APP);
                        mTargetList.add(new TargetApp(actionInfo));
                        mMyAdapter.notifyItemInserted(mTargetList.size() - 1);
                        showActionSetter(actionInfo);
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }

        private void showActionSetter(ActionInfo actionInfo) {
            Intent intent = new Intent(getActivity(), ActionSetterActivity.class);
            String packageName = actionInfo.getPackageName();
            intent.putExtra(ActionSetterActivity.EXTRA_PACKAGE_NAME,
                    Objects.equal(Pudding.PACKAGE_NAME, packageName) ?
                            packageName + "_preferences" : packageName);
            intent.putExtra(ActionSetterActivity.EXTRA_TITLE, actionInfo.getName());
            startActivity(intent);
        }

        private class TargetApp {
            final String prefName;
            final ActionInfo actionInfo;

            TargetApp(Context context) {
                PackageManager pm = context.getPackageManager();
                prefName = Pudding.PACKAGE_NAME + "_preferences.xml";
                actionInfo = (new ActionInfo(context,
                        pm.getLaunchIntentForPackage(Pudding.PACKAGE_NAME),
                        ActionInfo.TYPE_APP));
            }

            TargetApp(Context context, String packageName) {
                PackageManager pm = context.getPackageManager();
                prefName = packageName + ".xml";
                actionInfo = (new ActionInfo(context,
                        pm.getLaunchIntentForPackage(packageName), ActionInfo.TYPE_APP));
            }

            TargetApp(ActionInfo actionInfo) {
                prefName = actionInfo.getPackageName();
                this.actionInfo = actionInfo;
            }

            void delete(Context context) {
                File prefsdir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
                File pref = new File(prefsdir, prefName);
                pref.delete();
            }
        }

        protected static class Holder extends RecyclerView.ViewHolder {
            @Bind(R.id.icon)
            ImageView icon;
            @Bind(R.id.title)
            TextView title;
            @Bind(R.id.description)
            TextView description;

            public Holder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }

        private class MyAdapter extends RecyclerView.Adapter<Holder> {

            @Override
            public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.view_target_list_item, parent, false);
                final Holder holder = new Holder(v);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = holder.getAdapterPosition();
                        showActionSetter(mTargetList.get(position).actionInfo);
                    }
                });
                return holder;
            }

            @Override
            public void onBindViewHolder(Holder holder, int position) {
                ActionInfo info = mTargetList.get(position).actionInfo;
                if (Objects.equal(info.getPackageName(), Pudding.PACKAGE_NAME)) {
                    holder.icon.setImageDrawable(null);
                    holder.title.setText(R.string.default_action);
                    holder.description.setText(null);
                } else {
                    String name = info.getName();
                    Bitmap icon = info.getIcon();
                    if (Strings.isNullOrEmpty(name)) {
                        switch (info.getType()) {
                            case ActionInfo.TYPE_NONE:
                                name = getString(R.string.none);
                                break;
                            default:
                                name = getString(R.string.not_found);
                        }
                    }
                    holder.icon.setImageBitmap(icon);
                    holder.title.setText(name);
                    holder.description.setText(info.getPackageName());
                }
            }

            @Override
            public int getItemCount() {
                return mTargetList.size();
            }
        }
    }
}
