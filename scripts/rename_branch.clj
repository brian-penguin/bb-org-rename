#!/usr/bin/env bb
(require '[babashka.curl :as curl]
         '[cheshire.core :as json]
         '[clojure.java.shell :refer [sh]])

(use 'clojure.pprint)

; Config ----------------------------------------------------------------
(def config
  (edn/read-string (slurp "config.edn")))
(def username (:github-username config))
(def user-auth-token (:auth-token config))
(def org-name (:organization-name config))
;(def repo-file "test-repos.edn")
(def repo-file "output/repos-full.edn")

(def request-args
  {:basic-auth [username user-auth-token]
   :headers {"Accept" "application/vnd.github.v3+json"}})

; Debugging, quick check for valid token
(def token-check-resp
  (curl/get "https://api.github.com/user" request-args))
;(pprint token-check-resp)


(defn read-repo-data [file-name]
  (edn/read-string (slurp file-name)))

(defn default-is-master? [rename-values]
  (= (:default_branch rename-values) "master"))

(defn unarchived?[rename-values]
  (false? (:archived rename-values)))

(defn only-master-default-branch-repos [all-rename-values]
  (filter default-is-master? all-rename-values))

(defn only-unarchived-data [all-rename-values]
  (filter unarchived? all-rename-values))

(defn unarchived-and-has-master-repos [all-repos]
  (->> all-repos
       (filter unarchived?)
       (filter default-is-master?)))


;; Count the repos ---------------------------------------------------------------------------
(def total-repo-count
  (-> repo-file
      read-repo-data
      count))

(def all-effected-repos
  (-> repo-file
      read-repo-data
      unarchived-and-has-master-repos))

(println "Run Data")
(println (str "total repo count:                              " total-repo-count))
(println (str "total repo count with non-archived and master: " (count all-effected-repos)))


;; NOTE: this doesn't seem to work with any combination of credential and actions I give it.
;; Request to change branch ---------------------------------------------------------------------
;; Now we want to call the change-default-branch fn with the unarchived-and-has-master-repos
;; Example
;curl \
  ;-X POST \
  ;-H "Accept: application/vnd.github.v3+json" \
  ;https://api.github.com/repos/octocat/hello-world/branches/BRANCH/rename \
  ;-d '{"new_name":"new_name"}'

(defn repo-master-branch-rename-path [repo-data]
  (str "/repos/" (:full_name repo-data) "/branches/master/rename"))

(defn parse-response [response]
  (let [body (json/parse-string (:body response) true)
        status (:status response)
        headers (:headers response)]
    ; For debugging
    (prn "RESPONSE ------------")
    (pprint status)
    ;(pprint headers)
    ;(pprint (first body))
    (prn "--------------------")
    body))

(defn post-to-rename-url [repo-data]
  (let [uri (repo-master-branch-rename-path repo-data)]
    (pprint (str "Sending rename request to " uri))
    (-> (curl/request {:basic-auth [username user-auth-token]
                       :headers {"Accept" "application/vnd.github.v3+json"}
                       :body {"new_name" "main"}
                       :url {:scheme "https"
                             :host   "api.github.com"
                             :port   443
                             :path  uri
                             :method "post"}})
        parse-response)))


;;(map post-to-rename-url all-effected-repos)

;; Try just shelling out to the other repo instead!
(defn change-default-branch [repo-data]
  (let [repo-name (:full_name repo-data)]
    ;; NOTE: github-renaming should be using version "0.1.1"
    (-> (sh "github-renaming" "default-branch" "master" "main" "-t" user-auth-token "-r" repo-name "-y")
        :out
        str
        print)
    repo-name))

(map change-default-branch all-effected-repos)

