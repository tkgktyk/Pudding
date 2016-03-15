package jp.tkgktyk.xposed.pudding.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.common.collect.Lists;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.tkgktyk.xposed.pudding.Pudding;
import jp.tkgktyk.xposed.pudding.R;
import jp.tkgktyk.xposed.pudding.app.util.ActionInfo;

/**
 * Created by tkgktyk on 2015/06/07.
 */
public class ActionPickerActivity extends AppCompatActivity {
    private static final String EXTRA_TITLE = Pudding.PREFIX_EXTRA + "TITLE";
    private static final String EXTRA_TOOL_SET = Pudding.PREFIX_EXTRA + "TOOL_SET";
    public static final String EXTRA_ACTION_RECORD = Pudding.PREFIX_EXTRA + "ACTION_RECORD";

    public static final int TOOL_SET_DEFAULT = 0;

    public static void putExtras(Intent intent, CharSequence title, int toolSet) {
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_TOOL_SET, toolSet);
    }

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    private int mToolSet = TOOL_SET_DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_picker);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getIntent().getStringExtra(EXTRA_TITLE));

        mToolSet = getIntent().getIntExtra(EXTRA_TOOL_SET, TOOL_SET_DEFAULT);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new ActionPickerFragment())
                    .commit();
        }
    }

    public void returnActivity(ActionInfo actionInfo) {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_ACTION_RECORD, actionInfo.toRecord());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void pickTool() {
        Fragment fragment;
        switch (mToolSet) {
            case TOOL_SET_DEFAULT:
            default:
                fragment = new DefaultToolPickerFragment();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public static class ActionPickerFragment extends ListFragment {
        private static final int REQUEST_PICK_APP = 1;
        private static final int REQUEST_PICK_SHORTCUT = 2;
        private static final int REQUEST_CREATE_SHORTCUT = 3;

        private static final int[] TITLE_ID_LIST = {
                R.string.tool,
                R.string.application,
                R.string.shortcut,
                R.string.none
        };

        public ActionPickerFragment() {
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ArrayList<String> titles = Lists.newArrayList();
            for (int id : TITLE_ID_LIST) {
                titles.add(getString(id));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(),
                    android.R.layout.simple_list_item_1, titles);
            setListAdapter(adapter);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            ActionPickerActivity activity = (ActionPickerActivity) getActivity();
            switch (TITLE_ID_LIST[position]) {
                case R.string.tool: {
                    activity.pickTool();
                    break;
                }
                case R.string.application: {
                    Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_LAUNCHER));
                    intent.putExtra(Intent.EXTRA_TITLE, activity.getSupportActionBar().getTitle());

                    startActivityForResult(intent, REQUEST_PICK_APP);
                    break;
                }
                case R.string.shortcut: {
                    Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    intent.putExtra(Intent.EXTRA_INTENT, new Intent(
                            Intent.ACTION_CREATE_SHORTCUT));
                    intent.putExtra(Intent.EXTRA_TITLE, activity.getSupportActionBar().getTitle());

                    startActivityForResult(intent, REQUEST_PICK_SHORTCUT);
                    break;
                }
                case R.string.none: {
                    activity.returnActivity(new ActionInfo());
                    break;
                }
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            ActionPickerActivity activity = (ActionPickerActivity) getActivity();
            switch (requestCode) {
                case REQUEST_PICK_SHORTCUT:
                    if (RESULT_OK == resultCode) {
                        // expand shortcut to create if need
                        startActivityForResult(data, REQUEST_CREATE_SHORTCUT);
                    }
                    break;
                case REQUEST_CREATE_SHORTCUT:
                    if (RESULT_OK == resultCode) {
                        ActionInfo actionInfo = new ActionInfo(getActivity(), data,
                                ActionInfo.TYPE_SHORTCUT);
                        activity.returnActivity(actionInfo);
                    }
                    break;
                case REQUEST_PICK_APP:
                    if (RESULT_OK == resultCode) {
                        ActionInfo actionInfo = new ActionInfo(getActivity(), data,
                                ActionInfo.TYPE_APP);
                        activity.returnActivity(actionInfo);
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public static abstract class ToolPickerFragment extends ListFragment {
        protected abstract String[] getActionList();

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Context context = view.getContext();

            ArrayList<String> titles = Lists.newArrayList();
            for (String action : getActionList()) {
                titles.add(Pudding.getActionName(context, action));
            }
//            ArrayAdapter adapter = new ArrayAdapter<>(context,
//                    android.R.layout.simple_list_item_1, titles);
            MyAdapter adapter = new MyAdapter(context, titles);
            setListAdapter(adapter);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            ActionPickerActivity activity = (ActionPickerActivity) getActivity();
            ActionInfo actionInfo = new ActionInfo(activity, new Intent(getActionList()[position]),
                    ActionInfo.TYPE_TOOL);
            activity.returnActivity(actionInfo);
        }

        private class MyAdapter extends ArrayAdapter<String> {
            MyAdapter(Context context, ArrayList<String> titles) {
                super(context, R.layout.view_tool_list_item, R.id.action_name, titles);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View root = super.getView(position, convertView, parent);
                ImageView icon = (ImageView) root.findViewById(R.id.icon);
                int iconId = Pudding.getActionIconResource(getActionList()[position]);
                if (iconId != 0) {
                    icon.setImageResource(iconId);
                } else {
                    icon.setImageDrawable(null);
                }
                return root;
            }
        }
    }

    public static class DefaultToolPickerFragment extends ToolPickerFragment {
        private static final String[] ACTION_LIST = {
                // key
                Pudding.ACTION_BACK,
                Pudding.ACTION_HOME,
                Pudding.ACTION_RECENTS,
                Pudding.ACTION_LEFT,
                Pudding.ACTION_RIGHT,
                Pudding.ACTION_REFRESH,
                Pudding.ACTION_MOVE_HOME,
                Pudding.ACTION_MOVE_END,
                Pudding.ACTION_LOCK_SCREEN,
                // status bar
                Pudding.ACTION_NOTIFICATIONS,
                Pudding.ACTION_QUICK_SETTINGS,
                // other
                Pudding.ACTION_KILL,
                Pudding.ACTION_POWER_MENU,
        };

        @Override
        public String[] getActionList() {
            return ACTION_LIST;
        }
    }
}
