(ns cljs-yandex-asr.core
  (:require
    [chord.client :refer  [ws-ch]]
    [cljs.core.async :as async :refer [<! >! close! chan sliding-buffer]])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defn maybe-error [message]
  (let [message (-> (js/JSON.parse message)
                    (js->clj :keywordize-keys true)) 
        error? (= "Error" (.-type message))
        data (:data message)]
    (if error?
      {:error (:data message)}
      (select-keys (:data message) [:text :uttr]))))

(defn init-asr [asr-url asr-key asr-uuid]
  (let [init-json (js/JSON.stringify (clj->js {:type "message"
                                               :data {:uuid asr-uuid
                                                      :key asr-key
                                                      :format "audio/x-pcm;bit=16;rate=44100"}}))
        ws (<! (ws-ch asr-url {:format :str
                               :read-ch (async/chan (sliding-buffer 16) (map maybe-error))}))
        _ (>! (:ws-channel ws) init-json)
        init (<! (:ws-channel ws))]
    (merge ws init)))

(defn recognize [asr-key & [{:keys [asr-uuid asr-url retry-count]
                             :or {asr-url "wss://webasr.yandex.net/asrsocket.ws"
                                  retry-count 5}}]]
  (let [asr-uuid (or asr-uuid (random-uuid))
        chans {:<audio (chan (sliding-buffer 16))
               :>results (chan (sliding-buffer 16))}
        {:keys [<audio >results]} chans]

    ; We suppose that WebSocket is not reliable
    ; so we should revive it.
    (go-loop [ws (init-asr asr-url asr-key asr-uuid)
              counter retry-count]
      (let [{:keys [error ws-channel]} ws]
        (if-not error
          (do
            ; We unconditionally send data to SpeechKit Cloud
            (async/pipe <audio ws-channel)
            (go-loop []
              (let [[{:keys [message error]} _] (async/alts! [ws-channel (async/timeout 10)] {:priority true})]
                (cond
                  message (do
                            (>! >results message)
                            (recur))
                  error (do
                          (close! ws-channel)
                          (>! >results {:error error}))
                  :else (recur)))))
          (do
            (close! ws-channel)
            (>! >results {:error error})
            (when (pos? counter)
              (recur (init-asr asr-url asr-key asr-uuid) (dec counter)))))))
  chans))

