# cljs-yandex-asr

ClojureScript core.async interface for Yandex Speechkit Cloud — Yandex’s speech
recognition service


## Installation

cljs-yandex-asr is available in Maven Central. Add it to your `:dependencies `
in your Leiningen `project.clj`:


```clojure
[cljs-yandex-asr "0.1.0"]
```

## Compatibility

Tested against lastest stable versions of Chrome and Firefox


## Usage

cljs-yandex-asr uses [`cljs-audiocapture`][cljs-audiocapture] to
get stream from user’s microphone and send every frame to web socket.

cljs-yandex-aser namespace has one public funtion `recognize`. It returns
bidirectional channel you can put PCM frames to and read recognized text from.

Response is a vector `[text utterance?]` where text is recogized text
and `utterance?` is set to `true` if text is recognized finally, overwise
concider `text` as partial recognition that can be changed until `utterance?`
is `true`.

Also there is macro `load-key` to add API key to your code (as you cannot
store it in public repo). By default it loads key from `asr.key` in root
of your project or you can use *local* path as argument.

## Examples

You can use this pattern:

```clojure
(ns your-app
  (:require
    [cljs-audiocapture :refer [capture-audio]]
    [cljs-yandex-asr :as asr :include-macrose true]
    [cljs.core.async :as async])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))


(defn- process
  "Function that read from channel and process recognized text"
  [])

(defn ^:export main []
  (go
    (let [{:keys [audio-chan error]} (async/<! (capture-audio (async/sliding-buffer 1)))
          recognize-chan (asr/recognize (asr/load-key))
          process-chan (process)]
      (async/put! audio-chan :start)
      (async/pipe audio-chan recognize-chan)
      (async/pipe recognize-chan process-chan))))

```

[cljs-audiocapture]: https://github.com/voldmar/cljs-audiocapture
