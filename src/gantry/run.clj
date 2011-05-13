(ns gantry.run
  (:use [clojure.contrib.condition :only [raise]]
        [clojure.contrib.def :only (defvar)]
        clojure.set
        gantry.core
        gantry.log))

(def *resource* [])
(def *args* nil)

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
(defmacro with-resource [recs args & body]
  `(binding [*resource* ~recs *args* ~args]
     (do ~@body)))


(defn run [cmd & {:keys [tags] :or [tags nil]}]
  ; replace info with some kind of logging
  (let [hosts (resource-to-hosts (if tags (filter-by-tag *resource* tags) *resource*))]
    (doall (map #(log-multi-line :info (:host %) (validate cmd %)) (remote* hosts cmd *args*))) hosts))



;(with-resource hosts args
;               ; call user specified functions which call run
;               )
