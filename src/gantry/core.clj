(ns gantry.core
  (:use [clojure.contrib.condition :only [raise]]
        clojure.set
        clojure.java.io
        clojure.contrib.str-utils
        gantry.log)
  (:require clojure.contrib.io
            clojure.contrib.shell))

;(use 'clojure.contrib.condition)
;(use 'clojure.contrib.logging)
;(use 'clojure.contrib.str-utils)
;(require 'clojure.contrib.io)
;(require 'clojure.contrib.shell)

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
  (do (debug 
        (format "==> sending '%s' to h=%s:%s user=%s id=%s" cmd host (port args) (user args) (ssh-key args)))
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


(defn validate [cmd result]
  (if (success? result)
    ; maybe just return result here and let the caller do something with it
    (:out result)
    (raise 
      :type :remote-failed
      :message (if (not (empty? (:err result))) 
                        (format "command '%s' failed: %s" cmd (:err result))
                        (format "command '%s' failed with no output" cmd)))))
