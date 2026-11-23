(ns zenthyr.utils-test
  (:require [clojure.test :refer :all]
            [zenthyr.utils :refer :all])
  (:import [java.io ByteArrayOutputStream PrintStream]))

(deftest test-find-available-port
  (testing "Finds an available port"
    (let [port (find-available-port 8000)]
      (is (integer? port))
      (is (>= port 8000))))
  
  (testing "Skips occupied ports"
    (let [socket (java.net.ServerSocket. 0)
          port (.getLocalPort socket)]
      (try
        (let [found-port (find-available-port port)]
          (is (not= port found-port))
          (is (> found-port port)))
        (finally
          (.close socket))))))

(deftest test-suppress-stderr-logging!
  (testing "Suppresses specified log messages"
    (let [original-err System/err
          baos (ByteArrayOutputStream.)
          ps (PrintStream. baos)]
      (try
        (System/setErr ps)
        (suppress-stderr-logging!)
        
        ;; Should be suppressed
        (.println System/err "Exception in thread \"AppKit Thread\"")
        (.println System/err "process_requirement.cc:165")
        (.println System/err "Signature validation failed")
        
        ;; Should be allowed
        (.println System/err "This is a valid error")
        
        (let [output (.toString baos "UTF-8")]
          (is (not (.contains output "AppKit Thread")))
          (is (not (.contains output "process_requirement")))
          (is (not (.contains output "Signature validation")))
          (is (.contains output "This is a valid error")))
        
        (finally
          (System/setErr original-err))))))

(deftest preserves-partial-output-and-writes-byte-ranges
  (let [original System/err
        saved (ByteArrayOutputStream.)]
    (try
      (System/setErr (PrintStream. saved))
      (suppress-stderr-logging!)
      (.write System/err (.getBytes "first" "UTF-8"))
      (.write System/err (int \space))
      (is (= "" (.toString saved "UTF-8")))
      (let [second (.getBytes "xline\nsecond\n" "UTF-8")]
        (.write System/err second 1 (dec (alength second))))
      (is (= "first line\nsecond\n" (.toString saved "UTF-8")))
      (finally
        (System/setErr original)))))
