(ns zenthyr.core
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [zenthyr.chromium :as chromium]
   [zenthyr.utils :as utils]
   [zenthyr.edt-handler :as edt]))

(defn ensure-dependencies!
  []
  (let [project-root (io/file ".")
        node-modules (io/file project-root "node_modules")]
    ;; Check if node_modules exists, if not, install dependencies
    (when-not (.exists node-modules)
      (println "Installing Node.js dependencies...")
      (let [result (sh "npm" "install" :dir (.getPath project-root))]
        (if (zero? (:exit result))
          (println "Dependencies installed successfully")
          (do
            (println "Failed to install dependencies:" (:err result))
            (throw (ex-info "Failed to install Node.js dependencies" {:error (:err result)}))))))))

(defn start-vite-server
  []
  (let [project-root (io/file ".")
        vite-port (utils/find-available-port 5173)
        vite-process-builder (ProcessBuilder.
                              (into-array String ["npm" "run" "dev" "--" "--port" (str vite-port)]))
        _ (.directory vite-process-builder (.getCanonicalFile project-root))
        _ (.redirectErrorStream vite-process-builder true)
        vite-process (.start vite-process-builder)]
    
    (future (try
              (let [vite-input-stream (.getInputStream vite-process)
                    vite-reader (java.io.BufferedReader. (java.io.InputStreamReader. vite-input-stream))]
                (try
                  (loop []
                    (let [line (.readLine vite-reader)]
                      (when line
                        (println "Vite:" line)
                        (recur))))
                  (catch Exception e1
                    (println "Error reading Vite output:" (.getMessage e1)))))
              (catch Exception e2
                (println "Error in Vite output reader:" (.getMessage e2)))))
    
    (println "Started Vite server on port" vite-port)
    {:port vite-port :process vite-process}))      

(defn start-app!
  "Starts the zenthyr application.
   opts is a map with keys:
   :handler - Function to handle messages from frontend
   :vite-port - Optional port for vite server"
  [{:keys [handler vite-port] :as opts}]
  (utils/suppress-stderr-logging!)
  ;; Intercept exceptions on the main thread
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread e]
       (let [msg (str (.getMessage e) " " (str e))]
         (when-not (or (.contains msg "AppKit Thread")
                       (.contains msg "process_requirement")
                       (.contains msg "Signature validation"))
           (.printStackTrace e))))))

  ;; Intercept exceptions on the AWT Event Dispatch Thread (EDT)
   ;; This is critical for Swing UI freezes caused by uncaught exceptions
   (System/setProperty "sun.awt.exception.handler" "zenthyr.edt_handler.EDTExceptionHandler")
 
   (println "Starting zenthyr application...")
  (ensure-dependencies!) 
  (let [vite-result (if vite-port
                     {:port vite-port :process nil}
                     (start-vite-server))
        actual-vite-port (:port vite-result)
        vite-process (:process vite-result)
        app (chromium/start-chromium!
             {:vite-port actual-vite-port
              :handler handler})]
    ;; Keep the JVM running
    (utils/add-shutdown-hook! (fn []
                                (when vite-process
                                  (println "Stopping Vite server...")
                                  (.destroy vite-process)
                                  ;; Give it a moment to shut down gracefully
                                  (Thread/sleep 500)
                                  (when (.isAlive vite-process)
                                    (.destroyForcibly vite-process)))
                                (when-let [close-fn (:close app)]
                                  (try
                                    (close-fn)
                                    (catch Exception e
                                      (println "Error during shutdown:" (.getMessage e)))))
                                (println "Shutting down zenthyr application...")))
    (println "zenthyr application started. Press Ctrl+C to exit.")))
