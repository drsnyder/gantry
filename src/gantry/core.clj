(ns gantry.core
  (:use [clojure.contrib.condition :only [raise]]
        clojure.contrib.logging
        clojure.java.io
        clojure.contrib.str-utils)
  (:require clojure.contrib.io
            clojure.contrib.shell))

(use 'clojure.contrib.condition)
(use 'clojure.contrib.logging)
(use 'clojure.contrib.str-utils)
(require 'clojure.contrib.io)
(require 'clojure.contrib.shell)


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

(defn remote [host cmd & {:keys [id port user] :or {id nil port nil user nil}}]
  (do (debug (format "==> sending '%s' to h=%s:%s user=%s id=%s" cmd host port user id))
        (assoc 
          (apply clojure.contrib.shell/sh 
                 (flatten [(gen-ssh-cmd id port) (gen-host-addr user host) cmd :return-map true])) :host host)))

(defn remote* [hosts cmd & [ & args]]
  (let [cf (fn [h] (apply remote (filter #(not (nil? %)) (flatten [h cmd args])))) pool (agent-pool hosts)]
    (do 
      (map-agent-pool cf pool)
      (wait-agent-pool pool)
      (deref-agent-pool pool))))


(defn gen-rsync-cmd [host srcs dest & {:keys [id port user] :or {id nil port nil user nil}}]
  (if (or id port)
    (let [e-arg (str-join "  " (gen-ssh-cmd id port))]
      (flatten ["rsync" "-avzL" 
                "-e" e-arg
                srcs (str (gen-host-addr user host) ":" dest)]))
    (flatten ["rsync" "-avzL" 
              srcs (str (gen-host-addr user host) ":" dest)])))
    

(defn upload [host srcs dest & {:keys [id port user] :or {id (default-ssh-identity) port nil user (logged-in-user)}}]
  (assoc (apply clojure.contrib.shell/sh (flatten [(gen-rsync-cmd host srcs dest :id id :user user :port port) [:return-map true]])) :host host))

(defn upload* [hosts srcs dest & [ & args]]
  (let [cf (fn [h] (apply upload (filter #(not (nil? %)) (flatten [h srcs dest args])))) pool (agent-pool hosts)]
    (do 
      (map-agent-pool cf pool)
      (wait-agent-pool pool)
      (deref-agent-pool pool))))


(defn success? [result]
  (= 0 (:exit result)))


(defn test [host args]
  (println (str host " " args)))

(defn create-host
  "Create a host record.
   Example: (def app001 (create-host \"app001\" {:master true}))
  "
  [host & {:keys [id port user tags] :or {id nil port nil user nil tags nil}}]
  {:host host :tags (first tags)})

(defn filter-hosts [hosts f]
  (if f
    (filter f hosts)
    hosts)) 

(defmacro hoist [hosts & forms]
    `(doto ~hosts ~@forms))

;(hoist [(create-host ...)]
;  (run "git ...")
;  (run "cp -r")
;  (run "..."))

(defn validate-remote [cmd result]
  (if (success? result)
    (format "out: %s" (:out result))
    (raise 
      :type :remote-failed
      :message (if (not (empty? (:err result))) 
                        (format "command '%s' failed: %s" cmd (:err result))
                        (format "command '%s' failed with no output" cmd)))))

(defn run 
"Run the given command on the given hosts
Throws an exception when the return code is not zero"
([hosts #^String cmd & [ & args ]] 
 (map #(println (validate-remote cmd %)) (apply remote* (flatten [hosts cmd args]))))
([#^String cmd] (let [hosts *hosts*] (apply run (flatten [hosts cmd])))))



