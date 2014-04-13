(ns firmata-samples.sik
  (:require [firmata-samples.config :refer [port-name]]
            [firmata-samples.board :refer :all]
            [firmata.core :refer :all]
            [firmata.receiver :refer :all]
            [clojure.core.async :refer [timeout <!! <!]]))

(defn- wait-to-settle []
  ; TODO: This can be removed in future releases of clj-firmata
  (println "waiting for board to settle...")
  (<!! (timeout 2000))
  (println "ready"))

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

    (<!! (timeout 10))))

(defexample multiple-leds
  "4) Multiple LEDs - one after another

  A variety of samples which can be used with the 8-LED circuit."
  [board (open-board port-name)]

  (wait-to-settle)

  (let [pins [2,3,4,5,6,7,8,9]]

    (run-loop
     ; one after the other
     (doseq [v [:high :low]]
       (doseq [pin pins]
         (set-digital board pin v)
         (<!! (timeout 100))))
     )))

(defexample multiple-leds-single
  "4) Multiple LEDs - one at a time and reverse

  A variety of samples which can be used with the 8-LED circuit."
  [board (open-board port-name)]

  (wait-to-settle)

  (let [pins [2,3,4,5,6,7,8,9]]

    (run-loop

     ; one at a time
     (doseq [pin pins]
       (set-digital board pin :high)
       (<!! (timeout 100))
       (set-digital board pin :low))

     ;reverse it
     (doseq [pin (reverse pins)]
       (set-digital board pin :high)
       (<!! (timeout 100))
       (set-digital board pin :low)))))

(defexample multiple-leds-marquee
  "4) Multiple LEDs - marquee - mimic \"chase lights\" like those around signs.

  A variety of samples which can be used with the 8-LED circuit."
  [board (open-board port-name)]

  (wait-to-settle)

  (let [pins [2,3,4,5,6,7,8,9]]

    (run-loop
     ; marquee - mimic "chase lights" like those around signs.
     (doseq [index (range 4)]
       (set-digital board (nth pins index) :high)
       (set-digital board (+ (nth pins index) 4) :high)
       (<!! (timeout 200))
       (set-digital board (nth pins index) :low)
       (set-digital board (+ (nth pins index) 4) :low)))))

(defexample multiple-leds-random
  "4) Multiple LEDs - pick a random light

  A variety of samples which can be used with the 8-LED circuit."
  [board (open-board port-name)]

  (wait-to-settle)

  (let [pins [2,3,4,5,6,7,8,9]]
    ; random light
    (run-loop
     (let [pin (rand-nth pins)]
       (set-digital board pin :high)
       (<!! (timeout 100))
       (set-digital board pin :low)))))

(defexample push-buttons
  "5) Push buttons

  Dealing with digital inputs."
  [board (open-board port-name)]

  (wait-to-settle)

  (let [pressed (atom [false false])
        light-led (fn [[left right]]
                    (set-digital board 13 (if (and (or left right) (not (and left right))) :high :low)))]
    (-> board
        (set-pin-mode 2 :input)
        (enable-digital-port-reporting 2 true))

    (-> board
        (set-pin-mode 3 :input)
        (enable-digital-port-reporting 3 true))

    (on-digital-event board 2 #(light-led (reset! pressed [(= :low (:value %)) (last @pressed) ])))

    (on-digital-event board 3 #(light-led (reset! pressed [(first @pressed) (= :low (:value %))])))))

(defn arduino-map
  "Clojure implemation of the Arduino map function.
  http://arduino.cc/en/reference/map"
  [x, in-min, in-max, out-min, out-max]
  (int (/ (* (- x  in-min) (- out-max out-min)) (+ (- in-max in-min) out-min))))

(defn arduino-constrain
  "Clojure implementation of the Arduino constrain function.
  http://arduino.cc/en/Reference/Constrain"
  [x min max]
  (cond (< x min)      min
        (<= min x max) x
        :else          max))

(defexample photo-resistor
  "6) Photo Resistor

  Use a photoresistor (light sensor) to control the brightness
  of a LED."
  [board (open-board port-name)]

  (wait-to-settle)

  (set-pin-mode board 9 :pwm)
  (enable-analog-in-reporting board 0 true)

  ; modify manual min to the min value of your photo resistor
  (let [manual-min 300]
   (on-analog-event board 0
                    #(set-analog board 9
                                 (-> (:value %)
                                     (arduino-map manual-min 1023 0 255)
                                     (arduino-constrain 0 255))))))

(defn to-voltage
  "Takes an analog value and converts it to the true voltage value."
  [x]
  (* x 0.004882814))

(defexample temperature-sensor
  "7) Temperature Sensor

  Use the \"serial monitor\" window to read a temperature sensor."
  [board (open-board port-name)]

  (wait-to-settle)

  (enable-analog-in-reporting board 0 true)

  (on-analog-event board 0 #(let [voltage (to-voltage (:value %))
                                  degreeC (* (- voltage 0.5) 100.0)
                                  degreeF (+ 32.0 (* degreeC (/ 9.0 5.0)))]
                              (println "Voltage " voltage)
                              (println degreeC "ºC")
                              (println degreeF "ºF")))

  )
