(defproject zenthyr "0.1.0-SNAPSHOT"
  :description "A Clojure framework for building desktop applications with web technologies"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.12.0"]
                 [me.friwi/jcefmaven "141.0.10"]]
  :jvm-opts ["--add-opens=java.desktop/sun.awt=ALL-UNNAMED"
             "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED"
             "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"]
  :main ^:skip-aot zenthyr.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:resource-paths ["resources" "src/app"]}}
  :repl-options {:init-ns zenthyr.core}
  :aliases {"dev" ["run"]})
