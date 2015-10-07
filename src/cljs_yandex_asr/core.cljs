(ns cljs-yandex-asr.core
  (:require
    [chord.client :refer  [ws-ch]]
    [cljs-audiocapture :refer [capture-audio *AUDIO_FORMAT*]]
    [cljs-uuid-utils :as uuid]
    [cljs.core.async :as async]
    [cljs.core.async.impl.protocols :as p])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defn- bidi-ch
  "Constructs bidirectional channel"
  [read-ch write-ch & [{:keys [on-close]}]]
  (reify
    p/ReadPort
    (take! [_ handler]
      (p/take! read-ch handler))

    p/WritePort
    (put! [_ msg handler]
      (p/put! write-ch msg handler))

    p/Channel
    (close! [_]
      (p/close! read-ch)
      (p/close! write-ch)
      (when on-close
        (on-close)))))

(def ^:dynamic *asr-key*)
(def ^:dynamic *asr-websocket* "wss://webasr.yandex.net/asrsocket.ws")
(def ^:dynamic *retry-count* 5)


(defn recognize* [& [asr-uuid]]
  (let [audio-chan (async/chan)
        results-chan (async/chan)
        asr-chan (bidi-ch results-chan audio-chan)
        asr-uuid (or asr-uuid (uuid/uuid-string (uuid/make-random-uuid)))
        init-json (js/JSON.stringify (clj->js {:type "message"
                                               :data {:uuid asr-uuid
                                                      :key *asr-key*
                                                      :format *AUDIO_FORMAT*}}))]
    ; We suppose that WebSocket is not reliable
    ; so we should revive it.
    (go-loop []
        (let [{:keys [ws-channel error]} (async/<! (ws-ch *asr-websocket*
                                                          {:format :str}))]
          (if error
            (do
              (js/console.log "ASR socket error" error)
              (recur))

            (do
              (async/>! ws-channel init-json)
              (let [{:keys [message error]} (async/<! ws-channel)]
                (if error
                  (do
                    (js/console.log "ASR socket error" error)
                    (recur))
                  (do
                    (js/console.log "ASR init response" message)
                    #_(async/>! audio-chan :start)
                    (loop []
                      (async/>! ws-channel (async/<! audio-chan))
                      ; So, a couple words about timeout below.
                      ; The problem is ASR web socket constantly messages us,
                      ; even if we send no data.
                      (let [[{:keys [message error]} c]
                            (async/alts! [ws-channel (async/timeout 10)] {:priority true})]
                        (if-not message
                          (recur)
                          (if error
                            (js/console.log "ASR socket error" error)
                            (let [message (-> (js/JSON.parse message)
                                              (js->clj :keywordize-keys true)) 
                                  error? (= "Error" (.-type message) #_(:type message))
                                  data (:data message)]
                              (if error?
                                (js/console.log "ASR backend error" data)
                                (do
                                  (async/put! results-chan [(:text data) (:uttr data)])
                                  (recur)))))))))))))
          (recur)))
  asr-chan))

(defn recognize [asr-key & args]
  (binding [*asr-key* asr-key]
    (apply recognize* args)))
