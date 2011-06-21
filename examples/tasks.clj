
(use 'gantry.run)

(task freeshell
      (create-config
        (-> (create-resource) 
          (add "freeshell.net" :tags #{ :master }) 
          (add "sdf.org"))) )

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
