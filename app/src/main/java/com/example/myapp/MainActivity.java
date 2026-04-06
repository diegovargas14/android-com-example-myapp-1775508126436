package com.example.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;

public class MainActivity extends Activity {
    private WebView webView;
    private FrameLayout splashView;
    private ImageView splashIcon;
    private AnimatorSet pulseAnimator;
    private boolean hasPageFinished = false;
    private boolean splashDismissed = false;
    private static final String SOURCE_URL = "https://homexe.lovable.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#1a1a2e"));
        }

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        splashView = findViewById(R.id.splashView);
        splashIcon = findViewById(R.id.splashIcon);

        if (splashIcon != null) {
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.05f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.05f);
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(splashIcon, scaleX, scaleY);
            pulse.setDuration(800);
            pulse.setRepeatCount(ObjectAnimator.INFINITE);
            pulse.setRepeatMode(ObjectAnimator.REVERSE);
            pulseAnimator = new AnimatorSet();
            pulseAnimator.play(pulse);
            pulseAnimator.start();
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(isNetworkAvailable() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                hasPageFinished = true;
                hideSplash();
                // Adapt status bar color from page theme-color or body background
                view.evaluateJavascript(
                    "(function(){" +
                    "var m=document.querySelector('meta[name=\"theme-color\"]');" +
                    "if(m&&m.content)return m.content;" +
                    "var bg=getComputedStyle(document.body).backgroundColor;" +
                    "if(bg&&bg!=='rgba(0, 0, 0, 0)'&&bg!=='transparent')return bg;" +
                    "return '#FFFFFF';" +
                    "})()",
                    value -> {
                        if (value == null || value.equals("null")) return;
                        String colorStr = value.replace("\"", "").trim();
                        try {
                            int color;
                            if (colorStr.startsWith("rgb")) {
                                String[] parts = colorStr.replaceAll("[^0-9,]", "").split(",");
                                int r = Integer.parseInt(parts[0].trim());
                                int g = Integer.parseInt(parts[1].trim());
                                int b = Integer.parseInt(parts[2].trim());
                                color = Color.rgb(r, g, b);
                            } else {
                                color = Color.parseColor(colorStr);
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                runOnUiThread(() -> {
                                    getWindow().setStatusBarColor(color);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        float luminance = (Color.red(color) * 0.299f + Color.green(color) * 0.587f + Color.blue(color) * 0.114f) / 255f;
                                        View decor = getWindow().getDecorView();
                                        if (luminance > 0.5f) {
                                            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                                        } else {
                                            decor.setSystemUiVisibility(0);
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {
                            // Ignore parse errors
                        }
                    }
                );
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl != null && failingUrl.equals(SOURCE_URL)) {
                    hideSplash();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
                    hideSplash();
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
        });

        webView.loadUrl(SOURCE_URL);

    }

    private void hideSplash() {
        if (splashDismissed) return;
        splashDismissed = true;
        if (pulseAnimator != null) pulseAnimator.cancel();
        webView.setVisibility(View.VISIBLE);

        if (splashIcon != null) {
            splashIcon.animate()
                .scaleX(15f).scaleY(15f)
                .alpha(0f)
                .setDuration(500)
                .start();
        }
        splashView.animate()
            .alpha(0f)
            .setStartDelay(200)
            .setDuration(400)
            .withEndAction(() -> splashView.setVisibility(View.GONE));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
        return info != null && info.isConnected();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}