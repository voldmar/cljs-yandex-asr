(set-env!
  :source-paths   #{"src"}
  :dependencies '[[adzerk/boot-cljs           "1.7.48-5" :scope "test"]
                  [adzerk/bootlaces           "0.1.12"   :scope "test"]
                  [org.clojure/clojurescript  "1.7.122"]
                  [jarohen/chord              "0.6.0"]
                  [com.cognitect/transit-cljs "0.8.225"] ; To turn off warning about uuid
                  [org.clojure/core.async     "0.1.346.0-17112a-alpha"]])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.bootlaces :refer :all])

(def +version+ "0.1.4")
(bootlaces! +version+)

(task-options!
  pom  {:project      'cljs-yandex-asr
        :version      +version+
        :description  "ClojureScript core.async interface to Yandex SpeechKit Cloud"
        :url          "https://github.com/voldmar/cljs-yandex-asr"
        :scm          {:url "https://github.com/voldmar/cljs-yandex-asr"}
        :license      {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build []
  (comp
    (cljs :optimizations :advanced)))

