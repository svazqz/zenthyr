(ns leiningen.new.zenthyr
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]
            [clojure.java.shell :refer [sh]]))

(def render (renderer "zenthyr"))

(defn zenthyr
  "Creates a new Zenthyr desktop application.
   Supported options: +vue, +svelte, +angular (default: react)"
  [name & args]
  (let [data {:name name
              :sanitized (name-to-path name)}
        args-set (set args)
        framework (cond
                    (contains? args-set "+vue") "vue"
                    (contains? args-set "+svelte") "svelte"
                    (contains? args-set "+angular") "angular"
                    (contains? args-set "+react") "react"
                    :else "react")
        common-files [["project.clj" (render "common/project.clj" data)]
                      ["src/{{sanitized}}/main.clj" (render "common/main.clj" data)]
                      [".gitignore" (render "common/gitignore" data)]
                      [".editorconfig" (render "common/editorconfig" data)]
                      [".prettierrc" (render "common/prettierrc" data)]]
        framework-files (case framework
                          "react" [["src/app/src/App.tsx" (render "react/app/src/App.tsx" data)]
                                   ["src/app/src/main.tsx" (render "react/app/src/main.tsx" data)]
                                   ["src/app/src/index.css" (render "react/app/src/index.css" data)]
                                   ["src/app/index.html" (render "react/app/index.html" data)]
                                   ["package.json" (render "react/package.json" data)]
                                   ["vite.config.ts" (render "react/vite.config.ts" data)]
                                   ["tsconfig.json" (render "react/tsconfig.json" data)]
                                   ["tsconfig.node.json" (render "react/tsconfig.node.json" data)]
                                   [".eslintrc.js" (render "react/eslintrc.js" data)]]
                          "vue" [["src/app/src/App.vue" (render "vue/app/src/App.vue" data)]
                                 ["src/app/src/main.ts" (render "vue/app/src/main.ts" data)]
                                 ["src/app/index.html" (render "vue/app/index.html" data)]
                                 ["package.json" (render "vue/package.json" data)]
                                 ["vite.config.ts" (render "vue/vite.config.ts" data)]
                                 ["tsconfig.json" (render "vue/tsconfig.json" data)]
                                 ["tsconfig.node.json" (render "vue/tsconfig.node.json" data)]]
                          "svelte" [["src/app/src/App.svelte" (render "svelte/app/src/App.svelte" data)]
                                    ["src/app/src/main.ts" (render "svelte/app/src/main.ts" data)]
                                    ["src/app/src/app.css" (render "svelte/app/src/app.css" data)]
                                    ["src/app/index.html" (render "svelte/app/index.html" data)]
                                    ["package.json" (render "svelte/package.json" data)]
                                    ["vite.config.ts" (render "svelte/vite.config.ts" data)]
                                    ["tsconfig.json" (render "svelte/tsconfig.json" data)]
                                    ["tsconfig.node.json" (render "svelte/tsconfig.node.json" data)]]
                          "angular" [["src/app/src/app.component.ts" (render "angular/app/src/app.component.ts" data)]
                                     ["src/app/src/app.config.ts" (render "angular/app/src/app.config.ts" data)]
                                     ["src/app/src/main.ts" (render "angular/app/src/main.ts" data)]
                                     ["src/app/index.html" (render "angular/app/index.html" data)]
                                     ["package.json" (render "angular/package.json" data)]
                                     ["vite.config.ts" (render "angular/vite.config.ts" data)]
                                     ["tsconfig.json" (render "angular/tsconfig.json" data)]])]
    (main/info (str "Generating fresh Zenthyr project with " framework " framework."))
    (apply ->files data (concat common-files framework-files))
    (main/info "Initializing git repository.")
    (sh "git" "init" :dir name)))
