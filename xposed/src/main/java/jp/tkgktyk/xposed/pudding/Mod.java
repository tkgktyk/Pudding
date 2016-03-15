package jp.tkgktyk.xposed.pudding;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by tkgktyk on 2/6/16.
 */
public class Mod implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private XSharedPreferences mPrefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mPrefs = new XSharedPreferences(Pudding.PACKAGE_NAME);
        mPrefs.makeWorldReadable();

        Pudding.Settings settings = new Pudding.Settings(mPrefs);
        ModInternal.initZygote(mPrefs);
        ModPudding.initZygote(mPrefs);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("android") &&
                loadPackageParam.processName.equals("android")) {
            ModInternal.handleLoadPackage(loadPackageParam.classLoader);
        }
    }
}
