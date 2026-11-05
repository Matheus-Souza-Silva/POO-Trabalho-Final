# Simulado — Perguntas e Respostas do Champs

> **Objetivo:** treinar para a apresentação. As perguntas estão agrupadas por tema e as respostas são curtas, no estilo “responder em 20 segundos”. Quando fizer sentido, há o ponteiro para o arquivo/guia onde aprofundar.
>
> Use assim: leia a pergunta, responda em voz alta e só depois confira. Marque as que você errou e volte ao guia correspondente.
>
> Guias: [visão geral](00-guia-de-apresentacao.md) · [banco](01-banco-de-dados.md) · [Java](02-java-e-relatorios.md) · [mapa](03-mapa-do-codigo.md).

---

## 1. Visão geral (1–12)

**1. Qual é a finalidade do sistema?**
Organizar campeonatos de futebol: cadastrar campeonatos, equipes, jogadores, estádios e árbitros; inscrever equipes; agendar partidas; lançar resultados; calcular classificação e gerar relatórios.

**2. Quem usa o sistema?**
O organizador do campeonato. É um sistema desktop, não um site; os dados ficam em um MySQL local.

**3. Qual é a rotina principal?**
O registro de partidas (aba 7). É o CRUD que relaciona cinco tabelas (campeonato, mandante, visitante, estádio e árbitro) e altera a classificação.

**4. Qual é a entidade central?**
A `CLASSIFICACAO`, que liga campeonato e equipe (N:N) e é recalculada pelas partidas.

**5. Quais são os 4 CRUDs simples?**
Campeonato, Equipe, Estádio e Árbitro. Jogador é o CRUD extra; Partida é o principal.

**6. Qual é o CRUD extra e por quê?**
Jogador, porque tem uma FK para Equipe (1:N) e campos próprios (posição, camisa, nascimento).

**7. O que é CRUD?**
Create, Read, Update, Delete: inserir, consultar, alterar e excluir. No SQL: `INSERT`, `SELECT`, `UPDATE`, `DELETE`.

**8. Quais requisitos do enunciado o projeto atende?**
4 CRUDs simples, 1 CRUD extra, 1 CRUD principal com relacionamentos, consulta com JOIN, relatório da rotina principal, MySQL com PK/FK, Java + POO + Swing + JDBC + MVC, pesquisa individual e validação de campos.

**9. O que é MVC no projeto?**
`br.com.controle` = Model; `br.com.entidade` = DAO; `br.com.visao` = View. O controller está nos eventos da tela.

**10. Por que a classificação é automática?**
Porque um trigger `AFTER` em `PARTIDA` chama `sp_sincronizar_classificacao`, que recalcula os números a cada insert/update/delete.

**11. O que acontece quando lanço o placar?**
O Java faz `UPDATE` em `PARTIDA`; o trigger `BEFORE` valida; o `AFTER` recalcula a classificação; a tela recarrega.

**12. Onde estão os relatórios?**
No banco (`Champs_relatorios.sql`), no DAO (`ManterRelatorio`) e na aba 8 da `TelaPrincipal`.

---

## 2. Banco de dados e modelo (13–32)

**13. Quantas tabelas o banco tem?**
Sete: `CAMPEONATO`, `EQUIPE`, `ESTADIO`, `ARBITRO`, `JOGADOR`, `CLASSIFICACAO`, `PARTIDA`.

**14. Qual é a diferença entre PK e FK?**
PK identifica a linha da própria tabela; FK aponta para a PK de outra tabela e garante que o valor existe.

**15. Dê um exemplo de relação 1:N.**
Uma equipe tem vários jogadores (`JOGADOR.id_equipe`); um campeonato tem várias partidas.

**16. Dê um exemplo de N:N.**
Campeonato × Equipe, resolvido por `CLASSIFICACAO`.

**17. Por que `CLASSIFICACAO` tem PK composta?**
Porque representa a inscrição de uma equipe em um campeonato; `(id_campeonato, id_equipe)` é única.

**18. Por que `PARTIDA` tem duas FKs para `EQUIPE`?**
Porque uma partida tem mandante e visitante; são duas colunas apontando para a mesma tabela.

**19. O que `uq_jogador_camisa` garante?**
Que a mesma camisa não se repita **na mesma equipe**; o mesmo número pode existir em equipes diferentes.

**20. O que são colunas geradas?**
Colunas calculadas pelo MySQL. `saldo_gols = gols_marcados - gols_sofridos` e `pontuacao = vitorias * 3 + empates`.

**21. O que é `ENUM`?**
Uma coluna que só aceita uma lista fixa de valores, como o status da partida.

**22. Dê exemplos de CHECK.**
`chk_campeonato_datas` (fim ≥ início), `chk_estadio_capacidade` (> 0), `chk_partida_times` (mandante ≠ visitante), `chk_partida_placar_por_status`.

**23. O que é `ON DELETE RESTRICT`?**
Impede apagar o pai se houver filho. Ex.: equipe com jogador/inscrição/partida.

**24. Onde há `ON DELETE CASCADE`?**
Em `fk_classificacao_campeonato`: apagar um campeonato sem partidas apaga as inscrições.

**25. Como apagar uma equipe que tem jogadores e inscrição?**
Primeiro apagar os jogadores, depois remover a inscrição (se não houver partidas) e só então a equipe.

**26. O que acontece ao apagar um campeonato com partidas?**
A FK `fk_partida_campeonato` (RESTRICT) bloqueia. Sem partidas, o `CASCADE` apaga as inscrições.

**27. O que é `AUTO_INCREMENT`?**
O MySQL gera o próximo número da PK sozinho; o Java não envia o ID no INSERT.

**28. O que é `ENGINE=InnoDB`?**
A engine que suporta FKs e transações.

**29. O que é `utf8mb4_unicode_ci`?**
Collation que aceita Unicode e compara sem diferenciar maiúsculas/minúsculas; por isso `LIKE` é case-insensitive.

**30. O que é um índice?**
Uma estrutura que acelera buscas. Ex.: `idx_partida_data` e `idx_partida_campeonato_data`.

**31. `CLASSIFICACAO.status_atual` é automático?**
Não. É manual (`A_JOGAR`, `CLASSIFICADO`, `ELIMINADO`); a pontuação/colocação é que são automáticas.

**32. `JOGADOR.posicao` é `ENUM`?**
Não, é `VARCHAR(30)`. O combo do Java limita a seis valores, mas o banco aceita qualquer texto.

---

## 3. Triggers, procedures e regras (33–60)

**33. Qual a diferença entre procedure e trigger?**
Procedure é chamada com `CALL`; trigger é executado automaticamente pelo banco em INSERT/UPDATE/DELETE.

**34. Qual a diferença entre `BEFORE` e `AFTER`?**
`BEFORE` roda antes de gravar e pode cancelar; `AFTER` roda depois e atualiza outras tabelas.

**35. O que são `NEW` e `OLD`?**
`NEW` é a linha nova (insert/update); `OLD` é a antiga (update/delete).

**36. O que é `SIGNAL SQLSTATE '45000'`?**
Um erro definido pelo usuário, como um `throw`; cancela o comando e devolve a mensagem ao Java.

**37. O que é `DELIMITER`?**
Comando do cliente para trocar o terminador; permite procedures com `;` interno. Não é SQL do servidor.

**38. Quais são as quatro regras da partida?**
Data dentro do período do campeonato; equipes inscritas; equipe não joga duas vezes na rodada; árbitro/estádio livres no horário.

**39. Partida cancelada passa pelas quatro regras?**
Não; a procedure começa com `IF p_status <> 'CANCELADA'`. Mas FKs, CHECKs e o `UNIQUE` de confronto continuam valendo.

**40. Para que serve `p_id_partida` na validação?**
Para a partida não conflitar com ela mesma quando está sendo alterada (`id_partida <> p_id_partida`).

**41. A regra 4 vale só para o mesmo campeonato?**
Não; vale para qualquer campeonato. Um árbitro não pode estar em dois lugares ao mesmo tempo.

**42. A regra 4 compara data ou data/hora?**
O `DATETIME` exato. Dois jogos no mesmo dia em horários diferentes são permitidos.

**43. Quais são os três passos da sincronização?**
1) zerar os acumulados; 2) somar as partidas `ENCERRADA` (duas linhas por partida); 3) calcular a colocação com `ROW_NUMBER`.

**44. Por que `SUM(v)` conta vitórias?**
Porque no MySQL uma comparação booleana vale 1 (verdadeiro) ou 0 (falso).

**45. Por que usar `UNION ALL`?**
Para juntar a visão do mandante e a do visitante mantendo todas as linhas; o `GROUP BY` soma por equipe.

**46. Por que `ROW_NUMBER` não tem `PARTITION BY`?**
Porque o `WHERE` já filtra um único campeonato.

**47. Quais são os critérios de desempate?**
Pontos, vitórias, saldo de gols, gols marcados e `id_equipe`.

**48. Por que a colocação pode ser `NULL`?**
Porque quem ainda não jogou não tem posição; o relatório mostra `-`.

**49. O que faz `INSERT IGNORE` na inscrição?**
Insere; se a equipe já estiver inscrita, ignora a duplicata em vez de dar erro.

**50. O que o trigger de desinscrição bloqueia?**
Remover a inscrição de uma equipe que já tem qualquer partida no campeonato, inclusive cancelada.

**51. O que acontece ao alterar uma partida de campeonato?**
O `AFTER UPDATE` sincroniza o campeonato antigo e, se mudou, o novo.

**52. O que acontece ao excluir uma partida?**
Não há `BEFORE DELETE`; o `AFTER DELETE` sincroniza o campeonato da partida removida.

**53. Quais CHECKs controlam o placar?**
`chk_partida_gols` (≥ 0), `chk_partida_placar_completo` (os dois ou nenhum) e `chk_partida_placar_por_status` (coerência com o status).

**54. O que `chk_partida_tempo` garante?**
Que `tempo_partida` só seja preenchido com `status = 'EM_ANDAMENTO'`.

**55. Qual é a ordem dos triggers em um INSERT?**
`BEFORE INSERT` → gravação/CHECKs/FKs → `AFTER INSERT`.

**56. O que é `uq_partida_confronto`?**
Um `UNIQUE` que impede o mesmo confronto (campeonato, rodada, mandante, visitante) duas vezes.

**57. O que acontece se o `INSERT` violar um CHECK?**
O MySQL recusa com erro 3819 e a transação do comando é desfeita; o Java mostra a mensagem.

**58. O que é autocommit?**
O modo padrão em que cada comando é confirmado sozinho; o projeto não usa transação no Java.

**59. Por que a procedure recalcula do zero em vez de somar o último jogo?**
Porque partidas também são alteradas e excluídas; recalcular é mais simples e sempre bate com os dados.

**60. Qual é o erro de FK ao apagar um pai referenciado?**
Erro 1451 (`Cannot delete or update a parent row`); a tela traduz para “registro em uso”.

---

## 4. Relatórios e JOIN (61–72)

**61. Qual é o relatório principal e por quê?**
O de classificação, porque parte da `CLASSIFICACAO`, que liga campeonato e equipe e é alimentada pelas partidas; mostra todas as tabelas da rotina.

**62. Onde está o JOIN do relatório principal?**
`CLASSIFICACAO` + `CAMPEONATO` + `EQUIPE`, com subconsultas em `PARTIDA` + `ESTADIO`/`ARBITRO`.

**63. Por que `EQUIPE` aparece duas vezes no relatório de partidas?**
Uma vez como mandante (`m`) e outra como visitante (`v`).

**64. O que `DATE()` faz no filtro de período?**
Descarta a hora; o último dia do período entra inteiro.

**65. O que `DATE_FORMAT` faz?**
Formata a data para `dd/mm/aaaa hh:mm`.

**66. O que `COALESCE` faz no placar?**
Troca `NULL` por `-`, resultando em `- x -`.

**67. O que `COUNT(DISTINCT ...)` conta?**
Estádios/árbitros diferentes; se a equipe jogou duas vezes no mesmo estádio, conta 1.

**68. Como o aproveitamento é calculado?**
`pontos / (jogos * 3) * 100`, com `IF` para evitar divisão por zero e `ROUND(..., 1)`.

**69. Por que usar procedure no relatório?**
Porque recebe parâmetros (campeonato e período) e centraliza o JOIN no banco; o Java só chama e lê.

**70. Como o Java chama a procedure?**
`con.prepareCall("{call nome(?, ?)}")`, `setInt`/`setDate` e `executeQuery()`.

**71. Como gerar PDF?**
O botão chama `JTable.print(...)`; a janela de impressão do sistema permite “Salvar como PDF”.

**72. Por que o resumo divide jogos e gols por 2?**
Porque cada partida encerrada aparece na linha das duas equipes; a soma conta tudo em dobro.

---

## 5. Java, POO e MVC (73–90)

**73. Classe × objeto?**
Classe é o molde (`Equipe`); objeto é uma instância concreta criada com `new Equipe()`.

**74. O que é encapsulamento?**
Atributos `private` acessados por `get`/`set`; a classe controla seus dados.

**75. Onde há herança?**
As classes `Manter...` herdam de `DAO`; a `TelaPrincipal` herda de `JFrame`.

**76. Onde há polimorfismo?**
No `toString()` sobrescrito: o `JComboBox` chama o mesmo método e cada classe devolve um texto diferente.

**77. O que é construtor?**
O método chamado no `new`, que inicializa o objeto. Na `TelaPrincipal`, monta a tela e carrega os dados.

**78. O que é uma interface? Onde aparece?**
Um contrato de métodos. As interfaces do Swing (`ActionListener`, `DocumentListener`, `ListSelectionListener`) são implementadas por lambdas/classes anônimas.

**79. O que é uma classe anônima?**
Uma classe sem nome criada na hora. Aparece no `DocumentListener` e no `DefaultTableModel`.

**80. O que é uma lambda?**
Uma função anônima concisa. Aparece nos `ActionListener` dos botões e nos `Runnable` de validação.

**81. O que é uma model anêmica?**
Uma classe só com dados e get/set, sem regras. É o caso das classes de `controle`.

**82. Por que as datas são `String` no modelo?**
Para simplificar a tela e o `setString`; o MySQL converte para `DATE`/`DATETIME`. A validação de formato é feita no Java.

**83. Por que os gols são `Integer`?**
Porque o banco usa `NULL` para “sem placar”; `int` não representa `NULL`.

**84. Por que `Classificacao` não tem `toString()`?**
Porque não é usada em combo; a `JTable` usa os getters.

**85. Onde está o MVC?**
`controle` = Model; `entidade` = DAO; `visao` = View; o controller está nos eventos da tela.

**86. Por que não há classe Controller?**
Foi uma simplificação: a tela coordena as ações. A crítica é que ela ficou com 2.073 linhas.

**87. O que é um DAO?**
Data Access Object: classe que isola o SQL e monta os objetos.

**88. O que é `ArrayList`?**
Uma lista dinâmica; usada para devolver as linhas dos SELECTs.

**89. O que é `throws Exception`?**
Declara que o método pode lançar qualquer exceção; a tela captura e chama `mostrarErro`.

**90. O que é `toString()` e por que sobrescrever?**
É a representação textual do objeto; sobrescrever faz o combo mostrar `1 - Tigres FC` em vez do endereço de memória.

---

## 6. JDBC (91–102)

**91. O que é JDBC?**
A API do Java para acessar bancos relacionais; o driver traduz para o protocolo do MySQL.

**92. Quais são os passos de uma operação JDBC?**
Abrir conexão, preparar SQL, preencher parâmetros, executar, ler `ResultSet` e fechar.

**93. `Statement` × `PreparedStatement`?**
`Statement` concatena valores no SQL; `PreparedStatement` usa `?`, envia valores separados e evita SQL Injection.

**94. Como o `PreparedStatement` evita SQL Injection?**
Os valores não são interpretados como SQL; são enviados como dados.

**95. `execute` × `executeUpdate` × `executeQuery`?**
`execute` serve para qualquer comando; `executeUpdate` para INSERT/UPDATE/DELETE e devolve linhas afetadas; `executeQuery` para SELECT e devolve `ResultSet`.

**96. O que é `ResultSet`?**
O cursor com as linhas do resultado; `next()` avança e `getX()` lê.

**97. O que é `wasNull()`?**
Diz se a última coluna lida era `NULL`; necessário porque `getInt` devolve `0` para `NULL`.

**98. O que é `CallableStatement`?**
Um `PreparedStatement` para procedures, com a sintaxe `{call nome(?, ?)}`.

**99. O que o `finally` garante?**
Que o `fecharBanco()` execute mesmo se ocorrer exceção.

**100. O que o `fecharBanco()` fecha?**
Apenas o `PreparedStatement`; não fecha `ResultSet` nem `Connection`. É uma limitação.

**101. Por que o driver usa `com.mysql.jdbc.Driver`?**
É o nome antigo; o Connector/J 8+ recomenda `com.mysql.cj.jdbc.Driver`. Se a classe antiga não existir, a conexão fica nula.

**102. O que é um pool de conexões?**
Um conjunto de conexões reutilizáveis; o projeto não usa, abre uma por operação.

---

## 7. Swing e tela (103–120)

**103. O que é `JFrame`?**
A janela principal do Swing; `TelaPrincipal` herda dela.

**104. O que é `JTabbedPane`?**
O componente de abas; a tela tem 8 abas e a aba 8 tem 2 abas internas.

**105. O que é `JTable`?**
A tabela que exibe linhas/colunas; usa um `TableModel`.

**106. O que é `DefaultTableModel`?**
O modelo padrão da `JTable`; permite `addRow` e `setRowCount(0)`.

**107. Por que a tabela não é editável?**
Porque `isCellEditable` retorna `false`; a edição é feita pelo formulário.

**108. O que é `JComboBox`?**
Uma lista suspensa; pode guardar `String` ou objetos (`Equipe`, `Campeonato` etc.).

**109. Como o combo mostra o objeto?**
Chamando `toString()` do objeto.

**110. O que é `DocumentListener`?**
Um listener que avisa quando o texto de um campo muda (`insertUpdate`, `removeUpdate`, `changedUpdate`).

**111. O que é `ActionListener`?**
Um listener que reage a uma ação (clique de botão, seleção de combo).

**112. O que é `ListSelectionListener`?**
Um listener que reage à seleção de linhas de uma tabela/lista.

**113. O que é `getValueIsAdjusting()`?**
Um teste para saber se a seleção ainda está sendo ajustada; evita validar várias vezes.

**114. O que é `JOptionPane`?**
A classe de diálogos prontos: mensagem, confirmação, erro.

**115. O que é `JScrollPane`?**
O painel com barras de rolagem; envolve as tabelas.

**116. O que são `BorderLayout`, `GridLayout` e `FlowLayout`?**
Gerenciadores de layout: bordas, grade e fluxo horizontal.

**117. O que é a EDT?**
A Event Dispatch Thread, thread única do Swing que executa eventos e atualiza a interface.

**118. Por que a tela usa `EventQueue.invokeLater`?**
Para criar a janela na EDT, evitando problemas de concorrência.

**119. A tela usa threads de fundo?**
Não. As consultas JDBC rodam na EDT; se o banco demorar, a janela congela. Para o volume do trabalho, é aceitável.

**120. O que é `JTable.print`?**
O método que imprime a tabela; abre a janela de impressão do sistema, que permite salvar em PDF.

---

## 8. Código específico (121–140)

**121. Onde está a conexão JDBC?**
Em `DAO.abrirBanco()`; as classes `Manter...` herdam de `DAO`.

**122. Onde está a pesquisa individual?**
Nos métodos `pesquisarRegistro(...)` de cada `Manter...`; o botão Consultar chama esses métodos.

**123. Onde está a validação dos botões?**
Nas lambdas `validar` de cada aba, registradas por `vincularValidacao`; combos/tabelas usam `aoMudarCombo`/`aoSelecionarTabela`.

**124. Onde está o JOIN no Java?**
Em `ManterPartida.SELECT_PARTIDA`, `ManterJogador.SELECT_JOGADOR` e `ManterClassificacao` (listagem/pesquisa).

**125. Onde estão os triggers?**
Em `Champs_procedures_triggers.sql`; os `BEFORE` chamam `sp_validar_partida` e os `AFTER` chamam `sp_sincronizar_classificacao`.

**126. Onde estão as colunas geradas?**
Em `CLASSIFICACAO.saldo_gols` e `CLASSIFICACAO.pontuacao` (`Champs_schema.sql`).

**127. Onde estão os relatórios?**
No banco (`Champs_relatorios.sql`), no DAO (`ManterRelatorio`) e na aba 8 (`TelaPrincipal.criarPainelRelatorios`).

**128. Onde está a tradução de erros?**
Em `TelaPrincipal.mostrarErro`, que verifica `uq_jogador_camisa`, `chk_jogador_camisa`, `Duplicate entry`, `foreign key constraint fails` e falhas de conexão.

**129. Onde estão os caches?**
`cacheJogadores` e `cachePartidas` na `TelaPrincipal`; são preenchidos em `recarregarTudo` e usados na seleção das tabelas.

**130. Onde está a `main`?**
No fim de `TelaPrincipal.java`; configura o Nimbus e chama `new TelaPrincipal().setVisible(true)` na EDT.

**131. Onde está o `TelaPrincipal.form`?**
Existe, mas é um stub mínimo do NetBeans; a tela real é montada em `configurarJanelaCompleta` e nos `criarPainel...`.

**132. Onde está o `99_testes.sql`?**
Ele foi **substituído** por `Champs_dados.sql`, que tem mais casos distintos. O `testar.sh` foi atualizado para carregar `Champs_dados.sql` e conferir contagens, classificação, relatórios e regras. O roteiro antigo só existe no histórico do Git.

**133. Onde está o `testar.sh`?**
Em `SQL_Champs/testar.sh`; sobe MySQL no Docker, executa os scripts e verifica os resultados.

**134. Onde está o `Champs_dados.sql`?**
Em `SQL_Champs/Champs_dados.sql`; 4 campeonatos, 10 equipes, 21 jogadores e 28 partidas.

**135. Onde está a impressão/PDF?**
No `btnImprimirRel` da aba 8; usa `JTable.print(JTable.PrintMode.FIT_WIDTH, ...)`.

**136. Onde está a seleção de combo por ID?**
Nos métodos `selecionarComboCampeonato`, `selecionarComboEquipe`, `selecionarComboEstadio` e `selecionarComboArbitro`.

**137. Onde está a regra de mandante ≠ visitante?**
Na validação da aba 7 (`timesDiferentes`) e no `chk_partida_times` do banco.

**138. Onde está a regra de camisa 1..99?**
Na validação da aba 3 (`camisaOk`) e no `chk_jogador_camisa` do banco.

**139. Onde está a regra de capacidade > 0?**
Na validação da aba 4 (`inteiroPositivo`) e no `chk_estadio_capacidade`.

**140. Onde está a regra de data final ≥ inicial?**
Na validação da aba 1 e no `chk_campeonato_datas`.

---

## 9. Pegadinhas (141–160)

**141. O Java calcula a classificação?**
Não. O Java só exibe; o cálculo é do trigger/procedure no MySQL.

**142. Onde está o Controller?**
Não há classe separada; os `ActionListener` da `TelaPrincipal` fazem esse papel.

**143. A conexão é fechada?**
Em geral não: `fecharBanco()` fecha só o `PreparedStatement`. `ManterRelatorio` é a exceção e fecha a `Connection`.

**144. Por que `com.mysql.jdbc.Driver` e não `com.mysql.cj.jdbc.Driver`?**
É o nome antigo; o Connector/J 8+ recomenda o novo. Se a classe antiga não existir, o `Class.forName` falha e a conexão fica nula.

**145. Por que a linha da `JTable` corresponde ao cache?**
Porque `setAutoCreateRowSorter(false)` mantém a ordem do `SELECT` igual à do `ArrayList`.

**146. Por que `wasNull()` é necessário?**
Porque `getInt` devolve `0` para `NULL`; sem `wasNull`, um jogo sem placar pareceria `0 x 0`.

**147. Por que a data da partida é `String`?**
Para a tela e o DAO trabalharem com texto; o MySQL converte para `DATETIME`.

**148. O botão desabilitado dispara o `ActionListener`?**
Não. O `if (!btn.isEnabled())` dentro da lambda é uma proteção extra, redundante no uso normal.

**149. O que acontece se a partida mudar de campeonato?**
O `AFTER UPDATE` sincroniza o campeonato antigo e o novo.

**150. Partida cancelada ocupa rodada/árbitro/estádio?**
Não para as quatro regras; mas FKs, CHECKs e o `UNIQUE` de confronto continuam valendo.

**151. Partida cancelada bloqueia desinscrição?**
Sim; o trigger de desinscrição não filtra status.

**152. A posição do jogador é garantida pelo banco?**
Não; é `VARCHAR`; só o combo do Java limita.

**153. O status da classificação é automático?**
Não; `status_atual` é manual. A pontuação/colocação é automática.

**154. O que acontece se o banco estiver desligado ao abrir o programa?**
`recarregarTudo` captura e só imprime no console; a janela abre vazia. Ao tentar uma ação, `mostrarErro` mostra a mensagem de conexão.

**155. A tela usa `SwingWorker`?**
Não; o JDBC roda na EDT. É uma limitação de responsividade.

**156. O `TelaPrincipal.form` tem os componentes?**
Não; é um stub mínimo. A tela é feita em código.

**157. O que acontece se eu tentar excluir uma equipe com inscrição, mas sem partidas?**
A FK `fk_classificacao_equipe` (RESTRICT) bloqueia; remova a inscrição antes.

**158. O que acontece se eu tentar excluir um campeonato com partidas?**
A FK `fk_partida_campeonato` (RESTRICT) bloqueia; sem partidas, o `CASCADE` apaga as inscrições.

**159. Por que o relatório de classificação mostra `-` na posição?**
Porque `colocacao_equipe` é `NULL` para quem não jogou; a tela formata como `-`.

**160. Por que o resumo soma jogos e divide por 2?**
Porque cada partida encerrada aparece na linha das duas equipes; a soma dos jogos é o dobro do número de partidas.

---

## 10. Exercícios “o que acontece se…” (161–185)

**161. Inserir uma partida com data fora do período do campeonato.**
O `BEFORE INSERT` chama `sp_validar_partida`; a regra 1 falha; `SIGNAL` com “Data da partida fora do periodo do campeonato”; o INSERT é cancelado.

**162. Inserir uma partida com uma equipe não inscrita.**
Regra 2 falha; “Equipe nao inscrita neste campeonato”; INSERT cancelado.

**163. Inserir uma partida com a mesma equipe em dois jogos da rodada.**
Regra 3 falha; “Equipe ja possui partida nesta rodada”; INSERT cancelado.

**164. Inserir uma partida com o mesmo árbitro no mesmo horário.**
Regra 4 falha; “Arbitro ou estadio ja ocupado neste horario”; INSERT cancelado.

**165. Cancelar uma partida que estava `ENCERRADA`.**
O `BEFORE UPDATE` vê `NEW.status = CANCELADA` e pula as regras; o `AFTER UPDATE` recalcula; os pontos somem.

**166. Reativar uma partida cancelada para `ENCERRADA`.**
O `BEFORE UPDATE` valida normalmente; se a data estiver fora do campeonato, recusa.

**167. Excluir uma partida encerrada.**
O `AFTER DELETE` recalcula a classificação sem ela; os pontos e gols somem.

**168. Mudar a partida de campeonato.**
O `AFTER UPDATE` recalcula o campeonato antigo e o novo.

**169. Cadastrar dois jogadores com a mesma camisa na mesma equipe.**
`uq_jogador_camisa` gera `Duplicate entry`; a tela traduz para “Já existe um jogador com esse número de camisa nessa equipe.”

**170. Cadastrar jogador com camisa 0.**
`chk_jogador_camisa` recusa; a tela traduz para “O número da camisa precisa estar entre 1 e 99.”

**171. Excluir uma equipe que tem jogadores e partidas.**
A FK de jogador ou de partida (RESTRICT) bloqueia; erro 1451; a tela traduz.

**172. Remover a inscrição de uma equipe que já jogou.**
O trigger `trg_classificacao_before_delete` recusa com “Equipe possui partidas neste campeonato”.

**173. Excluir um campeonato que não tem partidas, mas tem inscrições.**
O `ON DELETE CASCADE` de `fk_classificacao_campeonato` apaga as inscrições.

**174. Pesquisar “1” no campo de campeonato.**
O DAO tenta `Integer.valueOf("1")`; o `ORDER BY (id = 1) DESC` coloca o campeonato de ID 1 antes de nomes que contenham “1”; `LIMIT 1` devolve um.

**175. Pesquisar “abc” no campo de campeonato.**
`codigo` fica `null`, o ID vira `-1`; a busca fica só por `nome LIKE '%abc%'`; se não achar, retorna `null` e a tela avisa.

**176. Editar uma célula da `JTable`.**
Não é possível; `isCellEditable` retorna `false`. A alteração é feita pelo formulário.

**177. Clicar no cabeçalho para ordenar.**
Não ordena; `setAutoCreateRowSorter(false)` está desligado para não dessincronizar o cache.

**178. Selecionar uma linha da tabela de partidas.**
O listener usa `cachePartidas.get(row)` para preencher o formulário; se o cache estiver vazio, não faz nada.

**179. Trocar o campeonato na aba 6.**
O `ActionListener` de `cbCampClass` chama `carregarTabelaClassificacao`, que consulta `listarPorCampeonato`.

**180. Gerar o relatório sem equipe inscrita.**
A lista vem vazia; a tabela fica vazia; o rodapé mostra “Equipes: 0”; a tela avisa para usar a aba de Inscrição.

**181. Gerar o relatório de partidas com data final antes da inicial.**
O botão fica desabilitado pela validação da tela (que checa a ordem). Se o método fosse chamado diretamente, ele só verifica `dataValida` (formato), não a ordem; a proteção contra fim < início está na validação do botão e na mensagem “datas válidas”.

**182. Imprimir sem gerar relatório.**
O botão fica desabilitado porque a tabela atual tem 0 linhas; se clicado, mostra “Gere um relatório antes de imprimir.”

**183. Trocar de aba interna do relatório.**
O `addChangeListener` reexecuta `validar`, habilitando/desabilitando a impressão conforme a tabela visível.

**184. Fechar a janela.**
`EXIT_ON_CLOSE` encerra o programa.

**185. O MySQL cai no meio de uma operação.**
A `SQLException` chega ao `catch` da tela; `mostrarErro` detecta falha de conexão e mostra a mensagem amigável.

---

## 11. Flashcards (respostas de uma linha)

| Termo | Resposta |
|---|---|
| PK | Identifica a linha; única e não nula. |
| FK | Aponta para a PK de outra tabela. |
| N:N | Muitos-para-muitos; resolvido por tabela intermediária. |
| `CLASSIFICACAO` | Inscrição + números da equipe no campeonato. |
| `PARTIDA` | Rotina principal; 5 FKs e regras. |
| Trigger | Bloco automático em INSERT/UPDATE/DELETE. |
| Procedure | Bloco nomeado chamado com `CALL`. |
| `BEFORE` | Valida antes de gravar. |
| `AFTER` | Atualiza depois de gravar. |
| `NEW`/`OLD` | Linha nova/antiga. |
| `SIGNAL` | Lança erro do usuário. |
| Coluna gerada | Calculada pelo MySQL. |
| `ENUM` | Lista fixa de valores. |
| `CHECK` | Regra de validação. |
| `UNIQUE` | Impede repetição. |
| `RESTRICT` | Não deixa apagar pai com filho. |
| `CASCADE` | Apaga/atualiza filhos junto. |
| `AUTO_INCREMENT` | MySQL gera o ID. |
| `InnoDB` | Engine com FK/transações. |
| `utf8mb4` | Codificação Unicode. |
| `JOIN` | Junta tabelas por FK=PK. |
| `INNER JOIN` | Só linhas com correspondência. |
| `LEFT JOIN` | Todas da esquerda, mesmo sem correspondência. |
| `ROW_NUMBER` | Numera linhas na ordem. |
| `SUM(v)` | Conta vitórias (booleano 1/0). |
| `UNION ALL` | Junta mantendo duplicatas. |
| `GROUP BY` | Agrupa para somar. |
| `COALESCE` | Troca `NULL` por outro valor. |
| `DATE_FORMAT` | Formata data. |
| `DATE()` | Descarta a hora. |
| `COUNT(DISTINCT)` | Conta valores diferentes. |
| `IF` | Condicional do MySQL. |
| JDBC | API Java para banco. |
| `PreparedStatement` | SQL com `?`; evita injection. |
| `CallableStatement` | Chamada de procedure. |
| `ResultSet` | Linhas do SELECT. |
| `wasNull` | Detecta `NULL` após `getInt`. |
| `finally` | Executa sempre. |
| DAO | Classe que isola SQL. |
| MVC | Model/View/Controller. |
| `JFrame` | Janela Swing. |
| `JTabbedPane` | Abas. |
| `JTable` | Tabela. |
| `DefaultTableModel` | Modelo da tabela. |
| `JComboBox` | Lista suspensa. |
| `DocumentListener` | Detecta mudança de texto. |
| `ActionListener` | Detecta clique/seleção. |
| `ListSelectionListener` | Detecta seleção de linha. |
| EDT | Thread do Swing. |
| `JTable.print` | Imprime/salva PDF. |

---

## 12. Roteiro de treino em 30 minutos

1. **5 min:** responda as perguntas 1–12 sem olhar.
2. **5 min:** responda 13–32 (banco).
3. **5 min:** responda 33–60 (triggers/procedures).
4. **5 min:** responda 61–72 (relatórios) e 91–102 (JDBC).
5. **5 min:** responda 103–120 (Swing).
6. **5 min:** leia as pegadinhas 141–160 e os exercícios 161–185.

Se errar alguma, volte ao guia correspondente e anote a resposta com suas palavras. Na apresentação, o importante é **explicar o porquê**, não decorar.

---

## 13. Últimas perguntas que todo mundo deveria saber responder

1. **Qual é a rotina principal?** Partida.
2. **Qual é a entidade central?** Classificação.
3. **Onde está o JOIN?** Nas procedures e nos DAOs de partida/jogador/classificação.
4. **Como a classificação é calculada?** Trigger `AFTER` → procedure que zera, soma e ordena.
5. **Quais são as quatro regras da partida?** Data, inscrição, rodada, árbitro/estádio.
6. **O que é `PreparedStatement`?** SQL com `?`; valores separados.
7. **O que é `CallableStatement`?** Chamada de procedure.
8. **Onde está o MVC?** `controle`, `entidade`, `visao`.
9. **Por que a tela valida e o banco também?** Usabilidade + garantia.
10. **Como gerar PDF?** `JTable.print` → janela de impressão → salvar em PDF.
