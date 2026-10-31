DROP DATABASE IF EXISTS campeonato;

CREATE DATABASE IF NOT EXISTS campeonato
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE campeonato;

-- 1. Tabela CAMPEONATO
CREATE TABLE IF NOT EXISTS CAMPEONATO (
    id_campeonato INT PRIMARY KEY,
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
CREATE TABLE IF NOT EXISTS EQUIPE (
    id_equipe INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    cidade VARCHAR(30) NOT NULL
);

-- 3. Tabela ESTADIO
CREATE TABLE IF NOT EXISTS ESTADIO (
    id_estadio INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    capacidade INT NOT NULL,

    CHECK (capacidade > 0)
);

-- 4. Tabela JOGADOR
CREATE TABLE IF NOT EXISTS JOGADOR (
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

-- 5. Tabela CLASSIFICAÇÃO
CREATE TABLE CLASSIFICACAO (
    id_campeonato INT NOT NULL,
    id_equipe INT NOT NULL,
    colocacao_equipe INT,
    partidas_jogadas INT NOT NULL DEFAULT 0,
    vitorias INT NOT NULL DEFAULT 0,
    empates INT NOT NULL DEFAULT 0,
    derrotas INT NOT NULL DEFAULT 0,
    gols_marcados INT NOT NULL DEFAULT 0,
    gols_sofridos INT NOT NULL DEFAULT 0,
    saldo_gols INT GENERATED ALWAYS AS (gols_marcados - gols_sofridos) STORED,
    pontuacao  INT GENERATED ALWAYS AS (vitorias * 3 + empates) STORED,
    status_atual ENUM(
    'EM_ANDAMENTO',
    'CLASSIFICADO',
    'NAO_CLASSIFICADO',
    'REBAIXADO',
    'CAMPEAO'
	) NOT NULL DEFAULT 'EM_ANDAMENTO',
    
    PRIMARY KEY (id_campeonato, id_equipe),
    
    UNIQUE KEY uq_classif_posicao (id_campeonato, colocacao_equipe),
    
    CONSTRAINT fk_classif_campeonato FOREIGN KEY (id_campeonato)
        REFERENCES CAMPEONATO(id_campeonato) ON DELETE CASCADE ON UPDATE CASCADE,
        
    CONSTRAINT fk_classif_equipe FOREIGN KEY (id_equipe)
        REFERENCES EQUIPE(id_equipe) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- 6. Tabela ÁRBITRO
CREATE TABLE IF NOT EXISTS ARBITRO(
	id_arbitro INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    registro_federacao VARCHAR(30) UNIQUE,
    categoria ENUM(
        'LOCAL',
        'ESTADUAL',
        'NACIONAL',
        'FIFA'
    ) NOT NULL DEFAULT 'LOCAL'
);

-- 7. Tabela PARTIDA
CREATE TABLE IF NOT EXISTS PARTIDA (
    id_partida INT PRIMARY KEY AUTO_INCREMENT,
    id_campeonato INT NOT NULL,
    id_mandante INT NOT NULL,
    id_visitante INT NOT NULL,
    id_estadio INT NOT NULL,
    id_arbitro INT NOT NULL,
    data_partida DATETIME NOT NULL,
    gols_mandante INT,
    gols_visitante INT,
    acrescimo_1_tempo INT NOT NULL DEFAULT 0,
    acrescimo_2_tempo INT NOT NULL DEFAULT 0,

    status ENUM(
        'AGENDADA',
        'EM_ANDAMENTO',
        'ENCERRADA',
        'CANCELADA'
    ) NOT NULL DEFAULT 'AGENDADA',
    
    tempo_partida ENUM(
		'PRIMEIRO_TEMPO',
        'SEGUNDO_TEMPO'
    ),

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
        
	CONSTRAINT fk_partida_arbitro
        FOREIGN KEY (id_arbitro)
        REFERENCES ARBITRO(id_arbitro)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_gols_mandante
        CHECK (gols_mandante IS NULL OR gols_mandante >= 0),

    CONSTRAINT chk_gols_visitante
        CHECK (gols_visitante IS NULL OR gols_visitante >= 0),

    CONSTRAINT chk_placar_completo
        CHECK ((gols_mandante IS NULL AND gols_visitante IS NULL) OR (gols_mandante IS NOT NULL AND gols_visitante IS NOT NULL)),
        
	CONSTRAINT chk_times_diferentes
		CHECK(id_mandante <> id_visitante),
        
	CONSTRAINT chk_status_partida
		CHECK (
			(status = 'AGENDADA'
				AND gols_mandante IS NULL
				AND gols_visitante IS NULL)

			OR

			(status = 'EM_ANDAMENTO'
				AND gols_mandante IS NOT NULL
				AND gols_visitante IS NOT NULL)

			OR

			(status = 'ENCERRADA'
				AND gols_mandante IS NOT NULL
				AND gols_visitante IS NOT NULL)

			OR

			(status = 'CANCELADA'
				AND gols_mandante IS NULL
				AND gols_visitante IS NULL)
		),	
        
        CONSTRAINT chk_acrescimos
			CHECK(acrescimo_1_tempo >= 0 AND acrescimo_2_tempo >= 0)
);

