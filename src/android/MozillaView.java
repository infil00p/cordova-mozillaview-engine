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

public class MozillaView extends GeckoView implements CordovaWebView{

    private static final String TAG = "MozillaView";
    
    static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER =
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


    public MozillaView(Context context) {
        super(context);
        if (CordovaInterface.class.isInstance(context))
        {
            this.cordova = (CordovaInterface) context;
        }
        
        /*
         * Load the chrome and content delegates
         */
        chrome = new CordovaGeckoViewChrome(this, cordova);
        
        // Handle the keycodes
        
        this.setChromeDelegate(chrome);
        
        this.loadConfiguration();
    }
    
    /**
     * Check configuration parameters from Config.
     * Approved list of URLs that can be loaded into Cordova
     *      <access origin="http://server regexp" subdomains="true" />
     * Log level: ERROR, WARN, INFO, DEBUG, VERBOSE (default=ERROR)
     *      <log level="DEBUG" />
     */
    private void loadConfiguration() {
 
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

    public void stopLoading() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean canGoBack() {
        currentBrowser = this.getCurrentBrowser();
        if(currentBrowser != null)
        {
            return currentBrowser.canGoBack();
        }
        return false;
    }

    @Override
    public void clearCache(boolean b) {
        //TODO: Implement this
    }

    @Override
    public void clearHistory() {
        //
    }

    @Override
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

    @Override
    public void handlePause(boolean keepRunning) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleResume(boolean keepRunning,
            boolean activityResultKeepRunning) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleDestroy() {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void sendJavascript(String statement) {
        Log.d(TAG, "Use the bridge: " + statement);
    }


    @Override
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
    public boolean isCustomViewShowing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void showCustomView(View view, CustomViewCallback callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void hideCustomView() {
        // TODO Auto-generated method stub
        
    }




    @Override
    public int getVisibility() {
        return super.getVisibility();
    }

    @Override
    public void setOverScrollMode(int overScrollNever) {
        super.setOverScrollMode(overScrollNever);
    }



    @Override
    public void setNetworkAvailable(boolean online) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public CordovaResourceApi getResourceApi() {
        return resourceApi;
    }

    @Override
    public void sendPluginResult(PluginResult cr, String callbackId) {
        chrome.addPluginResult(cr, callbackId);
    }

    
    @Override
    public PluginManager getPluginManager() {
        // TODO Auto-generated method stub
        return pluginManager;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public String getUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(CordovaInterface cordova, List<PluginEntry> pluginEntries,
            Whitelist internalWhitelist, Whitelist externalWhitelist,
            CordovaPreferences preferences) {
        
        //Setup the resourceApi and pluginManager
        pluginManager = new PluginManager(this, this.cordova, pluginEntries);
        resourceApi = new CordovaResourceApi(this.getContext(), pluginManager);
        
        //Load the mozilla JS bridge 
        importScript("resource://android/assets/www/bridge/mozilla.js");
    }

    /*
     * onKeyDown
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(boundKeyCodes.contains(keyCode))
        {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    this.loadUrl("javascript:cordova.fireDocumentEvent('volumedownbutton');");
                    return true;
            }
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    this.loadUrl("javascript:cordova.fireDocumentEvent('volumeupbutton');");
                    return true;
            }
            else
            {
                return super.onKeyDown(keyCode, event);
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            return !(this.startOfHistory()) || isButtonPlumbedToJs(KeyEvent.KEYCODE_BACK);

        }
        else if(keyCode == KeyEvent.KEYCODE_MENU)
        {
            //How did we get here?  Is there a childView?
            View childView = this.getFocusedChild();
            if(childView != null)
            {
                //Make sure we close the keyboard if it's present
                InputMethodManager imm = (InputMethodManager) cordova.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(childView.getWindowToken(), 0);
                cordova.getActivity().openOptionsMenu();
                return true;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
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
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        // If back key
        if (keyCode == KeyEvent.KEYCODE_BACK) {
                // The webview is currently displayed
                // If back key is bound, then send event to JavaScript
                if (isButtonPlumbedToJs(KeyEvent.KEYCODE_BACK)) {
                    this.loadUrl("javascript:cordova.fireDocumentEvent('backbutton');");
                    return true;
                } else {
                    // If not bound
                    // Go to previous page in webview if it is possible to go back
                    if (this.backHistory()) {
                        return true;
                    }
                    // If not, then invoke default behavior
                }
        }
        // Legacy
        else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (this.lastMenuEventTime < event.getEventTime()) {
                this.loadUrl("javascript:cordova.fireDocumentEvent('menubutton');");
            }
            this.lastMenuEventTime = event.getEventTime();
            return super.onKeyUp(keyCode, event);
        }
        // If search key
        else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            this.loadUrl("javascript:cordova.fireDocumentEvent('searchbutton');");
            return true;
        }

        //Does webkit change this behavior?
        return super.onKeyUp(keyCode, event);
    }
    
    /*
     * WTF is this?
     * @see org.apache.cordova.CordovaWebView#setButtonPlumbedToJs(int, boolean)
     */
    
    
    @Override
    public void setButtonPlumbedToJs(int keyCode, boolean override) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isButtonPlumbedToJs(int keyCode) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Whitelist getWhitelist() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CordovaPreferences getPreferences() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onFilePickerResult(Uri uri) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Object postMessage(String id, Object data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Whitelist getExternalWhitelist() {
        // TODO Auto-generated method stub
        return null;
    }

}
