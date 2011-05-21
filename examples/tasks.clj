
(use 'gantry.run)

(task date
      (run "date"))

(task upload-tests
      (push "tests" "tmp"))
