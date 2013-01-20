(ns allegro-watcher.core-test
  (:require [net.cgrand.enlive-html :as html])
  (:use midje.sweet
        allegro-watcher.core))

(fact "category page url"
  (category-page-url :category "samsung-i9300-galaxy-s-iii-121198") 
  => "http://allegro.pl/samsung-i9300-galaxy-s-iii-121198?offer_type=0&p=1"

  (category-page-url :category "samsung-i9300-galaxy-s-iii-121198" :page 3) 
  => "http://allegro.pl/samsung-i9300-galaxy-s-iii-121198?offer_type=0&p=3"

  (category-page-url :category "samsung-i9300-galaxy-s-iii-121198" :page 3 :buy-now true) 
  => "http://allegro.pl/samsung-i9300-galaxy-s-iii-121198?offer_type=1&p=3"

  (category-page-url :category "samsung-i9300-galaxy-s-iii-121198" :buy-now true) 
  => "http://allegro.pl/samsung-i9300-galaxy-s-iii-121198?offer_type=1&p=1"

  (category-page-url :category "samsung-i9300-galaxy-s-iii-121198" :buy-now false) 
  => "http://allegro.pl/samsung-i9300-galaxy-s-iii-121198?offer_type=0&p=1")

(defn foo-url [p]
  (category-page-url :category "foo" :page p :buy-now true))

(fact "fetching all pages in category"
  (fetch-all-pages "foo" map) => [...page-1... ...page-2... ...page-3...]

  (provided
    (fetch "http://allegro.pl/foo?offer_type=1&p=1") => ...page-1...
    (fetch "http://allegro.pl/foo?offer_type=1&p=2") => ...page-2...
    (fetch "http://allegro.pl/foo?offer_type=1&p=3") => ...page-3...
    (pages-count ...page-1...) => 3))

(future-fact "fetching returns structures queriable by enlive" :slow
  (let [page (fetch "http://www.google.com")]
    (html/select page [:title html/text-node]) => ["Google"]))

(def doc (-> "test/category-page.html"
             slurp
             (java.io.StringReader.)
             html/html-resource))

(fact "parsing out the total number of pages"
  (pages-count doc) => 10)

(fact "parsing items"
  (count (items doc)) => 50
  (count (filter (cheaper-than 1500) (items doc))) => 2
  (first (items doc)) 
  => {:name "Samsung Galaxy S3 i9300 16GB IGŁA! GW12 (*442633)"
      :url "http://allegro.pl/samsung-galaxy-s3-i9300-16gb-igla-gw12-442633-i2957598095.html"
      :price 1561.54})

(fact "normalizing prices"
  (normalize-price "1 576,54 zł") => 1576.54)

(fact "filtering items by price"
  (let [items [{:price 1499.99} {:price 1500} {:price 1501}]]
    (count (filter (cheaper-than 1500) items)) => 1))
