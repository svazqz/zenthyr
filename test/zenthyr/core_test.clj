(ns zenthyr.core-test
  (:require [clojure.test :refer :all]
            [zenthyr.core :refer :all]
            [zenthyr.utils :as utils]
            [zenthyr.chromium :as chromium]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]))

(deftest test-ensure-dependencies!
  (testing "Installs dependencies if node_modules is missing"
    (let [sh-called (atom false)]
      (with-redefs [io/file (fn [& args] 
                              (proxy [java.io.File] ["."] 
                                (exists [] false)
                                (getPath [] ".")))
                    sh (fn [& args] 
                         (reset! sh-called true)
                         {:exit 0})]
        (ensure-dependencies!)
        (is @sh-called))))
  
  (testing "Skips installation if node_modules exists"
    (let [sh-called (atom false)]
      (with-redefs [io/file (fn [& args] 
                              (proxy [java.io.File] ["."] 
                                (exists [] true)))
                    sh (fn [& args] 
                         (reset! sh-called true)
                         {:exit 0})]
        (ensure-dependencies!)
        (is (not @sh-called))))))

(deftest test-start-vite-server
  (testing "Starts the frontend process with the selected port"
    (let [root (.toFile (java.nio.file.Files/createTempDirectory
                         "zenthyr-vite-test" (make-array java.nio.file.attribute.FileAttribute 0)))
          port-hint (atom nil)]
      (try
        (spit (io/file root "package.json")
              "{\"scripts\":{\"dev\":\"node -e \\\"process.exit(process.argv.slice(1).join(' ') === '--port 12345' ? 0 : 1)\\\" --\"}}")
        (with-redefs [io/file (fn [& _] root)
                      utils/find-available-port (fn [hint] (reset! port-hint hint) 12345)
                      clojure.core/future-call (fn [consume] (consume))]
          (let [{:keys [port process]} (start-vite-server)]
            (try
              (is (= 12345 port))
              (is (= 5173 @port-hint))
              (is (.waitFor process 10 java.util.concurrent.TimeUnit/SECONDS))
              (when-not (.isAlive process)
                (is (zero? (.exitValue process))))
              (finally
                (.destroyForcibly process)))))
        (finally
          (doseq [entry (reverse (file-seq root))]
            (io/delete-file entry true)))))))

(deftest test-start-app
  (testing "Starts the frontend and browser and registers cleanup"
    (let [calls (atom [])
          hook (atom nil)
          original-handler (Thread/getDefaultUncaughtExceptionHandler)
          original-edt-handler (System/getProperty "sun.awt.exception.handler")]
      (try
        (with-redefs [ensure-dependencies! #(swap! calls conj :dependencies)
                      start-vite-server (fn [] (swap! calls conj :vite) {:port 8080 :process nil})
                      chromium/start-chromium! (fn [options]
                                                 (swap! calls conj options)
                                                 {:close #(swap! calls conj :closed)})
                      utils/suppress-stderr-logging! (fn [])
                      utils/add-shutdown-hook! #(reset! hook %)]
          (start-app! {})
          (is (= [:dependencies :vite {:vite-port 8080 :handler nil}] @calls))
          (when (is (fn? @hook))
            (@hook)
            (is (= :closed (last @calls)))))
        (finally
          (Thread/setDefaultUncaughtExceptionHandler original-handler)
          (if original-edt-handler
            (System/setProperty "sun.awt.exception.handler" original-edt-handler)
            (System/clearProperty "sun.awt.exception.handler")))))))

(deftest test-installation-failure-preserves-the-message
  (with-redefs [io/file (fn [& _]
                        (proxy [java.io.File] ["."]
                          (exists [] false)
                          (getPath [] ".")))
                sh (constantly {:exit 1 :err "fixture failure"})]
    (let [failure (try
                    (ensure-dependencies!)
                    nil
                    (catch clojure.lang.ExceptionInfo e e))]
      (is (some? failure))
      (is (= {:error "fixture failure"} (ex-data failure))))))

(deftest test-existing-vite-server-is-used-and-shutdown-failures-are-contained
  (let [hook (atom nil)
        browser-options (atom nil)
        handler (fn [_] :response)
        original-handler (Thread/getDefaultUncaughtExceptionHandler)
        original-edt-handler (System/getProperty "sun.awt.exception.handler")]
    (try
      (with-redefs [ensure-dependencies! (constantly nil)
                    start-vite-server (fn [] (throw (ex-info "unexpected Vite launch" {})))
                    chromium/start-chromium! (fn [options]
                                               (reset! browser-options options)
                                               {:close #(throw (ex-info "fixture close error" {}))})
                    utils/suppress-stderr-logging! (constantly nil)
                    utils/add-shutdown-hook! #(reset! hook %)]
        (start-app! {:vite-port 8123 :handler handler})
        (is (= {:vite-port 8123 :handler handler} @browser-options))
        (is (fn? @hook))
        (is (re-find #"Error during shutdown: fixture close error"
                     (with-out-str (@hook)))))
      (finally
        (Thread/setDefaultUncaughtExceptionHandler original-handler)
        (if original-edt-handler
          (System/setProperty "sun.awt.exception.handler" original-edt-handler)
          (System/clearProperty "sun.awt.exception.handler"))))))

(deftest shutdown-stops-a-frontend-process-that-does-not-exit-gracefully
  (let [events (atom [])
        hook (atom nil)
        original-handler (Thread/getDefaultUncaughtExceptionHandler)
        original-edt-handler (System/getProperty "sun.awt.exception.handler")
        fixture (proxy [Process] []
                  (destroy [] (swap! events conj :terminate))
                  (isAlive [] true)
                  (destroyForcibly [] (swap! events conj :force) nil))]
    (try
      (with-redefs [ensure-dependencies! (constantly nil)
                    start-vite-server (constantly {:port 8123 :process fixture})
                    chromium/start-chromium! (constantly {:close #(swap! events conj :browser-close)})
                    utils/suppress-stderr-logging! (constantly nil)
                    utils/add-shutdown-hook! #(reset! hook %)]
        (start-app! {})
        (when (is (fn? @hook))
          (@hook)
          (is (= [:terminate :force :browser-close] @events))))
      (finally
        (Thread/setDefaultUncaughtExceptionHandler original-handler)
        (if original-edt-handler
          (System/setProperty "sun.awt.exception.handler" original-edt-handler)
          (System/clearProperty "sun.awt.exception.handler"))))))
