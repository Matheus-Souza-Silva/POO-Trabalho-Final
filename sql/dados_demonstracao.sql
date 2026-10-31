-- =====================================================================
-- 04_dados_demonstracao.sql  |  Dados para demonstracao
-- Requisito da secao 9: "Dados cadastrados para demonstracao".
-- Executar DEPOIS de 01, 02 e 03 (com os triggers ativos: a classificacao
-- e calculada automaticamente ao inserir as partidas).
-- =====================================================================

USE campeonato;

-- 1) Campeonato -------------------------------------------------------
INSERT INTO CAMPEONATO (nome, data_inicio, data_fim, status) VALUES
('Copa Regional 2026', '2026-08-01', '2026-12-20', 'EM_ANDAMENTO');

-- 2) Equipes ----------------------------------------------------------
INSERT INTO EQUIPE (nome, cidade) VALUES
('Tigres FC',      'Sao Paulo'),
('Aguas EC',       'Campinas'),
('Leoes do Norte', 'Recife'),
('Falcoes United', 'Belo Horizonte'),
('Tubaroes SC',    'Santos'),
('Panteras AC',    'Curitiba');

-- 3) Estadios ---------------------------------------------------------
INSERT INTO ESTADIO (nome, cidade, capacidade) VALUES
('Arena Central',   'Sao Paulo',      45000),
('Estadio do Vale', 'Campinas',       18000),
('Arena Litoral',   'Santos',         22000);

-- 4) Arbitros ---------------------------------------------------------
INSERT INTO ARBITRO (nome, registro_federacao, categoria) VALUES
('Carlos Andrade', 'FED-1001', 'FIFA'),
('Marina Souza',   'FED-1002', 'NACIONAL'),
('Paulo Ribeiro',  'FED-1003', 'ESTADUAL');

-- 5) Jogadores --------------------------------------------------------
INSERT INTO JOGADOR (nome, posicao, numero_camisa, data_nascimento, id_equipe) VALUES
('Rafael Lima',   'Goleiro',   1,  '1995-04-12', 1),
('Bruno Alves',   'Atacante',  9,  '1997-08-30', 1),
('Diego Matos',   'Goleiro',   1,  '1994-02-18', 2),
('Igor Nunes',    'Atacante',  10, '1998-11-05', 2),
('Felipe Rocha',  'Goleiro',   1,  '1996-06-22', 3),
('Caio Mendes',   'Meia',      8,  '1999-01-09', 3),
('Tiago Freitas', 'Goleiro',   1,  '1993-12-03', 4),
('Lucas Barros',  'Atacante',  7,  '1997-03-27', 4),
('Andre Pires',   'Goleiro',   1,  '1995-09-14', 5),
('Vitor Hugo',    'Atacante',  11, '2000-05-21', 5),
('Sergio Dias',   'Goleiro',   1,  '1992-07-08', 6),
('Renan Costa',   'Meia',      10, '1998-02-16', 6);

-- 6) Inscricao das equipes no campeonato ------------------------------
INSERT INTO CLASSIFICACAO (id_campeonato, id_equipe) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6);

-- 7) Partidas ---------------------------------------------------------
--    Os triggers recalculam a classificacao a cada INSERT.
INSERT INTO PARTIDA
    (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro,
     data_partida, gols_mandante, gols_visitante, status)
VALUES
-- rodada 1
(1, 1, 1, 2, 1, 1, '2026-09-05 16:00:00', 2, 1, 'ENCERRADA'),
(1, 1, 3, 4, 2, 2, '2026-09-05 18:30:00', 0, 0, 'ENCERRADA'),
(1, 1, 5, 6, 3, 3, '2026-09-06 16:00:00', 3, 1, 'ENCERRADA'),
-- rodada 2
(1, 2, 2, 5, 2, 1, '2026-09-12 16:00:00', 1, 1, 'ENCERRADA'),
(1, 2, 4, 1, 2, 2, '2026-09-12 18:30:00', 2, 3, 'ENCERRADA'),
-- rodada 3 (ainda nao realizadas)
(1, 3, 6, 3, 3, 3, '2026-11-14 16:00:00', NULL, NULL, 'AGENDADA'),
(1, 3, 1, 5, 1, 1, '2026-11-15 16:00:00', NULL, NULL, 'AGENDADA'),
(1, 3, 2, 4, 2, 2, '2026-11-15 18:30:00', NULL, NULL, 'CANCELADA');

-- 8) Conferencia ------------------------------------------------------
SELECT colocacao, equipe, jogos, vitorias, empates, derrotas,
       gols_marcados, gols_sofridos, saldo_gols, pontos
  FROM vw_classificacao
 WHERE id_campeonato = 1
 ORDER BY colocacao IS NULL, colocacao;

SELECT rodada, mandante, visitante, data_hora, placar, status
  FROM vw_partida_completa
 WHERE id_campeonato = 1
 ORDER BY data_partida;
