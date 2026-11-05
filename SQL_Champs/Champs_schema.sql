-- =====================================================================
-- Champs_schema.sql  |  Estrutura do banco CAMPEONATO (MySQL 8.0.16+)
-- Ordem de execucao: Champs_schema -> Champs_procedures_triggers -> Champs_relatorios
-- =====================================================================

DROP DATABASE IF EXISTS campeonato;

CREATE DATABASE campeonato
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campeonato;

-- ---------------------------------------------------------------------
-- 1) CAMPEONATO  (CRUD simples)
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 2) EQUIPE  (CRUD simples)
-- ---------------------------------------------------------------------
CREATE TABLE EQUIPE (
    id_equipe INT PRIMARY KEY AUTO_INCREMENT,
    nome      VARCHAR(100) NOT NULL,
    cidade    VARCHAR(100) NOT NULL,

    CONSTRAINT uq_equipe_nome UNIQUE (nome)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3) ESTADIO  (CRUD simples)
-- ---------------------------------------------------------------------
CREATE TABLE ESTADIO (
    id_estadio INT PRIMARY KEY AUTO_INCREMENT,
    nome       VARCHAR(100) NOT NULL,
    cidade     VARCHAR(100) NOT NULL,
    capacidade INT NOT NULL,

    CONSTRAINT uq_estadio_nome_cidade UNIQUE (nome, cidade),
    CONSTRAINT chk_estadio_capacidade CHECK (capacidade > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4) JOGADOR  (CRUD simples)
-- ---------------------------------------------------------------------
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
    -- A mesma camisa nao pode repetir DENTRO da mesma equipe.
    -- O UNIQUE e composto: (id_equipe, numero_camisa), nao apenas a camisa.
    CONSTRAINT uq_jogador_camisa UNIQUE (id_equipe, numero_camisa),
    CONSTRAINT chk_jogador_camisa CHECK (numero_camisa BETWEEN 1 AND 99)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5) ARBITRO  (CRUD simples)
-- ---------------------------------------------------------------------
CREATE TABLE ARBITRO (
    id_arbitro         INT PRIMARY KEY AUTO_INCREMENT,
    nome               VARCHAR(100) NOT NULL,
    registro_federacao VARCHAR(30)  NOT NULL,
    categoria          ENUM('LOCAL','ESTADUAL','NACIONAL','FIFA')
                       NOT NULL DEFAULT 'LOCAL',

    CONSTRAINT uq_arbitro_registro UNIQUE (registro_federacao)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6) CLASSIFICACAO  (= tabela de INSCRICAO + acumulados do campeonato)
--    pontuacao e saldo_gols sao COLUNAS GERADAS: o banco calcula sozinho.
--    colocacao_equipe e preenchida pela procedure de sincronizacao,
--    disparada pelos triggers da tabela PARTIDA.
-- ---------------------------------------------------------------------
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

    -- Colunas geradas: nao sao enviadas no INSERT/UPDATE. O MySQL
    -- recalcula sozinho quando gols/vitorias/empates mudam.
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

-- ---------------------------------------------------------------------
-- 7) PARTIDA  (CRUD PRINCIPAL: registrar jogo e resultado)
-- ---------------------------------------------------------------------
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
    -- ON UPDATE RESTRICT: colunas usadas em CHECK nao podem ter ON UPDATE CASCADE
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
    -- placar so existe quando o jogo aconteceu (ou esta acontecendo)
    CONSTRAINT chk_partida_placar_por_status CHECK (
        (status = 'ENCERRADA' AND gols_mandante IS NOT NULL AND gols_visitante IS NOT NULL)
        OR (status IN ('AGENDADA','CANCELADA') AND gols_mandante IS NULL AND gols_visitante IS NULL)
        OR (status = 'EM_ANDAMENTO')
    ),
    -- 1o/2o tempo so faz sentido com o jogo acontecendo
    CONSTRAINT chk_partida_tempo CHECK (tempo_partida IS NULL OR status = 'EM_ANDAMENTO'),
    -- um confronto so pode ocorrer uma vez por rodada no campeonato
    CONSTRAINT uq_partida_confronto UNIQUE (id_campeonato, rodada, id_mandante, id_visitante),

    INDEX idx_partida_data (data_partida),
    INDEX idx_partida_campeonato_data (id_campeonato, data_partida)
) ENGINE=InnoDB;