;; gorilla-repl.fileformat = 1

;; **
;;; # Small Bars no bias
;;; Learning rate 1e-8
;; **

;; @@
(ns small-bars-no-bias
  (:require [gorilla-plot.core :as plot]
            [gg4clj.core :as gg4clj]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [cnc.analytics :as a]
            [incanter.stats :as s]
            [cnc.execute :as exe]
            [clojure.pprint :refer [pprint]]
            [hasch.core :refer [uuid]]
            [boltzmann.matrix :refer [full-matrix]]
            [clojure.java.shell :refer [sh]]
            [cnc.core :refer [state]]
            [cnc.execute :refer [slurp-bytes]]
            [clojure.core.matrix :as mat]
            [clj-hdf5.core :as hdf5]
            [konserve.protocols :refer [-get-in -bget -exists?]]
            [geschichte.platform :refer [<!?]]
            [boltzmann.core :refer [train-cd sample-gibbs]]
            [boltzmann.theoretical :refer [create-theoretical-rbm]]
            [boltzmann.formulas :as f]
            [boltzmann.protocols :refer [-weights -biases]]
            [boltzmann.visualize :as v]
            [clojure.core.matrix :refer [dot matrix]]
            [datomic.api :as d]
            [clojure.core.async :refer [chan] :as async]
            [quil.core :as q]))
(def store (get-in @state [:repo :store]))
(def states (apply cartesian-product (repeat 9 [0 1])))
(def conn (a/conn "train current rbms4"))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/conn</span>","value":"#'small-bars-no-bias/conn"}
;; <=

;; **
;;; ## Experiments without bias
;; **

;; @@
(->> (d/q '[:find ?vls ?w-hist ?b-hist
       :where
       [?exp :ref/data #uuid "0b619370-6716-5ae8-89b6-9c38b4e11e94"]
       [?exp :ref/trans-params ?vid]
       [?exp :ref/training-params ?train-params-id]
       [(cnc.analytics/load-key ?vid) ?vls]
       [(:base-directory ?vls) ?base-dir]
       [(.contains ?base-dir #_"experiments/Thu Mar 19 13:08" "experiments/Fri Mar 20 18")]
       [(:output ?vls) ?out]
       [(:weight_theo_history.h5 ?out) ?w-hist]
       [(:bias_theo_history.h5 ?out) ?b-hist]]
     (d/db conn))
     (map (fn [[p w b]]
               (let [c (/ (-> (a/get-hdf5-tensor store w "/weight") first count) 2)
                     vc (- (-> (a/get-hdf5-tensor store b "/weight") first count) 
                           (-> p :exp-params :training-params :h_count))
                     wh (mapv #(->> % 
                                    (drop c) 
                                    (partition vc)
                                    (mapv vec))
                              (take-nth 10
                                        (a/get-hdf5-tensor store w "/weight")))
                     vbh (mapv #(->> % (take vc) vec) 
                               (take-nth 10
                                         (a/get-hdf5-tensor store b "/weight")))
                     hbh (mapv #(->> % (drop vc) vec) 
                               (take-nth 10
                                         (a/get-hdf5-tensor store b "/weight")))]
               (assoc p 
                 :restricted-weights (last wh)
                 :weight-history wh
                 
                 :v-biases (last vbh)
                 :v-bias-history vbh
                 :h-biases (last hbh)
                 :h-bias-history hbh))))
     (def no-bias-exps))
(-> no-bias-exps first :exp-params)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:git-commit-id</span>","value":":git-commit-id"},{"type":"html","content":"<span class='clj-string'>&quot;e1ea0ba05b8d949c0d3915d791948084c30901e8&quot;</span>","value":"\"e1ea0ba05b8d949c0d3915d791948084c30901e8\""}],"value":"[:git-commit-id \"e1ea0ba05b8d949c0d3915d791948084c30901e8\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:training-params</span>","value":":training-params"},{"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:h_count</span>","value":":h_count"},{"type":"html","content":"<span class='clj-long'>5</span>","value":"5"}],"value":"[:h_count 5]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:dt</span>","value":":dt"},{"type":"html","content":"<span class='clj-double'>0.1</span>","value":"0.1"}],"value":"[:dt 0.1]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:epochs</span>","value":":epochs"},{"type":"html","content":"<span class='clj-long'>500</span>","value":"500"}],"value":"[:epochs 500]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:burn_in_time</span>","value":":burn_in_time"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:burn_in_time 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:learning_rate</span>","value":":learning_rate"},{"type":"html","content":"<span class='clj-double'>5.0E-7</span>","value":"5.0E-7"}],"value":"[:learning_rate 5.0E-7]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:phase_duration</span>","value":":phase_duration"},{"type":"html","content":"<span class='clj-double'>2000.0</span>","value":"2000.0"}],"value":"[:phase_duration 2000.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:weight_recording_interval</span>","value":":weight_recording_interval"},{"type":"html","content":"<span class='clj-double'>100.0</span>","value":"100.0"}],"value":"[:weight_recording_interval 100.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:sim_setup_kwargs</span>","value":":sim_setup_kwargs"},{"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:grng_seed</span>","value":":grng_seed"},{"type":"html","content":"<span class='clj-long'>42</span>","value":"42"}],"value":"[:grng_seed 42]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:rng_seeds_seed</span>","value":":rng_seeds_seed"},{"type":"html","content":"<span class='clj-long'>42</span>","value":"42"}],"value":"[:rng_seeds_seed 42]"}],"value":"{:grng_seed 42, :rng_seeds_seed 42}"}],"value":"[:sim_setup_kwargs {:grng_seed 42, :rng_seeds_seed 42}]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:stdp_burnin</span>","value":":stdp_burnin"},{"type":"html","content":"<span class='clj-double'>5.0</span>","value":"5.0"}],"value":"[:stdp_burnin 5.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:bias_learning_rate</span>","value":":bias_learning_rate"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:bias_learning_rate 0.0]"}],"value":"{:h_count 5, :dt 0.1, :epochs 500, :burn_in_time 0.0, :learning_rate 5.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 42, :rng_seeds_seed 42}, :stdp_burnin 5.0, :bias_learning_rate 0.0}"}],"value":"[:training-params {:h_count 5, :dt 0.1, :epochs 500, :burn_in_time 0.0, :learning_rate 5.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 42, :rng_seeds_seed 42}, :stdp_burnin 5.0, :bias_learning_rate 0.0}]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:calibration-id</span>","value":":calibration-id"},{"type":"html","content":"<span class='clj-unkown'>#uuid &quot;22f685d0-ea7f-53b5-97d7-c6d6cadc67d3&quot;</span>","value":"#uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\""}],"value":"[:calibration-id #uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:init-rweights</span>","value":":init-rweights"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"}],"value":"[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"}],"value":"[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"}],"value":"[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"}],"value":"[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"},{"type":"html","content":"<span class='clj-double'>1.0E-5</span>","value":"1.0E-5"}],"value":"[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]"}],"value":"[[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]]"}],"value":"[:init-rweights [[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]]]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:init-biases</span>","value":":init-biases"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>-1.5341301556155136</span>","value":"-1.5341301556155136"},{"type":"html","content":"<span class='clj-double'>-9.510913440211544</span>","value":"-9.510913440211544"},{"type":"html","content":"<span class='clj-double'>-2.0159380056943514</span>","value":"-2.0159380056943514"},{"type":"html","content":"<span class='clj-double'>1.2062999975130388</span>","value":"1.2062999975130388"},{"type":"html","content":"<span class='clj-double'>-2.7633099136703025</span>","value":"-2.7633099136703025"},{"type":"html","content":"<span class='clj-double'>0.8206392901391488</span>","value":"0.8206392901391488"},{"type":"html","content":"<span class='clj-double'>1.2050512968752354</span>","value":"1.2050512968752354"},{"type":"html","content":"<span class='clj-double'>-2.7620516045991153</span>","value":"-2.7620516045991153"},{"type":"html","content":"<span class='clj-double'>0.8192053284323875</span>","value":"0.8192053284323875"},{"type":"html","content":"<span class='clj-double'>0.7027248426055048</span>","value":"0.7027248426055048"},{"type":"html","content":"<span class='clj-double'>1.2372358615489172</span>","value":"1.2372358615489172"},{"type":"html","content":"<span class='clj-double'>0.8801590566333531</span>","value":"0.8801590566333531"},{"type":"html","content":"<span class='clj-double'>0.8040422074136998</span>","value":"0.8040422074136998"},{"type":"html","content":"<span class='clj-double'>0.9519249106300683</span>","value":"0.9519249106300683"}],"value":"[-1.5341301556155136 -9.510913440211544 -2.0159380056943514 1.2062999975130388 -2.7633099136703025 0.8206392901391488 1.2050512968752354 -2.7620516045991153 0.8192053284323875 0.7027248426055048 1.2372358615489172 0.8801590566333531 0.8040422074136998 0.9519249106300683]"}],"value":"[:init-biases [-1.5341301556155136 -9.510913440211544 -2.0159380056943514 1.2062999975130388 -2.7633099136703025 0.8206392901391488 1.2050512968752354 -2.7620516045991153 0.8192053284323875 0.7027248426055048 1.2372358615489172 0.8801590566333531 0.8040422074136998 0.9519249106300683]]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:data-id</span>","value":":data-id"},{"type":"html","content":"<span class='clj-unkown'>#uuid &quot;0b619370-6716-5ae8-89b6-9c38b4e11e94&quot;</span>","value":"#uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\""}],"value":"[:data-id #uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:source-path</span>","value":":source-path"},{"type":"html","content":"<span class='clj-string'>&quot;/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py&quot;</span>","value":"\"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\""}],"value":"[:source-path \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:args</span>","value":":args"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;srun-log&quot;</span>","value":"\"srun-log\""},{"type":"html","content":"<span class='clj-string'>&quot;python&quot;</span>","value":"\"python\""},{"type":"html","content":"<span class='clj-string'>&quot;/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py&quot;</span>","value":"\"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\""}],"value":"[\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]"}],"value":"[:args [\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]]"}],"value":"{:git-commit-id \"e1ea0ba05b8d949c0d3915d791948084c30901e8\", :training-params {:h_count 5, :dt 0.1, :epochs 500, :burn_in_time 0.0, :learning_rate 5.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 42, :rng_seeds_seed 42}, :stdp_burnin 5.0, :bias_learning_rate 0.0}, :calibration-id #uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\", :init-rweights [[1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5] [1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5 1.0E-5]], :init-biases [-1.5341301556155136 -9.510913440211544 -2.0159380056943514 1.2062999975130388 -2.7633099136703025 0.8206392901391488 1.2050512968752354 -2.7620516045991153 0.8192053284323875 0.7027248426055048 1.2372358615489172 0.8801590566333531 0.8040422074136998 0.9519249106300683], :data-id #uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\", :source-path \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\", :args [\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]}"}
;; <=

;; **
;;; ## Full experiments
;; **

;; @@
(->> (d/q '[:find ?vls ?w-hist ?b-hist
       :where
       [?exp :ref/data #uuid "0b619370-6716-5ae8-89b6-9c38b4e11e94"]
       [?exp :ref/trans-params ?vid]
       [?exp :ref/training-params ?train-params-id]
       [(cnc.analytics/load-key ?vid) ?vls]
       [(:base-directory ?vls) ?base-dir]
       [(.contains ?base-dir "experiments/Fri Mar 20")]
       [(:output ?vls) ?out]
       [(:weight_theo_history.h5 ?out) ?w-hist]
       [(:bias_theo_history.h5 ?out) ?b-hist]]
     (d/db conn))
     (map (fn [[p w b]]
               (let [c (/ (-> (a/get-hdf5-tensor store w "/weight") first count) 2)
                     vc (- (-> (a/get-hdf5-tensor store b "/weight") first count) 
                           (-> p :exp-params :training-params :h_count))
                     wh (mapv #(->> % 
                                    (drop c) 
                                    (partition vc)
                                    (mapv vec))
                              (take-nth 10
                                        (a/get-hdf5-tensor store w "/weight")))
                     vbh (mapv #(->> % (take vc) vec) 
                               (take-nth 10
                                         (a/get-hdf5-tensor store b "/weight")))
                     hbh (mapv #(->> % (drop vc) vec) 
                               (take-nth 10
                                         (a/get-hdf5-tensor store b "/weight")))]
               (assoc p 
                 :restricted-weights (last wh)
                 :weight-history wh
                 
                 :v-biases (last vbh)
                 :v-bias-history vbh
                 :h-biases (last hbh)
                 :h-bias-history hbh))))
     (def exps))
(-> exps first :exp-params)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:git-commit-id</span>","value":":git-commit-id"},{"type":"html","content":"<span class='clj-string'>&quot;30a7cf5efbc21cffd282a115c3787a389d26d460&quot;</span>","value":"\"30a7cf5efbc21cffd282a115c3787a389d26d460\""}],"value":"[:git-commit-id \"30a7cf5efbc21cffd282a115c3787a389d26d460\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:training-params</span>","value":":training-params"},{"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:h_count</span>","value":":h_count"},{"type":"html","content":"<span class='clj-long'>5</span>","value":"5"}],"value":"[:h_count 5]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:dt</span>","value":":dt"},{"type":"html","content":"<span class='clj-double'>0.1</span>","value":"0.1"}],"value":"[:dt 0.1]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:epochs</span>","value":":epochs"},{"type":"html","content":"<span class='clj-long'>1000</span>","value":"1000"}],"value":"[:epochs 1000]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:burn_in_time</span>","value":":burn_in_time"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:burn_in_time 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:learning_rate</span>","value":":learning_rate"},{"type":"html","content":"<span class='clj-double'>1.0E-7</span>","value":"1.0E-7"}],"value":"[:learning_rate 1.0E-7]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:phase_duration</span>","value":":phase_duration"},{"type":"html","content":"<span class='clj-double'>2000.0</span>","value":"2000.0"}],"value":"[:phase_duration 2000.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:weight_recording_interval</span>","value":":weight_recording_interval"},{"type":"html","content":"<span class='clj-double'>100.0</span>","value":"100.0"}],"value":"[:weight_recording_interval 100.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:sim_setup_kwargs</span>","value":":sim_setup_kwargs"},{"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:grng_seed</span>","value":":grng_seed"},{"type":"html","content":"<span class='clj-long'>52</span>","value":"52"}],"value":"[:grng_seed 52]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:rng_seeds_seed</span>","value":":rng_seeds_seed"},{"type":"html","content":"<span class='clj-long'>52</span>","value":"52"}],"value":"[:rng_seeds_seed 52]"}],"value":"{:grng_seed 52, :rng_seeds_seed 52}"}],"value":"[:sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:stdp_burnin</span>","value":":stdp_burnin"},{"type":"html","content":"<span class='clj-double'>5.0</span>","value":"5.0"}],"value":"[:stdp_burnin 5.0]"}],"value":"{:h_count 5, :dt 0.1, :epochs 1000, :burn_in_time 0.0, :learning_rate 1.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}, :stdp_burnin 5.0}"}],"value":"[:training-params {:h_count 5, :dt 0.1, :epochs 1000, :burn_in_time 0.0, :learning_rate 1.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}, :stdp_burnin 5.0}]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:calibration-id</span>","value":":calibration-id"},{"type":"html","content":"<span class='clj-unkown'>#uuid &quot;22f685d0-ea7f-53b5-97d7-c6d6cadc67d3&quot;</span>","value":"#uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\""}],"value":"[:calibration-id #uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:data-id</span>","value":":data-id"},{"type":"html","content":"<span class='clj-unkown'>#uuid &quot;0b619370-6716-5ae8-89b6-9c38b4e11e94&quot;</span>","value":"#uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\""}],"value":"[:data-id #uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:source-path</span>","value":":source-path"},{"type":"html","content":"<span class='clj-string'>&quot;/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py&quot;</span>","value":"\"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\""}],"value":"[:source-path \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:args</span>","value":":args"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;srun-log&quot;</span>","value":"\"srun-log\""},{"type":"html","content":"<span class='clj-string'>&quot;python&quot;</span>","value":"\"python\""},{"type":"html","content":"<span class='clj-string'>&quot;/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py&quot;</span>","value":"\"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\""}],"value":"[\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]"}],"value":"[:args [\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]]"}],"value":"{:git-commit-id \"30a7cf5efbc21cffd282a115c3787a389d26d460\", :training-params {:h_count 5, :dt 0.1, :epochs 1000, :burn_in_time 0.0, :learning_rate 1.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}, :stdp_burnin 5.0}, :calibration-id #uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\", :data-id #uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\", :source-path \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\", :args [\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]}"}
;; <=

;; @@
(def bars (<!? (-bget store #uuid "0b619370-6716-5ae8-89b6-9c38b4e11e94"  #(-> % :input-stream slurp read-string))))
bars
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"}],"value":"[1 0 0 1 0 0 1 0 0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"}],"value":"[0 0 1 0 0 1 0 0 1]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>1</span>","value":"1"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-long'>0</span>","value":"0"}],"value":"[1 1 1 0 0 0 0 0 0]"}],"value":"[[1 0 0 1 0 0 1 0 0] [0 0 1 0 0 1 0 0 1] [1 1 1 0 0 0 0 0 0]]"}
;; <=

;; @@
(defn setup []
  (q/smooth)                          ;; Turn on anti-aliasing
  (q/frame-rate 60)                    ;; Set framerate to 1 FPS
  (q/background 200 100 100))


(defn draw-rects ([pixel-matrix] (draw-rects pixel-matrix 255))
  ([pixel-matrix alpha]
  (let [hc (count (first pixel-matrix))
        vc (count pixel-matrix)
        w (/ (q/width) hc)
        h (/ (q/height) vc)]
    (doseq [x (range hc)
            y (range vc)]
      (q/fill (* (last (v/grayscale (get-in pixel-matrix [y x]))) 255) alpha)
      (q/rect (* x w) (* y h) w h)))))

(def weight-atom (atom (-> no-bias-exps first :weight-history cycle) #_trad-receptive-fields))
(def v-bias-atom (atom (-> no-bias-exps first :bias-history cycle)))

(q/defsketch example                  ;; Define a new sketch named example
  :title "Receptive fields"    ;; Set the title of the sketch
  :setup setup                        ;; Specify the setup fn
  :draw (fn draw []
          (let [fw (first @weight-atom)
                fb (repeat 14 0) #_(first @bias-atom)]
            (when fw
              (q/stroke 255 0 0 0)
              (q/stroke-weight 3)
            (->> fw
                  (v/receptive-fields 3)
                  (v/tile 5) 
                  (mapv vec) 
                  draw-rects)
              (q/stroke 255 0 0 255)
              (q/stroke-weight 3)
              (draw-rects (mapv vec (partition 5 (drop 9 fb))) 140)
            (swap! weight-atom rest)
              #_(swap! bias-atom rest))))                          ;; Specify the draw fn
  :size [500 100])                    ;; You struggle to beat the golden ratio
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/example</span>","value":"#'small-bars-no-bias/example"}
;; <=

;; @@
(defn dist-theo [{:keys [restricted-weights v-biases h-biases]}]
  (let [ev-rbm (create-theoretical-rbm restricted-weights v-biases h-biases)]
    (reduce (fn [f s] (update-in f [s] 
                             (fnil + 0)
                             (f/prob-visi ev-rbm s)))
        {}
     (f/state-space 9))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/dist-theo</span>","value":"#'small-bars-no-bias/dist-theo"}
;; <=

;; @@
(let [no-bias-probs (map (fn [exp] (map (dist-theo exp) states)) no-bias-exps)
      no-bias-avg (map s/mean (mat/transpose no-bias-probs))
      no-bias-sd (map s/sd (mat/transpose no-bias-probs))
      probs (map (fn [exp] (map (dist-theo exp) states)) exps)
      avg (map s/mean (mat/transpose probs))
      sd (map s/sd (mat/transpose probs))] 
  (gg4clj/view 
  [[:<- :d (gg4clj/data-frame {:x (map (comp str vec) 
                      (apply concat (repeat 2 states)))    
                               :y (vec (concat no-bias-avg avg))
                               :sdup (vec (concat (mat/add no-bias-avg no-bias-sd) 
                                                  (mat/add avg sd)))
                               :sddown (vec (concat (mat/sub no-bias-avg no-bias-sd)
                                                    (mat/sub avg sd)))
                               :z (vec (concat (repeat (count states) "theo no-bias") (repeat (count states) "theo full")))})]
  (gg4clj/r+ [:ggplot :d [:aes {:x :x :y :y :fill :z}]]
                        [:geom_bar {:stat "identity" :position (keyword "position_dodge()")}]
             [:geom_errorbar [:aes {:ymin :sddown, :ymax :sdup}], 
                              {:width 0.5
                               :position (keyword "position_dodge(.9)")}])]
  {:width 10}))
;; @@
;; =>
;; <=

;; @@
(map s/mean (mat/transpose (map #(map % bars) (map dist-theo no-bias-exps))))
(map s/mean (mat/transpose (map #(map % bars) (map dist-theo exps))))
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.21614018581735212</span>","value":"0.21614018581735212"},{"type":"html","content":"<span class='clj-double'>0.19779216187963614</span>","value":"0.19779216187963614"},{"type":"html","content":"<span class='clj-double'>0.04680800867231154</span>","value":"0.04680800867231154"}],"value":"(0.21614018581735212 0.19779216187963614 0.04680800867231154)"}
;; <=

;; @@
(def back-ch (chan 3e4))
(def trad-rbm (train-cd (create-theoretical-rbm 9 5)
          bars
          :epochs 10000
          :init-learning-rate 0.01
          :learning-rate-fn (fn [i s] i)
          :back-ch back-ch))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/trad-rbm</span>","value":"#'small-bars-no-bias/trad-rbm"}
;; <=

;; @@
(async/close! back-ch)
(def history (<!? (async/into [] back-ch)))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/history</span>","value":"#'small-bars-no-bias/history"}
;; <=

;; @@
(def trad-rbm #boltzmann.theoretical.TheoreticalRBM{:restricted-weights [[-0.8841340318279167 2.251071635836021 3.5673896795687194 -3.420249147683104 -1.2819443536331754 0.9862760699510305 -3.4208225115014526 -1.2835930112086564 0.9873557565397155] [5.228078612942525 3.153110701050849 -1.6835081697846173 2.18766698353923 -1.575343145114616 -4.827752402673504 2.1873629031641753 -1.5766255413938415 -4.828597199783511] [-1.1414929820445003 2.0231880113442706 3.829782067690602 -3.5034730413120085 -1.2842516806749793 1.422658547637368 -3.502344192821967 -1.28558125705108 1.422741375870716] [-1.0225817906128736 2.081521313666241 3.6883277133438406 -3.4370661291171474 -1.2858671906995713 1.2297101281301448 -3.4375017775416783 -1.2845039114086085 1.2298397140916468] [4.467966121624177 3.087131915623917 -1.2084866875874103 1.4317318887144581 -1.5665440680582514 -4.347260768337335 1.4332610804749855 -1.566357645033309 -4.346089328590838]] :v-biases [-1.5341301556155136 -9.510913440211544 -2.0159380056943514 1.2062999975130388 -2.7633099136703025 0.8206392901391488 1.2050512968752354 -2.7620516045991153 0.8192053284323875] :h-biases [0.7027248426055048 1.2372358615489172 0.8801590566333531 0.8040422074136998 0.9519249106300683]})
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/trad-rbm</span>","value":"#'small-bars-no-bias/trad-rbm"}
;; <=

;; @@
(def trad-weight-traces (take-nth 100 (map first history)))

;; @@

;; @@
(def trad-receptive-fields (map (partial v/receptive-fields 3)  trad-weight-traces))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/trad-receptive-fields</span>","value":"#'small-bars-no-bias/trad-receptive-fields"}
;; <=

;; @@
(do (apply plot/compose 
       (map #(plot/list-plot %1 :color %2 :joined true
                             :plot-range [[0 300] [-4 5]] :plot-size 800)
       (mat/transpose (map flatten trad-weight-traces))
       (cycle ["red" "green" "blue" "orange" "brown" "black" "grey" "orange" "pink" "yellow"]))))
;; @@

;; @@
(def trad-theo (reduce (fn [f s] (update-in f [s] 
                             (fnil + 0)
                             (f/prob-visi trad-rbm s)))
        {}
     (f/state-space 9)))
#_(map trad-theo states)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/trad-theo</span>","value":"#'small-bars-no-bias/trad-theo"}
;; <=

;; @@
(map #(f/prob-visi trad-rbm %) bars)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.32970446311135887</span>","value":"0.32970446311135887"},{"type":"html","content":"<span class='clj-double'>0.4789328796661246</span>","value":"0.4789328796661246"},{"type":"html","content":"<span class='clj-double'>0.11647926760908169</span>","value":"0.11647926760908169"}],"value":"(0.32970446311135887 0.4789328796661246 0.11647926760908169)"}
;; <=

;; @@
(def trad-samples (map (partial take 9) (sample-gibbs trad-rbm 1e5)))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;small-bars-no-bias/trad-samples</span>","value":"#'small-bars-no-bias/trad-samples"}
;; <=

;; @@
(let [trad-freqs (a/sample-freqs trad-samples)] 
  (gg4clj/view 
  [[:<- :d (gg4clj/data-frame 
             {:x (map (comp str vec) 
                      (apply concat (repeat 3 states)))    
                               
              :y (vec (concat (map (dist-theo (first no-bias-exps)) states) 
                              (map trad-theo states) 
                              (map #(or (trad-freqs %) 0) states)))
                               :z (vec (concat (repeat (count states) "ev-theo") (repeat (count states) "trad-theo") (repeat (count states) "trad-samples")))})]
  (gg4clj/r+ [:ggplot :d [:aes {:x :x :y :y :fill :z}]]
                        [:geom_bar {:stat "identity" :position (keyword "position_dodge()")}])]
  {:width 10}
  ))
;; @@
;; =>
;; <=

;; @@
(plot/histogram (flatten (map :restricted-weights no-bias-exps)) :bins 30)
(plot/histogram (flatten (map :restricted-weights (take 1 exps))) :bins 30)
(plot/histogram (flatten (:restricted-weights trad-rbm)) :bins 30)
;; @@
;; =>
;;; {"type":"vega","content":{"axes":[{"scale":"x","type":"x"},{"scale":"y","type":"y"}],"scales":[{"name":"x","type":"linear","range":"width","zero":false,"domain":{"data":"0e59c691-05e0-47ab-be82-a791151f71df","field":"data.x"}},{"name":"y","type":"linear","range":"height","nice":true,"zero":false,"domain":{"data":"0e59c691-05e0-47ab-be82-a791151f71df","field":"data.y"}}],"marks":[{"type":"line","from":{"data":"0e59c691-05e0-47ab-be82-a791151f71df"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"y":{"scale":"y","field":"data.y"},"interpolate":{"value":"step-before"},"fill":{"value":"steelblue"},"fillOpacity":{"value":0.4},"stroke":{"value":"steelblue"},"strokeWidth":{"value":2},"strokeOpacity":{"value":1}}}}],"data":[{"name":"0e59c691-05e0-47ab-be82-a791151f71df","values":[{"x":-4.828597199783511,"y":0},{"x":-4.493374672692643,"y":2.0},{"x":-4.158152145601775,"y":2.0},{"x":-3.8229296185109076,"y":0.0},{"x":-3.48770709142004,"y":2.0},{"x":-3.152484564329172,"y":4.0},{"x":-2.8172620372383044,"y":0.0},{"x":-2.4820395101474366,"y":0.0},{"x":-2.146816983056569,"y":0.0},{"x":-1.811594455965701,"y":0.0},{"x":-1.476371928874833,"y":5.0},{"x":-1.141149401783965,"y":8.0},{"x":-0.805926874693097,"y":2.0},{"x":-0.47070434760222907,"y":0.0},{"x":-0.13548182051136115,"y":0.0},{"x":0.19974070657950677,"y":0.0},{"x":0.5349632336703747,"y":0.0},{"x":0.8701857607612427,"y":0.0},{"x":1.2054082878521106,"y":2.0},{"x":1.5406308149429786,"y":6.0},{"x":1.8758533420338466,"y":0.0},{"x":2.2110758691247145,"y":4.0},{"x":2.5462983962155823,"y":1.0},{"x":2.88152092330645,"y":0.0},{"x":3.216743450397318,"y":2.0},{"x":3.5519659774881855,"y":0.0},{"x":3.8871885045790533,"y":3.0},{"x":4.222411031669921,"y":0.0},{"x":4.557633558760789,"y":1.0},{"x":4.8928560858516565,"y":0.0},{"x":5.228078612942524,"y":0.0},{"x":5.563301140033392,"y":1.0},{"x":5.89852366712426,"y":0}]}],"width":400,"height":247.2187957763672,"padding":{"bottom":20,"top":10,"right":10,"left":50}},"value":"#gorilla_repl.vega.VegaView{:content {:axes [{:scale \"x\", :type \"x\"} {:scale \"y\", :type \"y\"}], :scales [{:name \"x\", :type \"linear\", :range \"width\", :zero false, :domain {:data \"0e59c691-05e0-47ab-be82-a791151f71df\", :field \"data.x\"}} {:name \"y\", :type \"linear\", :range \"height\", :nice true, :zero false, :domain {:data \"0e59c691-05e0-47ab-be82-a791151f71df\", :field \"data.y\"}}], :marks [{:type \"line\", :from {:data \"0e59c691-05e0-47ab-be82-a791151f71df\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :y {:scale \"y\", :field \"data.y\"}, :interpolate {:value \"step-before\"}, :fill {:value \"steelblue\"}, :fillOpacity {:value 0.4}, :stroke {:value \"steelblue\"}, :strokeWidth {:value 2}, :strokeOpacity {:value 1}}}}], :data [{:name \"0e59c691-05e0-47ab-be82-a791151f71df\", :values ({:x -4.828597199783511, :y 0} {:x -4.493374672692643, :y 2.0} {:x -4.158152145601775, :y 2.0} {:x -3.8229296185109076, :y 0.0} {:x -3.48770709142004, :y 2.0} {:x -3.152484564329172, :y 4.0} {:x -2.8172620372383044, :y 0.0} {:x -2.4820395101474366, :y 0.0} {:x -2.146816983056569, :y 0.0} {:x -1.811594455965701, :y 0.0} {:x -1.476371928874833, :y 5.0} {:x -1.141149401783965, :y 8.0} {:x -0.805926874693097, :y 2.0} {:x -0.47070434760222907, :y 0.0} {:x -0.13548182051136115, :y 0.0} {:x 0.19974070657950677, :y 0.0} {:x 0.5349632336703747, :y 0.0} {:x 0.8701857607612427, :y 0.0} {:x 1.2054082878521106, :y 2.0} {:x 1.5406308149429786, :y 6.0} {:x 1.8758533420338466, :y 0.0} {:x 2.2110758691247145, :y 4.0} {:x 2.5462983962155823, :y 1.0} {:x 2.88152092330645, :y 0.0} {:x 3.216743450397318, :y 2.0} {:x 3.5519659774881855, :y 0.0} {:x 3.8871885045790533, :y 3.0} {:x 4.222411031669921, :y 0.0} {:x 4.557633558760789, :y 1.0} {:x 4.8928560858516565, :y 0.0} {:x 5.228078612942524, :y 0.0} {:x 5.563301140033392, :y 1.0} {:x 5.89852366712426, :y 0})}], :width 400, :height 247.2188, :padding {:bottom 20, :top 10, :right 10, :left 50}}}"}
;; <=

;; @@
(do (apply plot/compose 
       (map #(plot/list-plot %1 :color %2 :joined true
                             :plot-range [[0 300] [-4 4]] :plot-size 800)
       (mat/transpose (->> exps first :weight-history (take-nth 40) (map flatten)))
       (cycle ["red" "green" "blue" "orange" "brown" "black" "grey" "orange" "pink"]))))
(do (apply plot/compose 
       (map #(plot/list-plot %1 :color %2 :joined true
                             :plot-range [[0 300] [-6 4]] :plot-size 800)
       (mat/transpose (->> no-bias-exps first :weight-history (take-nth 40) (map flatten)))
       (cycle ["red" "green" "blue" "orange" "brown" "black" "grey" "orange" "pink"]))))
;; @@
;; =>
;; <=

;; @@

;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:git-commit-id</span>","value":":git-commit-id"},{"type":"html","content":"<span class='clj-string'>&quot;30a7cf5efbc21cffd282a115c3787a389d26d460&quot;</span>","value":"\"30a7cf5efbc21cffd282a115c3787a389d26d460\""}],"value":"[:git-commit-id \"30a7cf5efbc21cffd282a115c3787a389d26d460\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:training-params</span>","value":":training-params"},{"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:h_count</span>","value":":h_count"},{"type":"html","content":"<span class='clj-long'>5</span>","value":"5"}],"value":"[:h_count 5]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:dt</span>","value":":dt"},{"type":"html","content":"<span class='clj-double'>0.1</span>","value":"0.1"}],"value":"[:dt 0.1]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:epochs</span>","value":":epochs"},{"type":"html","content":"<span class='clj-long'>1000</span>","value":"1000"}],"value":"[:epochs 1000]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:burn_in_time</span>","value":":burn_in_time"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:burn_in_time 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:learning_rate</span>","value":":learning_rate"},{"type":"html","content":"<span class='clj-double'>1.0E-7</span>","value":"1.0E-7"}],"value":"[:learning_rate 1.0E-7]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:phase_duration</span>","value":":phase_duration"},{"type":"html","content":"<span class='clj-double'>2000.0</span>","value":"2000.0"}],"value":"[:phase_duration 2000.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:weight_recording_interval</span>","value":":weight_recording_interval"},{"type":"html","content":"<span class='clj-double'>100.0</span>","value":"100.0"}],"value":"[:weight_recording_interval 100.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:sim_setup_kwargs</span>","value":":sim_setup_kwargs"},{"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:grng_seed</span>","value":":grng_seed"},{"type":"html","content":"<span class='clj-long'>52</span>","value":"52"}],"value":"[:grng_seed 52]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:rng_seeds_seed</span>","value":":rng_seeds_seed"},{"type":"html","content":"<span class='clj-long'>52</span>","value":"52"}],"value":"[:rng_seeds_seed 52]"}],"value":"{:grng_seed 52, :rng_seeds_seed 52}"}],"value":"[:sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:stdp_burnin</span>","value":":stdp_burnin"},{"type":"html","content":"<span class='clj-double'>5.0</span>","value":"5.0"}],"value":"[:stdp_burnin 5.0]"}],"value":"{:h_count 5, :dt 0.1, :epochs 1000, :burn_in_time 0.0, :learning_rate 1.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}, :stdp_burnin 5.0}"}],"value":"[:training-params {:h_count 5, :dt 0.1, :epochs 1000, :burn_in_time 0.0, :learning_rate 1.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}, :stdp_burnin 5.0}]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:calibration-id</span>","value":":calibration-id"},{"type":"html","content":"<span class='clj-unkown'>#uuid &quot;22f685d0-ea7f-53b5-97d7-c6d6cadc67d3&quot;</span>","value":"#uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\""}],"value":"[:calibration-id #uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:data-id</span>","value":":data-id"},{"type":"html","content":"<span class='clj-unkown'>#uuid &quot;0b619370-6716-5ae8-89b6-9c38b4e11e94&quot;</span>","value":"#uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\""}],"value":"[:data-id #uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:source-path</span>","value":":source-path"},{"type":"html","content":"<span class='clj-string'>&quot;/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py&quot;</span>","value":"\"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\""}],"value":"[:source-path \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:args</span>","value":":args"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;srun-log&quot;</span>","value":"\"srun-log\""},{"type":"html","content":"<span class='clj-string'>&quot;python&quot;</span>","value":"\"python\""},{"type":"html","content":"<span class='clj-string'>&quot;/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py&quot;</span>","value":"\"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\""}],"value":"[\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]"}],"value":"[:args [\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]]"}],"value":"{:git-commit-id \"30a7cf5efbc21cffd282a115c3787a389d26d460\", :training-params {:h_count 5, :dt 0.1, :epochs 1000, :burn_in_time 0.0, :learning_rate 1.0E-7, :phase_duration 2000.0, :weight_recording_interval 100.0, :sim_setup_kwargs {:grng_seed 52, :rng_seeds_seed 52}, :stdp_burnin 5.0}, :calibration-id #uuid \"22f685d0-ea7f-53b5-97d7-c6d6cadc67d3\", :data-id #uuid \"0b619370-6716-5ae8-89b6-9c38b4e11e94\", :source-path \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\", :args [\"srun-log\" \"python\" \"/wang/users/weilbach/cluster_home/model-nmsampling/code/ev_cd/train.py\"]}"}
;; <=

;; @@
(-> exps first keys)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>(:base-directory :weight-history :weights :topic :output :h-bias :v-bias :h-bias-history :exp-params :v-bias-history)</span>","value":"(:base-directory :weight-history :weights :topic :output :h-bias :v-bias :h-bias-history :exp-params :v-bias-history)"}
;; <=

;; @@
(map :h-bias exps)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.5338071</span>","value":"0.5338071"},{"type":"html","content":"<span class='clj-unkown'>0.6069039</span>","value":"0.6069039"},{"type":"html","content":"<span class='clj-unkown'>0.65348727</span>","value":"0.65348727"},{"type":"html","content":"<span class='clj-unkown'>-0.9270922</span>","value":"-0.9270922"},{"type":"html","content":"<span class='clj-unkown'>1.1769408</span>","value":"1.1769408"}],"value":"[0.5338071 0.6069039 0.65348727 -0.9270922 1.1769408]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>1.4951296</span>","value":"1.4951296"},{"type":"html","content":"<span class='clj-unkown'>1.205095</span>","value":"1.205095"},{"type":"html","content":"<span class='clj-unkown'>0.4512258</span>","value":"0.4512258"},{"type":"html","content":"<span class='clj-unkown'>1.0241032</span>","value":"1.0241032"},{"type":"html","content":"<span class='clj-unkown'>0.7461223</span>","value":"0.7461223"}],"value":"[1.4951296 1.205095 0.4512258 1.0241032 0.7461223]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>-0.5746842</span>","value":"-0.5746842"},{"type":"html","content":"<span class='clj-unkown'>1.1168165</span>","value":"1.1168165"},{"type":"html","content":"<span class='clj-unkown'>0.6668395</span>","value":"0.6668395"},{"type":"html","content":"<span class='clj-unkown'>1.1717786</span>","value":"1.1717786"},{"type":"html","content":"<span class='clj-unkown'>0.69925183</span>","value":"0.69925183"}],"value":"[-0.5746842 1.1168165 0.6668395 1.1717786 0.69925183]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.71956116</span>","value":"0.71956116"},{"type":"html","content":"<span class='clj-unkown'>0.9530921</span>","value":"0.9530921"},{"type":"html","content":"<span class='clj-unkown'>0.5497177</span>","value":"0.5497177"},{"type":"html","content":"<span class='clj-unkown'>0.90940005</span>","value":"0.90940005"},{"type":"html","content":"<span class='clj-unkown'>1.2621924</span>","value":"1.2621924"}],"value":"[0.71956116 0.9530921 0.5497177 0.90940005 1.2621924]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.9277305</span>","value":"0.9277305"},{"type":"html","content":"<span class='clj-unkown'>1.0226426</span>","value":"1.0226426"},{"type":"html","content":"<span class='clj-unkown'>-0.21146896</span>","value":"-0.21146896"},{"type":"html","content":"<span class='clj-unkown'>0.7094261</span>","value":"0.7094261"},{"type":"html","content":"<span class='clj-unkown'>0.8774933</span>","value":"0.8774933"}],"value":"[0.9277305 1.0226426 -0.21146896 0.7094261 0.8774933]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>1.012272</span>","value":"1.012272"},{"type":"html","content":"<span class='clj-unkown'>0.5211767</span>","value":"0.5211767"},{"type":"html","content":"<span class='clj-unkown'>1.0030317</span>","value":"1.0030317"},{"type":"html","content":"<span class='clj-unkown'>0.9936572</span>","value":"0.9936572"},{"type":"html","content":"<span class='clj-unkown'>0.88590735</span>","value":"0.88590735"}],"value":"[1.012272 0.5211767 1.0030317 0.9936572 0.88590735]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.72336453</span>","value":"0.72336453"},{"type":"html","content":"<span class='clj-unkown'>0.91735005</span>","value":"0.91735005"},{"type":"html","content":"<span class='clj-unkown'>1.1606265</span>","value":"1.1606265"},{"type":"html","content":"<span class='clj-unkown'>0.6605213</span>","value":"0.6605213"},{"type":"html","content":"<span class='clj-unkown'>1.245871</span>","value":"1.245871"}],"value":"[0.72336453 0.91735005 1.1606265 0.6605213 1.245871]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.81209856</span>","value":"0.81209856"},{"type":"html","content":"<span class='clj-unkown'>0.48982817</span>","value":"0.48982817"},{"type":"html","content":"<span class='clj-unkown'>0.58385533</span>","value":"0.58385533"},{"type":"html","content":"<span class='clj-unkown'>0.82647806</span>","value":"0.82647806"},{"type":"html","content":"<span class='clj-unkown'>1.1543561</span>","value":"1.1543561"}],"value":"[0.81209856 0.48982817 0.58385533 0.82647806 1.1543561]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.80182046</span>","value":"0.80182046"},{"type":"html","content":"<span class='clj-unkown'>0.89352393</span>","value":"0.89352393"},{"type":"html","content":"<span class='clj-unkown'>0.98751485</span>","value":"0.98751485"},{"type":"html","content":"<span class='clj-unkown'>0.9616782</span>","value":"0.9616782"},{"type":"html","content":"<span class='clj-unkown'>1.1441209</span>","value":"1.1441209"}],"value":"[0.80182046 0.89352393 0.98751485 0.9616782 1.1441209]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>1.5500712</span>","value":"1.5500712"},{"type":"html","content":"<span class='clj-unkown'>1.1518936</span>","value":"1.1518936"},{"type":"html","content":"<span class='clj-unkown'>0.84032923</span>","value":"0.84032923"},{"type":"html","content":"<span class='clj-unkown'>0.8247806</span>","value":"0.8247806"},{"type":"html","content":"<span class='clj-unkown'>1.1075603</span>","value":"1.1075603"}],"value":"[1.5500712 1.1518936 0.84032923 0.8247806 1.1075603]"}],"value":"([0.5338071 0.6069039 0.65348727 -0.9270922 1.1769408] [1.4951296 1.205095 0.4512258 1.0241032 0.7461223] [-0.5746842 1.1168165 0.6668395 1.1717786 0.69925183] [0.71956116 0.9530921 0.5497177 0.90940005 1.2621924] [0.9277305 1.0226426 -0.21146896 0.7094261 0.8774933] [1.012272 0.5211767 1.0030317 0.9936572 0.88590735] [0.72336453 0.91735005 1.1606265 0.6605213 1.245871] [0.81209856 0.48982817 0.58385533 0.82647806 1.1543561] [0.80182046 0.89352393 0.98751485 0.9616782 1.1441209] [1.5500712 1.1518936 0.84032923 0.8247806 1.1075603])"}
;; <=

;; @@
(map :h-bias no-bias-exps)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"},{"type":"list-like","open":"<span class='clj-vector'>[</span>","close":"<span class='clj-vector'>]</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"},{"type":"html","content":"<span class='clj-unkown'>0.0</span>","value":"0.0"}],"value":"[0.0 0.0 0.0 0.0 0.0]"}],"value":"([0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0] [0.0 0.0 0.0 0.0 0.0])"}
;; <=

;; @@

;; @@