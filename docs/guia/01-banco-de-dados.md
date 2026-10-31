# Guia de Estudo - Banco de Dados `campeonato`

Este guia explica o banco do sistema para que **qualquer integrante** consiga abrir os scripts, explicar o que cada parte faz e responder às perguntas do professor. Ele é construído junto com o banco: cada etapa concluída acrescenta uma seção.

| Seção | Assunto | Arquivo explicado |
|---|---|---|
| 1 | Estrutura do banco (tabelas, chaves, regras) | `sql/Champs_schema.sql` |
| 2 | Regras automáticas (triggers e procedures) | `sql/Champs_procedures_triggers.sql` |
| 3 | Relatório e JOIN | `sql/Champs_relatorios.sql` |
| 4 | Como abrir e explicar cada arquivo | todos |
| 5 | Perguntas e respostas (revisão geral) | todos |

---

## 1. Estrutura do banco

### 1.1 O sistema em uma frase

Um sistema para **organizar campeonatos de futebol**: cadastramos campeonatos, equipes, estádios e árbitros, inscrevemos as equipes num campeonato e registramos as **partidas** (rotina principal). A classificação é calculada automaticamente a partir dos resultados.

### 1.2 As tabelas

| Tabela | Para que serve | Tipo no trabalho |
|---|---|---|
| `CAMPEONATO` | nome, período (`data_inicio`/`data_fim`) e situação do campeonato | CRUD simples |
| `EQUIPE` | nome e cidade do time | CRUD simples |
| `ESTADIO` | nome, cidade e capacidade | CRUD simples |
| `ARBITRO` | nome, registro na federação e categoria | CRUD simples |
| `JOGADOR` | jogadores de cada equipe | extra (opcional) |
| `CLASSIFICACAO` | **inscrição** de uma equipe num campeonato + seus números (jogos, vitórias, gols, pontos, colocação) | apoio à rotina principal |
| `PARTIDA` | o jogo: quem joga, onde, quando, quem apita, placar e situação | **CRUD principal** |

### 1.3 Como as tabelas se relacionam

```
                 CAMPEONATO
                 |        |
            (1:N)          (1:N)
                 |        |
        CLASSIFICACAO    PARTIDA ---- (N:1) ---- ESTADIO
        (inscricao)       |   |  \
                 |        |   |   `-- (N:1) ---- ARBITRO
            (N:1)         |   |
                 |   mandante visitante
                 |     (N:1)   (N:1)
                 |        |   |
                 `------ EQUIPE ---- (1:N) ---- JOGADOR
```

Lendo o desenho:

- **Um campeonato tem várias partidas**; cada partida pertence a um campeonato (`PARTIDA.id_campeonato`).
- **Cada partida liga duas equipes**: `id_mandante` (joga em casa) e `id_visitante`. As duas colunas apontam para a **mesma tabela** `EQUIPE`.
- Cada partida acontece **em um estádio** e é apitada **por um árbitro**.
- **Campeonato × Equipe é N:N** (um campeonato tem várias equipes; uma equipe pode disputar vários campeonatos). Relação N:N sempre precisa de uma **tabela intermediária**: aqui é a `CLASSIFICACAO`, que além de registrar a inscrição guarda os números da equipe naquele campeonato.
- **Uma equipe tem vários jogadores** (`JOGADOR.id_equipe`).

### 1.4 Chave primária (PK)

Identifica **cada linha de forma única**. Não repete e não pode ser nula.

- Quase todas as tabelas usam um número automático: `id_campeonato INT PRIMARY KEY AUTO_INCREMENT`. O MySQL gera 1, 2, 3… sozinho.
- `CLASSIFICACAO` usa **chave composta**: `PRIMARY KEY (id_campeonato, id_equipe)`. Ou seja, a mesma equipe não pode ser inscrita duas vezes no mesmo campeonato, mas pode estar em campeonatos diferentes.

### 1.5 Chave estrangeira (FK)

É uma coluna que **aponta para a chave primária de outra tabela**. O banco garante que o valor existe lá. Exemplo em `PARTIDA`:

```sql
CONSTRAINT fk_partida_arbitro
    FOREIGN KEY (id_arbitro) REFERENCES ARBITRO (id_arbitro)
    ON DELETE RESTRICT ON UPDATE CASCADE
```

- Não dá para gravar uma partida com `id_arbitro = 99` se esse árbitro não existe.
- **`ON DELETE RESTRICT`**: não deixa apagar um árbitro que já apitou alguma partida. O banco responde com o **erro 1451** (`Cannot delete or update a parent row`). Isso protege o histórico; o Java vai mostrar uma mensagem amigável no lugar desse erro.
- **`ON DELETE CASCADE`** (usado só em `fk_classificacao_campeonato`): ao apagar um campeonato, as inscrições dele são apagadas junto. Mas, se o campeonato já tiver partidas, a FK de `PARTIDA` (que é `RESTRICT`) impede a exclusão antes disso.
- **`ON UPDATE CASCADE`**: se o id do pai mudar, os filhos acompanham. Em `fk_partida_mandante` e `fk_partida_visitante` foi usado `ON UPDATE RESTRICT` porque essas colunas participam de um CHECK (`chk_partida_times`), e o MySQL não permite CASCADE em colunas usadas por CHECK.

### 1.6 Regras de validação: CHECK, UNIQUE, NOT NULL, ENUM

| Regra | Onde | O que impede |
|---|---|---|
| `chk_campeonato_datas` | CAMPEONATO | terminar antes de começar (`data_fim >= data_inicio`) |
| `chk_estadio_capacidade` | ESTADIO | capacidade zero ou negativa |
| `chk_jogador_camisa` | JOGADOR | camisa fora de 1 a 99 |
| `chk_classificacao_soma` | CLASSIFICACAO | jogos ≠ vitórias + empates + derrotas |
| `chk_classificacao_colocacao` | CLASSIFICACAO | colocação 0 ou negativa (pode ser nula: quem ainda não jogou) |
| `chk_partida_times` | PARTIDA | um time jogar contra ele mesmo |
| `chk_partida_rodada`, `chk_partida_acrescimo`, `chk_partida_gols` | PARTIDA | rodada ≤ 0, acréscimo ou gols negativos |
| `chk_partida_placar_completo` | PARTIDA | placar pela metade (só gols do mandante) |
| `chk_partida_placar_por_status` | PARTIDA | jogo encerrado sem placar, ou agendado/cancelado com placar |
| `uq_equipe_nome`, `uq_campeonato_nome`, `uq_arbitro_registro`, `uq_estadio_nome_cidade` | várias tabelas | cadastros duplicados |
| `uq_jogador_camisa` | JOGADOR | dois jogadores com a mesma camisa no mesmo time |
| `uq_partida_confronto` | PARTIDA | o mesmo confronto duas vezes na mesma rodada |

- **`NOT NULL`**: o campo é obrigatório.
- **`ENUM`**: só aceita valores de uma lista. Ex.: `status` da partida só pode ser `AGENDADA`, `EM_ANDAMENTO`, `ENCERRADA` ou `CANCELADA`. No Java esses valores aparecem num `JComboBox`.
- **`DEFAULT`**: valor usado quando não informamos nada (ex.: partida nasce `AGENDADA`).

### 1.7 Colunas calculadas pelo próprio banco

Em `CLASSIFICACAO`:

```sql
saldo_gols INT GENERATED ALWAYS AS (gols_marcados - gols_sofridos) STORED,
pontuacao  INT GENERATED ALWAYS AS (vitorias * 3 + empates) STORED,
```

Ninguém grava essas colunas: o MySQL calcula sempre que os valores de origem mudam. Vitória vale 3 pontos e empate 1, então a pontuação **nunca fica errada**.

### 1.8 O que estava quebrado e como foi corrigido

Ao rodar o script antigo, o MySQL parava com:

```
ERROR 3820: Check constraint 'chk_classificacao_colocacao' refers to non-existing column 'colocacao_equipe'.
```

A coluna tinha sido renomeada para `colocacao`, mas a regra CHECK e a procedure de classificação ainda usavam `colocacao_equipe`. Como a tabela `CLASSIFICACAO` não era criada, **nenhuma partida podia ser salva** (o trigger da partida tenta atualizar a classificação). A correção foi padronizar o nome `colocacao_equipe` em todos os lugares.

Também renomeamos os arquivos com o prefixo do projeto (`Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql`) e criamos os testes.

### 1.9 Como testar

Veja o README. Em resumo: `bash sql/testar.sh` (com Docker) ou rodar `99_testes.sql` no Workbench num banco recém-criado. O arquivo de testes tem seções:

- **A) Dados de teste:** 1 campeonato, 6 equipes, 3 estádios, 3 árbitros, 8 partidas.
- **B) Classificação:** confere se os triggers calcularam certo (Tigres FC 1º com 6 pontos, por exemplo). Cada linha mostra `OK` ou `FALHOU`.
- **D) Erros esperados:** comandos que **devem** ser recusados, como apagar um campeonato que tem partidas.

### Perguntas prováveis (seção 1)

**Qual é a diferença entre chave primária e chave estrangeira?**
A primária identifica cada linha da própria tabela (ex.: `id_equipe` em `EQUIPE`). A estrangeira é uma coluna que aponta para a primária de outra tabela (ex.: `PARTIDA.id_mandante` aponta para `EQUIPE.id_equipe`) e garante que o valor existe.

**Por que `CLASSIFICACAO` tem duas colunas na chave primária?**
Porque ela representa a relação N:N entre campeonato e equipe. A combinação `(id_campeonato, id_equipe)` é única: uma equipe só se inscreve uma vez em cada campeonato.

**Como a partida sabe qual é o time da casa e o visitante, se as duas colunas apontam para EQUIPE?**
São duas chaves estrangeiras diferentes (`fk_partida_mandante` e `fk_partida_visitante`) para a mesma tabela. O CHECK `chk_partida_times` impede que sejam o mesmo time.

**O que acontece se eu tentar excluir uma equipe que já jogou?**
O banco recusa (erro 1451) por causa do `ON DELETE RESTRICT`. Assim o histórico das partidas não fica com referências quebradas.

**Quem calcula os pontos da equipe?**
O próprio banco: `pontuacao` é uma coluna gerada (`vitorias * 3 + empates`). As vitórias e empates são contados pela procedure de classificação (seção 2).

---

## 2. Regras automáticas (`Champs_procedures_triggers.sql`)

### 2.1 Procedure e trigger, em uma frase cada

- **Procedure (`PROCEDURE`)**: um bloco de comandos SQL **com nome**, guardado no banco, que pode receber parâmetros. É como um método: escrevemos uma vez e chamamos com `CALL nome(...)`.
- **Trigger (`TRIGGER`)**: um bloco que o **próprio banco executa sozinho** quando algo acontece numa tabela (`INSERT`, `UPDATE` ou `DELETE`). Ninguém chama um trigger: ele "dispara".
  - **`BEFORE`** roda **antes** de gravar: serve para **validar** e, se preciso, impedir a gravação.
  - **`AFTER`** roda **depois** de gravar: serve para **atualizar outras tabelas** com base no que mudou.
  - Dentro do trigger, `NEW` é a linha nova (como vai ficar) e `OLD` é a linha antiga (como estava).

### 2.2 O que acontece quando o Java salva uma partida

```
Java: INSERT INTO PARTIDA ...  (ou UPDATE)
   |
   v
trg_partida_before_insert / trg_partida_before_update
   |  CALL sp_validar_partida(...)
   |  regra quebrada? -> SIGNAL -> gravacao CANCELADA, erro volta para o Java
   v
MySQL grava a partida (e confere as FKs e os CHECKs)
   |
   v
trg_partida_after_insert / trg_partida_after_update / trg_partida_after_delete
   |  CALL sp_sincronizar_classificacao(id_campeonato)
   v
CLASSIFICACAO atualizada (pontos, saldo, colocacao)
```

Vantagem: **as regras valem sempre**, mesmo que alguém altere os dados direto pelo Workbench, sem passar pelo sistema.

### 2.3 As regras da partida (`sp_validar_partida`)

A procedure recebe os dados da partida e testa 4 regras, nesta ordem. Partidas `CANCELADA` não são validadas e não contam como conflito (um jogo cancelado não ocupa ninguém).

| # | Regra | Mensagem | Exemplo recusado |
|---|---|---|---|
| 1 | data dentro do período do campeonato | `Data da partida fora do periodo do campeonato` | jogo em 2030 num campeonato de 2026 |
| 2 | mandante e visitante inscritos | `Equipe nao inscrita neste campeonato` | equipe cadastrada mas sem inscrição |
| 3 | equipe não joga duas vezes na rodada | `Equipe ja possui partida nesta rodada` | Tigres já jogou a rodada 1 e é escalado de novo |
| 4 | árbitro e estádio livres no horário | `Arbitro ou estadio ja ocupado neste horario` | mesmo árbitro em dois jogos às 16:00 do mesmo dia |

Detalhes que costumam ser perguntados:

- **`SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '...'`** é como o MySQL "lança um erro" escrito por nós (parecido com `throw` no Java). `45000` é o código para erro definido pelo usuário. No Java, esse erro chega como `SQLException` e `e.getMessage()` traz o texto da mensagem.
- **`id_partida <> p_id_partida`**: quando alteramos uma partida (ex.: lançar o placar), ela já está na tabela. Sem essa condição, a partida "conflitaria com ela mesma" na regra 3 e na 4.
- **`IF EXISTS (SELECT 1 FROM ...)`**: pergunta "existe pelo menos uma linha assim?". O `1` é só um valor qualquer; o que importa é se a consulta encontra algo.
- A regra 4 não filtra por campeonato: um árbitro não pode estar em dois lugares ao mesmo tempo, nem em campeonatos diferentes.

Também existe o CHECK **`chk_partida_tempo`** (no `Champs_schema.sql`): `tempo_partida` (1º/2º tempo) só pode ser preenchido com o jogo `EM_ANDAMENTO`.

### 2.4 A classificação automática (`sp_sincronizar_classificacao`)

Estratégia **"recalcular do zero"**: em vez de somar e subtrair a cada mudança (o que acumula erros), a procedure refaz a conta inteira do campeonato a partir das partidas. São 3 passos:

1. **Zera** jogos, vitórias, empates, derrotas, gols e colocação de todas as equipes inscritas no campeonato.
2. **Soma as partidas `ENCERRADA`**. Cada partida gera duas linhas: uma para o mandante e uma para o visitante (com os gols invertidos), unidas com `UNION ALL`. Depois o `GROUP BY id_equipe` soma tudo por equipe. Em `(gols_mandante > gols_visitante) AS v`, a comparação vale 1 quando é verdadeira e 0 quando é falsa, então somar `v` dá o número de vitórias.
3. **Calcula a colocação** com `ROW_NUMBER() OVER (ORDER BY pontuacao DESC, vitorias DESC, saldo_gols DESC, gols_marcados DESC, id_equipe)`: numera as equipes 1, 2, 3… na ordem dos critérios de desempate. Quem ainda não jogou fica com colocação vazia (`NULL`).

`pontuacao` e `saldo_gols` não aparecem nos passos porque são colunas geradas (seção 1.7): mudam sozinhas quando vitórias, empates e gols mudam.

### 2.5 Inscrição de equipes

- **`sp_inscrever_equipe(campeonato, equipe)`**: insere a equipe em `CLASSIFICACAO` e recalcula. O `INSERT IGNORE` evita erro se ela já estiver inscrita.
- **Desinscrever** é um `DELETE` em `CLASSIFICACAO`. O trigger **`trg_classificacao_before_delete`** impede a saída de uma equipe que já tem partidas no campeonato (`Equipe possui partidas neste campeonato`), para não deixar partidas com equipes "fora" da tabela.
- A procedure de classificação **não inscreve ninguém sozinha**: só quem foi inscrito aparece na tabela. Antes, uma equipe não inscrita que jogasse era inscrita automaticamente, o que escondia o erro.
- `status_atual` (`A_JOGAR`, `CLASSIFICADO`, `ELIMINADO`) é preenchido **manualmente** pelo usuário; o banco não calcula.

### 2.6 Como testar

No `99_testes.sql`:

- **Seção D**: um comando que deve falhar para cada regra, cada um marcado com `-- ESPERADO ERRO: <mensagem>`.
- **Seção E**: alterações que **devem** funcionar: lançar o placar de um jogo agendado (e a Panteras ganha 3 pontos), jogo no último dia do campeonato, jogo no horário de um jogo cancelado, inscrever e desinscrever uma equipe sem jogos.

### Perguntas prováveis (seção 2)

**Qual a diferença entre trigger e procedure?**
A procedure é chamada por alguém (`CALL`). O trigger é executado automaticamente pelo banco quando acontece um `INSERT`, `UPDATE` ou `DELETE` na tabela. No nosso banco, os triggers chamam as procedures.

**Por que usar `BEFORE` para validar e `AFTER` para a classificação?**
Validação precisa acontecer antes de gravar, para poder cancelar. A classificação só pode ser recalculada depois que a partida nova (ou alterada) já está na tabela.

**Se o banco já valida, o Java precisa validar também?**
O banco é a última barreira: garante as regras até para quem usa o Workbench. O Java valida o básico (campo vazio, número inválido) para dar uma resposta rápida e mostra ao usuário a mensagem que o banco devolve.

**Como a mensagem de erro chega na tela?**
O `SIGNAL` gera um erro no MySQL; o JDBC transforma em `SQLException`; o Controller pega com `try/catch` e mostra `e.getMessage()` num `JOptionPane`.

**Por que recalcular a classificação inteira em vez de só somar o último jogo?**
Porque partidas também são alteradas e excluídas. Recalcular do zero é mais simples e o resultado sempre bate com as partidas gravadas.

---

## 3. Relatório e JOIN (`Champs_relatorios.sql`)

### 3.1 O problema que o JOIN resolve

A tabela `PARTIDA` guarda só **códigos** das tabelas relacionadas:

| id_partida | id_mandante | id_visitante | id_estadio | id_arbitro |
|---|---|---|---|---|
| 1 | 1 | 2 | 1 | 1 |

Para o usuário isso não diz nada. O enunciado exige mostrar **informações compreensíveis**:

| rodada | data_hora | mandante | visitante | estadio | arbitro | placar | status |
|---|---|---|---|---|---|---|---|
| 1 | 05/09/2026 16:00 | Tigres FC | Aguas EC | Arena Central | Carlos Andrade | 2 x 1 | ENCERRADA |

### 3.2 Como o `INNER JOIN` funciona

```sql
  FROM PARTIDA p
 INNER JOIN EQUIPE  m ON m.id_equipe  = p.id_mandante
 INNER JOIN EQUIPE  v ON v.id_equipe  = p.id_visitante
 INNER JOIN ESTADIO e ON e.id_estadio = p.id_estadio
 INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro
```

- Para cada partida, o `INNER JOIN` busca na outra tabela a linha cuja **chave primária** é igual à **chave estrangeira** da partida (a condição depois do `ON`). Assim conseguimos usar `m.nome`, `e.nome` etc.
- **`INNER`** significa: só aparecem as linhas que têm correspondência dos dois lados. Como as FKs garantem que o mandante, o estádio e o árbitro existem, nenhuma partida se perde.
- **Apelidos** (`p`, `m`, `v`, `e`, `a`) deixam o SQL mais curto.
- **Por que `EQUIPE` aparece duas vezes?** A partida tem duas equipes. Cada `JOIN` com `EQUIPE` busca uma delas: `m` pelo `id_mandante` e `v` pelo `id_visitante`. Sem apelidos diferentes, o MySQL não saberia de qual `nome` estamos falando.

### 3.3 A procedure `sp_relatorio_partidas_periodo`

```sql
CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');
```

- **Parâmetros**: o campeonato e o período (data inicial e final). É por isso que o relatório é uma **procedure** e não uma consulta fixa: a tela passa os filtros escolhidos pelo usuário.
- **`DATE(p.data_partida) BETWEEN p_data_inicio AND p_data_fim`**: `data_partida` tem data e hora. O `DATE()` descarta a hora, então um jogo às 20:00 do último dia ainda entra no período.
- **`DATE_FORMAT(..., '%d/%m/%Y %H:%i')`**: mostra a data no formato brasileiro (`05/09/2026 16:00`).
- **`CONCAT(COALESCE(p.gols_mandante, '-'), ' x ', ...)`**: monta o placar `2 x 1`. `COALESCE` troca o `NULL` (jogo sem placar) por `-`, resultando em `- x -`.
- **`ORDER BY p.data_partida`**: em ordem cronológica.

Ele é o relatório da **rotina principal** que o enunciado exige (seção 6): trabalha só com partidas, filtradas por período, como no exemplo "consultas agendadas por período".

### 3.4 Como o Java vai usar (Fase 5)

```java
CallableStatement cs = conexao.prepareCall("{call sp_relatorio_partidas_periodo(?, ?, ?)}");
cs.setInt(1, idCampeonato);
cs.setDate(2, java.sql.Date.valueOf(dataInicio));
cs.setDate(3, java.sql.Date.valueOf(dataFim));
ResultSet rs = cs.executeQuery();   // cada rs.next() é uma linha da tabela do relatório
```

A tela de relatório mostra as linhas num `JTable` e os totais no rodapé. O botão **Imprimir** usa `JTable.print()`, e a janela de impressão permite salvar em PDF.

O JOIN também vai aparecer **no código Java**: a listagem de partidas (`PartidaDAO`) e a tabela de classificação (`ClassificacaoDAO`) usam `SELECT ... INNER JOIN` direto, sem procedure.

### 3.5 Como testar

Seção **C** do `99_testes.sql`: as duas chamadas mostram tabelas que devem bater com o comentário `ESPERADO` logo acima (setembro: 5 jogos; só o dia 05/09: 2 jogos).

### Perguntas prováveis (seção 3)

**Onde está o JOIN do sistema?**
Em `sql/Champs_relatorios.sql`, na procedure `sp_relatorio_partidas_periodo`, que junta `PARTIDA` com `EQUIPE` (duas vezes), `ESTADIO` e `ARBITRO`. No Java, também nas consultas de listagem de partidas e da classificação.

**O que aconteceria sem o JOIN?**
A tela mostraria só códigos (`id_mandante = 1`), e o usuário não saberia quais times jogaram.

**Qual a diferença entre `INNER JOIN` e `LEFT JOIN`?**
O `INNER` só traz linhas que têm correspondência nas duas tabelas. O `LEFT` traz todas as linhas da tabela da esquerda, mesmo sem correspondência (com `NULL` nas colunas da direita). Aqui toda partida tem equipes, estádio e árbitro (as FKs garantem), então o `INNER` basta.

**Por que o relatório é sobre partidas e não sobre equipes?**
O enunciado exige um relatório da **rotina principal**, e a nossa rotina principal é o registro de partidas.

---

## 4. Como abrir e explicar cada arquivo

Roteiro para a apresentação: abra os arquivos **nesta ordem** e siga os blocos (cada bloco começa com uma linha `-- -----`).

### `Champs_schema.sql`: a estrutura

1. **Cabeçalho**: banco para MySQL 8, ordem de execução `Champs_schema -> Champs_procedures_triggers -> Champs_relatorios`.
2. **`DROP DATABASE IF EXISTS` / `CREATE DATABASE`**: apaga e recria o banco do zero, com `utf8mb4` (aceita acentos).
3. **Tabelas simples** (`CAMPEONATO`, `EQUIPE`, `ESTADIO`, `ARBITRO`): mostre a PK `AUTO_INCREMENT`, um `UNIQUE` e um `CHECK` (ex.: `chk_campeonato_datas`).
4. **`JOGADOR`**: primeiro exemplo de **FK** (`fk_jogador_equipe`), relação 1:N com `EQUIPE`.
5. **`CLASSIFICACAO`**: PK composta (relação N:N), FKs, **colunas geradas** (`saldo_gols`, `pontuacao`).
6. **`PARTIDA`**: 5 FKs (campeonato, mandante, visitante, estádio, árbitro) e os CHECKs da partida (`chk_partida_times`, `chk_partida_placar_por_status`, `chk_partida_tempo`).

### `Champs_procedures_triggers.sql`: as regras automáticas

1. **Cabeçalho**: o desenho do fluxo (BEFORE valida → grava → AFTER recalcula) e as 4 regras.
2. **`sp_sincronizar_classificacao`**: os 3 passos (zera, soma partidas encerradas, calcula colocação).
3. **`sp_inscrever_equipe`**: inscrição + recálculo.
4. **`sp_validar_partida`**: um `IF ... SIGNAL` por regra.
5. **Triggers BEFORE** (`trg_partida_before_insert`, `trg_partida_before_update`): só chamam a validação com os valores de `NEW`.
6. **Triggers AFTER** (`trg_partida_after_insert`, `_update`, `_delete`): só chamam a sincronização.
7. **`trg_classificacao_before_delete`**: impede desinscrever quem já jogou.

Dica: explique `DELIMITER $$`. Como os blocos têm vários `;` dentro, trocamos temporariamente o fim de comando para `$$`, para o Workbench não cortar a procedure no meio. No final voltamos com `DELIMITER ;`.

### `Champs_relatorios.sql`: o relatório

1. **`sp_relatorio_partidas_periodo`**: parâmetros, os 4 `INNER JOIN`, o filtro de período e a formatação (data e placar).

### `99_testes.sql`: a prova de que funciona

Seções A (dados), B (classificação), C (relatório), D (erros esperados) e E (alterações válidas). Uma boa demonstração: rodar um comando da seção D no Workbench e mostrar a mensagem de erro.

---

## 5. Perguntas e respostas (revisão geral)

**1. Quantas tabelas o banco tem e quais são os CRUDs?**
Sete. CRUDs simples: `CAMPEONATO`, `EQUIPE`, `ESTADIO`, `ARBITRO`. CRUD principal: `PARTIDA`. `CLASSIFICACAO` é a inscrição com a classificação. `JOGADOR` é extra.

**2. Qual é a rotina principal e por que ela não é um cadastro simples?**
O registro de partidas. Uma partida relaciona campeonato, duas equipes, estádio e árbitro (5 chaves estrangeiras) e ainda afeta a classificação.

**3. O que é `AUTO_INCREMENT`?**
O MySQL gera o valor da chave sozinho (1, 2, 3…). No Java, não informamos o id ao inserir.

**4. O que é uma relação 1:N? Dê um exemplo do projeto.**
Uma linha de uma tabela se liga a várias de outra. Uma equipe tem vários jogadores (`JOGADOR.id_equipe`); um campeonato tem várias partidas.

**5. E N:N?**
Os dois lados têm vários: um campeonato tem várias equipes e uma equipe disputa vários campeonatos. É resolvida com a tabela intermediária `CLASSIFICACAO`.

**6. Para que serve o `UNIQUE`?**
Impede valores repetidos numa coluna que não é a PK. Ex.: `uq_arbitro_registro` impede dois árbitros com o mesmo registro na federação.

**7. Para que serve o `CHECK`? Dê um exemplo.**
Valida uma condição antes de gravar. `chk_partida_times` impede um time de jogar contra ele mesmo.

**8. O que é um `ENUM`?**
Uma coluna que só aceita valores de uma lista fixa, como `status` da partida (`AGENDADA`, `EM_ANDAMENTO`, `ENCERRADA`, `CANCELADA`).

**9. O que significa `ON DELETE RESTRICT`?**
Não deixa apagar uma linha que é referenciada por outra tabela. Ex.: não dá para apagar um estádio que já recebeu partidas (erro 1451).

**10. Onde usamos `ON DELETE CASCADE` e por quê?**
Em `fk_classificacao_campeonato`: ao apagar um campeonato (sem partidas), as inscrições dele somem junto, porque não fazem sentido sozinhas.

**11. O que é uma coluna gerada?**
Coluna calculada pelo banco a partir de outras. `pontuacao = vitorias * 3 + empates` e `saldo_gols = gols_marcados - gols_sofridos`.

**12. O que é uma stored procedure?**
Um bloco de SQL com nome, guardado no banco, que recebe parâmetros e é chamado com `CALL`. Ex.: `sp_relatorio_partidas_periodo`.

**13. O que é um trigger?**
Um bloco executado automaticamente pelo banco quando há `INSERT`, `UPDATE` ou `DELETE` numa tabela. Ex.: `trg_partida_after_insert` recalcula a classificação.

**14. O que são `NEW` e `OLD` dentro de um trigger?**
`NEW` é a linha como vai ficar (insert/update); `OLD` é como estava (update/delete).

**15. O que faz o `SIGNAL SQLSTATE '45000'`?**
Gera um erro com a nossa mensagem e cancela o comando, como um `throw` no Java.

**16. Quais regras o banco aplica a uma partida?**
Data dentro do período do campeonato; equipes inscritas; equipe não joga duas vezes na rodada; árbitro e estádio não ficam em dois jogos no mesmo horário. Partidas canceladas não contam.

**17. Como a classificação é calculada?**
A cada mudança em `PARTIDA`, um trigger chama `sp_sincronizar_classificacao`, que zera os números, soma as partidas encerradas e numera as equipes com `ROW_NUMBER()` pelos critérios de desempate.

**18. Quais são os critérios de desempate?**
Pontos, depois vitórias, saldo de gols, gols marcados e, por último, o id da equipe (para nunca empatar).

**19. Por que só partidas `ENCERRADA` contam para a classificação?**
Jogos agendados ou em andamento ainda não têm resultado final, e jogos cancelados não aconteceram.

**20. O que é `INNER JOIN` e onde ele aparece?**
Junta linhas de tabelas relacionadas pela igualdade entre FK e PK. Aparece no relatório (`Champs_relatorios.sql`) e nas consultas Java de partidas e classificação.

**21. Por que usar procedure para o relatório?**
Porque ele recebe filtros (campeonato e período) escolhidos pelo usuário na tela.

**22. Como os dados de demonstração entram no banco?**
Pelas telas do sistema Java (os CRUDs). Os dados do `99_testes.sql` servem só para testar o banco.

**23. Como provar que as regras funcionam?**
Rodar `99_testes.sql`: todo teste mostra `OK` e os comandos marcados `ESPERADO ERRO` são recusados com a mensagem certa. Com Docker, `bash sql/testar.sh` faz isso automaticamente.
