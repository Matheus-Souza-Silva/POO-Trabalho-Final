# Guia de Apresentação - Sistema de Campeonato

Material para **todos os integrantes**. Na apresentação cada um abre o código, explica e responde a **2 perguntas** (0,5 cada) sobre **qualquer parte** do projeto. Este guia dá a visão do todo e aponta onde estudar cada assunto.

| Guia | Assunto |
|---|---|
| este arquivo | visão geral, requisitos, MVC, roteiro da demonstração, banco de perguntas |
| [`01-banco-de-dados.md`](01-banco-de-dados.md) | banco MySQL: tabelas, chaves, regras, triggers, relatório |
| `02-java-mvc-e-conexao.md` | pacotes `controle`, `entidade` (`DAO.java`) e `visao` |
| `03-cruds-simples.md` | Campeonato, Equipe, Estádio e Árbitro (inserir, consultar, alterar, excluir) |
| `04-crud-principal.md` | Inscrição, Classificação automática e Partidas (5 FKs + validações) |
| `05-relatorio-e-demonstracao.md` | *(a preencher na Fase 6 - Relatório e PDF)* |

---

## 1. O sistema em 1 minuto

- **Problema:** organizar um campeonato de futebol. É preciso saber quem joga contra quem, onde, quando, com qual árbitro, e qual é a classificação.
- **Quem usa:** o organizador do campeonato.
- **Cadastros (CRUDs simples - Fase 4):** Campeonato, Equipe, Estádio, Árbitro.
- **Rotina principal (CRUD principal - Fase 5):** **Inscrição, Classificação e Partida**. O organizador inscreve as equipes no campeonato, agenda o jogo (campeonato, rodada, mandante, visitante, estádio, árbitro, data/hora) e depois lança o resultado. A **classificação é atualizada sozinha** pelos triggers do banco.
- **Relatório (Fase 6):** partidas de um campeonato num período, com nomes, placar e situação.

---

## 2. Requisitos do enunciado → onde estão no projeto

| Requisito | Onde está | Situação |
|---|---|---|
| 4 CRUDs simples | Tabelas em `sql/Champs_schema.sql`; classes `ManterCampeonato`, `ManterEquipe`, `ManterEstadio`, `ManterArbitro` em `br.com.entidade`; abas 1 a 4 em `TelaPrincipal.java` | **pronto** |
| CRUD principal com relacionamentos | Tabela `PARTIDA` (5 FKs); regras em `sql/Champs_procedures_triggers.sql`; classes `ManterClassificacao` e `ManterPartida`; abas 5 e 6 em `TelaPrincipal.java` | **pronto** |
| Consulta Individual | Método `pesquisarRegistro(...)` em todas as classes `Manter...` conectado ao botão `Consultar` de cada aba | **pronto** |
| Validação de Campos Vazios | Método `vincularValidacao` (`DocumentListener`) em `TelaPrincipal.java` que trava os botões Cadastrar/Alterar/Excluir enquanto houver campos obrigatórios vazios | **pronto** |
| Consulta com JOIN | `sp_relatorio_partidas_periodo` em `sql/Champs_relatorios.sql`; `ManterPartida.pesquisarTudo()` (JOIN com 5 tabelas) e `ManterClassificacao.listarPorCampeonato()` (JOIN com `EQUIPE`) | **pronto** |
| Relatório da rotina principal | `sp_relatorio_partidas_periodo` (partidas por período); tela *(Fase 6)* | banco pronto |
| MySQL, tabelas, PK, FK, relacionamentos | `sql/Champs_schema.sql` (guia do banco, seção 1) | **pronto** |
| Script SQL de criação | `sql/Champs_schema.sql`, `Champs_procedures_triggers.sql`, `Champs_relatorios.sql` | **pronto** |
| Dados cadastrados para demonstração | Cadastrados pelas telas em `TelaPrincipal.java`; testes do banco em `sql/99_testes.sql` | **pronto** |
| Java + POO | Encapsulamento em `br.com.controle`, Herança (`extends DAO` e `extends JFrame`) e Polimorfismo (`@Override toString()`) | **pronto** |
| JFrame/Swing | `TelaPrincipal.java` no pacote `br.com.visao` com `JTabbedPane`, `JTable`, `JComboBox`, `JTextField`, `JButton` e `JOptionPane` | **pronto** |
| JDBC | `DAO.java` (`abrirBanco` / `fecharBanco`) e classes `Manter...` no pacote `br.com.entidade` | **pronto** |
| MVC | Pacotes `br.com.controle` (Model/Beans), `br.com.entidade` (DAO/Persistência) e `br.com.visao` (Telas Swing) | **pronto** |

---

## 3. Arquitetura MVC do projeto (Implementação Real)

