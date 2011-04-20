(ns gantry.role
  (:use gantry.core))



(defn create-host
  "Create a host record.
   Example: (def app001 (create-host \"app001\" {:master true}))
  "
  [host & tags]
  {:host host :tags (first tags)})

(defn create-role
  "Create a role.
   Example: (def web (create-role [(create-host \"host1\" {:master true}) (create-host \"host2\" {:thumb true})])) 
  "
  [hosts & tags]
  {:hosts hosts :tags (first tags)})


(defn filter-hosts [hosts f]
  (if f
    (filter f hosts)
    hosts)) 


;;;;;;;;;;;;;;;;;
; one possible way
(with-role web
           (run "git ...")
           (run "cp -r")
           (run "ln -s")
           (run "touch ..." :when (fn [keys] (:master keys)))
           )

(def *role* nil)

(defmacro with-role
  ([role-binding & body]
   `(binding [*role* ~role-binding]
      (do ~@body))))

(defn run [cmd]
  (loop [hosts (*role* :hosts)]
    (let [host (first hosts)]
      (when (not (nil? host))
        (do (println (str "=> " (send-command host cmd)))
          (recur (rest hosts)))))))

;;;;;;;;;;;;;;;;;;;
; another possibility
(defmacro hoist [role & forms]
    `(doto ~role ~@forms))

(hoist (create-role ...)
  (run "git ...")
  (run "cp -r")
  (run "..."))

(defn run 
"Run the given command on the role.
Throws an exception when the return code is not zero"
[role] 
    )


    
