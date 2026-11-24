(defproject zenthyr "0.1.0-SNAPSHOT"
  :description "A Clojure framework for building desktop applications with web technologies"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://opensource.org/license/mit/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.12.0"]
                 [me.friwi/jcefmaven "141.0.10"]]
  :aot [zenthyr.core zenthyr.edt-handler]
  :jvm-opts ["--add-opens=java.desktop/sun.awt=ALL-UNNAMED"
             "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED"
             "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
             "--enable-native-access=ALL-UNNAMED"]
  :target-path "target/%s"
  :profiles {:coverage {:plugins [[lein-cloverage "1.2.2"]]}
             :native-amd64 {:dependencies [[me.friwi/jcef-natives-linux-amd64 "jcef-2caef5a+cef-141.0.10+g1d65b0d+chromium-141.0.7390.123"]]}
             :native-arm64 {:dependencies [[me.friwi/jcef-natives-linux-arm64 "jcef-2caef5a+cef-141.0.10+g1d65b0d+chromium-141.0.7390.123"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:resource-paths ["resources"]}}
  :repl-options {:init-ns zenthyr.core})
