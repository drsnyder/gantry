(ns gantry.main
  (:use clojure.contrib.command-line
        clojure.contrib.str-utils
        [clojure.contrib.condition :only [handler-case raise print-stack-trace *condition*]]
        gantry.core
        gantry.log
        gantry.run)
  (:gen-class))



(defn call [nm config]
  (when-let [fun (ns-resolve *ns* (symbol nm))]
    (with-config config (fun))))

(defn resolve-targets [file hosts]
  (if file
    (load-file file)
    (create-config (reduce #(add %1 %2) (create-resource) hosts))))

(defn perform-actions [config action-file actions]
  (do
    (load-file action-file)
    (loop [c config as actions]
      (and (not (empty? as))
           (recur (call (first as) c) (rest as))))))
    ;(doall (map #(call % config (:args config)) actions))))


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
      [[hosts       h  "The remote hosts" ""]
       [port           "The ssh port of the remote hosts" 22]
       [ssh-key     k  "SSH key file to use for authentication" nil]
       [action-file f  "The file to load actions from" "gantryfile"]
       [config-file c  "The file to load your configuration from" nil]
       [arg-set  s  "Add arguments to be passed down to the actions" nil]
       actions]
    (do 

      (debug (str "hosts: " hosts))
      (debug (str "port: " port))
      (debug (str "file: " action-file))
      (debug (str "configfile: " config-file))
      (debug (str "config settings: " (merge-settings-to-config {} arg-set)))
      (debug (str "commands: " actions))


      (debug (str "config: " (resolve-targets config-file (re-split #"," hosts))))
      
      (handler-case :type
                    (let [base (resolve-targets config-file (re-split #"," hosts))
                          config (set-args base {:port port})]
                      (perform-actions config action-file actions))
                    (handle :remote-failed
                            (do (println (str "remote failed: \n" (:message *condition*))) 
                              (print-stack-trace *condition*) 
                              (System/exit 1))))


      (debug (str "done .."))

      (System/exit 0))))
