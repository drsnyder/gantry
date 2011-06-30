(ns gantry.core-test
  (:use clojure.test
     gantry.core))


(deftest gen-ssh-cmd-test
         (is (= ["ssh"] (gen-ssh-cmd)))
         (is (= ["ssh" "-p" "22"] (gen-ssh-cmd nil 22)))
         (is (= ["ssh" "-o" "StrictHostKeyChecking=no" "-i" "file" "-p" "22" ] (gen-ssh-cmd "file" 22))))
         

(deftest gen-rsync-cmd-test
         (is (= ["rsync" "-avzL" "source-dir" "localhost:dest-dir"] (gen-rsync-cmd "localhost" "source-dir" "dest-dir")))
         (is (= ["rsync" "-avzL" "-e" "ssh  -p  22" "source-dir" "localhost:dest-dir"] (gen-rsync-cmd "localhost" "source-dir" "dest-dir" {:port 22}))))


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

         (let [bad-port (remote "localhost" "echo -n 'no sshd on 23'" { :port 23 })]
           (is (= (:exit bad-port) 255))
           (is (= (:err bad-port) "ssh: connect to host localhost port 23: Connection refused\r\n")))

         (let [bad-key (remote "localhost" "echo -n 'no such key'" { :id "bla" })]
           (is (= (:exit bad-key) 0))
           (is (= (:err bad-key) "Warning: Identity file bla not accessible: No such file or directory.\n"))))
