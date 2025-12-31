(ns zenthyr.core
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [zenthyr.chromium :as chromium]
   [zenthyr.utils :as utils]
   [main :as main])
  (:gen-class))

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
        vite-port (utils/find-available-port 5173)]
    (future (try
              (println "Starting Vite development server on port" vite-port "...")
              (let [vite-process-builder (ProcessBuilder.
                                          (into-array String ["npx" "vite" "src/app" "--port" (str vite-port)]))
                    _ (.directory vite-process-builder (.getCanonicalFile project-root))
                    _ (.redirectErrorStream vite-process-builder true)
                    vite-process (.start vite-process-builder)
                    vite-input-stream (.getInputStream vite-process)
                    vite-reader (java.io.BufferedReader. (java.io.InputStreamReader. vite-input-stream))]
                (try
                  (loop []
                    (let [line (.readLine vite-reader)]
                      (when line
                        (println "Vite:" line)
                        (recur))))
                  (catch Exception e1
                    (println "Error reading Vite output:" (.getMessage e1))))
                vite-process)
              (catch Exception e2
                (println "Error starting Vite server:" (.getMessage e2)))))
    vite-port))      

(defn -main
  "Main entry point for the zenthyr application"
  [& args]
  (println "Starting zenthyr application...")
  (ensure-dependencies!) 
  (let [vite-port (start-vite-server)
        app (chromium/start-chromium!
             {:vite-port vite-port})]
    ;; Keep the JVM running
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (when-let [close-fn (:close app)]
                                   (try
                                     (close-fn)
                                     (catch Exception e
                                       (println "Error during shutdown:" (.getMessage e)))))
                                 (println "Shutting down zenthyr application..."))))
    (println "zenthyr application started. Press Ctrl+C to exit.")))
