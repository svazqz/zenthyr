(ns zenthyr.chromium
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [zenthyr.ipc :refer [send-to-clients]]))

(defonce callbacks (atom {}))

;; Start the Chromium window
(defn start-chromium!
  ([] (start-chromium! {}))
  ([{:keys [vite-port ipc-port]
     :or {vite-port 5173
          ipc-port 8080}}]
   (let [vite-url (str "http://localhost:" vite-port)
         project-root (io/file ".")
         app-dir (io/file "src/app")
         main-js (.getAbsolutePath (io/file project-root "src/zenthyr/main.js"))
         process-builder (ProcessBuilder.
                          (into-array ["npx"
                                       "--no"
                                       "electron"
                                       "--"
                                       main-js
                                       (str ipc-port)
                                       (str vite-port)
                                       vite-url
                                       (str 800) (str 600)]))
         _ (.directory process-builder (.getCanonicalFile project-root))
         _ (.redirectErrorStream process-builder true)
         _ (println "Starting Electron with command: npx --no electron --" main-js vite-port vite-url 800 600)
         process (.start process-builder)
         input-stream (.getInputStream process)
         reader (java.io.BufferedReader. (java.io.InputStreamReader. input-stream))]
     (println "Electron process started with PID:" (.pid process))
     ;; Start a thread to read and print the process output
     (future
       (try
         (loop []
           (let [line (.readLine reader)]
             (when line
               (println "Electron (Main thread):" line)
               (recur))))
         (catch Exception e
           (println "Error reading Electron output:" (.getMessage e)))))
     
     ;; Return a map with process information and control functions
     {:process process
      :execute-js (fn [code callback]
                    (let [id (str (java.util.UUID/randomUUID))]
                      (swap! callbacks assoc id callback)
                      (send-to-clients (json/generate-string
                                        {:type "execute-js"
                                         :code code
                                         :id id}))))
      :close (fn []
               (println "Closing Electron window...")
               (.destroy process))})))

