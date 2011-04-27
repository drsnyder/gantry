(ns gantry.core
  (:use [clojure.contrib.condition :only [raise]]
        clojure.contrib.logging
        clojure.set
        clojure.java.io
        clojure.contrib.str-utils)
  (:require clojure.contrib.io
            clojure.contrib.shell))

;(use 'clojure.contrib.condition)
;(use 'clojure.contrib.logging)
;(use 'clojure.contrib.str-utils)
;(require 'clojure.contrib.io)
;(require 'clojure.contrib.shell)


; FIXME: make this (set-log-level! :debug)
; (set-log-level! java.util.logging.Level/ALL) 
(defn set-log-level! [level]
  "Sets the root logger's level, and the level of all of its Handlers, to level.
   Level should be one of the constants defined in java.util.logging.Level."
  (let [logger (impl-get-log "")]
    (.setLevel logger level)
    (doseq [handler (.getHandlers logger)]
      (. handler setLevel level))))



(def *hosts* [])

(defn hash-flip [ht]
  (reduce #(assoc %1 (ht %2) %2) {} (keys ht)))

(defn default-ssh-identity []
   (.getPath (clojure.contrib.io/file (. System getProperty "user.home") ".ssh" "id_dsa")))

(defn logged-in-user [] (. System getProperty "user.name"))

(defn gen-ssh-cmd [& [ id port]] 
    (concat 
      (if id
        ["ssh" "-o" "StrictHostKeyChecking=no" "-i" id]
        ["ssh"])
      (if port
        ["-p" (str port)]
        [])))

        
(defn gen-host-addr [user host]
    (if user
      (str user "@" host)
      host))

(defn agent-pool [aseq]
  (doall (map #(agent %) aseq)))

(defn wait-agent-pool [agents & timeout]
  (if timeout
    (apply await-for timeout agents)
    (apply await-for 100000 agents)))

(defn map-agent-pool [f agents]
  (doseq [a agents] (send-off a f)))

(defn deref-agent-pool [agents]
  (doall (map #(deref %) agents)))

(defn- user [h] (:user h))
(defn- port [h] (:port h))
(defn- ssh-key [h] (:id h))


(defn remote [host cmd & [args]]
  (do (debug (format "==> sending '%s' to h=%s:%s user=%s id=%s" cmd host (port args) (user args) (ssh-key args)))
        (assoc 
          (apply clojure.contrib.shell/sh 
                 (flatten [(gen-ssh-cmd (ssh-key args) (port args)) 
                           (gen-host-addr (user args) host) cmd :return-map true])) :host host)))


(defn remote* [hosts cmd & [args]]
  (let [cf (fn [h] (remote h cmd args)) pool (agent-pool hosts)]
    (do 
      (map-agent-pool cf pool)
      (wait-agent-pool pool)
      (deref-agent-pool pool))))


(defn gen-rsync-cmd [host srcs dest & [args]]
  (if (or (ssh-key args) (port args))
    (let [e-arg (str-join "  " (gen-ssh-cmd (ssh-key args) (port args)))]
      (flatten ["rsync" "-avzL" 
                "-e" e-arg
                srcs (str (gen-host-addr (user args) host) ":" dest)]))
    (flatten ["rsync" "-avzL" 
              srcs (str (gen-host-addr (user args) host) ":" dest)])))
    

(defn upload [host srcs dest & [args]]
  (do (debug (format "==> uploading src %s to h=%s:%s => %s user=%s id=%s" 
                     (str srcs) host (port args) dest (user args) (ssh-key args)))
    (assoc 
      (apply clojure.contrib.shell/sh 
             (flatten [(gen-rsync-cmd host srcs dest args) [:return-map true]])) :host host)))

(defn upload* [hosts srcs dest & [args]]
  (let [cf (fn [h] (upload h srcs dest args)) pool (agent-pool hosts)]
    (do 
      (debug (format "==> uploading src %s to h=%s:%s => %s user=%s id=%s" 
                     (str srcs) (str hosts) (port args) dest (user args) (ssh-key args)))
      (map-agent-pool cf pool)
      (wait-agent-pool pool)
      (deref-agent-pool pool))))


(defn success? [result]
  (= 0 (:exit result)))


(defmacro hoist
  "Hoist some actions on a set of hosts. 

   Examples: 
    (hoist ['newdy.huddler.com', 'rudy.huddler.com']  (run 'uptime') (run 'ls'))
    (hoist ['newdy.huddler.com', 'rudy.huddler.com'] {:id 'path/to/key' :port 22} (run 'uptime') (run 'ls'))
  
   Functions supported: run, push, or create your own!"
  ([hosts args forms]
   (let [ghosts (gensym) gargs (gensym)]
     `(let [~ghosts ~hosts ~gargs ~args]
        ~@(map (fn [f]
                 (if (seq? f)
                   ; run hosts cmd args
                   `(~(first f) ~ghosts ~@(next f) ~gargs)
                   `(~f ~ghosts)))
               forms))))
  ([hosts forms] (hoist hosts {} forms)))



(defn validate-remote [cmd result]
  (if (success? result)
    ; maybe just return result here and let the caller do something with it
    (format "out: %s" (:out result))
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
  (doall (map #(info (validate-remote cmd %)) (remote* hosts cmd args))) hosts)


; works 
;(hoist ["utility001.huddler.com"] {:port 880}
;  (run "uptime")
;  (run "ls -l"))


