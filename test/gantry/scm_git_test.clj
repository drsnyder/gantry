(ns gantry.scm-git-test
  (:use clojure.test
     gantry.core
     [gantry.scm.git :as git])
  (:require 
     clojure.contrib.io))


(deftest gen-clone-cmd-test
         (is (= "git clone -q git@github.com:drsnyder/gantry.git /tmp/gantry.src" 
                (git/clone-cmd "git@github.com:drsnyder/gantry.git" "/tmp/gantry.src")))
         (is (= "git clone -q git@github.com:drsnyder/gantry.git /tmp/gantry.src && cd /tmp/gantry.src && git checkout -q -b deploy abc" 
                (git/clone-cmd "git@github.com:drsnyder/gantry.git" "/tmp/gantry.src" "abc"))))
         
(deftest clone-test
         (local "rm -rf /tmp/gantry.src")
         (let [c (git/clone "localhost" "git@github.com:drsnyder/gantry.git" "/tmp/gantry.src")]
           (is (= (:exit c) 0))
           (is (file-exists "/tmp/gantry.src/project.clj"))))

(deftest checkout-test
         (local "rm -rf /tmp/gantry.checkout.src")
         (let [c (git/checkout "localhost" "git@github.com:drsnyder/gantry.git" "/tmp/gantry.checkout.src")]
           (is (= (:exit c) 0))
           (is (file-exists "/tmp/gantry.checkout.src/project.clj"))))
