-- =====================================================================
-- 05_relatorios.sql  |  Relatorios da ROTINA PRINCIPAL (requisito secao 6)
--
-- View nao aceita parametro (WHERE data BETWEEN ?), por isso relatorio
-- com filtro e feito com PROCEDURE: o Java chama CALL ... e le o ResultSet.
-- Executar em: USE campeonato;  (depois de 01, 02 e 03)
-- =====================================================================

USE campeonato;

DROP PROCEDURE IF EXISTS sp_relatorio_partidas_periodo;

DELIMITER $$

-- ---------------------------------------------------------------------
-- RELATORIO 1 (principal): partidas por periodo
-- Uso: CALL sp_relatorio_partidas_periodo(1, '2026-03-01', '2026-03-31');
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_relatorio_partidas_periodo(
    IN p_id_campeonato INT,
    IN p_data_inicio   DATE,
    IN p_data_fim      DATE
)
BEGIN
    SELECT campeonato,
           rodada,
           data_hora,
           mandante,
           visitante,
           estadio,
           arbitro,
           placar,
           status
      FROM vw_partida_completa
     WHERE id_campeonato = p_id_campeonato
       AND data_partida >= p_data_inicio
       AND data_partida <  DATE_ADD(p_data_fim, INTERVAL 1 DAY)
     ORDER BY data_partida;
END$$

-- ---------------------------------------------------------------------
-- RELATORIO 2: classificacao do campeonato (com aproveitamento)
-- Uso: CALL sp_relatorio_classificacao(1);
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_relatorio_classificacao$$

CREATE PROCEDURE sp_relatorio_classificacao(IN p_id_campeonato INT)
BEGIN
    SELECT colocacao,
           equipe,
           jogos,
           vitorias,
           empates,
           derrotas,
           gols_marcados,
           gols_sofridos,
           saldo_gols,
           pontos,
           aproveitamento_pct,
           status_atual
      FROM vw_classificacao
     WHERE id_campeonato = p_id_campeonato
     ORDER BY colocacao IS NULL, colocacao;
END$$

-- ---------------------------------------------------------------------
-- RELATORIO 3: resumo por rodada (jogos, gols e media)
-- Uso: CALL sp_relatorio_rodadas(1);
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_relatorio_rodadas$$

CREATE PROCEDURE sp_relatorio_rodadas(IN p_id_campeonato INT)
BEGIN
    SELECT rodada,
           partidas,
           partidas_encerradas,
           total_gols,
           media_gols
      FROM vw_resumo_por_rodada
     WHERE id_campeonato = p_id_campeonato
     ORDER BY rodada;
END$$

DELIMITER ;
