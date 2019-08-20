(ns clojure.i18n.resources
  (:require [clojure.i18n.locale :as locale])
  (:import clojure.lang.Namespace
           gnu.gettext.GettextResource
           [java.util MissingResourceException ResourceBundle]))

(def ns-pattern->package-name (atom {}))

(defn add-package-pattern! [ns-pattern package-name]
  (swap! ns-pattern->package-name assoc ns-pattern package-name))

(defn namespace->package-name ^String [^Namespace namespac]
  (or (:i18n/package (meta namespace))
      (let [ns-str (name (ns-name namespac))]
        (some
         (fn [[pattern package-name]]
           (when (re-matches pattern ns-str)
             package-name))
         @ns-pattern->package-name))))

(defn package-name->bundle-name ^String [package-name]
  (format "%s.Messages" (munge package-name)))

(defn namespace->bundle-name ^String [^Namespace namespac]
  (some->> namespac namespace->package-name (get package-name->bundle-name)))

;; TODO - we should probably cache this!
(defn ^ResourceBundle bundle
  [^String bundle-name, locale]
  (when bundle-name
    (try
      (GettextResource/getBundle bundle-name (locale/->Locale locale))
      ;; base-name or loc were nil
      (catch NullPointerException _
        nil)
      ;; no bundle for the base-name and/or locale
      (catch MissingResourceException _
        nil))))

(defn namespace->bundle ^ResourceBundle [^Namespace namespac locale]
  (when-let [bundle-name (namespace->bundle-name namespac)]
    (bundle bundle-name locale)))

(defn lookup
  ^String [^Namespace namespac, locale, ^String message]
  (some-> (namespace->bundle namespac locale)
          (GettextResource/gettext message)))
