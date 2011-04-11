(ns gantry.core
  (:use [clojure.contrib.condition :only [raise]]
        [clojure.pprint :only [pprint]]
        clj-ssh.ssh
        clojure.contrib.logging
        clojure.java.io)
  (:require clojure.contrib.io)
  (:import com.jcraft.jsch.JSch
           com.jcraft.jsch.Logger))

(def ^{:dynamic true} *ssh-log-levels*
  {com.jcraft.jsch.Logger/DEBUG :debug
   com.jcraft.jsch.Logger/INFO  :info
   com.jcraft.jsch.Logger/WARN  :warn
   com.jcraft.jsch.Logger/ERROR :error
   com.jcraft.jsch.Logger/FATAL :fatal})

(deftype SshLogger
         [log-level]
         com.jcraft.jsch.Logger
         (isEnabled
           [_ level]
           (>= level log-level))
         (log
           [_ level message]
           (clojure.contrib.logging/log (*ssh-log-levels* level) message nil "clj-ssh.ssh")))

(JSch/setLogger (SshLogger. com.jcraft.jsch.Logger/FATAL))

(defn hash-flip [ht]
  (reduce #(assoc %1 (ht %2) %2) {} (keys ht)))

(defn set-ssh-log-level! [level]
  (JSch/setLogger (SshLogger. ((hash-flip *ssh-log-levels*) level))))

(defn default-ssh-identity []
   (.getPath (clojure.contrib.io/file (. System getProperty "user.home") ".ssh" "id_dsa")))

(defn send-commands [host commands & {:keys [id] :or {id nil}} ]
  (with-ssh-agent [false]
    (println (str "here" (nil? id)))
    (if (not (nil? id)) 
      (add-identity id) 
      (do (println "inside") (add-identity (default-ssh-identity)) (println "after")))

    (let [session (session host :strict-host-key-checking :no)]
      (with-connection session
         (loop [cmds commands results []]
           (if (nil? (first cmds)) 
             results
             (recur (rest cmds) (conj results (second (ssh session (first cmds)))))))))))

(defn send-command [host command & {:keys [id] :or {id nil}} ]
  (send-commands host [command] :id id))
