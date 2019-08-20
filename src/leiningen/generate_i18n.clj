(ns leiningen.generate-i18n
  (:require [leiningen.core
             [eval :as lein]
             [project :as project]]))


(defn- plugin-dep-vector
  "Get the version of this plugin."
  [{:keys [plugins]}]
  (some
   (fn [[plugin-symb :as dep-vector]]
     (when (= plugin-symb 'clojure.i18n/clojure.i18n)
       dep-vector))
   plugins))

(defn- i18n-decs-profile
  "Lein profile that includes a dependency for `clojure.i18n`. Used for merging with the project we're checking's
  profile, so `clojure.i18n.reader` will be available when evaluating in the project itself."
  [project]
  {:dependencies [(plugin-dep-vector project)]})

(defn generate-i18n
  "TODO"
  [{options :clojure.i18n, :keys [source-paths], :as project}]
  (let [options (merge {:source-paths source-paths} options)
        start-time-ms (System/currentTimeMillis)
        project       (project/merge-profiles project [(i18n-decs-profile project)])]
    (println "Generating POT file with options:" options)
    (lein/eval-in-project
     project
     `(clojure.i18n.reader/print-i18n-in-source-paths '~options)
     '(require 'clojure.i18n.reader))
    (println (format "Done in %0.1f seconds." (/ (- (System/currentTimeMillis) start-time-ms) 1000.0)))))
