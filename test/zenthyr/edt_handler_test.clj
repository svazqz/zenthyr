(ns zenthyr.edt-handler-test
  (:require [clojure.test :refer :all]
            [zenthyr.edt-handler :refer :all])
  (:import [zenthyr.edt_handler EDTExceptionHandler]))

(deftest test-suppression-logic
  (testing "Should suppress AppKit Thread exceptions"
    (let [handler (EDTExceptionHandler.)
          output-stream (java.io.ByteArrayOutputStream.)
          print-stream (java.io.PrintStream. output-stream)
          original-err System/err]
      (try
        (System/setErr print-stream)
        
        ;; Case 1: Ignored exception
        (.uncaughtException handler (Thread/currentThread) 
                           (Exception. "Exception in thread \"AppKit Thread\""))
        (is (= "" (.toString output-stream)))
        
        ;; Case 2: Ignored process_requirement
        (.reset output-stream)
        (.uncaughtException handler (Thread/currentThread) 
                           (Exception. "process_requirement.cc:165"))
        (is (= "" (.toString output-stream)))
        
        ;; Case 3: Real exception (should print)
        (.reset output-stream)
        (.uncaughtException handler (Thread/currentThread) 
                           (Exception. "Real Error"))
        (is (not= "" (.toString output-stream)))
        
        (finally
          (System/setErr original-err))))))
