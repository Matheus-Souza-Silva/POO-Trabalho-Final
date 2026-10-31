-- =====================================================================
-- 02_views.sql  |  Consultas prontas (VIEWS)
-- Views NAO recebem parametros -> sao listagens reutilizaveis.
-- Relatorios com parametro (periodo, campeonato) ficam em 05_relatorios.sql.
-- Executar em: USE campeonato;
-- =====================================================================

USE campeonato;

-- ---------------------------------------------------------------------
-- vw_partida_completa
-- Requisito da secao 5 (JOIN): mostra NOMES no lugar das chaves
-- estrangeiras. PARTIDA + CAMPEONATO + EQUIPE (2x) + ESTADIO + ARBITRO.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_partida_completa AS
SELECT p.id_partida,
       p.id_campeonato,
       c.nome AS campeonato,
       p.rodada,
       p.id_mandante,
       m.nome AS mandante,
       p.id_visitante,
       v.nome AS visitante,
       p.id_estadio,
       e.nome AS estadio,
       e.cidade AS cidade_estadio,
       p.id_arbitro,
       a.nome AS arbitro,
       a.categoria AS categoria_arbitro,
       p.data_partida,
       DATE_FORMAT(p.data_partida, '%d/%m/%Y %H:%i') AS data_hora,
       p.gols_mandante,
       p.gols_visitante,
       CONCAT(COALESCE(p.gols_mandante, '-'), ' x ',
              COALESCE(p.gols_visitante, '-')) AS placar,
       p.acrescimo,
       p.status,
       p.tempo_partida
FROM PARTIDA p
INNER JOIN CAMPEONATO c ON c.id_campeonato = p.id_campeonato
INNER JOIN EQUIPE     m ON m.id_equipe     = p.id_mandante
INNER JOIN EQUIPE     v ON v.id_equipe     = p.id_visitante
INNER JOIN ESTADIO    e ON e.id_estadio    = p.id_estadio
INNER JOIN ARBITRO    a ON a.id_arbitro    = p.id_arbitro;

-- ---------------------------------------------------------------------
-- vw_classificacao
-- Tabela de classificacao pronta para a tela/relatorio: nome da equipe,
-- pontos, saldo, aproveitamento e posicao (colocacao_equipe).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_classificacao AS
SELECT c.id_campeonato,
       cam.nome AS campeonato,
       c.colocacao_equipe AS colocacao,
       c.id_equipe,
       e.nome  AS equipe,
       e.cidade,
       c.partidas_jogadas AS jogos,
       c.vitorias,
       c.empates,
       c.derrotas,
       c.gols_marcados,
       c.gols_sofridos,
       c.saldo_gols,
       c.pontuacao AS pontos,
       ROUND(100 * c.pontuacao / NULLIF(c.partidas_jogadas * 3, 0), 1) AS aproveitamento_pct,
       c.status_atual
FROM CLASSIFICACAO c
INNER JOIN EQUIPE     e   ON e.id_equipe     = c.id_equipe
INNER JOIN CAMPEONATO cam ON cam.id_campeonato = c.id_campeonato;

-- ---------------------------------------------------------------------
-- vw_resumo_por_rodada
-- Apoia o relatorio da rotina principal (jogos e gols por rodada).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_resumo_por_rodada AS
SELECT p.id_campeonato,
       c.nome AS campeonato,
       p.rodada,
       COUNT(*) AS partidas,
       SUM(p.status = 'ENCERRADA') AS partidas_encerradas,
       COALESCE(SUM(CASE WHEN p.status = 'ENCERRADA'
                         THEN p.gols_mandante + p.gols_visitante END), 0) AS total_gols,
       ROUND(AVG(CASE WHEN p.status = 'ENCERRADA'
                      THEN p.gols_mandante + p.gols_visitante END), 2) AS media_gols
FROM PARTIDA p
INNER JOIN CAMPEONATO c ON c.id_campeonato = p.id_campeonato
GROUP BY p.id_campeonato, c.nome, p.rodada;

-- ---------------------------------------------------------------------
-- vw_partidas_agendadas
-- Jogos ainda nao realizados, para o "painel" do sistema.
-- (sem filtro por NOW(): assim a lista nao fica vazia em demonstracao)
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_partidas_agendadas AS
SELECT id_partida, campeonato, rodada, mandante, visitante,
       estadio, arbitro, data_partida, data_hora, status
FROM vw_partida_completa
WHERE status = 'AGENDADA'
ORDER BY data_partida;
