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
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Objects;

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
    private static final String CLASS_PHONE_WINDOW = "com.android.internal.policy.impl.PhoneWindow";
    private static final String CLASS_PHONE_WINDOW_M = "com.android.internal.policy.PhoneWindow";

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
                                        "shared_prefs"), packageName + ".xml");
                                forActions = xml.exists() ?
                                        Pudding.getSharedPreferences(activity, packageName) :
                                        mPrefs;
                                Pudding.Settings settings = new Pudding.Settings(mPrefs, forActions);
                                final boolean contain = settings.blacklist.contains(activity.getPackageName());
                                final boolean white = settings.whitelistMode ? contain : !contain;
                                if (white && !activity.isChild()
                                        && !isIgnoredByWorkaround1(settings, activity)) {
                                    XposedHelpers.setAdditionalInstanceField(activity,
                                            FIELD_SETTINGS, settings);
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                        }

                        boolean isIgnoredByWorkaround1(Pudding.Settings settings, Activity activity) {
                            return settings.workaround1
                                    && Objects.equal(activity.getClass().getName(), "com.android.systemui.recents.RecentsActivity");
                        }
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
                                ViewGroup pudding = getPuddingLayout(activity);
                                if (pudding != null) {
                                    XposedHelpers.removeAdditionalInstanceField(activity, FIELD_PUDDING_LAYOUT);
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                        }
                    });
            final Class<?> classPhoneWindow = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ?
                    XposedHelpers.findClass(CLASS_PHONE_WINDOW_M, null) :
                    XposedHelpers.findClass(CLASS_PHONE_WINDOW, null);
            XposedHelpers.findAndHookMethod(classPhoneWindow, "setContentView", int.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
//                                logD("setContentView");
                                Context context = (Context) XposedHelpers.callMethod(methodHookParam.thisObject, "getContext");
                                Pudding.Settings settings = getSettings(context);
                                if (settings == null) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    int layoutRes = (Integer) methodHookParam.args[0];
                                    View content = LayoutInflater.from(context).inflate(layoutRes, null);
                                    View pudding = install(context, content, settings, null);
                                    XposedHelpers.callMethod(methodHookParam.thisObject, "setContentView", pudding, pudding.getLayoutParams());
                                }
                            } catch (Throwable t) {
                                logE(t);
                                invokeOriginalMethod(methodHookParam);
                            }
                            return null;
                        }
                    });
            XposedHelpers.findAndHookMethod(classPhoneWindow, "setContentView", View.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
//                                logD("setContentView");
                                Context context = (Context) XposedHelpers.callMethod(methodHookParam.thisObject, "getContext");
                                Pudding.Settings settings = getSettings(context);
                                if (settings == null) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    View content = (View) methodHookParam.args[0];
                                    View pudding = install(context, content, settings, null);
                                    XposedHelpers.callMethod(methodHookParam.thisObject, "setContentView", pudding, pudding.getLayoutParams());
                                }
                            } catch (Throwable t) {
                                logE(t);
                                invokeOriginalMethod(methodHookParam);
                            }
                            return null;
                        }
                    });
            XposedHelpers.findAndHookMethod(classPhoneWindow, "setContentView", View.class,
                    ViewGroup.LayoutParams.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
//                                logD("setContentView");
                                Context context = (Context) XposedHelpers.callMethod(methodHookParam.thisObject, "getContext");
                                View content = (View) methodHookParam.args[0];
                                if (content instanceof PuddingLayout) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    Pudding.Settings settings = getSettings(context);
                                    if (settings == null) {
                                        invokeOriginalMethod(methodHookParam);
                                    } else {
                                        ViewGroup.LayoutParams lp
                                                = (ViewGroup.LayoutParams) methodHookParam.args[1];
                                        View pudding = install(context, content, settings, lp);
                                        XposedHelpers.callMethod(methodHookParam.thisObject, "setContentView", pudding, pudding.getLayoutParams());
                                    }
                                }
                            } catch (Throwable t) {
                                logE(t);
                                invokeOriginalMethod(methodHookParam);
                            }
                            return null;
                        }
                    });
            XposedHelpers.findAndHookMethod(classPhoneWindow, "addContentView", View.class,
                    ViewGroup.LayoutParams.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
//                                logD("addContentView");
                                Context context = (Context) XposedHelpers.callMethod(methodHookParam.thisObject, "getContext");
                                Pudding.Settings settings = getSettings(context);
                                if (settings == null) {
                                    invokeOriginalMethod(methodHookParam);
                                } else {
                                    View content = (View) methodHookParam.args[0];
                                    ViewGroup.LayoutParams lp
                                            = (ViewGroup.LayoutParams) methodHookParam.args[1];
                                    ViewGroup pudding = getPuddingLayout(context);
                                    if (pudding == null) {
                                        pudding = install(context, content, settings, lp);
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

    private static PuddingLayout install(Context context, View content,
                                         Pudding.Settings settings, ViewGroup.LayoutParams lp) {
        PuddingLayout puddingLayout = generatePudding(context, settings, lp);
        if (lp != null) {
            puddingLayout.addView(content, lp);
        } else {
            puddingLayout.addView(content);
        }
        XposedHelpers.setAdditionalInstanceField(context,
                FIELD_PUDDING_LAYOUT, puddingLayout);
        return puddingLayout;
    }

    private static PuddingLayout generatePudding(final Context context,
                                                 Pudding.Settings settings, ViewGroup.LayoutParams lp) {
        final PuddingLayout puddingLayout = new PuddingLayout(context);
        puddingLayout.setLayoutParams(lp != null ? lp :
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        puddingLayout.useMarginForDrawer(settings.marginForDrawer);
        puddingLayout.setCancelByMultiTouch(settings.singleTouch);

        ActionInfo top = new ActionInfo(settings.actions.top);
        puddingLayout.setTopDrawable(top.newIconDrawable(context));
        ActionInfo bottom = new ActionInfo(settings.actions.bottom);
        puddingLayout.setBottomDrawable(bottom.newIconDrawable(context));
        ActionInfo left = new ActionInfo(settings.actions.left);
        puddingLayout.setLeftDrawable(left.newIconDrawable(context));
        ActionInfo right = new ActionInfo(settings.actions.right);
        puddingLayout.setRightDrawable(right.newIconDrawable(context));

        puddingLayout.setOnOverscrollListener(new PuddingLayout.OnOverscrollListener() {
            @Override
            public void onOverscrollTop() {
                Pudding.Settings settings = getSettings(context);
                if (settings.vibrate) {
                    puddingLayout.performHapticFeedback();
                }
                new ActionInfo(settings.actions.top).launch(context);
            }

            @Override
            public void onOverscrollBottom() {
                Pudding.Settings settings = getSettings(context);
                if (settings.vibrate) {
                    puddingLayout.performHapticFeedback();
                }
                new ActionInfo(settings.actions.bottom).launch(context);
            }

            @Override
            public void onOverscrollLeft() {
                Pudding.Settings settings = getSettings(context);
                if (settings.vibrate) {
                    puddingLayout.performHapticFeedback();
                }
                new ActionInfo(settings.actions.left).launch(context);
            }

            @Override
            public void onOverscrollRight() {
                Pudding.Settings settings = getSettings(context);
                if (settings.vibrate) {
                    puddingLayout.performHapticFeedback();
                }
                new ActionInfo(settings.actions.right).launch(context);
            }
        });

        return puddingLayout;
    }
}
