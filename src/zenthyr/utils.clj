(ns zenthyr.utils
  (:import [java.io PrintStream FilterOutputStream]))

(defn find-available-port
  [port]
  (try
    (let [socket (java.net.ServerSocket. port)]
      (.close socket)
      port)
    (catch java.net.BindException _
      (find-available-port (inc port)))))

(defn add-shutdown-hook!
  [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(defn suppress-stderr-logging! []
  (let [original-err System/err
        buffer (java.io.ByteArrayOutputStream.)
        
        process-buffer!
        (fn []
          (let [s (.toString buffer "UTF-8")]
            (when (.contains s "\n")
              (let [lines (clojure.string/split s #"\n" -1)]
                ;; Reset buffer
                (.reset buffer)
                ;; Process all complete lines (all except the last element which is the remainder)
                (dotimes [i (dec (count lines))]
                  (let [line (nth lines i)]
                    (when-not (or (.contains line "AppKit Thread")
                                  (.contains line "process_requirement")
                                  (.contains line "Signature validation")
                                  (.contains line "DEPRECATED_ENDPOINT")
                                  (.contains line "NSOSStatusErrorDomain"))
                      (.write original-err (.getBytes (str line "\n"))))))
                ;; Put back the last partial segment
                (let [last-segment (last lines)]
                  (when (not-empty last-segment)
                    (.write buffer (.getBytes last-segment))))))))
        
        filter-stream 
        (proxy [PrintStream] [original-err]
          (write 
            ([b-or-buf]
             (if (integer? b-or-buf)
               (do
                 (.write buffer (int b-or-buf))
                 (process-buffer!))
               (do
                 (.write buffer ^bytes b-or-buf 0 (alength ^bytes b-or-buf))
                 (process-buffer!))))
            ([buf off len]
             (.write buffer ^bytes buf off len)
             (process-buffer!))))]
    (System/setErr filter-stream)))
