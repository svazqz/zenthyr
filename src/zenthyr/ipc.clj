(ns zenthyr.ipc
  (:require
   [zenthyr.utils :as utils]
   [org.httpkit.server :as server]
   [cheshire.core :as json]
   [main :refer [handle-sync-request]]))

(defonce clients (atom #{}))
(defonce ws-port (atom nil))

(defn send-to-clients [message]
  (doseq [client @clients]
    (server/send! client (json/generate-string message))))

(defn handle-message [message]
  (if (:requestId message)
    ;; Handle synchronous request
    (let [response (try
                     {:type "response"
                      :requestId (:requestId message)
                      :data (handle-sync-request message)}
                     (catch Exception e
                       {:type "error"
                        :requestId (:requestId message)
                        :error (.getMessage e)}))]
      response)
    ;; Handle regular message
    nil))

(defn start-websocket-server
  []
  (let [port (utils/find-available-port 8080)]
    (reset! ws-port port)
    (server/run-server
     (fn [request]
       (if (:websocket? request)
         (server/as-channel request
                            {:on-open (fn [ch]
                                        (swap! clients conj ch)
                                        (println "WebSocket opened"))
                             :on-close (fn [ch status]
                                         (swap! clients disj ch)
                                         (println "WebSocket closed:" status))
                             :on-receive (fn [ch data]
                                           (let [parsed-data (json/parse-string data true)
                                                 response (handle-message parsed-data)]
                                             (when (not (= response nil))
                                               (server/send! ch (json/generate-string response)))))})
         {:status 200
          :headers {"Content-Type" "text/plain"}
          :body "HTTP Server: WebSocket endpoint"}))
     {:port port})
    (println "WebSocket server started on port:" port)
    port))

(defn initialize-ipc
  [app]
  (Thread/sleep 1000))