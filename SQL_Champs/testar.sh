#!/bin/bash
# =====================================================================
# testar.sh  |  Roda os scripts num MySQL descartavel (Docker) e confere
#               o resultado do 99_testes.sql.
# Uso: bash SQL_Champs/testar.sh      (precisa do Docker aberto)
# Sem Docker: rode os 3 scripts Champs_*.sql e o 99 no Workbench e confira os 'OK'.
# =====================================================================

cd "$(dirname "$0")"
CONTAINER=poo-mysql-teste

# 1) Sobe um MySQL 8.4 novo; --rm apaga tudo quando ele parar
docker run -d --rm --name $CONTAINER -e MYSQL_ROOT_PASSWORD=teste mysql:8.4 > /dev/null || exit 1
trap 'docker stop $CONTAINER > /dev/null' EXIT

# Executa o mysql dentro do container (127.0.0.1 = so responde quando o servidor esta pronto)
mysql() { docker exec -i -e MYSQL_PWD=teste $CONTAINER mysql -h 127.0.0.1 -uroot "$@"; }

echo "Aguardando o MySQL iniciar..."
until mysql -e "SELECT 1" &> /dev/null; do sleep 2; done

# 2) Instala o banco; qualquer erro aqui interrompe o teste
for arquivo in Champs_schema.sql Champs_procedures_triggers.sql Champs_relatorios.sql; do
    [ -f $arquivo ] || continue
    echo "Executando $arquivo"
    mysql < $arquivo || exit 1
done

# 3) Roda os testes (--force: continua apos os erros esperados)
saida=$(mysql --force -t campeonato < 99_testes.sql 2>&1)
echo "$saida"

# 4) Confere: nenhum FALHOU, todo erro esperado aconteceu e nenhum erro a mais
problemas=0
if echo "$saida" | grep -q "FALHOU"; then
    echo ">> Ha testes com FALHOU"; problemas=1
fi
esperados=$(grep "^-- ESPERADO ERRO: " 99_testes.sql | sed "s/^-- ESPERADO ERRO: //")
while read -r mensagem; do
    [ -z "$mensagem" ] && continue
    echo "$saida" | grep -qF "$mensagem" || { echo ">> Erro esperado nao aconteceu: $mensagem"; problemas=1; }
done <<< "$esperados"
qtd_esperados=$(echo "$esperados" | grep -c .)
qtd_erros=$(echo "$saida" | grep -c "^ERROR")
if [ "$qtd_erros" -ne "$qtd_esperados" ]; then
    echo ">> Erros na saida: $qtd_erros, esperados: $qtd_esperados"; problemas=1
fi

if [ $problemas -eq 0 ]; then
    echo "TODOS OS TESTES PASSARAM"
else
    exit 1
fi
