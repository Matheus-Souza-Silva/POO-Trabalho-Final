-- =====================================================================
-- 99_testes.sql  |  Roteiro de testes do banco CAMPEONATO
--
-- Executar DEPOIS dos 3 scripts Champs_*.sql, num banco recem-criado (os ids abaixo
-- contam com isso). Pode ser rodado no Workbench ou pelo testar.sh.
--
-- Como ler o resultado:
--   * cada SELECT de teste mostra 'OK' ou 'FALHOU'
--   * comandos marcados com "ESPERADO ERRO" DEVEM dar erro: e a regra
--     do banco funcionando
-- ATENCAO: estes dados sao so para teste. Os dados reais do sistema
-- sao cadastrados pelas telas Java.
-- =====================================================================

USE campeonato;

-- ---------------------------------------------------------------------
-- A) DADOS DE TESTE
-- ---------------------------------------------------------------------
INSERT INTO CAMPEONATO (nome, data_inicio, data_fim, status) VALUES
('Copa Regional 2026', '2026-08-01', '2026-12-20', 'EM_ANDAMENTO');      -- id 1

INSERT INTO EQUIPE (nome, cidade) VALUES
('Tigres FC',      'Sao Paulo'),        -- id 1
('Aguas EC',       'Campinas'),         -- id 2
('Leoes do Norte', 'Recife'),           -- id 3
('Falcoes United', 'Belo Horizonte'),   -- id 4
('Tubaroes SC',    'Santos'),           -- id 5
('Panteras AC',    'Curitiba');         -- id 6

INSERT INTO ESTADIO (nome, cidade, capacidade) VALUES
('Arena Central',   'Sao Paulo', 45000),   -- id 1
('Estadio do Vale', 'Campinas',  18000),   -- id 2
('Arena Litoral',   'Santos',    22000);   -- id 3

INSERT INTO ARBITRO (nome, registro_federacao, categoria) VALUES
('Carlos Andrade', 'FED-1001', 'FIFA'),       -- id 1
('Marina Souza',   'FED-1002', 'NACIONAL'),   -- id 2
('Paulo Ribeiro',  'FED-1003', 'ESTADUAL');   -- id 3

-- Inscricao das 6 equipes no campeonato 1
INSERT INTO CLASSIFICACAO (id_campeonato, id_equipe) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);

-- Partidas: a cada INSERT os triggers recalculam a classificacao
INSERT INTO PARTIDA
    (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro,
     data_partida, gols_mandante, gols_visitante, status)
VALUES
(1, 1, 1, 2, 1, 1, '2026-09-05 16:00', 2,    1,    'ENCERRADA'),   -- id 1
(1, 1, 3, 4, 2, 2, '2026-09-05 18:30', 0,    0,    'ENCERRADA'),   -- id 2
(1, 1, 5, 6, 3, 3, '2026-09-06 16:00', 3,    1,    'ENCERRADA'),   -- id 3
(1, 2, 2, 5, 2, 1, '2026-09-12 16:00', 1,    1,    'ENCERRADA'),   -- id 4
(1, 2, 4, 1, 2, 2, '2026-09-12 18:30', 2,    3,    'ENCERRADA'),   -- id 5
(1, 3, 6, 3, 3, 3, '2026-11-14 16:00', NULL, NULL, 'AGENDADA'),    -- id 6
(1, 3, 1, 5, 1, 1, '2026-11-15 16:00', NULL, NULL, 'AGENDADA'),    -- id 7
(1, 3, 2, 4, 2, 2, '2026-11-15 18:30', NULL, NULL, 'CANCELADA');   -- id 8

-- ---------------------------------------------------------------------
-- B) TESTES DE CLASSIFICACAO (calculada pelos triggers)
-- ---------------------------------------------------------------------
SELECT IF((SELECT CONCAT(colocacao_equipe, '|', pontuacao, '|', saldo_gols) FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 1) = '1|6|2', 'OK', 'FALHOU')
       AS 'Tigres 1o, 6 pontos, saldo 2';

SELECT IF((SELECT CONCAT(colocacao_equipe, '|', pontuacao) FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 5) = '2|4', 'OK', 'FALHOU')
       AS 'Tubaroes 2o, 4 pontos';

SELECT IF((SELECT CONCAT(colocacao_equipe, '|', pontuacao) FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 6) = '6|0', 'OK', 'FALHOU')
       AS 'Panteras 6o, 0 pontos';

-- ---------------------------------------------------------------------
-- C) TESTE DO RELATORIO (conferir o resultado com o comentario ESPERADO)
-- ---------------------------------------------------------------------

-- ESPERADO: 5 linhas; a 1a: 1 | 05/09/2026 16:00 | Tigres FC | Aguas EC | Arena Central | Carlos Andrade | 2 x 1 | ENCERRADA
CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');

-- ESPERADO: 2 linhas (os dois jogos de 05/09) - a data final entra no periodo
CALL sp_relatorio_partidas_periodo(1, '2026-09-05', '2026-09-05');

-- ---------------------------------------------------------------------
-- D) ERROS ESPERADOS (cada comando abaixo DEVE ser recusado pelo banco)
-- ---------------------------------------------------------------------

-- FK com ON DELETE RESTRICT: campeonato com partidas nao pode ser apagado
-- ESPERADO ERRO: Cannot delete or update a parent row
DELETE FROM CAMPEONATO WHERE id_campeonato = 1;

-- Regra 3: a mesma equipe nao joga duas vezes na mesma rodada
-- ESPERADO ERRO: Equipe ja possui partida nesta rodada
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 1, 1, 3, 3, 3, '2026-09-07 16:00');          -- Tigres (1) ja jogou na rodada 1

-- Regra 4: arbitro (ou estadio) em dois jogos no mesmo horario
-- ESPERADO ERRO: Arbitro ou estadio ja ocupado neste horario
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 4, 3, 6, 3, 1, '2026-09-05 16:00');          -- arbitro 1 apita a partida 1 nesse horario
-- ESPERADO ERRO: Arbitro ou estadio ja ocupado neste horario
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 4, 3, 6, 1, 3, '2026-09-05 16:00');          -- estadio 1 recebe a partida 1 nesse horario

-- Regra 1: data fora do periodo do campeonato (01/08 a 20/12/2026)
-- ESPERADO ERRO: Data da partida fora do periodo do campeonato
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 4, 3, 6, 3, 3, '2030-01-01 16:00');

-- Regra 2: equipe que nao foi inscrita no campeonato
INSERT INTO EQUIPE (nome, cidade) VALUES ('Sem Inscricao FC', 'Natal');   -- vira a equipe 7
-- ESPERADO ERRO: Equipe nao inscrita neste campeonato
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 4, 7, 1, 3, 3, '2026-10-01 16:00');

-- CHECK: tempo da partida (1o/2o tempo) so existe com o jogo em andamento
-- ESPERADO ERRO: chk_partida_tempo
UPDATE PARTIDA SET tempo_partida = 'PRIMEIRO_TEMPO' WHERE id_partida = 1;

-- Equipe que ja jogou nao pode ser desinscrita (perderia o historico)
-- ESPERADO ERRO: Equipe possui partidas neste campeonato
DELETE FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 1;

-- ---------------------------------------------------------------------
-- E) ALTERACOES VALIDAS (devem funcionar; alteram os dados de teste)
-- ---------------------------------------------------------------------

-- Lancar o placar de um jogo agendado nao conflita com ele mesmo
UPDATE PARTIDA SET status = 'ENCERRADA', gols_mandante = 2, gols_visitante = 0 WHERE id_partida = 6;
SELECT IF((SELECT pontuacao FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 6) = 3,
          'OK', 'FALHOU') AS 'Placar lancado: Panteras soma 3 pontos';

-- Jogo no ultimo dia do campeonato e aceito
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 10, 1, 2, 1, 1, '2026-12-20 20:00');
SELECT IF((SELECT COUNT(*) FROM PARTIDA WHERE rodada = 10) = 1,
          'OK', 'FALHOU') AS 'Jogo no ultimo dia aceito';

-- Jogo cancelado libera horario, estadio, arbitro e rodada (mesmo horario da partida 8)
INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, id_estadio, id_arbitro, data_partida)
VALUES (1, 3, 4, 2, 2, 2, '2026-11-15 18:30');
SELECT IF((SELECT COUNT(*) FROM PARTIDA WHERE rodada = 3 AND status <> 'CANCELADA') = 3,
          'OK', 'FALHOU') AS 'Jogo cancelado libera o horario';

-- Inscrever e desinscrever uma equipe que ainda nao jogou
CALL sp_inscrever_equipe(1, 7);
SELECT IF((SELECT colocacao_equipe IS NULL AND pontuacao = 0 FROM CLASSIFICACAO
            WHERE id_campeonato = 1 AND id_equipe = 7), 'OK', 'FALHOU') AS 'Equipe inscrita sem jogos';
DELETE FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 7;
SELECT IF((SELECT COUNT(*) FROM CLASSIFICACAO WHERE id_equipe = 7) = 0,
          'OK', 'FALHOU') AS 'Equipe sem jogos desinscrita';
