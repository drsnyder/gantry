
(use 'gantry.run)

(task date
      (run "date"))

(task upload-tests
      (push "test" "tmp"))

(task uptime
      (run "uptime"))

(task hostname
      (run "hostname" :tags #{ :master }))

(task print-internal-config
      (println (get-args (get-config)))
      (println (get-config)))
