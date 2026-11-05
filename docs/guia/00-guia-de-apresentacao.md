# Guia de Apresentação - Sistema de Campeonato

Material para **todos os integrantes**. Na apresentação cada um abre o código, explica e responde a **2 perguntas** (0,5 cada) sobre **qualquer parte** do projeto. Este guia dá a visão do todo e aponta onde estudar cada assunto.

| Guia | Assunto |
|---|---|
| este arquivo | visão geral, requisitos, MVC, roteiro da demonstração, banco de perguntas |
| [`01-banco-de-dados.md`](01-banco-de-dados.md) | banco MySQL: tabelas, chaves, regras, triggers, procedures, relatórios |
| [`02-java-e-relatorios.md`](02-java-e-relatorios.md) | Java: pacotes, DAOs, pesquisa individual, validação dos botões, relatórios e PDF |

---

## 1. O sistema em 1 minuto

- **Problema:** organizar um campeonato de futebol. É preciso saber quem joga contra quem, onde, quando, com qual árbitro, e qual é a classificação.
- **Quem usa:** o organizador do campeonato.
- **Cadastros (CRUDs simples):** Campeonato, Equipe, Estádio e Árbitro.
- **CRUD extra:** **Jogador**, ligado a uma equipe (nome, posição, camisa e nascimento).
- **Rotina principal (CRUD principal):** **Partida**. O organizador agenda o jogo (campeonato, rodada, mandante, visitante, estádio, árbitro, data/hora) e depois lança o resultado. A **classificação é atualizada sozinha**.
- **Entidade central:** a **classificação**, que liga campeonato e equipes e é recalculada pelas partidas.
- **Relatórios:** classificação do campeonato (relatório principal, com JOIN entre as tabelas da rotina) e partidas por período (com impressão/PDF).

---

## 2. Requisitos do enunciado → onde estão no projeto

| Requisito | Onde está | Situação |
|---|---|---|
| 4 CRUDs simples | Campeonato, Equipe, Estádio e Árbitro: tabelas em `SQL_Champs/Champs_schema.sql` e abas 1, 2, 4 e 5 da `TelaPrincipal` | ✅ |
| CRUD extra | Jogador: tabela `JOGADOR`, FK para `EQUIPE`, aba 3 da `TelaPrincipal` | ✅ |
| CRUD principal com relacionamentos | Partida: tabela `PARTIDA` com 5 FKs; regras em `SQL_Champs/Champs_procedures_triggers.sql`; aba 7 da `TelaPrincipal` | ✅ |
| Consulta com JOIN | `sp_relatorio_classificacao` e `sp_relatorio_partidas_periodo` em `SQL_Champs/Champs_relatorios.sql`; consultas Java em `ManterPartida`, `ManterClassificacao` e `ManterJogador` | ✅ |
| Relatório da rotina principal | Aba **8. Relatórios** → classificação (relatório principal) e partidas por período | ✅ |
| MySQL, tabelas, PK, FK, relacionamentos | `SQL_Champs/Champs_schema.sql` (guia do banco, seção 1) | ✅ |
| Script SQL de criação | `Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql` | ✅ |
| Dados cadastrados para demonstração | cadastrados pelas telas; testes do banco em `SQL_Champs/99_testes.sql` | ✅ |
| Java + POO | classes de modelo, DAO e tela em `Java_Champs/src/br/com/` | ✅ |
| JFrame/Swing | `TelaPrincipal` no pacote `visao` | ✅ |
| JDBC | `DAO` e classes `Manter...` no pacote `entidade` | ✅ |
| MVC | pacotes `controle` (Model), `entidade` (DAO) e `visao` (View) | ✅ |
| Pesquisa individual | `pesquisarRegistro(...)` em cada `Manter...`, ligada ao botão **Consultar** | ✅ |
| Validação de campos | `vincularValidacao(...)` e validações dos eventos da tela | ✅ |

---

## 3. Arquitetura MVC do projeto

```
  VIEW (visao)              CONTROLE (controle)          ENTIDADE / DAO (entidade)          BANCO
  TelaPrincipal  --clique-->  Campeonato/Equipe/...  -->  Manter...  --SQL (JDBC)-->  MySQL
  (JFrame)       <--dados---  (objetos de dados)     <--  (PreparedStatement)      <--  (tabelas, triggers)
```

- **Model:** as classes de dados (`Campeonato`, `Equipe`, `Jogador`, `Partida`, `RelatorioClassificacao`...) e as classes de relatório.
- **DAO / Entidade:** as classes `Manter...`, que fazem o SQL, e a classe `DAO`, que abre/fecha a conexão.
- **View:** a `TelaPrincipal` (Swing). Ela valida os campos, mostra os dados e repassa os cliques; não contém SQL.
- **Controller:** no projeto, a tela faz o papel de controle das ações (valida e chama o DAO). A separação principal está entre **View** e **DAO**, que é o que o trabalho precisa demonstrar.

Por que não colocar tudo no botão? O enunciado proíbe concentrar a lógica nos eventos do `JFrame`. Separando, cada parte tem uma responsabilidade e pode ser explicada e alterada sozinha.

---

## 4. Roteiro da demonstração

O enunciado pede: **Cadastro → Relacionamento → Rotina Principal → Consulta com JOIN → Relatório**.

1. **Cadastro:** criar um campeonato, 4 equipes, alguns jogadores, 2 estádios e 2 árbitros. Mostrar que **Cadastrar** fica desabilitado com campo vazio e mostrar alteração/exclusão em um deles.
2. **Pesquisa individual:** digitar parte do nome no campo **Pesquisar** e clicar em **Consultar**; o formulário é preenchido com o registro.
3. **Relacionamento:** inscrever as equipes no campeonato (aba 6).
4. **Rotina principal:** agendar partidas; tentar uma partida inválida (mesma equipe na mesma rodada) e mostrar a mensagem do banco; lançar o placar de uma partida.
5. **Consulta com JOIN:** a listagem de partidas mostra nomes, não códigos. Mostrar a classificação atualizada.
6. **Relatório principal:** aba 8 → **Relatório de Classificação**; mostrar pontos, saldo, aproveitamento, estádios e árbitros.
7. **Relatório de partidas por período:** gerar e usar **Imprimir / Salvar em PDF**.

---

## 5. Banco de perguntas por tema

Os temas são os da seção 11 do enunciado. Respostas completas do banco estão no [guia do banco](01-banco-de-dados.md); as do Java e dos relatórios, no [guia Java](02-java-e-relatorios.md).

### Funcionamento geral
- **Qual é a finalidade do sistema?** Organizar campeonatos de futebol: cadastros, inscrição, agendamento e resultado das partidas, classificação e relatórios.
- **Qual é a rotina principal?** O registro de partidas, que relaciona campeonato, duas equipes, estádio e árbitro.
- **O que acontece quando lanço o resultado de uma partida?** O Java faz um `UPDATE` em `PARTIDA`; o trigger `BEFORE` valida; o trigger `AFTER` recalcula a classificação do campeonato.
- **Qual é a entidade central?** A `CLASSIFICACAO`, que liga campeonato e equipe e é alimentada pelas partidas.

### MySQL, chaves e relacionamentos
- **PK × FK?** A PK identifica a linha; a FK aponta para a PK de outra tabela e garante que o valor existe.
- **Dê um exemplo de cada tipo de relacionamento.** 1:N: equipe → jogadores, campeonato → partidas. N:N: campeonato × equipe, via `CLASSIFICACAO`.
- **Por que a partida tem duas FKs para `EQUIPE`?** Mandante e visitante são duas equipes diferentes da mesma tabela.
- **Por que não consigo excluir uma equipe que já jogou?** `ON DELETE RESTRICT` nas FKs de `PARTIDA`: protege o histórico.

### JOIN
- **Onde está o JOIN?** Em `SQL_Champs/Champs_relatorios.sql` (nas duas procedures) e nas consultas Java de partidas e classificação.
- **Por que `EQUIPE` aparece duas vezes no JOIN?** Uma vez para o mandante (apelido `m`) e outra para o visitante (`v`).

### CRUD principal
- **O que torna Partida diferente de um cadastro simples?** Depende de 5 outras tabelas, tem regras (período, inscrição, rodada, horário) e altera a classificação.
- **Quais regras o sistema aplica?** As 4 regras de `sp_validar_partida` e os CHECKs da partida (guia do banco, seção 2.3).

### Relatório
- **Qual é o relatório principal e por que ele é da rotina principal?** A classificação do campeonato. Ela usa `CLASSIFICACAO`, `CAMPEONATO`, `EQUIPE`, `PARTIDA`, `ESTADIO` e `ARBITRO`, mostrando a ligação de todas as tabelas da rotina.
- **Por que procedure?** Recebe parâmetros (campeonato e datas) escolhidos na tela.
- **Como gerar PDF?** Botão Imprimir (`JTable.print()`) → "Salvar como PDF" na janela de impressão.

### CRUDs, pesquisa e validação
- **O que é CRUD?** Create (inserir), Read (consultar/listar), Update (alterar), Delete (excluir). Cada um corresponde a `INSERT`, `SELECT`, `UPDATE` e `DELETE`.
- **Quais são os 4 CRUDs simples?** Campeonato, Equipe, Estádio e Árbitro. O **Jogador** é um CRUD extra, ligado a Equipe.
- **Como funciona a pesquisa individual?** O botão Consultar chama `pesquisarRegistro(termo)`; o DAO procura por id/nome (e cidade, registro, posição, equipe ou camisa quando existe) e devolve o objeto; a tela preenche os campos.
- **Por que os botões ficam desabilitados?** Porque o `DocumentListener` reavalia os campos a cada tecla e só habilita Cadastrar/Alterar/Excluir quando os dados obrigatórios estão válidos. Consultar só habilita com um termo digitado.
- **O banco também valida?** Sim. A tela dá resposta rápida; os triggers do banco garantem as regras mesmo para alterações feitas no Workbench.

### POO, classes e objetos
- **Classe × objeto?** A classe é o molde (`Equipe` com nome e cidade); o objeto é uma equipe concreta criada com `new Equipe()`.
- **O que é encapsulamento?** Atributos `private` acessados só por métodos `get`/`set`, para a classe controlar seus dados.
- **Onde há herança no projeto?** Todas as classes `Manter...` herdam de `DAO`; a `TelaPrincipal` herda de `JFrame`.
- **O que é construtor?** Método chamado no `new`, que inicializa o objeto.
- **Para que serve sobrescrever `toString()`?** Para o `JComboBox` mostrar o nome da equipe em vez do endereço do objeto.

### MVC
- **O que é MVC?** Separar o sistema em Model (dados e acesso ao banco), View (telas) e Controller (ligação e regras da aplicação).
- **Por que usar?** Organização: cada parte tem uma responsabilidade; a tela não conhece SQL e o DAO não conhece a tela.

### JFrame/Swing
- **O que é JFrame?** A janela do Swing. A tela principal do sistema é uma classe que herda de `JFrame`.
- **Componentes usados:** `JTextField` (texto), `JComboBox` (lista de opções), `JTable` (tabela), `JButton`, `JTabbedPane` e `JOptionPane` (mensagens).

### JDBC
- **O que é JDBC?** A API do Java para falar com bancos de dados. O driver (MySQL Connector/J) faz a tradução para o MySQL.
- **Passos básicos:** abrir a conexão (`DriverManager.getConnection`), preparar o SQL (`PreparedStatement` com `?`), executar (`execute`/`executeUpdate` para insert/update/delete, `executeQuery` para select), ler o `ResultSet` e fechar a conexão.
- **Por que `PreparedStatement` com `?`?** Os valores são enviados separados do SQL. Isso evita erros com aspas e impede SQL Injection.
- **Como chamar uma procedure?** `CallableStatement` com `{call nome(?, ?)}`.

### Apresentação do código
- **Onde está a pesquisa individual?** Nos métodos `pesquisarRegistro` de `ManterCampeonato`, `ManterEquipe`, `ManterJogador`, `ManterEstadio`, `ManterArbitro`, `ManterPartida` e `ManterClassificacao`, chamados pelos botões **Consultar** da `TelaPrincipal`.
- **Onde está a validação dos botões?** No método `vincularValidacao` e nas lambdas `validar` de cada aba da `TelaPrincipal`.
- **Onde estão os relatórios?** No banco, em `Champs_relatorios.sql`; no Java, em `ManterRelatorio`; na tela, na aba **8. Relatórios**.
