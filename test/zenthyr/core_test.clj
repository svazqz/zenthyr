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

(deftest test-start-app!
  (testing "Orchestrates startup"
    (let [handler (fn [_] {:ok true})
          ensure-deps-called (atom false)
          start-vite-called (atom false)
          start-chromium-called (atom false)
          shutdown-hook-added (atom false)]
      (with-redefs [ensure-dependencies! (fn [] (reset! ensure-deps-called true))
                    start-vite-server (fn []
                                        (reset! start-vite-called true)
                                        {:port 8080 :process nil})
                    chromium/start-chromium! (fn [opts]
                                               (reset! start-chromium-called true)
                                               (is (= 8080 (:vite-port opts)))
                                               (is (fn? (:handler opts)))
                                               {:close (fn [] :closed)})
                    utils/suppress-stderr-logging! (fn [])
                    utils/add-shutdown-hook! (fn [_]
                                               (reset! shutdown-hook-added true))]
        (start-app! {:handler handler})
        (is @ensure-deps-called)
        (is @start-vite-called)
        (is @start-chromium-called)
        (is @shutdown-hook-added)))))

(deftest test-start-app!-with-custom-handler
  (testing "Uses provided handler"
    (let [handler (fn [_] {:ok true})
          ensure-deps-called (atom false)
          start-vite-called (atom false)
          start-chromium-called (atom false)
          shutdown-hook-added (atom false)]
      (with-redefs [ensure-dependencies! (fn [] (reset! ensure-deps-called true))
                    start-vite-server (fn []
                                        (reset! start-vite-called true)
                                        {:port 8080 :process nil})
                    chromium/start-chromium! (fn [opts]
                                               (reset! start-chromium-called true)
                                               (is (= 8080 (:vite-port opts)))
                                               (is (fn? (:handler opts)))
                                               {:close (fn [] :closed)})
                    utils/suppress-stderr-logging! (fn [])
                    utils/add-shutdown-hook! (fn [_]
                                               (reset! shutdown-hook-added true))]
        (start-app! {:handler handler})
        (is @ensure-deps-called)
        (is @start-vite-called)
        (is @start-chromium-called)
        (is @shutdown-hook-added)))))
