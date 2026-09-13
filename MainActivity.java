package com.dhiqar.tv;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    // ضع بيانات اشتراكك في هذه الأسطر فقط.
    private static final String SERVER = "http://a83967.xyz:8080";
    private static final String USERNAME = "106253449142";
    private static final String PASSWORD = "510546558453";

    private FrameLayout root;
    private WebView webView;
    private PlayerView playerView;
    private ExoPlayer player;
    private ImageView splash;
    private TextView splashText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(3, 7, 17));
        getWindow().setNavigationBarColor(Color.rgb(3, 7, 17));

        buildRoot();
        showSplash();
    }

    private void buildRoot() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(3, 7, 17));
        setContentView(root);
    }

    private void showSplash() {
        splash = new ImageView(this);
        splash.setImageResource(com.dhiqar.tv.R.drawable.splash_crop);
        splash.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));

        splashText = new TextView(this);
        splashText.setText("ذي قار TV 2027\nجاري تجهيز القنوات...");
        splashText.setTextColor(Color.WHITE);
        splashText.setTextSize(16);
        splashText.setGravity(Gravity.CENTER);
        splashText.setShadowLayer(12f, 0f, 0f, Color.CYAN);

        FrameLayout.LayoutParams textParams =
                new FrameLayout.LayoutParams(-1, -2);
        textParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        textParams.bottomMargin = 35;
        root.addView(splashText, textParams);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(splash, View.SCALE_X, 1.0f, 1.07f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(splash, View.SCALE_Y, 1.0f, 1.07f);
        scaleX.setDuration(2600);
        scaleY.setDuration(2600);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.start();

        handler.postDelayed(this::showApp, 3000);
    }

    private void showApp() {
        if (splash != null) {
            splash.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                root.removeView(splash);
                if (splashText != null) root.removeView(splashText);
            }).start();
        }

        createWebView();
        createPlayer();
        loadChannels();
    }

    private void createWebView() {
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidPlayer");

        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void createPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView = new PlayerView(this);
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setBackgroundColor(Color.BLACK);
        playerView.setVisibility(View.GONE);

        // مكان المشغل مطابق تقريباً للوحة اليمنى في تصميم التلفزيون.
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, 0);
        p.gravity = Gravity.TOP;
        p.topMargin = 105;
        p.leftMargin = 10;
        p.rightMargin = 10;
        p.height = dp(330);
        root.addView(playerView, p);
    }

    private void loadChannels() {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String u = URLEncoder.encode(USERNAME, StandardCharsets.UTF_8.name());
                String p = URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8.name());

                String endpoint = SERVER + "/player_api.php?username=" + u
                        + "&password=" + p + "&action=get_live_streams";

                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setUseCaches(false);

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new Exception("HTTP " + code);

                StringBuilder body = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) body.append(line);
                }

                JSONArray data = new JSONArray(body.toString());
                JSONArray clean = new JSONArray();

                for (int i = 0; i < data.length(); i++) {
                    JSONObject s = data.optJSONObject(i);
                    if (s == null) continue;
                    String id = s.optString("stream_id", "");
                    if (id.isEmpty()) continue;

                    JSONObject c = new JSONObject();
                    c.put("id", i + 1);
                    c.put("streamId", id);
                    c.put("name", s.optString("name", "Channel " + (i + 1)));
                    c.put("logo", s.optString("stream_icon", ""));
                    c.put("category", categoryFor(s.optString("name", "")));
                    c.put("epg", "بث مباشر");
                    clean.put(c);
                }

                final String json = clean.toString();
                handler.post(() -> webView.evaluateJavascript(
                        "window.setChannels(" + JSONObject.quote(json) + ");", null));

            } catch (Exception e) {
                final String msg = e.getMessage() == null ? "تعذر الاتصال بالسيرفر" : e.getMessage();
                handler.post(() -> webView.evaluateJavascript(
                        "window.showServerError(" + JSONObject.quote(msg) + ");", null));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private String categoryFor(String n) {
        String s = n.toLowerCase();
        if (s.matches(".*(sport|bein|ssc|رياض).*")) return "sport";
        if (s.matches(".*(movie|cinema|osn|rotana|فيلم|سينما).*")) return "movies";
        if (s.matches(".*(news|jazeera|خبر|أخبار).*")) return "news";
        if (s.matches(".*(kids|cartoon|طفل|أطفال).*")) return "kids";
        if (s.matches(".*(iraq|العراق|sharqiya|dijlah).*")) return "iraq";
        return "all";
    }

    private void playStream(String streamId) {
        String url = SERVER + "/live/" + USERNAME + "/" + PASSWORD + "/" + streamId + ".m3u8";

        try {
            playerView.setVisibility(View.VISIBLE);
            MediaItem item = MediaItem.fromUri(Uri.parse(url));
            player.setMediaItem(item);
            player.prepare();
            player.play();
        } catch (Exception e) {
            if (webView != null) webView.evaluateJavascript(
                    "window.playerError(" + JSONObject.quote(e.getMessage()) + ");", null);
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void play(String streamId) {
            handler.post(() -> playStream(streamId));
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        if (player != null) {
            player.release();
            player = null;
        }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
