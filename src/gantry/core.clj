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

;(defn send-commands [host commands & {:keys [id] :or {id nil}} ]


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

(defn remote [host cmd & {:keys [id port user] :or {id nil port nil user nil}}]
  (apply clojure.contrib.shell/sh (flatten [(gen-ssh-cmd id port) (gen-host-addr user host) cmd :return-map true])))

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


