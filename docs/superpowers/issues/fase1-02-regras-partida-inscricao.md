**Título:** [Banco] Etapa 2 — Regras da partida e da inscrição

## Problema
O banco aceita: equipe 2× na mesma rodada, árbitro/estádio em dois jogos no mesmo horário, partida fora do período, equipe não inscrita jogando, `tempo_partida` em jogo encerrado e desinscrição de equipe que já jogou.

## Escopo
- `sp_validar_partida` + triggers `BEFORE INSERT/UPDATE` em PARTIDA, com as mensagens:
  1. `Data da partida fora do periodo do campeonato`
  2. `Equipe nao inscrita neste campeonato`
  3. `Equipe ja possui partida nesta rodada`
  4. `Arbitro ou estadio ja ocupado neste horario`
- `CHECK chk_partida_tempo`
- Classificação deixa de inscrever equipes sozinha; trigger bloqueia desinscrição com a mensagem `Equipe possui partidas neste campeonato`

## Pronto quando
- [ ] Cada regra tem um teste de erro esperado no `99_testes.sql`
- [ ] Lançar placar de jogo agendado funciona (Panteras soma 3 pts)
- [ ] Jogo no último dia do campeonato é aceito
- [ ] Jogo cancelado libera o horário para remarcação
- [ ] Inscrever e desinscrever equipe sem jogos funciona
- [ ] `bash sql/testar.sh` → `TODOS OS TESTES PASSARAM`
- [ ] Guia: seção "2. Regras automáticas"

Plano: `docs/superpowers/plans/2026-10-30-fase1-banco-de-dados.md` → Etapa 2
