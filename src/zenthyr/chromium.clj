(ns zenthyr.chromium
  (:require [cheshire.core :as json]
            [main :as main])
  (:import [me.friwi.jcefmaven CefAppBuilder MavenCefAppHandlerAdapter]
           [org.cef CefApp CefClient CefSettings]
           [org.cef.browser CefBrowser CefMessageRouter CefFrame]
           [org.cef.callback CefQueryCallback]
           [org.cef.handler CefMessageRouterHandlerAdapter CefDisplayHandlerAdapter]
           [javax.swing JFrame WindowConstants SwingUtilities]
           [java.awt BorderLayout Component]
           [java.awt.event WindowAdapter WindowEvent]))

(defn create-message-router []
  (let [router (CefMessageRouter/create)]
    (.addHandler router
                 (proxy [CefMessageRouterHandlerAdapter] []
                   (onQuery [^CefBrowser browser ^CefFrame frame query-id ^String request persistent ^CefQueryCallback callback]
                     (try
                       (let [message (json/parse-string request true)
                             response (main/handle-sync-request message)]
                         (.success callback (json/generate-string response))
                         true)
                       (catch Exception e
                         (.failure callback 500 (.getMessage e))
                         true))))
                 true)
    router))

(defn start-chromium!
  [{:keys [vite-port]
    :or {vite-port 5173}}]
  (let [builder (CefAppBuilder.)
        _ (.setInstallDir builder (java.io.File. "jcef-bundle"))
        _ (.setProgressHandler builder (me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler.))
        
        ;; Configure cache path
        cef-settings (.getCefSettings builder)
        _ (set! (.root_cache_path cef-settings) (.getAbsolutePath (java.io.File. "jcef-cache")))

        app (.build builder)
        client (.createClient app)
        router (create-message-router)
        _ (.addMessageRouter client router)
        url (str "http://localhost:" vite-port)
        browser (.createBrowser client url false false)
        ui-comp (.getUIComponent browser)]
    
    ;; Setup the window
    (SwingUtilities/invokeLater
     (fn []
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
                                 (.dispose app)
                                 (.dispose frame)
                                 (System/exit 0))))
         
         (.setVisible frame true))))
    
    {:app app
     :browser browser
     :close (fn [] 
              (SwingUtilities/invokeLater 
               (fn [] (.dispose app))))}))
