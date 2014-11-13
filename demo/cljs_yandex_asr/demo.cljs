(ns cljs-yandex-asr.demo
  (:require
    [cljs-audiocapture :refer [capture-audio]]
    [cljs-yandex-asr :as asr :include-macros true]
    [cljs.core.async :as async])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(defn- escape [string]
  (let [div (.createElement js/document "div")]
    (.appendChild div (.createTextNode js/document string))
    (.-innerHTML div)))

(defn- render []
  (let [utterance-span (.querySelector js/document ".text__utterance")
        partial-span (.querySelector js/document ".text__partial")
        render-chan (async/chan)]
    (go-loop [utterance ""]
        (let [[text utterance?] (async/<! render-chan)
              text (escape text)
              utterance (if utterance? (str utterance " " text) utterance)
              partial-text (if-not utterance? text "")]
          (aset utterance-span "innerHTML" utterance)
          (aset partial-span "innerHTML" partial-text)
          (recur utterance)))
    render-chan))

(defn ^:export main []
  (go
    (let [{:keys [audio-chan error]} (async/<! (capture-audio (async/sliding-buffer 1)))
          recognize-chan (asr/recognize (asr/load-key))
          render-chan (render)]
      (async/put! audio-chan :start)
      (async/pipe audio-chan recognize-chan)
      (async/pipe recognize-chan render-chan))))

