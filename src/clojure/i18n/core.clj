(ns clojure.i18n.core
  (:refer-clojure :exclude [format])
  (:require [clojure.i18n
             [locale :as locale]
             [resources :as resources]]
            [clojure.tools.logging :as log]
            [potemkin.types :as p.types]
            [pretty.core :refer [PrettyPrintable]])
  (:import java.text.MessageFormat))

(defmacro defpackage [package-name ns-pattern]
  `(resources/add-package-pattern! ~ns-pattern ~package-name))

(defn source-address-header [] :wow)

(defn- x [m]
  )

(defn- source-address
  "The `public-settings/source-address-header` header's value, or the `(:remote-addr request)` if not set."
  [{:keys [headers remote-addr]}]
  (or (some->> (source-address-header) (get headers))
      remote-addr))

(defn- source-address
  "The `public-settings/source-address-header` header's value, or the `(:remote-addr request)` if not set."
  [{{source-addr (source-address-header)} :headers, :keys [remote-addr]}]
  (or source-addr remote-addr))



;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                Localized String                                                |
;;; +----------------------------------------------------------------------------------------------------------------+

(defn format
  (^String [_ translated-string]
   translated-string)

  (^String [locale, ^String format-string & args]
   (.format (MessageFormat. format-string (locale/->Locale locale)) (to-array args))))

(defn translate
  "Translate a message into the given locale, interpolating as
  needed. Messages are looked up in the resource bundle associated with the
  given namespace"
  ^String [locale namespace-or-symb message & args]
  (try
    (let [translated-string (or (resources/lookup namespace-or-symb locale message)
                                   message)]
      (apply format locale translated-string args))
    (catch IllegalArgumentException e
      ;; Not translating this string to prevent an unfortunate stack overflow. If this string happened to be the one
      ;; that had the typo, we'd just recur endlessly without logging an error.
      (log/errorf e "Unable to translate string '%s'" message)
      message)))

(p.types/definterface+ Translatable
  (translate-with-locale ^String [this locale]))

(p.types/deftype+ LocalizedString [locale-type ^String message args metadata]
  PrettyPrintable
  (pretty [_]
    (list 'system-localized-string message args metadata))

  clojure.lang.IObj
  (meta [_] metadata)
  (withMeta [_ new-meta]
    (LocalizedString. locale-type message args new-meta))

  clojure.lang.Named
  (getNamespace [_]
    (name (ns-name (:ns metadata))))

  CharSequence
  (charAt [this index]
    (.charAt (translate-with-locale this (locale/locale locale-type)) index))
  (chars [this]
    (.chars (translate-with-locale this (locale/locale locale-type))))
  (codePoints [this]
    (.codePoints (translate-with-locale this (locale/locale locale-type))))
  (length [this]
    (.length (translate-with-locale this (locale/locale locale-type))))
  (subSequence [this start end]
    (.subSequence (translate-with-locale this (locale/locale locale-type)) start end))
  (toString [this]
    (translate-with-locale this (locale/locale locale-type)))

  Translatable
  (translate-with-locale [_ locale]
    (apply translate locale (:ns metadata) message args)))

(defn localized-string [locale-type message args metadata]
  (LocalizedString. locale-type message args metadata))

(defn localized-string?
  "Returns `true` if `x` is a system or user localized string instance"
  [x]
  (instance? Translatable x))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                 Helper Macros                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+

(set! *warn-on-reflection* true) ; NOCOMMIT

(defn localized-string-impl [locale-type msg args metadata]
  `(localized-string ~locale-type ~msg ~args ~metadata)
  )

(defmacro localized-string* [locale-type msg args metadata]
  (localized-string-impl locale-type msg args metadata))

(defmacro deferred-tru [msg & args]
  `(localized-string* :user ~(eval msg) ~(vec args) ~(assoc (meta &form) :ns *ns*)))

(defmacro deferred-trs [msg & args]
  `(localized-string* :system ~(eval msg) ~(vec args) ~(assoc (meta &form) :ns *ns*)))

(def ^String str*
  "Ensures that `trs`/`tru` isn't called prematurely, during compilation."
  (if *compile-files*
    (fn [& _]
      (throw (Exception. "Premature i18n string lookup. Is there a top-level call to `trs` or `tru`?")))
    str))

(defmacro tru
  "Applies `str` to `deferred-tru`'s expansion.
  Prefer this over `deferred-tru`. Use `deferred-tru` only in code executed at compile time, or where `str` is manually
  applied to the result."
  [msg & args]
  `(str* ~(with-meta `(deferred-tru ~msg ~@args) (meta &form))))

(defmacro trs
  "Applies `str` to `deferred-trs`'s expansion.
  Prefer this over `deferred-trs`. Use `deferred-trs` only in code executed at compile time, or where `str` is manually
  applied to the result."
  [msg & args]
  `(str* ~(with-meta `(deferred-trs ~msg ~@args) (meta &form))))
