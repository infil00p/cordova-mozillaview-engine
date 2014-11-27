NIGHTLY_PATH="https://ftp.mozilla.org/pub/mozilla.org/mobile/nightly/latest-mozilla-central-android/"
GECKOVIEW_LIBRARY=$NIGHTLY_PATH"geckoview_library.zip"
GECKOVIEW_ASSETS=$NIGHTLY_PATH"geckoview_assets.zip"

download() {
    TMPDIR=$(mktemp -d xwdl.XXXXXX)
    pushd $TMPDIR > /dev/null
    echo "Fetching $1..."
    curl -# $1 -o library.zip
    unzip -q library.zip
    rm library.zip
    PACKAGENAME=$(ls|head -n 1)
    echo "Installing $PACKAGENAME"
    cp -R $PACKAGENAME ../libs
    popd > /dev/null
    rm -r $TMPDIR
}

copy_support_library() {
  android=$(which android)
  SDK_DIR=${android/tools\/android/}
  echo "Copying Android Support Library from $SDK_DIR"
  cp $SDK_DIR/extras/android/support/v4/android-support-v4.jar libs
}

update_client_library()
{
  cd libs/geckoview_library
  android update project -p .
}

download $GECKOVIEW_LIBRARY
download $GECKOVIEW_ASSETS
copy_support_library
update_client_library
