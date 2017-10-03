(set-env!
  :resource-paths #{"resources"}
  :dependencies '[[cljsjs/boot-cljsjs "0.7.1"  :scope "test"]
                  [cljsjs/react "15.6.2-0"]
                  [cljsjs/react-input-autosize "2.0.0-1"]
                  [cljsjs/react-onclickoutside "5.9.0-0"]
                  [cljsjs/prop-types "15.5.10-0"]
                  [cljsjs/classnames "2.2.3-0"]])

(require '[cljsjs.boot-cljsjs.packaging :refer :all]
         '[boot.core :as boot]
         '[boot.tmpdir :as tmpd]
         '[clojure.java.io :as io]
         '[boot.util :refer [dosh]])

(def +lib-version+ "1.4.2")
(def +version+ (str +lib-version+ "-0"))
(def +lib-folder+ (format "react-bootstrap-typeahead-%s" +lib-version+))

(task-options!
  pom {:project 'cljsjs/react-bootstrap-typeahead
       :version +version+
       :description "React-based typeahead component that uses Bootstrap as a base for styles and behaviors"
       :url "https://github.com/ericgio/react-bootstrap-typeahead"
       :license {"MIT" "https://raw.githubusercontent.com/ericgio/react-bootstrap-typeahead/master/LICENSE.md"}
       :scm {:url "https://github.com/cljsjs/packages"}})

(deftask download-src []
  (download :url (format "https://github.com/ericgio/react-bootstrap-typeahead/archive/v%s.zip" +lib-version+)
            :checksum "EB93B9E67031A08D5476F2831442BFC2"
            :unzip true))

(deftask build []
  (let [tmp (boot/tmp-dir!)]
    (with-pre-wrap
      fileset
      (doseq [f (boot/input-files fileset)]
        (let [target (io/file tmp (tmpd/path f))
              src (tmpd/file f)]
          (io/make-parents target)
          (io/copy src target)))
      ; (io/copy
      ;  (io/file tmp +lib-folder+ "webpack.config.js")
      ;  (io/file tmp +lib-folder+ "webpack-cljsjs.config.js"))
      (binding [*sh-dir* (str (io/file tmp +lib-folder+))]
        (dosh "chmod" "+x" "scripts/build.sh")
        (dosh "npm" "install" "--ignore-scripts")
        (dosh "npm" "install" "webpack")
        (dosh "npm" "run" "build")
        ; (dosh-cmd "./node_modules/.bin/webpack")
        ;((sh "./node_modules/.bin/webpack" "--config" "webpack-cljsjs.config.js"))
        )
      (-> fileset (boot/add-resource tmp) boot/commit!))))

(deftask package []
  (comp
   (download-src)
   (build)
   (sift :move { (re-pattern (str "^" +lib-folder+ "/dist/react-bootstrap-typeahead.js")) "cljsjs/react-bootstrap-typeahead/development/react-bootstrap-typeahead.inc.js"

                (re-pattern (str "^" +lib-folder+ "/dist/react-bootstrap-typeahead.min.js")) "cljsjs/react-bootstrap-typeahead/production/react-bootstrap-typeahead.min.inc.js"

                (re-pattern (str "^" +lib-folder+ "/css/(.*)")) "cljsjs/react-bootstrap-typeahead/common/$1"})

   (sift :include #{#"^cljsjs"})
   (deps-cljs :name "cljsjs.react-bootstrap-typeahead" :requires ["cljsjs.react"])
   (pom)
   (jar)))
