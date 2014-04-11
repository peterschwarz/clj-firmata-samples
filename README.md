# clj-firmata-samples

Provides [clj-firmata](https://github.com/peterschwarz/clj-firmata) samples. 

The examples follow the circuit layouts from the [SparkFun Inventor's Kit guide](https://dlnmh9ip6v2uc.cloudfront.net/datasheets/Kits/SFE-SIK-RedBoard-Guide-Version3.0-Online.pdf)

## Usage

Checkout this repo, and edit the `(def port-name "cu.usbmodemfd131")` line in the firmata-samples.config namespace.  You can find your port name by using
   
    => (require '[serial.util :refer [list-ports]])
    => (list-ports)

Running the examples are quite simple.  For example, to run the first blink example, we can run the following: 

    => (require '[firmata-samples.board :refer :all] 
                '[firmata-samples.blink :refer :all])
     
    => (run-example blink)
    ; ... after we've grown board of blinking...
    => (stop-example blink)


## License

Copyright © 2014 Peter Schwarz

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
