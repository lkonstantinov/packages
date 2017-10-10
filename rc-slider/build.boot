(set-env!
  :resource-paths #{"resources"}
  :dependencies '[[cljsjs/boot-cljsjs "0.7.1"  :scope "test"]
                  [cljsjs/react "15.5.4-0"]
                  [cljsjs/react-dom "15.5.4-0"]
                  [cljsjs/classnames "2.2.3-0"]])

(require '[cljsjs.boot-cljsjs.packaging :refer :all]
         '[boot.core :as boot]
         '[boot.tmpdir :as tmpd]
         '[clojure.java.io :as io]
         '[boot.util :refer [sh]])

(def +lib-version+ "8.3.1")
(def +version+ (str +lib-version+ "-0"))

(task-options!
 pom  {:project     'cljsjs/rc-slider
       :version     +version+
       :description "Slider ui component for React"
       :url         "https://github.com/react-component/slider"
       :scm         {:url "https://github.com/cljsjs/packages"}
       :license     {"MIT" "http://opensource.org/licenses/MIT"}})

(defn- cmd [x]
  (cond-> x
          (re-find #"^Windows" (.get (System/getProperties) "os.name")) (str ".cmd")))

(defn- path [x]
  (.toString (java.nio.file.Paths/get x (into-array String nil))))

(deftask build []
  (let [tmp (boot/tmp-dir!)
        +lib-folder+ (str "slider-" +lib-version+)]
    (with-pre-wrap
      fileset
      (doseq [f (boot/input-files fileset)]
        (let [target (io/file tmp (tmpd/path f))]
          (io/make-parents target)
          (io/copy (tmpd/file f) target)))
      (io/copy
          (io/file tmp "package.json")
          (io/file tmp +lib-folder+ "package.json"))
      (binding [boot.util/*sh-dir* (str (io/file tmp +lib-folder+))]
        ((sh (cmd "npm") "install"))
        ((sh (cmd "npm") "run" "build"))
        ((sh (cmd "npm") "run" "dist")))
      (-> fileset (boot/add-resource tmp) boot/commit!))))

(deftask package []
  (comp
    (download :url (str "https://github.com/react-component/slider/archive/" +lib-version+ ".zip")
              :checksum "846BDC3D750FB2D7D99D1139611ECD67"
              :unzip true)
    (build)
    (sift :move {#".*rc-slider.js" "cljsjs/rc-slider/development/rc-slider.inc.js"
                 #".*rc-slider.min.js" "cljsjs/rc-slider/production/rc-slider.min.inc.js"
                 #".*rc-slider.css" "cljsjs/rc-slider/common/rc-slider.inc.css"})
    (sift :include #{#"^cljsjs"})
    (deps-cljs :name "cljsjs.rc-slider"
               :requires ["cljsjs.react"
                          "cljsjs.react.dom"
                          "cljsjs.classnames"])
    (pom)
    (jar)))
