(ns baby.pat.supatom.s3
          (:require [baby.pat.simplaws :as aws :refer [aws-clients]]
                    [baby.pat.simplaws.s3 :as s3]
                    [baby.pat.supatom]
                    [baby.pat.vt :as vt]
                    [orchestra.core :refer [defn-spec]]
                    [clojure.edn]
                    [medley.core]))

(defn-spec commit! ::vt/discard
  "Commits a value to an object."
  ([client ::vt/any id ::vt/str contents ::vt/any]
   (s3/put-object aws-clients client-variant bucket id contents)
   ))

(defn-spec snapshot ::vt/discard
  "Returns a snapshot of an object."
  ([id ::vt/qkw] (snapshot supatom-variants :file/default id))
  ([variant ::vt/qkw id ::vt/qkw] (snapshot supatom-variants variant id))
  ([universe ::vt/map variant ::vt/qkw id ::vt/qkw]
   (let [bucket (vt/<- universe [:supatom-variant/id variant :supatom-variant/connection :bucket])
         client-variant (vt/<- universe [:supatom-variant/id variant :supatom-variant/client])]
     (if (s3/object-exists? aws-clients client-variant bucket id)
       (s3/get-object aws-clients client-variant bucket id)
       (commit! universe variant id "nil")))))

(defn default-supatom-s3-commit-with [{:keys [id variant write-with source-atom]}]
  (commit! variant (vt/safe-kws id) (write-with @source-atom)))

(defn default-supatom-s3-snapshot-with [{:keys [id variant read-with]}]
  (let [ss (snapshot variant (vt/safe-kws id))
        _ (tap> {:ss ss :ty (type ss)})]
    (read-with (slurp ss))))

(defmethod baby.pat.supatom/commit-with :s3/default [supatom]
  (default-supatom-s3-commit-with supatom))
(defmethod baby.pat.supatom/snapshot-with :s3/default [supatom]
  (default-supatom-s3-snapshot-with supatom))

(defmethod baby.pat.supatom/commit-with :lts/dev [supatom]
  (default-supatom-s3-commit-with supatom))
(defmethod baby.pat.supatom/snapshot-with :lts/dev [supatom]
  (default-supatom-s3-snapshot-with supatom))

(def supatom-s3-default-overlay {:variant :s3/default
                                 :write-with str
                                 :read-with clojure.edn/read-string})

(def supatom (baby.pat.supatom/*supatom supatom-s3-default-overlay))

(defn-spec checkout ::vt/supatom [stage ::vt/kw-or-str dt ::vt/kw-or-str]
  (supatom {:id (keyword (vt/singular dt) "id")
            :variant :dt/directory
            :connection {:bucket (vt/safe-kws (keyword "cache" (name stage)))}}))

(comment
  (def ccc (supatom {:id :dumb/luck
                     :variant :lts/dev
                     :backing :s3}))

  @ccc
;
  )
