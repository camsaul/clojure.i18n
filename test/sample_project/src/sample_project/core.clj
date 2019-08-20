(ns sample-project.src.sample-project.core
  (:require [clojure.i18n.core :as i18n]))

(i18n/trs "Message 1 {0}" "COOL")

(do
  (i18n/tru "Message 2 {0}" "WOW"))

(i18n/deferred-trs "Message 3 {0}" "COOL")

(def message
  (let [wow? true]
    (i18n/deferred-trs "Message 4 {0}" wow?)))

(def bird-message
  ^{:i18n/remark "This is a bird type that is shown in the bird selection drop-down."} (i18n/trs "Toucan"))

(def a-message-with-double-quotes
  (i18n/tru "Someone wanted to put \"quotes\" in this message."))

(defn f []
  (i18n/tru "Inside a fn!"))


(i18n/trs "Here is an example
of a multiline string. Wow!")

(i18n/trs (str "And here is one that is multi-line "
               "As a result of using a function like (str)."))
