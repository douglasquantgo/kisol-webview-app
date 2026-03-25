package com.kisoltec.webviewapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;

/**
 * MainActivity - WebView App para KisolTec
 *
 * Compatível com Android 5.0 (API 21) até versões recentes
 * Inclui tratativas especiais para dispositivos Samsung
 */
public class MainActivity extends AppCompatActivity {

    private static final String URL_APP = "https://kisoltec-producao-webapp.web.app/";
    private static final int PAGE_LOAD_TIMEOUT = 30000; // 30 segundos

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorLayout;
    private LinearLayout loadingLayout;
    private Button btnRetry;

    private boolean isPageLoaded = false;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tratativa para Samsung Android 5 - evitar crash com SSL
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                // Força inicialização de cookies para Android 5
                CookieSyncManager.createInstance(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setContentView(R.layout.activity_main);

        initViews();
        setupTimeout();
        checkInternetAndLoad();
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        errorLayout = findViewById(R.id.errorLayout);
        loadingLayout = findViewById(R.id.loadingLayout);
        btnRetry = findViewById(R.id.btnRetry);

        btnRetry.setOnClickListener(v -> checkInternetAndLoad());
    }

    private void setupTimeout() {
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = () -> {
            if (!isPageLoaded) {
                showError();
                Toast.makeText(MainActivity.this,
                    "Tempo de carregamento esgotado", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void checkInternetAndLoad() {
        if (isInternetAvailable()) {
            setupWebView();
            loadUrl();
        } else {
            showError();
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
            getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Configurações básicas
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Configurações de cache
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getCacheDir().getAbsolutePath());

        // Configurações para mixed content (HTTP em HTTPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        // User Agent - importante para compatibilidade
        String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(userAgent + " KisolTecWebView/1.0");

        // Configurar WebViewClient
        webView.setWebViewClient(new CustomWebViewClient());

        // Configurar WebChromeClient para progresso
        webView.setWebChromeClient(new CustomWebChromeClient());
    }

    private void loadUrl() {
        loadingLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        isPageLoaded = false;

        // Iniciar timeout
        timeoutHandler.postDelayed(timeoutRunnable, PAGE_LOAD_TIMEOUT);

        webView.loadUrl(URL_APP);
    }

    private void showContent() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        isPageLoaded = true;
        loadingLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void showError() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        isPageLoaded = false;
        loadingLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    // ========== WebViewClient Customizado ==========

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Manter navegação dentro do WebView
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            showContent();

            // Sincronizar cookies no Android 5
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                CookieSyncManager.getInstance().sync();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request,
                                    WebResourceError error) {
            super.onReceivedError(view, request, error);

            // Só mostra erro se for o frame principal
            if (request.isForMainFrame()) {
                showError();
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                        WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);

            if (request.isForMainFrame()) {
                showError();
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) {
            // IMPORTANTE: Para Samsung Android 5
            // Em produção, você deve implementar validação adequada de certificados
            // Por ora, prosseguir para evitar bloqueio em dispositivos antigos
            handler.proceed();
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler,
                                              String host, String realm) {
            handler.cancel();
        }
    }

    // ========== WebChromeClient Customizado ==========

    private class CustomWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);

            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            // Opcional: atualizar título da atividade
        }
    }

    // ========== Navegação com botão voltar ==========

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        // Confirmação para sair
        if (keyCode == KeyEvent.KEYCODE_BACK && !webView.canGoBack()) {
            showExitConfirmation();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja realmente sair do aplicativo?")
            .setPositiveButton("Sim", (dialog, which) -> {
                finish();
            })
            .setNegativeButton("Não", null)
            .show();
    }

    // ========== Ciclo de vida ==========

    @Override
    protected void onResume() {
        super.onResume();

        // Resume WebView
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }

        // Sync cookies para Android 5
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieSyncManager.getInstance().stopSync();
            CookieSyncManager.getInstance().sync();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause WebView
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }

        // Sync cookies para Android 5
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieSyncManager.getInstance().stopSync();
        }
    }

    @Override
    protected void onDestroy() {
        // Limpar WebView para evitar vazamento de memória
        if (webView != null) {
            webView.destroy();
        }

        timeoutHandler.removeCallbacks(timeoutRunnable);
        super.onDestroy();
    }

    // ========== Utilitários ==========

    private int getDeviceSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    private boolean isSamsungDevice() {
        String manufacturer = Build.MANUFACTURER;
        return manufacturer != null && manufacturer.toLowerCase().contains("samsung");
    }

    private String getDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL + " (API " + Build.VERSION.SDK_INT + ")";
    }
}
