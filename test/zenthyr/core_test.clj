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
  (testing "Starts vite server on available port"
    (let [port (atom nil)]
      (with-redefs [utils/find-available-port (fn [p] 
                                                (reset! port p) 
                                                12345)
                    clojure.core/future-call (fn [f] nil)]
        (is (= 12345 (start-vite-server)))
        (is (= 5173 @port))))))

(deftest test-main-flow
  (testing "Main entry point orchestrates startup"
    (let [ensure-deps-called (atom false)
          start-vite-called (atom false)
          start-chromium-called (atom false)
          shutdown-hook-added (atom false)]
      (with-redefs [ensure-dependencies! (fn [] (reset! ensure-deps-called true))
                    start-vite-server (fn [] (reset! start-vite-called true) 8080)
                    chromium/start-chromium! (fn [opts] 
                                               (reset! start-chromium-called true)
                                               (is (= 8080 (:vite-port opts)))
                                               {:close (fn [] :closed)})
                    utils/suppress-stderr-logging! (fn [])
                    utils/add-shutdown-hook! (fn [f]
                                               (reset! shutdown-hook-added true))]
        (-main)
        (is @ensure-deps-called)
        (is @start-vite-called)
        (is @start-chromium-called)
        (is @shutdown-hook-added)))))
