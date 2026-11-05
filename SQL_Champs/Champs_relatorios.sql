-- =====================================================================
-- Champs_relatorios.sql  |  Relatorios da ROTINA PRINCIPAL
--
-- 1) sp_relatorio_classificacao: classificacao de um campeonato.
--    E o relatorio principal: parte da CLASSIFICACAO (que liga CAMPEONATO
--    e EQUIPE), e busca na PARTIDA os jogos em casa/fora, os estadios e os
--    arbitros de cada equipe. Assim aparecem, com nomes, as tabelas que a
--    rotina principal relaciona.
--
-- 2) sp_relatorio_partidas_periodo: partidas de um campeonato num periodo,
--    com nomes no lugar dos codigos, placar e situacao.
--
-- Os dois recebem parametros (campeonato e, no segundo, as datas) e sao
-- chamados pelo Java com CallableStatement.
--
-- Executar em: USE campeonato;  (depois de Champs_schema e Champs_procedures_triggers)
-- =====================================================================

USE campeonato;

DROP PROCEDURE IF EXISTS sp_relatorio_classificacao;

DELIMITER $$

-- ---------------------------------------------------------------------
-- Relatorio principal: classificacao completa de um campeonato.
-- Uso: CALL sp_relatorio_classificacao(1);
-- ---------------------------------------------------------------------
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
           -- aproveitamento: pontos possiveis = jogos * 3
           IF(cl.partidas_jogadas = 0, 0,
              ROUND(cl.pontuacao * 100.0 / (cl.partidas_jogadas * 3), 1)) AS aproveitamento,
           -- jogos em casa e fora (partidas encerradas)
           (SELECT COUNT(*)
              FROM PARTIDA p
             WHERE p.id_campeonato = cl.id_campeonato
               AND p.id_mandante = cl.id_equipe
               AND p.status = 'ENCERRADA') AS jogos_casa,
           (SELECT COUNT(*)
              FROM PARTIDA p
             WHERE p.id_campeonato = cl.id_campeonato
               AND p.id_visitante = cl.id_equipe
               AND p.status = 'ENCERRADA') AS jogos_fora,
           -- estadios e arbitros que a equipe encontrou no campeonato
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
END$$

DROP PROCEDURE IF EXISTS sp_relatorio_partidas_periodo$$

-- ---------------------------------------------------------------------
-- Partidas de um campeonato num periodo, com NOMES no lugar dos codigos.
-- INNER JOIN liga cada partida as tabelas relacionadas. EQUIPE entra
-- duas vezes, com apelidos diferentes: m = mandante, v = visitante.
-- Uso: CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_relatorio_partidas_periodo(
    IN p_id_campeonato INT,
    IN p_data_inicio   DATE,
    IN p_data_fim      DATE
)
BEGIN
    SELECT p.rodada,
           DATE_FORMAT(p.data_partida, '%d/%m/%Y %H:%i') AS data_hora,
           m.nome AS mandante,
           v.nome AS visitante,
           e.nome AS estadio,
           a.nome AS arbitro,
           -- jogo sem placar aparece como "- x -"
           CONCAT(COALESCE(p.gols_mandante, '-'), ' x ',
                  COALESCE(p.gols_visitante, '-')) AS placar,
           p.status
      FROM PARTIDA p
     INNER JOIN EQUIPE  m ON m.id_equipe  = p.id_mandante
     INNER JOIN EQUIPE  v ON v.id_equipe  = p.id_visitante
     INNER JOIN ESTADIO e ON e.id_estadio = p.id_estadio
     INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro
     WHERE p.id_campeonato = p_id_campeonato
       -- DATE() ignora a hora: o ultimo dia do periodo entra inteiro
       AND DATE(p.data_partida) BETWEEN p_data_inicio AND p_data_fim
     ORDER BY p.data_partida;
END$$

DELIMITER ;
