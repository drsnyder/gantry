(ns gantry.hoist
  (:use [clojure.contrib.condition :only [raise]]
        gantry.core
        gantry.log))

(defmacro hoist
  "Hoist some actions on a set of hosts. 

   Examples: 
    (hoist ['newdy.huddler.com', 'rudy.huddler.com'] {} (run 'uptime') (run 'ls'))
  
   Functions supported: run, push, or create your own!"
  [hosts args & forms]
   (let [ghosts (gensym) gargs (gensym)]
     `(let [~ghosts ~hosts ~gargs ~args]
        ~@(map (fn [f]
                 (if (seq? f)
                   `(~(first f) ~ghosts ~@(next f) ~gargs)
                   `(~f ~ghosts)))
               forms))))




(defn run 
  "Run the given command on the given hosts.
   Example: (run ['host1', 'host2'] 'uptime') 
   Throws an exception when the return code is not zero"
  [hosts cmd & [args]] 
  ; replace info with some kind of logging
  (doall (map #(log-multi-line :info (:host %) (validate cmd %)) (remote* hosts cmd args))) hosts)


; works 
;(hoist ["utility001.huddler.com"] {}
;  (run "uptime")
;  (run "ls -l"))
