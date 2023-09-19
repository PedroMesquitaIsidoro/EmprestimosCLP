(ns vid4.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [compojure.core :refer :all] ; Requer a biblioteca Compojure para definir rotas
            [compojure.route :as route]  ; Requer o módulo de rotas do Compojure
            [clojure.math.numeric-tower :as math]
            [clojure.string :as str]
            [clojure.java.jdbc :as sql]  ; Requer a biblioteca JDBC para interagir com o banco de dados
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:gen-class))
; Configuração do banco de dados MySQL
(def db-config {:subprotocol "mysql"
                ;; :subname "//localhost:3306/dados"
                 :subname "//127.0.0.1:3306/teste?verifyServerCertificate=false&useSSL=true"
                :user "root"
                :password ""})

(defn string-handler [_]
  {:status 200
   :body "Sistema de emprestimos"})

; Definição do DDL (Data Definition Language) da tabela "emprestimos"
(def emprestimos-table-ddl
  (sql/create-table-ddl :emprestimos
                        [[:id_emprestimo "int(11)" :primary :key :auto_increment]
                         [:data_ini :datetime]
                         [:parcelas :int]
                         [:taxa_juros :real]
                         [:valor_emprestado :float]
                         [:saldo_devedor :float]
                         [:id_usuario "int(11)"]]))

(def parcelas-table-ddl
  (sql/create-table-ddl :parcelas
                        [[:id_parcelas "int(11)" :primary :key :auto_increment]
                         [:numero_parcela "int(4)"]
                         [:vencimento :date]
                         [:valor_parcela :float]
                         [:status :int]
                          ;; status will be 1 or 0
                         [:id_emprestimo "int(11)"]
                         ["FOREIGN KEY (id_emprestimo) REFERENCES emprestimos (id_emprestimo)"]]))
;Converter datetime to date
(defn date-str [date]
  (first (str/split date #"T")))

;Adicionar x meses na data atual
(defn add-1-month [date x]
  (-> (java.time.LocalDate/parse date)
      (.plusMonths x)
      str))

(defn insert-parcelas [id-empr valor_parcela numero_parcela vencimento]

  (sql/insert! db-config :parcelas {:id_emprestimo id-empr :valor_parcela valor_parcela :numero_parcela numero_parcela :vencimento  vencimento :status 0}))

;Função para criar uma instância de empréstimos e associar a n parcelas na outra tabela
(defn insert-emprestimos [data_ini parcelas taxa_juros valor_emprestado]


  (let [id_empr (sql/insert! db-config  :emprestimos {:data_ini data_ini :parcelas parcelas :taxa_juros taxa_juros :valor_emprestado valor_emprestado :saldo_devedor (* valor_emprestado (math/expt (+ 1 taxa_juros) parcelas))})]

    (loop [x 1] (when (<= x parcelas)

                  (insert-parcelas (get (first (first id_empr)) 1) (/ (* valor_emprestado (math/expt (+ 1 taxa_juros) parcelas)) parcelas) x (add-1-month (date-str data_ini) x))

                  (recur (+ x 1))))))

; Função que verifica se uma tabela existe no banco de dados
(defn table-exists? [table-name]
  (let [sql-statement (str "SHOW TABLES LIKE '" table-name "'")]
    (not (empty? (sql/query db-config [sql-statement])))))

; Função que cria a tabela "emprestimos" se ela não existir
(defn create-tables-if-not-exist []
  (when-not (table-exists? "emprestimos")
    (sql/db-do-commands db-config emprestimos-table-ddl)))

; Chama a função para criar a tabela "emprestimos" se ela não existir
(create-tables-if-not-exist)

(defn create-tables-if-not-exist []
  (when-not (table-exists? "parcelas")
    (sql/db-do-commands db-config parcelas-table-ddl)))

; Chama a função para criar a tabela "parcelas" se ela não existir
(create-tables-if-not-exist)

;ver todos os emprestimos
(defn get-emprestimos [_]
  {:status 200
   :body (sql/query db-config ["select * from emprestimos"])})

(defn create-emprestimos [request]
  (let [json-data (:body-params request)]
    (cond
      (some nil? (vals json-data))
      {:status 400
       :body "Parâmetros inválidos"}
    
      :else
      (try
        (insert-emprestimos  (:data_ini json-data)
                             (:parcelas json-data)
                             (:taxa_juros json-data)
                            (:valor_emprestado json-data)
                            )
        {:status 201
         :body "Empréstimo criado com sucesso"}))))


;; (defn create-emprestimos [request]
;;   (let [json-data (:body-params request)] ; Access the JSON payload
;;     {:status 200
;;      :body {:data_ini (:data_ini json-data)
;;             :user (:user json-data)
;;             :taxa_juros (:taxa_juros json-data)}}))



;; (defn create-emprestimos [request]
;;   (let [parsed-body (-> request :body muuntaja/parse)]
;;     (if (contains? parsed-body "user")
;;       {:status 200
;;        :body (get parsed-body "user")}
;;       {:status 400
;;        :body "The 'user' field is missing in the request body"})))






 


(def app
  (ring/ring-handler
   (ring/router
    ["/" 
     ["emprestimos" {:get get-emprestimos
                     :post create-emprestimos}] 
     ["" string-handler]]
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty app {:port  3000
                             :join? false}))

(defn -main
  [& args]
  (start))
