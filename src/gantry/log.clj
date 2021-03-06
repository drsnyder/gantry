(ns gantry.log
  (:use [clojure.string :only [upper-case]]
         clojure.contrib.str-utils))

;; FIXME: make this (set-log-level! :debug)
;; (set-log-level! java.util.logging.Level/ALL) 
;(defn set-log-level! [level]
;  "Sets the root logger's level, and the level of all of its Handlers, to level.
;   Level should be one of the constants defined in java.util.logging.Level."
;  (let [logger (impl-get-log "")]
;    (.setLevel logger level)
;    (doseq [handler (.getHandlers logger)]
;      (. handler setLevel level))))

(def *current-log-level* :info)

(def *levels* {:error 1
               :info  2
               :debug 3})


(defn active-levels []
  (do 
    (let [current *current-log-level*]
      (if (current *levels*)
        (filter #(<= (% *levels*) (current *levels*)) (keys *levels*))
        ()))))

(defn active-level? [level] 
  (> (count (filter #(= level %) (active-levels))) 0))

(defn- log 
  ([level host msg]
   (log level (format "[%s] %s" host msg)))
  ([level msg]
    (and (active-level? level)
      (binding [*out* *err*]
        (println (format "%s %s" (upper-case (name level)) msg))))))

(defn debug 
  ([msg] (log :debug msg))
  ([host msg] (log :debug host msg)))

(defn info
  ([msg] (log :info msg))
  ([host msg] (log :info host msg)))

(defn error
  ([msg] (log :error msg))
  ([host msg] (log :error host msg)))

(defn log-multi-line [level host mlmsg]
  (doall (map #(log level host %) (re-split #"\n" mlmsg))))
  
