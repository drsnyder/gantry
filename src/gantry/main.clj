(ns gantry.main
  (:use clojure.contrib.command-line
        clojure.contrib.str-utils
        [clojure.contrib.condition :only [handler-case raise print-stack-trace *condition*]]
        gantry.core
        gantry.log
        gantry.run)
  (:gen-class))

(defn call [nm hosts args]
  (when-let [fun (ns-resolve *ns* (symbol nm))]
    (with-resource hosts args (fun))))

(defn resolve-targets [file hosts]
  (if file
    (load-file file)
    {:hosts hosts}))

(defn perform-actions [config action-file actions]
  (do
    (load-file action-file)
    (doall 
      (map #(call % (:hosts config) (:args config)) actions))))


(defn error-exit [msg]
   (binding [*out* *err*]
     (do 
       (print msg)
       (System/exit 1))))


; to run java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts host1,host2
;
(defn -main [& args]
  (with-command-line args
      "Gantry"
      [[hosts          "The remote hosts" nil]
       [port           "The ssh port of the remote hosts" 22]
       [ssh-key     k  "SSH key file to use for authentication" nil]
       [action-file f  "The file to load actions from" "gantryfile"]
       [config-file c  "The file to load your configuration from" nil]
       actions]
    (do 

      (debug (str "hosts: " hosts))
      (debug (str "port: " port))
      (debug (str "file: " action-file))
      (debug (str "configfile: " config-file))
      (debug (str "commands: " actions))

      
      (handler-case :type
                    (perform-actions (resolve-targets config-file (re-split #"," hosts)) action-file actions)
                    (handle :remote-failed
                            (do (print-stack-trace *condition*) (System/exit 1))))


      (debug (str "done .."))

      (System/exit 0))))
