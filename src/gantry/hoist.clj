(ns gantry.hoist
  (:use [clojure.contrib.condition :only [raise]]
        gantry.core
        gantry.log))

(defn success? [result]
  (= 0 (:exit result)))


(defmacro hoist
  "Hoist some actions on a set of hosts. 

   Examples: 
    (hoist ['newdy.huddler.com', 'rudy.huddler.com'] (run 'uptime') (run 'ls'))
  
   Functions supported: run, push, or create your own!"
  [hosts & forms]
   (let [ghosts (gensym) gargs (gensym)]
     `(let [~ghosts ~hosts ~gargs nil]
        ~@(map (fn [f]
                 (if (seq? f)
                   ; run hosts cmd args
                   `(~(first f) ~ghosts ~@(next f) ~gargs)
                   `(~f ~ghosts)))
               forms))))



(defn validate-remote [cmd result]
  (if (success? result)
    ; maybe just return result here and let the caller do something with it
    (:out result)
    (raise 
      :type :remote-failed
      :message (if (not (empty? (:err result))) 
                        (format "command '%s' failed: %s" cmd (:err result))
                        (format "command '%s' failed with no output" cmd)))))

(defn run 
  "Run the given command on the given hosts.
   Example: (run ['host1', 'host2'] 'uptime') 
   Throws an exception when the return code is not zero"
  [hosts cmd & [args]] 
  ; replace info with some kind of logging
  (doall (map #(log-multi-line :info (:host %) (validate-remote cmd %)) (remote* hosts cmd args))) hosts)


; works 
;(hoist ["utility001.huddler.com"] 
;  (run "uptime")
;  (run "ls -l"))
