(ns zenthyr.edt-handler
  (:gen-class
   :name zenthyr.edt_handler.EDTExceptionHandler
   :implements [java.lang.Thread$UncaughtExceptionHandler]
   :methods [[handle [Throwable] void]]))

(defn -handle
  [this ^Throwable t]
  (let [msg (str (.getMessage t) " " (str t))]
    (when-not (or (.contains msg "AppKit Thread")
                  (.contains msg "process_requirement")
                  (.contains msg "Signature validation"))
      (.printStackTrace t))))

(defn -uncaughtException
  [this thread t]
  (-handle this t))
