(ns allegro-watcher.core
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as html]))

(def base-url "http://allegro.pl")

(defn category-page-url [& {:keys [category page buy-now] 
                            :or {page 1 buy-now false}}]
  (let [params [["offer_type=" (if buy-now 1 0)]
                ["p=" page]]
        params-str (map #(apply str %) params)]
    (str base-url "/" category "?" (string/join "&" params-str))))

(defn pages-count [doc]
  (let [pager-text (html/select doc [:.pagerText html/text-node])]
    (Integer/parseInt (last pager-text))))

(defn fetch [url]
  (html/html-resource (java.net.URL. url)))

(defn fetch-all-pages 
  ([category] (fetch-all-pages category pmap))
  ([category mapper]
   (let [url #(category-page-url :category category :page % :buy-now true)
         fetch-url (comp fetch url)
         first-page (fetch-url 1)
         cnt (pages-count first-page)]
     (cons first-page (doall (mapper fetch-url (range 2 (inc cnt))))))))

(defn parse-name [row]
  (apply str (html/select row [:.iTitle :span html/text-node])))

(defn parse-url [row]
  (let [link (first (html/select row [:a.iTitle]))
        href (get-in link [:attrs :href])] 
    (str base-url href)))

(defn normalize-price [price]
  (-> price
      (string/replace #" |zł|[^\d,]" "")
      (string/replace "," ".")
      (Double/parseDouble)))

(defn parse-price [row]
  (let [price-node (html/select row [:.iPriceBN html/text-node])]
    (normalize-price (apply str price-node))))

(defn parse-item [row]
  {:name (parse-name row)
   :price (parse-price row)
   :url (parse-url row)})

(defn items [doc]
  (let [item-rows (html/select doc [:.itemListing :tbody :tr])]
    (map parse-item item-rows)))

(defn cheaper-than [price]
  #(< (:price %) price))

(defn pmapcat [f coll]
  (doall (apply concat (pmap f coll))))

(defn -main [& [category price]]
  (let [pages (fetch-all-pages category)
        all-items (pmapcat items pages)
        price (Double/parseDouble price)
        items (->> all-items (filter (cheaper-than price)) (sort-by :price))]
    (doseq [{:keys [name price url]} items]
      (println price " " name)
      (println url)
      (println))
    (System/exit 0)))
