(defproject {{name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [zenthyr "0.1.0-SNAPSHOT"]]
  :main ^:skip-aot {{name}}.main
  :target-path "target/%s"
  :jvm-opts ["--add-opens=java.desktop/sun.awt=ALL-UNNAMED"
             "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED"
             "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
             "--enable-native-access=ALL-UNNAMED"]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
