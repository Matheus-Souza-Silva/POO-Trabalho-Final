# Guia de Estudo - Banco de Dados campeonato (Integrado com o Java)

Este guia explica o banco do sistema e como ele conversa com as classes Java dos pacotes br.com.controle, br.com.entidade e br.com.visao para que qualquer integrante consiga abrir os scripts e o código no NetBeans, explicar o que cada parte faz e responder às perguntas do professor.

| Seção | Assunto | Arquivos explicados |
|---|---|---|
| 1 | Estrutura do banco (tabelas, chaves, regras) + Classes Java | sql/Champs_schema.sql + br.com.controle / br.com.entidade |
| 2 | Regras automáticas (triggers e procedures) + Tratamento no Java | sql/Champs_procedures_triggers.sql + ManterPartida / ManterClassificacao |
| 3 | Relatório e JOIN (Banco + Consultas Java) | sql/Champs_relatorios.sql + ManterPartida / ManterClassificacao |
| 4 | Como abrir e explicar cada arquivo na apresentação | SQL + Java |
| 5 | Perguntas e respostas (revisão geral SQL + JDBC) | todos |

---

## 1. Estrutura do banco e mapeamento para o Java

### 1.1 O sistema em uma frase

Um sistema para organizar campeonatos de futebol: cadastramos campeonatos, equipes, estádios e árbitros, inscrevemos as equipes num campeonato e registramos as partidas (rotina principal). A classificação é calculada automaticamente a partir dos resultados.

### 1.2 As tabelas e suas classes Java equivalentes

| Tabela MySQL | Classe Model (br.com.controle) | Classe DAO (br.com.entidade) | Aba em TelaPrincipal.java | Tipo no trabalho |
|---|---|---|---|---|
| CAMPEONATO | Campeonato.java | ManterCampeonato.java | Aba 1. Campeonatos | CRUD simples (Fase 4) |
| EQUIPE | Equipe.java | ManterEquipe.java | Aba 2. Equipes | CRUD simples (Fase 4) |
| ESTADIO | Estadio.java | ManterEstadio.java | Aba 3. Estádios | CRUD simples (Fase 4) |
| ARBITRO | Arbitro.java | ManterArbitro.java | Aba 4. Árbitros | CRUD simples (Fase 4) |
| JOGADOR | Jogador.java | ManterJogador.java | (extra/opcional) | extra (opcional) |
| CLASSIFICACAO | Classificao.java | ManterClassificacao.java | Aba 5. Inscrição & Classificação | apoio à rotina principal (Fase 5) |
| PARTIDA | Partida.java | ManterPartida.java | Aba 6. Partidas (CRUD Principal) | CRUD principal (Fase 5) |

> Operações implementadas em cada Manter: inserir(), alterar(), deletar(), pesquisarTudo() (listagem na JTable) e pesquisarRegistro() (consulta individual por ID ou Nome pelo botão Consultar).

---

### 1.3 Como as tabelas se relacionam

| Relação | Tabelas ligadas | Explicação |
|---|---|---|
| 1:N | CAMPEONATO -> PARTIDA | Um campeonato tem várias partidas (PARTIDA.id_campeonato). |
| 1:N | EQUIPE -> PARTIDA (duas vezes) | Cada partida liga duas equipes da mesma tabela EQUIPE: id_mandante (casa) e id_visitante (fora). |
| 1:N | ESTADIO -> PARTIDA | Um estádio recebe várias partidas (PARTIDA.id_estadio). |
| 1:N | ARBITRO -> PARTIDA | Um árbitro apita várias partidas (PARTIDA.id_arbitro). |
| N:N | CAMPEONATO x EQUIPE (via CLASSIFICACAO) | Um campeonato tem várias equipes e uma equipe disputa vários campeonatos. A tabela intermediária CLASSIFICACAO registra a inscrição e guarda os pontos/gols. |
| 1:N | EQUIPE -> JOGADOR | Uma equipe possui vários jogadores (JOGADOR.id_equipe). |

---

### 1.4 Chave primária (PK)

Identifica cada linha de forma única. Não repete e não pode ser nula.

- Chave Simples (AUTO_INCREMENT): Quase todas as tabelas usam um número automático (id_campeonato INT PRIMARY KEY AUTO_INCREMENT). O MySQL gera 1, 2, 3... sozinho (por isso no Java o campo txtId fica setEditable(false) e não entra no comando INSERT).
- Chave Composta: CLASSIFICACAO usa PRIMARY KEY (id_campeonato, id_equipe). Ou seja, a mesma equipe não pode ser inscrita duas vezes no mesmo campeonato, mas pode estar em campeonatos diferentes.

---

### 1.5 Chave estrangeira (FK)

É uma coluna que aponta para a chave primária de outra tabela. O banco garante que o valor existe lá. Exemplo em PARTIDA (CONSTRAINT fk_partida_arbitro FOREIGN KEY (id_arbitro) REFERENCES ARBITRO (id_arbitro) ON DELETE RESTRICT ON UPDATE CASCADE):

- No Java (TelaPrincipal.java): As 5 FKs de PARTIDA são selecionadas através de componentes JComboBox<Campeonato>, JComboBox<Equipe>, JComboBox<Estadio> e JComboBox<Arbitro>, evitando que o usuário digite um ID inexistente.
- ON DELETE RESTRICT: Não deixa apagar um árbitro, estádio ou equipe que já participou de alguma partida (erro 1451 do MySQL). O método mostrarErro(ex) da TelaPrincipal captura e exibe esse aviso num JOptionPane.
- ON DELETE CASCADE: Usado em fk_classificacao_campeonato — ao apagar um campeonato sem partidas, as inscrições dele são apagadas junto.
- ON UPDATE CASCADE / RESTRICT: Em fk_partida_mandante e fk_partida_visitante foi usado ON UPDATE RESTRICT porque essas colunas participam de um CHECK (chk_partida_times).

---

### 1.6 Regras de validação: CHECK, UNIQUE, NOT NULL, ENUM (Banco + Tela Java)

| Regra no Banco | Tabela | O que impede | Como o Java trata na tela |
|---|---|---|---|
| chk_campeonato_datas | CAMPEONATO | Terminar antes de começar (data_fim >= data_inicio) | Captura o erro e avisa no JOptionPane |
| chk_estadio_capacidade | ESTADIO | Capacidade zero ou negativa (capacidade > 0) | Conversão numérica e validação de preenchimento |
| chk_jogador_camisa | JOGADOR | Camisa fora de 1 a 99 | Check direto no banco |
| chk_classificacao_soma | CLASSIFICACAO | jogos != vitorias + empates + derrotas | Calculado pela procedure sp_sincronizar_classificacao |
| chk_classificacao_colocacao | CLASSIFICACAO | Colocação 0 ou negativa (pode ser NULL se não jogou) | Classificao.java usa Integer colocacaoEquipe |
| chk_partida_times | PARTIDA | Um time jogar contra ele mesmo (id_mandante <> id_visitante) | Bloqueado pelo banco e exibido no JOptionPane |
| chk_partida_rodada / gols | PARTIDA | Rodada <= 0, acréscimo ou gols negativos | Valores padrão válidos nos campos |
| chk_partida_placar_completo | PARTIDA | Placar pela metade (só gols de um time) | ManterPartida envia ambos os gols ou ambos NULL |
| chk_partida_placar_por_status | PARTIDA | Jogo ENCERRADA sem placar, ou AGENDADA/CANCELADA com placar | Ao mudar cbStatusPartida, limpa os gols e envia pst.setNull(8, Types.INTEGER) |
| uq_equipe_nome, uq_campeonato_nome, uq_arbitro_registro | Várias | Cadastros duplicados | Captura Duplicate entry e avisa no JOptionPane |
| uq_partida_confronto | PARTIDA | Mesmo confronto duas vezes na mesma rodada | Bloqueado pelo banco e exibido na tela |

- Validação de campos vazios no Java (vincularValidacao): A TelaPrincipal possui um DocumentListener que mantém os botões Cadastrar, Alterar e Excluir desabilitados (setEnabled(false)) enquanto os campos obrigatórios estiverem em branco.
- Colunas ENUM: No Java, todas as colunas ENUM são representadas por JComboBox<String> com as opções exatas do banco.

---

### 1.7 Colunas calculadas pelo próprio banco (GENERATED ALWAYS)

Na tabela CLASSIFICACAO:
- saldo_gols: GENERATED ALWAYS AS (gols_marcados - gols_sofridos) STORED
- pontuacao: GENERATED ALWAYS AS (vitorias * 3 + empates) STORED

O Java nunca faz INSERT ou UPDATE nessas duas colunas: o MySQL calcula sozinho e a classe ManterClassificacao apenas lê os valores prontos no SELECT (rs.getInt("saldo_gols") e rs.getInt("pontuacao")).

---

## 2. Regras automáticas (Champs_procedures_triggers.sql) e integração JDBC

### 2.1 Procedure e Trigger, em uma frase cada

- Procedure (PROCEDURE): Um bloco de comandos SQL com nome, guardado no banco, que recebe parâmetros e é chamado com CALL nome(...) (como fazemos em ManterClassificacao.inscreverEquipe()).
- Trigger (TRIGGER): Um bloco que o próprio banco executa sozinho antes (BEFORE) ou depois (AFTER) de um INSERT, UPDATE ou DELETE numa tabela.

---

### 2.2 O que acontece passo a passo quando o Java salva uma partida

1. Na Tela (TelaPrincipal - Aba 6): O usuário clica em "Registrar / Agendar" ou "Salvar Placar / Alterar". O método montarObjetoPartida() cria o objeto Partida (br.com.controle.Partida).
2. No DAO (ManterPartida extends DAO): O método inserir(p) ou alterar(p) chama abrirBanco() e executa o INSERT ou UPDATE via PreparedStatement.
3. Trigger BEFORE no MySQL (trg_partida_before_insert / update): Chama CALL sp_validar_partida(...). Se alguma regra for quebrada, dispara SIGNAL SQLSTATE '45000', cancela a gravação e devolve a mensagem para TelaPrincipal.mostrarErro(ex) exibir no JOptionPane.
4. Gravação: Se tudo estiver válido, o MySQL grava a partida e confere as FKs e os CHECKs.
5. Trigger AFTER no MySQL (trg_partida_after_insert / update / delete): Chama CALL sp_sincronizar_classificacao(id_campeonato), recalculando toda a tabela CLASSIFICACAO.
6. Atualização da Tela: O Java chama recarregarTudo() e atualiza automaticamente as JTable das Abas 5 e 6!

---

### 2.3 As 4 regras da partida (sp_validar_partida)

Partidas com status CANCELADA não são validadas e não ocupam equipe, árbitro nem estádio. Para as demais:

| # | Regra validada | Mensagem devolvida para o JOptionPane do Java | Exemplo recusado |
|---|---|---|---|
| 1 | Data dentro do período do campeonato | Data da partida fora do periodo do campeonato | Jogo em 2030 num campeonato de 2026 |
| 2 | Mandante e visitante inscritos | Equipe nao inscrita neste campeonato | Equipe cadastrada na Aba 2, mas não inscrita na Aba 5 |
| 3 | Equipe não joga duas vezes na rodada | Equipe ja possui partida nesta rodada | Tigres já jogou a rodada 1 e é escalado de novo na rodada 1 |
| 4 | Árbitro e estádio livres no horário | Arbitro ou estadio ja ocupado neste horario | Mesmo árbitro ou estádio em dois jogos às 16:00 do mesmo dia |

---

### 2.4 A classificação automática (sp_sincronizar_classificacao)

Estratégia "recalcular do zero" em 3 passos sempre que uma partida muda:
1. Zera jogos, vitórias, empates, derrotas, gols e colocação de todas as equipes inscritas no campeonato.
2. Soma as partidas ENCERRADA (contando cada equipe como mandante e como visitante via UNION ALL).
3. Calcula a colocação (1º, 2º...) usando a função de janela ROW_NUMBER() OVER (ORDER BY pontuacao DESC, vitorias DESC, saldo_gols DESC, gols_marcados DESC, id_equipe). Quem ainda não jogou fica com colocacao_equipe = NULL (exibido como "-" na tela).

---

### 2.5 Inscrição de equipes (ManterClassificacao.java)

- Inscrever (inscreverEquipe): Executa CALL sp_inscrever_equipe(?, ?), que faz INSERT IGNORE INTO CLASSIFICACAO e já chama sp_sincronizar_classificacao.
- Remover inscrição (removerInscricao): Executa DELETE FROM CLASSIFICACAO WHERE id_campeonato = ? AND id_equipe = ?. Se a equipe já tiver jogos, o trigger trg_classificacao_before_delete bloqueia com a mensagem 'Equipe possui partidas neste campeonato'.
- Atualizar Status (atualizarStatus): Executa UPDATE CLASSIFICACAO SET status_atual = ? para alternar entre A_JOGAR, CLASSIFICADO e ELIMINADO.

---

## 3. Relatório e INNER JOIN (Champs_relatorios.sql e classes Manter)

### 3.1 Onde o INNER JOIN está no projeto

O INNER JOIN aparece em 3 lugares para transformar códigos numéricos de chaves estrangeiras (id_mandante = 1) em nomes reais (Tigres FC):

1. No banco (sql/Champs_relatorios.sql): Dentro da procedure sp_relatorio_partidas_periodo.
2. Em ManterPartida.java (pesquisarTudo e pesquisarRegistro): Junta PARTIDA p com 5 tabelas (CAMPEONATO c, EQUIPE m, EQUIPE v, ESTADIO es e ARBITRO a) para preencher os nomes exibidos na JTable da Aba 6.
3. Em ManterClassificacao.java (listarPorCampeonato): Junta CLASSIFICACAO c com EQUIPE e (INNER JOIN EQUIPE e ON e.id_equipe = c.id_equipe) para exibir o nome da equipe na tabela de classificação da Aba 5.

> Por que a tabela EQUIPE aparece duas vezes no INNER JOIN da partida?
> Porque uma partida tem duas equipes diferentes: usamos o apelido m (INNER JOIN EQUIPE m ON m.id_equipe = p.id_mandante) para buscar o nome do mandante e o apelido v (INNER JOIN EQUIPE v ON v.id_equipe = p.id_visitante) para buscar o nome do visitante.

---

## 4. Como abrir e explicar cada arquivo na apresentação

### Parte 1: Banco de Dados (No MySQL Workbench)
1. Champs_schema.sql: Mostrar CREATE TABLE, as PKs AUTO_INCREMENT, a PK composta de CLASSIFICACAO, as Colunas Geradas (saldo_gols, pontuacao), as 5 FKs de PARTIDA e os CHECKs.
2. Champs_procedures_triggers.sql: Mostrar sp_validar_partida (com os 4 SIGNAL SQLSTATE '45000'), sp_sincronizar_classificacao (os 3 passos com ROW_NUMBER()) e os triggers BEFORE e AFTER.
3. Champs_relatorios.sql: Mostrar a procedure sp_relatorio_partidas_periodo com os INNER JOIN.

### Parte 2: Aplicação Java (No NetBeans)
1. Pacote br.com.controle: Abrir Arbitro.java ou Partida.java para mostrar o encapsulamento (private + get/set), o uso de Integer para aceitar null nos gols e o @Override public String toString() usado pelos JComboBox.
2. Pacote br.com.entidade:
   - Abrir DAO.java para mostrar a conexão JDBC (abrirBanco e fecharBanco).
   - Abrir ManterEquipe.java (exemplo de CRUD simples com inserir, alterar, deletar, pesquisarTudo e pesquisarRegistro).
   - Abrir ManterClassificacao.java (mostrar CALL sp_inscrever_equipe(?, ?) e o INNER JOIN com EQUIPE).
   - Abrir ManterPartida.java (mostrar preencherParametros tratando Types.INTEGER para gols nulos e o INNER JOIN com 5 tabelas).
3. Pacote br.com.visao: Abrir TelaPrincipal.java, mostrar o método vincularValidacao (que trava botões enquanto houver campos vazios) e fazer a demonstração ao vivo nas 6 abas.

---

## 5. Perguntas e respostas rápidas (Revisão Geral SQL + Java)

- 1. Quantas tabelas o banco tem e quais são os CRUDs?
  Sete tabelas. Os 4 CRUDs simples são CAMPEONATO, EQUIPE, ESTADIO e ARBITRO. O CRUD principal é PARTIDA, apoiado por CLASSIFICACAO (inscrição e pontuação automática). JOGADOR é extra.
- 2. Qual a diferença entre Chave Primária (PK) e Chave Estrangeira (FK)?
  A PK identifica unicamente cada linha da própria tabela; a FK aponta para a PK de outra tabela garantindo que o registro relacionado existe.
- 3. O que significa ON DELETE RESTRICT e ON DELETE CASCADE?
  RESTRICT impede excluir um registro pai que já possui filhos vinculados (ex: não deixa excluir uma equipe ou árbitro que já tem partida registrada). CASCADE apaga os filhos automaticamente (usado apenas nas inscrições de um campeonato ainda sem partidas).
- 4. O que é uma coluna gerada (GENERATED ALWAYS)?
  É uma coluna calculada automaticamente pelo MySQL: pontuacao = vitorias * 3 + empates e saldo_gols = gols_marcados - gols_sofridos.
- 5. Qual a diferença entre Stored Procedure e Trigger?
  A Procedure é chamada explicitamente com CALL nome(...) (como em sp_inscrever_equipe). O Trigger dispara sozinho automaticamente antes (BEFORE) ou depois (AFTER) de um INSERT, UPDATE ou DELETE na tabela.
- 6. Se o banco já valida as regras da partida com Triggers e CHECKs, o que o Java valida?
  O Java valida a interface: impede cliques em botões com campos vazios (DocumentListener no método vincularValidacao), limpa os campos de gols quando o status é AGENDADA ou CANCELADA, envia Types.INTEGER nulo pelo JDBC (pst.setNull) e captura qualquer erro do banco (SQLException) para exibir o texto do SIGNAL num JOptionPane.
- 7. Onde estão os pilares de POO (Encapsulamento, Herança e Polimorfismo) no código Java?
  Encapsulamento: atributos private com getters/setters nas classes de br.com.controle. Herança: todas as classes Manter usam extends DAO para herdar a conexão JDBC, e TelaPrincipal usa extends JFrame. Polimorfismo: sobrescrita @Override public String toString() nas entidades para exibição nos JComboBox.
- 8. Como funciona o botão Consultar de cada aba?
  Ele chama o método pesquisarRegistro da classe Manter correspondente, que executa um SELECT ... WHERE id = ? OR nome LIKE ? LIMIT 1 com PreparedStatement e preenche os campos da tela com os dados encontrados.

