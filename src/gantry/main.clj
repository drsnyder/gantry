(ns gantry.main
  (:use clojure.contrib.command-line
        gantry.core
        gantry.log
        gantry.hoist)
  (:gen-class))

; to run java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts host1,host2
(defn -main [& args]
  (with-command-line args
      "Gantry"
      [[hosts     "The remote hosts" nil]
       [port      "The ssh port of the remote hosts" 22]
       remaining]
    (println "hosts: " hosts)
    (println "port: " port)
    (println "remaining: " remaining)))
