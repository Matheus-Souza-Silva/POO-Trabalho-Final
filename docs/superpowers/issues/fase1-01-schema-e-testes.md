**Título:** [Banco] Etapa 1 — Banco cria sem erros + roteiro de testes

## Problema
`CLASSIFICACAO` não é criada (`ERROR 3820`: o CHECK usa `colocacao_equipe`, mas a coluna se chama `colocacao`) e, por isso, nenhuma partida pode ser inserida.

## Escopo
- Renomear para `Champs_schema.sql` e `Champs_procedures_triggers.sql`
- Padronizar a coluna como `colocacao_equipe`
- `sql/99_testes.sql` (SQL puro, roda no Workbench) e `sql/testar.sh` (MySQL descartável no Docker)
- README: como instalar (Champs_schema → Champs_procedures_triggers → Champs_relatorios) e testar

## Pronto quando
- [ ] `bash sql/testar.sh` → `TODOS OS TESTES PASSARAM`
- [ ] Classificação calculada: Tigres 1º (6 pts, saldo 2), Tubarões 2º (4 pts), Panteras 6º (0 pts)
- [ ] Excluir campeonato com partidas é bloqueado
- [ ] Guia `docs/guia/01-banco-de-dados.md` com a seção "1. Estrutura do banco"

Plano: `docs/superpowers/plans/2026-10-30-fase1-banco-de-dados.md` → Etapa 1
