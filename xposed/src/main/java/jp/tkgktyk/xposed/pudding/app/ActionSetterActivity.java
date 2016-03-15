/*
 * Copyright 2015 Takagi Katsuyuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.tkgktyk.xposed.pudding.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.tkgktyk.xposed.pudding.Pudding;
import jp.tkgktyk.xposed.pudding.R;
import jp.tkgktyk.xposed.pudding.app.util.ActionInfo;
import jp.tkgktyk.xposed.pudding.app.util.SimpleToast;

/**
 * Created by tkgktyk on 2015/07/02.
 */
public class ActionSetterActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_NAME = Pudding.PREFIX_EXTRA + "PACKAGE_NAME";
    public static final String EXTRA_TITLE= Pudding.PREFIX_EXTRA + "TITLE";

    private static final int REQUEST_ACTION = 1;

    private static final int TOP = 0;
    private static final int LEFT = 1;
    private static final int BOTTOM = 2;
    private static final int RIGHT = 3;
    private static final int COUNT = 4;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private String mPackageName;

    private MyAdapter mMyAdapter;
    private boolean mIsChanged;

    private Pudding.Settings.Actions mActions;
    private int mDirection;

    private SimpleToast mToast = new SimpleToast();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_setter);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPackageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        setTitle(getIntent().getStringExtra(EXTRA_TITLE));

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMyAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mMyAdapter);

        mActions = new Pudding.Settings.Actions(Pudding.getSharedPreferences(this, mPackageName));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        saveActions();
    }

    private boolean saveActions() {
        if (mIsChanged) {
            mActions.save(Pudding.getSharedPreferences(this, mPackageName));
            mIsChanged = false;
            mToast.show(this, R.string.saved);
            return true;
        }
        return false;
    }

    private void showActionPicker(int direction) {
        mDirection = direction;
        Intent intent = new Intent(this, ActionPickerActivity.class);
        ActionPickerActivity.putExtras(intent, getSupportActionBar().getTitle(),
                ActionPickerActivity.TOOL_SET_DEFAULT);
        startActivityForResult(intent, REQUEST_ACTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ACTION:
                if (resultCode == RESULT_OK) {
                    ActionInfo.Record record = (ActionInfo.Record) data
                            .getSerializableExtra(ActionPickerActivity.EXTRA_ACTION_RECORD);
                    switch (mDirection) {
                        case TOP:
                            mActions.top = record;
                            break;
                        case LEFT:
                            mActions.left = record;
                            break;
                        case BOTTOM:
                            mActions.bottom = record;
                            break;
                        case RIGHT:
                            mActions.right = record;
                            break;
                    }
                    mIsChanged = true;
                    mMyAdapter.notifyItemChanged(mDirection);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected static class Holder extends RecyclerView.ViewHolder {
        @Bind(R.id.direction)
        TextView direction;
        @Bind(R.id.icon)
        ImageView icon;
        @Bind(R.id.action_name)
        TextView name;
        @Bind(R.id.action_type)
        TextView type;

        public Holder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<Holder> {

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_action_list_item, parent, false);
            final Holder holder = new Holder(v);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showActionPicker(holder.getAdapterPosition());
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            ActionInfo info = null;
            switch (position) {
                case TOP:
                    info = new ActionInfo(mActions.top);
                    holder.direction.setText(R.string.direction_top);
                    break;
                case LEFT:
                    info = new ActionInfo(mActions.left);
                    holder.direction.setText(R.string.direction_left);
                    break;
                case BOTTOM:
                    info = new ActionInfo(mActions.bottom);
                    holder.direction.setText(R.string.direction_bottom);
                    break;
                case RIGHT:
                    info = new ActionInfo(mActions.right);
                    holder.direction.setText(R.string.direction_right);
                    break;
            }
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
            holder.name.setText(name);
            switch (info.getType()) {
                case ActionInfo.TYPE_TOOL:
                    holder.type.setText(R.string.tool);
                    break;
                case ActionInfo.TYPE_APP:
                    holder.type.setText(R.string.application);
                    break;
                case ActionInfo.TYPE_SHORTCUT:
                    holder.type.setText(R.string.shortcut);
                    break;
                default:
                    holder.type.setText(null);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return COUNT;
        }
    }
}
