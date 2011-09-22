(ns gantry.scm.git
  (:use gantry.core))

(defn clone-cmd
  "Generate a git clone command with the given src repo and dst directory."
  [src dst &[branch]]
  (if branch
    (format "%s && cd %s && git checkout -q -b deploy %s" (clone-cmd src dst) dst branch)
    (format "git clone -q %s %s" src dst)))

(defn clone*
  [hosts src dst &[branch]]
  (remote* hosts (clone-cmd src dst branch)))


(defn clone
  "Clone repo src to dst on host."
  [host src dst &[branch]]
  (first (clone* [host] src dst branch)))

(def checkout clone)
(def checkout* clone*)
