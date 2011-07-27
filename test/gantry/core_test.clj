(ns gantry.core-test
  (:use clojure.test
     gantry.core)
  (:require 
     clojure.contrib.io))


(deftest gen-ssh-cmd-test
         (is (= ["ssh"] (gen-ssh-cmd)))
         (is (= ["ssh" "-p" "22"] (gen-ssh-cmd nil 22)))
         (is (= ["ssh" "-o" "StrictHostKeyChecking=no" "-i" "file" "-p" "22" ] (gen-ssh-cmd "file" 22))))
         

(deftest gen-rsync-cmd-test
         (is (= ["rsync" "-avzL" "source-dir" "localhost:dest-dir"] (gen-rsync-cmd "localhost" "source-dir" "dest-dir")))
         (is (= ["rsync" "-avzL" "-e" "ssh  -p  22" "source-dir" "localhost:dest-dir"] (gen-rsync-cmd "localhost" "source-dir" "dest-dir" {:port 22}))))


; you need to make sure you have enabled ssh on your localhost
(deftest remote-test
         (let [uptime (remote "localhost" "uptime")]
           (is (= (:exit uptime) 0))
           (is (> (count (:out uptime)) 0)))

         (let [hello (remote "localhost" "echo -n 'hello'")]
           (is (= (:exit hello) 0))
           (is (= (:out hello) "hello")))

         (let [hello (remote "localhost" "echo -n 'hello, world'" { :port 22 })]
           (is (= (:exit hello) 0))
           (is (= (:out hello) "hello, world")))

         (let [hello (remote "localhost" "echo -n 'hello, world user'" { :user (logged-in-user) })]
           (is (= (:exit hello) 0))
           (is (= (:out hello) "hello, world user")))

         (let [bad-port (remote "localhost" "echo -n 'no sshd on 23'" { :port 23 })]
           (is (= (:exit bad-port) 255))
           (is (= (:err bad-port) "ssh: connect to host localhost port 23: Connection refused\r\n")))

         (let [bad-key (remote "localhost" "echo -n 'no such key'" { :id "bla" })]
           (is (= (:exit bad-key) 0))
           (is (= (:err bad-key) "Warning: Identity file bla not accessible: No such file or directory.\n"))))


(deftest remote*-test
         (let [uptimes (remote* ["localhost" "localhost"] "uptime")]
           (is (= (count uptimes) 2))
           (is (= (reduce #(+ %1 (:exit %2)) 0 uptimes) 0))))


(deftest upload-test
         (local "rm -rf /tmp/gantry-tests")
         (let [up (upload "localhost" "LICENSE" "/tmp/gantry-tests")]
           (is (= (:exit up) 0))
           (is (file-exists "/tmp/gantry-tests/LICENSE")))

         (local "rm -rf /tmp/gantry-tests")
         (let [ups (upload "localhost" ["LICENSE" "project.clj"] "/tmp/gantry-tests")]
           (is (= (:exit ups) 0))
           (is (file-exists "/tmp/gantry-tests/LICENSE"))
           (is (file-exists "/tmp/gantry-tests/project.clj")))

         (local "rm -rf /tmp/gantry-tests")
         (let [bad-up (upload "localhost" "file-does-not-exist" "/tmp/gantry-tests")]
           (is (= (:exit bad-up) 23))
           (is (not (file-exists "/tmp/gantry-tests/LICENSE")))))
         
(deftest upload*-test
         (local "rm -rf /tmp/upload-star-test")
         (let [res (upload* ["localhost" "localhost"] "test" "/tmp/upload-star-test")]
           (is (= (count res) 2))
           (is (= (reduce #(+ %1 (:exit %2)) 0 res) 0))))

