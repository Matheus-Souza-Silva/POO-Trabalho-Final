# Guia de Estudo - Banco de Dados `campeonato` (Integrado com o Java)

Este guia explica o banco do sistema e como ele conversa com as classes Java dos pacotes `br.com.controle`, `br.com.entidade` e `br.com.visao` para que **qualquer integrante** consiga abrir os scripts e o código no NetBeans, explicar o que cada parte faz e responder às perguntas do professor.

| Seção | Assunto | Arquivos explicados |
|---|---|---|
| 1 | Estrutura do banco (tabelas, chaves, regras) + Classes Java correspondentes | `sql/Champs_schema.sql` + `br.com.controle` / `br.com.entidade` |
| 2 | Regras automáticas (triggers e procedures) + Tratamento no Java | `sql/Champs_procedures_triggers.sql` + `ManterPartida` / `ManterClassificacao` |
| 3 | Relatório e JOIN (Banco + Consultas Java) | `sql/Champs_relatorios.sql` + `ManterPartida` / `ManterClassificacao` |
| 4 | Como abrir e explicar cada arquivo na apresentação | SQL + Java |
| 5 | Perguntas e respostas (revisão geral SQL + JDBC) | todos |

---

## 1. Estrutura do banco e mapeamento para o Java

### 1.1 O sistema em uma frase

Um sistema para **organizar campeonatos de futebol**: cadastramos campeonatos, equipes, estádios e árbitros, inscrevemos as equipes num campeonato e registramos as **partidas** (rotina principal). A classificação é calculada automaticamente a partir dos resultados.

### 1.2 As tabelas e suas classes Java equivalentes

| Tabela MySQL | Classe Model (`br.com.controle`) | Classe DAO (`br.com.entidade`) | Aba em `TelaPrincipal.java` | Tipo no trabalho |
|---|---|---|---|---|
| `CAMPEONATO` | `Campeonato.java` | `ManterCampeonato.java` | Aba 1. Campeonatos | CRUD simples (Fase 4) |
| `EQUIPE` | `Equipe.java` | `ManterEquipe.java` | Aba 2. Equipes | CRUD simples (Fase 4) |
| `ESTADIO` | `Estadio.java` | `ManterEstadio.java` | Aba 3. Estádios | CRUD simples (Fase 4) |
| `ARBITRO` | `Arbitro.java` | `ManterArbitro.java` | Aba 4. Árbitros | CRUD simples (Fase 4) |
| `JOGADOR` | `Jogador.java` | `ManterJogador.java` | *(extra/opcional)* | extra (opcional) |
| `CLASSIFICACAO` | `Classificao.java` | `ManterClassificacao.java` | Aba 5. Inscrição & Classificação | apoio à rotina principal (Fase 5) |
| `PARTIDA` | `Partida.java` | `ManterPartida.java` | Aba 6. Partidas (CRUD Principal) | **CRUD principal (Fase 5)** |

> **Operações implementadas em cada `Manter...`:** `inserir()`, `alterar()`, `deletar()`, `pesquisarTudo()` (listagem na `JTable`) e `pesquisarRegistro()` (consulta individual por ID ou Nome pelo botão **Consultar**).

### 1.3 Como as tabelas se relacionam


CAMPEONATO
|        |
(1:N)          (1:N)
|        |
CLASSIFICACAO    PARTIDA ---- (N:1) ---- ESTADIO
(inscricao)       |   |  
|        |   |   -- (N:1) ---- ARBITRO (N:1)         |   | |   mandante visitante |     (N:1)   (N:1) |        |   | ------ EQUIPE ---- (1:N) ---- JOGADOR

Lendo o desenho:

- **Um campeonato tem várias partidas**; cada partida pertence a um campeonato (`PARTIDA.id_campeonato`).
- **Cada partida liga duas equipes**: `id_mandante` (joga em casa) e `id_visitante`. As duas colunas apontam para a **mesma tabela** `EQUIPE`.
- Cada partida acontece **em um estádio** e é apitada **por um árbitro**.
- **Campeonato × Equipe é N:N** (um campeonato tem várias equipes; uma equipe pode disputar vários campeonatos). Relação N:N sempre precisa de uma **tabela intermediária**: aqui é a `CLASSIFICACAO`, que além de registrar a inscrição guarda os números da equipe naquele campeonato.
- **Uma equipe tem vários jogadores** (`JOGADOR.id_equipe`).

### 1.4 Chave primária (PK)

Identifica **cada linha de forma única**. Não repete e não pode ser nula.

- Quase todas as tabelas usam um número automático: `id_campeonato INT PRIMARY KEY AUTO_INCREMENT`. O MySQL gera 1, 2, 3… sozinho (por isso no Java o campo `txtId...` fica `setEditable(false)` e não entra no comando `INSERT`).
- `CLASSIFICACAO` usa **chave composta**: `PRIMARY KEY (id_campeonato, id_equipe)`. Ou seja, a mesma equipe não pode ser inscrita duas vezes no mesmo campeonato, mas pode estar em campeonatos diferentes.

### 1.5 Chave estrangeira (FK)

É uma coluna que **aponta para a chave primária de outra tabela**. O banco garante que o valor existe lá. Exemplo em `PARTIDA`:

```sql
CONSTRAINT fk_partida_arbitro
    FOREIGN KEY (id_arbitro) REFERENCES ARBITRO (id_arbitro)
    ON DELETE RESTRICT ON UPDATE CASCADE

 * Na tela Java (TelaPrincipal.java), as 5 FKs de PARTIDA são selecionadas através de componentes JComboBox<Campeonato>, JComboBox<Equipe>, JComboBox<Estadio> e JComboBox<Arbitro>, evitando que o usuário digite um ID inexistente.
 * ON DELETE RESTRICT: não deixa apagar um árbitro, estádio ou equipe que já participou de alguma partida. O banco responde com o erro 1451 (Cannot delete or update a parent row), e o método mostrarErro(ex) da TelaPrincipal exibe o aviso num JOptionPane.
 * ON DELETE CASCADE (usado só em fk_classificacao_campeonato): ao apagar um campeonato sem partidas, as inscrições dele são apagadas junto.
 * ON UPDATE CASCADE: se o id do pai mudar, os filhos acompanham. Em fk_partida_mandante e fk_partida_visitante foi usado ON UPDATE RESTRICT porque essas colunas participam de um CHECK (chk_partida_times), e o MySQL não permite CASCADE em colunas usadas por CHECK.
1.6 Regras de validação: CHECK, UNIQUE, NOT NULL, ENUM (Banco + Tela Java)
| Regra no Banco | Onde | O que impede | Como o Java ajuda na tela |
|---|---|---|---|
| chk_campeonato_datas | CAMPEONATO | terminar antes de começar (data_fim >= data_inicio) | Campos de data formatados e captura de erro no JOptionPane |
| chk_estadio_capacidade | ESTADIO | capacidade zero ou negativa | Conversão numérica e validação de preenchimento |
| chk_jogador_camisa | JOGADOR | camisa fora de 1 a 99 | Check direto no banco |
| chk_classificacao_soma | CLASSIFICACAO | jogos ≠ vitórias + empates + derrotas | Calculado pela procedure sp_sincronizar_classificacao |
| chk_classificacao_colocacao | CLASSIFICACAO | colocação 0 ou negativa (pode ser nula: quem ainda não jogou) | Classificao.java usa Integer colocacaoEquipe para aceitar null |
| chk_partida_times | PARTIDA | um time jogar contra ele mesmo | Exibe aviso se selecionar a mesma equipe em Mandante e Visitante |
| chk_partida_rodada, chk_partida_acrescimo, chk_partida_gols | PARTIDA | rodada ≤ 0, acréscimo ou gols negativos | Valores padrão válidos nos campos |
| chk_partida_placar_completo | PARTIDA | placar pela metade (só gols do mandante) | ManterPartida envia ambos os gols ou ambos NULL |
| chk_partida_placar_por_status | PARTIDA | jogo encerrado sem placar, ou agendado/cancelado com placar | Ao mudar cbStatusPartida na tela, limpa os gols se AGENDADA/CANCELADA e usa pst.setNull(8, Types.INTEGER) |
| uq_equipe_nome, uq_campeonato_nome, uq_arbitro_registro, uq_estadio_nome_cidade | várias tabelas | cadastros duplicados | Captura Duplicate entry e avisa no JOptionPane |
| uq_partida_confronto | PARTIDA | o mesmo confronto duas vezes na mesma rodada | Bloqueado pelo banco e exibido na tela |
 * Validação de campos vazios no Java (vincularValidacao): além do NOT NULL do banco, a TelaPrincipal possui um DocumentListener que mantém os botões Cadastrar, Alterar e Excluir desabilitados (setEnabled(false)) enquanto os campos obrigatórios estiverem em branco.
 * ENUM: no Java, todas as colunas ENUM (status do campeonato, categoria do árbitro, status_atual da classificação, status e tempo_partida da partida) são representadas por JComboBox<String> com as opções exatas do banco.
1.7 Colunas calculadas pelo próprio banco
Em CLASSIFICACAO:
saldo_gols INT GENERATED ALWAYS AS (gols_marcados - gols_sofridos) STORED,
pontuacao  INT GENERATED ALWAYS AS (vitorias * 3 + empates) STORED,

O Java nunca faz INSERT ou UPDATE nessas duas colunas: o MySQL calcula sozinho e a classe ManterClassificacao apenas lê os valores prontos no SELECT (rs.getInt("saldo_gols") e rs.getInt("pontuacao")).
2. Regras automáticas (Champs_procedures_triggers.sql) e integração JDBC
2.1 Procedure e trigger, em uma frase cada
 * Procedure (PROCEDURE): um bloco de comandos SQL com nome, guardado no banco, que pode receber parâmetros e é chamado com CALL nome(...) (como fazemos em ManterClassificacao.inscreverEquipe()).
 * Trigger (TRIGGER): um bloco que o próprio banco executa sozinho quando acontece um INSERT, UPDATE ou DELETE numa tabela (como quando ManterPartida grava ou altera um jogo).
2.2 O que acontece passo a passo quando o Java salva uma partida
TelaPrincipal (Aba 6): clique em "Registrar / Agendar" ou "Salvar Placar / Alterar"
   |  montarObjetoPartida() cria o objeto Partida (br.com.controle.Partida)
   v
ManterPartida.inserir(p) / alterar(p) (br.com.entidade.ManterPartida extends DAO)
   |  abrirBanco() -> PreparedStatement: INSERT INTO PARTIDA ... (ou UPDATE)
   v
trg_partida_before_insert / trg_partida_before_update (no MySQL)
   |  CALL sp_validar_partida(...)
   |  regra quebrada? -> SIGNAL SQLSTATE '45000' -> SQLException volta para TelaPrincipal.mostrarErro(ex)
   v
MySQL grava a partida (e confere as FKs e os CHECKs)
   |
   v
trg_partida_after_insert / trg_partida_after_update / trg_partida_after_delete
   |  CALL sp_sincronizar_classificacao(id_campeonato)
   v
CLASSIFICACAO atualizada no banco -> Java chama recarregarTudo() e atualiza as JTables das Abas 5 e 6!

2.3 As regras da partida (sp_validar_partida)
| # | Regra | Mensagem devolvida para o JOptionPane do Java | Exemplo recusado |
|---|---|---|---|
| 1 | data dentro do período do campeonato | Data da partida fora do periodo do campeonato | jogo em 2030 num campeonato de 2026 |
| 2 | mandante e visitante inscritos | Equipe nao inscrita neste campeonato | equipe cadastrada na Aba 2, mas não inscrita na Aba 5 |
| 3 | equipe não joga duas vezes na rodada | Equipe ja possui partida nesta rodada | Tigres já jogou a rodada 1 e é escalado de novo na rodada 1 |
| 4 | árbitro e estádio livres no horário | Arbitro ou estadio ja ocupado neste horario | mesmo árbitro ou estádio em dois jogos às 16:00 do mesmo dia |
2.4 A classificação automática (sp_sincronizar_classificacao)
 * Zera jogos, vitórias, empates, derrotas, gols e colocação de todas as equipes inscritas no campeonato.
 * Soma as partidas ENCERRADA (como mandante e como visitante via UNION ALL).
 * Calcula a colocação com ROW_NUMBER() OVER (ORDER BY pontuacao DESC, vitorias DESC, saldo_gols DESC, gols_marcados DESC, id_equipe). Quem ainda não jogou fica com colocacao_equipe = NULL (que a TelaPrincipal exibe como "-" na tabela).
2.5 Inscrição de equipes (ManterClassificacao.java)
 * Inscrever (inscreverEquipe): executa CALL sp_inscrever_equipe(?, ?), que faz INSERT IGNORE INTO CLASSIFICACAO e já chama sp_sincronizar_classificacao.
 * Remover inscrição (removerInscricao): executa DELETE FROM CLASSIFICACAO WHERE id_campeonato = ? AND id_equipe = ?. Se a equipe já tiver jogos, o trigger trg_classificacao_before_delete dispara SIGNAL SQLSTATE '45000' com a mensagem 'Equipe possui partidas neste campeonato', bloqueando a exclusão.
 * Atualizar Status (atualizarStatus): executa UPDATE CLASSIFICACAO SET status_atual = ? para mudar manualmente entre A_JOGAR, CLASSIFICADO e ELIMINADO.
3. Relatório e JOIN (Champs_relatorios.sql e classes Manter...)
3.1 Onde o INNER JOIN está no projeto
O INNER JOIN está presente em 3 lugares para transformar códigos de chaves estrangeiras em nomes legíveis:
 * No banco (sql/Champs_relatorios.sql): dentro da procedure sp_relatorio_partidas_periodo.
 * Em ManterPartida.java (pesquisarTudo e pesquisarRegistro): junta PARTIDA p com 5 tabelas (CAMPEONATO c, EQUIPE m, EQUIPE v, ESTADIO es e ARBITRO a) para preencher os atributos nomeCampeonato, nomeMandante, nomeVisitante, nomeEstadio e nomeArbitro do objeto Partida.
 * Em ManterClassificacao.java (listarPorCampeonato): junta CLASSIFICACAO c com EQUIPE e (INNER JOIN EQUIPE e ON e.id_equipe = c.id_equipe) para mostrar o nome da equipe na tabela de classificação.
SELECT p.*, c.nome AS nome_camp, m.nome AS nome_mand,
       v.nome AS nome_vis, es.nome AS nome_est, a.nome AS nome_arb
  FROM PARTIDA p
 INNER JOIN CAMPEONATO c  ON c.id_campeonato = p.id_campeonato
 INNER JOIN EQUIPE     m  ON m.id_equipe     = p.id_mandante
 INNER JOIN EQUIPE     v  ON v.id_equipe     = p.id_visitante
 INNER JOIN ESTADIO    es ON es.id_estadio   = p.id_estadio
 INNER JOIN ARBITRO    a  ON a.id_arbitro    = p.id_arbitro

 * Por que EQUIPE aparece duas vezes? Porque uma partida tem duas equipes: usamos o apelido m para buscar o nome do id_mandante e v para o id_visitante.
4. Como abrir e explicar cada arquivo na apresentação
Parte Banco de Dados (No MySQL Workbench)
 * Champs_schema.sql: mostrar CREATE TABLE, as PKs AUTO_INCREMENT, a PK composta de CLASSIFICACAO, as Colunas Geradas (saldo_gols, pontuacao), as 5 FKs de PARTIDA e os CHECKs.
 * Champs_procedures_triggers.sql: mostrar sp_validar_partida (com os 4 SIGNAL SQLSTATE '45000'), sp_sincronizar_classificacao (os 3 passos com ROW_NUMBER()) e os triggers BEFORE e AFTER.
 * Champs_relatorios.sql: mostrar a procedure sp_relatorio_partidas_periodo com os INNER JOIN.
Parte Java (No NetBeans)
 * Pacote br.com.controle: abrir Arbitro.java ou Partida.java para mostrar o encapsulamento (private + get/set), o uso de Integer para aceitar null nos gols e o @Override public String toString() usado pelos JComboBox.
 * Pacote br.com.entidade:
   * Abrir DAO.java para mostrar a conexão JDBC (abrirBanco e fecharBanco).
   * Abrir ManterEquipe.java (exemplo de CRUD simples com inserir, alterar, deletar, pesquisarTudo e pesquisarRegistro).
   * Abrir ManterClassificacao.java (mostrar CALL sp_inscrever_equipe(?, ?) e o INNER JOIN com EQUIPE).
   * Abrir ManterPartida.java (mostrar preencherParametros tratando Types.INTEGER para gols nulos e o INNER JOIN com 5 tabelas).
 * Pacote br.com.visao: abrir TelaPrincipal.java, mostrar o método vincularValidacao (que trava botões enquanto houver campos vazios) e fazer a demonstração ao vivo nas 6 abas.
5. Perguntas e respostas rápidas (SQL + Java)
1. Se o banco já valida as regras da partida com Triggers e CHECKs, o que o Java valida?
O Java valida a interface: impede cliques em botões com campos vazios (DocumentListener no método vincularValidacao), limpa automaticamente os campos de gols quando o status é AGENDADA ou CANCELADA, envia Types.INTEGER nulo pelo JDBC (pst.setNull) e captura qualquer erro de regra do banco (SQLException) para exibir o texto exato do SIGNAL num JOptionPane.
2. Onde está a herança no projeto Java?
Todas as classes Manter... do pacote br.com.entidade herdam da classe DAO (extends DAO), reaproveitando os objetos con, pst, rs e os métodos abrirBanco() e fecharBanco(). E a TelaPrincipal herda de javax.swing.JFrame.
3. Como funciona o botão Consultar de cada aba?
Ele chama o método pesquisarRegistro da classe Manter correspondente, que executa um SELECT ... WHERE id = ? OR nome LIKE ? LIMIT 1, retorna o objeto preenchido e carrega os dados diretamente nos campos da tela para edição ou exclusão.

