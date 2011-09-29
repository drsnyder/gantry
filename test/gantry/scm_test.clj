(ns gantry.scm-test
  (:use clojure.test
     [clojure.string :only [trim-newline]]
     gantry.core
     gantry.scm.git
     gantry.scm)
  (:require 
     clojure.contrib.io))


(deftest checkout-test
         (local "rm -rf /tmp/gantry.scm.checkout.src")
         (let [c (checkout 
                   "localhost" "git@github.com:drsnyder/gantry.git" 
                   "/tmp/gantry.scm.checkout.src" git-checkout)]
           (is (= (:exit c) 0))
           (is (file-exists "/tmp/gantry.scm.checkout.src/project.clj"))))

(deftest checkout-star-test
         (local "rm -rf /tmp/gantry.scm.checkout.src")
         (let [c (checkout
                   ["localhost"] "git@github.com:drsnyder/gantry.git" 
                   "/tmp/gantry.scm.checkout.src" git-checkout*)]
           (is (= (:exit (first c)) 0))
           (is (file-exists "/tmp/gantry.scm.checkout.src/project.clj"))))

(deftest checkout-branch-test
         (local "rm -rf /tmp/gantry.scm.checkout.src")
         (let [c (checkout 
                   "localhost" "git@github.com:drsnyder/gantry.git" 
                   "/tmp/gantry.scm.checkout.src" 
                   git-checkout "4bc2ed2b83c6dadff4fc1bd05de848ff9b452f4d")]
           (is (= (:exit c) 0))
           (is (file-exists "/tmp/gantry.scm.checkout.src/project.clj")))

         (let [rev (:out (remote "localhost" "cd /tmp/gantry.scm.checkout.src && git rev-parse HEAD"))]
           (is (= (trim-newline rev) "4bc2ed2b83c6dadff4fc1bd05de848ff9b452f4d"))))

            

(deftest checkout-branch-star-test
         (local "rm -rf /tmp/gantry.scm.checkout.src")
         (let [c (checkout 
                   ["localhost"] "git@github.com:drsnyder/gantry.git" 
                   "/tmp/gantry.scm.checkout.src" 
                   git-checkout* "4bc2ed2b83c6dadff4fc1bd05de848ff9b452f4d")]
           (is (= (:exit (first c)) 0))
           (is (file-exists "/tmp/gantry.scm.checkout.src/project.clj")))

         (let [rev (:out (remote "localhost" "cd /tmp/gantry.scm.checkout.src && git rev-parse HEAD"))]
           (is (= (trim-newline rev) "4bc2ed2b83c6dadff4fc1bd05de848ff9b452f4d"))))
