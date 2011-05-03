(ns gantry.main
  (:use clojure.contrib.command-line
        clojure.contrib.str-utils
        gantry.core
        gantry.log
        gantry.hoist)
  (:gen-class))

(defn call [nm args]
  (when-let [fun (ns-resolve *ns* (symbol nm))]
    (fun args)))


; to run java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts host1,host2
(defn -main [& args]
  (with-command-line args
      "Gantry"
      [[hosts     "The remote hosts" nil]
       [port      "The ssh port of the remote hosts" 22]
       [file f    "The ssh port of the remote hosts" nil]
       remaining]
    (do 
      (println "hosts: " hosts)
      (println "port: " port)
      (println "file: " file)
      (println "remaining: " remaining)
      (and file (load-file file))
      (doall (map #(call % (re-split #"," hosts)) remaining))
      (println "done ..")
      (System/exit 0))))
