package in.securebank.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Android wrapper for the responsive SecureBank web client.
 * For the full deployment, replace the local asset with a configured HTTPS backend/web URL.
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16, 35, 68));
        getWindow().setNavigationBarColor(Color.rgb(16, 35, 68));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(243, 246, 251));

        TextView banner = new TextView(this);
        banner.setText("SECUREBANK  •  EDUCATIONAL DEMO");
        banner.setTextColor(Color.WHITE);
        banner.setTextSize(11);
        banner.setLetterSpacing(0.06f);
        banner.setPadding(dp(16), dp(12), dp(16), dp(12));
        banner.setBackgroundColor(Color.rgb(16, 35, 68));
        root.addView(banner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        WebView webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(243, 246, 251));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/index.html");
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView footer = new TextView(this);
        footer.setText("Demo only • No real transactions");
        footer.setTextColor(Color.rgb(105, 119, 145));
        footer.setTextSize(10);
        footer.setPadding(dp(12), dp(7), dp(12), dp(7));
        footer.setBackgroundColor(Color.WHITE);
        root.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
