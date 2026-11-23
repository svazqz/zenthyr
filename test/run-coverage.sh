#!/bin/sh
set -eu
architecture=$(dpkg --print-architecture)
zenthyr_jdk="/usr/lib/jvm/java-17-openjdk-$architecture"
export PATH="$zenthyr_jdk/bin:$PATH"
export LD_LIBRARY_PATH="$zenthyr_jdk/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export ZENTHYR_NATIVE_TESTS=1
exec timeout 180 xvfb-run -a lein -o with-profile "+coverage,+native-$architecture" cloverage
