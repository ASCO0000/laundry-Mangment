package com.laundry.customer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Android Native Host Activity for the Blue Wave Customer Application.
 * Supports emulator access (http://10.0.2.2:8080), physical devices via local IP,
 * pull-to-refresh, dark mode WebSettings, and client-side history navigation.
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private SharedPreferences prefs;
    private static final String PREF_SERVER_URL = "server_url";
    private static final String DEFAULT_EMULATOR_URL = "http://10.0.2.2:8080/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("LaundryAppPrefs", MODE_PRIVATE);
        String serverUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_EMULATOR_URL);

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefreshLayout);

        // Configure Modern WebSettings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                swipeRefresh.setRefreshing(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                swipeRefresh.setRefreshing(false);
                if (request.isForMainFrame()) {
                    showServerConnectionDialog();
                }
            }
        });

        swipeRefresh.setOnRefreshListener(() -> webView.reload());

        // Load application
        webView.loadUrl(serverUrl);
    }

    private void showServerConnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Server Connection");
        builder.setMessage("Cannot connect to laundry backend.\n\n• For Android Emulator: http://10.0.2.2:8080/\n• For Physical Phone: http://<PC-LAN-IP>:8080/\n\nEnter your server URL:");

        final EditText input = new EditText(this);
        input.setText(prefs.getString(PREF_SERVER_URL, DEFAULT_EMULATOR_URL));
        builder.setView(input);

        builder.setPositiveButton("Connect", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    newUrl = "http://" + newUrl;
                }
                if (!newUrl.endsWith("/")) {
                    newUrl = newUrl + "/";
                }
                prefs.edit().putString(PREF_SERVER_URL, newUrl).apply();
                webView.loadUrl(newUrl);
            }
        });

        builder.setNeutralButton("Offline Mode", (dialog, which) -> {
            webView.loadUrl("file:///android_asset/web/index.html");
        });

        builder.setNegativeButton("Retry", (dialog, which) -> webView.reload());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
