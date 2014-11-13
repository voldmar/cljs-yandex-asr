(defproject cljs-yandex-asr "0.1.0-SNAPSHOT"
  :description "ClojureScript core.async interface to Yandex ASR"
  :url "https://github.com/voldmar/cljs-yandex-asr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [cljs-audiocapture "0.1.0"]
                 [org.clojars.leanpixel/cljs-uuid-utils "1.0.0-SNAPSHOT"]
                 [jarohen/chord "0.4.2"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {:output-to "cljs_yandex_asr.js"
                         :output-dir "out"
                         :optimizations :none
                         :source-map true}}
             {:id "demo"
              :source-paths ["demo"]
              :compiler {:output-to "cljs_yandex_asr_demo.js"
                         :output-dir "out-demo"
                         :optimizations :none
                                                                                                                                :source-map true}}
             {:id "prod"
              :source-paths ["src"]
              :compiler {:output-to "cljs_yandex_asr.min.js"
                         :optimizations :advanced }}]})
