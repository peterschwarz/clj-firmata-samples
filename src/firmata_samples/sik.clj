(ns firmata-samples.sik
  (:require [firmata-samples.config :refer [port-name]]
            [firmata-samples.board :refer :all]
            [firmata.core :refer :all]
            [firmata.receiver :refer :all]
            [clojure.core.async :refer [timeout <!! <!]]))

(defn- wait-to-settle []
  ; TODO: This can be removed in future releases of clj-firmata
  (println "waiting for things to settle")
  (<!! (timeout 2000)))

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

  (wait-to-settle)

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

       (<! (timeout sleep-time))))))

(declare main-colors show-spectrum)

(defexample rgb-led
  "3) RGB LED

  Make an RGB LED display a rainbow of colors!"
  [board (open-board port-name)]

  (wait-to-settle)

  (let [red-pin 9
        green-pin 10
        blue-pin 11]
    (run-loop

     (main-colors board red-pin green-pin blue-pin)

     (show-spectrum board red-pin green-pin blue-pin))))

(defn- main-colors
  "Displays the main RGB colors, one at a time"
  [board red-pin green-pin blue-pin]

  (set-pin-mode board red-pin :output)
  (set-pin-mode board green-pin :output)
  (set-pin-mode board blue-pin :output)


  (let [sleep-time 1000]
    (<!! (timeout sleep-time))

    (set-digital board red-pin :high)
    (set-digital board green-pin :low)
    (set-digital board blue-pin :low)

    (<!! (timeout sleep-time))

    (set-digital board red-pin :low)
    (set-digital board green-pin :high)
    (set-digital board blue-pin :low)

    (<!! (timeout sleep-time))

    (set-digital board red-pin :low)
    (set-digital board green-pin :high)
    (set-digital board blue-pin :high)

    ; cyan
    (<!! (timeout sleep-time))

    (set-digital board red-pin :low)
    (set-digital board green-pin :high)
    (set-digital board blue-pin :low)

    ; purple
    (<!! (timeout sleep-time))

    (set-digital board red-pin :high)
    (set-digital board green-pin :low)
    (set-digital board blue-pin :high)

    ; white
    (<!! (timeout sleep-time))

    (set-digital board red-pin :high)
    (set-digital board green-pin :high)
    (set-digital board blue-pin :high)

    (<!! (timeout sleep-time))))

(defn- show-spectrum
  [board red-pin green-pin blue-pin]
  (set-pin-mode board red-pin :pwm)
  (set-pin-mode board green-pin :pwm)
  (set-pin-mode board blue-pin :pwm)
  (doseq [x (range 768)]
    (cond
      (<= x 255) (do
                   (set-analog board red-pin (- 255 x))
                   (set-analog board green-pin x)
                   (set-analog board blue-pin 0))
      (<= x 511) (do
                   (set-analog board red-pin 0)
                   (set-analog board green-pin (- 255 x 256))
                   (set-analog board blue-pin (- x 256)))
      :else (do
                   (set-analog board red-pin (- x 512))
                   (set-analog board green-pin 0)
                   (set-analog board blue-pin (- 255 x 512))))

    (<!! (timeout 10))
    )
  )
