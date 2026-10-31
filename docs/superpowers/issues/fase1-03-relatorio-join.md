**Título:** [Banco] Etapa 3 — Relatório principal com INNER JOIN

## Problema
Seções 5 (JOIN) e 6 (relatório da rotina principal) do enunciado. O `relatorios.sql` foi apagado em `df1de36`.

## Escopo
- `sql/Champs_relatorios.sql` com `sp_relatorio_partidas_periodo(campeonato, data_inicio, data_fim)`: PARTIDA + EQUIPE (mandante e visitante) + ESTADIO + ARBITRO, mostrando nomes no lugar de códigos

## Pronto quando
- [ ] Setembro/2026 → 5 partidas; 1ª linha `1 | 05/09/2026 16:00 | Tigres FC | Aguas EC | Arena Central | Carlos Andrade | 2 x 1 | ENCERRADA`
- [ ] Período de um dia (05/09 a 05/09) → 2 partidas
- [ ] `bash sql/testar.sh` → `TODOS OS TESTES PASSARAM`
- [ ] Guia: seção "3. Relatório e JOIN"

Plano: `docs/superpowers/plans/2026-10-30-fase1-banco-de-dados.md` → Etapa 3
