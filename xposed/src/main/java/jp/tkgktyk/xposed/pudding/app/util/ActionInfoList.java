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

package jp.tkgktyk.xposed.pudding.app.util;

import android.support.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by tkgktyk on 2015/07/05.
 */
public class ActionInfoList extends ArrayList<ActionInfo> {
    public String toStringForPreference() {
        ArrayList<ActionInfo.Record> records = Lists.newArrayListWithCapacity(size());
        for (ActionInfo info : this) {
            records.add(info.toRecord());
        }

        return new Gson().toJson(records);
    }

    @NonNull
    public static ActionInfoList fromPreference(String stringFromPreference) {
        // cannot use List<ActionInfo> directly when Gson parses List<ActionInfo>.
        ActionInfo.Record[] records = new Gson().fromJson(stringFromPreference, ActionInfo.Record[].class);
        if (records == null) {
            return new ActionInfoList();
        }

        ActionInfoList actions = new ActionInfoList(records.length);
        for (ActionInfo.Record record : records) {
            actions.add(new ActionInfo(record));
        }
        return actions;
    }

    public ActionInfoList() {
        super();
    }

    private ActionInfoList(int capacity) {
        super(capacity);
    }
}
