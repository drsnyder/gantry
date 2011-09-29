(ns gantry.scm-git-test
  (:use clojure.test
     [clojure.string :only [trim-newline]]
     gantry.core
     gantry.scm.git)
  (:require 
     clojure.contrib.io))


(deftest gen-clone-cmd-test
         (is (= "git clone -q git@github.com:drsnyder/gantry.git /tmp/gantry.src" 
                (clone-cmd "git@github.com:drsnyder/gantry.git" "/tmp/gantry.src")))
         (is (= "git clone -q git@github.com:drsnyder/gantry.git /tmp/gantry.src && cd /tmp/gantry.src && git checkout -q -b deploy abc" 
                (clone-cmd "git@github.com:drsnyder/gantry.git" "/tmp/gantry.src" "abc"))))
         
(deftest clone-test
         (local "rm -rf /tmp/gantry.src")
         (let [c (clone "localhost" "git@github.com:drsnyder/gantry.git" "/tmp/gantry.src")]
           (is (= (:exit c) 0))
           (is (file-exists "/tmp/gantry.src/project.clj"))))

(deftest checkout-test
         (local "rm -rf /tmp/gantry.checkout.src")
         (let [c (git-checkout "localhost" "git@github.com:drsnyder/gantry.git" "/tmp/gantry.checkout.src")]
           (is (= (:exit c) 0))
           (is (file-exists "/tmp/gantry.checkout.src/project.clj"))))

;
(deftest checkout-branch-test
         (local "rm -rf /tmp/gantry.checkout.src")
         (let [c (git-checkout "localhost" "git@github.com:drsnyder/gantry.git" "/tmp/gantry.checkout.src" "4bc2ed2b83c6dadff4fc1bd05de848ff9b452f4d")]
           (is (= (:exit c) 0))
           (is (file-exists "/tmp/gantry.checkout.src/project.clj")))

         (let [rev (:out (remote "localhost" "cd /tmp/gantry.checkout.src && git rev-parse HEAD"))]
           (is (= (trim-newline rev) "4bc2ed2b83c6dadff4fc1bd05de848ff9b452f4d"))))

            
