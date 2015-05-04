package org.apache.cordova.engine.mozilla;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.LOG;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.apache.cordova.Whitelist;
import org.json.JSONException;
import org.mozilla.gecko.GeckoView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;

public class CordovaGeckoView extends GeckoView implements CordovaWebViewEngine.EngineView{

    private static final String TAG = "MozillaView";
    
    private static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER =
            new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);

    //The CordovaInterface, we need to have access to this!
    private CordovaInterface cordova;
    private String url;

    public PluginManager pluginManager;

    //Geckoview's current browser object! 
    private Browser currentBrowser;
    private CordovaGeckoViewChrome chrome;
    private CordovaGeckoViewContent content;

    private int loadUrlTimeout;

    private CordovaResourceApi resourceApi;

    private long lastMenuEventTime;

    private HashSet<Integer> boundKeyCodes = new HashSet<Integer>();
    private CordovaGeckoViewEngine parentEngine;

    public CordovaGeckoView(Context context) {
        super(context);
    }

    public CordovaGeckoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CordovaGeckoView(CordovaGeckoViewEngine cordovaGeckoViewEngine) {
        super(cordovaGeckoViewEngine.mCtx);
        parentEngine = cordovaGeckoViewEngine;
    }


    // Package visibility to enforce that only SystemWebViewEngine should call this method.
    void init(CordovaGeckoViewEngine parentEngine, CordovaInterface cordova) {
        this.cordova = cordova;
        this.parentEngine = parentEngine;
        this.pluginManager = parentEngine.pluginManager;

        //Load the mozilla JS bridge
        importScript("resource://android/assets/www/bridge/mozilla.js");
    }

    /**
     * Check configuration parameters from Config.
     * Approved list of URLs that can be loaded into Cordova
     *      <access origin="http://server regexp" subdomains="true" />
     * Log level: ERROR, WARN, INFO, DEBUG, VERBOSE (default=ERROR)
     *      <log level="DEBUG" />
     */
    void loadConfiguration() {

        if ("true".equals(this.getProperty("Fullscreen", "false"))) {
            this.cordova.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            this.cordova.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }


    /**
     * Get string property for activity.
     *
     * @param name
     * @param defaultValue
     * @return the String value for the named property
     */
    public String getProperty(String name, String defaultValue) {
        Bundle bundle = this.cordova.getActivity().getIntent().getExtras();
        if (bundle == null) {
            return defaultValue;
        }
        name = name.toLowerCase(Locale.getDefault());
        Object p = bundle.get(name);
        if (p == null) {
            return defaultValue;
        }
        return p.toString();
    }

    @Override
    public void setId(int i) {
        super.setId(i);
    }

    @Override
    public void setVisibility(int invisible) {
        super.setVisibility(invisible);
    }


    /*
     * (non-Javadoc)
     * @see org.apache.cordova.CordovaWebView#loadUrl(java.lang.String)
     */
    public void loadUrl(String url) {
        if(chrome != null)
        {
            currentBrowser = this.getCurrentBrowser();
            if(currentBrowser != null)
            {
                currentBrowser.loadUrl(url);
            }
        }
    }

    
    /**
     * Load the url into the webview.
     *
     * @param url
     */
    public void loadUrlIntoView(final String url, boolean recreatePlugins) {
        LOG.d(TAG, ">>> loadUrl(" + url + ")");

        if (recreatePlugins) {
            this.url = url;
            this.pluginManager.init();
        }
        
        loadUrl(url);

    }

    public boolean canGoBack() {
        currentBrowser = this.getCurrentBrowser();
        if(currentBrowser != null)
        {
            return currentBrowser.canGoBack();
        }
        return false;
    }

    public boolean backHistory() {
        currentBrowser = this.getCurrentBrowser();
        if(currentBrowser != null)
        {
            boolean returnValue = currentBrowser.canGoBack();
            if(returnValue)
            {
                currentBrowser.goBack();
            }
            return returnValue;
        }
        else
            return false;
    }


    public void handleResume(boolean keepRunning,
            boolean activityResultKeepRunning) {
        // TODO Auto-generated method stub
        
    }

    public void showWebPage(String errorUrl, boolean b, boolean c,
            HashMap<String, Object> params) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public View getFocusedChild() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getVisibility() {
        return super.getVisibility();
    }

    @Override
    public void setOverScrollMode(int overScrollNever) {
        super.setOverScrollMode(overScrollNever);
    }

    public CordovaResourceApi getResourceApi() {
        return resourceApi;
    }

    public void sendPluginResult(PluginResult cr, String callbackId) {
        chrome.addPluginResult(cr, callbackId);
    }

    
    public PluginManager getPluginManager() {
        // TODO Auto-generated method stub
        return pluginManager;
    }

    public View getView() {
        return this;
    }

    public String getUrl() {
        // TODO Auto-generated method stub
        return null;
    }


    private boolean startOfHistory() {
        //Figure out if we're at the start of GeckoView's history, and return
        currentBrowser = this.getCurrentBrowser();
        if(currentBrowser != null)
            return !currentBrowser.canGoBack();
        else
            return true;
    }

    @Override
    public CordovaWebView getCordovaWebView() {
        return parentEngine != null ? parentEngine.getCordovaWebView() : null;
    }
}
