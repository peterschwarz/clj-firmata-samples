(ns firmata-samples.sik
  (:require [firmata-samples.config :refer [port-name]]
            [firmata-samples.board :refer :all]
            [firmata.core :refer :all]
            [firmata.receiver :refer :all]
            [clojure.core.async :refer [timeout <!! <!]]))


(defexample blink
  "1) Blinking an LED

  Blinks an LED wired to pin 13."
  [board (open-board port-name)]
  (run-loop
   (let [pin 13
         sleep-time 1000]
     (set-digital board pin :high)

     (<! (timeout sleep-time))

     (set-digital board pin :low)

     (<! (timeout sleep-time)))))


(defexample potentiometer
  "2) Potentiometer

  Read the values off of a potentiometer wired to analog pin A0"
  [board (open-board port-name)]

  ; TODO: This can be removed in future releases of clj-firmata
  (println "waiting for things to settle")
  (<!! (timeout 2000))

  (let [sensor (atom 1000)]
    (println "Enabling analog-in reporting")
    (enable-analog-in-reporting board 0 true)

    (println "Creating analog event handler")
    (on-analog-event board 0 #(reset! sensor (:value %)))

    (run-loop

     (let [pin 13
           sleep-time @sensor]
       (set-digital board pin :high)

       (<! (timeout sleep-time))

       (set-digital board pin :low)

       (<! (timeout sleep-time))))
  ))
