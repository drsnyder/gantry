(ns gantry.run-test
  (:use clojure.test
        [clojure.contrib.condition :only [handler-case raise print-stack-trace *condition*]]
        gantry.run))

(def a-config (create-config 
                (-> (create-resource) 
                  (add "a01.example.com" :tags #{ :master }) 
                  (add "a02.example.com")
                  (add "a03.example.com"))))

(def l-config (create-config 
                (-> (create-resource) 
                  (add "localhost" :tags #{ :master }) 
                  (add "localhost")
                  (add "localhost"))))

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


(deftest update-config-test
          (binding [*config* a-config]
            (with-config (update-config :resource 
                                         (-> (create-resource)
                                           (add "a04.example.com" :tags #{ :a04 })))
                         (is (= (count (get-resource (get-config))) 4))
                         (is (= (count (filter-by-tag (get-resource (get-config)) #{ :a04 })) 1)))))

(deftest config-run-test
         (binding [*config* l-config]
           (let [ret (run "uptime")]
             (is (= (count ret) (count (get-resource (get-config)))))
             )))

(deftest config-run-fail-test
         (binding [*config* l-config]
           (is (= :caught (handler-case :type
                                        (run "exit 1")
                                        (handle :remote-failed
                                                :caught))))))


