# gantry

Operations support and deployment in clojure. Inspired by [crane](https://github.com/getwoven/crane), 
[capistrano](http://capify.org), and [fabric](http://fabfile.org). 

This is a work in progress. The interfaces may change.

# Quickstart

## Command line tool

Get gantry:

    $ curl -Lk https://github.com/downloads/drsnyder/gantry/gantry > gantry; chmod +x gantry

    $ cat > gantryfile
    (use 'gantry.run)
    (task hello
       (run (format "echo \"hello %s at $(hostname)\"" (:name (get-args (get-config))))))
    ^D

    $ ./gantry -H one.myhost.com,two.myhost.com -t hello -s name=bob 
    INFO [one.myhost.com] hello bob at one.myhost.com
    INFO [two.myhost.com] hello bob at two.myhost.com


Within a task, you have access to all that you normally would in clojure plus two additional 
functions for remote operations on the specified hosts or "resources". You can `run` a 
command on all of the hosts provided on the command line or in your configuration or you 
can `upload` files to the same set of hosts.

Examples:

    (run "yum install htop")
    (upload "files/sudoers" "/etc/sudoers")

If you have a more complicated (or just more) set of hosts, you can greate a task to define your
"resources". Such a task would look like (add this to your gantryfile to test):
    
    (task mysite
      (update-config :resource (-> (create-resource) 
                                (add "one.myhost.com" :tags #{ :master }) 
                                (add "two.myhost.com")))))

With this defined, you can then run gantry like so to load up your configuration before any tasks
are run:

    $ ./gantry -t mysite,hello -s name=bob

## Remote execution 

The gantry.core library contains several functions for executing commands remotely via ssh. The 
functionality described above is based off of this library. The two primary functions are `remote` 
and `upload`, with `remote*` and `upload*` being multi-host versions of the same. The multi-host
versions execute each command concurrently on each host using the clojure agents facility and 
`send-off`.

To use the core library, add `[gantry "0.0.1-SNAPSHOT"]` to the dependencies in your project.clj.

Examples:

    (remote "host.com" "yum install -y atop")
    (remote "host.com" "yum install -y atop" {:user "deployer"})

    (remote* ["host.com", "host2.com"] "yum install -y atop" {:user "deployer"})

    (upload "host.com" "filea" "/tmp")
    (upload "host.com" ["filea", "fileb"] "/tmp" {:port 222})

    (upload* ["host.com", "host2.com"] ["filea", "fileb"] "/tmp" {:id "/home/deployer/my-key"})


See the source code for more documentation.
    
## TODO
 + clean up import/requires
 + download/*
 + capture?

## DONE
 + local exec
 + process the results of a command as soon as they are ready. don't wait for the whole agent pool to be derefed (using future insteaf of agents)

## License

Copyright (c) 2011 Damon Snyder 

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

