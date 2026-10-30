
CREATE DATABASE sistema_gestao;
USE sistema_gestao;

-- 1. Tabela CAMPEONATO
CREATE TABLE CAMPEONATO (
    id_campeonato INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    status ENUM(
        'PLANEJADO',
        'EM_ANDAMENTO',
        'ENCERRADO',
        'CANCELADO'
    ) NOT NULL DEFAULT 'PLANEJADO',

    CHECK (data_fim >= data_inicio)
);

-- 2. Tabela EQUIPE
CREATE TABLE EQUIPE (
    id_equipe INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    cidade VARCHAR(30) NOT NULL
);

-- 3. Tabela ESTADIO
CREATE TABLE ESTADIO (
    id_estadio INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    capacidade INT NOT NULL,

    CHECK (capacidade > 0)
);

-- 4. Tabela JOGADOR
CREATE TABLE JOGADOR (
    id_jogador INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    posicao VARCHAR(100) NOT NULL,
    id_equipe INT,

    CONSTRAINT fk_jogador_equipe
        FOREIGN KEY (id_equipe)
        REFERENCES EQUIPE(id_equipe)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE CLASSIFICACAO (
	id_equipe INT PRIMARY KEY,
	nome_equipe VARCHAR,
    colocacao_equipe INT,
    partidas_jogadas INT NOT NULL DEFAULT 0,
    vitorias INT NOT NULL DEFAULT 0,
    empates INT NOT NULL DEFAULT 0,
	derrotas INT NOT NULL DEFAULT 0,
    pontuacao INT NOT NULL,
    gol_marcados INT NOT NULL DEFAULT 0,
    gol_sofridos INT NOT NULL DEFAULT 0,
    saldo_gols INT NOT NULL DEFAULT 0,
    status_atual ENUM ('ELIMINADO', 'CLASSIFICADO', 'A JOGAR') NOT NULL DEFAULT 'CLASSIFICADO',
    FOREIGN KEY (id_equipe) REFERENCES EQUIPE (id_equipe)
);

-- 5. Tabela PARTIDA
CREATE TABLE PARTIDA (
    id_partida INT PRIMARY KEY AUTO_INCREMENT,
    id_campeonato INT NOT NULL,
    id_mandante INT NOT NULL,
    id_visitante INT NOT NULL,
    id_estadio INT NOT NULL,
    data_partida DATETIME NOT NULL,
    gols_mandante INT,
    gols_visitante INT,

    status ENUM(
        'AGENDADA',
        'EM_ANDAMENTO',
        'ENCERRADA',
        'CANCELADA'
    ) NOT NULL DEFAULT 'AGENDADA',

    CONSTRAINT fk_partida_campeonato
        FOREIGN KEY (id_campeonato)
        REFERENCES CAMPEONATO(id_campeonato)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_partida_mandante
        FOREIGN KEY (id_mandante)
        REFERENCES EQUIPE(id_equipe)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_partida_visitante
        FOREIGN KEY (id_visitante)
        REFERENCES EQUIPE(id_equipe)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_partida_estadio
        FOREIGN KEY (id_estadio)
        REFERENCES ESTADIO(id_estadio)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_gols_mandante
        CHECK (gols_mandante IS NULL OR gols_mandante >= 0),

    CONSTRAINT chk_gols_visitante
        CHECK (gols_visitante IS NULL OR gols_visitante >= 0),

    CONSTRAINT chk_placar_completo
        CHECK (
            (gols_mandante IS NULL AND gols_visitante IS NULL)
            OR
            (gols_mandante IS NOT NULL AND gols_visitante IS NOT NULL)
        )
);