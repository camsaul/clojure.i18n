(ns clojure.i18n.reader
  (:require [clojure.i18n.core :as core]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.namespace.find :as ns.find])
  (:import java.io.File))

(def ^:dynamic *output-format* :pot)
(def ^:dynamic *output-writer* nil)

(defmulti print-localized-string
  {:arglists '([locale-type message args metadata])}
  (fn [& _] *output-format*))

(defn- trim-and-escape [message]
  (-> message
      (str/escape {\" "\\\""})
      (str/replace #"\s+" " ")
      str/trim))

(defmethod print-localized-string :pot
  [_ message _ {:keys [ns line], remark :i18n/remark, :as metadata}]
  (binding [*out* (if *output-writer* *output-writer* *out*)]
    (printf "#: %s:%s\n" ns line)
    (when remark
      (printf "#: %s\n" remark))
    ;; TODO - should we print any other keys in metadata?
    (printf "msgid \"%s\"\n" (trim-and-escape message))
    (printf "msgstr \"\"\n\n")))

(def print-i18n-redefs
  (atom
   {#'core/localized-string-impl print-localized-string}))

;; TODO - could this be sped up by parallelizing stuff?
(defn print-i18n-in-source-paths
  {:style/indent 0}
  [{:keys [source-paths output-file output-format], :as options}]
  (cond
    output-format
    (binding [*output-format* output-format]
      (print-i18n-in-source-paths (dissoc options :output-format)))

    output-file
    (with-open [writer (io/writer (io/file output-file))]
      (binding [*output-writer* writer]
        (print-i18n-in-source-paths (dissoc options :output-file))))

    :else
    (with-redefs-fn @print-i18n-redefs
      (fn []
        (doseq [source-path       source-paths
                :let              [source-dir (io/file source-path)]
                :when             (.isDirectory source-dir)
                ^File source-file (ns.find/find-sources-in-dir source-dir)
                :let              [filename (.getCanonicalPath source-file)]]
          (println (format "Generating i18n for file %s..." filename))
          (load-file filename))
        (flush)))))
