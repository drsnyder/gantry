(ns gantry.main
  (:use clojure.contrib.str-utils
        clojure.contrib.java-utils
        [clojure.contrib.condition :only [handler-case raise print-stack-trace *condition*]]
        gantry.core
        gantry.log
        gantry.run)
  (:require [clargon.core :as c])
  (:gen-class))



(defn call [nm config]
  (when-let [fun (ns-resolve *ns* (symbol nm))]
    (with-config config (fun))))

(defn resolve-targets [file hosts]
  (if (not (empty? hosts))
    (create-config (reduce #(add %1 %2) (create-resource) hosts))
    (load-file file)))

(defn perform-actions [config action-file actions]
  (do
    (load-file action-file)
    (loop [c config as actions]
      (and (not (empty? as))
           (recur (call (first as) c) (rest as))))))
    ;(doall (map #(call % config (:args config)) actions))))

(defn file-exists [path]
  (. (clojure.contrib.java-utils/file path) exists))


(defn print-error [msg & exit]
   (binding [*out* *err*]
     (do 
       (print msg)
       (and exit (System/exit 1)))))

(def gantry-options [(c/optional ["-H" "--hosts"      "The hosts to run the tasks on" :default []] #(vec (.split % ",")))
                     (c/optional ["-p" "--port"       "The ssh port to connect to on the remote hosts" :default 22])
                     (c/optional ["-k" "--ssh-key"    "The ssh key to use to connect to on the remote hosts" :default nil])
                     (c/optional ["-f" "--gantryfile" "Load the tasks from this file" :default "gantryfile"])
                     (c/optional ["-t" "--tasks"      "The tasks to run" :default []] #(vec (.split % ",")))
                     (c/optional ["-c" "--config"     "Load the configuration / resources from this file" :default nil])
                     (c/optional ["-s" "--args"       "Arguments to be passed down to the tasks. Takes the form key=value[,key=value]" :default {}] #(merge-settings {} %))])

  

; to run java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts host1,host2
;
(defn -main [& args]
  (let [opts (apply c/clargon (cons args (flatten gantry-options)))]

    (do
      ;(println "Parsed opts: " opts)

      (if (or (file-exists (:gantryfile opts)) (not (empty? (:hosts opts))))
        (do 
          (handler-case :type
                        (let [base (resolve-targets (:config opts) (:hosts opts))
                              config (set-args base (merge {:port (:port opts) :ssh-key (:ssh-key opts)} (:args opts)))]
                          (perform-actions config (:gantryfile opts) (:tasks opts)))
                        (handle :remote-failed
                                (do (print-error (str "remote failed: \n" (:message *condition*))) 
                                  (print-stack-trace *condition*) 
                                  (System/exit 1)))))
        (do 
          (print-error "Error, we need either -f|--gantryfile, a gantryfile or -H") 
          (c/show-help (flatten (map #(% "" args) gantry-options)))))
        
        ; FIXME: why do we need this?
        (System/exit 0))))
