(ns cnc.repo
  (:require [cnc.eval-map :refer [find-fn]]
            [full.async :refer [<??]]
            [hasch.core :refer [uuid]]
            [konserve.memory :refer [new-mem-store]]
            [konserve.filestore :refer [new-fs-store]]
            [konserve.protocols :refer [-get-in -assoc-in]]
            [replikativ.core :refer [server-peer client-peer]]
            [replikativ.stage :as s]
            [replikativ.p2p.fetch :refer [fetch]]
            [replikativ.p2p.hash :refer [ensure-hash]]
            [replikativ.p2p.block-detector :refer [block-detector]]
            [replikativ.platform :refer [create-http-kit-handler! start]]
            [replikativ.platform-log :refer [debug]]
            [clojure.core.async :refer [>!! chan go-loop <!]]
            [datomic.api :as d] ;; to read schema file id literals
            ))


(defn init-repo [config]
  (let [{:keys [user repo branches store remote peer]} config
        store (<?? (new-fs-store store))
        err-ch (chan)
        peer-server (server-peer (create-http-kit-handler! peer err-ch) ;; TODO client-peer?
                                 "benjamin"
                                 store
                                 err-ch
                                 (comp (partial block-detector :peer-core)
                                       (partial fetch store err-ch)
                                       ensure-hash
                                       (partial block-detector :p2p-surface)))
        #_(client-peer "benjamin"
                       store err-ch
                       (comp (partial fetch store err-ch)
                             ensure-hash))
        stage (<?? (s/create-stage! user peer-server err-ch eval))
        res {:store store
             :peer peer-server
             :err-ch err-ch
             :stage stage
             :id repo}]
    (go-loop [e (<! err-ch)]
      (when e
        (println "TOP-LEVEL error:" e)
        (recur (<! e))))

    (when-not (= peer :client)
      (start peer-server))

    (when remote
      (try
        (<?? (s/connect! stage remote))
        (catch Exception e
          (debug "Initial connection failed: " e))))

    (when repo
      (<?? (s/subscribe-repos! stage {user {repo branches}})))
    res))


(comment
  (do
    (require '[cnc.core :refer [state]])
    (require '[replikativ.crdt.repo.stage :as rs])
    (def stage (get-in @state [:repo :stage]))
    (def store (get-in @state [:repo :store]))
    (def repo-id (get-in @state [:repo :id])))
  (clojure.pprint/pprint @stage)



  (<?? (s/connect! stage "ws://127.0.0.1:31744"))
  ;; initialization steps
  (def new-id (<?? (rs/create-repo! stage :description "ev-cd experiments.")))

  (<?? (s/subscribe-repos! stage {"weilbach@dopamine.kip" {#uuid "64588c27-362f-40d0-9b95-63dc03072033"  #{#_"calibrate" "master" #_"train current rbms" #_"sample"}}}))

  (clojure.pprint/pprint (<?? (-get-in store [#uuid "214e7e59-8ba0-543a-9ea3-7076bb1f518b"])))
  (clojure.pprint/pprint (<?? (-get-in store [#uuid "059280a0-3682-592b-bac4-99ba12795972"])))

  (<?? (rs/transact stage ["weilbach@dopamine.kip" repo-id "master"]
                    [[(find-fn 'create-db)
                      {:name "ev-experiments"}]
                     [(find-fn 'transact-schema)
                      (-> "resources/schema.edn"
                          slurp
                          read-string)]]))

  (<?? (rs/commit! stage {"weilbach@dopamine.kip" {repo-id #{"master"}}}))


  (<?? (rs/branch! stage
                   ["weilbach@dopamine.kip" repo-id]
                   "sample"
                   (first (get-in @stage ["weilbach@dopamine.kip" repo-id :state :branches "master"]))))

  (<?? (-assoc-in store ["schema"] (read-string (slurp "resources/schema.edn"))))

  (type (<?? (-get-in store [["weilbach@dopamine.kip" repo-id]]))))
