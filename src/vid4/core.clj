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
                :subname "//127.0.0.1:3306/teste?verifyServerCertificate=false&useSSL=true"
                ;; :subname "//127.0.0.1:3306/teste?verifyServerCertificate=false&useSSL=true"
                :user "root"
                :password ""})
(defn data_ini []
  (.toString(java.time.LocalDateTime/now)))

(defn taxa_juros []
  0.04)

;Calcula o valor final que o cliente irá pagar
(defn calculo_saldo_devedor [valor_emprestado parcelas]
  (* valor_emprestado (math/expt (+ 1 (taxa_juros)) parcelas)))

(defn valor_parcela [valor_emprestado parcelas]
  
  (/(calculo_saldo_devedor valor_emprestado parcelas) parcelas))


(defn aprovar_emprestimo [valor_parcela salario_liquido]

  (if (> valor_parcela (* 0.3 salario_liquido)) false true))





(defn string-handler [_]
  {:status 200
   :body "Sistema de emprestimos"})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Definicoes de tabelas ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Definição  da tabela "emprestimos"
(def emprestimos-table-ddl
  (sql/create-table-ddl :emprestimos
                        [[:id_emprestimo "int(11)" :primary :key :auto_increment]
                         [:data_ini :datetime]
                         [:parcelas :int]
                         [:taxa_juros :real]
                         [:valor_emprestado :float]
                         [:saldo_devedor :float]
                         [:id_usuario "int(11)"]]))

; Definicao da tabela de parcelas
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

; Definicao da tabela de simulacao de emprestimo
(def simulacao-table-ddl
  (sql/create-table-ddl :simulacao
                        [[:id_simulacao "int(11)" :primary :key :auto_increment]
                         [:data_ini :datetime]
                         [:parcelas :int]
                         [:taxa_juros :real]
                         [:valor_emprestado :float]
                         [:saldo_devedor :float]
                          [:id_usuario "int(11)"]]))

(def parcelasSimulada-table-ddl
  (sql/create-table-ddl :parcelasSimulada
                        [[:id_parcelas "int(11)" :primary :key :auto_increment]
                         [:numero_parcela "int(4)"]
                         [:vencimento :date]
                         [:valor_parcela :float]
                         [:status :int]
                          ;; status will be 1 or 0
                         [:id_simulacao "int(11)"]
                         ["FOREIGN KEY (id_simulacao) REFERENCES simulacao (id_simulacao)"]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;FINAL de Definicoes de tabelas ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn insert-parcelasSimulada [id-empr valor_parcela numero_parcela vencimento]

  (sql/insert! db-config :parcelasSimulada {:id_simulacao id-empr :valor_parcela valor_parcela :numero_parcela numero_parcela :vencimento  vencimento :status 0}))



;Função para criar uma instância de empréstimos e associar a n parcelas na outra tabela
(defn insert-emprestimos [ parcelas  valor_emprestado]


  (let [id_empr (sql/insert! db-config  :emprestimos {:data_ini (data_ini) :parcelas parcelas :taxa_juros (taxa_juros) :valor_emprestado valor_emprestado :saldo_devedor (calculo_saldo_devedor valor_emprestado parcelas)})]

    (loop [x 1] (when (<= x parcelas)

                  (insert-parcelas (get (first (first id_empr)) 1) (/ (calculo_saldo_devedor valor_emprestado parcelas) parcelas) x (add-1-month (date-str (data_ini)) x))

                  (recur (+ x 1))))))

;Função para criar uma instância de empréstimos simulados e associar a n parcelas na outra tabela
(defn insert-emprestimosSimulados [parcelas  valor_emprestado]


  (let [id_empr (sql/insert! db-config  :simulacao {:data_ini (data_ini) :parcelas parcelas :taxa_juros (taxa_juros) :valor_emprestado valor_emprestado :saldo_devedor (calculo_saldo_devedor valor_emprestado parcelas)})]

    (loop [x 1] (when (<= x parcelas)

                  (insert-parcelasSimulada (get (first (first id_empr)) 1) (/ (calculo_saldo_devedor valor_emprestado parcelas) parcelas) x (add-1-month (date-str (data_ini)) x))

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

(defn create-tables-if-not-exist []
  (when-not (table-exists? "simulacao")
    (sql/db-do-commands db-config simulacao-table-ddl)))

; Chama a função para criar a tabela "simulacao" se ela não existir
(create-tables-if-not-exist)

(defn create-tables-if-not-exist []
  (when-not (table-exists? "parcelasSimulada")
    (sql/db-do-commands db-config parcelasSimulada-table-ddl)))

; Chama a função para criar a tabela "parcelasSimulada" se ela não existir
(create-tables-if-not-exist)


;ver A SIMULACAO das parcelas
(defn get-simulacaoParcelas-and-delete [_]
  (let [select-parcelas-query "SELECT * FROM parcelasSimulada"
        select-simulacao-query "SELECT * FROM simulacao"]
    (let [result-parcelas (sql/query db-config select-parcelas-query)
          result-simulacao (sql/query db-config select-simulacao-query)]
      (if (and (seq result-parcelas) (seq result-simulacao)) ; Verifica se ambas as consultas retornaram resultados
        (do
          (sql/execute! db-config ["DELETE FROM parcelasSimulada"])
          (sql/execute! db-config ["DELETE FROM simulacao"])
          {:status 200
           :body {:resultado-parcelas result-parcelas
                  :resultado-simulacao result-simulacao
                  :mensagem "Selecionado e apagado com sucesso."}})
        {:status 404
         :body {:resultado-parcelas nil
                :resultado-simulacao nil
                :mensagem "Nenhum registro encontrado para o ID especificado."}}))))

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
        (if (aprovar_emprestimo (valor_parcela (:valor_emprestado json-data) (:parcelas json-data)) (:salario_liquido json-data))
        (do(insert-emprestimos  
                             (:parcelas json-data)
                             (:valor_emprestado json-data))
        {:status 201
         :body "Empréstimo criado com sucesso"}
        
        )
        
        (do  {:status 202
         :body "Empréstimo recusado"} ))))))

(defn create-simulacao [request]
  (let [json-data (:body-params request)]
    (cond
      (some nil? (vals json-data))
      {:status 400
       :body "Parâmetros inválidos"}

      :else
      (try
        (insert-emprestimosSimulados  
                                      (:parcelas json-data)        
                                      (:valor_emprestado json-data))
        {:status 201
         :body "Simulação criada com sucesso"}))))


(def app
  (ring/ring-handler
   (ring/router
    ["/"
     ["emprestimos" {:get get-emprestimos
                     :post create-emprestimos}]
     ["simulacao" {:get get-simulacaoParcelas-and-delete
                   :post create-simulacao}]
     ["" string-handler]]
    {:data {:muuntaja m/instance
            :middleware [muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty app {:port  3000
                             :join? false}))

(defn -main
  [& args]
  (start))
