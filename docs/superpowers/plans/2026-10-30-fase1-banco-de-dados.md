# Fase 1 — Banco de Dados `campeonato` — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Banco `campeonato` criável sem erros, com as regras da partida protegidas, o relatório principal com JOIN pronto para o Java e um guia de estudo que permita a qualquer integrante explicar tudo.

**Architecture:** Três scripts de instalação (`Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql`) e um arquivo de testes em SQL puro (`99_testes.sql`) que qualquer um lê e roda no Workbench. O `testar.sh` só automatiza esse mesmo roteiro num MySQL descartável no Docker.

**Tech Stack:** MySQL 8.4 (mínimo 8.0.16), Docker (`mysql:8.4`), bash.

**Spec:** enunciado do trabalho + roteiro geral [`2026-10-30-campeonato-mvc.md`](2026-10-30-campeonato-mvc.md).

## Global Constraints

- **Simplicidade acima de tudo:** cada linha precisa ser explicável na apresentação. Nada de SQL dinâmico (`PREPARE`), helpers de teste, janelas de tempo ou regras “a mais”. Na dúvida, fica de fora.
- Comentários em português, curtos, explicando **o porquê**, no padrão já usado (blocos separados por `-- -----...`).
- Padrões já estabelecidos: tabelas em MAIÚSCULAS, colunas `snake_case`, PK `id_<tabela>`, constraints `fk_`/`uq_`/`chk_<tabela>_<regra>`, procedures `sp_`, triggers `trg_<tabela>_<momento>_<evento>`, `DELIMITER $$` com `DROP ... IF EXISTS` antes de cada objeto.
- Identificadores e mensagens de erro sem acento (as mensagens vão aparecer no Java).
- `sql/` ao final: **somente** `Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql`, `99_testes.sql`, `testar.sh`. Sem views, sem script de dados (os dados reais são cadastrados pelo Java).
- **Sem commits/PRs pelo executor.** Cada etapa termina com: testes passando + seção correspondente no guia de estudo + lista de arquivos alterados. O time revisa, faz commit e PR (`Closes #N`).

## Review Focus

1. **Lançar o placar de um jogo agendado** não pode acusar conflito com o próprio jogo (as regras ignoram a própria partida). Teste na Etapa 2, seção E.
2. **Último dia do campeonato** é aceito (comparação por `DATE(...) BETWEEN`). Teste na Etapa 2, seção E.
3. **Jogo CANCELADO libera** rodada, árbitro e estádio para remarcar. Teste na Etapa 2, seção E.
4. **Excluir campeonato que tem partidas** é bloqueado pela FK (erro 1451) — o Java da Fase 2 mostra mensagem amigável. Teste na Etapa 1, seção D.
5. **Desinscrever equipe que já jogou** é bloqueado. Teste na Etapa 2, seção D.

---

## Arquivos

| Arquivo | Conteúdo | Etapa |
|---|---|---|
| `sql/Champs_schema.sql` | `git mv` de `bd_campeonato.sql`; corrige coluna; CHECK de `tempo_partida` | 1, 2 |
| `sql/Champs_procedures_triggers.sql` | `git mv` de `procedures_triggers.sql`; regras da partida e da inscrição | 1, 2 |
| `sql/Champs_relatorios.sql` | procedure do relatório principal com `INNER JOIN` | 3 |
| `sql/99_testes.sql` | roteiro de testes em SQL puro | 1–3 |
| `sql/testar.sh` | roda tudo num MySQL descartável (≈20 linhas) | 1 |
| `docs/guia/01-banco-de-dados.md` | guia de estudo do banco (uma seção por etapa) | 1–4 |
| `docs/guia/00-guia-de-apresentacao.md` | guia global do projeto + banco de perguntas | 4 |
| `README.md` | como instalar e testar o banco | 1 |

## Formato do `99_testes.sql` (vale para todas as etapas)

SQL puro, dividido em seções, nesta ordem:

```
-- A) DADOS DE TESTE          inserts da tabela abaixo
-- B) TESTES DE CLASSIFICACAO SELECT IF(<condicao>, 'OK', 'FALHOU') AS '<o que esta sendo testado>';
-- C) TESTE DO RELATORIO      CALL ... com o resultado esperado escrito em comentario
-- D) ERROS ESPERADOS         -- ESPERADO ERRO: <mensagem exata>   seguido do comando que deve falhar
-- E) ALTERACOES VALIDAS      comandos que devem funcionar + SELECT IF(...) conferindo o efeito
```

`D` vem antes de `E` porque `E` altera os dados; assim `B` e `C` sempre veem a fixture original.

**`testar.sh`** (nesta ordem): sobe `poo-mysql-teste` (`mysql:8.4`, senha `teste`, `--rm`) e garante `docker stop` ao sair (`trap`); espera o MySQL responder; roda `01`, `02`, `04` (para no primeiro erro); roda `99_testes.sql` com `--force -t` e mostra a saída; no fim imprime `TODOS OS TESTES PASSARAM` somente se (a) não houver `FALHOU` na saída, (b) cada `-- ESPERADO ERRO: <msg>` do arquivo aparecer na saída e (c) não houver nenhum erro além dos esperados. Caso contrário imprime o que faltou e sai com 1.

**Dados de teste (seção A)** — campeonato 1 `Copa Regional 2026` (`2026-08-01` a `2026-12-20`, `EM_ANDAMENTO`); equipes 1–6 `Tigres FC`/Sao Paulo, `Aguas EC`/Campinas, `Leoes do Norte`/Recife, `Falcoes United`/Belo Horizonte, `Tubaroes SC`/Santos, `Panteras AC`/Curitiba; estádios 1–3 `Arena Central`/Sao Paulo/45000, `Estadio do Vale`/Campinas/18000, `Arena Litoral`/Santos/22000; árbitros 1–3 `Carlos Andrade`/FED-1001/FIFA, `Marina Souza`/FED-1002/NACIONAL, `Paulo Ribeiro`/FED-1003/ESTADUAL; equipes 1–6 inscritas no campeonato 1; partidas:

| id | rod | jogo | est | arb | data | placar | status |
|---|---|---|---|---|---|---|---|
| 1 | 1 | 1×2 | 1 | 1 | 2026-09-05 16:00 | 2×1 | ENCERRADA |
| 2 | 1 | 3×4 | 2 | 2 | 2026-09-05 18:30 | 0×0 | ENCERRADA |
| 3 | 1 | 5×6 | 3 | 3 | 2026-09-06 16:00 | 3×1 | ENCERRADA |
| 4 | 2 | 2×5 | 2 | 1 | 2026-09-12 16:00 | 1×1 | ENCERRADA |
| 5 | 2 | 4×1 | 2 | 2 | 2026-09-12 18:30 | 2×3 | ENCERRADA |
| 6 | 3 | 6×3 | 3 | 3 | 2026-11-14 16:00 | — | AGENDADA |
| 7 | 3 | 1×5 | 1 | 1 | 2026-11-15 16:00 | — | AGENDADA |
| 8 | 3 | 2×4 | 2 | 2 | 2026-11-15 18:30 | — | CANCELADA |

---

### Etapa 1 (Issue #1): Banco cria sem erros + roteiro de testes

Hoje `CLASSIFICACAO` não é criada (`ERROR 3820`: o CHECK usa `colocacao_equipe`, a coluna se chama `colocacao`) e, por consequência, nenhuma partida pode ser inserida.

**Files:** Rename `bd_campeonato.sql` → `Champs_schema.sql`, `procedures_triggers.sql` → `Champs_procedures_triggers.sql` (`git mv`); Create `sql/99_testes.sql`, `sql/testar.sh`, `docs/guia/01-banco-de-dados.md`; Modify `README.md`.

**Interfaces — Produces:** nome definitivo da coluna `CLASSIFICACAO.colocacao_equipe` (o mesmo que a procedure já usa); formato do `99_testes.sql` e `testar.sh` descritos acima.

- [ ] **Step 1:** `git mv` dos dois scripts e ajuste dos cabeçalhos (ordem de instalação `Champs_schema -> Champs_procedures_triggers -> Champs_relatorios`).
- [ ] **Step 2:** escrever `testar.sh` e `99_testes.sql` com as seções A, B e D:

```sql
-- B) TESTES DE CLASSIFICACAO (calculada pelos triggers ao inserir as partidas)
SELECT IF((SELECT CONCAT(colocacao_equipe,'|',pontuacao,'|',saldo_gols) FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 1) = '1|6|2', 'OK', 'FALHOU') AS 'Tigres 1o, 6 pontos, saldo 2';
SELECT IF((SELECT CONCAT(colocacao_equipe,'|',pontuacao) FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 5) = '2|4', 'OK', 'FALHOU') AS 'Tubaroes 2o, 4 pontos';
SELECT IF((SELECT CONCAT(colocacao_equipe,'|',pontuacao) FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 6) = '6|0', 'OK', 'FALHOU') AS 'Panteras 6o, 0 pontos';

-- D) ERROS ESPERADOS
-- ESPERADO ERRO: Cannot delete or update a parent row
DELETE FROM CAMPEONATO WHERE id_campeonato = 1;
```
- [ ] **Step 3:** `bash sql/testar.sh` → falha com `ERROR 3820 ... 'colocacao_equipe'` em `Champs_schema.sql`.
- [ ] **Step 4:** corrigir: coluna `colocacao_equipe INT NULL,` e `CHECK (colocacao_equipe IS NULL OR colocacao_equipe > 0)`.
- [ ] **Step 5:** `bash sql/testar.sh` → `TODOS OS TESTES PASSARAM`.
- [ ] **Step 6:** README — seção “Banco de dados”: instalar no Workbench (Champs_schema → Champs_procedures_triggers → Champs_relatorios), testar com `bash sql/testar.sh` (Docker) **ou** abrindo o `99_testes.sql` no Workbench e conferindo os `OK`.
- [ ] **Step 7:** guia — criar `docs/guia/01-banco-de-dados.md` com a seção **“1. Estrutura do banco”**: diagrama das 7 tabelas e relacionamentos (texto/ASCII), o papel de cada tabela, PK, FK (`ON DELETE RESTRICT` × `CASCADE`, com o exemplo do erro 1451), CHECK, UNIQUE, colunas geradas (`saldo_gols`, `pontuacao`), e “o que estava quebrado e por quê”.
- [ ] **Step 8:** entregar para revisão (lista de arquivos alterados).

---

### Etapa 2 (Issue #2): Regras da partida e da inscrição

Hoje o banco aceita: equipe jogando 2× na mesma rodada, árbitro/estádio em dois jogos no mesmo horário, partida fora do período do campeonato, equipe não inscrita jogando (e a procedure a inscreve sozinha), `tempo_partida` em jogo encerrado e desinscrição de equipe que já jogou.

**Files:** Modify `sql/Champs_schema.sql`, `sql/Champs_procedures_triggers.sql`, `sql/99_testes.sql`, `docs/guia/01-banco-de-dados.md`.

**Interfaces — Produces:**
- `CONSTRAINT chk_partida_tempo CHECK (tempo_partida IS NULL OR status = 'EM_ANDAMENTO')` em `PARTIDA`.
- `PROCEDURE sp_validar_partida(IN p_id_partida INT, IN p_id_campeonato INT, IN p_rodada INT, IN p_id_mandante INT, IN p_id_visitante INT, IN p_id_estadio INT, IN p_id_arbitro INT, IN p_data DATETIME, IN p_status VARCHAR(20))` — um `IF ... THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '...'; END IF;` por regra, nesta ordem. Partida `CANCELADA` não é validada. Nas regras 3 e 4, ignora a própria partida (`id_partida <> p_id_partida`; no INSERT os triggers passam `0`) e partidas `CANCELADA`.
  1. `Data da partida fora do periodo do campeonato` — `DATE(p_data) NOT BETWEEN data_inicio AND data_fim`
  2. `Equipe nao inscrita neste campeonato` — mandante ou visitante sem linha em `CLASSIFICACAO`
  3. `Equipe ja possui partida nesta rodada` — mandante ou visitante em outra partida do mesmo campeonato e rodada
  4. `Arbitro ou estadio ja ocupado neste horario` — outra partida com o mesmo árbitro ou o mesmo estádio e `data_partida = p_data`
- `TRIGGER trg_partida_before_insert` e `trg_partida_before_update` — uma linha cada: `CALL sp_validar_partida(...)` com os campos de `NEW` (`0` como id no insert).
- `sp_sincronizar_classificacao` **sem** o passo 1 (`INSERT IGNORE ... UNION`) — só recalcula quem está inscrito.
- `TRIGGER trg_classificacao_before_delete` — `Equipe possui partidas neste campeonato` se existir partida do campeonato com a equipe.

- [ ] **Step 1:** acrescentar ao `99_testes.sql`:

```sql
-- D) ERROS ESPERADOS
-- ESPERADO ERRO: Equipe ja possui partida nesta rodada
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 1, 1, 3, 3, 3, '2026-09-07 16:00');          -- Tigres ja jogou na rodada 1
-- ESPERADO ERRO: Arbitro ou estadio ja ocupado neste horario
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 4, 3, 6, 3, 1, '2026-09-05 16:00');          -- arbitro 1 apita a partida 1 nesse horario
-- ESPERADO ERRO: Data da partida fora do periodo do campeonato
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 4, 3, 6, 3, 3, '2030-01-01 16:00');
-- ESPERADO ERRO: Equipe nao inscrita neste campeonato
INSERT INTO EQUIPE (nome, cidade) VALUES ('Sem Inscricao FC', 'Natal');   -- vira a equipe 7
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 4, 7, 1, 3, 3, '2026-10-01 16:00');
-- ESPERADO ERRO: chk_partida_tempo
UPDATE PARTIDA SET tempo_partida = 'PRIMEIRO_TEMPO' WHERE id_partida = 1;
-- ESPERADO ERRO: Equipe possui partidas neste campeonato
DELETE FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 1;

-- E) ALTERACOES VALIDAS
UPDATE PARTIDA SET status = 'ENCERRADA', gols_mandante = 2, gols_visitante = 0 WHERE id_partida = 6;
SELECT IF((SELECT pontuacao FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 6) = 3,
          'OK', 'FALHOU') AS 'Placar lancado: Panteras soma 3 pontos';
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 10, 1, 2, 1, 1, '2026-12-20 20:00');          -- ultimo dia do campeonato
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 3, 4, 2, 2, 2, '2026-11-15 18:30');           -- mesmo horario da partida 8, que foi cancelada
CALL sp_inscrever_equipe(1, 7);
SELECT IF((SELECT colocacao_equipe IS NULL AND pontuacao = 0 FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 7), 'OK', 'FALHOU') AS 'Equipe inscrita sem jogos';
DELETE FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 7;   -- sem partidas: pode sair
```
- [ ] **Step 2:** `bash sql/testar.sh` → falha: `Erro esperado nao aconteceu: Equipe ja possui partida nesta rodada`.
- [ ] **Step 3:** implementar o CHECK, a procedure, os 3 triggers e remover o passo 1 da sincronização.
- [ ] **Step 4:** `bash sql/testar.sh` → `TODOS OS TESTES PASSARAM`.
- [ ] **Step 5:** guia — seção **“2. Regras automáticas”**: o que é procedure e trigger, `BEFORE` × `AFTER`, o fluxo “salvou partida → trigger valida → grava → trigger recalcula classificação”, as 4 regras com um exemplo cada, `SIGNAL` e como a mensagem chega ao Java, e a procedure de classificação explicada passo a passo (zera, soma, ordena com `ROW_NUMBER`).
- [ ] **Step 6:** entregar para revisão.

---

### Etapa 3 (Issue #3): Relatório principal com INNER JOIN

**Files:** Create `sql/Champs_relatorios.sql`; Modify `sql/99_testes.sql`, `docs/guia/01-banco-de-dados.md`.

**Interfaces — Produces** (usado pelo Java na Fase 5):
- `PROCEDURE sp_relatorio_partidas_periodo(IN p_id_campeonato INT, IN p_data_inicio DATE, IN p_data_fim DATE)` — `PARTIDA` com `INNER JOIN` em `EQUIPE` (duas vezes: `m` mandante, `v` visitante), `ESTADIO` e `ARBITRO`. Colunas, nesta ordem: `rodada`, `data_hora` (`DATE_FORMAT(data_partida, '%d/%m/%Y %H:%i')`), `mandante`, `visitante`, `estadio`, `arbitro`, `placar` (`CONCAT(COALESCE(gols_mandante,'-'), ' x ', COALESCE(gols_visitante,'-'))`), `status`. Filtro: `id_campeonato = p_id_campeonato AND DATE(data_partida) BETWEEN p_data_inicio AND p_data_fim`. `ORDER BY data_partida`.
- Só esse relatório no banco. A tabela de classificação será um `SELECT ... INNER JOIN` no próprio Java (`ClassificacaoDAO`), para o grupo mostrar JOIN também no código.

- [ ] **Step 1:** acrescentar a seção C ao `99_testes.sql`:

```sql
-- C) TESTE DO RELATORIO
-- ESPERADO: 5 linhas; a 1a: 1 | 05/09/2026 16:00 | Tigres FC | Aguas EC | Arena Central | Carlos Andrade | 2 x 1 | ENCERRADA
CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');
-- ESPERADO: 2 linhas (os dois jogos de 05/09) - a data final entra no periodo
CALL sp_relatorio_partidas_periodo(1, '2026-09-05', '2026-09-05');
```
- [ ] **Step 2:** `bash sql/testar.sh` → falha: `PROCEDURE campeonato.sp_relatorio_partidas_periodo does not exist`.
- [ ] **Step 3:** escrever `Champs_relatorios.sql`.
- [ ] **Step 4:** `bash sql/testar.sh` → `TODOS OS TESTES PASSARAM`, e conferir na saída as duas tabelas do relatório contra os comentários `ESPERADO`.
- [ ] **Step 5:** guia — seção **“3. Relatório e JOIN”**: o que é `INNER JOIN`, por que `EQUIPE` aparece duas vezes com apelidos diferentes, antes/depois (códigos × nomes, como no enunciado), por que procedure (recebe parâmetros) e como o Java vai chamar (`CallableStatement` com `{call sp_relatorio_partidas_periodo(?, ?, ?)}`).
- [ ] **Step 6:** entregar para revisão.

---

### Etapa 4 (Issue #4): Guias de estudo e de apresentação

O banco está pronto; agora o material para o grupo aprender e responder ao professor. Sem código novo.

**Files:** Modify `docs/guia/01-banco-de-dados.md`; Create `docs/guia/00-guia-de-apresentacao.md`.

- [ ] **Step 1:** fechar `01-banco-de-dados.md` com **“4. Como abrir e explicar cada arquivo”** (roteiro de leitura do `01`, `02`, `04` em ordem, bloco a bloco) e **“5. Perguntas e respostas do banco”** (mínimo 20, respostas de 2–4 linhas, citando o arquivo e o nome do objeto: PK, FK, chave composta, N:N, CHECK, UNIQUE, coluna gerada, trigger, procedure, `SIGNAL`, JOIN, `ON DELETE`, por que o banco valida, etc.).
- [ ] **Step 2:** criar `00-guia-de-apresentacao.md` (escopo global), com:
  - **O sistema em 1 minuto:** problema, rotina principal, quem usa.
  - **Requisitos do enunciado → onde estão no projeto** (tabela: 4 CRUDs, CRUD principal, JOIN, relatório, POO, MVC, JFrame, JDBC, MySQL, PK/FK, dados de demonstração). Itens das fases Java marcados `(a preencher na Fase N)`.
  - **Arquitetura MVC do projeto:** desenho das camadas e o caminho de um clique (Tela → Controller → DAO → banco → volta).
  - **Roteiro da demonstração:** Cadastro → Inscrição → Partida → JOIN → Relatório.
  - **Banco de perguntas global** por tema da seção 11 do enunciado (código, POO, classes e objetos, MVC, JFrame/Swing, JDBC, MySQL, CRUDs, PK/FK, relacionamentos, JOIN, CRUD principal, relatório, funcionamento geral). Temas do banco respondidos; temas conceituais (POO, MVC, JDBC, Swing) respondidos de forma geral agora e ligados ao código nas fases seguintes.
- [ ] **Step 3:** conferir que todo nome citado nos guias (arquivo, tabela, procedure, trigger, mensagem) existe com a mesma grafia nos scripts: `grep` de cada nome em `sql/`.
- [ ] **Step 4:** entregar para revisão.
