(ns zenthyr.chromium-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [zenthyr.chromium])
  (:import [org.cef.callback CefQueryCallback]))

(defn- exercise-request [handler request]
  (let [calls (atom [])
        callback (reify CefQueryCallback
                   (success [_ output] (swap! calls conj [:success (json/parse-string output true)]))
                   (failure [_ status message] (swap! calls conj [:failure status message])))
        router (#'zenthyr.chromium/message-handler handler)]
    (is (.onQuery router nil nil 42 request false callback))
    @calls))

(deftest dispatches-the-decoded-browser-message
  (is (= [[:success {:operation "fixture" :processed true}]]
         (exercise-request #(assoc % :processed true) "{\"operation\":\"fixture\"}"))))

(deftest reports-decode-and-handler-errors
  (let [calls (exercise-request (fn [_] (throw (ex-info "fixture failure" {}))) "{}")]
    (is (= [[:failure 500 "fixture failure"]] calls)))
  (let [called? (atom false)
        [[event status _]] (exercise-request (fn [_] (reset! called? true)) "[")]
    (is (= [:failure 500] [event status]))
    (is (false? @called?))))
