(ns zenthyr.native-chromium-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [zenthyr.chromium :as chromium])
  (:import [com.sun.net.httpserver HttpServer HttpHandler HttpExchange]
           [java.awt.event WindowEvent]
           [java.net InetSocketAddress]
           [java.nio.charset StandardCharsets]
           [javax.swing JFrame SwingUtilities]))

(def fixture-page
  "<!doctype html><title>Bridge fixture</title>
  <script>
    const timer = setInterval(() => {
      if (window.zenthyr) {
        clearInterval(timer);
        window.zenthyr.invoke({operation: 'ping'})
          .then(result => fetch('/result', {method: 'POST', body: JSON.stringify(result.data)}))
          .catch(error => fetch('/result', {method: 'POST', body: JSON.stringify({error: error.message})}));
      }
    }, 30);
  </script>")

(deftest native-browser-routes-a-message-from-local-javascript
  (when (= "1" (System/getenv "ZENTHYR_NATIVE_TESTS"))
    (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)
          result (promise)]
      (.createContext server "/" (reify HttpHandler
        (^void handle [_ ^HttpExchange exchange]
          (let [content (.getBytes fixture-page StandardCharsets/UTF_8)]
            (.set (.getResponseHeaders exchange) "Content-Type" "text/html; charset=utf-8")
            (.sendResponseHeaders exchange 200 (alength content))
            (with-open [body (.getResponseBody exchange)]
              (.write body content))))))
      (.createContext server "/result" (reify HttpHandler
        (^void handle [_ ^HttpExchange exchange]
          (with-open [body (.getRequestBody exchange)]
            (deliver result (json/parse-string (slurp body) true)))
          (.sendResponseHeaders exchange 204 -1)
          (.close exchange))))
      (.start server)
      (try
        (let [{:keys [close]} (chromium/start-chromium!
                               {:vite-port (.getPort (.getAddress server))
                                :handler (fn [message] {:reply (:operation message)})})]
          (try
            (is (= {:reply "ping"} (deref result 20000 :timeout)))
            (SwingUtilities/invokeAndWait
              (fn []
                (let [frame (first (filter #(and (instance? JFrame %)
                                                (= "Zenthyr" (.getTitle ^JFrame %)))
                                           (JFrame/getFrames)))]
                  (is (some? frame))
                  (when frame
                    (is (.isVisible frame))
                    (.dispatchEvent frame (WindowEvent. frame WindowEvent/WINDOW_CLOSING))
                    (is (not (.isVisible frame)))))))
            (finally
              (close)
              (SwingUtilities/invokeAndWait
                #(doseq [frame (JFrame/getFrames)]
                   (.dispose frame))))))
        (finally
          (.stop server 0))))))
