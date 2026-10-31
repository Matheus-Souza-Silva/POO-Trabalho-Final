-- =====================================================================
-- Champs_procedures_triggers.sql  |  Regras automaticas do banco
--
-- Fluxo ao salvar uma PARTIDA (INSERT/UPDATE):
--   1) trigger BEFORE chama sp_validar_partida  -> recusa se quebrar regra
--   2) o MySQL grava a partida
--   3) trigger AFTER chama sp_sincronizar_classificacao(campeonato)
--        -> recalcula jogos, V/E/D, gols, pontos e colocacao
--
-- Regras da partida (sp_validar_partida):
--   1. data dentro do periodo do campeonato
--   2. mandante e visitante inscritos no campeonato
--   3. equipe nao joga duas vezes na mesma rodada
--   4. arbitro e estadio nao ficam em dois jogos no mesmo horario
--   (partidas CANCELADAS nao contam)
--
-- Regras de negocio aplicadas (futebol):
--   vitoria = 3 pontos | empate = 1 ponto | derrota = 0
--   saldo de gols = gols marcados - gols sofridos   (coluna gerada)
--   desempate: pontos > vitorias > saldo > gols marcados > id_equipe
--
-- Executar em: USE campeonato;  (depois de Champs_schema.sql)
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
    -- So equipes INSCRITAS tem linha aqui (sp_validar_partida garante
    -- que nenhuma equipe nao inscrita entra em campo).

    -- 1) Zera os acumuladores do campeonato (quem nao jogou fica com 0)
    UPDATE CLASSIFICACAO
       SET partidas_jogadas = 0,
           vitorias         = 0,
           empates          = 0,
           derrotas         = 0,
           gols_marcados    = 0,
           gols_sofridos    = 0,
           colocacao_equipe = NULL
     WHERE id_campeonato = p_id_campeonato;

    -- 2) Soma as partidas ENCERRADAS, uma linha por equipe (casa + fora)
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

    -- 3) Colocacao (1o, 2o, ...) com criterios de desempate.
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
-- Valida uma partida ANTES de ela ser gravada (chamada pelos triggers
-- BEFORE INSERT e BEFORE UPDATE). Se uma regra for quebrada, o SIGNAL
-- cancela a gravacao e devolve a mensagem para quem tentou salvar (o Java).
-- p_id_partida serve para a partida nao conflitar com ela mesma ao ser
-- alterada (no INSERT ela ainda nao existe na tabela).
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_validar_partida$$

CREATE PROCEDURE sp_validar_partida(
    IN p_id_partida    INT,
    IN p_id_campeonato INT,
    IN p_rodada        INT,
    IN p_id_mandante   INT,
    IN p_id_visitante  INT,
    IN p_id_estadio    INT,
    IN p_id_arbitro    INT,
    IN p_data          DATETIME,
    IN p_status        VARCHAR(20)
)
BEGIN
    -- Partida cancelada nao ocupa equipe, arbitro nem estadio: nao valida
    IF p_status <> 'CANCELADA' THEN

        -- Regra 1: a data precisa estar dentro do periodo do campeonato
        IF NOT EXISTS (SELECT 1 FROM CAMPEONATO
                        WHERE id_campeonato = p_id_campeonato
                          AND DATE(p_data) BETWEEN data_inicio AND data_fim) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Data da partida fora do periodo do campeonato';
        END IF;

        -- Regra 2: as duas equipes precisam estar inscritas no campeonato
        IF NOT EXISTS (SELECT 1 FROM CLASSIFICACAO
                        WHERE id_campeonato = p_id_campeonato AND id_equipe = p_id_mandante)
           OR NOT EXISTS (SELECT 1 FROM CLASSIFICACAO
                           WHERE id_campeonato = p_id_campeonato AND id_equipe = p_id_visitante) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Equipe nao inscrita neste campeonato';
        END IF;

        -- Regra 3: nenhuma das duas equipes pode ter outro jogo na mesma rodada
        IF EXISTS (SELECT 1 FROM PARTIDA
                    WHERE id_campeonato = p_id_campeonato
                      AND rodada = p_rodada
                      AND id_partida <> p_id_partida
                      AND status <> 'CANCELADA'
                      AND (id_mandante  IN (p_id_mandante, p_id_visitante)
                        OR id_visitante IN (p_id_mandante, p_id_visitante))) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Equipe ja possui partida nesta rodada';
        END IF;

        -- Regra 4: arbitro e estadio nao podem estar em dois jogos no mesmo
        -- horario (vale para qualquer campeonato)
        IF EXISTS (SELECT 1 FROM PARTIDA
                    WHERE data_partida = p_data
                      AND id_partida <> p_id_partida
                      AND status <> 'CANCELADA'
                      AND (id_arbitro = p_id_arbitro OR id_estadio = p_id_estadio)) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Arbitro ou estadio ja ocupado neste horario';
        END IF;

    END IF;
END$$

-- ---------------------------------------------------------------------
-- TRIGGERS BEFORE: validam a partida antes de gravar.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_partida_before_insert$$

CREATE TRIGGER trg_partida_before_insert
BEFORE INSERT ON PARTIDA
FOR EACH ROW
BEGIN
    CALL sp_validar_partida(NEW.id_partida, NEW.id_campeonato, NEW.rodada,
                            NEW.id_mandante, NEW.id_visitante, NEW.id_estadio,
                            NEW.id_arbitro, NEW.data_partida, NEW.status);
END$$

DROP TRIGGER IF EXISTS trg_partida_before_update$$

CREATE TRIGGER trg_partida_before_update
BEFORE UPDATE ON PARTIDA
FOR EACH ROW
BEGIN
    CALL sp_validar_partida(NEW.id_partida, NEW.id_campeonato, NEW.rodada,
                            NEW.id_mandante, NEW.id_visitante, NEW.id_estadio,
                            NEW.id_arbitro, NEW.data_partida, NEW.status);
END$$

-- ---------------------------------------------------------------------
-- TRIGGERS AFTER: qualquer mudanca em PARTIDA mantem a classificacao coerente,
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

-- ---------------------------------------------------------------------
-- Desinscricao: equipe que ja tem partida no campeonato nao pode sair,
-- senao a partida ficaria com uma equipe "fora" da classificacao.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_classificacao_before_delete$$

CREATE TRIGGER trg_classificacao_before_delete
BEFORE DELETE ON CLASSIFICACAO
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM PARTIDA
                WHERE id_campeonato = OLD.id_campeonato
                  AND (id_mandante = OLD.id_equipe OR id_visitante = OLD.id_equipe)) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Equipe possui partidas neste campeonato';
    END IF;
END$$

DELIMITER ;
