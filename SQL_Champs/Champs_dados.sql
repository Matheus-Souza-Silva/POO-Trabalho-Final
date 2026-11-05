-- =====================================================================
-- Champs_dados.sql  |  Dados de demonstracao (INSERTs variados)
--
-- Execute DEPOIS de:
--   Champs_schema.sql
--   Champs_procedures_triggers.sql
--   Champs_relatorios.sql
--
-- IMPORTANTE:
--   * use num banco recem-criado (os IDs abaixo contam com AUTO_INCREMENT);
--   * NAO execute junto com o 99_testes.sql: os dois arquivos criam dados
--     diferentes e o 99_testes espera o banco vazio para conferir IDs;
--   * os dados sao ficticios e servem para demonstrar as telas/relatorios.
--
-- O que este arquivo cobre:
--   4 campeonatos (PLANEJADO, EM_ANDAMENTO, ENCERRADO e CANCELADO)
--   10 equipes, 5 estadios e 5 arbitros (todos com nomes/registros unicos)
--   inscricoes em 3 campeonatos
--   21 jogadores com camisas unicas por equipe (e algumas datas NULL)
--   28 partidas com placar, jogos agendados, cancelados e em andamento
-- =====================================================================

USE campeonato;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 1) CAMPEONATOS
-- ---------------------------------------------------------------------
INSERT INTO CAMPEONATO (nome, data_inicio, data_fim, status) VALUES
('Copa Regional 2026',      '2026-08-01', '2026-12-20', 'EM_ANDAMENTO'), -- id 1
('Liga Metropolitana 2026', '2026-03-01', '2026-07-31', 'ENCERRADO'),    -- id 2
('Copa dos Campeoes 2026',  '2026-10-01', '2026-11-30', 'PLANEJADO'),    -- id 3
('Torneio de Verao 2027',   '2027-01-05', '2027-02-20', 'CANCELADO');    -- id 4

-- ---------------------------------------------------------------------
-- 2) EQUIPES (10 nomes/cidades diferentes)
-- ---------------------------------------------------------------------
INSERT INTO EQUIPE (nome, cidade) VALUES
('Tigres FC',        'Sao Paulo'),        -- id 1
('Aguas EC',         'Campinas'),         -- id 2
('Leoes do Norte',   'Recife'),           -- id 3
('Falcoes United',   'Belo Horizonte'),   -- id 4
('Tubaroes SC',      'Santos'),           -- id 5
('Panteras AC',      'Curitiba'),         -- id 6
('Corvos FC',        'Porto Alegre'),     -- id 7
('Oncas EC',         'Manaus'),           -- id 8
('Gavioes do Sul',   'Florianopolis'),    -- id 9
('Titas FC',         'Brasilia');         -- id 10

-- ---------------------------------------------------------------------
-- 3) ESTADIOS (nome + cidade unicos; capacidades diferentes)
-- ---------------------------------------------------------------------
INSERT INTO ESTADIO (nome, cidade, capacidade) VALUES
('Arena Central',     'Sao Paulo',      45000),  -- id 1
('Estadio do Vale',   'Campinas',       18000),  -- id 2
('Arena Litoral',     'Santos',         22000),  -- id 3
('Arena da Amazonia', 'Manaus',         44000),  -- id 4
('Estadio do Cerrado','Brasilia',       30000);  -- id 5

-- ---------------------------------------------------------------------
-- 4) ARBITROS (registros unicos e categorias variadas)
-- ---------------------------------------------------------------------
INSERT INTO ARBITRO (nome, registro_federacao, categoria) VALUES
('Carlos Andrade', 'FED-1001', 'FIFA'),      -- id 1
('Marina Souza',   'FED-1002', 'NACIONAL'),  -- id 2
('Paulo Ribeiro',  'FED-1003', 'ESTADUAL'),  -- id 3
('Ana Beatriz',    'FED-1004', 'FIFA'),      -- id 4
('Joao Vitor',     'FED-1005', 'LOCAL');     -- id 5

-- ---------------------------------------------------------------------
-- 5) INSCRICOES
--    Copa Regional: as 10 equipes
--    Liga Metropolitana: equipes 1..6
--    Copa dos Campeoes: equipes 3, 4, 7 e 8
-- ---------------------------------------------------------------------
INSERT INTO CLASSIFICACAO (id_campeonato, id_equipe) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6),
(3, 3), (3, 4), (3, 7), (3, 8);

-- ---------------------------------------------------------------------
-- 6) JOGADORES
--    UNIQUE (id_equipe, numero_camisa): a camisa nao repete na equipe.
--    Nomes e camisas sao unicos dentro de cada equipe.
-- ---------------------------------------------------------------------
INSERT INTO JOGADOR (nome, posicao, numero_camisa, data_nascimento, id_equipe) VALUES
('Rafael Alves',   'GOLEIRO',   1,  '1995-03-12', 1),
('Bruno Lima',     'ATACANTE',  9,  '1998-07-25', 1),
('Caio Mendes',    'MEIA',      10, '2000-01-15', 1),
('Diego Castro',   'GOLEIRO',   1,  '1994-11-02', 2),
('Lucas Prado',    'MEIA',      10, '1999-04-18', 2),
('Marcos Vieira',  'ATACANTE',  7,  '1997-09-30', 3),
('Andre Souza',    'GOLEIRO',   1,  '1993-06-11', 3),
('Felipe Rocha',   'VOLANTE',   5,  '1996-02-20', 4),
('Gustavo Dias',   'ATACANTE',  9,  '2001-03-08', 4),
('Henrique Alves', 'GOLEIRO',   1,  '1992-12-01', 5),
('Igor Santos',    'ATACANTE',  11, '1999-08-19', 5),
('Joao Pedro',     'MEIA',      8,  '2000-05-27', 6),
('Kleber Nunes',   'GOLEIRO',   1,  '1991-10-10', 6),
('Leandro Costa',  'ZAGUEIRO',  4,  '1998-01-05', 7),
('Murilo Rocha',   'ATACANTE',  9,  NULL,         7),
('Nicolas Melo',   'GOLEIRO',   1,  '1995-07-14', 8),
('Otavio Reis',    'MEIA',      10, '2002-02-22', 8),
('Paulo Henrique', 'VOLANTE',   6,  '1997-11-11', 9),
('Renan Silva',    'GOLEIRO',   1,  '1994-04-04', 9),
('Samuel Araujo',  'ATACANTE',  7,  '1999-09-09', 10),
('Tiago Freitas',  'GOLEIRO',   1,  NULL,         10);

-- ---------------------------------------------------------------------
-- 7) PARTIDAS DA COPA REGIONAL (campeonato 1)
--    Rodada 1 e 2: jogos ENCERRADOS (contam para a classificacao).
--    Rodada 3: mistura AGENDADA, CANCELADA e EM_ANDAMENTO.
--    Datas dentro de 2026-08-01 a 2026-12-20.
-- ---------------------------------------------------------------------
INSERT INTO PARTIDA
    (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro,
     data_partida, gols_mandante, gols_visitante, status, tempo_partida)
VALUES
-- Rodada 1
(1, 1, 1,  10, 1, 1, '2026-09-05 16:00', 2,    1,    'ENCERRADA', NULL),
(1, 1, 2,   9, 2, 2, '2026-09-05 18:30', 1,    1,    'ENCERRADA', NULL),
(1, 1, 3,   8, 3, 3, '2026-09-06 16:00', 0,    2,    'ENCERRADA', NULL),
(1, 1, 4,   7, 4, 4, '2026-09-06 18:00', 3,    0,    'ENCERRADA', NULL),
(1, 1, 5,   6, 5, 5, '2026-09-07 16:00', 2,    2,    'ENCERRADA', NULL),
-- Rodada 2
(1, 2, 1,   9, 1, 2, '2026-09-12 16:00', 1,    0,    'ENCERRADA', NULL),
(1, 2, 10,  8, 2, 1, '2026-09-12 18:30', 0,    0,    'ENCERRADA', NULL),
(1, 2, 2,   7, 3, 4, '2026-09-13 16:00', 2,    1,    'ENCERRADA', NULL),
(1, 2, 3,   6, 4, 3, '2026-09-13 18:00', 1,    2,    'ENCERRADA', NULL),
(1, 2, 4,   5, 5, 5, '2026-09-14 16:00', 0,    1,    'ENCERRADA', NULL),
-- Rodada 3 (situacoes diferentes)
(1, 3, 1,   8, 1, 3, '2026-10-10 16:00', NULL, NULL, 'AGENDADA',    NULL),
(1, 3, 9,   7, 2, 4, '2026-10-10 18:30', NULL, NULL, 'AGENDADA',    NULL),
(1, 3, 10,  6, 5, 1, '2026-10-11 16:00', NULL, NULL, 'CANCELADA',   NULL),
(1, 3, 2,   5, 3, 2, '2026-10-11 18:00', 1,    0,    'EM_ANDAMENTO','PRIMEIRO_TEMPO'),
(1, 3, 3,   4, 4, 5, '2026-10-12 16:00', 1,    1,    'ENCERRADA',   NULL);

-- ---------------------------------------------------------------------
-- 8) PARTIDAS DA LIGA METROPOLITANA (campeonato 2, encerrado)
--    Todas as equipes inscritas (1..6); jogos em abril/maio/junho.
-- ---------------------------------------------------------------------
INSERT INTO PARTIDA
    (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro,
     data_partida, gols_mandante, gols_visitante, status, tempo_partida)
VALUES
(2, 1, 1, 6, 1, 1, '2026-04-04 16:00', 2, 0, 'ENCERRADA', NULL),
(2, 1, 2, 5, 2, 2, '2026-04-04 18:00', 1, 1, 'ENCERRADA', NULL),
(2, 1, 3, 4, 3, 3, '2026-04-05 16:00', 0, 3, 'ENCERRADA', NULL),
(2, 2, 1, 5, 1, 2, '2026-05-09 16:00', 1, 2, 'ENCERRADA', NULL),
(2, 2, 6, 4, 2, 3, '2026-05-09 18:00', 0, 0, 'ENCERRADA', NULL),
(2, 2, 2, 3, 3, 1, '2026-05-10 16:00', 3, 1, 'ENCERRADA', NULL),
(2, 3, 1, 4, 1, 3, '2026-06-13 16:00', 2, 1, 'ENCERRADA', NULL),
(2, 3, 2, 6, 2, 1, '2026-06-13 18:00', 1, 0, 'ENCERRADA', NULL),
(2, 3, 3, 5, 3, 2, '2026-06-14 16:00', 0, 0, 'ENCERRADA', NULL);

-- ---------------------------------------------------------------------
-- 9) PARTIDAS DA COPA DOS CAMPEOES (campeonato 3, planejado)
--    Apenas equipes 3, 4, 7 e 8 foram inscritas.
-- ---------------------------------------------------------------------
INSERT INTO PARTIDA
    (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro,
     data_partida, gols_mandante, gols_visitante, status, tempo_partida)
VALUES
(3, 1, 3, 7, 4, 4, '2026-10-20 16:00', NULL, NULL, 'AGENDADA', NULL),
(3, 1, 4, 8, 5, 5, '2026-10-20 18:00', NULL, NULL, 'AGENDADA', NULL),
(3, 2, 3, 8, 2, 1, '2026-11-10 16:00', NULL, NULL, 'AGENDADA', NULL),
(3, 2, 4, 7, 3, 2, '2026-11-10 18:00', NULL, NULL, 'AGENDADA', NULL);

COMMIT;

-- ---------------------------------------------------------------------
-- 10) CONFERENCIA VISUAL (opcional)
--     Rode estas consultas para ver a classificacao e o relatorio prontos.
-- ---------------------------------------------------------------------
SELECT 'Dados de demonstracao carregados' AS status;

-- Classificacao da Copa Regional
CALL sp_relatorio_classificacao(1);

-- Partidas da Copa Regional em setembro/2026
CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');
