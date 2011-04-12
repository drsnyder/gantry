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
    (if (not (nil? id)) 
      (add-identity id) 
      (add-identity (default-ssh-identity)))
    (let [session (session host :strict-host-key-checking :no)]
      (with-connection session
         (loop [cmds commands results []]
           (if (nil? (first cmds)) 
             results
             (let [ret (ssh session (first cmds))]
               (if (not (= (first ret) 0))
                 (throw (Exception. (format "Remote cmd '%s' failed" (first cmds))))
                 (recur (rest cmds) (conj results (second ret)))))))))))

(defn send-command [host command & {:keys [id] :or {id nil}} ]
  (send-commands host [command] :id id))

(defn remote [#^String host #^String cmd] (send-command host cmd))

(defn rsync-cmd
  [{:keys [key-path host user srcs dest]}]
  (if key-path
    (let [e-arg (format "ssh -o StrictHostKeyChecking=no -i %s" key-path)]
      (flatten ["rsync" "-avzL" "--delete"
                "-e" e-arg
                srcs (str user "@" host ":" dest)]))
    (flatten ["rsync" "-avzL" "--delete"
              srcs (str user "@" host ":" dest)])))
