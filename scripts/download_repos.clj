#!/usr/bin/env bb
(require '[babashka.curl :as curl]
         '[cheshire.core :as json])

(use 'clojure.pprint)

(def config
  (edn/read-string (slurp "config.edn")))

(def username (:github-username config))
(def user-auth-token (:auth-token config))
(def org-name (:organization-name config))

(def request-args
  {:basic-auth [username user-auth-token]
   :headers {"Accept" "application/vnd.github.v3+json"}})

;(def token-check-resp
  ;(curl/get "https://api.github.com/user" request-args)) )

(def all-repos-url
  (str "https://api.github.com/orgs/" org-name "/repos"))


; For debugging
(defn parse-response [response]
  (let [body (json/parse-string (:body response))
         status (:status response)
         headers (:headers response)]
    ;(prn "RESPONSE ------------")
    ;(pprint status)
    ;(pprint headers)
    ;(pprint (first body))
    ;(prn "--------------------")
    body))

(defn parse-repo-data[repo-data]
  {:id (get repo-data "id")
   :name (get repo-data "name")
   :url (get repo-data "url")
   :default-branch (get repo-data "default_branch")
   :description (get repo-data "description")})

(defn body->values [resp-body]
  (map parse-repo-data resp-body))

(defn request-repo-page [page]
  (-> (curl/request {:basic-auth [username user-auth-token]
                     :headers {"Accept" "application/vnd.github.v3+json"}
                     :url {:scheme "https"
                           :host   "api.github.com"
                           :port   443
                           :path   (str "/orgs/" org-name "/repos")
                           :method "get"
                           :query  (str "per_page=100&page=" page)}})
    parse-response
    body->values))

(def page-range (map inc (range 12)))
(defn all-repos [] (map request-repo-page page-range))


;; Store the repo data so I don't have to run this again
(spit "output/repos.edn" (pr-str (flatten (all-repos))))
