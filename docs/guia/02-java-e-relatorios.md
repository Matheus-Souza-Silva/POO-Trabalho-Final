# Guia de Estudo - Java, CRUDs e Relatórios

Este guia explica a parte Java do **Champs** e os relatórios, para que qualquer integrante consiga abrir o código, apontar as classes e responder às perguntas da apresentação.

> Guias relacionados: [visão geral e apresentação](00-guia-de-apresentacao.md) e [banco de dados](01-banco-de-dados.md).

---

## 1. O caminho de um clique

```
TelaPrincipal (Swing)
   │  lê os campos e valida
   ▼
Manter... (DAO, pacote br.com.entidade)
   │  monta o SQL com PreparedStatement
   ▼
DAO (conexão JDBC)
   │
   ▼
MySQL  ──►  devolve ResultSet ou erro
   │
   ▼
Manter... monta o objeto (Campeonato, Equipe, Partida...)
   │
   ▼
TelaPrincipal atualiza a JTable ou mostra JOptionPane
```

Exemplo real (cadastro de equipe):

```java
// 1. a tela monta o objeto
Equipe eq = new Equipe();
eq.setNome(txtNomeEquipe.getText().trim());
eq.setCidade(txtCidadeEquipe.getText().trim());

// 2. o DAO grava
daoEquipe.inserir(eq);

// 3. a tela recarrega as tabelas
recarregarTudo();
```

### Pacotes do projeto

| Pacote | O que contém | Exemplos |
|---|---|---|
| `br.com.controle` | classes de dados (Model) e classes usadas nos relatórios | `Campeonato`, `Equipe`, `Jogador`, `Estadio`, `Arbitro`, `Partida`, `Classificacao`, `RelatorioClassificacao`, `RelatorioPartida` |
| `br.com.entidade` | conexão e acesso ao banco (DAO) | `DAO`, `ManterCampeonato`, `ManterEquipe`, `ManterJogador`, `ManterEstadio`, `ManterArbitro`, `ManterClassificacao`, `ManterPartida`, `ManterRelatorio` |
| `br.com.visao` | interface gráfica (View) | `TelaPrincipal` |

> A classe de classificação foi renomeada para `Classificacao` (sem o erro de digitação antigo).
>
> A `TelaPrincipal` é escrita em código Java. O arquivo `TelaPrincipal.form` foi removido para o NetBeans abrir o arquivo como **fonte**, sem tentar carregar um editor visual que não corresponde ao código. Os antigos `Cadastro`, `Alterar`, `Deletar` e `Listar` são rascunhos não usados.

---

## 2. Conexão JDBC (`DAO`)

A classe `DAO` guarda a conexão e os objetos JDBC usados pelos filhos:

```java
Connection con;
PreparedStatement pst;
ResultSet rs;
```

Cada `Manter...` **herda** de `DAO` (`extends DAO`). Por isso consegue chamar `abrirBanco()`, usar `pst` e fechar no `finally`:

```java
public void inserir(Equipe e) throws Exception {
    try {
        abrirBanco();
        String query = "INSERT INTO EQUIPE (nome, cidade) VALUES (?, ?)";
        pst = (PreparedStatement) con.prepareStatement(query);
        pst.setString(1, e.getNome());
        pst.setString(2, e.getCidade());
        pst.execute();
    } finally {
        fecharBanco();
    }
}
```

Pontos importantes para a apresentação:

- **`PreparedStatement` com `?`**: os valores vão separados do SQL. Isso evita erro com aspas e impede SQL Injection.
- **`execute()` / `executeUpdate()` / `executeQuery()`**: o primeiro serve para INSERT/DELETE, o segundo para UPDATE (retorna quantas linhas mudaram) e o terceiro para SELECT (retorna `ResultSet`).
- **`finally`**: garante que o banco seja fechado mesmo quando ocorre uma exceção.
- **`try/catch` na tela**: o erro do banco chega como `Exception`/`SQLException` e é mostrado no `JOptionPane` pelo método `mostrarErro`.

---

## 3. Pesquisa individual (botão Consultar)

Cada CRUD tem um campo **Pesquisar** e um botão **Consultar**. O botão chama o método `pesquisarRegistro(...)` da classe `Manter...` correspondente.

| Entidade | Método | O que aceita |
|---|---|---|
| Campeonato | `pesquisarRegistro(String)` | id ou parte do nome |
| Equipe | `pesquisarRegistro(String)` | id, parte do nome ou da cidade |
| Jogador | `pesquisarRegistro(String)` | id, nome, posição, nome da equipe ou número da camisa |
| Estádio | `pesquisarRegistro(String)` | id, parte do nome ou da cidade |
| Árbitro | `pesquisarRegistro(String)` | id, parte do nome ou do registro na federação |
| Partida | `pesquisarRegistro(String)` | id, equipe, campeonato, estádio ou árbitro |
| Classificação | `pesquisarRegistro(int idCampeonato, int idEquipe)` | chave composta (campeonato + equipe) |

### Como o SQL funciona

Exemplo do árbitro:

```sql
SELECT id_arbitro, nome, registro_federacao, categoria
  FROM ARBITRO
 WHERE id_arbitro = ? OR nome LIKE ? OR registro_federacao LIKE ?
 ORDER BY (id_arbitro = ?) DESC, nome
 LIMIT 1;
```

- `LIKE '%texto%'` procura o texto em qualquer posição.
- `ORDER BY (id_arbitro = ?) DESC` coloca primeiro o resultado cujo id é exatamente o digitado.
- `LIMIT 1` evita devolver várias linhas para o formulário.
- Se o texto não for número, o código usa `-1` no lugar do id; assim a comparação numérica não encontra ninguém e a busca fica só por nome/registro.

### Como o botão preenche a tela

```java
btnConsultar.addActionListener(e -> {
    String termo = txtBuscaArbitro.getText().trim();
    if (termo.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Digite o ID, nome ou registro...");
        return;
    }
    try {
        Arbitro a = daoArbitro.pesquisarRegistro(termo);
        if (a != null) {
            txtIdArbitro.setText(String.valueOf(a.getIdArbitro()));
            txtNomeArbitro.setText(a.getNome());
            txtRegArbitro.setText(a.getRegistro_federacao());
            cbCatArbitro.setSelectedItem(a.getCategoria());
        } else {
            JOptionPane.showMessageDialog(this, "Nenhum árbitro encontrado para: " + termo);
        }
    } catch (Exception ex) {
        mostrarErro(ex);
    }
});
```

O método devolve o **objeto pronto**; a tela só copia os atributos para os campos. Foi a adaptação do modelo `PesquisarRegistro(Objeto)` do enunciado: em vez de o DAO alterar o objeto recebido, ele devolve o objeto encontrado (ou `null`). O efeito para o usuário é o mesmo: o formulário é preenchido com o registro do banco.

---

## 4. Validação dos botões

A tela usa três recursos do Swing:

1. **`DocumentListener`** nos `JTextField`: dispara sempre que o texto muda.
2. **`ActionListener`** nos `JComboBox`: dispara quando muda a seleção.
3. **`ListSelectionListener`** nas `JTable`: dispara quando uma linha é selecionada.

O método `vincularValidacao` registra o mesmo validador em todos os campos:

```java
private void vincularValidacao(Runnable validador, JTextField... campos) {
    DocumentListener dl = new DocumentListener() {
        public void insertUpdate(DocumentEvent e) { validador.run(); }
        public void removeUpdate(DocumentEvent e) { validador.run(); }
        public void changedUpdate(DocumentEvent e) { validador.run(); }
    };
    for (JTextField campo : campos) {
        campo.getDocument().addDocumentListener(dl);
    }
    validador.run();   // executa uma vez ao montar a tela
}
```

Exemplo do campeonato:

```java
Runnable validar = () -> {
    boolean camposOk = !vazio(txtNomeCamp.getText())
            && dataValida(txtInicioCamp.getText())
            && dataValida(txtFimCamp.getText())
            && !Date.valueOf(txtFimCamp.getText()).before(Date.valueOf(txtInicioCamp.getText()));
    boolean temId = !vazio(txtIdCamp.getText());
    btnSalvar.setEnabled(camposOk);
    btnConsultar.setEnabled(!vazio(txtBuscaCamp.getText()));
    btnAlterar.setEnabled(camposOk && temId);
    btnDeletar.setEnabled(temId);
};
vincularValidacao(validar, txtIdCamp, txtNomeCamp, txtInicioCamp, txtFimCamp, txtBuscaCamp);
```

### Regras implementadas

| Onde | Regra |
|---|---|
| Campeonato | nome preenchido, datas válidas e `data_fim >= data_inicio` |
| Equipe | nome e cidade preenchidos |
| Jogador | nome preenchido; camisa inteira entre 1 e 99; equipe escolhida; nascimento vazio ou data válida que não esteja no futuro |
| Estádio | nome e cidade preenchidos; capacidade inteira > 0 |
| Árbitro | nome e registro preenchidos |
| Partida | campeonato/equipes/estádio/árbitro escolhidos; rodada > 0; data/hora válida; acréscimo e gols >= 0; mandante ≠ visitante; placar completo quando `ENCERRADA`; nenhum gol quando `AGENDADA`/`CANCELADA` |
| Inscrição | campeonato e equipe escolhidos para inscrever; linha selecionada para atualizar status/remover |
| Relatórios | campeonato escolhido; datas válidas para o relatório de partidas |

Além disso:

- **Excluir** e **remover inscrição** pedem confirmação (`JOptionPane.showConfirmDialog`).
- O botão **Limpar** limpa os campos, a pesquisa e a seleção da tabela.
- O botão **Consultar** só habilita quando há texto no campo de pesquisa.
- A validação da tela é a primeira barreira; os **triggers do banco** continuam valendo mesmo para quem alterar os dados pelo Workbench.

### 4.1 CRUD extra de Jogador

O jogador é o CRUD extra que liga uma pessoa a uma equipe:

| Campo Java | Coluna MySQL | Regra |
|---|---|---|
| `nome` | `nome` | obrigatório |
| `posicao` | `posicao` | escolhida no `JComboBox` (goleiro, zagueiro, lateral, volante, meia, atacante) |
| `numeroCamisa` | `numero_camisa` | inteiro de 1 a 99 |
| `dataNascimento` | `data_nascimento` | opcional; quando preenchida, data válida e não futura |
| `idEquipe` | `id_equipe` | FK obrigatória para `EQUIPE` |

- **`uq_jogador_camisa`** (`id_equipe`, `numero_camisa`) impede dois jogadores com a mesma camisa na mesma equipe. O erro do banco é traduzido na tela: “Já existe um jogador com esse número de camisa nessa equipe.”
- **`fk_jogador_equipe`** usa `ON DELETE RESTRICT`: uma equipe que possui jogadores não pode ser excluída enquanto eles existirem.
- O `ManterJogador` faz `INNER JOIN EQUIPE` para trazer `nomeEquipe`, que aparece na tabela e no `toString()` do jogador.
- O botão **Consultar** procura por id, nome, posição, nome da equipe ou camisa; o formulário é preenchido com os dados retornados.

### 4.2 IDs, AUTO_INCREMENT e ordem das tabelas

- O `ID` é **gerado pelo banco** (`AUTO_INCREMENT`). Por isso o campo aparece como **“ID (automático)”**, fica não editável e é preenchido somente depois de cadastrar ou consultar um registro.
- Não se deve digitar o ID no cadastro: o MySQL escolhe o próximo número. Se um registro é excluído, o número não é reaproveitado — podem existir “buracos” na sequência, o que é normal.
- As listagens dos CRUDs usam `ORDER BY id_...` para mostrar as linhas na ordem dos IDs. A **classificação** é a exceção: ela é ordenada por colocação/pontos, que é o sentido do relatório.
- A busca individual aceita o ID digitado, mas quem gera o ID continua sendo o banco.

---

## 5. O CRUD principal (Partida)

A partida depende de cinco tabelas (campeonato, mandante, visitante, estádio e árbitro). Os combos são preenchidos por `recarregarTudo()`:

```java
for (Campeonato c : camps) {
    cbCampPartida.addItem(c);
}
for (Equipe eq : equipes) {
    cbMandante.addItem(eq);
    cbVisitante.addItem(eq);
}
```

Os objetos aparecem no combo por causa do `toString()`:

```java
@Override
public String toString() {
    return idCampeonato + " - " + nome + " [" + status + "]";
}
```

Ao salvar, `montarObjetoPartida()` lê os combos e os campos:

```java
Partida p = new Partida();
p.setIdCampeonato(((Campeonato) cbCampPartida.getSelectedItem()).getIdCampeonato());
p.setRodada(Integer.parseInt(txtRodadaPartida.getText().trim()));
p.setIdMandante(((Equipe) cbMandante.getSelectedItem()).getIdEquipe());
p.setIdVisitante(((Equipe) cbVisitante.getSelectedItem()).getIdEquipe());
p.setIdEstadio(((Estadio) cbEstadioPartida.getSelectedItem()).getIdEstadio());
p.setIdArbitro(((Arbitro) cbArbitroPartida.getSelectedItem()).getIdArbitro());
```

A listagem usa `INNER JOIN` para mostrar **nomes** em vez de códigos:

```sql
SELECT p.*, c.nome AS nome_camp, m.nome AS nome_mand,
       v.nome AS nome_vis, es.nome AS nome_est, a.nome AS nome_arb
  FROM PARTIDA p
 INNER JOIN CAMPEONATO c ON c.id_campeonato = p.id_campeonato
 INNER JOIN EQUIPE m     ON m.id_equipe     = p.id_mandante
 INNER JOIN EQUIPE v     ON v.id_equipe     = p.id_visitante
 INNER JOIN ESTADIO es   ON es.id_estadio   = p.id_estadio
 INNER JOIN ARBITRO a    ON a.id_arbitro    = p.id_arbitro
 ORDER BY p.id_partida;
```

---

## 6. Relatórios

### 6.1 Relatório principal: classificação

O relatório principal é o da **classificação**, porque ela é a entidade que conecta o campeonato, as equipes e as partidas. A procedure `sp_relatorio_classificacao` está em `SQL_Champs/Champs_relatorios.sql`:

```sql
CALL sp_relatorio_classificacao(1);
```

Ela devolve, para cada equipe inscrita:

- `posicao`, `equipe`, `cidade`;
- `pontos`, `jogos`, `vitorias`, `empates`, `derrotas`;
- `gols_pro`, `gols_contra`, `saldo_gols`;
- `aproveitamento` (`pontos / (jogos * 3) * 100`);
- `jogos_casa`, `jogos_fora`;
- `estadios` e `arbitros` que a equipe encontrou (subconsultas com `INNER JOIN` em `PARTIDA` + `ESTADIO` e `PARTIDA` + `ARBITRO`);
- `status_atual`.

Assim, o relatório usa **todas as tabelas da rotina principal**: `CLASSIFICACAO`, `CAMPEONATO`, `EQUIPE`, `PARTIDA`, `ESTADIO` e `ARBITRO`.

### 6.2 Relatório de partidas por período

```sql
CALL sp_relatorio_partidas_periodo(1, '2026-09-01', '2026-09-30');
```

Mostra rodada, data/hora formatada, mandante, visitante, estádio, árbitro, placar (`2 x 1`, `- x -` quando não há placar) e status. O filtro `DATE(data_partida) BETWEEN ...` inclui o último dia inteiro.

### 6.3 Como o Java chama a procedure

A classe `ManterRelatorio` usa `CallableStatement`:

```java
cs = con.prepareCall("{call sp_relatorio_classificacao(?)}");
cs.setInt(1, idCampeonato);
rs = cs.executeQuery();
```

Para o relatório de partidas:

```java
cs = con.prepareCall("{call sp_relatorio_partidas_periodo(?, ?, ?)}");
cs.setInt(1, idCampeonato);
cs.setDate(2, java.sql.Date.valueOf(dataInicio));
cs.setDate(3, java.sql.Date.valueOf(dataFim));
rs = cs.executeQuery();
```

Cada linha do `ResultSet` vira um objeto `RelatorioClassificacao` ou `RelatorioPartida`, e a tela monta a `JTable`.

### 6.4 Impressão e PDF

O botão **Imprimir / Salvar em PDF** usa:

```java
tabela.print(JTable.PrintMode.FIT_WIDTH,
        new MessageFormat("Champs - Classificação"),
        new MessageFormat("Página {0}"));
```

O Swing abre a janela de impressão do sistema; nela é possível escolher a impressora ou **salvar como PDF**.

---

## 7. Como executar e testar

### Banco

No MySQL Workbench, na ordem:

```
SQL_Champs/Champs_schema.sql
SQL_Champs/Champs_procedures_triggers.sql
SQL_Champs/Champs_relatorios.sql
```

Depois, para testar:

```bash
bash SQL_Champs/testar.sh
```

Sem Docker, rode o `SQL_Champs/99_testes.sql` no Workbench. O resultado esperado é:

- todos os testes de classificação e relatório com `OK`;
- exatamente os comandos marcados `ESPERADO ERRO` falhando;
- nenhum `FALHOU`.

### Aplicação

1. Abra `Java_Champs` no NetBeans.
2. Adicione o `mysql-connector-j-9.3.0.jar` nas bibliotecas.
3. Confira a conexão em `br.com.entidade.DAO` (`localhost:3306`, banco `campeonato`, usuário `root`, senha vazia).
4. Execute `br.com.visao.TelaPrincipal`.

Pela linha de comando:

```powershell
javac -encoding UTF-8 -d build/classes -cp "mysql-connector-j-9.3.0.jar" (Get-ChildItem src -Recurse -Filter *.java).FullName
java -cp "build/classes;mysql-connector-j-9.3.0.jar" br.com.visao.TelaPrincipal
```

---

## 8. Roteiro da demonstração

1. **Campeonato:** cadastre um campeonato e mostre que o botão **Cadastrar** fica desabilitado enquanto o nome está vazio; tente uma data final anterior à inicial.
2. **Equipes, jogadores, estádios e árbitros:** cadastre os registros. Mostre a **pesquisa individual** (digite parte do nome no campo Pesquisar e clique em Consultar).
3. **Inscrição:** inscreva as equipes no campeonato e mostre a classificação zerada.
4. **Partida:** agende um jogo. Tente salvar com o mesmo time como mandante e visitante (o botão fica desabilitado). Tente uma partida fora do período do campeonato e mostre a mensagem do banco.
5. **Placar:** lance o resultado de uma partida e mostre a classificação recalculada.
6. **Relatório principal:** abra **8. Relatórios**, escolha o campeonato e clique em **Relatório de Classificação**. Mostre as colunas de pontos, saldo, aproveitamento, estádios e árbitros.
7. **Partidas por período:** gere o outro relatório e use **Imprimir / Salvar em PDF**.

---

## 9. Perguntas prováveis

**Onde está o MVC no projeto?**
`br.com.controle` tem as classes de dados (Model), `br.com.entidade` tem os DAOs e a conexão, e `br.com.visao` tem a tela Swing. A tela não contém SQL: ela chama os métodos `inserir`, `alterar`, `deletar`, `pesquisarTudo` e `pesquisarRegistro`.

**Como funciona a pesquisa individual?**
O botão Consultar lê o campo Pesquisar e chama `pesquisarRegistro(...)`. O DAO monta um `SELECT ... WHERE id = ? OR nome LIKE ?`, devolve o objeto encontrado e a tela preenche os campos. Se não encontrar, mostra uma mensagem e devolve `null`.

**Por que os botões ficam desabilitados?**
Para evitar que o usuário tente cadastrar/alterar/excluir com campo vazio ou inválido. O `DocumentListener` reavalia as condições a cada tecla; os combos e a seleção da tabela também disparam a validação.

**Qual a diferença entre a validação da tela e a do banco?**
A tela dá resposta rápida e melhora a usabilidade. O banco é a última barreira: os triggers valem para qualquer programa e para alterações feitas direto no Workbench.

**O que acontece quando lanço o placar de uma partida?**
O Java faz um `UPDATE` em `PARTIDA`; o trigger `BEFORE` valida; o trigger `AFTER` chama `sp_sincronizar_classificacao`, que recalcula jogos, vitórias, empates, derrotas, gols, pontos e colocação.

**Por que o relatório principal usa a classificação?**
Porque a classificação é a entidade que liga campeonato e equipes e é alimentada pelas partidas. A procedure parte dela e faz JOIN/subconsultas nas demais tabelas da rotina principal, mostrando dados compreensíveis (nomes em vez de códigos).

**Por que usar procedure no relatório?**
Porque o relatório recebe parâmetros escolhidos na tela (campeonato e período) e fica centralizado no banco, podendo ser testado pelo Workbench e reaproveitado pelo Java.

**Como o Java lê a procedure?**
Com `CallableStatement`: `con.prepareCall("{call nome(?, ?)}")`, `setInt`/`setDate` nos parâmetros e `executeQuery()` para ler o `ResultSet`.

**Como gerar PDF?**
O botão de impressão chama `JTable.print()`. A janela de impressão do sistema oferece a opção de salvar como PDF.

**Por que `EQUIPE` aparece duas vezes no JOIN de partidas?**
Porque uma partida tem duas equipes: o mandante (`m`) e o visitante (`v`). Cada `INNER JOIN` busca uma delas com um apelido diferente.

**Como o Jogador se relaciona com a Equipe?**
`JOGADOR.id_equipe` é uma FK para `EQUIPE.id_equipe` (relação 1:N: uma equipe tem vários jogadores). O `ManterJogador` faz `INNER JOIN EQUIPE` para mostrar o nome da equipe; o `UNIQUE (id_equipe, numero_camisa)` impede camisas repetidas na mesma equipe.

**O que é `PreparedStatement` e por que usar `?`?**
É a forma de enviar o SQL com espaços reservados. Os valores são enviados separadamente, o que evita problemas com aspas e ataques de SQL Injection.

**Onde está a herança no projeto?**
As classes `Manter...` herdam de `DAO` (reaproveitam conexão e métodos) e a `TelaPrincipal` herda de `JFrame` (herança da biblioteca Swing).

**Onde está o encapsulamento?**
Nos modelos (`Campeonato`, `Equipe`, `Jogador`, `Partida` etc.), com atributos `private` e métodos `get`/`set`.

**Para que serve o `toString()`?**
Para o `JComboBox` mostrar `1 - Tigres FC (Sao Paulo)` em vez do endereço de memória do objeto.
