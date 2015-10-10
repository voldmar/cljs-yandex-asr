(ns cljs-yandex-asr.core
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]) 
  (:require
    [chord.client :refer [ws-ch]]
    [chord.channels :refer [bidi-ch]]
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [<! >! close! chan sliding-buffer]]))

(defn maybe-error [message]
  (if (:error message)
    message
    (let [message (-> message
                      :message
                      js/JSON.parse
                      (js->clj :keywordize-keys true))
          message (case (:type message)
                    "InitResponse" {:message (:data message)}
                    "Error" {:error (:data message)}
                    "AddDataResponse" {:message (select-keys (:data message) [:text :uttr])}
                    message)]
      message)))

(defn init-asr
  "Make web-socket and init ASR with UUID, key and format

  Return channel to read Chord’s WS channel from. Write audio chunks
  to this WS-channel and read ASR responses"
  [asr-url asr-key asr-uuid]
  (let [>init (chan)]
    (go
      (let [init-json (js/JSON.stringify (clj->js {:type "message"
                                                   :data {:uuid (str asr-uuid)
                                                          :key asr-key
                                                          :format "audio/x-pcm;bit=16;rate=44100"}}))
            ; I want messages from ASR web-socket
            ; be hashmap either with :error key or with payload
            ; Because chord uses formatters I can’t use
            ; this channel as :read-ch option and need to
            ; make proxy for ws bidi-channel
            wrapper (chan (async/sliding-buffer 4) (map maybe-error))
            ws (<! (ws-ch asr-url {:format :str}))
            ws (assoc ws :ws-channel (bidi-ch
                                       (async/pipe (:ws-channel ws) wrapper)
                                       (:ws-channel ws)))
            _ (>! (:ws-channel ws) init-json)
            init (<! (:ws-channel ws))]
        (>! >init (merge ws init))))
    >init))

(defn recognize
  "Recognize audio stream in real time using Yandex SpeechKit Cloud

  Returns map with :>audio channel to put audio chunks and
  :<results channel to read responses from

  Every response is hash map with :text containing recognized text
  and :uttr flag if text contain fully recognized fragment

  May contain :error key with error info"

  [asr-key & [{:keys [asr-uuid asr-url retry-count]
               :or {asr-url "wss://webasr.yandex.net/asrsocket.ws"
                    retry-count 5}}]]
  (let [asr-uuid (or asr-uuid (random-uuid))
        <audio (chan (sliding-buffer 16))
        >results (chan (sliding-buffer 16))]

    ; We suppose that WebSocket is not reliable
    ; so we should revive it.
    (go-loop [{:keys [ws-channel error]} (<! (init-asr asr-url asr-key asr-uuid))
              counter retry-count]
      (if error
        (>! >results {:error error}) 
        (let [[errors messages] (async/split :error ws-channel)]
          (async/pipe <audio ws-channel false)
          (async/pipe messages >results false)
          ; Block until error, then show error
          (let [e (<! errors)]
            (js/console.log "Error" (pr-str e))
            (>! >results e))))
      (close! ws-channel)
      (when (pos? (dec counter))
        (recur (<! (init-asr asr-url asr-key asr-uuid)) (dec counter))))
    {:>audio <audio
     :<results >results}))

