(ns firmata-samples.board
  (:require [firmata.core :as f]
            [clojure.core.async :refer [chan go-loop <! >! >!! timeout] :as a]))


(defn reset-board
  "Resets the board on the given port to the basic state."
  [port-name]
  (let [board (f/open-board port-name)]
    (f/close! board)))

(defprotocol BoardExample
  (run-example [this])
  (stop-example [this]))

(defmacro defexample
  [name doc-str board-def & body]

  `(def ^{:doc ~doc-str}
     ~name
       (let [state# (atom {})]
         (reify BoardExample
           (run-example
            [_]
            (reset! state# {:board ~(last board-def) :channel (chan 1)})
            (let [~(first board-def) (:board @state#)
                  ~'__control-ch (:channel @state#)]
              ~(cons 'do body)
              @state#))

           (stop-example
            [_]
            (a/close! (:channel @state#))
            (f/close! (:board @state#))
            (reset! state# {}))

           ))))


(defmacro run-loop
  [& body]
  `(do
     (assert ~'__control-ch "must be called within a defexample")
     (let [c# ~'__control-ch]
       (>!! c# :go)
       (go-loop []
             (when-let [x (<! c#)]
               ~(cons 'do body)
               (>! c# :go)
               (recur))))))
