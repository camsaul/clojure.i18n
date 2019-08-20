(ns clojure.i18n.locale
  (:require [potemkin.types :as p.types])
  (:import java.util.Locale))

(p.types/defprotocol+ CoerceToLocale
  (->Locale ^java.util.Locale [this]
    "Coerce argument to a `java.util.Locale`, if possible. Looks up corresponding Locale for language tag Strings;
    returns instances of Locale as-is."))

(extend-protocol CoerceToLocale
  nil
  (->Locale [this] this)

  Object
  (->Locale [this]
    (throw (IllegalArgumentException. (format "Don't know how to coerce %s to a java.util.Locale." this))))

  String
  (->Locale [this]
    (Locale/forLanguageTag this))

  Locale
  (->Locale [this] this))

(defn locale-for-language-tag ^Locale [^String language-tag]
  (->Locale language-tag))

(defn available-locales []) ; TODO

(defmulti locale
  {:arglists '([locale-type])}
  keyword)

;; TODO - not sure if we need this
(def ^:dynamic ^Locale *system-locale* nil)

(defmethod locale :system
  [_]
  (or *system-locale* (Locale/getDefault)))

(def ^:dynamic ^Locale *user-locale* nil)

(defmethod locale :user
  [_]
  (or *user-locale* (locale :system)))

(defmacro with-user-locale [locale & body]
  `(binding [*user-locale* (->Locale locale)]
     ~@body))

(defn set-system-locale!
  "This sets the local for the instance"
  [locale]
  (Locale/setDefault (->Locale locale)))
