# Mapa do Código — onde está cada coisa

> **Objetivo:** ser o índice de consulta rápida do projeto. Quando o professor perguntar “onde está…?”, este guia leva ao arquivo, à faixa de linhas e ao método.
>
> Os números de linha correspondem ao código atual do repositório. Se o código for alterado, eles podem deslocar; use o nome do método como âncora principal.
>
> Guias relacionados: [visão geral](00-guia-de-apresentacao.md) · [banco](01-banco-de-dados.md) · [Java](02-java-e-relatorios.md) · [simulado](04-perguntas-e-respostas.md).

---

## 1. Como usar este mapa

Três formas de consulta:

1. **Por arquivo:** seção 2 (SQL) e seção 3 (Java).
2. **Por funcionalidade:** seção 4 (matriz “tela → DAO → SQL → banco”).
3. **Por assunto:** seção 5 (“onde encontrar validação, conexão, JOIN, trigger…”).

Dica para a apresentação: decore a **matriz da seção 4**. Ela responde 80% das perguntas “onde está…?”.

---

## 2. Mapa dos arquivos SQL

### 2.1 `Champs_schema.sql` (191 linhas)

[`SQL_Champs/Champs_schema.sql`](../../SQL_Champs/Champs_schema.sql)

| Linhas | Bloco | O que contém |
|---:|---|---|
| 1–12 | cabeçalho + criação do banco | `DROP DATABASE`, `CREATE DATABASE`, `USE`, charset/collation |
| 14–27 | `CAMPEONATO` | PK, `nome`, datas, `status ENUM`, `uq_campeonato_nome`, `chk_campeonato_datas` |
| 29–38 | `EQUIPE` | PK, `nome`, `cidade`, `uq_equipe_nome` |
| 40–51 | `ESTADIO` | PK, nome, cidade, capacidade, `uq_estadio_nome_cidade`, `chk_estadio_capacidade` |
| 53–71 | `JOGADOR` | PK, FK equipe, `uq_jogador_camisa`, `chk_jogador_camisa` |
| 73–84 | `ARBITRO` | PK, nome, registro, `categoria ENUM`, `uq_arbitro_registro` |
| 86–126 | `CLASSIFICACAO` | PK composta, FKs, colunas geradas, `chk_classificacao_soma`, `chk_classificacao_colocacao` |
| 128–191 | `PARTIDA` | PK, 5 FKs, 8 CHECKs, `uq_partida_confronto`, índices |

**Sub-mapas úteis:**

| Assunto | Linhas |
|---|---:|
| Colunas geradas `saldo_gols`/`pontuacao` | 104–107 |
| PK composta de `CLASSIFICACAO` | 112 |
| FKs de `PARTIDA` | 150–165 |
| CHECKs de placar/status/tempo | 170–185 |
| `uq_partida_confronto` | 187 |
| Índices de data | 189–190 |

### 2.2 `Champs_procedures_triggers.sql` (289 linhas)

[`SQL_Champs/Champs_procedures_triggers.sql`](../../SQL_Champs/Champs_procedures_triggers.sql)

| Linhas | Bloco | O que contém |
|---:|---|---|
| 1–26 | cabeçalho | fluxo, regras, pontuação, `USE campeonato` |
| 28–30 | `DROP PROCEDURE` + `DELIMITER $$` | preparação |
| 32–122 | `sp_sincronizar_classificacao` | passo 1 (zerar), passo 2 (somar), passo 3 (colocação) |
| 124–136 | `sp_inscrever_equipe` | `INSERT IGNORE` + sincronização |
| 138–205 | `sp_validar_partida` | regras 1–4 + `SIGNAL` |
| 207–230 | triggers `BEFORE` | insert/update chamam validação |
| 232–269 | triggers `AFTER` | insert/update/delete sincronizam |
| 271–287 | `trg_classificacao_before_delete` | bloqueia desinscrição com partidas |
| 289 | `DELIMITER ;` | volta ao normal |

**Sub-mapas:**

| Regra | Linhas |
|---|---:|
| Regra 1 — data no período | 164–170 |
| Regra 2 — equipes inscritas | 172–179 |
| Regra 3 — equipe uma vez na rodada | 181–191 |
| Regra 4 — árbitro/estádio livre | 193–202 |
| Passo 1 — zerar | 42–53 |
| Passo 2 — somar | 55–100 |
| Passo 3 — colocação | 102–121 |

### 2.3 `Champs_relatorios.sql` (118 linhas)

[`SQL_Champs/Champs_relatorios.sql`](../../SQL_Champs/Champs_relatorios.sql)

| Linhas | Bloco | O que contém |
|---:|---|---|
| 1–23 | cabeçalho | explicação dos dois relatórios |
| 25–81 | `sp_relatorio_classificacao` | JOIN + subconsultas + aproveitamento |
| 83–116 | `sp_relatorio_partidas_periodo` | JOIN + filtro de período + formatação |
| 118 | `DELIMITER ;` | fim |

### 2.4 `Champs_dados.sql` (183 linhas)

[`SQL_Champs/Champs_dados.sql`](../../SQL_Champs/Champs_dados.sql)

| Linhas | Bloco | Quantidade |
|---:|---|---:|
| 1–25 | cabeçalho + `START TRANSACTION` | — |
| 27–34 | campeonatos | 4 |
| 36–49 | equipes | 10 |
| 51–59 | estádios | 5 |
| 61–69 | árbitros | 5 |
| 71–81 | inscrições | 20 |
| 83–109 | jogadores | 21 |
| 111–138 | partidas da Copa Regional | 15 |
| 140–156 | partidas da Liga Metropolitana | 9 |
| 158–169 | partidas da Copa dos Campeões | 4 |
| 171 | `COMMIT` | — |
| 173–183 | conferência (`CALL`) | 2 chamadas |

### 2.5 `testar.sh` (53 linhas)

[`SQL_Champs/testar.sh`](../../SQL_Champs/testar.sh)

| Linhas | Bloco |
|---:|---|
| 1–15 | cabeçalho, `cd`, container e `FALHAS` |
| 17–20 | `docker rm` + `docker run` + `trap` |
| 23 | função `mysql()` |
| 25–26 | espera o MySQL |
| 29–39 | executa schema, procedures, relatórios e `Champs_dados.sql` |
| 41–71 | helpers `consultar`, `verificar` e `verificar_erro` |
| 73–82 | confere as contagens do `Champs_dados` |
| 84–94 | confere a classificação e as partidas encerradas |
| 96–103 | confere as duas procedures de relatório |
| 105–126 | testa as regras que devem falhar |
| 128–133 | resultado final |

> **Estado atual:** o `99_testes.sql` foi **substituído** por `Champs_dados.sql`; o `testar.sh` foi atualizado para carregar esse arquivo e conferir contagens, classificação, relatórios e quatro regras. Veja a seção 9 do [guia do banco](01-banco-de-dados.md#9-testarsh-e-o-champs_dadossql) e a seção 12.3 do [guia de apresentação](00-guia-de-apresentacao.md#123-o-99_testessql-foi-substituído-pelo-champs_dadossql).

---

## 3. Mapa dos arquivos Java

### 3.1 Pacote `br.com.controle` (model)

| Arquivo | Linhas | Campos | Métodos principais | Observação |
|---|---:|---|---|---|
| [`Campeonato.java`](../../Java_Champs/src/br/com/controle/Campeonato.java) | 55 | 3–8 | getters/setters 10–48; `toString` 50–53 | datas `String` |
| [`Equipe.java`](../../Java_Champs/src/br/com/controle/Equipe.java) | 36 | 3–6 | getters/setters 8–30; `toString` 32–35 | usada em combos |
| [`Estadio.java`](../../Java_Champs/src/br/com/controle/Estadio.java) | 45 | 3–8 | getters/setters 10–39; `toString` 41–44 | capacidade `int` |
| [`Arbitro.java`](../../Java_Champs/src/br/com/controle/Arbitro.java) | 45 | 3–7 | getters/setters 9–39; `toString` 41–44 | categoria `String` |
| [`Jogador.java`](../../Java_Champs/src/br/com/controle/Jogador.java) | 86 | 12–22 | getters/setters 24–78; `toString` 80–85 | `nomeEquipe` extra |
| [`Partida.java`](../../Java_Champs/src/br/com/controle/Partida.java) | 168 | 3–23 | getters/setters 25–167 | 5 nomes extras; gols `Integer` |
| [`Classificacao.java`](../../Java_Champs/src/br/com/controle/Classificacao.java) | 121 | 3–16 | getters/setters 18–120 | `colocacaoEquipe` `Integer` |
| [`RelatorioClassificacao.java`](../../Java_Champs/src/br/com/controle/RelatorioClassificacao.java) | 185 | 10–32 | getters/setters 34–176; formatadores 178–184 | linha do relatório |
| [`RelatorioPartida.java`](../../Java_Champs/src/br/com/controle/RelatorioPartida.java) | 81 | 7–16 | getters/setters 18–80 | linha do relatório |

### 3.2 Pacote `br.com.entidade` (DAO)

#### `DAO.java` (39 linhas)

[`DAO.java`](../../Java_Champs/src/br/com/entidade/DAO.java)

| Linhas | Elemento | O que faz |
|---:|---|---|
| 12–14 | campos `con`, `pst`, `rs` | conexão, statement e resultado |
| 16–31 | `abrirBanco()` | carrega driver, abre conexão, trata exceções |
| 33–38 | `fecharBanco()` | fecha apenas `pst` |

#### `ManterCampeonato.java` (117 linhas)

| Linhas | Método | SQL |
|---:|---|---|
| 10–23 | `inserir` | `INSERT INTO CAMPEONATO ...` |
| 25–40 | `alterar` | `UPDATE CAMPEONATO ...` |
| 42–52 | `deletar` | `DELETE FROM CAMPEONATO ...` |
| 54–89 | `pesquisarRegistro` | `SELECT ... WHERE id OR nome LIKE ...` |
| 91–106 | `pesquisarTudo` | `SELECT ... ORDER BY id` |
| 108–116 | `montarCampeonato` | `ResultSet` → objeto |

#### `ManterEquipe.java` (110 linhas)

| Linhas | Método | SQL |
|---:|---|---|
| 10–21 | `inserir` | `INSERT INTO EQUIPE ...` |
| 23–35 | `alterar` | `UPDATE EQUIPE ...` |
| 37–47 | `deletar` | `DELETE FROM EQUIPE ...` |
| 49–85 | `pesquisarRegistro` | `SELECT ... WHERE id OR nome/cidade LIKE ...` |
| 87–101 | `pesquisarTudo` | `SELECT ... ORDER BY id` |
| 103–109 | `montarEquipe` | `ResultSet` → objeto |

#### `ManterEstadio.java` (112 linhas)

| Linhas | Método | SQL |
|---:|---|---|
| 10–22 | `inserir` | `INSERT INTO ESTADIO ...` |
| 24–37 | `alterar` | `UPDATE ESTADIO ...` |
| 39–49 | `deletar` | `DELETE FROM ESTADIO ...` |
| 51–86 | `pesquisarRegistro` | `SELECT ... WHERE id OR nome/cidade LIKE ...` |
| 88–102 | `pesquisarTudo` | `SELECT ... ORDER BY id` |
| 104–111 | `montarEstadio` | `ResultSet` → objeto |

#### `ManterArbitro.java` (114 linhas)

| Linhas | Método | SQL |
|---:|---|---|
| 10–22 | `inserir` | `INSERT INTO ARBITRO ...` |
| 24–38 | `alterar` | `UPDATE ARBITRO ...` |
| 40–50 | `deletar` | `DELETE FROM ARBITRO ...` |
| 52–87 | `pesquisarRegistro` | `SELECT ... WHERE id OR nome/registro LIKE ...` |
| 89–104 | `pesquisarTudo` | `SELECT ... ORDER BY id` |
| 106–113 | `montarArbitro` | `ResultSet` → objeto |

#### `ManterJogador.java` (150 linhas)

| Linhas | Método | Destaque |
|---:|---|---|
| 19–23 | `SELECT_JOGADOR` | `INNER JOIN EQUIPE` + `nome_equipe` |
| 25–36 | `inserir` | 5 parâmetros |
| 38–50 | `alterar` | 5 + ID |
| 52–65 | `preencherParametros` | data nula → `setNull(Types.DATE)` |
| 67–77 | `deletar` | por ID |
| 83–121 | `pesquisarRegistro` | ID, nome, posição, equipe, `CAST(camisa AS CHAR)` |
| 123–137 | `pesquisarTudo` | JOIN + `ORDER BY id` |
| 139–149 | `montarJogador` | `nome_equipe` no objeto |

#### `ManterPartida.java` (191 linhas)

| Linhas | Método | Destaque |
|---:|---|---|
| 15–23 | `SELECT_PARTIDA` | 5 JOINs; `EQUIPE` duas vezes (m/v) |
| 25–37 | `inserir` | 12 parâmetros |
| 39–53 | `alterar` | 12 + ID |
| 55–86 | `preencherParametros` | gols/tempo nulos; data como `String` |
| 88–98 | `deletar` | por ID |
| 104–145 | `pesquisarRegistro` | ID ou nomes de equipe/campeonato/estádio/árbitro |
| 147–161 | `pesquisarTudo` | JOIN + `ORDER BY id` |
| 163–190 | `montarPartida` | `wasNull` para gols; 5 nomes extras |

#### `ManterClassificacao.java` (122 linhas)

| Linhas | Método | Destaque |
|---:|---|---|
| 11–22 | `inscreverEquipe` | `CALL sp_inscrever_equipe` |
| 25–36 | `removerInscricao` | `DELETE` (trigger bloqueia) |
| 39–51 | `atualizarStatus` | `UPDATE status_atual` |
| 53–76 | `pesquisarRegistro(int,int)` | chave composta |
| 79–102 | `listarPorCampeonato` | JOIN + ordem por colocação |
| 104–121 | `montarClassificacao` | `wasNull` na colocação |

#### `ManterRelatorio.java` (132 linhas)

| Linhas | Método | Destaque |
|---:|---|---|
| 24–66 | `classificacaoPorCampeonato` | `CallableStatement` + `wasNull` na posição |
| 71–101 | `partidasPorPeriodo` | 3 parâmetros; `setDate` |
| 108–131 | `fecharRecursos` | fecha `cs`, `pst` e `con` |

### 3.3 Pacote `br.com.visao` — `TelaPrincipal.java` (2.073 linhas)

[`TelaPrincipal.java`](../../Java_Champs/src/br/com/visao/TelaPrincipal.java)

#### Mapa geral

| Linhas | Bloco |
|---:|---|
| 1–11 | imports (`controle.*`, `entidade.*`, `java.awt.*`, Swing) |
| 13–21 | Javadoc da classe |
| 21 | `public class TelaPrincipal extends JFrame` |
| 23–84 | campos (DAOs, componentes, caches) |
| 86–90 | construtor |
| 92–251 | helpers de validação/tabela e montagem da janela |
| 253–449 | aba 1 — Campeonato |
| 451–611 | aba 2 — Equipe |
| 613–818 | aba 3 — Jogador |
| 820–993 | aba 4 — Estádio |
| 995–1166 | aba 5 — Árbitro |
| 1168–1319 | aba 6 — Inscrição & Classificação |
| 1321–1625 | aba 7 — Partidas |
| 1627–1850 | aba 8 — Relatórios |
| 1852–1952 | `recarregarTudo` |
| 1954–1977 | `carregarTabelaClassificacao` |
| 1979–2015 | seletores de combo |
| 2017–2050 | `mostrarErro` |
| 2052–2058 | `initComponents` |
| 2060–2072 | `main` |

#### Helpers (linhas 92–251)

| Linhas | Método | Para que serve |
|---:|---|---|
| 101–124 | `vincularValidacao` | `DocumentListener` que roda o validador |
| 126–130 | `aoMudarCombo` | `ActionListener` nos combos |
| 132–140 | `aoSelecionarTabela` | `ListSelectionListener` nas tabelas |
| 142–151 | `modelo` | `DefaultTableModel` não editável |
| 153–159 | `configurarTabela` | altura, sem sorter, seleção simples |
| 161–163 | `vazio` | string nula/vazia |
| 165–177 | `dataValida` | data ISO |
| 179–189 | `dataHoraValida` | data + hora |
| 191–205 | `dataNascimentoValida` | opcional, não futura |
| 207–216 | `inteiroOuNulo` | inteiro ou `null` |
| 218–221 | `inteiroPositivo` | > 0 |
| 223–226 | `inteiroNaoNegativo` | ≥ 0 |
| 228–251 | `configurarJanelaCompleta` | título, tamanho, 8 abas |

#### Aba 1 — Campeonato (253–449)

| Linhas | Método |
|---:|---|
| 256–401 | `criarPainelCampeonato` |
| 403–411 | `campeonatoDaTabela` |
| 413–419 | `preencherCamposCampeonato` |
| 421–440 | `consultarCampeonato` |
| 442–449 | `limparCamposCampeonato` |

#### Aba 2 — Equipe (451–611)

| Linhas | Método |
|---:|---|
| 454–604 | `criarPainelEquipe` |
| 606–611 | `limparCamposEquipe` |

#### Aba 3 — Jogador (613–818)

| Linhas | Método |
|---:|---|
| 616–784 | `criarPainelJogador` |
| 786–793 | `preencherCamposJogador` |
| 795–806 | `montarObjetoJogador` |
| 808–818 | `limparCamposJogador` |

#### Aba 4 — Estádio (820–993)

| Linhas | Método |
|---:|---|
| 823–985 | `criarPainelEstadio` |
| 987–993 | `limparCamposEstadio` |

#### Aba 5 — Árbitro (995–1166)

| Linhas | Método |
|---:|---|
| 998–1158 | `criarPainelArbitro` |
| 1160–1166 | `limparCamposArbitro` |

#### Aba 6 — Inscrição & Classificação (1168–1319)

| Linhas | Método |
|---:|---|
| 1171–1305 | `criarPainelClassificacao` |
| 1307–1319 | `atualizarBotoesClassificacao` |

#### Aba 7 — Partidas (1321–1625)

| Linhas | Método |
|---:|---|
| 1324–1570 | `criarPainelPartidas` |
| 1572–1589 | `preencherCamposPartida` |
| 1591–1613 | `montarObjetoPartida` |
| 1615–1625 | `limparCamposPartida` |

#### Aba 8 — Relatórios (1627–1850)

| Linhas | Método |
|---:|---|
| 1630–1730 | `criarPainelRelatorios` |
| 1732–1737 | `tabelaRelatorioAtual` |
| 1739–1799 | `gerarRelatorioClassificacao` |
| 1801–1850 | `gerarRelatorioPartidas` |

#### Fechamento (1852–2072)

| Linhas | Método |
|---:|---|
| 1855–1952 | `recarregarTudo` |
| 1954–1977 | `carregarTabelaClassificacao` |
| 1981–1988 | `selecionarComboCampeonato` |
| 1990–1997 | `selecionarComboEquipe` |
| 1999–2006 | `selecionarComboEstadio` |
| 2008–2015 | `selecionarComboArbitro` |
| 2017–2050 | `mostrarErro` |
| 2054–2058 | `initComponents` |
| 2060–2072 | `main` |

---

## 4. Matriz “tela → DAO → SQL → banco”

### 4.1 CRUDs

| Funcionalidade | Tela (método) | DAO (método) | SQL | Banco |
|---|---|---|---|---|
| Cadastrar campeonato | `criarPainelCampeonato` (328–346) | `ManterCampeonato.inserir` | `INSERT INTO CAMPEONATO` | `CAMPEONATO` |
| Consultar campeonato | `consultarCampeonato` (421–440) | `ManterCampeonato.pesquisarRegistro` | `SELECT ... WHERE id OR nome` | `CAMPEONATO` |
| Alterar campeonato | `criarPainelCampeonato` (350–368) | `ManterCampeonato.alterar` | `UPDATE CAMPEONATO` | `CAMPEONATO` |
| Excluir campeonato | `criarPainelCampeonato` (370–391) | `ManterCampeonato.deletar` | `DELETE FROM CAMPEONATO` | `CAMPEONATO` |
| Cadastrar equipe | `criarPainelEquipe` (516–532) | `ManterEquipe.inserir` | `INSERT INTO EQUIPE` | `EQUIPE` |
| Consultar equipe | `criarPainelEquipe` (534–553) | `ManterEquipe.pesquisarRegistro` | `SELECT ... id/nome/cidade` | `EQUIPE` |
| Alterar equipe | `criarPainelEquipe` (555–571) | `ManterEquipe.alterar` | `UPDATE EQUIPE` | `EQUIPE` |
| Excluir equipe | `criarPainelEquipe` (573–594) | `ManterEquipe.deletar` | `DELETE FROM EQUIPE` | `EQUIPE` |
| Cadastrar jogador | `criarPainelJogador` (698–712) | `ManterJogador.inserir` | `INSERT INTO JOGADOR` | `JOGADOR` |
| Consultar jogador | `criarPainelJogador` (714–733) | `ManterJogador.pesquisarRegistro` | `SELECT ... JOIN EQUIPE` | `JOGADOR` + `EQUIPE` |
| Alterar jogador | `criarPainelJogador` (735–750) | `ManterJogador.alterar` | `UPDATE JOGADOR` | `JOGADOR` |
| Excluir jogador | `criarPainelJogador` (752–774) | `ManterJogador.deletar` | `DELETE FROM JOGADOR` | `JOGADOR` |
| Cadastrar estádio | `criarPainelEstadio` (893–911) | `ManterEstadio.inserir` | `INSERT INTO ESTADIO` | `ESTADIO` |
| Consultar estádio | `criarPainelEstadio` (913–933) | `ManterEstadio.pesquisarRegistro` | `SELECT ... id/nome/cidade` | `ESTADIO` |
| Alterar estádio | `criarPainelEstadio` (935–952) | `ManterEstadio.alterar` | `UPDATE ESTADIO` | `ESTADIO` |
| Excluir estádio | `criarPainelEstadio` (954–975) | `ManterEstadio.deletar` | `DELETE FROM ESTADIO` | `ESTADIO` |
| Cadastrar árbitro | `criarPainelArbitro` (1064–1082) | `ManterArbitro.inserir` | `INSERT INTO ARBITRO` | `ARBITRO` |
| Consultar árbitro | `criarPainelArbitro` (1084–1106) | `ManterArbitro.pesquisarRegistro` | `SELECT ... id/nome/registro` | `ARBITRO` |
| Alterar árbitro | `criarPainelArbitro` (1108–1125) | `ManterArbitro.alterar` | `UPDATE ARBITRO` | `ARBITRO` |
| Excluir árbitro | `criarPainelArbitro` (1127–1148) | `ManterArbitro.deletar` | `DELETE FROM ARBITRO` | `ARBITRO` |

### 4.2 Inscrição, classificação e partidas

| Funcionalidade | Tela (método) | DAO (método) | SQL/procedure | Banco |
|---|---|---|---|---|
| Inscrever equipe | `btnInscrever` (1237–1252) | `ManterClassificacao.inscreverEquipe` | `CALL sp_inscrever_equipe` | `CLASSIFICACAO` |
| Atualizar status | `btnStatusClass` (1256–1273) | `ManterClassificacao.atualizarStatus` | `UPDATE ... status_atual` | `CLASSIFICACAO` |
| Remover inscrição | `btnDesinscrever` (1277–1299) | `ManterClassificacao.removerInscricao` | `DELETE FROM CLASSIFICACAO` | `CLASSIFICACAO` |
| Listar classificação | `carregarTabelaClassificacao` (1954–1977) | `ManterClassificacao.listarPorCampeonato` | `SELECT ... JOIN EQUIPE` | `CLASSIFICACAO` + `EQUIPE` |
| Agendar partida | `btnAgendar` (1480–1495) | `ManterPartida.inserir` | `INSERT INTO PARTIDA` | `PARTIDA` + triggers |
| Consultar partida | `btnConsultar` (1497–1516) | `ManterPartida.pesquisarRegistro` | `SELECT ... 5 JOINs` | `PARTIDA` + 5 tabelas |
| Lançar placar/alterar | `btnAtualizar` (1519–1535) | `ManterPartida.alterar` | `UPDATE PARTIDA` | `PARTIDA` + triggers |
| Excluir partida | `btnExcluir` (1538–1560) | `ManterPartida.deletar` | `DELETE FROM PARTIDA` | `PARTIDA` + trigger |
| Listar partidas | `recarregarTudo` (1936–1948) | `ManterPartida.pesquisarTudo` | `SELECT ... 5 JOINs` | `PARTIDA` + 5 tabelas |

### 4.3 Relatórios

| Funcionalidade | Tela (método) | DAO (método) | Procedure | Tabelas |
|---|---|---|---|---|
| Relatório de classificação | `gerarRelatorioClassificacao` (1739–1799) | `ManterRelatorio.classificacaoPorCampeonato` | `sp_relatorio_classificacao` | `CLASSIFICACAO`, `CAMPEONATO`, `EQUIPE`, `PARTIDA`, `ESTADIO`, `ARBITRO` |
| Partidas por período | `gerarRelatorioPartidas` (1801–1850) | `ManterRelatorio.partidasPorPeriodo` | `sp_relatorio_partidas_periodo` | `PARTIDA`, `EQUIPE` (2x), `ESTADIO`, `ARBITRO` |
| Imprimir/PDF | `btnImprimirRel` (1698–1716) | — | — | `JTable.print` |

---

## 5. “Onde encontrar…” (índice por assunto)

| Assunto | Arquivo | Linhas |
|---|---|---:|
| Conexão JDBC | `DAO.java` | 12–31 |
| Fechamento | `DAO.java` | 33–38 |
| Driver antigo | `DAO.java` | 18 |
| URL/usuário/senha | `DAO.java` | 19–21 |
| `PreparedStatement` (exemplo) | `ManterEquipe.java` | 12–17 |
| `ResultSet` / `wasNull` (gols) | `ManterPartida.java` | 174–178 |
| `ResultSet` / `wasNull` (colocação) | `ManterClassificacao.java` | 109–110 |
| `CallableStatement` | `ManterRelatorio.java` | 31–33, 78–83 |
| JOIN de jogador | `ManterJogador.java` | 19–23 |
| JOIN de partida | `ManterPartida.java` | 15–23 |
| JOIN de classificação | `ManterClassificacao.java` | 59–64, 83–91 |
| Pesquisa individual (campeonato) | `ManterCampeonato.java` | 54–89 |
| Pesquisa individual (equipe) | `ManterEquipe.java` | 49–85 |
| Pesquisa individual (jogador) | `ManterJogador.java` | 83–121 |
| Pesquisa individual (estádio) | `ManterEstadio.java` | 51–86 |
| Pesquisa individual (árbitro) | `ManterArbitro.java` | 52–87 |
| Pesquisa individual (partida) | `ManterPartida.java` | 104–145 |
| Pesquisa individual (classificação) | `ManterClassificacao.java` | 53–76 |
| Validação de campos | `TelaPrincipal.java` | 101–124 (mecanismo) |
| Validação do campeonato | `TelaPrincipal.java` | 296–308 |
| Validação do jogador | `TelaPrincipal.java` | 663–678 |
| Validação da partida | `TelaPrincipal.java` | 1420–1457 |
| Validação do relatório | `TelaPrincipal.java` | 1680–1693 |
| Listener de status da partida | `TelaPrincipal.java` | 1385–1404 |
| Cache de jogadores | `TelaPrincipal.java` | 48, 1899–1908 |
| Cache de partidas | `TelaPrincipal.java` | 75, 1936–1948 |
| Recarga geral | `TelaPrincipal.java` | 1855–1952 |
| Seleção de combo por ID | `TelaPrincipal.java` | 1981–2015 |
| Tradução de erros | `TelaPrincipal.java` | 2017–2050 |
| Impressão/PDF | `TelaPrincipal.java` | 1698–1716 |
| `main`/Nimbus/EDT | `TelaPrincipal.java` | 2060–2072 |
| Schema (tabelas) | `Champs_schema.sql` | 17–191 |
| Colunas geradas | `Champs_schema.sql` | 104–107 |
| Trigger BEFORE | `Champs_procedures_triggers.sql` | 207–230 |
| Trigger AFTER | `Champs_procedures_triggers.sql` | 232–269 |
| Regras da partida | `Champs_procedures_triggers.sql` | 138–205 |
| Sincronização | `Champs_procedures_triggers.sql` | 32–122 |
| Relatório principal | `Champs_relatorios.sql` | 25–81 |
| Relatório por período | `Champs_relatorios.sql` | 83–116 |
| Dados de demonstração | `Champs_dados.sql` | 1–183 |

---

## 6. Inventário de métodos (assinatura → propósito)

### 6.1 DAOs

| Classe | Método | Propósito |
|---|---|---|
| `DAO` | `abrirBanco()` | carregar driver e abrir conexão |
| `DAO` | `fecharBanco()` | fechar `PreparedStatement` |
| `ManterCampeonato` | `inserir(Campeonato)` | cadastrar campeonato |
| `ManterCampeonato` | `alterar(Campeonato)` | atualizar campeonato |
| `ManterCampeonato` | `deletar(Campeonato)` | excluir campeonato |
| `ManterCampeonato` | `pesquisarRegistro(String)` | buscar por ID/nome |
| `ManterCampeonato` | `pesquisarTudo()` | listar todos |
| `ManterCampeonato` | `montarCampeonato(ResultSet)` | linha → objeto |
| `ManterEquipe` | `inserir/alterar/deletar` | CRUD de equipe |
| `ManterEquipe` | `pesquisarRegistro(String)` | ID/nome/cidade |
| `ManterEquipe` | `pesquisarTudo()` | listar |
| `ManterEquipe` | `montarEquipe(ResultSet)` | linha → objeto |
| `ManterEstadio` | `inserir/alterar/deletar` | CRUD de estádio |
| `ManterEstadio` | `pesquisarRegistro(String)` | ID/nome/cidade |
| `ManterEstadio` | `pesquisarTudo()` | listar |
| `ManterEstadio` | `montarEstadio(ResultSet)` | linha → objeto |
| `ManterArbitro` | `inserir/alterar/deletar` | CRUD de árbitro |
| `ManterArbitro` | `pesquisarRegistro(String)` | ID/nome/registro |
| `ManterArbitro` | `pesquisarTudo()` | listar |
| `ManterArbitro` | `montarArbitro(ResultSet)` | linha → objeto |
| `ManterJogador` | `inserir/alterar/deletar` | CRUD de jogador |
| `ManterJogador` | `preencherParametros(Jogador)` | 5 parâmetros (data nula) |
| `ManterJogador` | `pesquisarRegistro(String)` | ID/nome/posição/equipe/camisa |
| `ManterJogador` | `pesquisarTudo()` | listar com JOIN |
| `ManterJogador` | `montarJogador(ResultSet)` | linha → objeto |
| `ManterPartida` | `inserir/alterar/deletar` | CRUD principal |
| `ManterPartida` | `preencherParametros(Partida)` | 12 parâmetros |
| `ManterPartida` | `pesquisarRegistro(String)` | ID/nomes |
| `ManterPartida` | `pesquisarTudo()` | listar com 5 JOINs |
| `ManterPartida` | `montarPartida(ResultSet)` | linha → objeto |
| `ManterClassificacao` | `inscreverEquipe(int,int)` | `CALL` da procedure |
| `ManterClassificacao` | `removerInscricao(int,int)` | DELETE |
| `ManterClassificacao` | `atualizarStatus(int,int,String)` | UPDATE do status |
| `ManterClassificacao` | `pesquisarRegistro(int,int)` | busca por chave composta |
| `ManterClassificacao` | `listarPorCampeonato(int)` | listar classificação |
| `ManterClassificacao` | `montarClassificacao(ResultSet)` | linha → objeto |
| `ManterRelatorio` | `classificacaoPorCampeonato(int)` | chamar procedure 1 |
| `ManterRelatorio` | `partidasPorPeriodo(int,String,String)` | chamar procedure 2 |
| `ManterRelatorio` | `fecharRecursos(CallableStatement)` | fechar cs/con |

### 6.2 `TelaPrincipal` — ações por aba

| Aba | Ação | Linhas |
|---:|---|---:|
| 1 | Cadastrar campeonato | 328–346 |
| 1 | Consultar campeonato | 421–440 |
| 1 | Alterar campeonato | 350–368 |
| 1 | Excluir campeonato | 370–391 |
| 1 | Limpar | 393–396 |
| 2 | Cadastrar equipe | 516–532 |
| 2 | Consultar equipe | 534–553 |
| 2 | Alterar equipe | 555–571 |
| 2 | Excluir equipe | 573–594 |
| 2 | Limpar | 596–599 |
| 3 | Cadastrar jogador | 698–712 |
| 3 | Consultar jogador | 714–733 |
| 3 | Alterar jogador | 735–750 |
| 3 | Excluir jogador | 752–774 |
| 3 | Limpar | 776–779 |
| 4 | Cadastrar estádio | 893–911 |
| 4 | Consultar estádio | 913–933 |
| 4 | Alterar estádio | 935–952 |
| 4 | Excluir estádio | 954–975 |
| 4 | Limpar | 977–980 |
| 5 | Cadastrar árbitro | 1064–1082 |
| 5 | Consultar árbitro | 1084–1106 |
| 5 | Alterar árbitro | 1108–1125 |
| 5 | Excluir árbitro | 1127–1148 |
| 5 | Limpar | 1150–1153 |
| 6 | Inscrever | 1237–1252 |
| 6 | Atualizar status | 1256–1273 |
| 6 | Remover inscrição | 1277–1299 |
| 6 | Carregar tabela | 1954–1977 |
| 7 | Agendar | 1480–1495 |
| 7 | Consultar | 1497–1516 |
| 7 | Alterar | 1519–1535 |
| 7 | Excluir | 1538–1560 |
| 7 | Limpar | 1562–1565 |
| 8 | Relatório de classificação | 1739–1799 |
| 8 | Partidas por período | 1801–1850 |
| 8 | Imprimir | 1698–1716 |
| 8 | Limpar | 1718–1723 |

---

## 7. Roteiros de leitura

### 7.1 Leitura de 30 minutos (visão geral)

1. `Champs_schema.sql` — tabelas e FKs (10 min).
2. `ManterEquipe.java` — CRUD mais curto (5 min).
3. `TelaPrincipal.criarPainelEquipe` — tela + validação + ações (10 min).
4. `Champs_procedures_triggers.sql` — as 4 regras e os triggers (5 min).

### 7.2 Leitura de 2 horas (domínio)

1. Guia do banco, seções 1–3.
2. `Champs_schema.sql` inteiro.
3. Guia do banco, seções 6–7 (procedures/triggers).
4. `Champs_procedures_triggers.sql` inteiro.
5. Guia do Java, seções 3–5 (models/DAOs).
6. `DAO.java` + `ManterEquipe` + `ManterJogador` + `ManterPartida`.
7. Guia do Java, seções 6 (TelaPrincipal).
8. `TelaPrincipal.java` por abas.
9. Guia do banco, seção 7 (relatórios) + `ManterRelatorio`.
10. `Champs_relatorios.sql`.

### 7.3 Leitura de 10 minutos antes da apresentação

1. Matriz da seção 4 deste mapa.
2. As 4 regras da partida.
3. Os 3 passos da sincronização.
4. Os 5 fluxos da seção 8 do [guia Java](02-java-e-relatorios.md#8-fluxos-de-ponta-a-ponta-resumo-técnico).
5. As limitações da seção 11 do [guia Java](02-java-e-relatorios.md#11-limitações-decisões-e-pegadinhas).

### 7.4 Por perfil

| Perfil | Foco |
|---|---|
| “Sou do banco” | `Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql`, `Champs_dados.sql` |
| “Sou do Java/DAO” | `DAO.java`, todas as `Manter...`, `ManterRelatorio` |
| “Sou da tela” | `TelaPrincipal.java` (helpers + abas 1–8) |
| “Sou dos relatórios” | `Champs_relatorios.sql`, `ManterRelatorio`, aba 8, `JTable.print` |
| “Vou apresentar tudo” | este mapa + [guia de apresentação](00-guia-de-apresentacao.md) + [simulado](04-perguntas-e-respostas.md) |

---

## 8. Checklist de navegação

- [ ] Sei abrir a classe de modelo de qualquer entidade.
- [ ] Sei abrir o DAO de qualquer entidade e localizar o `INSERT`/`SELECT`.
- [ ] Sei abrir a aba correspondente na `TelaPrincipal` e localizar o botão.
- [ ] Sei localizar a validação de cada campo.
- [ ] Sei localizar o JOIN no DAO e na procedure.
- [ ] Sei localizar os triggers no arquivo SQL.
- [ ] Sei localizar a procedure de cada relatório.
- [ ] Sei localizar a impressão/PDF.
- [ ] Sei localizar as limitações e explicá-las.
