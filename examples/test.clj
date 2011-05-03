(println "in test")
(use 'gantry.core)
(use 'gantry.hoist)
(defn deploy [hosts]
  (do (println (str "deploying to " hosts))
    (doall (hoist [hosts] (run "uptime")))
      (println "done")))
