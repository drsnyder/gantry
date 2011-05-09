; java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts newdy.huddler.com -f tmp/test.clj  deploy
  
(use 'gantry.core)
(use 'gantry.hoist)

(defn deploy [hosts args]
  (do (println (str "deploying to " hosts))
    ; go back to hoist {} forms
    (doall (hoist hosts args (run "uptime")))
      (println "done")))

(defn hello [hosts args]
  (println "hello!"))
