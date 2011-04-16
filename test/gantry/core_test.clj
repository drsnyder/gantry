(ns gantry.core-test
  (:use clojure.test
     gantry.core))


(deftest gen-ssh-cmd-test
         (is (= ["ssh"] (gen-ssh-cmd)))
         (is (= ["ssh" "-p" "22"] (gen-ssh-cmd nil 22)))
         (is (= ["ssh" "-o" "StrictHostKeyChecking=no" "-i" "file" "-p" "22" ] (gen-ssh-cmd "file" 22))))
         

(deftest gen-rsync-cmd-test
         (is (= ["rsync" "-avzL" "source-dir" "localhost:dest-dir"] (gen-rsync-cmd "localhost" "source-dir" "dest-dir")))
         (is (= ["rsync" "-avzL" "-e" "ssh  -p  22" "source-dir" "localhost:dest-dir"] 
                (gen-rsync-cmd "localhost" "source-dir" "dest-dir" :port 22))))


