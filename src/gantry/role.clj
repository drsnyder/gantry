(ns gantry.role
  (:use gantry.core))





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


    
