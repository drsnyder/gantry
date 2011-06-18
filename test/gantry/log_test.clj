(ns gantry.log-test
  (:use clojure.test
     gantry.log))


(deftest active-level-test

         (binding [*current-log-level* (keyword "debug")]
           (is (:error :info :debug) (active-levels)))
           
         (binding [*current-log-level* (keyword "info")]
           (is (:error :info :debug) (active-levels)))

         (binding [*current-log-level* (keyword "error")]
           (is (:error :info :debug) (active-levels))))

