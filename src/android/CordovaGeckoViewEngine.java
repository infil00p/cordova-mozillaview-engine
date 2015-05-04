package org.apache.cordova.engine.mozilla;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.webkit.WebChromeClient;

import org.apache.cordova.CordovaBridge;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;

import org.apache.cordova.engine.SystemWebView;

import java.util.List;
import java.util.Map;

/**
 * Created by jbowser on 15-04-28.
 */
public class CordovaGeckoViewEngine implements CordovaWebViewEngine {


    CordovaGeckoView webView;
    Context mCtx;
    protected CordovaBridge bridge;
    protected CordovaWebViewEngine.Client client;
    protected CordovaWebView parentWebView;
    protected CordovaInterface cordova;
    protected PluginManager pluginManager;
    protected CordovaGeckoViewChrome chrome;
    protected CordovaResourceApi resourceApi;
    protected NativeToJsMessageQueue nativeToJsMessageQueue;
    protected CordovaPreferences prefs;
    private BroadcastReceiver receiver;


    /** Used when created via reflection. */
    public CordovaGeckoViewEngine(Context context, CordovaPreferences preferences) {
        mCtx = context;
        webView = new CordovaGeckoView(this);
        //testView = new SystemWebView(context);
        prefs = preferences;
    }

    public CordovaGeckoViewEngine(CordovaGeckoView webView) {
        this.webView = webView;
    }



    @Override
    public void init(CordovaWebView parentWebView, CordovaInterface cordova, Client client, CordovaResourceApi resourceApi, PluginManager pluginManager, NativeToJsMessageQueue nativeToJsMessageQueue) {
        this.parentWebView = parentWebView;
        this.cordova = cordova;
        this.client = client;
        this.resourceApi = resourceApi;
        this.pluginManager = pluginManager;
        this.nativeToJsMessageQueue = nativeToJsMessageQueue;

        chrome = new CordovaGeckoViewChrome(this, cordova);

        // We set the delegate on the Engine first.

        webView.setChromeDelegate(chrome);
        webView.loadConfiguration();
        webView.init(this, cordova);
    }

    @Override
    public CordovaWebView getCordovaWebView() {
        return parentWebView;
    }

    @Override
    public ICordovaCookieManager getCookieManager() {
        return null;
    }

    @Override
    public View getView() {
        return null;
    }


    /**
     * Load the url into the webview.
     */
    @Override
    public void loadUrl(final String url, boolean clearNavigationStack) {
        webView.loadUrl(url);
    }

    @Override
    public void stopLoading() {
        //This does nothing! Why do we have to implement this?
        webView.getCurrentBrowser().stop();
    }


    @Override
    public String getUrl() {
        return webView.getUrl();
    }

    @Override
    public void clearCache() {
        //I don't think we can clear the cache
    }

    @Override
    public void clearHistory() {
        //TODO: Figure out how to clear history
    }


    @Override
    public boolean canGoBack() {
        return webView.canGoBack();
    }

    /**
     * Go to previous page in history.
     *
     * @return true if we went back, false if we are already at top
     */
    @Override
    public boolean goBack() {
        // Check webview first to see if there is a history
        // This is needed to support curPage#diffLink, since they are added to parentEngine's history, but not our history url array (JQMobile behavior)
        if (webView.canGoBack()) {
            webView.backHistory();
            return true;
        }
        return false;
    }

    @Override
    public void setPaused(boolean value) {

    }

    @Override
    public void destroy() {

    }


}
