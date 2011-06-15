(ns gantry.run
  (:use [clojure.contrib.condition :only [raise]]
        [clojure.contrib.def :only (defvar)]
        clojure.contrib.str-utils
        clojure.set
        gantry.core
        gantry.log))

(def *resource* [])
(def *config* {})
(def *args* nil)


(defn get-resource [config]
  (:resource config))

(defn set-resource [config recs]
  (assoc config :resource recs))

(defn create-config [recs]
  (set-resource {} recs))

(defn get-config [] *config*)

(defn get-args [] *args*)

(defn split-config-set [setting] 
  (let [tokens (re-split #"=" setting)] 
    [(keyword (first tokens)) (second tokens)])) 

(defn merge-settings-to-config [config settings]
  (assoc config :args (reduce #(assoc %1 (first (split-config-set %2)) (second (split-config-set %2))) config (re-split #"," settings))))

(defn create-resource [] [])

(defn add [recs host & {:keys [tags] :or [tags #{}]}]
  (conj recs {:host host :tags tags}))

(defn match-tag [rec tag]
  (not (empty? (intersection tag (:tags rec)))))

(defn filter-by-tag [recs tag] 
  (filter #(match-tag % tag) recs))

(defn resource-to-hosts [recs]
  (doall (reduce #(conj %1 (:host %2)) [] recs)))

; call from main with specified config :hosts :args
(defmacro with-args [args & body]
  `(binding [*args* ~args]
     (do ~@body)))

(defmacro task [sym & forms]
  ; do a def in here that defines the function with one parameter
  (let [gconfig (gensym) gret (gensym)]
    `(defn ~sym [~gconfig] 
       (binding [*config* ~gconfig] 
         (let [~gret (do ~@forms)]
           (if (= (type ~gret) clojure.lang.PersistentArrayMap)
             ~gret
             (get-config)))))))

(defn run [cmd & {:keys [tags] :or [tags nil]}]
  ; replace info with some kind of logging
  (let [config (get-config)
        resource (get-resource config)
        hosts (resource-to-hosts 
                (if tags 
                  (filter-by-tag resource tags) 
                  resource))]
    (doall 
      (map #(log-multi-line :info (:host %) (validate cmd %)) 
           (remote* hosts cmd *args*))) 
    resource))


;(defn upload* [hosts srcs dest & [args]]
(defn push [srcs dest & {:keys [tags] :or [tags nil]}]
  ; replace info with some kind of logging
  (let [config (get-config)
        resource (get-resource config)
        hosts (resource-to-hosts 
                (if tags 
                  (filter-by-tag resource tags) 
                  resource))]
    (doall 
      (map #(log-multi-line :info (:host %) (validate dest %)) 
           (upload* hosts srcs dest *args*))) 
    resource))

