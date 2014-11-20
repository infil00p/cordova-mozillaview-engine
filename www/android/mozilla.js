/*
 * Sample script injected into the 'chrome' scope of the GeckoView.
 * This script sets up a simple system of injecting a JS object into all content loaded
 * into the GeckoView.
 */

const { classes: Cc, interfaces: Ci, manager: Cm, utils: Cu, results: Cr } = Components;

Cu.import("resource://gre/modules/Services.jsm");

var BrowserApp = null;

function load(params) {
  console.log("Injected script loaded");

  // Get access to the core BrowserApp object since it has a lot of helpful methods  
  BrowserApp = params.window.BrowserApp;
  BrowserApp.deck.addEventListener("DOMWindowCreated", onWindowCreated, true);
  BrowserApp.deck.addEventListener("load", onPageLoad, true);


}

function onWindowCreated(event) {
  console.log("We're attaching the bridge to the window");
  // the target is an HTMLDocument
  let contentDocument = event.target;

  // We need the unprotected version of the contentWindow for injecting JS objects
  let unsafeWindow = contentDocument.defaultView.wrappedJSObject;

  let mozillaBridge = {
    exec: function(service, action, callbackId, params, callback) {
      console.log("Callback ID:" + callbackId);
      console.log("JSON Args:" + params);
      var bundle = { "service" : service, "action" : action, "args": params, "callbackId" : callbackId };
      return GeckoView.sendRequestForResult(bundle).then(result => {
        //Send the package to an exec override.
        callback(Cu.cloneInto(result, unsafeWindow));
      });
    },
    poll: function(callback) {
      console.log("Polling the object");
      var bundle = { "action" : "gap_poll" };
      return GeckoView.sendRequestForResult(bundle).then(result => {
        callback(Cu.cloneInto(result, unsafeWindow));
      });
    }
  };

  // Use Cu.cloneInto to add the contentObject into the content window
  // https://developer.mozilla.org/en-US/docs/Components.utils.cloneInto
  unsafeWindow.mozillaBridge = Cu.cloneInto(mozillaBridge, unsafeWindow, { cloneFunctions: true });
}

function onPageLoad(event) {
  // the target is an HTMLDocument
  let contentDocument = event.target;

  // We can get the <browser> element used to host the content
  let browser = BrowserApp.getBrowserForDocument(contentDocument);
  console.log("Page loaded: " + browser.contentTitle);
}
