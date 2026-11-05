#!/bin/bash
# =====================================================================
# testar.sh  |  Sobe um MySQL 8.4 descartavel (Docker), instala o banco,
#               carrega Champs_dados.sql e confere dados, classificacao,
#               relatorios e regras de negocio.
#
# Uso: bash SQL_Champs/testar.sh      (precisa do Docker aberto)
#
# O antigo 99_testes.sql foi substituido por Champs_dados.sql, que tem
# mais casos distintos (4 campeonatos, 28 partidas, status variados).
# =====================================================================

cd "$(dirname "$0")"
CONTAINER=poo-mysql-teste
FALHAS=0

# 1) Sobe um MySQL 8.4 novo; --rm apaga tudo quando ele parar
docker rm -f "$CONTAINER" > /dev/null 2>&1 || true
docker run -d --rm --name "$CONTAINER" -e MYSQL_ROOT_PASSWORD=teste mysql:8.4 > /dev/null || exit 1
trap 'docker stop "$CONTAINER" > /dev/null 2>&1' EXIT

# Executa o mysql dentro do container (127.0.0.1 = so responde quando o servidor esta pronto)
mysql() { docker exec -i -e MYSQL_PWD=teste "$CONTAINER" mysql -h 127.0.0.1 -uroot "$@"; }

echo "Aguardando o MySQL iniciar..."
until mysql -e "SELECT 1" &> /dev/null; do sleep 2; done

# 2) Instala o banco e carrega os dados de demonstracao/teste
for arquivo in Champs_schema.sql Champs_procedures_triggers.sql Champs_relatorios.sql Champs_dados.sql; do
    if [ ! -f "$arquivo" ]; then
        echo ">> Arquivo nao encontrado: $arquivo"
        exit 1
    fi
    echo "Executando $arquivo"
    if ! mysql < "$arquivo"; then
        echo ">> Falha ao executar $arquivo"
        exit 1
    fi
done

# 3) Helpers de verificacao
consultar() { mysql -N -B campeonato -e "$1"; }

verificar() {
    local descricao="$1"
    local esperado="$2"
    local obtido="$3"
    if [ "$esperado" = "$obtido" ]; then
        echo "OK    $descricao"
    else
        echo "FALHOU $descricao"
        echo "       esperado: [$esperado]"
        echo "       obtido:   [$obtido]"
        FALHAS=$((FALHAS + 1))
    fi
}

verificar_erro() {
    local descricao="$1"
    local mensagem="$2"
    local comando="$3"
    local saida
    saida=$(mysql --force campeonato -e "$comando" 2>&1 || true)
    if echo "$saida" | grep -qF "$mensagem"; then
        echo "OK    $descricao"
    else
        echo "FALHOU $descricao (mensagem esperada: $mensagem)"
        echo "$saida" | sed 's/^/       /'
        FALHAS=$((FALHAS + 1))
    fi
}

# 4) Contagens do Champs_dados.sql
verificar "4 campeonatos, 10 equipes, 5 estadios, 5 arbitros, 21 jogadores, 28 partidas e 20 inscricoes" \
    $'4\t10\t5\t5\t21\t28\t20' \
    "$(consultar "SELECT (SELECT COUNT(*) FROM CAMPEONATO),
                         (SELECT COUNT(*) FROM EQUIPE),
                         (SELECT COUNT(*) FROM ESTADIO),
                         (SELECT COUNT(*) FROM ARBITRO),
                         (SELECT COUNT(*) FROM JOGADOR),
                         (SELECT COUNT(*) FROM PARTIDA),
                         (SELECT COUNT(*) FROM CLASSIFICACAO);")"

# 5) Classificacao calculada pelos triggers
verificar "Tigres FC: 6 pontos e 1o lugar na Copa Regional" \
    $'6\t1' \
    "$(consultar "SELECT pontuacao, colocacao_equipe
                    FROM CLASSIFICACAO
                   WHERE id_campeonato = 1 AND id_equipe = 1;")"

verificar "Copa Regional tem 11 partidas encerradas" \
    "11" \
    "$(consultar "SELECT COUNT(*) FROM PARTIDA
                   WHERE id_campeonato = 1 AND status = 'ENCERRADA';")"

# 6) Relatorios (procedures)
verificar "relatorio de classificacao devolve 10 equipes" \
    "10" \
    "$(consultar "CALL sp_relatorio_classificacao(1);" | wc -l | tr -d ' ')"

verificar "relatorio de setembro/2026 devolve 10 partidas" \
    "10" \
    "$(consultar "CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');" | wc -l | tr -d ' ')"

# 7) Regras de negocio (devem falhar)
verificar_erro "recusa partida com data fora do periodo" \
    "Data da partida fora do periodo do campeonato" \
    "INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante,
                          id_estadio, id_arbitro, data_partida)
     VALUES (1, 9, 1, 2, 1, 1, '2027-01-10 16:00');"

verificar_erro "recusa equipe jogando duas vezes na rodada" \
    "Equipe ja possui partida nesta rodada" \
    "INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante,
                          id_estadio, id_arbitro, data_partida)
     VALUES (1, 1, 1, 2, 1, 1, '2026-09-05 20:00');"

verificar_erro "recusa desinscrever equipe que ja jogou" \
    "Equipe possui partidas neste campeonato" \
    "DELETE FROM CLASSIFICACAO WHERE id_campeonato = 1 AND id_equipe = 1;"

verificar_erro "recusa camisa repetida na mesma equipe" \
    "uq_jogador_camisa" \
    "INSERT INTO JOGADOR (nome, posicao, numero_camisa, id_equipe)
     VALUES ('Jogador de Teste', 'MEIA', 1, 1);"

# 8) Resultado final
if [ "$FALHAS" -eq 0 ]; then
    echo "TODOS OS TESTES PASSARAM"
else
    echo ">> $FALHAS teste(s) falharam"
    exit 1
fi
