#!/usr/bin/env bb

(ns core
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]))

;; Google API key for the Youtube API
(def api-key "")

;; the API portal is here:
;; https://developers.google.com/youtube/v3/docs/search/list?hl=en&apix_params=%7B%22part%22%3A%5B%22snippet%22%5D%2C%22channelId%22%3A%22UCT60JMcMi0evjm-j-I8gwVw%22%2C%22maxResults%22%3A50%2C%22order%22%3A%22date%22%2C%22q%22%3A%22%5C%22zu+tisch%5C%22+%5C%22arte+family%5C%22%22%7D&apix=true

(defn episode-downloaded?
  [episode-name]
  (->> (fs/list-dir "" "*.description")
    (map #(fs/file-name %))
    (filter #(clojure.string/includes? % episode-name))
    (empty?)
    (not)))

(let [episodes (->> (-> (http/get (str
                                    "https://youtube.googleapis.com/youtube/v3/search?part=snippet&channelId=UCT60JMcMi0evjm-j-I8gwVw&q=%22zu%20tisch%22%20%22arte%20family%22&maxResults=50&key="
                                    api-key)
                          {:accept :json
                           :content-type :json})
                      :body
                      (json/parse-string true)
                      :items)
                 (filter #(clojure.string/includes? % "| Zu Tisch | ARTE Family")))]
  (doall
    (doseq [episode episodes]
      (let [title (-> episode
                    :snippet
                    :title)]
        (if (episode-downloaded? title)
          (println title "is already present and won't be downloaded.")
          (shell (str "yt-dlp -o \"" (-> episode
                                       :snippet
                                       :publishedAt
                                       (subs 0 10))
                   " - "
                   (-> episode
                     :snippet
                     :title)
                   "\" --write-sub --write-auto-sub --sub-lang \"de.*\" --write-thumbnail --write-description -S vcodec:h264 "
                   (-> episode
                     :id
                     :videoId)))))))
  (println "finished downloading episodes"))

