(ns zenthyr.utils)

(defn find-available-port
  [port]
  (try
    (let [socket (java.net.ServerSocket. port)]
      (.close socket)
      port)
    (catch java.net.BindException _
      (find-available-port (inc port)))))