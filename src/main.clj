(ns main)

;; State for our counter
(def counter-state (atom 0))

;; Function to handle counter messages
(defn handle-counter-message [message]
  (case (get-in message [:data :type])
    "increment" (do
                  (swap! counter-state inc)
                  {:result @counter-state})
    "decrement" (do
                  (swap! counter-state dec)
                  {:result @counter-state})
    "get-value" {:result @counter-state}
    ;; Default case
    {:error true :message "Unknown message type"}))

(defn handle-sync-request [message]
  ;; Implement your synchronous request handling here
  ;; Example:
  (case (:action message)
    "counter" (handle-counter-message message) 
    {:error "Unknown request type"}))