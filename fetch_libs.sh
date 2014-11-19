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
    echo "Installing $PACKAGENAME into xwalk_core_library..."
    cp -R $PACKAGENAME ../libs
    popd > /dev/null
    rm -r $TMPDIR
}

download $GECKOVIEW_LIBRARY
download $GECKOVIEW_ASSETS
