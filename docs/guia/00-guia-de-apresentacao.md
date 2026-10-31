# Guia de Apresentação - Sistema de Campeonato

Material para **todos os integrantes**. Na apresentação cada um abre o código, explica e responde a **2 perguntas** (0,5 cada) sobre **qualquer parte** do projeto. Este guia dá a visão do todo e aponta onde estudar cada assunto.

> Partes marcadas com *(a preencher na Fase N)* ainda não foram implementadas. Este guia é atualizado ao final de cada fase.

| Guia | Assunto |
|---|---|
| este arquivo | visão geral, requisitos, MVC, roteiro da demonstração, banco de perguntas |
| [`01-banco-de-dados.md`](01-banco-de-dados.md) | banco MySQL: tabelas, chaves, regras, triggers, relatório |
| `02-java-mvc-e-conexao.md` | *(a preencher na Fase 2)* |
| `03-cruds-simples.md` | *(a preencher na Fase 3)* |
| `04-crud-principal.md` | *(a preencher na Fase 4)* |
| `05-relatorio-e-demonstracao.md` | *(a preencher na Fase 5)* |

---

## 1. O sistema em 1 minuto

- **Problema:** organizar um campeonato de futebol. É preciso saber quem joga contra quem, onde, quando, com qual árbitro, e qual é a classificação.
- **Quem usa:** o organizador do campeonato.
- **Cadastros (CRUDs simples):** Campeonato, Equipe, Estádio, Árbitro.
- **Rotina principal (CRUD principal):** **Partida**. O organizador agenda o jogo (campeonato, rodada, mandante, visitante, estádio, árbitro, data/hora) e depois lança o resultado. A **classificação é atualizada sozinha**.
- **Relatório:** partidas de um campeonato num período, com nomes, placar e situação.

---

## 2. Requisitos do enunciado → onde estão no projeto

| Requisito | Onde está | Situação |
|---|---|---|
| 4 CRUDs simples | Campeonato, Equipe, Estádio, Árbitro: tabelas em `sql/Champs_schema.sql`; telas *(a preencher na Fase 3)* | banco pronto |
| CRUD principal com relacionamentos | Partida: tabela `PARTIDA` com 5 FKs; regras em `sql/Champs_procedures_triggers.sql`; tela *(a preencher na Fase 4)* | banco pronto |
| Consulta com JOIN | `sp_relatorio_partidas_periodo` em `sql/Champs_relatorios.sql`; consultas Java *(a preencher na Fase 4)* | banco pronto |
| Relatório da rotina principal | `sp_relatorio_partidas_periodo` (partidas por período); tela *(a preencher na Fase 5)* | banco pronto |
| MySQL, tabelas, PK, FK, relacionamentos | `sql/Champs_schema.sql` (guia do banco, seção 1) | pronto |
| Script SQL de criação | `sql/Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql` | pronto |
| Dados cadastrados para demonstração | cadastrados pelas telas *(Fase 5)*; testes do banco em `sql/99_testes.sql` | pendente |
| Java + POO | classes de modelo, DAO, controller e telas *(Fases 2 a 4)* | pendente |
| JFrame/Swing | pacote `visao` *(Fases 2 a 5)* | pendente |
| JDBC | `ConexaoBD` e DAOs *(Fase 2)* | pendente |
| MVC | pacotes `modelo`, `controle`, `visao` *(Fase 2)* | pendente |

---

## 3. Arquitetura MVC do projeto

```
  VIEW (visao)              CONTROLLER (controle)          MODEL (modelo + modelo/dao)          BANCO
  TelaPartida  --clique-->  PartidaController  --chama-->  PartidaDAO  --SQL (JDBC)-->  MySQL
  (JFrame)     <--resposta- (valida, trata erro) <-objetos- (Partida)    <--linhas----   (tabelas, triggers)
```

- **Model:** as classes de dados (`Partida`, `Equipe`…) e os DAOs, que fazem o SQL.
- **View:** as telas Swing (`JFrame`). Só mostram dados e repassam os cliques.
- **Controller:** recebe o pedido da tela, valida, chama o DAO e devolve o resultado ou a mensagem de erro.

Por que não colocar tudo no botão? O enunciado proíbe concentrar a lógica nos eventos do JFrame. Separando, cada parte tem uma responsabilidade e pode ser explicada e alterada sozinha.

*(Fase 2: acrescentar aqui o caminho real de um clique, com nomes de métodos.)*

---

## 4. Roteiro da demonstração

O enunciado pede: **Cadastro → Relacionamento → Rotina Principal → Consulta com JOIN → Relatório**.

1. **Cadastro:** criar um campeonato, 4 equipes, 2 estádios, 2 árbitros. Mostrar alteração e exclusão em um deles.
2. **Relacionamento:** inscrever as equipes no campeonato.
3. **Rotina principal:** agendar partidas; tentar uma partida inválida (mesma equipe na mesma rodada) e mostrar a mensagem do banco; lançar o placar de uma partida.
4. **Consulta com JOIN:** a listagem de partidas mostra nomes, não códigos. Mostrar a classificação atualizada.
5. **Relatório:** gerar as partidas do período e imprimir/salvar em PDF.

*(Detalhes de cada tela: Fases 3 a 5.)*

---

## 5. Banco de perguntas por tema

Os temas são os da seção 11 do enunciado. Para o banco de dados, as respostas completas estão no [guia do banco](01-banco-de-dados.md) (seção 5).

### Funcionamento geral
- **Qual é a finalidade do sistema?** Organizar campeonatos de futebol: cadastros, agendamento e resultado das partidas, classificação e relatório.
- **Qual é a rotina principal?** O registro de partidas, que relaciona campeonato, duas equipes, estádio e árbitro.
- **O que acontece quando lanço o resultado de uma partida?** O Java faz um `UPDATE` em `PARTIDA`; o trigger `BEFORE` valida; o trigger `AFTER` recalcula a classificação do campeonato.

### MySQL, chaves e relacionamentos
- **PK × FK?** A PK identifica a linha; a FK aponta para a PK de outra tabela e garante que o valor existe.
- **Dê um exemplo de cada tipo de relacionamento.** 1:N: equipe → jogadores, campeonato → partidas. N:N: campeonato × equipe, via `CLASSIFICACAO`.
- **Por que a partida tem duas FKs para `EQUIPE`?** Mandante e visitante são duas equipes diferentes da mesma tabela.
- **Por que não consigo excluir uma equipe que já jogou?** `ON DELETE RESTRICT` nas FKs de `PARTIDA`: protege o histórico.

### JOIN
- **Onde está o JOIN?** `sp_relatorio_partidas_periodo` (`sql/Champs_relatorios.sql`) e as consultas Java de partidas e classificação *(Fase 4)*.
- **Por que `EQUIPE` aparece duas vezes no JOIN?** Uma vez para o mandante (apelido `m`) e outra para o visitante (`v`).

### CRUD principal
- **O que torna Partida diferente de um cadastro simples?** Depende de 5 outras tabelas, tem regras (período, inscrição, rodada, horário) e altera a classificação.
- **Quais regras o sistema aplica?** As 4 regras de `sp_validar_partida` e os CHECKs da partida (guia do banco, seção 2.3).

### Relatório
- **Qual é o relatório e por que ele é da rotina principal?** Partidas de um campeonato num período. É feito sobre `PARTIDA`, que é a rotina principal.
- **Por que procedure?** Recebe parâmetros (campeonato e datas) escolhidos na tela.
- **Como gerar PDF?** Botão Imprimir (`JTable.print()`) → "Salvar como PDF" na janela de impressão *(Fase 5)*.

### CRUDs
- **O que é CRUD?** Create (inserir), Read (consultar/listar), Update (alterar), Delete (excluir). Cada um corresponde a `INSERT`, `SELECT`, `UPDATE` e `DELETE`.
- **Quais são os 4 CRUDs simples?** Campeonato, Equipe, Estádio e Árbitro.

### POO, classes e objetos
- **Classe × objeto?** A classe é o molde (`Equipe` com nome e cidade); o objeto é uma equipe concreta criada com `new Equipe()`.
- **O que é encapsulamento?** Atributos `private` acessados só por métodos `get`/`set`, para a classe controlar seus dados.
- **Onde há herança no projeto?** Todas as telas herdam de `JFrame` (`extends JFrame`) *(apontar no código na Fase 2)*.
- **O que é construtor?** Método chamado no `new`, que inicializa o objeto.
- **Para que serve sobrescrever `toString()`?** Para o `JComboBox` mostrar o nome da equipe em vez do endereço do objeto *(Fase 3)*.

*(Exemplos com o nosso código: Fases 2 e 3.)*

### MVC
- **O que é MVC?** Separar o sistema em Model (dados e acesso ao banco), View (telas) e Controller (ligação e regras da aplicação).
- **Por que usar?** Organização: cada parte tem uma responsabilidade; a tela não conhece SQL e o DAO não conhece a tela.

### JFrame/Swing
- **O que é JFrame?** A janela do Swing. Cada tela do sistema é uma classe que herda de `JFrame`.
- **Componentes usados:** `JTextField` (texto), `JComboBox` (lista de opções), `JTable` (tabela), `JButton` e `JOptionPane` (mensagens) *(detalhes nas Fases 3 e 4)*.

### JDBC
- **O que é JDBC?** A API do Java para falar com bancos de dados. O driver (MySQL Connector/J) faz a tradução para o MySQL.
- **Passos básicos:** abrir a conexão (`DriverManager.getConnection`), preparar o SQL (`PreparedStatement` com `?`), executar (`executeUpdate` para insert/update/delete, `executeQuery` para select), ler o `ResultSet` e fechar a conexão.
- **Por que `PreparedStatement` com `?`?** Os valores são enviados separados do SQL. Isso evita erros com aspas e impede SQL Injection.
- **Como chamar uma procedure?** `CallableStatement` com `{call nome(?, ?)}`.

*(Exemplos com o nosso código: Fase 2.)*
