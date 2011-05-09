(ns gantry.run
  (:use [clojure.contrib.condition :only [raise]]
        gantry.core
        gantry.log))

(def *hosts* nil)
(def *args* nil)

; call from main with specified config :hosts :args
(defmacro with-resource [hosts args & body ]
  `(binding [~*hosts* ~hosts ~*args* args]
     `(do ~@body)))


(defn run [cmd] 
  ; replace info with some kind of logging
  (doall (map #(log-multi-line :info (:host %) (validate cmd %)) (remote* *hosts* cmd *args*))) *hosts*)


(with-resource hosts args
               ; call user specified functions which call run
               )
