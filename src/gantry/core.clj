(ns gantry.core
  (:use [clojure.contrib.condition :only [raise]]
        clojure.contrib.logging
        clojure.java.io)
  (:require clojure.contrib.io
            clojure.contrib.shell))


(defn hash-flip [ht]
  (reduce #(assoc %1 (ht %2) %2) {} (keys ht)))

(defn default-ssh-identity []
   (.getPath (clojure.contrib.io/file (. System getProperty "user.home") ".ssh" "id_dsa")))

(defn logged-in-user [] (. System getProperty "user.name"))

;(defn send-commands [host commands & {:keys [id] :or {id nil}} ]

(defn rsync-cmd 
  [host srcs dest & {:keys [key-path user] :or {key-path (default-ssh-identity) user (logged-in-user)}}]
  (if key-path
    (let [e-arg (format "ssh -o StrictHostKeyChecking=no -i %s" key-path)]
      (flatten ["rsync" "-avzL" "--delete"
                "-e" e-arg
                srcs (str user "@" host ":" dest)]))
    (flatten ["rsync" "-avzL" "--delete"
              srcs (str user "@" host ":" dest)])))

(defn ssh-cmd 
  [host cmd & {:keys [key-path user] :or {key-path (default-ssh-identity) user (logged-in-user)}}]
  (if key-path
    (let [ssh-cmd-str ["ssh" "-o" "StrictHostKeyChecking=no" "-i" key-path]]
      (flatten [ssh-cmd-str (str user "@" host) cmd]))
    (flatten ["ssh" (str user "@" host) cmd])))

;(apply sh (ssh-cmd "newdy.huddler.com" "ls"))
