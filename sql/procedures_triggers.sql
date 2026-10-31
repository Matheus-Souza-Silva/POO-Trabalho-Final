-- =====================================================================
-- 03_procedures_triggers.sql  |  Automacao do calculo da classificacao
--
-- Fluxo:
--   INSERT/UPDATE/DELETE em PARTIDA
--        -> trigger chama sp_sincronizar_classificacao(campeonato)
--        -> procedure recalcula jogos, V/E/D, gols, pontos e colocacao
--
-- Regras de negocio aplicadas (futebol):
--   vitoria = 3 pontos | empate = 1 ponto | derrota = 0
--   saldo de gols = gols marcados - gols sofridos   (coluna gerada)
--   desempate: pontos > vitorias > saldo > gols marcados > id_equipe
--
-- Executar em: USE campeonato;  (depois de 01 e 02)
-- ATENCAO: DELIMITER e comando do cliente (Workbench/CLI), nao do MySQL.
-- =====================================================================

USE campeonato;

DROP PROCEDURE IF EXISTS sp_sincronizar_classificacao;

DELIMITER $$

-- ---------------------------------------------------------------------
-- Recalcula a classificacao INTEIRA de um campeonato a partir de PARTIDA.
-- Estrategia "recalcular do zero": simples de explicar e sempre correta
-- (nao tem risco de acumular erros de soma/subtracao).
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_sincronizar_classificacao(IN p_id_campeonato INT)
BEGIN
    -- 1) Garante linha de classificacao para toda equipe que entrou em campo
    INSERT IGNORE INTO CLASSIFICACAO (id_campeonato, id_equipe)
    SELECT id_campeonato, id_mandante  FROM PARTIDA WHERE id_campeonato = p_id_campeonato
    UNION
    SELECT id_campeonato, id_visitante FROM PARTIDA WHERE id_campeonato = p_id_campeonato;

    -- 2) Zera os acumuladores do campeonato (quem nao jogou fica com 0)
    UPDATE CLASSIFICACAO
       SET partidas_jogadas = 0,
           vitorias         = 0,
           empates          = 0,
           derrotas         = 0,
           gols_marcados    = 0,
           gols_sofridos    = 0,
           colocacao_equipe = NULL
     WHERE id_campeonato = p_id_campeonato;

    -- 3) Soma as partidas ENCERRADAS, uma linha por equipe (casa + fora)
    UPDATE CLASSIFICACAO c
      JOIN (
            SELECT id_equipe,
                   COUNT(*) AS jogos,
                   SUM(v)   AS vitorias,
                   SUM(e)   AS empates,
                   SUM(d)   AS derrotas,
                   SUM(gp)  AS gols_marcados,
                   SUM(gc)  AS gols_sofridos
              FROM (
                    -- equipe como MANDANTE
                    SELECT id_mandante AS id_equipe,
                           (gols_mandante > gols_visitante) AS v,
                           (gols_mandante = gols_visitante) AS e,
                           (gols_mandante < gols_visitante) AS d,
                           gols_mandante AS gp,
                           gols_visitante AS gc
                      FROM PARTIDA
                     WHERE id_campeonato = p_id_campeonato
                       AND status = 'ENCERRADA'
                    UNION ALL
                    -- equipe como VISITANTE (gols invertidos)
                    SELECT id_visitante,
                           (gols_visitante > gols_mandante),
                           (gols_visitante = gols_mandante),
                           (gols_visitante < gols_mandante),
                           gols_visitante,
                           gols_mandante
                      FROM PARTIDA
                     WHERE id_campeonato = p_id_campeonato
                       AND status = 'ENCERRADA'
                   ) jogos
          GROUP BY id_equipe
           ) s ON s.id_equipe = c.id_equipe
     SET c.partidas_jogadas = s.jogos,
         c.vitorias         = s.vitorias,
         c.empates          = s.empates,
         c.derrotas         = s.derrotas,
         c.gols_marcados    = s.gols_marcados,
         c.gols_sofridos    = s.gols_sofridos
   WHERE c.id_campeonato = p_id_campeonato;

    -- 4) Colocacao (1o, 2o, ...) com criterios de desempate.
    --    ROW_NUMBER() = funcao de janela (MySQL 8+). Quem nao jogou fica NULL.
    UPDATE CLASSIFICACAO c
      JOIN (
            SELECT id_equipe,
                   ROW_NUMBER() OVER (
                       ORDER BY pontuacao      DESC,
                                vitorias       DESC,
                                saldo_gols     DESC,
                                gols_marcados  DESC,
                                id_equipe
                   ) AS posicao
              FROM CLASSIFICACAO
             WHERE id_campeonato = p_id_campeonato
               AND partidas_jogadas > 0
           ) r ON r.id_equipe = c.id_equipe
     SET c.colocacao_equipe = r.posicao
   WHERE c.id_campeonato = p_id_campeonato;
END$$

-- ---------------------------------------------------------------------
-- Inscreve uma equipe no campeonato e ja atualiza a classificacao.
-- Usada pelo CRUD de "inscricao" / tela de classificacao.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_inscrever_equipe$$

CREATE PROCEDURE sp_inscrever_equipe(IN p_id_campeonato INT, IN p_id_equipe INT)
BEGIN
    INSERT IGNORE INTO CLASSIFICACAO (id_campeonato, id_equipe)
    VALUES (p_id_campeonato, p_id_equipe);

    CALL sp_sincronizar_classificacao(p_id_campeonato);
END$$

-- ---------------------------------------------------------------------
-- TRIGGERS: qualquer mudanca em PARTIDA mantem a classificacao coerente,
-- mesmo que a alteracao seja feita direto no Workbench (sem passar pelo Java).
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_partida_after_insert$$

CREATE TRIGGER trg_partida_after_insert
AFTER INSERT ON PARTIDA
FOR EACH ROW
BEGIN
    CALL sp_sincronizar_classificacao(NEW.id_campeonato);
END$$

DROP TRIGGER IF EXISTS trg_partida_after_update$$

CREATE TRIGGER trg_partida_after_update
AFTER UPDATE ON PARTIDA
FOR EACH ROW
BEGIN
    CALL sp_sincronizar_classificacao(OLD.id_campeonato);

    IF NEW.id_campeonato <> OLD.id_campeonato THEN
        CALL sp_sincronizar_classificacao(NEW.id_campeonato);
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_partida_after_delete$$

CREATE TRIGGER trg_partida_after_delete
AFTER DELETE ON PARTIDA
FOR EACH ROW
BEGIN
    CALL sp_sincronizar_classificacao(OLD.id_campeonato);
END$$

DELIMITER ;
