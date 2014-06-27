(ns firmata-samples.lcd
  (:refer-clojure :exclude [print println])
  (:require [firmata.core :refer :all]
            [firmata.util])
  (:import [java.util.concurrent TimeUnit]))

; commands
(def ^{:private true} LCD_CLEARDISPLAY   0x01)
(def ^{:private true} LCD_RETURNHOME     0x02)
(def ^{:private true} LCD_ENTRYMODESET   0x04)
(def ^{:private true} LCD_DISPLAYCONTROL 0x08)
(def ^{:private true} LCD_CURSORSHIFT    0x10)
(def ^{:private true} LCD_FUNCTIONSET    0x20)
(def ^{:private true} LCD_SETCGRAMADDR   0x40)
(def ^{:private true} LCD_SETDDRAMADDR   0x80)

; flags for display entry mode
(def ^{:private true} LCD_ENTRYRIGHT          0x00)
(def ^{:private true} LCD_ENTRYLEFT           0x02)
(def ^{:private true} LCD_ENTRYSHIFTINCREMENT 0x01)
(def ^{:private true} LCD_ENTRYSHIFTDECREMENT 0x00)

; flags for display on/off control
(def ^{:private true} LCD_DISPLAYON  0x04)
(def ^{:private true} LCD_DISPLAYOFF 0x00)
(def ^{:private true} LCD_CURSORON   0x02)
(def ^{:private true} LCD_CURSOROFF  0x00)
(def ^{:private true} LCD_BLINKON    0x01)
(def ^{:private true} LCD_BLINKOFF   0x00  )

; flags for display/cursor shift
(def ^{:private true} LCD_DISPLAYMOVE 0x08)
(def ^{:private true} LCD_CURSORMOVE  0x00)
(def ^{:private true} LCD_MOVERIGHT   0x04)
(def ^{:private true} LCD_MOVELEFT    0x00)

; flags for function set
(def ^{:private true} LCD_8BITMODE 0x10)
(def ^{:private true} LCD_4BITMODE 0x00)
(def ^{:private true} LCD_2LINE    0x08)
(def ^{:private true} LCD_1LINE    0x00)
(def ^{:private true} LCD_5x10DOTS 0x04)
(def ^{:private true} LCD_5x8DOTS  0x00)

(declare init begin-4bit-mode begin-8bit-mode clear)

(defn create-lcd

  ([board rs-pin enable-pin d0 d1 d2 d3]
    (create-lcd board 
                :four-bit-mode rs-pin nil enable-pin
                d0 d1 d2 d3
                nil nil nil nil))

  ([board display-function rs rw enable d0 d1 d2 d3 d4 d5 d6 d7]
    (init {:board board
     :display-function (if (= display-function :four-bit-mode)
                         (bit-or LCD_4BITMODE LCD_1LINE LCD_5x8DOTS)
                         (bit-or LCD_8BITMODE LCD_1LINE LCD_5x8DOTS)),
     :rs-pin rs,
     :rw-pin rw,
     :enable-pin enable,
     :data-pins [d0, d1, d2, d3, d4, d5, d6, d7]})))

(defn- init
  [lcd]
  (let [board (:board lcd)
        {rs-pin :rs-pin, rw-pin :rw-pin, enable-pin :enable-pin} lcd]
    (set-pin-mode board rs-pin :output)
    (when rw-pin (set-pin-mode board rw-pin :output))
    (set-pin-mode board enable-pin :output)
    (doseq [pin (:data-pins lcd)]
      (when pin 
        (set-pin-mode board pin :output))))
  lcd)

(defn- has-bit-values? [val first-bit & b]
  (not (= 0 (apply bit-and (conj b first-bit val)))))

(defn- high-low [x]
  (if (= 0 x) :low :high))

(defn- delay-micros [micros]
  (.sleep TimeUnit/MICROSECONDS micros))

; 
; Low-level commands

(defn- pulse-enable [lcd]
  (let [board (:board lcd)
        enable-pin (:enable-pin lcd)]
    (set-digital board enable-pin :low)
    (delay-micros 1)  
    (set-digital board enable-pin :high)
    (delay-micros 1)
    (set-digital board enable-pin :low)
    (delay-micros 100)))

(defn- write-bits [lcd value bits]
  ; (clojure.core/println "writing value" (firmata.util/to-hex-str value))
  (let [board (:board lcd)]
    (dotimes [i bits]
      (let [pin (get (:data-pins lcd) i)
            v (-> value 
                (bit-shift-right i)
                (bit-and 0x01)
                high-low)]
        ; (clojure.core/println "writing" v "to pin" pin)
        (set-digital board pin v))))
  (pulse-enable lcd))

(defn- write-8bits [lcd value]
  (write-bits lcd value 8))

(defn- write-4bits [lcd value]
  (write-bits lcd value 4))

(defn- send-value
  [lcd value mode]

  (set-digital (:board lcd) (:rs-pin lcd) mode)

  (when (:rw-pin lcd)
    (set-digital (:board lcd) (:rw-pin) :low))

  (if (has-bit-values? (:display-function lcd) LCD_8BITMODE)
    (write-8bits lcd value)
    (do 
      (write-4bits lcd (bit-shift-right value 4))
      (write-4bits lcd value)))

  )

(defn- command
  [lcd value]
  (send-value lcd value :low))

(defn- write
  [lcd value]
  (send-value lcd value :high))

;
; High-level commands

(defn begin
  ([lcd columns lines] (begin lcd columns lines LCD_5x8DOTS))
  ([lcd columns lines dot-size]
  (let [board (:board lcd) 
        numlines lines]

    (with-local-vars [display-function (:display-function lcd)
                      display-control 0
                      display-mode 0]

        (when (> lines 1)
          (var-set display-function (bit-or @display-function LCD_2LINE)))

        (when (and (not= dot-size 0) (= lines 1))
          (var-set display-function (bit-or @display-function LCD_5x10DOTS)))

        (delay-micros 50000)

        (set-digital board (:rs-pin lcd) :low)
        (set-digital board (:enable-pin lcd) :low)

        (when (:rw-pin board)
          (set-digital board (:rw-pin lcd) :clojure.walk))

        (if (has-bit-values? @display-function LCD_8BITMODE)
          (begin-8bit-mode lcd @display-function)
          (begin-4bit-mode lcd))

        (command lcd (bit-or LCD_FUNCTIONSET @display-function))

        (var-set display-control (bit-or LCD_DISPLAYON LCD_CURSOROFF LCD_BLINKOFF))

        ; (display lcd)
        (command lcd @display-control)

        (clear lcd)

        (var-set display-mode (bit-or LCD_ENTRYLEFT LCD_ENTRYSHIFTDECREMENT))

        (command lcd (bit-or LCD_ENTRYMODESET @display-mode))

        (merge lcd {:display-function @display-function
                    :display-mode @display-mode
                    :display-control @display-control
                    :num-lines numlines})))))

(defn- begin-4bit-mode [lcd]
  ; several tries
  (write-4bits lcd 0x03)
  (delay-micros 4500)

  (write-4bits lcd 0x03)
  (delay-micros 4500)

  (write-4bits lcd 0x03)
  (delay-micros 150)

  (write-4bits lcd 0x02))

(defn- begin-8bit-mode [lcd display-function]
  ; several tries
  (command lcd (bit-or LCD_FUNCTIONSET display-function))
  (delay-micros 4500)
  (command lcd (bit-or LCD_FUNCTIONSET display-function))
  (delay-micros 150)
  (command lcd (bit-or LCD_FUNCTIONSET display-function)))

(defn clear [lcd]
  (command lcd LCD_CLEARDISPLAY)
  (delay-micros 2000)
  lcd)

(defn home [lcd]
  (command lcd LCD_RETURNHOME)
  (delay-micros 2000)
  lcd)

(defn set-cursor [lcd col row]
  (let [row-offsets [0x00 0x40 0x14 0x54]
        r (if (>= row (:num-lines lcd)) (- (:num-lines lcd) 1) row)]
    (command lcd (bit-or LCD_SETDDRAMADDR (+ col (get row-offsets r)))))
  lcd)

(defn- display-setting-on [lcd key setting]
  (let [updated-value (bit-or (key lcd) setting)]
    (command lcd updated-value)
    (assoc lcd key updated-value)))

(defn- display-setting-off [lcd key setting]
  (let [updated-value (bit-and (key lcd) (bit-not setting))]
    (command lcd updated-value)
    (assoc lcd key updated-value)))

(defn no-display [lcd] (display-setting-off lcd :display-control LCD_DISPLAYON))
(defn display    [lcd] (display-setting-on  lcd :display-control LCD_DISPLAYON))

(defn no-cursor [lcd] (display-setting-off lcd :display-control LCD_CURSORON))
(defn cursor    [lcd] (display-setting-on  lcd :display-control LCD_CURSORON))

(defn no-blink [lcd] (display-setting-off lcd :display-control LCD_BLINKON))
(defn blink    [lcd] (display-setting-on  lcd :display-control LCD_BLINKON))

(defn left-to-right [lcd] (display-setting-on  lcd :display-mode LCD_ENTRYLEFT))
(defn right-to-left [lcd] (display-setting-off lcd :display-mode LCD_ENTRYLEFT))

(defn autoscroll    [lcd] (display-setting-on  lcd :display-mode LCD_ENTRYSHIFTINCREMENT))
(defn no-autoscroll [lcd] (display-setting-off lcd :display-mode LCD_ENTRYSHIFTINCREMENT))

(defn scroll-display-left [lcd] 
  (command (bit-or LCD_CURSORSHIFT LCD_DISPLAYMOVE LCD_MOVELEFT))
  lcd)

(defn scroll-display-right [lcd] 
  (command (bit-or LCD_CURSORSHIFT LCD_DISPLAYMOVE LCD_MOVERIGHT))
  lcd)

(defn create-char [lcd location char-map]
  (assert (= 8 (count char-map)))
  (let [loc (bit-and location 0x07)]
    (command lcd (bit-or LCD_SETCGRAMADDR (bit-shift-left loc 3)))
    (doseq [i (range 8)]
      (write lcd (get char-map i :low))))
  lcd)

(defn print [lcd value]
  (doseq [c (str value)]
    (write lcd (int c)))
  lcd)

(defn println 
  ([lcd] (println lcd ""))
  ([lcd string]
    (-> lcd 
      (print string)
      (print "\n"))))
