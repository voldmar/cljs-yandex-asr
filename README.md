# cljs-yandex-asr

ClojureScript core.async interface for Yandex Speechkit Cloud — Yandex’s speech
recognition service


## Installation

cljs-yandex-asr is available in Maven Central. Add it to your `:dependencies `
in your Leiningen `project.clj` or `build.boot`:


```clojure
[cljs-yandex-asr "0.1.4"]
```

## Compatibility

Tested against lastest stable versions of Chrome and Firefox


## Usage

cljs-yandex-asr intented to use with [`cljs-audiocapture`][cljs-audiocapture]
to get stream from user’s microphone and send every frame to web socket. But
feel free to experiment with other sources using this procotol.

`cljs-yandex-asr.core` namespace has one public funtion `recognize`. It returns
returns map with `:>audio` channel to put PCM frames to and `:<results` channel
to read responses from.

Each message taken from `<results` is hashmap either with keys `:text` and `:utterance?`
or `:error` if error is occured. `utterance?` is set to `true` if text is recognized
finally, overwise concider `text` as partial recognition that can be changed until `utterance?`
is `true`.

Also there is macro `load-key` to add API key to your code (as you cannot
store it in public repo). By default it loads key from `asr.key` in root
of your project or you can use *local* path as argument.

## Examples

You can use this pattern:

```clojure
(ns your-app.core
  (:require
    [cljs-audiocapture :refer [capture-audio]]
    [cljs-yandex-asr :as asr :include-macrose true]
    [cljs.core.async :as async])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(defn ^:export main []
  (js/console.log "Starting Application") 
  (go
    (let [{:keys [audio-chan error]} (async/<! (capture-audio {}))
          {:keys [>audio <results]} (asr/recognize (asr/load-key))]
      (if error
        (js/console.error error)
        (do
          (async/pipe audio-chan >audio)
          (async/put! audio-chan :start)
          (loop []
            (when-let [result (async/<! <results)]
              (js/console.log "From ASR" (pr-str result))
              (recur))))))))

```

## License

Copyright © YANDEX LLC, 2015. Distributed under
the Eclipse Public License, which can be found in [LICENSE.md](LICENSE.md)
at the root of this distribution. By using this software in any fashion,
you are agreeing to be bound by the terms of this license. You must not remove
this notice, or any other, from this software.

![Yandex Logo](https://yastatic.net/morda-logo/i/turkey_logos/logo.svg)

Use of Yandex SpeechKit Cloud service is under conditions of Yandex SpeechKit
Cloud Terms of Use. Full text of Yandex SpeechKit Cloud Terms of Use is
available at [http://legal.yandex.ru/speechkit_cloud/](http://legal.yandex.ru/speechkit_cloud/)

[cljs-audiocapture]: https://github.com/voldmar/cljs-audiocapture
