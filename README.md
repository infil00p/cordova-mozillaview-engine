

Apache Cordova MozillaView Engine Proof-of-Concept
===

Cordova MozillaView Engine is a part of a Proof-of-Concept for 
third party engines to work with Apache Cordova.  This currently only works with the
4.0.x branch of Apache Cordova found on GitHub, and as of yet, does not work with any
official Apache Cordova release.  This code should still be considered experimental.

(Note: I also am completely unclear as to which licence this is under right now, Assume Apache 2.0 or MPL)

Directions (We're still trying to automate more of this, sorry):

Android-only:

1. Pull down the `4.0.x` branch of [Apache Cordova](https://github.com/apache/cordova-android) found here (https://github.com/apache/cordova-android/tree/4.0.x)
2. Clone this repository.
3. Run `sh fetch_libs.sh` to download the GeckoView library
4. Generate a project with `./bin/create`
5. Run Plugman: `plugman install --platform android --plugin <path_to_mozillaview_engine>/cordova-mozillaview-engine/ --project .`
6. Add the `libs/geckoview_library` as a dependency in `project.properties`. (Note: Relative Paths work for libraries, not absolute paths.  Manually edit if necessary.)

Cordova CLI:

1. Install the latest version of the Cordova CLI from npm (Requires at least 3.5.0-0.2.6)
2. Pull down the `4.0.x` branch of [Apache Cordova](https://github.com/apache/cordova-android) found here (https://github.com/apache/cordova-android/tree/4.0.x)
3. Clone this repository.
4. Run `sh fetch_libs.sh` to download the GeckoView library
5. Create a project with `cordova create`
6. Add the Android platform with `cordova platform add <path to cordova-android>`
7. Add the MozillaView Engine plugin with `cordova plugin add <path to mozillaview-engine-plugin>`

Requirements:

`fetch_libs.sh` requires `curl` and `unzip` to be installed, and on the current `PATH`.  In this case, it also requires that you have the Android SDK installed with the Android Support Library v4, since it will copy the JAR out of the directory so it can be installed by plugman/CLI.

Note: To use the index.html in the www directory, the URI must be changed to resource://android/asset/www/index.html.  This is a relatively minor change, but this is a PoC.
