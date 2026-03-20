(ns zenthyr.chromium
  (:require [cheshire.core :as json])
  (:import [me.friwi.jcefmaven CefAppBuilder MavenCefAppHandlerAdapter]
           [org.cef CefApp CefClient CefSettings]
           [org.cef.browser CefBrowser CefMessageRouter CefFrame]
           [org.cef.callback CefQueryCallback]
           [org.cef.handler CefMessageRouterHandlerAdapter CefDisplayHandlerAdapter]
           [javax.swing JFrame WindowConstants SwingUtilities]
           [java.awt BorderLayout Component Desktop]
           [java.awt.desktop AppReopenedListener QuitHandler]
           [java.awt.event WindowAdapter WindowEvent]))

(defn create-message-router [handler]
  (let [router (CefMessageRouter/create)]
    (.addHandler router
                 (proxy [CefMessageRouterHandlerAdapter] []
                   (onQuery [^CefBrowser browser ^CefFrame frame query-id ^String request persistent ^CefQueryCallback callback]
                     (try
                       (let [message (json/parse-string request true)
                             response (handler message)]
                         (.success callback (json/generate-string response))
                         true)
                       (catch Exception e
                         (.failure callback 500 (.getMessage e))
                         true))))
                 true)
    router))

(defn start-chromium!
  [{:keys [vite-port handler]
    :or {vite-port 5173}}]
  (let [builder (CefAppBuilder.)
        _ (.setInstallDir builder (java.io.File. "jcef-bundle"))
        _ (.setProgressHandler builder (me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler.))
        
        ;; Configure CEF args to suppress errors/warnings
        _ (.addJcefArgs builder (into-array String ["--no-sandbox"]))
        _ (.addJcefArgs builder (into-array String ["--disable-gpu-shader-disk-cache"]))
        _ (.addJcefArgs builder (into-array String ["--use-mock-keychain"]))
        _ (.addJcefArgs builder (into-array String ["--disable-features=CalculateNativeWinOcclusion"]))
        
        ;; Additional flags to suppress signature validation noise
        _ (.addJcefArgs builder (into-array String ["--disable-gpu-sandbox"]))
        _ (.addJcefArgs builder (into-array String ["--disable-software-rasterizer"]))
        _ (.addJcefArgs builder (into-array String ["--disable-dev-shm-usage"]))
        
        ;; Configure cache path
        cef-settings (.getCefSettings builder)
        _ (set! (.root_cache_path cef-settings) (.getAbsolutePath (java.io.File. "jcef-cache")))
        _ (set! (.log_severity cef-settings) org.cef.CefSettings$LogSeverity/LOGSEVERITY_DISABLE)

        app (.build builder)
        client (.createClient app)
        router (create-message-router handler)
        _ (.addMessageRouter client router)
        url (str "http://localhost:" vite-port)
        browser (.createBrowser client url false false)
        ui-comp (.getUIComponent browser)]
    
    ;; Setup the window
    (SwingUtilities/invokeLater
     (fn []
       ;; Set uncaught exception handler for the EDT to suppress AppKit errors
       (.setUncaughtExceptionHandler 
        (Thread/currentThread)
        (reify java.lang.Thread$UncaughtExceptionHandler
          (uncaughtException [_ thread e]
            (let [msg (str (.getMessage e) " " (str e))]
              (when-not (or (.contains msg "AppKit Thread")
                            (.contains msg "process_requirement")
                            (.contains msg "Signature validation"))
                (.printStackTrace e))))))

       (let [frame (JFrame. "Zenthyr")]
         (.setSize frame 800 600)
         (.setLayout frame (BorderLayout.))
         (.add frame ui-comp BorderLayout/CENTER)
         (.setDefaultCloseOperation frame WindowConstants/DO_NOTHING_ON_CLOSE)
         
         ;; Inject the bridge script when the browser loads
         (.addDisplayHandler client 
                             (proxy [CefDisplayHandlerAdapter] []
                               (onAddressChange [^CefBrowser browser ^CefFrame frame ^String url]
                                 (let [script "
                                    if (!window.zenthyr) {
                                      window.zenthyr = {
                                        invoke: function(message) {
                                          return new Promise(function(resolve, reject) {
                                            window.cefQuery({
                                              request: JSON.stringify(message),
                                              onSuccess: function(response) {
                                                resolve({data: JSON.parse(response)});
                                              },
                                              onFailure: function(code, msg) {
                                                reject(new Error(msg));
                                              }
                                            });
                                          });
                                        },
                                        emit: function(message) {
                                           window.cefQuery({
                                              request: JSON.stringify(Object.assign({type: 'emit'}, message)),
                                              onSuccess: function() {},
                                              onFailure: function() {}
                                            });
                                        }
                                      };
                                      console.log('Zenthyr bridge injected');
                                    }
                                 "]
                                   (.executeJavaScript browser script url 0)))))
         
         (.addWindowListener frame
                              (proxy [WindowAdapter] []
                                (windowClosing [e]
                                  (try
                                    (.setVisible frame false)
                                    (catch Exception ex
                                      (println "Error hiding window:" (.getMessage ex))
                                      (.dispose frame))))))

         ;; Handle Dock icon click to reopen window
         (when (Desktop/isDesktopSupported)
           (let [desktop (Desktop/getDesktop)]
             ;; Handle re-opening from dock (macOS)
             (try
               (when (.isSupported desktop java.awt.Desktop$Action/APP_EVENT_FOREGROUND)
                 (.addAppEventListener desktop
                                       (reify AppReopenedListener
                                         (appReopened [this e]
                                           (SwingUtilities/invokeLater #(.setVisible frame true))))))
               (catch Exception e 
                 (println "Warning: Could not register AppReopenedListener:" (.getMessage e))))
             
             ;; Handle Cmd+Q
             (try
               (when (.isSupported desktop java.awt.Desktop$Action/APP_QUIT_HANDLER)
                 (.setQuitHandler desktop
                                  (reify QuitHandler
                                    (handleQuitRequestWith [this e response]
                                      (.performQuit response)
                                      (System/exit 0)))))
               (catch Exception e
                 (println "Warning: Could not register QuitHandler:" (.getMessage e))))))
         
         (.setVisible frame true))))
    
    {:app app
     :browser browser
     :close (fn [] 
              (SwingUtilities/invokeLater 
               (fn [] (.dispose app))))}))
