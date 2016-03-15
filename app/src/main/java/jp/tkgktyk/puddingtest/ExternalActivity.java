package jp.tkgktyk.puddingtest;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.tkgktyk.lib.pudding.PuddingLayout;

public class ExternalActivity extends AppCompatActivity implements PuddingLayout.OnOverscrollListener {

    @Bind(R.id.swipeActionLayout)
    PuddingLayout mPuddingLayout;
    @Bind(R.id.webView)
    WebView mWebView;

    @OnClick(R.id.link)
    void onLinkClicked() {
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external);
        ButterKnife.bind(this);

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("http://forum.xda-developers.com/xposed/modules/mod-force-touch-detector-t3130154");

        mPuddingLayout.setOnOverscrollListener(this);
        mPuddingLayout.setExternalTarget(mWebView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPuddingLayout.setTopDrawable(getDrawable(android.R.drawable.ic_menu_rotate));
            mPuddingLayout.setBottomDrawable(getDrawable(android.R.drawable.ic_menu_gallery));
            mPuddingLayout.setLeftDrawable(getDrawable(android.R.drawable.ic_media_previous));
            mPuddingLayout.setRightDrawable(getDrawable(android.R.drawable.ic_media_next));
        } else {
            mPuddingLayout.setTopDrawable(getResources().getDrawable(android.R.drawable.ic_menu_rotate));
            mPuddingLayout.setBottomDrawable(getResources().getDrawable(android.R.drawable.ic_menu_gallery));
            mPuddingLayout.setLeftDrawable(getResources().getDrawable(android.R.drawable.ic_media_previous));
            mPuddingLayout.setRightDrawable(getResources().getDrawable(android.R.drawable.ic_media_next));
        }
    }

    @Override
    public void onOverscrollTop() {
        Toast.makeText(this, "top", Toast.LENGTH_SHORT).show();
        mPuddingLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPuddingLayout.setRefreshing(false);
            }
        }, 3000);
    }

    @Override
    public void onOverscrollBottom() {
        Toast.makeText(this, "bottom", Toast.LENGTH_SHORT).show();
        mPuddingLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPuddingLayout.setRefreshing(false);
            }
        }, 3000);
    }

    @Override
    public void onOverscrollLeft() {
        Toast.makeText(this, "left", Toast.LENGTH_SHORT).show();
        mPuddingLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPuddingLayout.setRefreshing(false);
            }
        }, 3000);
    }

    @Override
    public void onOverscrollRight() {
        Toast.makeText(this, "right", Toast.LENGTH_SHORT).show();
        mPuddingLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPuddingLayout.setRefreshing(false);
            }
        }, 3000);
    }
}
