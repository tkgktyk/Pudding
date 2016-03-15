package jp.tkgktyk.xposed.pudding.app.util;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

/**
 * Created by tkgktyk on 2/11/16.
 */
public class SimpleToast {
    private Toast mToast;

    public void show(Context context, @StringRes int textId) {
        show(context, context.getString(textId));
    }

    public void show(Context context, String text) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        mToast.show();
    }
}
