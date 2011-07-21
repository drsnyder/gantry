(ns gantry.run
  (:use [clojure.contrib.condition :only [raise]]
        [clojure.contrib.def :only (defvar)]
        clojure.contrib.str-utils
        clojure.set
        gantry.core
        gantry.log))

; TODO: pull, capture

(def *config* {})

(defn get-config [] 
  *config*)

(defn get-resource [config]
  (:resource config))

(defn get-args [config] 
  (:args config))

(defn set-resource [config recs]
  (assoc config :resource recs))

(defn set-args [config args]
  (assoc config :args args))

(defn create-config [recs]
  (set-resource {} recs))


(defn get-config [] *config*)

; FIXME: we need a way to merge configs within a task. OTW, your args will get
; clobbered

(defn update-config [ & {:keys [resource args] :or {resource nil args nil}}]
  (let [config (set-resource 
                 (get-config) 
                 (if resource 
                   (reduce #(conj %1 %2) (get-resource (get-config)) resource) 
                   (get-resource (get-config))))]
    (set-args config (merge (get-args config) args))))


(defn split-argument-set 
  "Arguments passed into gantry on the command line are specified as key=val 
  pairs that are comma separated like so commit=abc123,no_purge=true."
  [setting] 
  (let [tokens (re-split #"=" setting)] 
    [(keyword (first tokens)) (second tokens)])) 

(defn merge-arguments 
  "Merge the parsed key=value arguments into an existing hash map."
  [config settings]
  (if settings
    (merge config 
           (reduce #(assoc %1 (first (split-argument-set %2)) (second (split-argument-set %2))) 
                   config (re-split #"," settings)))
    config))

(defn create-resource [] [])

(defn add 
  "Add a host to a resource. Specify tags by adding :tags #{}."
  [recs host & {:keys [tags] :or [tags #{}]}]
  (conj recs {:host host :tags tags}))


(defn match-tag 
  "Determines if the specified tag matches the resource."
  [rec tag]
  (not (empty? (intersection tag (:tags rec)))))

(defn filter-by-tag 
  "Filter resources by a given tag."
  [recs tag] 
  (filter #(match-tag % tag) recs))

(defn resource-to-hosts 
  "Convert a resource to a sequence of hosts. Does the tag filtering of the resources."
  [recs & {:keys [tags] :or [tags nil]}]
  (let [frecs (if tags (filter-by-tag recs tags) recs)]
    (doall (reduce #(conj %1 (:host %2)) [] frecs))))


; call from main with specified config :hosts :args
(defmacro with-config 
  "Enclose a set of tasks or other operations with the given config."
  [config & body]
  `(binding [*config* ~config]
     (do ~@body)))


(defmacro task 
  "This function should be used in your gantryfile to generate tasks that can selectively
  be invoked from gantry.

  Creates a task function with the definition (def ~sym [ & config]). Checks to see if an
  clojure.lang.PersistentArrayMap is being returned and if not, returns the current config. 
  This allows for the chaining of tasks that alter the configuration as they are passed through 
  the pipeline.

  For example, you might call tasks apps,setup,deploy to configure your app servers, setup the install
  of your application and deploy it. The apps task can create and return the resource configuration which
  will then be passed along to the subsequent tasks.

  Example:

  (task uptime
    (run \"uptime\"))

  You could also nest tasks within your gantryfile. For example, if you wanted to call deploy only, you could

    (task deploy
      (let [config (apps)]
        (do 
          (setup config)
          (deploy config))))
  "
  [sym & forms]
  ; do a def in here that defines the function with one parameter
  (let [gconfig (gensym) gret (gensym)]
    `(defn ~sym [ & ~gconfig] 
       (binding [*config* (if ~gconfig ~gconfig (get-config))] 
         (let [~gret (do ~@forms)]
           (if (= (type ~gret) clojure.lang.PersistentArrayMap)
             ~gret
             (get-config)))))))


(defn run 
  "Should be run within a task or with-config.
  
  Invoke the supplied cmd on the hosts specified in your resource definition. If you want to filter the command
  based on a set of tags, supply :tags #{}. For example:

  (run 'yum install httpd httpd-devel' :tags #{ :app })

  This will run the supplied command only on the resources tagged with :app.
  "
  [cmd & {:keys [tags] :or [tags nil]}]
  ; replace info with some kind of logging
  (let [resource (get-resource (get-config))
        hosts (resource-to-hosts resource :tags tags)]
      (doall (remote* hosts cmd (merge (get-args (get-config)) { :cb (fn [h] (log-multi-line :info (:host h) (validate cmd h)))} )))))


(defn push 
  "Should be run within a task or with-config.

  Pushes srcs to dest on the hosts specified in your resource definition. If you want to filter the command
  based on a set of tags, supply :tags #{}. For example:

  (push [\"filea\" \"fileb\"] \"/tmp\")
  "
  [srcs dest & {:keys [tags] :or [tags nil]}]
  ; replace info with some kind of logging
  (let [resource (get-resource (get-config))
        hosts (resource-to-hosts resource :tags tags)]
    (doall (upload* hosts srcs dest 
                    (merge (get-args (get-config)) 
                           {:cb (fn [h] (log-multi-line :info (:host h) (validate dest h)))})))))

