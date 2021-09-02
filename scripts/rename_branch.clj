#!/usr/bin/env bb
(require '[babashka.curl :as curl]
         '[cheshire.core :as json]
         '[clojure.java.shell :refer [sh]])

(use 'clojure.pprint)

(def config
  (edn/read-string (slurp "config.edn")))

(def username (:github-username config))
(def user-auth-token (:auth-token config))
(def org-name (:organization-name config))

(def request-args
  {:basic-auth [username user-auth-token]
   :headers {"Accept" "application/vnd.github.v3+json"}})

(def token-check-resp
  (curl/get "https://api.github.com/user" request-args))

; For debugging
(defn parse-response [response]
  (let [body (json/parse-string (:body response) true)
         status (:status response)
         headers (:headers response)]
    (prn "RESPONSE ------------")
    (pprint status)
    (pprint headers)
    ;(pprint (sort body))
    (prn "--------------------")
    body))

; Debugging
;(parse-response token-check-resp)

;; Goal: Make the default branch main from master (hardcode is okay! I don't plan on doing this again)
;; I'm going to shell out to a local version of the gem "github-renaming" which I've modified to remove prompts
;; This should only work on repos which are unarchived and have "master" default-branch


(defn change-default-branch [repo]
  ;; NOTE: github-renaming should be using version "0.1.1"
  (-> (sh "github-renaming" "default-branch" "master" "main" "-t" user-auth-token "-r" repo)
      :out
      str
      print))

(defn read-repo-data [file-name]
  (edn/read-string (slurp file-name)))

(defn repo-data->rename-values[repo-data]
  (let [ default-branch (:default_branch repo-data)
        repo-full-name (:full_name repo-data)
        archived (:archived repo-data)]
    {:repo-full-name repo-full-name ; "thoughtbot/paperclip"
     :default-branch default-branch ; "master"
     :archived? archived}))         ; false

(defn parse-repo-data [repos]
  (map repo-data->rename-values repos))

(defn default-is-master? [rename-values]
  (= (:default-branch rename-values) "master"))

(defn unarchived?[rename-values]
  (false? (:archived? rename-values)))

(defn only-master-default-branch-repos [all-rename-values]
  (filter default-is-master? all-rename-values))

(defn only-unarchived-data [all-rename-values]
  (filter unarchived? all-rename-values))

(defn unarchived-and-has-master-repos [all-repos]
  (->> all-repos
       (filter unarchived?)
       (filter default-is-master?)))

;(def repo-file "test-repos.edn")
(def repo-file "output/repos-full.edn")

;; https://docs.github.com/en/rest/reference/repos#rename-a-branch
;; This is the api endpoint we want to hit for each of our repos to rename a branch from master to main

(def total-repo-count
  (-> repo-file
      read-repo-data
      parse-repo-data
      count))

(def total-effected-repos
  (-> repo-file
      read-repo-data
      parse-repo-data
      unarchived-and-has-master-repos))

(println "Run Data")
(println (str "total repo count:                              " total-repo-count))
(println (str "total repo count with non-archived and master: " (count total-effected-repos)))

