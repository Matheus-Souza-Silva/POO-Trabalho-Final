# Guia de Apresentação e Visão Geral do Champs

> **Objetivo deste guia:** dar o mapa completo do projeto para que qualquer integrante consiga abrir o código, apontar uma classe, explicar o fluxo e responder perguntas do professor sobre **qualquer parte** do sistema.
>
> Os outros guias aprofundam cada camada:
>
> | Guia | Conteúdo |
> |---|---|
> | **este arquivo** | visão geral, arquitetura, mapa dos arquivos, fluxos de ponta a ponta, roteiro de demonstração e limitações |
> | [`01-banco-de-dados.md`](01-banco-de-dados.md) | MySQL: cada tabela, cada constraint, cada procedure/trigger e cada relatório |
> | [`02-java-e-relatorios.md`](02-java-e-relatorios.md) | Java: cada classe, cada método, JDBC, Swing, validações e relatórios |
> | [`03-mapa-do-codigo.md`](03-mapa-do-codigo.md) | índice “onde está o quê”: arquivo, faixa de linhas, método e assunto |
> | [`04-perguntas-e-respostas.md`](04-perguntas-e-respostas.md) | simulado com perguntas prováveis, pegadinhas e respostas curtas |

---

## 1. O sistema em um minuto

O **Champs** é um sistema desktop em **Java Swing** para **organizar campeonatos de futebol**. O organizador cadastra campeonatos, equipes, jogadores, estádios e árbitros; inscreve equipes em um campeonato; agenda partidas; lança resultados; e consulta a classificação e relatórios.

O banco **MySQL** guarda os dados e também concentra as regras mais importantes:

- a **classificação é recalculada automaticamente** sempre que uma partida é inserida, alterada ou excluída;
- os **triggers** recusam partidas com data fora do campeonato, equipe não inscrita, equipe jogando duas vezes na rodada e árbitro/estádio ocupado;
- as **procedures** dos relatórios fazem os JOINs e devolvem os dados já formatados para o Java.

O fluxo resumido é:

```text
Usuário preenche a tela
        ↓
TelaPrincipal valida os campos e monta um objeto
        ↓
Classe Manter... (DAO) monta o SQL com PreparedStatement
        ↓
MySQL grava/consulta, dispara triggers e devolve ResultSet ou erro
        ↓
DAO monta objetos Java
        ↓
Tela atualiza a JTable ou mostra JOptionPane
```

**Frase para decorar:** *a tela não escreve SQL; o DAO não conhece a tela; o banco é a última barreira das regras.*

---

## 2. O que existe no repositório

### 2.1 Árvore comentada

```text
POO-Trabalho-Final/
├── README.md                         # apresentação do projeto, funcionalidades e como executar
├── Organizacao_Projeto_POO.docx      # documento de organização do trabalho (não é código)
├── docs/
│   ├── guia/                         # ESTES guias de estudo
│   └── superpowers/                  # planos e issues do desenvolvimento (histórico)
├── SQL_Champs/
│   ├── Champs_schema.sql             # banco, 7 tabelas, PKs, FKs, CHECKs, UNIQUEs, índices
│   ├── Champs_procedures_triggers.sql# validação da partida, sincronização e triggers
│   ├── Champs_relatorios.sql         # procedures dos dois relatórios
│   ├── Champs_dados.sql              # dados de demonstração/teste (4 campeonatos, 28 partidas...)
│   └── testar.sh                     # sobe MySQL no Docker e tenta rodar os testes
└── Java_Champs/
    ├── src/br/com/controle/          # MODEL: classes de dados e linhas de relatório
    ├── src/br/com/entidade/          # DAO: conexão + SQL de cada entidade
    ├── src/br/com/visao/             # VIEW: TelaPrincipal (Swing)
    ├── nbproject/                    # configuração do projeto NetBeans
    └── build/                        # artefatos compilados (ignorados pelo Git)
```

### 2.2 O que é fonte e o que é artefato

- **Fonte (é o que importa):** `Java_Champs/src/**/*.java` e `SQL_Champs/*.sql`.
- **Configuração:** `Java_Champs/nbproject/project.properties` define `main.class=br.com.visao.TelaPrincipal`, codificação UTF-8, JDK 26 e o jar do Connector/J.
- **Artefato:** `Java_Champs/build/classes/*.class` é resultado de compilação; pode estar desatualizado. Nunca estude pelo `.class`.
- **`TelaPrincipal.form`:** existe, mas é apenas um **stub mínimo** do NetBeans (só a propriedade de fechar a janela). A tela real é construída em código Java; o `.form` não descreve os componentes. Isso é importante para não se perder: abra o `.java`.

### 2.3 Tamanhos aproximados (para ter noção do esforço)

| Arquivo | Linhas | Papel |
|---|---:|---|
| `TelaPrincipal.java` | ~2.073 | toda a interface, validações, eventos, cache e chamadas aos DAOs |
| `ManterPartida.java` | 191 | DAO do CRUD principal, com JOIN e tratamento de NULL |
| `ManterJogador.java` | 150 | DAO do CRUD extra, com JOIN em EQUIPE e data opcional |
| `ManterRelatorio.java` | 132 | chamadas às procedures com `CallableStatement` |
| `ManterClassificacao.java` | 122 | inscrição, status, consulta e listagem da classificação |
| `ManterCampeonato.java` | 117 | CRUD simples de campeonato |
| `ManterArbitro.java` | 114 | CRUD simples de árbitro |
| `ManterEstadio.java` | 112 | CRUD simples de estádio |
| `ManterEquipe.java` | 110 | CRUD simples de equipe |
| `DAO.java` | 39 | conexão JDBC e fechamento |
| `Champs_schema.sql` | 191 | estrutura do banco |
| `Champs_procedures_triggers.sql` | 289 | regras automáticas |
| `Champs_relatorios.sql` | 118 | relatórios |
| `Champs_dados.sql` | 183 | dados de demonstração |

---

## 3. Arquitetura em quatro camadas

### 3.1 A divisão real do projeto

```text
┌──────────────────────────────────────────────────────────────┐
│ VIEW — br.com.visao.TelaPrincipal                            │
│ JFrame, JTabbedPane, JTable, JTextField, JComboBox, JButton  │
│ Responsabilidades: montar a tela, validar campos, disparar   │
│ ações, mostrar mensagens e recarregar tabelas.               │
└───────────────────────────┬──────────────────────────────────┘
                            │ chama métodos como inserir(), alterar(),
                            │ deletar(), pesquisarRegistro(), pesquisarTudo()
┌───────────────────────────▼──────────────────────────────────┐
│ MODEL — br.com.controle                                       │
│ Campeonato, Equipe, Jogador, Estadio, Arbitro, Partida,       │
│ Classificacao, RelatorioClassificacao, RelatorioPartida       │
│ Responsabilidades: guardar dados (atributos private + get/set)│
│ e formatar textos de exibição (toString, getPosicaoFormatada).│
└───────────────────────────┬──────────────────────────────────┘
                            │ os DAOs recebem e devolvem esses objetos
┌───────────────────────────▼──────────────────────────────────┐
│ DAO / ENTIDADE — br.com.entidade                              │
│ DAO (conexão) + ManterCampeonato, ManterEquipe, ManterJogador,│
│ ManterEstadio, ManterArbitro, ManterClassificacao,            │
│ ManterPartida, ManterRelatorio                                │
│ Responsabilidades: abrir conexão, montar SQL, preencher       │
│ parâmetros, ler ResultSet e montar objetos.                   │
└───────────────────────────┬──────────────────────────────────┘
                            │ JDBC (PreparedStatement/CallableStatement)
┌───────────────────────────▼──────────────────────────────────┐
│ BANCO — MySQL 8 / database campeonato                          │
│ Tabelas, FKs, CHECKs, UNIQUEs, colunas geradas, triggers,     │
│ procedures e dados.                                           │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 Onde está o “Controller”?

Não existe uma classe `Controller` separada. No projeto, a **própria `TelaPrincipal` faz o papel de controller**: os `ActionListener` dos botões leem os campos, validam, montam o objeto e chamam o DAO. A separação principal demonstrada é **View × DAO × Model × Banco**.

Isso é aceitável para o escopo do trabalho, mas é bom saber a crítica: a tela acumula muita responsabilidade (2.073 linhas). Uma evolução natural seria extrair classes de controller/serviço e quebrar a tela em painéis menores.

### 3.3 Por que essa separação importa

- **Manutenção:** mudar o SQL de uma entidade fica em um único arquivo `Manter...`.
- **Reaproveitamento:** o mesmo DAO poderia ser usado por outra tela ou por um teste.
- **Segurança:** o uso de `PreparedStatement` com `?` evita SQL Injection.
- **Regras no banco:** qualquer alteração feita pelo Workbench também respeita as regras; não depende do Java.
- **Clareza para a apresentação:** cada pergunta pode ser respondida apontando a camada certa.

---

## 4. O caminho completo de um clique

Esta seção é o coração da apresentação: saber **quem chama quem**.

### 4.1 Cadastrar uma equipe

1. O usuário digita nome e cidade na aba **2. Equipes**.
2. A cada tecla, o `DocumentListener` criado por `vincularValidacao(...)` executa o `Runnable validar`.
3. O validador habilita **Cadastrar** somente se nome e cidade não estiverem vazios.
4. No clique, a lambda do botão:
   - cria `new Equipe()`;
   - copia `txtNomeEquipe` e `txtCidadeEquipe` com `trim()`;
   - chama `daoEquipe.inserir(eq)`.
5. `ManterEquipe.inserir`:
   - chama `abrirBanco()` (herdado de `DAO`);
   - monta `INSERT INTO EQUIPE (nome, cidade) VALUES (?, ?)`;
   - cria `PreparedStatement`, preenche os dois parâmetros e executa `pst.execute()`;
   - no `finally`, chama `fecharBanco()`.
6. A tela mostra “Equipe cadastrada!”, limpa os campos e chama `recarregarTudo()`.
7. `recarregarTudo()` consulta todos os DAOs e repovoa a `JTable` e os `JComboBox`.

**Código:** [`TelaPrincipal.criarPainelEquipe`](../../Java_Champs/src/br/com/visao/TelaPrincipal.java#L454-L604) e [`ManterEquipe.inserir`](../../Java_Champs/src/br/com/entidade/ManterEquipe.java#L10-L21).

### 4.2 Consultar pelo botão Pesquisar

1. O usuário digita um termo (ID, nome, cidade etc.) e clica em **Consultar**.
2. A tela chama `daoX.pesquisarRegistro(termo)`.
3. O DAO:
   - tenta converter o termo para `Integer`; se não conseguir, usa `-1` na comparação numérica;
   - executa um `SELECT ... WHERE id = ? OR nome LIKE ? ... ORDER BY (id = ?) DESC ... LIMIT 1`;
   - lê uma linha com `rs.next()` e monta o objeto com um método `montar...`.
4. A tela recebe o objeto (ou `null`) e preenche os campos; se for `null`, mostra “Nenhum ... encontrado”.

**Por que `ORDER BY (id = ?) DESC`?** Porque, se o usuário digitar `1`, o registro de ID 1 deve vir antes de um nome que contenha “1”. A expressão booleana vale 1 para o ID exato e 0 para os demais.

**Por que `LIMIT 1`?** Porque o formulário só comporta um registro. A busca devolve o primeiro candidato; não há tela de múltiplos resultados.

### 4.3 Agendar uma partida

1. A tela lê os cinco combos (campeonato, mandante, visitante, estádio, árbitro) e os campos (rodada, data/hora, placar, acréscimo, status, tempo).
2. `montarObjetoPartida()` converte os combos em IDs e os gols vazios em `null`.
3. `ManterPartida.inserir` executa `INSERT INTO PARTIDA (...)`.
4. O MySQL dispara o **trigger BEFORE INSERT**:
   - chama `sp_validar_partida(...)`;
   - se a data estiver fora do campeonato, a equipe não estiver inscrita, a equipe já tiver jogo na rodada ou o árbitro/estádio estiver ocupado, a procedure executa `SIGNAL SQLSTATE '45000'` e **cancela a gravação**.
5. Se a validação passar, o MySQL grava a partida e confere FKs, CHECKs e UNIQUEs.
6. O **trigger AFTER INSERT** chama `sp_sincronizar_classificacao(id_campeonato)`.
7. A procedure zera os acumulados do campeonato, soma as partidas `ENCERRADA` e recalcula a colocação.
8. O Java recebe sucesso e mostra “Partida registrada e classificação sincronizada!”; em erro, `mostrarErro` exibe a mensagem do banco.

**Código:** [`TelaPrincipal.criarPainelPartidas`](../../Java_Champs/src/br/com/visao/TelaPrincipal.java#L1324-L1570), [`ManterPartida.inserir`](../../Java_Champs/src/br/com/entidade/ManterPartida.java#L25-L37), [`sp_validar_partida`](../../SQL_Champs/Champs_procedures_triggers.sql#L147-L205), [`sp_sincronizar_classificacao`](../../SQL_Champs/Champs_procedures_triggers.sql#L37-L122).

### 4.4 Lançar o placar (alterar uma partida)

1. O usuário seleciona a partida na tabela ou pesquisa pelo ID.
2. Muda o status para `ENCERRADA`; o listener do combo preenche gols vazios com `0`.
3. Clica em **Salvar Placar / Alterar**.
4. `ManterPartida.alterar` executa `UPDATE PARTIDA SET ... WHERE id_partida = ?`.
5. **BEFORE UPDATE** valida com os valores de `NEW` (a partida alterada não conflita com ela mesma porque a procedure exclui `id_partida = p_id_partida`).
6. **AFTER UPDATE** recalcula a classificação do campeonato antigo; se a partida mudou de campeonato, recalcula também o novo.
7. A tela recarrega tudo; a aba 6 e o relatório passam a mostrar os novos números.

**Ponto de atenção:** o botão “Salvar Placar” não faz um UPDATE só dos gols; ele envia **todos** os campos da partida. Se algum combo estiver errado, a alteração inteira muda.

### 4.5 Excluir uma partida

1. A tela pede confirmação.
2. `ManterPartida.deletar` executa `DELETE FROM PARTIDA WHERE id_partida = ?`.
3. Não há trigger BEFORE DELETE na partida; o DELETE acontece (as FKs de outras tabelas não apontam para PARTIDA).
4. **AFTER DELETE** chama `sp_sincronizar_classificacao(OLD.id_campeonato)`; a classificação é refeita sem aquela partida.
5. A tela recarrega tudo.

### 4.6 Inscrever uma equipe

1. A aba 6 tem os combos de campeonato e equipe.
2. `ManterClassificacao.inscreverEquipe` executa `CALL sp_inscrever_equipe(?, ?)`.
3. A procedure faz `INSERT IGNORE INTO CLASSIFICACAO (id_campeonato, id_equipe)` e chama a sincronização.
4. Se a equipe já estava inscrita, o `INSERT IGNORE` não cria duplicata; a tela ainda mostra “Equipe inscrita no campeonato!”.

### 4.7 Gerar o relatório de classificação

1. O usuário escolhe o campeonato e clica em **Relatório de Classificação (Principal)**.
2. `ManterRelatorio.classificacaoPorCampeonato` abre a conexão, prepara `{call sp_relatorio_classificacao(?)}` com `CallableStatement`, passa o ID e executa.
3. A procedure devolve uma linha por equipe inscrita, com nome, cidade, pontos, jogos, V/E/D, gols, saldo, aproveitamento, jogos em casa/fora, quantidade de estádios/árbitros e status.
4. O DAO percorre o `ResultSet` e monta uma `ArrayList<RelatorioClassificacao>`.
5. A tela limpa a `JTable`, adiciona uma linha por objeto, calcula o resumo do rodapé e habilita a impressão.
6. **Imprimir / Salvar em PDF** chama `JTable.print(...)`; a janela do sistema permite escolher a impressora ou “Salvar como PDF”.

---

## 5. As oito abas e o que cada uma demonstra

| Aba | Entidade | Tipo no trabalho | DAO | Tabela principal | O que o professor costuma perguntar |
|---|---|---|---|---|---|
| 1. Campeonatos | `Campeonato` | CRUD simples | `ManterCampeonato` | `CAMPEONATO` | PK, `UNIQUE`, `CHECK` de datas, `ENUM` de status |
| 2. Equipes | `Equipe` | CRUD simples | `ManterEquipe` | `EQUIPE` | 1:N com jogador e partida; `UNIQUE` do nome |
| 3. Jogadores | `Jogador` | CRUD extra | `ManterJogador` | `JOGADOR` | FK para equipe, `UNIQUE (id_equipe, numero_camisa)`, data opcional |
| 4. Estádios | `Estadio` | CRUD simples | `ManterEstadio` | `ESTADIO` | capacidade `> 0`, `UNIQUE (nome, cidade)` |
| 5. Árbitros | `Arbitro` | CRUD simples | `ManterArbitro` | `ARBITRO` | registro único, categoria `ENUM` |
| 6. Inscrição & Classificação | `Classificacao` | apoio à rotina principal | `ManterClassificacao` | `CLASSIFICACAO` | PK composta, relação N:N, colunas geradas, status manual |
| 7. Partidas | `Partida` | CRUD principal | `ManterPartida` | `PARTIDA` | 5 FKs, triggers, regras, placar por status |
| 8. Relatórios | `RelatorioClassificacao`, `RelatorioPartida` | relatórios | `ManterRelatorio` | procedures | JOIN, parâmetros, `CallableStatement`, PDF |

### 5.1 Requisitos do enunciado → onde estão

| Requisito | Onde está |
|---|---|
| 4 CRUDs simples | Campeonato (aba 1), Equipe (aba 2), Estádio (aba 4) e Árbitro (aba 5) |
| CRUD extra | Jogador (aba 3), com FK para Equipe |
| CRUD principal com relacionamentos | Partida (aba 7), com 5 FKs e regras de negócio |
| Consulta com JOIN | `sp_relatorio_classificacao`, `sp_relatorio_partidas_periodo`, `ManterPartida.SELECT_PARTIDA`, `ManterJogador.SELECT_JOGADOR`, `ManterClassificacao` |
| Relatório da rotina principal | Aba 8 → classificação do campeonato |
| MySQL com PK, FK e relacionamentos | `Champs_schema.sql` |
| Java + POO + Swing + JDBC + MVC | pacotes `controle`, `entidade`, `visao` |
| Pesquisa individual | `pesquisarRegistro(...)` em cada `Manter...` |
| Validação de campos | `vincularValidacao(...)`, `aoMudarCombo(...)` e as lambdas `validar` de cada aba |

---

## 6. O modelo de dados em uma página

```text
CAMPEONATO 1 ───── N CLASSIFICACAO N ───── 1 EQUIPE 1 ───── N JOGADOR
     │                                              │
     │ 1                                            │ 1
     │                                              │
     N                                              N
  PARTIDA ─── N:1 ─── ESTADIO                     PARTIDA
     │
     ├── N:1 ─── ARBITRO
     ├── N:1 ─── EQUIPE (mandante)
     └── N:1 ─── EQUIPE (visitante)
```

- **`CLASSIFICACAO`** resolve a relação **N:N** entre campeonato e equipe e guarda os acumulados (jogos, V/E/D, gols, pontos, colocação). A chave primária é composta: `(id_campeonato, id_equipe)`.
- **`PARTIDA`** é a rotina principal: liga um campeonato, duas equipes (mandante e visitante), um estádio e um árbitro; tem placar, status e tempo de jogo.
- **`JOGADOR`** é 1:N com equipe; a camisa é única **dentro da equipe**.
- **`saldo_gols`** e **`pontuacao`** são **colunas geradas**: o MySQL calcula `gols_marcados - gols_sofridos` e `vitorias * 3 + empates` sozinho.

Para detalhes de cada coluna, veja o [guia do banco](01-banco-de-dados.md).

---

## 7. As duas barreiras de validação

O sistema valida em dois lugares de propósito:

| Barreira | Onde | Vantagem | Limite |
|---|---|---|---|
| **Java/Swing** | lambdas `validar` de cada aba; `montarObjeto...`; `mostrarErro` | resposta imediata, botão desabilitado, mensagem amigável | só vale para quem usa a tela |
| **MySQL** | FKs, `CHECK`s, `UNIQUE`s, triggers e procedures | vale para qualquer programa, inclusive Workbench | a mensagem é mais técnica e chega como `SQLException` |

Exemplos de regra que **só o banco** garante:

- data da partida dentro do período do campeonato;
- mandante e visitante inscritos no campeonato;
- equipe não joga duas vezes na mesma rodada;
- árbitro/estádio não ficam em dois jogos no mesmo horário;
- mesmo confronto não se repete na mesma rodada (`uq_partida_confronto`);
- equipe com partidas não pode ser desinscrita.

Exemplos de regra que a **tela** também confere:

- campos obrigatórios vazios;
- datas em `YYYY-MM-DD` e data final ≥ inicial;
- capacidade do estádio > 0;
- camisa entre 1 e 99;
- rodada > 0, acréscimo e gols ≥ 0;
- mandante ≠ visitante;
- placar coerente com o status.

---

## 8. Roteiro de demonstração (passo a passo)

> O enunciado pede a sequência **Cadastro → Relacionamento → Rotina Principal → Consulta com JOIN → Relatório**. Este roteiro segue essa ordem e já ensaia as mensagens que o professor pode pedir.

### Preparação

1. Banco criado com `Champs_schema.sql`, `Champs_procedures_triggers.sql` e `Champs_relatorios.sql`.
2. Opcional: rodar `Champs_dados.sql` para já ter dados variados.
3. Aplicação aberta pelo NetBeans (classe `br.com.visao.TelaPrincipal`) ou pela linha de comando.

### Passo 1 — Campeonato (CRUD simples)

- Aba **1. Campeonatos**.
- Mostre que **Cadastrar** começa desabilitado; digite o nome e veja o botão habilitar.
- Tente uma data final anterior à inicial: o botão continua desabilitado.
- Cadastre “Copa Demonstração 2026” com `2026-09-01` a `2026-12-30`, status `EM_ANDAMENTO`.
- Selecione a linha na tabela: os campos são preenchidos; **Alterar** e **Excluir** habilitam.
- Digite parte do nome no campo **Pesquisar** e clique em **Consultar**: o formulário é preenchido pelo DAO.
- **O que dizer:** “O ID é `AUTO_INCREMENT`, por isso o campo é somente leitura; a pesquisa individual usa `LIKE` e `LIMIT 1`.”

### Passo 2 — Equipes, jogadores, estádios e árbitros

- Aba **2. Equipes**: cadastre 4 equipes.
- Aba **3. Jogadores**: cadastre jogadores; mostre a camisa 1..99, a data opcional e o combo de equipe (FK).
- Tente cadastrar dois jogadores com a mesma camisa na mesma equipe: o banco recusa e a tela traduz a mensagem de `uq_jogador_camisa`.
- Aba **4. Estádios**: capacidade `45000`; tente `0` e veja o botão desabilitado.
- Aba **5. Árbitros**: cadastre com registro único.
- **O que dizer:** “Jogador é o CRUD extra; a FK `id_equipe` e o `UNIQUE (id_equipe, numero_camisa)` estão no schema.”

### Passo 3 — Relacionamento (inscrição)

- Aba **6. Inscrição & Classificação**.
- Escolha o campeonato e inscreva as 4 equipes.
- Mostre a tabela de classificação zerada, com `-` na posição (quem ainda não jogou tem `colocacao_equipe NULL`).
- **O que dizer:** “`CLASSIFICACAO` é a tabela intermediária do N:N; a PK composta impede duplicar a inscrição.”

### Passo 4 — Rotina principal (partidas)

- Aba **7. Partidas**.
- Agende um jogo: rodada 1, data dentro do campeonato, mandante e visitante diferentes, estádio e árbitro.
- Tente salvar com o mesmo time como mandante e visitante: o botão fica desabilitado.
- Tente uma data fora do período (ex.: `2027-01-10`): o banco recusa com `Data da partida fora do periodo do campeonato`.
- Tente a mesma equipe em dois jogos da rodada 1: o banco recusa com `Equipe ja possui partida nesta rodada`.
- Tente o mesmo árbitro no mesmo horário: `Arbitro ou estadio ja ocupado neste horario`.
- **O que dizer:** “A tela valida o básico; o trigger `BEFORE` é a última barreira e a mensagem chega como `SQLException`.”

### Passo 5 — Placar e classificação automática

- Selecione a partida, mude o status para `ENCERRADA` e informe o placar (ex.: 2 x 1).
- Clique em **Salvar Placar / Alterar**.
- Volte à aba 6 e mostre a classificação atualizada: 3 pontos para o vencedor, 1 para o empate, saldo e posição.
- **O que dizer:** “O `UPDATE` dispara `BEFORE` (valida) e `AFTER` (chama `sp_sincronizar_classificacao`), que recalcula do zero.”

### Passo 6 — Consulta com JOIN

- Na aba 7, a tabela mostra **nomes** de campeonato, mandante, visitante, estádio e árbitro, não IDs.
- Na aba 6, a tabela mostra o nome da equipe.
- **O que dizer:** “O `SELECT_PARTIDA` tem `INNER JOIN` com `CAMPEONATO`, `EQUIPE` duas vezes (m e v), `ESTADIO` e `ARBITRO`.”

### Passo 7 — Relatório principal

- Aba **8. Relatórios**.
- Escolha o campeonato e clique em **Relatório de Classificação (Principal)**.
- Mostre as colunas: posição, equipe, cidade, PTS, J, V, E, D, GP, GC, SG, aproveitamento, casa, fora, estádios, árbitros e status.
- Mostre o rodapé com o resumo (equipes, partidas encerradas, gols e média).
- **O que dizer:** “A procedure parte da `CLASSIFICACAO` e faz JOIN/subconsultas nas tabelas da rotina; é o relatório principal porque mostra a ligação de todas elas.”

### Passo 8 — Relatório de partidas por período e PDF

- Ainda na aba 8, escolha o período e clique em **Partidas por Período**.
- Mostre a tabela com data formatada `dd/mm/aaaa hh:mm`, placar `2 x 1` ou `- x -`.
- Clique em **Imprimir / Salvar em PDF**; na janela de impressão, escolha “Salvar como PDF”.
- **O que dizer:** “A procedure recebe campeonato e datas; o Java usa `CallableStatement`; a impressão é `JTable.print`.”

### Passo 9 — Exclusões e proteção do histórico

- Tente excluir uma equipe que já jogou: o banco recusa por `ON DELETE RESTRICT` (erro 1451).
- Tente remover a inscrição de uma equipe que já tem partidas: o trigger `trg_classificacao_before_delete` recusa com `Equipe possui partidas neste campeonato`.
- Exclua uma partida: a classificação é recalculada pelo `AFTER DELETE`.
- **O que dizer:** “As FKs e triggers protegem o histórico; não é só a tela que impede.”

---

## 9. Como estudar o código em três passos

### Passo A — Entenda os dados

1. Leia o [guia do banco](01-banco-de-dados.md), seções 1 a 3.
2. Abra `Champs_schema.sql` e localize as 7 tabelas, as PKs, as FKs e os CHECKs.
3. Desenhe o modelo em uma folha: 7 caixas e as setas. Se conseguir desenhar sem olhar, você entendeu a estrutura.

### Passo B — Entenda o caminho de um CRUD

1. Abra `TelaPrincipal.java` na aba 2 (Equipe), que é o CRUD mais curto.
2. Siga o clique de **Cadastrar** até `ManterEquipe.inserir`.
3. Volte e siga **Consultar** até `pesquisarRegistro`.
4. Depois leia `ManterCampeonato` e `ManterEstadio`; são o mesmo padrão.
5. Só então leia `ManterJogador` (JOIN + NULL) e `ManterPartida` (12 parâmetros + JOIN).

### Passo C — Entenda a parte difícil

1. Leia `sp_validar_partida` regra por regra.
2. Leia `sp_sincronizar_classificacao` passo 1, 2 e 3.
3. Leia os triggers BEFORE/AFTER.
4. Volte ao Java e veja como o erro do `SIGNAL` chega em `mostrarErro`.
5. Por último, leia as duas procedures de relatório e o `ManterRelatorio`.

> **Dica de apresentação:** não tente decorar linha por linha. Decore **cinco fluxos**: cadastrar, consultar, agendar partida, lançar placar e gerar relatório. Todo o resto é variação.

---

## 10. Divisão sugerida para a equipe

Se cada integrante precisa responder a duas perguntas sobre qualquer parte, uma divisão possível (sem “ilhas”) é:

| Integrante | Foco principal | Precisa saber também |
|---|---|---|
| 1 | Schema, PK/FK, CHECKs, dados de demonstração | fluxo de cadastro e erro 1451 |
| 2 | Triggers, `sp_validar_partida`, `sp_sincronizar_classificacao` | como o Java mostra a mensagem |
| 3 | DAOs, JDBC, pesquisa individual, `TelaPrincipal` (abas 1–5) | modelo e validações |
| 4 | Aba 6, aba 7, relatórios, Swing/impressão | procedures dos relatórios |

Todos devem saber responder: “qual é a rotina principal?”, “onde está o JOIN?”, “o que acontece quando lanço o placar?” e “por que a classificação é automática?”.

---

## 11. Perguntas de abertura (respostas curtas)

**1. Qual é a finalidade do sistema?**
Organizar campeonatos de futebol: cadastros, inscrição de equipes, agendamento e resultado de partidas, classificação automática e relatórios.

**2. Qual é a rotina principal?**
O registro de partidas (aba 7), que relaciona campeonato, duas equipes, estádio e árbitro e alimenta a classificação.

**3. Qual é a entidade central?**
A `CLASSIFICACAO`, que liga campeonato e equipe (N:N) e é recalculada pelas partidas.

**4. O que é CRUD?**
Create, Read, Update, Delete: inserir, consultar, alterar e excluir. No SQL: `INSERT`, `SELECT`, `UPDATE`, `DELETE`.

**5. Quais são os 4 CRUDs simples?**
Campeonato, Equipe, Estádio e Árbitro. Jogador é o CRUD extra; Partida é o principal.

**6. O que é MVC no projeto?**
`br.com.controle` é o Model; `br.com.entidade` é o DAO; `br.com.visao` é a View. O controller está nos eventos da tela.

**7. Onde está o JDBC?**
Em `DAO` (conexão) e nas classes `Manter...` (`PreparedStatement`, `ResultSet`). As procedures usam `CallableStatement` em `ManterRelatorio`.

**8. O que é uma PK?**
A coluna (ou combinação) que identifica cada linha de forma única. Ex.: `id_equipe`; em `CLASSIFICACAO`, `(id_campeonato, id_equipe)`.

**9. O que é uma FK?**
Uma coluna que aponta para a PK de outra tabela e garante que o valor existe. Ex.: `PARTIDA.id_mandante → EQUIPE.id_equipe`.

**10. Onde está o JOIN?**
Nas duas procedures de relatório e nos `SELECT` de `ManterPartida`, `ManterJogador` e `ManterClassificacao`.

**11. Como a classificação é calculada?**
Um trigger `AFTER` em `PARTIDA` chama `sp_sincronizar_classificacao`, que zera os acumulados, soma as partidas `ENCERRADA` e calcula a colocação com `ROW_NUMBER`.

**12. Por que a classificação não é calculada no Java?**
Porque precisa valer para qualquer alteração, inclusive feita direto no banco; e porque o banco tem os dados e as regras centralizados.

**13. Como o Java chama uma procedure?**
`con.prepareCall("{call nome(?, ?)}")`, `setInt`/`setDate` nos parâmetros e `executeQuery()` para ler o `ResultSet`.

**14. Como gerar PDF?**
O botão **Imprimir / Salvar em PDF** chama `JTable.print(...)`; a janela de impressão do sistema oferece “Salvar como PDF”.

**15. Onde estão as validações?**
Nas lambdas `validar` de cada aba (Swing) e nos CHECKs/triggers do MySQL (última barreira).

---

## 12. Limitações e respostas honestas

O professor pode perguntar “o que você faria diferente?”. Estas são as limitações reais do código atual; saber explicá-las mostra domínio.

### 12.1 A conexão não é fechada em todos os casos

`DAO.fecharBanco()` fecha apenas o `PreparedStatement`; **não fecha `ResultSet` nem `Connection`**. Cada operação abre uma conexão nova em `abrirBanco()`. Na prática, as conexões antigas ficam abertas até o garbage collector, o que pode esgotar o pool do MySQL em uso prolongado.

- **Resposta curta:** “O `finally` garante o fechamento do statement, mas a conexão não é fechada; é um ponto de melhoria.”
- **Como corrigir:** fechar `rs`, `pst` e `con` no `finally` (ou usar try-with-resources e um pool).

### 12.2 O driver usa o nome antigo

`DAO.abrirBanco()` chama `Class.forName("com.mysql.jdbc.Driver")`. O Connector/J 8+ recomenda `com.mysql.cj.jdbc.Driver`. Se a versão instalada não mantiver a classe antiga, o `ClassNotFoundException` é apenas impresso no console e a conexão fica nula, gerando erro depois.

- **Resposta curta:** “O nome antigo funciona em algumas versões por compatibilidade; o correto no Connector/J 9 é `com.mysql.cj.jdbc.Driver`.”
- **Como corrigir:** trocar a string e relançar a exceção em vez de só imprimir.

### 12.3 O `99_testes.sql` foi substituído pelo `Champs_dados.sql`

O `99_testes.sql` foi **substituído** pelo `Champs_dados.sql`: o arquivo de dados tem muito mais casos distintos (4 campeonatos em status diferentes, 28 partidas com placares/situações variadas, jogadores com data nula etc.) e serve tanto para demonstrar quanto para exercitar as regras. O `testar.sh` ainda referenciava o arquivo antigo — uma pendência de atualização do script.

- **Caminho atual:** usar `Champs_dados.sql` e verificar os números/relatórios (o script `testar.sh` foi ajustado para isso).
- **Se quiser o roteiro antigo:** ele pode ser recuperado do histórico:
  ```bash
  git show 2571563^:SQL_Champs/99_testes.sql > SQL_Champs/99_testes.sql
  ```
- **Ponto de atenção:** sem o arquivo, o `testar.sh` antigo não executava os testes e chegava a imprimir “TODOS OS TESTES PASSARAM” com a saída vazia. O script atualizado usa `Champs_dados.sql` e faz verificações de verdade.

### 12.4 O `TelaPrincipal.form` é um stub

O arquivo `.form` existe, mas contém apenas a configuração de fechar a janela. A interface real é construída em `configurarJanelaCompleta()` e nos métodos `criarPainel...`. Não abra o editor visual esperando ver a tela.

- **Resposta curta:** “A tela é feita em código Java; o `.form` é um resquício mínimo do NetBeans.”

### 12.5 O status da classificação é manual

`CLASSIFICACAO.status_atual` (`A_JOGAR`, `CLASSIFICADO`, `ELIMINADO`) é atualizado pelo botão da aba 6, não pelo banco. A pontuação e a colocação são automáticas; o status é uma decisão do organizador.

### 12.6 A posição do jogador não é `ENUM` no banco

No Java, a posição vem de um `JComboBox` com seis valores. No MySQL, `JOGADOR.posicao` é `VARCHAR(30)`: o banco aceita qualquer texto. A restrição é só da tela.

- **Resposta curta:** “Para o trabalho, o combo padroniza; se quiséssemos garantir no banco, usaríamos `ENUM` ou uma tabela de domínio.”

### 12.7 O resumo do relatório assume partidas completas

O rodapé do relatório de classificação calcula `partidas = somaJogos / 2` e `gols = somaGolsPro / 2`, porque cada partida encerrada conta para as duas equipes. Se os dados estiverem inconsistentes (por exemplo, uma partida encerrada sem as duas equipes na classificação), o resumo pode arredondar errado.

### 12.8 A tela recarrega tudo a cada escrita

`recarregarTudo()` consulta todas as tabelas e recria todos os combos depois de cada cadastro/alteração/exclusão. É simples e correto para o volume do trabalho, mas não é a estratégia mais eficiente.

### 12.9 Não há testes unitários Java

A verificação é manual (telas) e no banco (`testar.sh` com `Champs_dados.sql`). Um próximo passo seria JUnit para os DAOs e testes de integração com um banco temporário.

---

## 13. Glossário rápido

| Termo | Significado no projeto |
|---|---|
| **PK** | Primary Key: identifica a linha. Ex.: `id_campeonato`. |
| **FK** | Foreign Key: aponta para a PK de outra tabela. Ex.: `id_equipe` em `JOGADOR`. |
| **N:N** | Muitos-para-muitos; resolvido por `CLASSIFICACAO`. |
| **CHECK** | Regra de validação do banco. Ex.: `capacidade > 0`. |
| **UNIQUE** | Impede repetição. Ex.: `uq_equipe_nome`. |
| **ENUM** | Lista fixa de valores. Ex.: status da partida. |
| **Coluna gerada** | Calculada pelo MySQL. Ex.: `pontuacao = vitorias * 3 + empates`. |
| **Trigger** | Bloco executado automaticamente em INSERT/UPDATE/DELETE. |
| **Procedure** | Bloco SQL nomeado, chamado com `CALL`. |
| **SIGNAL** | “Lança um erro” no MySQL, como um `throw`. |
| **JDBC** | API Java para acessar bancos. |
| **PreparedStatement** | SQL com `?`, preenchido por setters; evita SQL Injection. |
| **CallableStatement** | JDBC para chamar procedures. |
| **ResultSet** | Cursor com as linhas devolvidas por um SELECT. |
| **DAO** | Data Access Object: classe que isola o SQL. |
| **Swing** | Biblioteca de interface do Java. |
| **EDT** | Event Dispatch Thread: thread do Swing que executa os eventos. |

---

## 14. Checklist final antes da apresentação

- [ ] Sei desenhar o modelo de 7 tabelas e explicar PK/FK/N:N.
- [ ] Sei explicar o caminho de um clique de cadastro, consulta e partida.
- [ ] Sei explicar o que o trigger `BEFORE` faz e o que o `AFTER` faz.
- [ ] Sei explicar por que a classificação é recalculada do zero.
- [ ] Sei apontar o JOIN nas procedures e nos DAOs.
- [ ] Sei explicar `PreparedStatement`, `CallableStatement` e `ResultSet`.
- [ ] Sei explicar as validações da tela e as do banco.
- [ ] Sei gerar o relatório e salvar em PDF.
- [ ] Sei responder às limitações (conexão, driver, `Champs_dados`/`99_testes`, `.form`).
- [ ] Consigo abrir qualquer arquivo e localizar o método principal em menos de 30 segundos.

> Próximo passo natural: leia o [guia do banco](01-banco-de-dados.md), depois o [guia do Java](02-java-e-relatorios.md) e use o [mapa do código](03-mapa-do-codigo.md) para navegar. O [simulado](04-perguntas-e-respostas.md) serve para testar se você consegue responder sem consultar.
