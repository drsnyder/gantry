
(use 'gantry.run)

(create-config
 (-> (create-resource) 
  (add "freeshell.net" :tags #{ :master }) 
  (add "sdf.org")))
