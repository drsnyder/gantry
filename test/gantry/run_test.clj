(ns gantry.run-test
  (:use clojure.test
     gantry.run))

(def a-config (create-config 
                (-> (create-resource) 
                  (add "a01.example.com" :tags #{ :master }) 
                  (add "a02.example.com")
                  (add "a03.example.com"))))

(deftest create-resource-test
         (is (empty? (get-resource (create-config (create-resource)))))
         (is (not (empty? (get-resource a-config)))))

(deftest filter-by-tags-test
         (is (= ["a01.example.com" "a02.example.com" "a03.example.com"] (resource-to-hosts (get-resource a-config))))
         (is (= ["a01.example.com"] (resource-to-hosts (get-resource a-config) :tags #{ :master }))))

(deftest merge-arguments-test
         (let [ret (merge-arguments {} "commit=abc123,file=bla")]
           (is (= (:commit ret) "abc123"))
           (is (= (:file ret) "bla"))))


