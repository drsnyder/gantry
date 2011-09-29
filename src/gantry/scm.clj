(ns gantry.scm
  (:use gantry.core))

(defn checkout
  [hosts src dst scm &[branch]] 
  (scm hosts src dst branch))
