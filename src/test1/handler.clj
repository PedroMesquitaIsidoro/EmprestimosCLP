(ns test1.handler
  (:require [compojure.core :refer :all] ; Requer a biblioteca Compojure para definir rotas
            [compojure.route :as route]  ; Requer o módulo de rotas do Compojure
            [clojure.java.jdbc :as sql]  ; Requer a biblioteca JDBC para interagir com o banco de dados
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])) ; Requer middleware Ring para configuração padrão

; Configuração do banco de dados MySQL
(def db-config {:subprotocol "mysql"
                :subname "//localhost:3306/dados"
                :user "pedro"
                :password "password"})

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

; Função que consulta todos os registros na tabela "emprestimos"
(defn get-all []
  (sql/query db-config ["select * from emprestimos"]))

; Define as rotas da aplicação
(defroutes app-routes
  (GET "/" [] (get-all)) ; Rota para criar tabelas 
  (GET "/vertodosemprestimos" [] (get-all)) ; Rota para recuperar todos os empréstimos
  (route/not-found "Not Found")) ; Rota para tratamento de requisições não encontradas

; Combina as rotas com as configurações padrão do Ring
(def app
  (wrap-defaults app-routes site-defaults))
