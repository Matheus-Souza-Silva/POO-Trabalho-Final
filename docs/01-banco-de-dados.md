# Guia do Banco de Dados `campeonato`

> **Objetivo:** explicar o banco **linha a linha**, para que qualquer integrante consiga abrir os scripts, dizer o que cada tabela, constraint, trigger e procedure faz, e responder perguntas do professor sobre qualquer detalhe.
>
> Guias relacionados: [visão geral](00-guia-de-apresentacao.md) · [Java e relatórios](02-java-e-relatorios.md) · [mapa do código](03-mapa-do-codigo.md) · [simulado](04-perguntas-e-respostas.md).

**Índice:** [1. Visão geral](#1-visão-geral-e-ordem-de-execução) · [2. Modelo relacional](#2-o-modelo-relacional) · [3. Tabela por tabela](#3-tabela-por-tabela) · [4. Constraints e erros](#4-catálogo-de-constraints-e-erros) · [5. Colunas geradas e ENUM](#5-colunas-geradas-e-enum) · [6. Procedures e triggers](#6-procedures-e-triggers) · [7. Relatórios](#7-os-relatórios) · [8. Dados de demonstração](#8-dados-de-demonstração-e-teste-champs_dadossql) · [9. testar.sh e Champs_dados](#9-testarsh-e-o-champs_dadossql) · [10. Testes manuais](#10-como-testar-o-banco-manualmente) · [11. Perguntas](#11-perguntas-e-respostas-banco) · [12. Erros comuns](#12-erros-comuns-de-quem-está-estudando) · [13. Checklist](#13-checklist-do-banco)

---

## 1. Visão geral e ordem de execução

### 1.1 Requisitos

- **MySQL 8.0.16 ou superior.** O projeto usa recursos que só existem no MySQL 8:
  - `CHECK` realmente aplicado (a partir de 8.0.16);
  - colunas geradas (`GENERATED ALWAYS AS ... STORED`);
  - funções de janela (`ROW_NUMBER() OVER (...)`).
- Codificação **`utf8mb4`** e collation **`utf8mb4_unicode_ci`**, para aceitar acentos e nomes em português.
- Engine **InnoDB** em todas as tabelas, porque ela suporta chaves estrangeiras e transações.

### 1.2 Os arquivos e a ordem

Execute **nesta ordem**, num banco MySQL limpo:

```text
1. SQL_Champs/Champs_schema.sql              → cria o database e as 7 tabelas
2. SQL_Champs/Champs_procedures_triggers.sql → cria procedures e triggers
3. SQL_Champs/Champs_relatorios.sql          → cria as procedures dos relatórios
4. SQL_Champs/Champs_dados.sql               → dados de demonstração/teste
```

| Arquivo | O que contém | Depende de |
|---|---|---|
| `Champs_schema.sql` | `DROP DATABASE`, `CREATE DATABASE`, `USE`, 7 `CREATE TABLE`, PKs, FKs, CHECKs, UNIQUEs, índices | nada |
| `Champs_procedures_triggers.sql` | `sp_sincronizar_classificacao`, `sp_inscrever_equipe`, `sp_validar_partida`, 6 triggers | schema |
| `Champs_relatorios.sql` | `sp_relatorio_classificacao`, `sp_relatorio_partidas_periodo` | schema + procedures (para os dados) |
| `Champs_dados.sql` | dados de demonstração/teste + duas chamadas de conferência | os três primeiros |

> **Atenção:** `Champs_schema.sql` começa com `DROP DATABASE IF EXISTS campeonato;`. Isso **apaga** o banco inteiro. Nunca rode em um banco com dados que você quer manter.

### 1.3 O cabeçalho do schema

```sql
DROP DATABASE IF EXISTS campeonato;

CREATE DATABASE campeonato
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campeonato;
```

- **`DROP DATABASE IF EXISTS`**: remove o banco se ele existir; se não existir, não dá erro.
- **`CREATE DATABASE`**: cria do zero.
- **`DEFAULT CHARACTER SET` / `COLLATE`**: definem a codificação e a comparação de textos.
  - `utf8mb4` aceita inclusive emojis e caracteres de 4 bytes.
  - `utf8mb4_unicode_ci` é **case-insensitive** (`ci` = case insensitive): `LIKE '%tigres%'` encontra `Tigres`, e `UNIQUE` considera `Tigres` e `tigres` iguais.
- **`USE campeonato`**: passa a apontar para o banco criado. Todos os scripts seguintes repetem esse `USE`.

### 1.4 O `DELIMITER`

Nos arquivos de procedures/triggers aparece:

```sql
DELIMITER $$

CREATE PROCEDURE ...
BEGIN
    ...
END$$

DELIMITER ;
```

- **`DELIMITER` é comando do cliente** (Workbench, CLI, `mysql`), **não do MySQL**. Ele muda o caractere que o cliente usa para saber onde termina um comando.
- Sem isso, o cliente cortaria a procedure no primeiro `;` interno.
- `$$` é só uma escolha; poderia ser `//` ou `;;`.
- No final, `DELIMITER ;` volta ao normal.

> **Pergunta provável:** “por que `DELIMITER` não aparece no código Java?” Porque ele é do cliente que executa o script; o servidor MySQL não o recebe.

---

## 2. O modelo relacional

### 2.1 As sete tabelas

| Tabela | Para que serve | Tipo no trabalho | Chave primária |
|---|---|---|---|
| `CAMPEONATO` | nome, período e situação do campeonato | CRUD simples | `id_campeonato` |
| `EQUIPE` | nome e cidade do time | CRUD simples | `id_equipe` |
| `ESTADIO` | nome, cidade e capacidade | CRUD simples | `id_estadio` |
| `ARBITRO` | nome, registro na federação e categoria | CRUD simples | `id_arbitro` |
| `JOGADOR` | jogadores de cada equipe | CRUD extra | `id_jogador` |
| `CLASSIFICACAO` | inscrição de uma equipe num campeonato + números | apoio à rotina principal | `(id_campeonato, id_equipe)` |
| `PARTIDA` | o jogo: quem, onde, quando, quem apita, placar e status | **CRUD principal** | `id_partida` |

### 2.2 Diagrama

```text
                    CAMPEONATO
                   1 │     1
                     │      └──────────────────────┐
                     │ N                           │ N
              CLASSIFICACAO                       PARTIDA
              (N:N)      N                        │  │  │  │
                     ┌───┘                        │  │  │  └── N:1 ── ARBITRO
                     │ 1                          │  │  └───── N:1 ── ESTADIO
                   EQUIPE ─────── 1:N ─────── JOGADOR│  └──────── N:1 ── EQUIPE (visitante)
                     │ 1                           │
                     └──────────── N:1 ────────────┘ EQUIPE (mandante)
```

Lendo o desenho:

- **Campeonato 1:N Partida**: um campeonato tem várias partidas; cada partida pertence a um campeonato.
- **Campeonato N:N Equipe**: um campeonato tem várias equipes e uma equipe pode disputar vários campeonatos. A tabela intermediária é `CLASSIFICACAO`, que também guarda os números.
- **Equipe 1:N Jogador**: uma equipe tem vários jogadores.
- **Equipe 1:N Partida (mandante)** e **Equipe 1:N Partida (visitante)**: a mesma tabela `EQUIPE` é referenciada duas vezes por `PARTIDA`.
- **Estádio 1:N Partida** e **Árbitro 1:N Partida**: cada partida acontece em um estádio e é apitada por um árbitro.

### 2.3 Matriz de chaves estrangeiras

| Tabela | Coluna | Aponta para | `ON DELETE` | `ON UPDATE` |
|---|---|---|---|---|
| `JOGADOR` | `id_equipe` | `EQUIPE.id_equipe` | `RESTRICT` | `CASCADE` |
| `CLASSIFICACAO` | `id_campeonato` | `CAMPEONATO.id_campeonato` | `CASCADE` | `CASCADE` |
| `CLASSIFICACAO` | `id_equipe` | `EQUIPE.id_equipe` | `RESTRICT` | `CASCADE` |
| `PARTIDA` | `id_campeonato` | `CAMPEONATO.id_campeonato` | `RESTRICT` | `CASCADE` |
| `PARTIDA` | `id_mandante` | `EQUIPE.id_equipe` | `RESTRICT` | `RESTRICT` |
| `PARTIDA` | `id_visitante` | `EQUIPE.id_equipe` | `RESTRICT` | `RESTRICT` |
| `PARTIDA` | `id_estadio` | `ESTADIO.id_estadio` | `RESTRICT` | `CASCADE` |
| `PARTIDA` | `id_arbitro` | `ARBITRO.id_arbitro` | `RESTRICT` | `CASCADE` |

### 2.4 O que cada opção significa na prática

- **`ON DELETE RESTRICT`**: não deixa apagar o pai se existir filho. Ex.: não dá para apagar uma equipe que tem jogador, inscrição ou partida. O MySQL responde com o **erro 1451** (`Cannot delete or update a parent row`).
- **`ON DELETE CASCADE`**: apaga os filhos junto. Só é usado em `CLASSIFICACAO.id_campeonato`: se um campeonato for apagado (e não tiver partidas), as inscrições dele somem.
- **`ON UPDATE CASCADE`**: se o ID do pai mudar, o filho acompanha. Na prática os IDs não mudam, mas a opção está lá.
- **`ON UPDATE RESTRICT`** em `id_mandante`/`id_visitante`: essas colunas participam do `CHECK chk_partida_times`; o projeto adotou `RESTRICT` para evitar conflito com a regra. O comentário no schema registra essa escolha.

### 2.5 Ordem de exclusão (quem depende de quem)

Para apagar um registro sem violar FKs, a ordem é a inversa das dependências:

```text
PARTIDA
   ↓
CLASSIFICACAO (inscrição)
   ↓
JOGADOR
   ↓
EQUIPE / ESTADIO / ARBITRO / CAMPEONATO
```

Exemplos:

- Para apagar uma **equipe**: primeiro apagar os jogadores, depois as inscrições (se não houver partidas) e só então a equipe.
- Para apagar um **campeonato**: se houver partidas, a FK `fk_partida_campeonato` bloqueia; sem partidas, o `CASCADE` apaga as inscrições.
- Para apagar um **estádio** ou **árbitro**: se já apitou/recebeu partida, a FK bloqueia.

---

## 3. Tabela por tabela

### 3.1 `CAMPEONATO`

```sql
CREATE TABLE CAMPEONATO (
    id_campeonato INT PRIMARY KEY AUTO_INCREMENT,
    nome          VARCHAR(100) NOT NULL,
    data_inicio   DATE NOT NULL,
    data_fim      DATE NOT NULL,
    status        ENUM('PLANEJADO','EM_ANDAMENTO','ENCERRADO','CANCELADO')
                  NOT NULL DEFAULT 'PLANEJADO',

    CONSTRAINT uq_campeonato_nome UNIQUE (nome),
    CONSTRAINT chk_campeonato_datas CHECK (data_fim >= data_inicio)
) ENGINE=InnoDB;
```

| Coluna | Tipo | Nulo? | Default | Significado |
|---|---|---|---|---|
| `id_campeonato` | `INT` | não | `AUTO_INCREMENT` | PK; o MySQL gera 1, 2, 3… |
| `nome` | `VARCHAR(100)` | não | — | nome do campeonato; único |
| `data_inicio` | `DATE` | não | — | primeiro dia permitido para partidas |
| `data_fim` | `DATE` | não | — | último dia permitido para partidas |
| `status` | `ENUM(...)` | não | `PLANEJADO` | situação do campeonato |

**Regras:**

- `uq_campeonato_nome`: não pode haver dois campeonatos com o mesmo nome (a collation é case-insensitive, então `Copa` e `copa` colidem).
- `chk_campeonato_datas`: `data_fim >= data_inicio`. O MySQL recusa a linha com erro de CHECK (código 3819).
- `status` só aceita os quatro valores do `ENUM`.

**No Java:** `Campeonato` guarda as datas como `String` (`YYYY-MM-DD`), porque a tela trabalha com texto e o DAO envia a string ao MySQL. O banco converte para `DATE`.

**Na tela:** aba 1; validação exige nome, datas válidas e `data_fim >= data_inicio`.

**Exemplos:**

```sql
-- aceito
INSERT INTO CAMPEONATO (nome, data_inicio, data_fim, status)
VALUES ('Copa Regional 2026', '2026-08-01', '2026-12-20', 'EM_ANDAMENTO');

-- recusado: data final antes da inicial (chk_campeonato_datas)
INSERT INTO CAMPEONATO (nome, data_inicio, data_fim)
VALUES ('Copa Impossível', '2026-12-01', '2026-01-01');

-- recusado: nome repetido (uq_campeonato_nome)
INSERT INTO CAMPEONATO (nome, data_inicio, data_fim)
VALUES ('Copa Regional 2026', '2027-01-01', '2027-02-01');
```

### 3.2 `EQUIPE`

```sql
CREATE TABLE EQUIPE (
    id_equipe INT PRIMARY KEY AUTO_INCREMENT,
    nome      VARCHAR(100) NOT NULL,
    cidade    VARCHAR(100) NOT NULL,

    CONSTRAINT uq_equipe_nome UNIQUE (nome)
) ENGINE=InnoDB;
```

| Coluna | Tipo | Nulo? | Significado |
|---|---|---|---|
| `id_equipe` | `INT` | não | PK auto-incremental |
| `nome` | `VARCHAR(100)` | não | nome do time; único |
| `cidade` | `VARCHAR(100)` | não | cidade do time |

- A equipe é referenciada por `JOGADOR`, `CLASSIFICACAO` e duas vezes por `PARTIDA`.
- `ON DELETE RESTRICT` em todas essas FKs: uma equipe com histórico não pode ser apagada.
- **Atenção:** a equipe pode ter uma inscrição em `CLASSIFICACAO` mesmo sem partidas; nesse caso a FK `fk_classificacao_equipe` também bloqueia a exclusão. É preciso remover a inscrição antes.

**No Java:** `Equipe` tem `idEquipe`, `nome`, `cidade`; `toString()` devolve `"1 - Tigres FC (Sao Paulo)"`, usado nos combos.

### 3.3 `ESTADIO`

```sql
CREATE TABLE ESTADIO (
    id_estadio INT PRIMARY KEY AUTO_INCREMENT,
    nome       VARCHAR(100) NOT NULL,
    cidade     VARCHAR(100) NOT NULL,
    capacidade INT NOT NULL,

    CONSTRAINT uq_estadio_nome_cidade UNIQUE (nome, cidade),
    CONSTRAINT chk_estadio_capacidade CHECK (capacidade > 0)
) ENGINE=InnoDB;
```

| Coluna | Tipo | Nulo? | Significado |
|---|---|---|---|
| `id_estadio` | `INT` | não | PK |
| `nome` | `VARCHAR(100)` | não | nome do estádio |
| `cidade` | `VARCHAR(100)` | não | cidade |
| `capacidade` | `INT` | não | número de lugares; deve ser > 0 |

- O `UNIQUE` é **composto** `(nome, cidade)`: dois estádios com o mesmo nome em cidades diferentes são permitidos; o mesmo nome na mesma cidade não.
- `chk_estadio_capacidade`: recusa `0` e valores negativos.
- A FK em `PARTIDA` é `RESTRICT`: estádio que já recebeu jogo não pode ser apagado.

**No Java:** `Estadio` tem `capacidade` como `int`; a tela valida `inteiroPositivo`.

### 3.4 `ARBITRO`

```sql
CREATE TABLE ARBITRO (
    id_arbitro         INT PRIMARY KEY AUTO_INCREMENT,
    nome               VARCHAR(100) NOT NULL,
    registro_federacao VARCHAR(30)  NOT NULL,
    categoria          ENUM('LOCAL','ESTADUAL','NACIONAL','FIFA')
                       NOT NULL DEFAULT 'LOCAL',

    CONSTRAINT uq_arbitro_registro UNIQUE (registro_federacao)
) ENGINE=InnoDB;
```

| Coluna | Tipo | Nulo? | Default | Significado |
|---|---|---|---|---|
| `id_arbitro` | `INT` | não | auto | PK |
| `nome` | `VARCHAR(100)` | não | — | nome |
| `registro_federacao` | `VARCHAR(30)` | não | — | registro único na federação |
| `categoria` | `ENUM(...)` | não | `LOCAL` | categoria |

- `uq_arbitro_registro`: não pode haver dois árbitros com o mesmo registro.
- A categoria é `ENUM`; o Java usa um `JComboBox` com os mesmos quatro valores.
- A FK em `PARTIDA` é `RESTRICT`: árbitro que já apitou não pode ser apagado.

### 3.5 `JOGADOR` (CRUD extra)

```sql
CREATE TABLE JOGADOR (
    id_jogador      INT PRIMARY KEY AUTO_INCREMENT,
    nome            VARCHAR(100) NOT NULL,
    posicao         VARCHAR(30)  NOT NULL,
    numero_camisa   INT NOT NULL,
    data_nascimento DATE,
    id_equipe       INT NOT NULL,

    CONSTRAINT fk_jogador_equipe
        FOREIGN KEY (id_equipe) REFERENCES EQUIPE (id_equipe)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_jogador_camisa UNIQUE (id_equipe, numero_camisa),
    CONSTRAINT chk_jogador_camisa CHECK (numero_camisa BETWEEN 1 AND 99)
) ENGINE=InnoDB;
```

| Coluna | Tipo | Nulo? | Significado |
|---|---|---|---|
| `id_jogador` | `INT` | não | PK |
| `nome` | `VARCHAR(100)` | não | nome do jogador |
| `posicao` | `VARCHAR(30)` | não | posição (texto livre no banco) |
| `numero_camisa` | `INT` | não | 1 a 99 |
| `data_nascimento` | `DATE` | **sim** | opcional; pode ser `NULL` |
| `id_equipe` | `INT` | não | FK para `EQUIPE` |

**Regras:**

- `fk_jogador_equipe`: o jogador precisa de uma equipe existente.
- `uq_jogador_camisa`: **a mesma camisa não repete dentro da mesma equipe**. É um `UNIQUE` composto `(id_equipe, numero_camisa)`. O mesmo número pode existir em equipes diferentes.
- `chk_jogador_camisa`: camisa entre 1 e 99.
- `data_nascimento` é o único campo opcional da tabela; o DAO grava `NULL` quando a tela deixa vazio (`pst.setNull(4, Types.DATE)`).
- **Nuance importante:** `posicao` é `VARCHAR`, não `ENUM`. O Java restringe a seis valores no combo, mas o banco aceita qualquer texto. Se o professor perguntar “o banco garante a posição?”, a resposta é **não**; só a tela padroniza.

**No Java:** `Jogador` tem `nomeEquipe` como campo extra (não existe na tabela), preenchido pelo `INNER JOIN EQUIPE` do `ManterJogador`.

**Exemplos:**

```sql
-- aceito
INSERT INTO JOGADOR (nome, posicao, numero_camisa, data_nascimento, id_equipe)
VALUES ('Rafael Alves', 'GOLEIRO', 1, '1995-03-12', 1);

-- recusado: camisa repetida na mesma equipe (uq_jogador_camisa)
INSERT INTO JOGADOR (nome, posicao, numero_camisa, id_equipe)
VALUES ('Outro Goleiro', 'GOLEIRO', 1, 1);

-- aceito: mesma camisa em outra equipe
INSERT INTO JOGADOR (nome, posicao, numero_camisa, id_equipe)
VALUES ('Goleiro do Time 2', 'GOLEIRO', 1, 2);

-- recusado: camisa 0 (chk_jogador_camisa)
INSERT INTO JOGADOR (nome, posicao, numero_camisa, id_equipe)
VALUES ('Camisa Zero', 'ATACANTE', 0, 1);
```

### 3.6 `CLASSIFICACAO` (inscrição + números)

```sql
CREATE TABLE CLASSIFICACAO (
    id_campeonato    INT NOT NULL,
    id_equipe        INT NOT NULL,
    colocacao_equipe INT NULL,

    partidas_jogadas INT NOT NULL DEFAULT 0,
    vitorias         INT NOT NULL DEFAULT 0,
    empates          INT NOT NULL DEFAULT 0,
    derrotas         INT NOT NULL DEFAULT 0,
    gols_marcados    INT NOT NULL DEFAULT 0,
    gols_sofridos    INT NOT NULL DEFAULT 0,

    saldo_gols INT GENERATED ALWAYS AS (gols_marcados - gols_sofridos) STORED,
    pontuacao  INT GENERATED ALWAYS AS (vitorias * 3 + empates) STORED,

    status_atual ENUM('A_JOGAR','CLASSIFICADO','ELIMINADO')
                 NOT NULL DEFAULT 'A_JOGAR',

    PRIMARY KEY (id_campeonato, id_equipe),

    CONSTRAINT fk_classificacao_campeonato
        FOREIGN KEY (id_campeonato) REFERENCES CAMPEONATO (id_campeonato)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_classificacao_equipe
        FOREIGN KEY (id_equipe) REFERENCES EQUIPE (id_equipe)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_classificacao_soma
        CHECK (partidas_jogadas = vitorias + empates + derrotas),

    CONSTRAINT chk_classificacao_colocacao
        CHECK (colocacao_equipe IS NULL OR colocacao_equipe > 0)
) ENGINE=InnoDB;
```

| Coluna | Tipo | Nulo? | Default | Significado |
|---|---|---|---|---|
| `id_campeonato` | `INT` | não | — | parte da PK; FK para campeonato |
| `id_equipe` | `INT` | não | — | parte da PK; FK para equipe |
| `colocacao_equipe` | `INT` | **sim** | — | posição (1º, 2º…); `NULL` para quem não jogou |
| `partidas_jogadas` | `INT` | não | 0 | jogos encerrados |
| `vitorias` | `INT` | não | 0 | vitórias |
| `empates` | `INT` | não | 0 | empates |
| `derrotas` | `INT` | não | 0 | derrotas |
| `gols_marcados` | `INT` | não | 0 | gols pró |
| `gols_sofridos` | `INT` | não | 0 | gols contra |
| `saldo_gols` | `INT` | gerada | — | `gols_marcados - gols_sofridos` |
| `pontuacao` | `INT` | gerada | — | `vitorias * 3 + empates` |
| `status_atual` | `ENUM(...)` | não | `A_JOGAR` | situação na competição |

**Por que a PK é composta?**

Porque a tabela representa a **inscrição** de uma equipe em um campeonato. A combinação `(id_campeonato, id_equipe)` é única: a mesma equipe não pode ser inscrita duas vezes no mesmo campeonato, mas pode estar em vários campeonatos.

**Por que as colunas geradas?**

`saldo_gols` e `pontuacao` não são enviadas no `INSERT`/`UPDATE`. O MySQL calcula sozinho quando `gols_marcados`, `gols_sofridos`, `vitorias` ou `empates` mudam. Isso evita inconsistência: ninguém precisa “lembrar” de atualizar a pontuação.

**`chk_classificacao_soma`:** garante que `partidas_jogadas = vitorias + empates + derrotas`. A procedure de sincronização preenche os quatro valores de forma coerente; se alguém tentar gravar dados incoerentes direto no Workbench, o CHECK recusa.

**`chk_classificacao_colocacao`:** a colocação pode ser `NULL` (quem ainda não jogou) ou maior que zero (1, 2, 3…). Não pode ser `0` nem negativa.

**`status_atual`:** é **manual**. Pode ser `A_JOGAR` (padrão), `CLASSIFICADO` ou `ELIMINADO`. O banco **não** calcula esse campo; a aba 6 tem um botão para atualizá-lo. Não confunda com `colocacao_equipe`, que é automática.

**FKs:**

- `fk_classificacao_campeonato` é `CASCADE` no delete: apagar um campeonato (sem partidas) apaga as inscrições.
- `fk_classificacao_equipe` é `RESTRICT`: uma equipe inscrita não pode ser apagada enquanto a inscrição existir.

**No Java:** `Classificacao` tem os mesmos campos, mais `nomeEquipe` (extra, vindo do JOIN). `colocacaoEquipe` é `Integer` para representar `NULL`; `saldoGols` e `pontuacao` são lidos do banco, não calculados no Java.

### 3.7 `PARTIDA` (CRUD principal)

```sql
CREATE TABLE PARTIDA (
    id_partida     INT PRIMARY KEY AUTO_INCREMENT,
    id_campeonato  INT NOT NULL,
    rodada         INT NOT NULL,
    id_mandante    INT NOT NULL,
    id_visitante   INT NOT NULL,
    id_estadio     INT NOT NULL,
    id_arbitro     INT NOT NULL,

    data_partida   DATETIME NOT NULL,
    gols_mandante  INT NULL,
    gols_visitante INT NULL,
    acrescimo      INT NOT NULL DEFAULT 0,

    status ENUM('AGENDADA','EM_ANDAMENTO','ENCERRADA','CANCELADA')
           NOT NULL DEFAULT 'AGENDADA',

    tempo_partida ENUM('PRIMEIRO_TEMPO','SEGUNDO_TEMPO') NULL,

    CONSTRAINT fk_partida_campeonato
        FOREIGN KEY (id_campeonato) REFERENCES CAMPEONATO (id_campeonato)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_partida_mandante
        FOREIGN KEY (id_mandante) REFERENCES EQUIPE (id_equipe)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_partida_visitante
        FOREIGN KEY (id_visitante) REFERENCES EQUIPE (id_equipe)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_partida_estadio
        FOREIGN KEY (id_estadio) REFERENCES ESTADIO (id_estadio)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_partida_arbitro
        FOREIGN KEY (id_arbitro) REFERENCES ARBITRO (id_arbitro)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_partida_rodada      CHECK (rodada > 0),
    CONSTRAINT chk_partida_acrescimo   CHECK (acrescimo >= 0),
    CONSTRAINT chk_partida_times       CHECK (id_mandante <> id_visitante),
    CONSTRAINT chk_partida_gols        CHECK (
        (gols_mandante  IS NULL OR gols_mandante  >= 0) AND
        (gols_visitante IS NULL OR gols_visitante >= 0)
    ),
    CONSTRAINT chk_partida_placar_completo CHECK (
        (gols_mandante IS NULL AND gols_visitante IS NULL) OR
        (gols_mandante IS NOT NULL AND gols_visitante IS NOT NULL)
    ),
    CONSTRAINT chk_partida_placar_por_status CHECK (
        (status = 'ENCERRADA' AND gols_mandante IS NOT NULL AND gols_visitante IS NOT NULL)
        OR (status IN ('AGENDADA','CANCELADA') AND gols_mandante IS NULL AND gols_visitante IS NULL)
        OR (status = 'EM_ANDAMENTO')
    ),
    CONSTRAINT chk_partida_tempo CHECK (tempo_partida IS NULL OR status = 'EM_ANDAMENTO'),
    CONSTRAINT uq_partida_confronto UNIQUE (id_campeonato, rodada, id_mandante, id_visitante),

    INDEX idx_partida_data (data_partida),
    INDEX idx_partida_campeonato_data (id_campeonato, data_partida)
) ENGINE=InnoDB;
```

| Coluna | Tipo | Nulo? | Default | Significado |
|---|---|---|---|---|
| `id_partida` | `INT` | não | auto | PK |
| `id_campeonato` | `INT` | não | — | FK para campeonato |
| `rodada` | `INT` | não | — | rodada; > 0 |
| `id_mandante` | `INT` | não | — | FK para equipe que joga em casa |
| `id_visitante` | `INT` | não | — | FK para equipe visitante |
| `id_estadio` | `INT` | não | — | FK para estádio |
| `id_arbitro` | `INT` | não | — | FK para árbitro |
| `data_partida` | `DATETIME` | não | — | data e hora |
| `gols_mandante` | `INT` | **sim** | — | gols do mandante; `NULL` se não há placar |
| `gols_visitante` | `INT` | **sim** | — | gols do visitante; `NULL` se não há placar |
| `acrescimo` | `INT` | não | 0 | acréscimos (minutos); ≥ 0 |
| `status` | `ENUM(...)` | não | `AGENDADA` | situação do jogo |
| `tempo_partida` | `ENUM(...)` | **sim** | — | 1º/2º tempo; só com `EM_ANDAMENTO` |

**As cinco FKs:** campeonato, mandante, visitante, estádio e árbitro. É isso que torna a partida o **CRUD principal com relacionamentos**.

**Os CHECKs, um por um:**

- `chk_partida_rodada`: `rodada > 0`.
- `chk_partida_acrescimo`: `acrescimo >= 0`.
- `chk_partida_times`: `id_mandante <> id_visitante`; um time não joga contra ele mesmo.
- `chk_partida_gols`: cada gol, se não for `NULL`, é ≥ 0.
- `chk_partida_placar_completo`: ou os **dois** gols são `NULL`, ou os **dois** estão preenchidos. Não existe “placar pela metade”.
- `chk_partida_placar_por_status`:
  - `ENCERRADA`: os dois gols são obrigatórios;
  - `AGENDADA` ou `CANCELADA`: os dois gols são obrigatoriamente `NULL`;
  - `EM_ANDAMENTO`: qualquer combinação permitida por `chk_partida_placar_completo` (os dois `NULL` ou os dois preenchidos).
- `chk_partida_tempo`: `tempo_partida` só pode ser preenchido se `status = 'EM_ANDAMENTO'`; caso contrário, tem que ser `NULL`.
- `uq_partida_confronto`: o mesmo confronto (`campeonato`, `rodada`, `mandante`, `visitante`) não pode se repetir. **Nuance:** a troca de mando (`A x B` e `B x A`) é uma combinação diferente para o `UNIQUE`, mas a regra 3 da `sp_validar_partida` bloqueia porque as duas equipes já teriam jogo na rodada.

**Índices:**

- `idx_partida_data`: acelera buscas por data.
- `idx_partida_campeonato_data`: acelera relatórios por campeonato + período.
- As FKs também criam índices internos no InnoDB.

**No Java:** `Partida` tem os campos da tabela + cinco campos extras de exibição (`nomeCampeonato`, `nomeMandante`, `nomeVisitante`, `nomeEstadio`, `nomeArbitro`), preenchidos pelo JOIN. `golsMandante`/`golsVisitante` são `Integer` para representar `NULL`.

---

## 4. Catálogo de constraints e erros

### 4.1 Todas as constraints nomeadas

| Nome | Tabela | Tipo | O que garante |
|---|---|---|---|
| `uq_campeonato_nome` | `CAMPEONATO` | UNIQUE | nome de campeonato único |
| `chk_campeonato_datas` | `CAMPEONATO` | CHECK | `data_fim >= data_inicio` |
| `uq_equipe_nome` | `EQUIPE` | UNIQUE | nome de equipe único |
| `uq_estadio_nome_cidade` | `ESTADIO` | UNIQUE | nome + cidade únicos |
| `chk_estadio_capacidade` | `ESTADIO` | CHECK | capacidade > 0 |
| `fk_jogador_equipe` | `JOGADOR` | FK | equipe existente; delete restrito |
| `uq_jogador_camisa` | `JOGADOR` | UNIQUE | camisa única por equipe |
| `chk_jogador_camisa` | `JOGADOR` | CHECK | camisa 1..99 |
| `uq_arbitro_registro` | `ARBITRO` | UNIQUE | registro único |
| `fk_classificacao_campeonato` | `CLASSIFICACAO` | FK | campeonato existente; delete cascade |
| `fk_classificacao_equipe` | `CLASSIFICACAO` | FK | equipe existente; delete restrito |
| `chk_classificacao_soma` | `CLASSIFICACAO` | CHECK | jogos = V + E + D |
| `chk_classificacao_colocacao` | `CLASSIFICACAO` | CHECK | colocação nula ou > 0 |
| `fk_partida_campeonato` | `PARTIDA` | FK | campeonato existente; delete restrito |
| `fk_partida_mandante` | `PARTIDA` | FK | mandante existente; delete restrito |
| `fk_partida_visitante` | `PARTIDA` | FK | visitante existente; delete restrito |
| `fk_partida_estadio` | `PARTIDA` | FK | estádio existente; delete restrito |
| `fk_partida_arbitro` | `PARTIDA` | FK | árbitro existente; delete restrito |
| `chk_partida_rodada` | `PARTIDA` | CHECK | rodada > 0 |
| `chk_partida_acrescimo` | `PARTIDA` | CHECK | acréscimo ≥ 0 |
| `chk_partida_times` | `PARTIDA` | CHECK | mandante ≠ visitante |
| `chk_partida_gols` | `PARTIDA` | CHECK | gols nulos ou ≥ 0 |
| `chk_partida_placar_completo` | `PARTIDA` | CHECK | os dois gols ou nenhum |
| `chk_partida_placar_por_status` | `PARTIDA` | CHECK | placar coerente com o status |
| `chk_partida_tempo` | `PARTIDA` | CHECK | tempo só em andamento |
| `uq_partida_confronto` | `PARTIDA` | UNIQUE | confronto único por rodada |

### 4.2 Códigos de erro do MySQL que aparecem no projeto

| Código | Situação | Exemplo no projeto | Como o Java trata |
|---|---|---|---|
| **1062** | `Duplicate entry` | nome de equipe repetido, camisa repetida, registro repetido | `mostrarErro` traduz “Já existe um cadastro com esse valor…” (e a camisa tem tradução específica) |
| **1451** | `Cannot delete or update a parent row` | apagar equipe com jogador/inscrição/partida | traduz “Não é possível excluir ou alterar este registro…” |
| **1452** | `Cannot add or update a child row` | gravar jogador com equipe inexistente, partida com FK inválida | mensagem do banco chega ao `JOptionPane` |
| **3819** | `Check constraint ... is violated` | capacidade 0, data final antes da inicial, camisa 0 | mensagem do banco |
| **1264** | `Out of range value` | número maior que o tipo | geralmente a tela já impede |
| **1406** | `Data too long` | texto maior que `VARCHAR` | mensagem do banco |
| **1045** | `Access denied` | usuário/senha errados | mensagem do banco |
| **1049** | `Unknown database` | banco `campeonato` não criado | mensagem do banco |
| **08S01** | `Communications link failure` | MySQL desligado | `mostrarErro` traduz “Não foi possível conectar ao MySQL…” |

> O código exato pode variar com a versão do MySQL; o importante é saber **qual constraint** gerou o erro e **por quê**.

### 4.3 Onde cada validação também existe no Java

| Regra do banco | O Java também valida? | Onde |
|---|---|---|
| campeonato: nome, datas, `data_fim >= data_inicio` | sim | `criarPainelCampeonato` (lambda `validar`) |
| equipe: nome e cidade | sim | `criarPainelEquipe` |
| estádio: capacidade > 0 | sim | `criarPainelEstadio` (`inteiroPositivo`) |
| árbitro: nome e registro | sim | `criarPainelArbitro` |
| jogador: camisa 1..99, equipe, data não futura | sim | `criarPainelJogador` |
| jogador: camisa única por equipe | **não** (só o banco) | `uq_jogador_camisa` |
| classificação: jogos = V+E+D | **não** (só o banco/procedure) | `chk_classificacao_soma` |
| partida: rodada > 0, acréscimo ≥ 0 | sim | `criarPainelPartidas` |
| partida: mandante ≠ visitante | sim | `criarPainelPartidas` |
| partida: placar completo/por status | sim | `criarPainelPartidas` + listener do status |
| partida: data dentro do campeonato | **não** (só o banco) | `sp_validar_partida` regra 1 |
| partida: equipes inscritas | **não** (só o banco) | `sp_validar_partida` regra 2 |
| partida: equipe uma vez por rodada | **não** (só o banco) | `sp_validar_partida` regra 3 |
| partida: árbitro/estádio livres no horário | **não** (só o banco) | `sp_validar_partida` regra 4 |
| partida: confronto único por rodada | **não** (só o banco) | `uq_partida_confronto` |
| desinscrever equipe com partidas | **não** (só o banco) | `trg_classificacao_before_delete` |

---

## 5. Colunas geradas e `ENUM`

### 5.1 Colunas geradas

```sql
saldo_gols INT GENERATED ALWAYS AS (gols_marcados - gols_sofridos) STORED,
pontuacao  INT GENERATED ALWAYS AS (vitorias * 3 + empates) STORED,
```

- **`GENERATED ALWAYS AS (expressão)`**: o valor é calculado pelo MySQL; não se faz `INSERT` nem `UPDATE` nessas colunas.
- **`STORED`**: o valor é gravado fisicamente na tabela e recalculado quando as colunas de origem mudam. (Existe também `VIRTUAL`, que não ocupa espaço e é calculado na leitura; o projeto usa `STORED`.)
- **Por que usar?** Para que a pontuação e o saldo nunca fiquem inconsistentes com vitórias/empates/gols. A regra “vitória = 3, empate = 1” fica em um único lugar.
- **O que a procedure atualiza?** Apenas `vitorias`, `empates`, `derrotas`, `gols_marcados`, `gols_sofridos`; `pontuacao` e `saldo_gols` mudam sozinhos.

**Pergunta provável:** “se eu der `UPDATE CLASSIFICACAO SET pontuacao = 10`, funciona?”
**Resposta:** não; o MySQL recusa atribuição a coluna gerada. A pontuação é derivada.

### 5.2 `ENUM`

| Tabela | Coluna | Valores |
|---|---|---|
| `CAMPEONATO` | `status` | `PLANEJADO`, `EM_ANDAMENTO`, `ENCERRADO`, `CANCELADO` |
| `ARBITRO` | `categoria` | `LOCAL`, `ESTADUAL`, `NACIONAL`, `FIFA` |
| `CLASSIFICACAO` | `status_atual` | `A_JOGAR`, `CLASSIFICADO`, `ELIMINADO` |
| `PARTIDA` | `status` | `AGENDADA`, `EM_ANDAMENTO`, `ENCERRADA`, `CANCELADA` |
| `PARTIDA` | `tempo_partida` | `PRIMEIRO_TEMPO`, `SEGUNDO_TEMPO` (ou `NULL`) |

- `ENUM` é uma lista fixa: o MySQL recusa valor fora dela.
- No Java, os valores aparecem em `JComboBox` com **exatamente** as mesmas strings. Se a tela enviar `"Encerrada"` em vez de `"ENCERRADA"`, o MySQL recusa (dependendo do `sql_mode`, pode virar string vazia ou erro).
- **Cuidado:** `ENUM` não é uma tabela de domínio; para o trabalho é suficiente, mas não é a forma mais flexível.

### 5.3 `DEFAULT`

- `CAMPEONATO.status` nasce `PLANEJADO`.
- `ARBITRO.categoria` nasce `LOCAL`.
- `CLASSIFICACAO.status_atual` nasce `A_JOGAR`.
- `PARTIDA.status` nasce `AGENDADA`.
- `PARTIDA.acrescimo` nasce `0`.
- `CLASSIFICACAO` inteiros nascem `0`.

O Java, na maioria das vezes, envia o valor explicitamente; o `DEFAULT` é uma rede de segurança para INSERTs feitos direto no SQL.

---

## 6. Procedures e triggers

### 6.1 Conceitos em uma frase cada

- **Procedure**: bloco SQL nomeado, guardado no banco, que pode receber parâmetros e é chamado com `CALL nome(...)`. É como um método.
- **Trigger**: bloco que o banco executa **sozinho** quando ocorre `INSERT`, `UPDATE` ou `DELETE` em uma tabela. Ninguém chama; ele “dispara”.
  - **`BEFORE`**: roda antes de gravar; serve para validar e cancelar.
  - **`AFTER`**: roda depois de gravar; serve para atualizar outras tabelas.
  - **`NEW`**: a linha nova (como vai ficar).
  - **`OLD`**: a linha antiga (como estava).
- **`SIGNAL SQLSTATE '45000'`**: lança um erro definido pelo usuário. É o `throw` do MySQL. O `MESSAGE_TEXT` chega ao Java dentro da `SQLException`.
- **`DELIMITER`**: já explicado; é do cliente.

### 6.2 `sp_sincronizar_classificacao` — a classificação automática

```sql
CREATE PROCEDURE sp_sincronizar_classificacao(IN p_id_campeonato INT)
BEGIN
    -- 1) zera
    UPDATE CLASSIFICACAO
       SET partidas_jogadas = 0,
           vitorias = 0, empates = 0, derrotas = 0,
           gols_marcados = 0, gols_sofridos = 0,
           colocacao_equipe = NULL
     WHERE id_campeonato = p_id_campeonato;

    -- 2) soma partidas encerradas
    UPDATE CLASSIFICACAO c
      JOIN (
            SELECT id_equipe,
                   COUNT(*) AS jogos,
                   SUM(v) AS vitorias,
                   SUM(e) AS empates,
                   SUM(d) AS derrotas,
                   SUM(gp) AS gols_marcados,
                   SUM(gc) AS gols_sofridos
              FROM (
                    SELECT id_mandante AS id_equipe,
                           (gols_mandante > gols_visitante) AS v,
                           (gols_mandante = gols_visitante) AS e,
                           (gols_mandante < gols_visitante) AS d,
                           gols_mandante AS gp,
                           gols_visitante AS gc
                      FROM PARTIDA
                     WHERE id_campeonato = p_id_campeonato
                       AND status = 'ENCERRADA'
                    UNION ALL
                    SELECT id_visitante,
                           (gols_visitante > gols_mandante),
                           (gols_visitante = gols_mandante),
                           (gols_visitante < gols_mandante),
                           gols_visitante,
                           gols_mandante
                      FROM PARTIDA
                     WHERE id_campeonato = p_id_campeonato
                       AND status = 'ENCERRADA'
                   ) jogos
           GROUP BY id_equipe
           ) s ON s.id_equipe = c.id_equipe
     SET c.partidas_jogadas = s.jogos,
         c.vitorias = s.vitorias,
         c.empates = s.empates,
         c.derrotas = s.derrotas,
         c.gols_marcados = s.gols_marcados,
         c.gols_sofridos = s.gols_sofridos
   WHERE c.id_campeonato = p_id_campeonato;

    -- 3) colocação
    UPDATE CLASSIFICACAO c
      JOIN (
            SELECT id_equipe,
                   ROW_NUMBER() OVER (
                       ORDER BY pontuacao DESC,
                                vitorias DESC,
                                saldo_gols DESC,
                                gols_marcados DESC,
                                id_equipe
                   ) AS posicao
              FROM CLASSIFICACAO
             WHERE id_campeonato = p_id_campeonato
               AND partidas_jogadas > 0
           ) r ON r.id_equipe = c.id_equipe
     SET c.colocacao_equipe = r.posicao
   WHERE c.id_campeonato = p_id_campeonato;
END
```

#### Passo 1 — zerar

- Atualiza **todas** as equipes inscritas no campeonato, inclusive as que nunca jogaram.
- Zera jogos, V/E/D, gols e colocação.
- Por que zerar? Porque a estratégia é **recalcular do zero**. Se uma partida foi alterada ou excluída, não sobra soma antiga.
- O `CHECK chk_classificacao_soma` continua satisfeito: `0 = 0 + 0 + 0`.

#### Passo 2 — somar as partidas encerradas

A subconsulta interna transforma **cada partida em duas linhas**: uma para o mandante, outra para o visitante.

Exemplo: partida `Tigres 2 x 1 Águas`.

| Visão | id_equipe | v | e | d | gp | gc |
|---|---:|---:|---:|---:|---:|---:|
| mandante | Tigres | 1 | 0 | 0 | 2 | 1 |
| visitante | Águas | 0 | 0 | 1 | 1 | 2 |

- `UNION ALL` junta as duas visões (mantém duplicatas; é o que queremos).
- `GROUP BY id_equipe` soma por equipe.
- `COUNT(*)` = jogos.
- `SUM(v)` = vitórias. Truque: no MySQL, uma comparação booleana vale **1** quando verdadeira e **0** quando falsa. Então `SUM(gols_mandante > gols_visitante)` conta vitórias sem `CASE`.
- `SUM(e)` = empates, `SUM(d)` = derrotas.
- `SUM(gp)` = gols marcados; `SUM(gc)` = gols sofridos.
- O `UPDATE ... JOIN (subquery) ... SET` atualiza a `CLASSIFICACAO` com esses totais.
- Só partidas `ENCERRADA` entram. `AGENDADA`, `EM_ANDAMENTO` e `CANCELADA` não contam.
- Equipes sem partida encerrada não aparecem no `JOIN` e continuam zeradas.

#### Passo 3 — colocação

- `ROW_NUMBER() OVER (ORDER BY ...)` numera as equipes de 1 a N na ordem dos critérios.
- Critérios: **pontuação DESC → vitórias DESC → saldo DESC → gols marcados DESC → id_equipe** (o id é o desempate final para não haver empate absoluto).
- Só entram equipes com `partidas_jogadas > 0`; quem não jogou fica com `colocacao_equipe = NULL`.
- Não há `PARTITION BY` porque o `WHERE` já restringe a um campeonato. Se a procedure fizesse todos os campeonatos de uma vez, seria necessário `PARTITION BY id_campeonato`.
- `ROW_NUMBER` não deixa buracos: 1, 2, 3… mesmo se houver empate nos critérios (o id desempata).

#### Por que “recalcular do zero” é uma boa escolha?

- Insert, update e delete de partida são tratados pelo mesmo algoritmo.
- Não há risco de esquecer de subtrair um gol ou uma vitória.
- O custo é pequeno para o volume do trabalho (poucas partidas).
- A desvantagem é que, em campeonatos gigantes, recalcular tudo a cada partida seria caro; aí faria sentido uma atualização incremental.

### 6.3 `sp_inscrever_equipe`

```sql
CREATE PROCEDURE sp_inscrever_equipe(IN p_id_campeonato INT, IN p_id_equipe INT)
BEGIN
    INSERT IGNORE INTO CLASSIFICACAO (id_campeonato, id_equipe)
    VALUES (p_id_campeonato, p_id_equipe);

    CALL sp_sincronizar_classificacao(p_id_campeonato);
END
```

- `INSERT IGNORE`: se a equipe já estiver inscrita, o `UNIQUE`/PK seria violado; o `IGNORE` transforma o erro em aviso e não insere de novo.
- Depois chama a sincronização; a equipe nova entra com zeros e `colocacao_equipe = NULL`.
- **Nuance:** `INSERT IGNORE` transforma alguns erros em aviso (o caso previsto é a duplicata). No projeto, a tela só oferece campeonato/equipe existentes, então o caso normal é a duplicata.
- A procedure **não** inscreve ninguém automaticamente ao salvar partida. A regra 2 da validação exige que a equipe já esteja em `CLASSIFICACAO`.

### 6.4 `sp_validar_partida` — as quatro regras

```sql
CREATE PROCEDURE sp_validar_partida(
    IN p_id_partida INT,
    IN p_id_campeonato INT,
    IN p_rodada INT,
    IN p_id_mandante INT,
    IN p_id_visitante INT,
    IN p_id_estadio INT,
    IN p_id_arbitro INT,
    IN p_data DATETIME,
    IN p_status VARCHAR(20)
)
BEGIN
    IF p_status <> 'CANCELADA' THEN
        -- regra 1: data dentro do campeonato
        -- regra 2: equipes inscritas
        -- regra 3: equipe não joga duas vezes na rodada
        -- regra 4: árbitro/estádio livres no horário
    END IF;
END
```

#### Regra 1 — data dentro do período do campeonato

```sql
IF NOT EXISTS (SELECT 1 FROM CAMPEONATO
                WHERE id_campeonato = p_id_campeonato
                  AND DATE(p_data) BETWEEN data_inicio AND data_fim) THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Data da partida fora do periodo do campeonato';
END IF;
```

- `DATE(p_data)` descarta a hora: um jogo às 22:00 do último dia ainda está dentro.
- Se o campeonato não existir, o `NOT EXISTS` é verdadeiro e a mensagem é “data fora do período” (a validação roda antes da FK).
- `BETWEEN` é inclusivo nas duas pontas.

#### Regra 2 — mandante e visitante inscritos

```sql
IF NOT EXISTS (SELECT 1 FROM CLASSIFICACAO
                WHERE id_campeonato = p_id_campeonato AND id_equipe = p_id_mandante)
   OR NOT EXISTS (SELECT 1 FROM CLASSIFICACAO
                   WHERE id_campeonato = p_id_campeonato AND id_equipe = p_id_visitante) THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Equipe nao inscrita neste campeonato';
END IF;
```

- A inscrição é a linha em `CLASSIFICACAO`; não basta a equipe existir em `EQUIPE`.
- Se uma das duas não estiver inscrita, a partida é recusada.

#### Regra 3 — equipe não joga duas vezes na rodada

```sql
IF EXISTS (SELECT 1 FROM PARTIDA
            WHERE id_campeonato = p_id_campeonato
              AND rodada = p_rodada
              AND id_partida <> p_id_partida
              AND status <> 'CANCELADA'
              AND (id_mandante  IN (p_id_mandante, p_id_visitante)
                OR id_visitante IN (p_id_mandante, p_id_visitante))) THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Equipe ja possui partida nesta rodada';
END IF;
```

- Verifica se **qualquer uma das duas equipes** já aparece como mandante ou visitante em outro jogo da mesma rodada e campeonato.
- `id_partida <> p_id_partida` evita que a própria partida, ao ser alterada, conflite consigo mesma.
- Partidas `CANCELADA` não ocupam a rodada.
- `IN (p_id_mandante, p_id_visitante)` é uma forma compacta de testar as duas colunas contra os dois times.

#### Regra 4 — árbitro e estádio livres no horário

```sql
IF EXISTS (SELECT 1 FROM PARTIDA
            WHERE data_partida = p_data
              AND id_partida <> p_id_partida
              AND status <> 'CANCELADA'
              AND (id_arbitro = p_id_arbitro OR id_estadio = p_id_estadio)) THEN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Arbitro ou estadio ja ocupado neste horario';
END IF;
```

- Compara o **DATETIME exato**, não a data inteira: dois jogos no mesmo dia em horários diferentes são permitidos.
- Vale para **qualquer campeonato**: um árbitro não pode estar em dois lugares ao mesmo tempo, nem em campeonatos diferentes.
- `OR` significa: se o árbitro **ou** o estádio já estiver ocupado, recusa.
- Partidas `CANCELADA` não ocupam.

#### Por que a procedure começa com `IF p_status <> 'CANCELADA'`?

Porque uma partida cancelada **não ocupa** equipe, árbitro nem estádio; ela é histórico. As regras 1–4 não se aplicam. Mas atenção: FKs, CHECKs e o `UNIQUE uq_partida_confronto` **continuam valendo** para uma partida cancelada. Ou seja, uma cancelada ainda precisa de campeonato/equipes/estádio/árbitro existentes, times diferentes e não pode repetir exatamente o mesmo confronto na mesma rodada.

#### Ordem das regras

A ordem é 1 → 2 → 3 → 4. A primeira que falhar dispara o `SIGNAL` e as seguintes não são testadas. Isso explica por que uma partida com dois problemas mostra a mensagem do primeiro.

### 6.5 Os triggers da partida

| Trigger | Quando dispara | O que faz |
|---|---|---|
| `trg_partida_before_insert` | antes de inserir | chama `sp_validar_partida` com os valores de `NEW` |
| `trg_partida_before_update` | antes de alterar | idem, com `NEW` (e `p_id_partida` para não conflitar consigo mesma) |
| `trg_partida_after_insert` | depois de inserir | chama `sp_sincronizar_classificacao(NEW.id_campeonato)` |
| `trg_partida_after_update` | depois de alterar | sincroniza `OLD.id_campeonato`; se mudou de campeonato, sincroniza também `NEW.id_campeonato` |
| `trg_partida_after_delete` | depois de excluir | sincroniza `OLD.id_campeonato` |
| `trg_classificacao_before_delete` | antes de excluir uma inscrição | recusa se a equipe já tem partida no campeonato |

#### Por que `BEFORE` para validar e `AFTER` para sincronizar?

- A validação precisa acontecer **antes** da gravação para poder cancelá-la.
- A sincronização precisa acontecer **depois** da gravação para que a procedure já leia a partida nova/alterada/excluída na tabela.
- No `AFTER DELETE`, a partida já não existe; por isso a procedure usa `OLD.id_campeonato`.

#### O caso da alteração que muda de campeonato

```sql
CREATE TRIGGER trg_partida_after_update
AFTER UPDATE ON PARTIDA
FOR EACH ROW
BEGIN
    CALL sp_sincronizar_classificacao(OLD.id_campeonato);

    IF NEW.id_campeonato <> OLD.id_campeonato THEN
        CALL sp_sincronizar_classificacao(NEW.id_campeonato);
    END IF;
END
```

- A partida saiu do campeonato antigo: a classificação antiga precisa ser refeita sem ela.
- A partida entrou no novo: a classificação nova precisa ser refeita com ela.
- Se o campeonato não mudou, uma chamada basta.

### 6.6 `trg_classificacao_before_delete` — desinscrição

```sql
CREATE TRIGGER trg_classificacao_before_delete
BEFORE DELETE ON CLASSIFICACAO
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM PARTIDA
                WHERE id_campeonato = OLD.id_campeonato
                  AND (id_mandante = OLD.id_equipe OR id_visitante = OLD.id_equipe)) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Equipe possui partidas neste campeonato';
    END IF;
END
```

- Impede remover a inscrição de uma equipe que já tem **qualquer** partida no campeonato, inclusive `CANCELADA`.
- Por que? Se a inscrição sumisse, a partida ficaria com uma equipe “fora” da classificação e o relatório poderia ficar inconsistente.
- Para desinscrever, a equipe não pode ter nenhuma partida no campeonato; é preciso excluir as partidas antes.
- **Diferença importante:** a validação da partida ignora `CANCELADA`, mas este trigger **não** ignora. Uma partida cancelada ainda bloqueia a desinscrição.

### 6.7 Linha do tempo completa

#### INSERT de partida ativa

```text
INSERT
  → BEFORE INSERT
      → sp_validar_partida (regras 1-4)
          → se falhar: SIGNAL, INSERT cancelado, erro volta ao Java
  → MySQL grava
      → FKs, CHECKs, UNIQUE
  → AFTER INSERT
      → sp_sincronizar_classificacao(NEW.id_campeonato)
  → COMMIT (autocommit)
```

#### UPDATE de partida

```text
UPDATE
  → BEFORE UPDATE
      → sp_validar_partida (com NEW; exclui a própria partida)
  → MySQL grava
  → AFTER UPDATE
      → sincroniza OLD.id_campeonato
      → se mudou de campeonato, sincroniza NEW.id_campeonato
```

#### DELETE de partida

```text
DELETE
  → (não há BEFORE DELETE)
  → MySQL remove
  → AFTER DELETE
      → sincroniza OLD.id_campeonato
```

#### Inscrição

```text
CALL sp_inscrever_equipe
  → INSERT IGNORE em CLASSIFICACAO
  → sp_sincronizar_classificacao
```

#### Desinscrição

```text
DELETE FROM CLASSIFICACAO
  → BEFORE DELETE
      → se existe partida da equipe no campeonato: SIGNAL
      → senão: remove
```

### 6.8 Casos de borda que valem ouro na apresentação

1. **Partida cancelada fora do período**: a validação ignora, então pode ser cadastrada (desde que FKs/CHECKs/UNIQUE passem). Isso é intencional: uma partida cancelada é histórico, não ocupa nada.
2. **Partida cancelada e desinscrição**: o trigger de desinscrição **não** ignora canceladas; a equipe ainda não pode sair.
3. **Alterar de `ENCERRADA` para `CANCELADA`**: o `BEFORE` vê `NEW.status = CANCELADA` e pula as regras; o `AFTER` recalcula e os pontos somem.
4. **Alterar de `CANCELADA` para `ENCERRADA`**: o `BEFORE` valida normalmente; se a data estiver fora do campeonato, recusa.
5. **Mudar a partida de campeonato**: o `AFTER` recalcula os dois campeonatos.
6. **Excluir uma partida**: não há trigger BEFORE, então o DELETE acontece; o `AFTER` recalcula.
7. **Empate absoluto nos critérios**: o `id_equipe` é o último desempate; a colocação nunca empata.
8. **Equipe sem partida**: fica com `colocacao_equipe = NULL`, e o relatório mostra `-`.
9. **Partida `EM_ANDAMENTO` com placar**: não conta para a classificação; só `ENCERRADA` conta.
10. **Placar pela metade**: o `chk_partida_placar_completo` recusa antes de qualquer trigger `AFTER`.

---

## 7. Os relatórios

### 7.1 `sp_relatorio_classificacao` — o relatório principal

```sql
CREATE PROCEDURE sp_relatorio_classificacao(IN p_id_campeonato INT)
BEGIN
    SELECT c.nome AS campeonato,
           cl.colocacao_equipe AS posicao,
           e.nome AS equipe,
           e.cidade,
           cl.pontuacao AS pontos,
           cl.partidas_jogadas AS jogos,
           cl.vitorias,
           cl.empates,
           cl.derrotas,
           cl.gols_marcados AS gols_pro,
           cl.gols_sofridos AS gols_contra,
           cl.saldo_gols,
           IF(cl.partidas_jogadas = 0, 0,
              ROUND(cl.pontuacao * 100.0 / (cl.partidas_jogadas * 3), 1)) AS aproveitamento,
           (SELECT COUNT(*) FROM PARTIDA p
             WHERE p.id_campeonato = cl.id_campeonato
               AND p.id_mandante = cl.id_equipe
               AND p.status = 'ENCERRADA') AS jogos_casa,
           (SELECT COUNT(*) FROM PARTIDA p
             WHERE p.id_campeonato = cl.id_campeonato
               AND p.id_visitante = cl.id_equipe
               AND p.status = 'ENCERRADA') AS jogos_fora,
           (SELECT COUNT(DISTINCT es.id_estadio)
              FROM PARTIDA p
             INNER JOIN ESTADIO es ON es.id_estadio = p.id_estadio
             WHERE p.id_campeonato = cl.id_campeonato
               AND (p.id_mandante = cl.id_equipe OR p.id_visitante = cl.id_equipe)
               AND p.status = 'ENCERRADA') AS estadios,
           (SELECT COUNT(DISTINCT a.id_arbitro)
              FROM PARTIDA p
             INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro
             WHERE p.id_campeonato = cl.id_campeonato
               AND (p.id_mandante = cl.id_equipe OR p.id_visitante = cl.id_equipe)
               AND p.status = 'ENCERRADA') AS arbitros,
           cl.status_atual
      FROM CLASSIFICACAO cl
     INNER JOIN EQUIPE e ON e.id_equipe = cl.id_equipe
     INNER JOIN CAMPEONATO c ON c.id_campeonato = cl.id_campeonato
     WHERE cl.id_campeonato = p_id_campeonato
     ORDER BY (cl.colocacao_equipe IS NULL), cl.colocacao_equipe,
              cl.pontuacao DESC, cl.saldo_gols DESC, e.nome;
END
```

#### Por que é o relatório principal?

Porque parte da **`CLASSIFICACAO`**, que é a entidade que liga `CAMPEONATO` e `EQUIPE` e é alimentada pelas `PARTIDA`. Ele mostra, com nomes, todas as tabelas da rotina principal:

- `CLASSIFICACAO` → números da equipe no campeonato;
- `CAMPEONATO` → nome do campeonato;
- `EQUIPE` → nome e cidade;
- `PARTIDA` → jogos em casa/fora, estádios e árbitros;
- `ESTADIO` e `ARBITRO` → nomes/quantidades.

#### Coluna por coluna

| Coluna | De onde vem | Observação |
|---|---|---|
| `campeonato` | `CAMPEONATO.nome` | JOIN |
| `posicao` | `CLASSIFICACAO.colocacao_equipe` | pode ser `NULL` |
| `equipe` | `EQUIPE.nome` | JOIN |
| `cidade` | `EQUIPE.cidade` | — |
| `pontos` | `CLASSIFICACAO.pontuacao` | coluna gerada |
| `jogos` | `CLASSIFICACAO.partidas_jogadas` | só encerradas |
| `vitorias`, `empates`, `derrotas` | `CLASSIFICACAO` | calculadas pela procedure |
| `gols_pro` | `gols_marcados` | alias |
| `gols_contra` | `gols_sofridos` | alias |
| `saldo_gols` | `CLASSIFICACAO.saldo_gols` | coluna gerada |
| `aproveitamento` | cálculo | `pontos / (jogos * 3) * 100`, com `IF` para evitar divisão por zero |
| `jogos_casa` | subconsulta em `PARTIDA` | `COUNT(*)` como mandante e `ENCERRADA` |
| `jogos_fora` | subconsulta em `PARTIDA` | `COUNT(*)` como visitante e `ENCERRADA` |
| `estadios` | subconsulta + `ESTADIO` | `COUNT(DISTINCT es.id_estadio)` |
| `arbitros` | subconsulta + `ARBITRO` | `COUNT(DISTINCT a.id_arbitro)` |
| `status_atual` | `CLASSIFICACAO.status_atual` | manual |

#### Detalhes de SQL

- **`IF(condição, valor_se_verdadeiro, valor_se_falso)`**: evita divisão por zero quando a equipe ainda não jogou.
- **`ROUND(..., 1)`**: aproveitamento com uma casa decimal.
- **`COUNT(DISTINCT ...)`**: conta estádios/árbitros **diferentes**; se a equipe jogou duas vezes no mesmo estádio, conta 1.
- **`(p.id_mandante = cl.id_equipe OR p.id_visitante = cl.id_equipe)`**: a equipe pode ter jogado em casa ou fora.
- **`ORDER BY (cl.colocacao_equipe IS NULL)`**: a expressão vale 0 para quem tem colocação e 1 para quem não tem; como a ordem é ascendente, quem tem posição aparece primeiro. Depois vêm `colocacao`, `pontuacao DESC`, `saldo_gols DESC` e `e.nome`.
- **Por que não usar `LEFT JOIN`?** Porque toda partida tem equipe/estádio/árbitro (FK), então o `INNER JOIN` não perde linhas. As subconsultas com `COUNT` devolvem 0 quando não há partida, sem precisar de `LEFT JOIN`.

#### Exemplo de chamada

```sql
CALL sp_relatorio_classificacao(1);
```

### 7.2 `sp_relatorio_partidas_periodo`

```sql
CREATE PROCEDURE sp_relatorio_partidas_periodo(
    IN p_id_campeonato INT,
    IN p_data_inicio DATE,
    IN p_data_fim DATE
)
BEGIN
    SELECT p.rodada,
           DATE_FORMAT(p.data_partida, '%d/%m/%Y %H:%i') AS data_hora,
           m.nome AS mandante,
           v.nome AS visitante,
           e.nome AS estadio,
           a.nome AS arbitro,
           CONCAT(COALESCE(p.gols_mandante, '-'), ' x ',
                  COALESCE(p.gols_visitante, '-')) AS placar,
           p.status
      FROM PARTIDA p
     INNER JOIN EQUIPE  m ON m.id_equipe  = p.id_mandante
     INNER JOIN EQUIPE  v ON v.id_equipe  = p.id_visitante
     INNER JOIN ESTADIO e ON e.id_estadio = p.id_estadio
     INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro
     WHERE p.id_campeonato = p_id_campeonato
       AND DATE(p.data_partida) BETWEEN p_data_inicio AND p_data_fim
     ORDER BY p.data_partida;
END
```

- **Parâmetros**: campeonato, data inicial e data final. É por isso que é uma procedure: a tela passa os filtros.
- **`INNER JOIN EQUIPE m` e `INNER JOIN EQUIPE v`**: a mesma tabela duas vezes, com apelidos diferentes (mandante e visitante). Sem apelidos, o MySQL não saberia qual `nome` usar.
- **`DATE(p.data_partida)`**: descarta a hora; o último dia do período entra inteiro.
- **`DATE_FORMAT(..., '%d/%m/%Y %H:%i')`**: formato brasileiro `05/09/2026 16:00`.
- **`COALESCE(p.gols_mandante, '-')`**: troca `NULL` por `-`; jogos sem placar aparecem `- x -`.
- **`CONCAT`**: junta as partes do placar.
- **`ORDER BY p.data_partida`**: ordem cronológica.
- **`p.status`**: mostra `AGENDADA`, `EM_ANDAMENTO`, `ENCERRADA` ou `CANCELADA`; o relatório inclui todos os status do período.

### 7.3 Como o Java chama as procedures

```java
CallableStatement cs = con.prepareCall("{call sp_relatorio_classificacao(?)}");
cs.setInt(1, idCampeonato);
ResultSet rs = cs.executeQuery();
```

```java
cs = con.prepareCall("{call sp_relatorio_partidas_periodo(?, ?, ?)}");
cs.setInt(1, idCampeonato);
cs.setDate(2, java.sql.Date.valueOf(dataInicio));
cs.setDate(3, java.sql.Date.valueOf(dataFim));
rs = cs.executeQuery();
```

- **`{call ...}`**: sintaxe JDBC para procedure.
- **`CallableStatement`**: subtipo de `PreparedStatement` para procedures.
- **`executeQuery()`**: a procedure devolve um `ResultSet` (um `SELECT`), então lemos como qualquer consulta.
- O Java monta um objeto `RelatorioClassificacao` ou `RelatorioPartida` por linha.

### 7.4 Exemplo de saída (dados de demonstração)

Com `Champs_dados.sql`, a Copa Regional (campeonato 1) tem 11 partidas encerradas. A classificação calculada pelos triggers fica:

| Pos. | Equipe | PTS | J | V | E | D | GP | GC | SG |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | Tigres FC | 6 | 2 | 2 | 0 | 0 | 3 | 1 | 2 |
| 2 | Falcões United | 4 | 3 | 1 | 1 | 1 | 4 | 2 | 2 |
| 3 | Onças EC | 4 | 2 | 1 | 1 | 0 | 2 | 0 | 2 |
| 4 | Panteras AC | 4 | 2 | 1 | 1 | 0 | 4 | 3 | 1 |
| 5 | Águas EC | 4 | 2 | 1 | 1 | 0 | 3 | 2 | 1 |
| 6 | Tubarões SC | 4 | 2 | 1 | 1 | 0 | 3 | 2 | 1 |
| 7 | Gaivões do Sul | 1 | 2 | 0 | 1 | 1 | 1 | 2 | -1 |
| 8 | Titas FC | 1 | 2 | 0 | 1 | 1 | 1 | 2 | -1 |
| 9 | Leões do Norte | 1 | 3 | 0 | 1 | 2 | 2 | 5 | -3 |
| 10 | Corvos FC | 0 | 2 | 0 | 0 | 2 | 1 | 5 | -4 |

> Confira com `CALL sp_relatorio_classificacao(1);`. As posições 2–6 mostram o desempate: pontos, vitórias, saldo e gols marcados. As posições 7–8 mostram o desempate final por `id_equipe` (Gaivões id 9 antes de Titas id 10).

**Atenção:** se você rodar `Champs_dados.sql` depois de outro arquivo de dados, os IDs podem não ser os mesmos; o exemplo acima assume banco recém-criado.

---

## 8. Dados de demonstração e teste (`Champs_dados.sql`)

### 8.1 O que o arquivo faz

- `USE campeonato;`
- `START TRANSACTION;` … `COMMIT;`: os INSERTs são atômicos; se algo falhar, nada fica pela metade.
- Insere na ordem das dependências: campeonatos → equipes → estádios → árbitros → inscrições → jogadores → partidas.
- Os IDs são gerados pelo `AUTO_INCREMENT`; os comentários `-- id 1`, `-- id 2` mostram a ordem esperada em banco vazio.
- No final, faz duas chamadas de conferência:
  ```sql
  SELECT 'Dados de demonstração carregados' AS status;
  CALL sp_relatorio_classificacao(1);
  CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');
  ```

### 8.2 O que ele contém

| Entidade | Quantidade | Detalhes |
|---|---:|---|
| Campeonatos | 4 | um em cada status: `EM_ANDAMENTO`, `ENCERRADO`, `PLANEJADO`, `CANCELADO` |
| Equipes | 10 | nomes e cidades únicos |
| Estádios | 5 | capacidades variadas (18.000 a 45.000) |
| Árbitros | 5 | registros `FED-1001`…`FED-1005`, categorias variadas |
| Inscrições | 20 | 10 na Copa Regional, 6 na Liga, 4 na Copa dos Campeões |
| Jogadores | 21 | camisas únicas por equipe; duas datas de nascimento `NULL` |
| Partidas | 28 | 15 na Copa Regional, 9 na Liga, 4 na Copa dos Campeões |

### 8.3 As partidas

- **Copa Regional (campeonato 1):**
  - rodada 1: 5 jogos `ENCERRADA`;
  - rodada 2: 5 jogos `ENCERRADA`;
  - rodada 3: 2 `AGENDADA`, 1 `CANCELADA`, 1 `EM_ANDAMENTO`, 1 `ENCERRADA`.
- **Liga Metropolitana (campeonato 2):** 9 jogos `ENCERRADA` em abril/maio/junho.
- **Copa dos Campeões (campeonato 3):** 4 jogos `AGENDADA` em outubro/novembro.
- **Torneio de Verão (campeonato 4):** sem partidas; status `CANCELADO`.

### 8.4 Por que os triggers não recusam esses dados?

As partidas de demonstração foram escolhidas para respeitar as regras:

- datas dentro do período de cada campeonato;
- equipes inscritas;
- nenhuma equipe duas vezes na mesma rodada;
- árbitro/estádio sem choque de horário;
- partidas canceladas sem placar;
- partidas encerradas com placar completo;
- `EM_ANDAMENTO` com `PRIMEIRO_TEMPO`.

Se você alterar manualmente uma data ou equipe, o trigger pode recusar. Isso é útil para demonstrar as regras.

### 8.5 Como usar na apresentação

1. Banco limpo + três scripts + `Champs_dados.sql`.
2. Abra a aplicação; as abas já vêm populadas.
3. Aba 6, escolha “Copa Regional 2026”; mostre a classificação calculada.
4. Aba 8, gere o relatório de classificação e o de partidas por período.
5. Aba 7, mostre uma partida `ENCERRADA`, uma `AGENDADA`, uma `CANCELADA` e a `EM_ANDAMENTO`.

---

## 9. `testar.sh` e o `Champs_dados.sql`

### 9.1 O que o `testar.sh` faz hoje

1. Remove um container antigo de teste (se existir) e sobe um MySQL 8.4 descartável no Docker:
   ```bash
   docker run -d --rm --name poo-mysql-teste \
     -e MYSQL_ROOT_PASSWORD=teste mysql:8.4
   ```
2. Registra um `trap` para parar o container ao sair.
3. Define uma função `mysql()` que executa o cliente dentro do container com `MYSQL_PWD=teste`.
4. Espera o servidor responder (`until mysql -e "SELECT 1"`).
5. Executa, na ordem, `Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql` e `Champs_dados.sql`; qualquer erro interrompe.
6. Confere, com consultas SQL:
   - contagens do `Champs_dados`: **4 campeonatos, 10 equipes, 5 estádios, 5 árbitros, 21 jogadores, 28 partidas e 20 inscrições**;
   - classificação da Copa Regional: **Tigres FC com 6 pontos e 1º lugar**;
   - **11 partidas encerradas** na Copa Regional;
   - o relatório de classificação devolve **10 equipes**;
   - o relatório de setembro/2026 devolve **10 partidas**.
7. Testa regras que **devem falhar**:
   - partida com data fora do período;
   - equipe jogando duas vezes na rodada;
   - desinscrição de equipe que já jogou;
   - camisa repetida na mesma equipe.
8. Mostra `OK`/`FALHOU` por verificação e imprime `TODOS OS TESTES PASSARAM` somente se todas passarem; caso contrário, sai com código 1.

### 9.2 Por que o `99_testes.sql` foi substituído

O `99_testes.sql` foi substituído pelo `Champs_dados.sql` porque o arquivo de dados tem **mais casos distintos**:

- 4 campeonatos, um em cada status (`PLANEJADO`, `EM_ANDAMENTO`, `ENCERRADO`, `CANCELADO`);
- 28 partidas com placares, jogos agendados, cancelados, um em andamento e um confronto encerrado na rodada 3;
- jogadores com datas de nascimento `NULL`;
- 20 inscrições em três campeonatos diferentes.

O `99_testes` era um roteiro separado, com dados próprios e verificações `OK`/`FALHOU`; o `Champs_dados` unificou dados de demonstração e teste. O `testar.sh` ficou um tempo apontando para o arquivo antigo (a pendência que motivou esta correção); agora ele usa `Champs_dados.sql` e faz as verificações diretamente.

- **Se quiser consultar o roteiro antigo:** ele pode ser recuperado do histórico:
  ```bash
  git show 2571563^:SQL_Champs/99_testes.sql > SQL_Champs/99_testes.sql
  ```
- **Para a apresentação:** o caminho atual é `Champs_dados.sql` + `testar.sh`; não é necessário rodar os dois arquivos de dados no mesmo banco.

### 9.3 O que o `99_testes.sql` continha (histórico)

Pelo histórico, o roteiro tinha cinco seções:

- **A) Dados de teste:** 1 campeonato, 6 equipes, 3 estádios, 3 árbitros e 8 partidas.
- **B) Classificação:** `SELECT`s que comparam os números calculados e mostram `OK`/`FALHOU` (ex.: Tigres em 1º com 6 pontos).
- **C) Relatórios:** chamadas às procedures com comentários `ESPERADO` (setembro: 5 jogos; dia 05/09: 2 jogos; classificação: 6 equipes).
- **D) Erros esperados:** comandos que **devem** falhar, cada um com `-- ESPERADO ERRO: <mensagem>` (data fora do período, equipe não inscrita, equipe duas vezes na rodada, árbitro ocupado, exclusão bloqueada, desinscrição bloqueada).
- **E) Alterações válidas:** lançar placar, jogo no último dia, jogo no horário de um cancelado, inscrever/desinscrever equipe sem jogos.

> Esse roteiro é útil para entender a ideia original, mas os números e IDs são diferentes dos do `Champs_dados`; não misture os dois no mesmo banco.

---

## 10. Como testar o banco manualmente

### 10.1 Criar e popular

```sql
SOURCE SQL_Champs/Champs_schema.sql;              -- ou abra e execute no Workbench
SOURCE SQL_Champs/Champs_procedures_triggers.sql;
SOURCE SQL_Champs/Champs_relatorios.sql;
SOURCE SQL_Champs/Champs_dados.sql;               -- opcional
```

### 10.2 Conferir a classificação

```sql
USE campeonato;
CALL sp_relatorio_classificacao(1);
CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');
```

### 10.3 Testar as regras (exemplos)

```sql
-- Deve falhar: data fora do período (Copa Regional vai até 2026-12-20)
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante,
                     id_estadio, id_arbitro, data_partida, status)
VALUES (1, 9, 1, 2, 1, 1, '2027-01-10 16:00', 'AGENDADA');

-- Deve falhar: equipe não inscrita (time 10 não está na Copa dos Campeões)
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante,
                     id_estadio, id_arbitro, data_partida, status)
VALUES (3, 9, 10, 4, 1, 1, '2026-10-25 16:00', 'AGENDADA');

-- Deve falhar: mesma equipe duas vezes na rodada
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante,
                     id_estadio, id_arbitro, data_partida, status)
VALUES (1, 1, 1, 2, 1, 1, '2026-09-05 20:00', 'AGENDADA');

-- Deve falhar: árbitro/estádio no mesmo horário
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante,
                     id_estadio, id_arbitro, data_partida, status)
VALUES (1, 9, 1, 2, 1, 1, '2026-09-05 16:00', 'AGENDADA');

-- Deve falhar: desinscrever equipe com partidas
DELETE FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 1;
```

### 10.4 Ver a classificação “crua”

```sql
SELECT id_campeonato, id_equipe, colocacao_equipe,
       partidas_jogadas, vitorias, empates, derrotas,
       gols_marcados, gols_sofridos, saldo_gols, pontuacao, status_atual
  FROM CLASSIFICACAO
 WHERE id_campeonato = 1
 ORDER BY (colocacao_equipe IS NULL), colocacao_equipe;
```

---

## 11. Perguntas e respostas (banco)

**1. Quantas tabelas o banco tem?**
Sete: `CAMPEONATO`, `EQUIPE`, `ESTADIO`, `ARBITRO`, `JOGADOR`, `CLASSIFICACAO` e `PARTIDA`.

**2. Qual é a diferença entre PK e FK?**
PK identifica a linha da própria tabela; FK aponta para a PK de outra tabela e garante que o valor existe.

**3. Por que `CLASSIFICACAO` tem PK composta?**
Porque representa a inscrição de uma equipe em um campeonato; a combinação `(id_campeonato, id_equipe)` é única.

**4. Por que a partida tem duas FKs para `EQUIPE`?**
Porque uma partida tem mandante e visitante; são duas colunas diferentes apontando para a mesma tabela.

**5. O que é `ON DELETE RESTRICT`?**
Impede apagar o pai se houver filho. Ex.: equipe com jogador/inscrição/partida.

**6. Onde há `ON DELETE CASCADE`?**
Em `fk_classificacao_campeonato`: apagar um campeonato sem partidas apaga as inscrições.

**7. O que é uma coluna gerada?**
Uma coluna calculada pelo MySQL a partir de outras. `pontuacao = vitorias * 3 + empates` e `saldo_gols = gols_marcados - gols_sofridos`.

**8. O que é um `CHECK`? Dê um exemplo.**
Uma regra de validação. `chk_partida_times` impede mandante = visitante.

**9. O que é `ENUM`?**
Uma coluna que só aceita uma lista fixa de valores, como o status da partida.

**10. O que é `AUTO_INCREMENT`?**
O MySQL gera o próximo número da PK sozinho. O Java não envia o ID no INSERT.

**11. O que é uma procedure?**
Um bloco SQL nomeado, com parâmetros, chamado com `CALL`.

**12. O que é um trigger?**
Um bloco executado automaticamente pelo banco em INSERT/UPDATE/DELETE.

**13. Qual a diferença entre `BEFORE` e `AFTER`?**
`BEFORE` roda antes de gravar e pode cancelar; `AFTER` roda depois e atualiza outras tabelas.

**14. O que são `NEW` e `OLD`?**
`NEW` é a linha nova; `OLD` é a linha antiga.

**15. O que faz `SIGNAL SQLSTATE '45000'`?**
Lança um erro com mensagem própria e cancela o comando, como um `throw`.

**16. Quais são as quatro regras da partida?**
Data dentro do período; equipes inscritas; equipe não joga duas vezes na rodada; árbitro/estádio livres no horário.

**17. Partida cancelada é validada?**
As quatro regras não; mas FKs, CHECKs e o `UNIQUE` de confronto continuam valendo.

**18. Como a classificação é calculada?**
A procedure zera os acumulados, soma as partidas `ENCERRADA` (duas linhas por partida, uma por equipe) e calcula a colocação com `ROW_NUMBER`.

**19. Quais são os critérios de desempate?**
Pontos, vitórias, saldo de gols, gols marcados e `id_equipe`.

**20. Por que só `ENCERRADA` conta?**
Porque jogos agendados/em andamento não têm resultado final, e cancelados não aconteceram.

**21. Por que a colocação pode ser `NULL`?**
Porque equipes que ainda não jogaram não têm posição; o relatório mostra `-`.

**22. O que é `ROW_NUMBER()`?**
Uma função de janela que numera linhas na ordem definida; aqui, 1º, 2º, 3º…

**23. Por que a procedure não usa `PARTITION BY`?**
Porque o `WHERE` já filtra um único campeonato.

**24. O que é `UNION ALL`?**
Junta os resultados das duas visões (mandante e visitante) mantendo todas as linhas; o `GROUP BY` soma por equipe.

**25. Por que `SUM(v)` conta vitórias?**
Porque no MySQL uma comparação booleana vale 1 (verdadeiro) ou 0 (falso); somar `v` conta os verdadeiros.

**26. O que é `UPDATE ... JOIN`?**
Atualiza uma tabela usando dados de outra tabela/subconsulta; aqui, `CLASSIFICACAO` recebe os totais calculados.

**27. O que é `INSERT IGNORE`?**
Insere; se houver duplicata, ignora em vez de dar erro. Usado em `sp_inscrever_equipe`.

**28. Por que a desinscrição é bloqueada?**
Porque uma equipe com partidas no campeonato não pode sair; o histórico ficaria inconsistente.

**29. Partida cancelada bloqueia desinscrição?**
Sim; o trigger de desinscrição não filtra status, diferente da validação da partida.

**30. Onde está o JOIN do relatório principal?**
`CLASSIFICACAO` + `CAMPEONATO` + `EQUIPE`, com subconsultas em `PARTIDA` + `ESTADIO`/`ARBITRO`.

**31. Por que `EQUIPE` aparece duas vezes no relatório de partidas?**
Porque uma é o mandante (`m`) e a outra o visitante (`v`).

**32. O que `DATE()` faz?**
Descarta a hora; permite que o último dia do período entre inteiro.

**33. O que `COALESCE` faz no placar?**
Troca `NULL` por `-`, resultando em `- x -`.

**34. O que `DATE_FORMAT` faz?**
Formata a data para `dd/mm/aaaa hh:mm`.

**35. O que é `COUNT(DISTINCT ...)`?**
Conta valores diferentes; usado para estádios/árbitros distintos.

**36. Por que o aproveitamento usa `IF`?**
Para evitar divisão por zero quando a equipe não jogou.

**37. O que é `ENGINE=InnoDB`?**
A engine que suporta FKs e transações.

**38. O que é `utf8mb4_unicode_ci`?**
Collation que aceita Unicode e compara sem diferenciar maiúsculas/minúsculas.

**39. O que é `DELIMITER`?**
Comando do cliente para trocar o terminador de comandos; permite procedures com `;` interno.

**40. Quantas partidas tem o `Champs_dados.sql`?**
28: 15 na Copa Regional, 9 na Liga e 4 na Copa dos Campeões.

**41. O que acontece ao apagar uma equipe com inscrição?**
A FK `fk_classificacao_equipe` (RESTRICT) bloqueia; é preciso remover a inscrição antes (se não houver partidas).

**42. O que acontece ao apagar um campeonato com partidas?**
A FK `fk_partida_campeonato` (RESTRICT) bloqueia. Sem partidas, o `CASCADE` apaga as inscrições.

**43. O que acontece ao apagar um jogador?**
É permitido; ele não é referenciado por ninguém.

**44. O que acontece ao apagar um estádio que já recebeu jogo?**
A FK `fk_partida_estadio` (RESTRICT) bloqueia.

**45. O que acontece ao apagar um árbitro que já apitou?**
A FK `fk_partida_arbitro` (RESTRICT) bloqueia.

**46. Qual é a diferença entre `UNIQUE` e `PK`?**
A PK identifica a linha e não aceita `NULL`; o `UNIQUE` apenas impede repetição e pode existir em várias colunas.

**47. O que é uma FK composta?**
Uma FK formada por mais de uma coluna. No projeto, as FKs são simples; a PK de `CLASSIFICACAO` é composta.

**48. O que é um índice?**
Uma estrutura que acelera buscas. `idx_partida_data` e `idx_partida_campeonato_data`.

**49. Por que a `PARTIDA` tem `acrescimo`?**
Para registrar os minutos de acréscimo; o banco só exige que seja ≥ 0.

**50. Por que `tempo_partida` é `NULL` fora de `EM_ANDAMENTO`?**
Porque o conceito de 1º/2º tempo só existe com o jogo em andamento; o CHECK garante.

---

## 12. Erros comuns de quem está estudando

| Erro | Por que acontece | Como evitar |
|---|---|---|
| Rodar `Champs_schema.sql` em banco com dados | o script faz `DROP DATABASE` | usar banco de teste ou backup |
| Rodar `Champs_dados.sql` e depois o `99_testes` antigo | IDs fixos e dados duplicados | usar um ou outro em banco limpo |
| Esquecer `USE campeonato` | tabelas não encontradas | os scripts já têm `USE`; no Workbench confira |
| Achar que `DELIMITER` é SQL | é comando do cliente | não copiar `DELIMITER` para dentro de uma procedure |
| Achar que o Java calcula a classificação | é o banco | trigger + procedure |
| Achar que `status_atual` é automático | é manual | botão da aba 6 |
| Achar que `JOGADOR.posicao` é `ENUM` | é `VARCHAR` | o combo padroniza, o banco não |
| Tentar apagar equipe com inscrição | FK RESTRICT | remover jogadores/inscrições/partidas na ordem |
| Achar que partida cancelada não precisa de FK | FKs/CHECKs/UNIQUE continuam | cadastrar com dados válidos |
| Achar que o `testar.sh` ainda usa `99_testes.sql` | ele foi atualizado para `Champs_dados.sql` | conferir a seção 9 |

---

## 13. Checklist do banco

- [ ] Sei explicar as 7 tabelas e as relações.
- [ ] Sei dizer todas as PKs e FKs.
- [ ] Sei explicar `ON DELETE RESTRICT` × `CASCADE`.
- [ ] Sei explicar colunas geradas e por que não se grava nelas.
- [ ] Sei listar os CHECKs da `PARTIDA`.
- [ ] Sei explicar as quatro regras da `sp_validar_partida`.
- [ ] Sei explicar os três passos da `sp_sincronizar_classificacao`.
- [ ] Sei explicar os seis triggers e quando cada um dispara.
- [ ] Sei explicar o JOIN das duas procedures de relatório.
- [ ] Sei explicar o `Champs_dados.sql` e o estado do `testar.sh`.
