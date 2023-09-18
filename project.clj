(defproject vid4 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [ring "1.9.0"]
                 [metosin/reitit "0.5.12"]
                 [metosin/muuntaja "0.6.8"]
                 [org.xerial/sqlite-jdbc "3.34.0"] ;; Adicione a dependência do driver SQLite JDBC
                 [com.zaxxer/HikariCP "4.0.3"]
                 [org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.clojure/math.numeric-tower "0.0.1"]
                 [mysql/mysql-connector-java "8.0.30"]] ;; Adicione uma dependência de pool de conexões (HikariCP)
  :main ^:skip-aot vid4.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
