(ns gantry.core
  (:use [clojure.contrib.condition :only [raise]]
        clojure.contrib.logging
        clojure.java.io
        clojure.contrib.str-utils)
  (:require clojure.contrib.io
            clojure.contrib.shell))


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
  (assoc (apply clojure.contrib.shell/sh (flatten [(gen-ssh-cmd id port) (gen-host-addr user host) cmd :return-map true])) :host host))

(defn premote [hosts cmd & [ & args]]
  (let [cf (fn [h] (apply remote (filter #(not (nil? %)) (flatten [h cmd args])))) pool (agent-pool hosts)]
    (do 
      (map-agent-pool cf pool)
      (wait-agent-pool pool)
      (deref-agent-pool pool))))

;(defn ptest-keys [host cmd & {:keys [id port user] :or {id nil port nil user nil}}]
;  (println (format "h=%s id=%s port=%s user=%s" host id port user)))
;
;(defn ptest-outer [host cmd & [& args]]
;    (do (println (flatten [host cmd args]))
;      (apply ptest-keys (filter #(not (nil? %)) (flatten [host cmd args])))))

(defn gen-rsync-cmd [host srcs dest & {:keys [id port user] :or {id nil port nil user nil}}]
  (if (or id port)
    (let [e-arg (str-join "  " (gen-ssh-cmd id port))]
      (flatten ["rsync" "-avzL" 
                "-e" e-arg
                srcs (str (gen-host-addr user host) ":" dest)]))
    (flatten ["rsync" "-avzL" 
              srcs (str (gen-host-addr user host) ":" dest)])))
    

(defn upload [host srcs dest & {:keys [id port user] :or {id (default-ssh-identity) port nil user (logged-in-user)}}]
  (apply clojure.contrib.shell/sh (flatten [(gen-rsync-cmd host srcs dest :id id :user user :port port) [:return-map true]])))


(defn success? [result]
  (= 0 (:exit result)))


