(ns firmata-samples.blink
  (:require [firmata-samples.config :refer [port-name]]
            [firmata-samples.board :refer :all]
            [firmata.core :refer :all]
            [clojure.core.async :refer [timeout]]))


(defexample blink
  "Runs example 1 from the SparkFun SIK guide.

  1) Blinking an LED"
  [board (open-board port-name)]
  (run-loop board
            (let [pin 13
                  sleep-time 1000]
              (set-digital board pin :high)

              (<! (timeout sleep-time))

              (set-digital board pin :low)

              (<! (timeout sleep-time)))))
