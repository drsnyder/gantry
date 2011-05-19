(ns gantry.run-test
  (:use clojure.test
     gantry.run))

(deftest create-resource-test
         (is (empty? (get-resource (create-config (create-resource)))))
         (is (not (empty? (get-resource (create-config 
                                          (-> (create-resource) 
                                            (add "newdy.huddler.com" :tags #{ :master }) 
                                            (add "rudy.huddler.com"))))))))


