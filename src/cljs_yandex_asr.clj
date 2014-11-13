(ns cljs-yandex-asr)

(defmacro load-key
  "Load key from any slurpable source"
  ([] (clojure.string/trim (slurp "asr.key")))
  ([path] (clojure.string/trim (slurp path))))

