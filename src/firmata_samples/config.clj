(ns firmata-samples.config
  (:require [firmata.util :refer [detect-arduino-port]]))

;
; NOTE If you need to refer to a specific serial port, 
; change this value
(def port-name (detect-arduino-port))
