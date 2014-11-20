/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

/**
 * Execute a cordova command.  It is up to the native side whether this action
 * is synchronous or asynchronous.  The native side can return:
 *      Synchronous: PluginResult object as a JSON string
 *      Asynchronous: Empty string ""
 * If async, the native side will cordova.callbackSuccess or cordova.callbackError,
 * depending upon the result of the action.
 *
 * @param {Function} success    The success callback
 * @param {Function} fail       The fail callback
 * @param {String} service      The name of the service to use
 * @param {String} action       Action to be run in cordova
 * @param {String[]} [args]     Zero or more arguments to pass to the method
 */
var cordova = require('cordova'),
    utils = require('cordova/utils'),
    base64 = require('cordova/base64'),
    channel = require('cordova/channel'),
    jsToNativeModes = {
        //We only support the JS_OBJECT in this one, because this is Mozilla and hijacking the prompt is ghetto
        JS_OBJECT: 0
    },
    nativeToJsModes = {
        // Polls for messages using the JS->Native bridge.
        POLLING: 0,
        // For LOAD_URL to be viable, it would need to have a work-around for
        // the bug where the soft-keyboard gets dismissed when a message is sent.
        LOAD_URL: 1
    },
    jsToNativeBridgeMode,  // Set lazily.
    nativeToJsBridgeMode = nativeToJsModes.POLLING, //We call "gap_event" on the mozillaBridge object
    pollEnabled = false,
    messagesFromNative = [],
    bridgeSecret = -1;

function mozillaviewExec(success, fail, service, action, args) {
    
    // Set default bridge modes if they have not already been set.
    if (jsToNativeBridgeMode === undefined) {
        mozillaviewExec.setJsToNativeBridgeMode(jsToNativeModes.JS_OBJECT);
    }

    // Process any ArrayBuffers in the args into a string.
    for (var i = 0; i < args.length; i++) {
        if (utils.typeName(args[i]) == 'ArrayBuffer') {
            args[i] = base64.fromArrayBuffer(args[i]);
        }
    }

    var callbackId = service + cordova.callbackId++,
        argsJson = JSON.stringify(args);

    if (success || fail) {
        cordova.callbacks[callbackId] = {success:success, fail:fail};
    }

    /* 
     * The bridge in Mozilla is async, so we need a callback here to handle this.
     *
     * The anonymous method is so we can process the messages.
     */

    window.mozillaBridge.exec(service, action, callbackId, argsJson, function(response) {
        var result = response.result;
        mozillaviewExec.processMessages(result, true);
    });
}

mozillaviewExec.init = function() {
    channel.onNativeReady.fire();
};

function pollOnceFromOnlineEvent() {
    pollOnce(true);
}

function pollOnce(opt_fromOnlineEvent) {
    window.mozillaBridge.poll(function(response) {
      var result = response.result;
      mozillaviewExec.processMessages(result);
    });
}

function pollingTimerFunc() {
    if (pollEnabled) {
        pollOnce();
        setTimeout(pollingTimerFunc, 50);
    }
}

mozillaviewExec.jsToNativeModes = jsToNativeModes;
mozillaviewExec.nativeToJsModes = nativeToJsModes;

mozillaviewExec.setJsToNativeBridgeMode = function(mode) {
    if (mode == jsToNativeModes.JS_OBJECT && !window._cordovaNative) {
        mode = jsToNativeModes.PROMPT;
    }
    jsToNativeBridgeMode = mode;
};

mozillaviewExec.setNativeToJsBridgeMode = function(mode) {
    //We only support polling right now
    pollEnabled = true;
    setTimeout(pollingTimerFunc, 1);
};

function buildPayload(payload, message) {
    var payloadKind = message.charAt(0);
    if (payloadKind == 's') {
        payload.push(message.slice(1));
    } else if (payloadKind == 't') {
        payload.push(true);
    } else if (payloadKind == 'f') {
        payload.push(false);
    } else if (payloadKind == 'N') {
        payload.push(null);
    } else if (payloadKind == 'n') {
        payload.push(+message.slice(1));
    } else if (payloadKind == 'A') {
        var data = message.slice(1);
        var bytes = window.atob(data);
        var arraybuffer = new Uint8Array(bytes.length);
        for (var i = 0; i < bytes.length; i++) {
            arraybuffer[i] = bytes.charCodeAt(i);
        }
        payload.push(arraybuffer.buffer);
    } else if (payloadKind == 'S') {
        payload.push(window.atob(message.slice(1)));
    } else if (payloadKind == 'M') {
        var multipartMessages = message.slice(1);
        while (multipartMessages !== "") {
            var spaceIdx = multipartMessages.indexOf(' ');
            var msgLen = +multipartMessages.slice(0, spaceIdx);
            var multipartMessage = multipartMessages.substr(spaceIdx + 1, msgLen);
            multipartMessages = multipartMessages.slice(spaceIdx + msgLen + 1);
            buildPayload(payload, multipartMessage);
        }
    } else {
        payload.push(JSON.parse(message));
    }
}

// Processes a single message, as encoded by NativeToJsMessageQueue.java.
function processMessage(message) {
    try {
        var firstChar = message.charAt(0);
        if (firstChar == 'J') {
            eval(message.slice(1));
        } else if (firstChar == 'S' || firstChar == 'F') {
            var success = firstChar == 'S';
            var keepCallback = message.charAt(1) == '1';
            var spaceIdx = message.indexOf(' ', 2);
            var status = +message.slice(2, spaceIdx);
            var nextSpaceIdx = message.indexOf(' ', spaceIdx + 1);
            var callbackId = message.slice(spaceIdx + 1, nextSpaceIdx);
            var payloadMessage = message.slice(nextSpaceIdx + 1);
            var payload = [];
            buildPayload(payload, payloadMessage);
            cordova.callbackFromNative(callbackId, success, status, payload, keepCallback);
        } else {
            console.log("processMessage failed: invalid message: " + JSON.stringify(message));
        }
    } catch (e) {
        console.log("processMessage failed: Error: " + e);
        console.log("processMessage failed: Stack: " + e.stack);
        console.log("processMessage failed: Message: " + message);
    }
}

var isProcessing = false;

// This is called from the NativeToJsMessageQueue.java.
mozillaviewExec.processMessages = function(messages, opt_useTimeout) {
    if (messages) {
        messagesFromNative.push(messages);
    }
    // Check for the reentrant case.
    if (isProcessing) {
        return;
    }
    if (opt_useTimeout) {
        window.setTimeout(mozillaviewExec.processMessages, 0);
        return;
    }
    isProcessing = true;
    try {
        // TODO: add setImmediate polyfill and process only one message at a time.
        while (messagesFromNative.length) {
            var msg = popMessageFromQueue();
            // The Java side can send a * message to indicate that it
            // still has messages waiting to be retrieved.
            if (msg == '*' && messagesFromNative.length === 0) {
                console.log("We should be polling here");
                setTimeout(pollOnce, 0);
                return;
            }
            processMessage(msg);
        }
    } finally {
        isProcessing = false;
    }
};

function popMessageFromQueue() {
    var messageBatch = messagesFromNative.shift();
    if (messageBatch == '*') {
        return '*';
    }

    var spaceIdx = messageBatch.indexOf(' ');
    var msgLen = +messageBatch.slice(0, spaceIdx);
    var message = messageBatch.substr(spaceIdx + 1, msgLen);
    messageBatch = messageBatch.slice(spaceIdx + msgLen + 1);
    if (messageBatch) {
        messagesFromNative.unshift(messageBatch);
    }
    return message;
}

module.exports = mozillaviewExec;
