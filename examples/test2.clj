; java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts newdy.huddler.com -f tmp/test.clj  deploy
  
(use 'gantry.core)
(use 'gantry.run)

(defn deploy []
  (do 
    (run "uptime")
    (println "done")))

(defn hello []
  (println "hello!"))
