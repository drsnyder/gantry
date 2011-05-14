; java -jar gantry-0.0.1-SNAPSHOT-standalone.jar --hosts newdy.huddler.com -f tmp/test.clj  deploy
  
(use 'gantry.core)
(use 'gantry.run)


(defn deploy [config]
  (do 
    (run (get-resource config) "uptime" :tags #{ :master })
    (println "done")) config)

(task date 
  (do 
    (run "date")
    (println "done")))

(defn sethello [config]
  (do 
    (println "sethello!") 
    (assoc config :sethello "bla")))

(defn printconfig [config]
  (do 
    (println (str "printconfig: " config))))

(task updateconfig 
      ; need an add-resource
      (set-resource (get-config) (add (get-resource (get-config)) "remus.huddler.com")))

(task printconfig2
      (println (get-config)))

