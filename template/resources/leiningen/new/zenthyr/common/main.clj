(ns {{name}}.main
  (:require [zenthyr.core :as zenthyr])
  (:gen-class))

(defn handler [message]
  (println "Received message from frontend:" message)
  (case (:type (:data message))
    "increment" {:result "incremented"}
    "decrement" {:result "decremented"}
    {:error "Unknown message type"}))

(defn -main
  "Starts the application."
  [& args]
  (zenthyr/start-app! {:handler handler}))
