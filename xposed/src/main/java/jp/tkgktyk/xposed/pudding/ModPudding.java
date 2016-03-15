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

package jp.tkgktyk.xposed.pudding;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.lib.pudding.PuddingLayout;
import jp.tkgktyk.xposed.pudding.app.util.ActionInfo;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class ModPudding extends XposedModule {
    private static final String FIELD_SETTINGS = Pudding.NAME + "_settings";
    private static final String FIELD_PUDDING_LAYOUT = Pudding.NAME + "_puddingLayout";

    private static XSharedPreferences mPrefs;

    public static void initZygote(XSharedPreferences prefs) {
        mPrefs = prefs;
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                Activity activity = (Activity) param.thisObject;
                                mPrefs.reload();
                                SharedPreferences forActions;
                                String packageName = activity.getPackageName();
                                Context modContext = Pudding.getModContext(activity);
                                File xml = new File(new File(modContext.getApplicationInfo().dataDir,
                                        "shared_prefs"), packageName);
                                forActions = xml.exists() ?
                                        Pudding.getSharedPreferences(activity, packageName) :
                                        mPrefs;
                                Pudding.Settings settings = new Pudding.Settings(mPrefs, forActions);
                                if (!settings.blacklist.contains(activity.getPackageName())
                                        && !activity.isChild()) {
                                    XposedHelpers.setAdditionalInstanceField(activity,
                                            FIELD_SETTINGS, settings);
//                                    if (settings != null) {
//                                        View content = activity.findViewById(android.R.id.content);
//                                        ViewParent parent = content.getParent();
//                                        if (parent != null && !(parent instanceof PuddingLayout)
//                                                && parent instanceof ViewGroup) {
//                                            install(activity, content, (ViewGroup) parent, settings);
//                                        }
////                                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
////                                    install(activity, decorView, settings);
//                                    }
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                        }
//
//                        void install(Activity activity, View content, ViewGroup parent,
//                                     Pudding.Settings settings) {
//                            PuddingLayout puddingLayout = generatePudding(activity, settings);
//
//                            for (int i = 0; i < parent.getChildCount(); ++i) {
//                                View child = parent.getChildAt(i);
//                                if (child == content) {
//                                    parent.removeViewAt(i);
//                                    puddingLayout.addView(content);
//                                    parent.addView(puddingLayout, i);
//                                }
//                            }
////                            parent.removeView(content);
////                            puddingLayout.addView(content);
////                            parent.addView(puddingLayout);
////                            List<View> contents = Lists.newArrayList();
////                            int count = target.getChildCount();
////                            for (int i = 0; i < count; ++i) {
////                                contents.add(target.getChildAt(i));
////                            }
////                            target.removeAllViews();
////                            for (View v : contents) {
////                                puddingLayout.addView(v, v.getLayoutParams());
////                            }
////                            target.addView(puddingLayout);
//                        }
//
//                        PuddingLayout generatePudding(final Activity activity,
//                                                      Pudding.Settings settings) {
//                            PuddingLayout puddingLayout = new PuddingLayout(activity);
//                            puddingLayout.setLayoutParams(
//                                    new ViewGroup.LayoutParams(
//                                            ViewGroup.LayoutParams.MATCH_PARENT,
//                                            ViewGroup.LayoutParams.MATCH_PARENT));
//
//                            ActionInfo top = new ActionInfo(settings.actions.top);
//                            puddingLayout.setTopDrawable(top.newIconDrawable(activity));
//                            ActionInfo bottom = new ActionInfo(settings.actions.bottom);
//                            puddingLayout.setBottomDrawable(bottom.newIconDrawable(activity));
//                            ActionInfo left = new ActionInfo(settings.actions.left);
//                            puddingLayout.setLeftDrawable(left.newIconDrawable(activity));
//                            ActionInfo right = new ActionInfo(settings.actions.right);
//                            puddingLayout.setRightDrawable(right.newIconDrawable(activity));
//
//                            puddingLayout.setOnOverscrollListener(new PuddingLayout.OnOverscrollListener() {
//                                @Override
//                                public void onOverscrollTop() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.top).launch(activity);
//                                }
//
//                                @Override
//                                public void onOverscrollBottom() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.bottom).launch(activity);
//                                }
//
//                                @Override
//                                public void onOverscrollLeft() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.left).launch(activity);
//                                }
//
//                                @Override
//                                public void onOverscrollRight() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.right).launch(activity);
//                                }
//                            });
//
//                            return puddingLayout;
//                        }
                    });
            XposedHelpers.findAndHookMethod(Activity.class, "onDestroy",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                Activity activity = (Activity) param.thisObject;
                                Pudding.Settings settings = getSettings(activity);
                                if (settings != null) {
                                    XposedHelpers.removeAdditionalInstanceField(activity, FIELD_SETTINGS);
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                        }
                    });
//            XposedHelpers.findAndHookMethod(Activity.class, "onResume",
//                    new XC_MethodHook() {
//                        @Override
//                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                            try {
//                                Activity activity = (Activity) param.thisObject;
//                                Pudding.Settings settings = getSettings(activity);
//                                if (settings != null) {
//                                    View content = activity.findViewById(android.R.id.content);
//                                    ViewParent parent = content.getParent();
//                                    if (parent != null && !(parent instanceof PuddingLayout)
//                                            && parent instanceof ViewGroup) {
//                                        install(activity, content, (ViewGroup) parent, settings);
//                                    }
////                                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
////                                    install(activity, decorView, settings);
//                                }
//                            } catch (Throwable t) {
//                                logE(t);
//                            }
//                        }
//
//                        void install(Activity activity, View content, ViewGroup parent,
//                                     Pudding.Settings settings) {
//                            PuddingLayout puddingLayout = generatePudding(activity, settings);
//
//                            for (int i = 0; i < parent.getChildCount(); ++i) {
//                                View child = parent.getChildAt(i);
//                                if (child == content) {
//                                    parent.removeViewAt(i);
//                                    puddingLayout.addView(content);
//                                    parent.addView(puddingLayout, i);
//                                }
//                            }
////                            parent.removeView(content);
////                            puddingLayout.addView(content);
////                            parent.addView(puddingLayout);
////                            List<View> contents = Lists.newArrayList();
////                            int count = target.getChildCount();
////                            for (int i = 0; i < count; ++i) {
////                                contents.add(target.getChildAt(i));
////                            }
////                            target.removeAllViews();
////                            for (View v : contents) {
////                                puddingLayout.addView(v, v.getLayoutParams());
////                            }
////                            target.addView(puddingLayout);
//                        }
//
//                        PuddingLayout generatePudding(final Activity activity,
//                                                      Pudding.Settings settings) {
//                            PuddingLayout puddingLayout = new PuddingLayout(activity);
//                            puddingLayout.setLayoutParams(
//                                    new ViewGroup.LayoutParams(
//                                            ViewGroup.LayoutParams.MATCH_PARENT,
//                                            ViewGroup.LayoutParams.MATCH_PARENT));
//
//                            ActionInfo top = new ActionInfo(settings.actions.top);
//                            puddingLayout.setTopDrawable(top.newIconDrawable(activity));
//                            ActionInfo bottom = new ActionInfo(settings.actions.bottom);
//                            puddingLayout.setBottomDrawable(bottom.newIconDrawable(activity));
//                            ActionInfo left = new ActionInfo(settings.actions.left);
//                            puddingLayout.setLeftDrawable(left.newIconDrawable(activity));
//                            ActionInfo right = new ActionInfo(settings.actions.right);
//                            puddingLayout.setRightDrawable(right.newIconDrawable(activity));
//
//                            puddingLayout.setOnOverscrollListener(new PuddingLayout.OnOverscrollListener() {
//                                @Override
//                                public void onOverscrollTop() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.top).launch(activity);
//                                }
//
//                                @Override
//                                public void onOverscrollBottom() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.bottom).launch(activity);
//                                }
//
//                                @Override
//                                public void onOverscrollLeft() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.left).launch(activity);
//                                }
//
//                                @Override
//                                public void onOverscrollRight() {
//                                    Pudding.Settings settings = getSettings(activity);
//                                    new ActionInfo(settings.actions.right).launch(activity);
//                                }
//                            });
//
//                            return puddingLayout;
//                        }
//                    });
            XposedHelpers.findAndHookMethod(Activity.class, "setContentView", int.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                Activity activity = (Activity) methodHookParam.thisObject;
                                Pudding.Settings settings = getSettings(activity);
                                if (settings == null) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    int layoutRes = (Integer) methodHookParam.args[0];
                                    View content = activity.getLayoutInflater().inflate(layoutRes, null);
                                    View pudding = install(activity, content, settings, null);
                                    activity.setContentView(pudding, pudding.getLayoutParams());
                                }
                            } catch (Throwable t) {
                                logE(t);
                                invokeOriginalMethod(methodHookParam);
                            }
                            return null;
                        }
                    });
            XposedHelpers.findAndHookMethod(Activity.class, "setContentView", View.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                Activity activity = (Activity) methodHookParam.thisObject;
                                Pudding.Settings settings = getSettings(activity);
                                if (settings == null) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    View content = (View) methodHookParam.args[0];
                                    View pudding = install(activity, content, settings, null);
                                    activity.setContentView(pudding, pudding.getLayoutParams());
                                }
                            } catch (Throwable t) {
                                logE(t);
                                invokeOriginalMethod(methodHookParam);
                            }
                            return null;
                        }
                    });
            XposedHelpers.findAndHookMethod(Activity.class, "setContentView", View.class,
                    ViewGroup.LayoutParams.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                Activity activity = (Activity) methodHookParam.thisObject;
                                View content = (View) methodHookParam.args[0];
                                if (content instanceof PuddingLayout) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    Pudding.Settings settings = getSettings(activity);
                                    if (settings == null) {
                                        invokeOriginalMethod(methodHookParam);
                                    } else {
                                        View pudding = install(activity, content, settings, null);
                                        ViewGroup.LayoutParams lp
                                                = (ViewGroup.LayoutParams) methodHookParam.args[1];
                                        activity.setContentView(pudding, lp);
                                    }
                                }
                            } catch (Throwable t) {
                                logE(t);
                                invokeOriginalMethod(methodHookParam);
                            }
                            return null;
                        }
                    });
            XposedHelpers.findAndHookMethod(Activity.class, "addContentView", View.class,
                    ViewGroup.LayoutParams.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                Activity activity = (Activity) methodHookParam.thisObject;
                                Pudding.Settings settings = getSettings(activity);
                                if (settings == null) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    View content = (View) methodHookParam.args[0];
                                    ViewGroup.LayoutParams lp
                                            = (ViewGroup.LayoutParams) methodHookParam.args[1];
                                    ViewGroup pudding = getPuddingLayout(activity);
                                    if (pudding == null) {
                                        pudding = install(activity, content, settings, lp);
                                    } else {
                                        pudding.addView(content, lp);
                                    }
                                }
                            } catch (Throwable t) {
                                logE(t);
                                invokeOriginalMethod(methodHookParam);
                            }
                            return null;
                        }
                    });
        } catch (Throwable t) {
            logE(t);
        }
    }

    private static Pudding.Settings getSettings(Object object) {
        return (Pudding.Settings) XposedHelpers.getAdditionalInstanceField(object, FIELD_SETTINGS);
    }

    private static PuddingLayout getPuddingLayout(Object object) {
        return (PuddingLayout) XposedHelpers.getAdditionalInstanceField(object, FIELD_PUDDING_LAYOUT);
    }

    private static PuddingLayout install(Activity activity, View content,
                                         Pudding.Settings settings, ViewGroup.LayoutParams lp) {
        PuddingLayout puddingLayout = generatePudding(activity, settings);
        if (lp != null) {
            puddingLayout.addView(content, lp);
        } else {
            puddingLayout.addView(content);
        }
        XposedHelpers.setAdditionalInstanceField(activity,
                FIELD_PUDDING_LAYOUT, puddingLayout);
        return puddingLayout;
    }

    private static PuddingLayout generatePudding(final Activity activity,
                                                 Pudding.Settings settings) {
        PuddingLayout puddingLayout = new PuddingLayout(activity);
        puddingLayout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        ActionInfo top = new ActionInfo(settings.actions.top);
        puddingLayout.setTopDrawable(top.newIconDrawable(activity));
        ActionInfo bottom = new ActionInfo(settings.actions.bottom);
        puddingLayout.setBottomDrawable(bottom.newIconDrawable(activity));
        ActionInfo left = new ActionInfo(settings.actions.left);
        puddingLayout.setLeftDrawable(left.newIconDrawable(activity));
        ActionInfo right = new ActionInfo(settings.actions.right);
        puddingLayout.setRightDrawable(right.newIconDrawable(activity));

        puddingLayout.setOnOverscrollListener(new PuddingLayout.OnOverscrollListener() {
            @Override
            public void onOverscrollTop() {
                Pudding.Settings settings = getSettings(activity);
                new ActionInfo(settings.actions.top).launch(activity);
            }

            @Override
            public void onOverscrollBottom() {
                Pudding.Settings settings = getSettings(activity);
                new ActionInfo(settings.actions.bottom).launch(activity);
            }

            @Override
            public void onOverscrollLeft() {
                Pudding.Settings settings = getSettings(activity);
                new ActionInfo(settings.actions.left).launch(activity);
            }

            @Override
            public void onOverscrollRight() {
                Pudding.Settings settings = getSettings(activity);
                new ActionInfo(settings.actions.right).launch(activity);
            }
        });

        return puddingLayout;
    }
}
