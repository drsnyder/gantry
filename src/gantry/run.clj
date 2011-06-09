(ns gantry.run
  (:use [clojure.contrib.condition :only [raise]]
        [clojure.contrib.def :only (defvar)]
        clojure.set
        gantry.core
        gantry.log))

(def *config* {})

(defn get-config [] 
  *config*)

(defn get-resource [config]
  (:resource config))

(defn get-args [config] 
  (:args config))

(defn set-resource [config recs]
  (assoc config :resource recs))

(defn set-args [config args]
  (assoc config :args args))

(defn create-config [recs]
  (set-resource {} recs))

(defn create-resource [] [])


(defn add 
  "Add a host to a resource."
  [recs host & {:keys [tags] :or [tags #{}]}]
  (conj recs {:host host :tags tags}))


(defn match-tag [rec tag]
  (not (empty? (intersection tag (:tags rec)))))

(defn filter-by-tag [recs tag] 
  (filter #(match-tag % tag) recs))

(defn resource-to-hosts [recs & {:keys [tags] :or [tags nil]}]
  (let [frecs (if tags (filter-by-tag recs tags) recs)]
    (doall (reduce #(conj %1 (:host %2)) [] frecs))))

; call from main with specified config :hosts :args
(defmacro with-config [config & body]
  `(binding [*config* ~config]
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
  (let [resource (get-resource (get-config))
        hosts (resource-to-hosts resource :tags tags)]
    (doall 
      (map #(log-multi-line :info (:host %) (validate cmd %)) 
           (remote* hosts cmd (get-args (get-config))))) 
    resource))


;(defn upload* [hosts srcs dest & [args]]
(defn push [srcs dest & {:keys [tags] :or [tags nil]}]
  ; replace info with some kind of logging
  (let [resource (get-resource (get-config))
        hosts (resource-to-hosts resource :tags tags)]
    (doall 
      (map #(log-multi-line :info (:host %) (validate dest %)) 
           (upload* hosts srcs dest (get-args (get-config))))) 
    resource))

