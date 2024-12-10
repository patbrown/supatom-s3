(ns baby.pat.supatom.s3
          (:require [baby.pat.simplaws :as aws]
                    [baby.pat.simplaws.s3 :as s3]
                    [baby.pat.supatom]
                    [baby.pat.jes.vt :as vt]
                    [baby.pat.jes.vt.util :as u]
                    [orchestra.core :refer [defn-spec]]
                    [clojure.edn]
                    [medley.core]))

(defn-spec commit! ::vt/discard
  "Commits a value to an object."
  ([client ::vt/any bucket ::vt/str id ::vt/str contents ::vt/any]
   (s3/put-object client bucket id contents)))

(defn-spec snapshot ::vt/discard
  "Returns a snapshot of an object."
  ([client ::vt/any bucket ::vt/str id ::vt/qkw]
   (if (s3/object-exists? client bucket id)
       (s3/get-object client bucket id)
       (commit! client bucket id "nil"))))

(defn default-supatom-s3-commit-with [{:keys [id connection write-with source-atom]}]
  (let [{:keys [client bucket]} connection]
    (commit! client bucket (u/safe-kws id) (write-with @source-atom))))

(defn default-supatom-s3-snapshot-with [{:keys [id connection read-with]}]
  (let [{:keys [client bucket]} connection]
    (read-with (snapshot client bucket (u/safe-kws id)))))

(defmethod baby.pat.supatom/commit-with :s3/default [supatom]
  (default-supatom-s3-commit-with supatom))
(defmethod baby.pat.supatom/snapshot-with :s3/default [supatom]
  (default-supatom-s3-snapshot-with supatom))

(def supatom-s3-default-overlay {:variant :s3/default
                                 :write-with str
                                 :read-with clojure.edn/read-string})

(defn supatom [config]
  (let [{:keys [connection] :as config} (merge supatom-s3-default-overlay config)
        {:keys [client bucket]} connection
        _ (when-not (s3/bucket-exists? client bucket)
            (s3/create-bucket client bucket))]
    (baby.pat.supatom/supatom-> config)))

(comment
  (def ccc (supatom {:id :dumb/luck
                     :variant :lts/dev
                     :backing :s3}))

  @ccc
;
  )
