-- =====================================================================
-- Champs_relatorios.sql  |  Relatorio da ROTINA PRINCIPAL (partidas)
--
-- Relatorio com parametros (campeonato e periodo) e feito com PROCEDURE:
-- o Java chama  {call sp_relatorio_partidas_periodo(?, ?, ?)}  e le o
-- resultado como uma consulta comum.
--
-- Executar em: USE campeonato;  (depois de Champs_schema e Champs_procedures_triggers)
-- =====================================================================

USE campeonato;

DROP PROCEDURE IF EXISTS sp_relatorio_partidas_periodo;

DELIMITER $$

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
