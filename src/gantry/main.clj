(ns gantry.main
  (:use clojure.set
        clojure.contrib.str-utils
        clojure.contrib.java-utils
        [clojure.contrib.condition :only [handler-case raise print-stack-trace *condition*]]
        gantry.core
        gantry.log
        gantry.run)
  (:require [clargon.core :as c])
  (:gen-class))


(def valid-log-levels #{ "debug" "info" "error" })

(defn call [nm config]
  (when-let [fun (ns-resolve *ns* (symbol nm))]
    (with-config config (fun))))

(defn hosts-to-config [hosts]
  (if (not (empty? hosts))
    (create-config (reduce #(add %1 %2) (create-resource) hosts))
    (create-config [])))

(defn perform-actions [config action-file actions]
  (do
    (load-file action-file)
    (loop [c config as actions]
      (and (not (empty? as))
           (recur (call (first as) c) (rest as))))))

(defn file-exists [path]
  (. (clojure.contrib.java-utils/file path) exists))

(defn valid-str [host]
  (> (count host) 0))

(defn valid-opts [opts]
  (and
    (or (file-exists (:gantryfile opts)) (not (= (count (:hosts opts)) 0)))
    (> (count (filter valid-str (:tasks opts))) 0)))

(defn print-error [msg & exit]
   (binding [*out* *err*]
     (do 
       (println)
       (println msg)
       (and exit (System/exit 1)))))

(def gantry-options [(c/optional ["-H" "--hosts"      "The hosts to run the tasks on" :default ""] #(vec (filter valid-str (.split % ","))))
                     (c/optional ["-p" "--port"       "The ssh port to connect to on the remote hosts" :default 22])
                     (c/optional ["-k" "--ssh-key"    "The ssh key to use to connect to on the remote hosts" :default nil])
                     (c/optional ["-f" "--gantryfile" "Load the tasks from this file" :default "gantryfile"])
                     (c/optional ["-l" "--loglevel"   "Set the log level" :default "info"] #(if (not (empty? (intersection #{ % } valid-log-levels))) (keyword %) :info))
                     (c/optional ["-t" "--tasks"      "The tasks to run" :default ""] #(vec (.split % ",")))
                     (c/optional ["-s" "--args"       "Arguments to be passed down to the tasks. Takes the form key=value[,key=value]" :default ""] #(if (> (count %) 0) (merge-settings {} %) {}))])

  

; to run java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts host1,host2
;
(defn -main [& args]
  (let [opts (apply c/clargon (cons args (flatten gantry-options)))]

    (do

      (if (valid-opts opts)
        ; FIXME: move this binding out of here
        (binding [*current-log-level* (:loglevel opts)]
                  (do 
                    (debug "opts: " opts)

                    (handler-case :type
                                  (let [base (hosts-to-config (:hosts opts))
                                        config (set-args base (merge {:port (:port opts) :ssh-key (:ssh-key opts)} (:args opts)))]
                                    (perform-actions config (:gantryfile opts) (:tasks opts)))
                                  (handle :remote-failed
                                          (do (print-error (str "remote failed: \n" (:message *condition*))) 
                                            (print-stack-trace *condition*) 
                                            (System/exit 1))))))
        (do 
          (print-error "Error, we need either -f|--gantryfile, a gantryfile or -H and -t") 
          (c/show-help (flatten (map #(% "" args) gantry-options)))))
        
        ; FIXME: why do we need this?
        (System/exit 0))))
