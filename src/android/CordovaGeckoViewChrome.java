package org.apache.cordova.engine.mozilla;

import org.apache.cordova.Config;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.mozilla.gecko.GeckoView;
import org.mozilla.gecko.GeckoView.MessageResult;
import org.mozilla.gecko.GeckoViewChrome;
import org.mozilla.gecko.PrefsHelper;

import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class CordovaGeckoViewChrome extends GeckoViewChrome {
    
    PluginManager pluginManager;
    NativeToJsMessageQueue jsMessageQueue; //Exists for code re-use, may not be used for a final release
    
    String LOGTAG = "CordovaGeckoViewChrome";
    
    CordovaGeckoViewChrome(MozillaView view, CordovaInterface cordova)
    {
        jsMessageQueue = new NativeToJsMessageQueue(view, cordova);
        pluginManager = view.getPluginManager();
        //We only do polling
        jsMessageQueue.setBridgeMode(0);
    }
    
    public void onReady(GeckoView view) {
        Log.i(LOGTAG, "Gecko is ready");

        PrefsHelper.setPref("devtools.debugger.remote-enabled", true);

        /* Load URL does nothing, we have to wait unitl things are ready before loading */
        view.addBrowser(Config.getStartUrl());
        //Make sure this is visible regardless of what Cordova does.
        view.setVisibility(View.VISIBLE);
    }
    
    public void onScriptMessage(GeckoView view, Bundle input, MessageResult out) {
        // First, get the parameters being passed into the service
        
        String callbackId = input.getString("callbackId");
        String service = input.getString("service");
        String action = input.getString("action");
        //We do the parsing on the plugin itself, not here.  This should be JSON
        String rawArgs = input.getString("args");
        
        
        //Do nothing if we're just polling, otherwise 
        if(!action.equals("gap_poll"))
        {
            if(pluginManager == null)
            {
                pluginManager = ((MozillaView) view).getPluginManager();
            }
            if(pluginManager != null)
            {
                Log.d(LOGTAG, "RAWARGS:" + rawArgs);
                pluginManager.exec(service, action, callbackId, rawArgs);
            }
        }        
        
        //By default we do exec chaining
        Bundle ret = new Bundle();
        // Get the messages and add them 
        String value = jsMessageQueue.popAndEncode(false);
        ret.putString("result", value);
        out.success(ret);
        
    }

    public void addPluginResult(PluginResult result, String callbackId)
    {
        jsMessageQueue.addPluginResult(result, callbackId);
    }
    
}
