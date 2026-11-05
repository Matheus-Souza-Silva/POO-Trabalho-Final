# Sistema Campeonato (Java + MVC + MySQL) — Roteiro Geral

> **Documento histórico de planejamento (out/2026).** A implementação final está descrita nos guias [`00`](../../guia/00-guia-de-apresentacao.md)–[`04`](../../guia/04-perguntas-e-respostas.md). Alguns nomes deste plano (`ConexaoBD`, `*Controller`, arquivos `02-java-mvc-e-conexao.md`, `03-cruds-simples.md` etc.) **não existem** no código final; use os guias atuais como referência.

**Spec:** enunciado do trabalho (`POO Trabalho em Grupo 3f83d05adda5803f9eb5f7d682b56de3.md`, entrega 06/11/2026).
**Plano detalhado da Fase 1:** [`2026-10-30-fase1-banco-de-dados.md`](2026-10-30-fase1-banco-de-dados.md)

## Princípio número 1: simples e explicável

Metade da nota é a apresentação individual: cada integrante abre o código, explica e responde a 2 perguntas sobre **qualquer parte** do projeto. Por isso:

- **Só o que o enunciado exige** ou o que protege a rotina principal. Nada “a mais”.
- **Um único formato repetido:** cada entidade tem as mesmas 4 peças (Model, DAO, Controller, Tela). Quem entende uma, entende todas.
- **Java básico:** classes, atributos `private` com get/set, construtores, `ArrayList`, `PreparedStatement`, `try/catch`. Sem generics próprios, lambdas elaboradas, streams, reflection, frameworks ou bibliotecas além do conector MySQL.
- **Status e categorias como `String`**, com valores fixos no `JComboBox` (iguais aos ENUMs do banco).
- **Comentários curtos em português** explicando o porquê.
- **Cada fase termina com seu trecho do guia de estudo** (`docs/guia/`).

## Decisões do time

- **Rotina principal:** PARTIDA (registrar jogo e resultado). **CRUDs simples:** CAMPEONATO, EQUIPE, ESTADIO, ARBITRO (JOGADOR fica fora do Java, a não ser que sobre tempo).
- **Banco = estrutura + regras.** Os dados são cadastrados pelo sistema Java; não há script de carga. Dados de teste existem só no `99_testes.sql`.
- **Sem views.** JOIN na procedure do relatório e nas consultas do Java (`PartidaDAO.listar()`, `ClassificacaoDAO.listar()`).
- **Commits e PRs manuais** pelo time, após revisão de cada etapa.

## Estrutura `sql/`

```
Champs_schema.sql              tabelas, PK, FK, CHECK
Champs_procedures_triggers.sql classificacao automatica + regras da partida e da inscricao
Champs_relatorios.sql          relatorio principal (partidas por periodo) com INNER JOIN
99_testes.sql              roteiro de testes em SQL puro (roda no Workbench)
testar.sh                  roda tudo num MySQL descartavel no Docker
```

Instalação no MySQL local (Workbench): Champs_schema → Champs_procedures_triggers → Champs_relatorios.

## Relatório

O enunciado não exige PDF; exige que o **sistema** tenha um relatório da rotina principal (seção 6, 0,15 da nota). Então: tela `TelaRelatorio` com filtros (campeonato, data inicial, data final), resultado em `JTable`, totais no rodapé (partidas, encerradas, canceladas, gols) e botão **Imprimir** com `JTable.print()` — nativo do Swing; a janela de impressão já permite “Salvar como PDF”.

## Guia de estudo (`docs/guia/`)

| Arquivo | Conteúdo | Criado/atualizado em |
|---|---|---|
| `00-guia-de-apresentacao.md` | visão global: requisitos → onde estão, MVC, roteiro da demo, **banco de perguntas de todos os temas da seção 11** | Fase 1 (criação), toda fase (atualiza) |
| `01-banco-de-dados.md` | tabelas, relacionamentos, regras, relatório, roteiro de leitura dos scripts, perguntas | Fase 1 |
| `02-java-mvc-e-conexao.md` | pacotes, `ConexaoBD`, JDBC, como uma tela fala com o banco | Fase 2 |
| `03-cruds-simples.md` | anatomia de um CRUD (Model/DAO/Controller/Tela) usando Árbitro como exemplo | Fase 3 |
| `04-crud-principal.md` | TelaPartida, combos, JOIN no `PartidaDAO`, regras do banco aparecendo na tela | Fase 4 |
| `05-relatorio-e-demonstracao.md` | TelaRelatorio, `CallableStatement`, roteiro final da apresentação | Fase 5 |

## Diagnóstico da varredura (estado em `df1de36`)

| # | Problema | Fase |
|---|----------|------|
| 1 | `CLASSIFICACAO` não é criada (`ERROR 3820`: coluna `colocacao` × `colocacao_equipe`) e, por isso, nenhuma partida pode ser inserida | 1 |
| 2 | `df1de36` apagou o `relatorios.sql` | 1 |
| 3 | Nomes dos arquivos não batem com os cabeçalhos (`Champs_schema.sql`…) | 1 |
| 4 | Banco aceita: equipe 2× na rodada, árbitro/estádio em dois jogos no mesmo horário, partida fora do período, equipe não inscrita, `tempo_partida` em jogo encerrado | 1 |
| 5 | Java fora do MVC: `Arbitro` em `controle`, `DAO` em `entidade`, driver antigo `com.mysql.jdbc.Driver`, conexão nunca fechada, jar do conector com caminho do Windows, `main.class` vazio | 2 |

## Fases

| Fase | Entrega | Critério de pronto |
|------|---------|--------------------|
| **1. Banco** | plano da Fase 1 (issues #1–#4) | `bash sql/testar.sh` → `TODOS OS TESTES PASSARAM`; guias `00` e `01` |
| **2. Base Java** | pacotes MVC, `Arbitro` → `modelo` (+ `id`), remover `entidade/`, `ConexaoBD` com `com.mysql.cj.jdbc.Driver`, conector em `lib/` (caminho relativo), `main.class` = `TelaPrincipal` com menu | app abre o menu e conecta; erros do banco aparecem em `JOptionPane` |
| **3. 4 CRUDs simples** | Campeonato, Equipe, Estádio, Árbitro — mesmo layout: formulário + `JTable` + Novo/Salvar/Excluir/Limpar (clicar na linha carrega para alterar) | os 4 fazem C, R, U e D |
| **4. CRUD principal** | TelaPartida (combos, data/hora, placar, status), listagem com `INNER JOIN`; TelaClassificacao (inscrever/desinscrever + classificação com JOIN) | mensagens das regras do banco aparecem na tela |
| **5. Relatório + demonstração** | TelaRelatorio; cadastro dos dados de demonstração **pelo sistema** + `mysqldump` de segurança; guia final | fluxo Cadastro → Inscrição → Partida → JOIN → Relatório demonstrável |

Pacotes Java (`Projeto Final - Campeonato/src/br/com/`):

```
modelo/      Campeonato, Equipe, Estadio, Arbitro, Partida, Classificacao   (dados: atributos + get/set)
modelo/dao/  ConexaoBD + CampeonatoDAO, EquipeDAO, EstadioDAO, ArbitroDAO,
             PartidaDAO, ClassificacaoDAO, RelatorioDAO                     (SQL com PreparedStatement)
controle/    CampeonatoController, EquipeController, EstadioController,
             ArbitroController, PartidaController, RelatorioController      (valida e chama o DAO)
visao/       TelaPrincipal, TelaCampeonato, TelaEquipe, TelaEstadio, TelaArbitro,
             TelaPartida, TelaClassificacao, TelaRelatorio                  (JFrame: só chama o Controller)
```

POO que aparece naturalmente (e que o guia aponta no código): **classe/objeto** (cada `new Equipe()`), **encapsulamento** (atributos `private` + get/set), **herança** (todas as telas `extends JFrame`), **construtores** e **sobrescrita** (`toString()` das entidades para os `JComboBox`), **associação** (`Partida` tem `Equipe` mandante e visitante).

**Risco (seção 9 — dados de demonstração):** sem script de carga, os dados só existem no MySQL de quem cadastrou. A Fase 5 prevê o cadastro pelo sistema e um `mysqldump` de segurança.
