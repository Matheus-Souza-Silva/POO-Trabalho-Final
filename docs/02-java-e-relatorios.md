# Guia do Java, Swing, DAOs e Relatórios

> **Objetivo:** explicar a parte Java **classe por classe e método por método**, para que qualquer integrante consiga abrir o código, apontar a responsabilidade de cada arquivo e responder perguntas do professor sobre qualquer detalhe.
>
> Guias relacionados: [visão geral](00-guia-de-apresentacao.md) · [banco de dados](01-banco-de-dados.md) · [mapa do código](03-mapa-do-codigo.md) · [simulado](04-perguntas-e-respostas.md).

**Índice:** [1. Ambiente](#1-ambiente-configuração-e-execução) · [2. Pacotes](#2-estrutura-dos-pacotes) · [3. Model](#3-as-classes-de-modelo) · [4. DAO](#4-a-classe-dao-conexão-jdbc) · [5. Manter...](#5-as-classes-manter-daos) · [6. TelaPrincipal](#6-a-telaprincipal-view) · [7. Relatórios](#7-relatórios-do-botão-ao-pdf) · [8. Fluxos](#8-fluxos-de-ponta-a-ponta-resumo-técnico) · [9. Validação](#9-matriz-de-validação-tela-e-banco) · [10. Erros](#10-catálogo-de-mensagens-e-erros) · [11. Limitações](#11-limitações-decisões-e-pegadinhas) · [12. Perguntas](#12-perguntas-e-respostas-javaswingjdbc) · [13. Checklist](#13-checklist-do-java)

---

## 1. Ambiente, configuração e execução

### 1.1 O projeto NetBeans

O projeto fica em `Java_Champs/`. A configuração importante está em [`nbproject/project.properties`](../Java_Champs/nbproject/project.properties):

| Propriedade | Valor | Significado |
|---|---|---|
| `src.dir` | `src` | pasta dos fontes |
| `build.dir` | `build` | pasta dos `.class` |
| `main.class` | `br.com.visao.TelaPrincipal` | classe executável |
| `javac.source` / `javac.target` | `26` | versão do Java configurada |
| `source.encoding` | `UTF-8` | codificação dos fontes (acentos) |
| `file.reference.mysql-connector-j-9.3.0.jar` | `../../POO/mysql-connector-j-9.3.0.jar` | caminho do driver JDBC no ambiente original |
| `javac.classpath` | o jar acima | classpath de compilação |

> O caminho `../../POO/mysql-connector-j-9.3.0.jar` é **relativo ao ambiente original**. Em outra máquina, adicione o jar em **Libraries** no NetBeans e ajuste o caminho. Sem o driver, o `Class.forName` falha e o sistema não conecta.

Os demais arquivos de build são gerados pelo NetBeans: `build.xml` é o script Ant do projeto, `nbproject/build-impl.xml` contém as tarefas internas e `manifest.mf` é o manifesto do jar (o `Main-Class` é inserido automaticamente pelo build). Nenhum deles tem lógica do sistema; o que importa para a apresentação é `project.properties` + `src/`.

### 1.2 Como executar

**No NetBeans:**

1. Abra a pasta `Java_Champs`.
2. Confirme o JDK e o jar do Connector/J nas bibliotecas.
3. Confira a conexão em `DAO` (`localhost:3306`, banco `campeonato`, usuário `root`, senha vazia).
4. Execute o projeto (F6). A `main` está em `TelaPrincipal`.

**Pela linha de comando (Windows/PowerShell):**

```powershell
javac -encoding UTF-8 -d build/classes -cp "caminho/para/mysql-connector-j-9.3.0.jar" (Get-ChildItem src -Recurse -Filter *.java).FullName
java -cp "build/classes;caminho/para/mysql-connector-j-9.3.0.jar" br.com.visao.TelaPrincipal
```

### 1.3 A `main` e o Nimbus

```java
public static void main(String args[]) {
    try {
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                javax.swing.UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
    } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
        System.out.println("Não foi possível aplicar o Nimbus: " + ex.getMessage());
    }
    java.awt.EventQueue.invokeLater(() -> new TelaPrincipal().setVisible(true));
}
```

- **`UIManager`**: tenta aplicar o tema **Nimbus**, que deixa os componentes Swing com aparência mais moderna. Se não existir, o `catch` só avisa no console e o Swing usa o tema padrão.
- **`EventQueue.invokeLater(...)`**: garante que a tela seja criada na **EDT** (Event Dispatch Thread), a thread correta do Swing. Criar/altear componentes fora da EDT pode causar travamentos e exceções imprevisíveis.
- **`new TelaPrincipal().setVisible(true)`**: o construtor monta a tela e carrega os dados; depois ela é exibida.

---

## 2. Estrutura dos pacotes

```text
br.com.controle   → MODEL
  Campeonato, Equipe, Estadio, Arbitro, Jogador,
  Partida, Classificacao, RelatorioClassificacao, RelatorioPartida

br.com.entidade   → DAO
  DAO,
  ManterCampeonato, ManterEquipe, ManterEstadio, ManterArbitro,
  ManterJogador, ManterPartida, ManterClassificacao, ManterRelatorio

br.com.visao      → VIEW
  TelaPrincipal
```

### 2.1 Direção das dependências

```text
visao  ──────►  entidade  ──────►  controle
  │                │                  │
  └────────────────┴──────────────────┘
         todos podem usar as classes de modelo
```

> **Qual MVC é este?** É o MVC didático do professor (aulas 007–010 do [repositório POO](https://github.com/Matheus-Souza-Silva/POO)): `controle` = Model, `entidade` = DAO, `visao` = View, e o controller está no evento do botão do `JFrame`. O projeto segue esse desenho; não existe classe `Controller` separada. O MVC clássico de 1990 (Smalltalk-80/GoF) é outro modelo, com um objeto Controller dedicado; não é o que o curso usa.

- A `visao` importa `br.com.controle.*` e `br.com.entidade.*`.
- As classes `Manter...` importam `br.com.controle` (para receber/devolver objetos) e `java.sql`.
- O `controle` **não** importa `entidade` nem `visao`: as classes de modelo são independentes.
- O `entidade` **não** importa `visao`: o DAO não conhece a tela.

### 2.2 O que é “Model” aqui

As classes de `controle` são **classes de dados** (POJOs): atributos privados, getters/setters e, em algumas, `toString()`. Não há regra de negócio nelas; quem valida é a tela e quem calcula é o banco. Isso é o que se chama de **modelo anêmico**; funciona para o trabalho, mas é um ponto de discussão.

---

## 3. As classes de modelo

### 3.1 Visão geral

| Classe | Campos principais | Tipos | Tabela/Origem | `toString()`? |
|---|---|---|---|---|
| `Campeonato` | id, nome, data_inicio, data_fim, status | `int`, `String` | `CAMPEONATO` | sim |
| `Equipe` | id, nome, cidade | `int`, `String` | `EQUIPE` | sim |
| `Estadio` | id, nome, cidade, capacidade | `int`, `String` | `ESTADIO` | sim |
| `Arbitro` | id, nome, registro_federacao, categoria | `int`, `String` | `ARBITRO` | sim |
| `Jogador` | id, nome, posicao, numeroCamisa, dataNascimento, idEquipe, nomeEquipe | `int`, `String` | `JOGADOR` + JOIN | sim |
| `Partida` | 13 campos da tabela + 5 nomes de exibição | `int`, `String`, `Integer` | `PARTIDA` + JOIN | não |
| `Classificacao` | 12 campos + nomeEquipe | `int`, `Integer`, `String` | `CLASSIFICACAO` + JOIN | não |
| `RelatorioClassificacao` | 18 campos | `int`, `Integer`, `double`, `String` | procedure | não (tem formatadores) |
| `RelatorioPartida` | 8 campos | `int`, `String` | procedure | não |

### 3.2 `Campeonato`

[`Campeonato.java`](../Java_Champs/src/br/com/controle/Campeonato.java#L3-L53)

```java
public class Campeonato {
    private int idCampeonato;
    private String nome;
    private String data_inicio; // Formato YYYY-MM-DD
    private String data_fim;    // Formato YYYY-MM-DD
    private String status;      // PLANEJADO, EM_ANDAMENTO, ENCERRADO, CANCELADO

    // getters e setters...

    @Override
    public String toString() {
        return idCampeonato + " - " + nome + " [" + status + "]";
    }
}
```

- **Por que as datas são `String`?** Porque a tela trabalha com texto no formato `YYYY-MM-DD`, o DAO envia a string e o MySQL converte para `DATE`. A validação de formato é feita na tela (`dataValida`).
- **`toString()`**: usado pelo `JComboBox<Campeonato>` da aba 6, 7 e 8. Sem ele, o combo mostraria algo como `br.com.controle.Campeonato@1b6d3586`.
- **Saída:** `1 - Copa Regional 2026 [EM_ANDAMENTO]`.

### 3.3 `Equipe`

[`Equipe.java`](../Java_Champs/src/br/com/controle/Equipe.java#L3-L35)

```java
private int idEquipe;
private String nome;
private String cidade;

@Override
public String toString() {
    return idEquipe + " - " + nome + " (" + cidade + ")";
}
```

- Usada nos combos de jogador, inscrição, mandante, visitante e relatório.
- **Saída:** `1 - Tigres FC (Sao Paulo)`.

### 3.4 `Estadio`

[`Estadio.java`](../Java_Champs/src/br/com/controle/Estadio.java#L3-L44)

```java
private int idEstadio;
private String nome;
private String cidade;
private int capacidade;

@Override
public String toString() {
    return idEstadio + " - " + nome + " (" + cidade + ")";
}
```

- `capacidade` é `int`; a tela valida `> 0` antes de habilitar o cadastro.
- **Saída:** `1 - Arena Central (Sao Paulo)`.

### 3.5 `Arbitro`

[`Arbitro.java`](../Java_Champs/src/br/com/controle/Arbitro.java#L3-L44)

```java
private int idArbitro;
private String nome;
private String registro_federacao;
private String categoria;

@Override
public String toString() {
    return idArbitro + " - " + nome + " (" + categoria + ")";
}
```

- **Saída:** `1 - Carlos Andrade (FIFA)`.

### 3.6 `Jogador`

[`Jogador.java`](../Java_Champs/src/br/com/controle/Jogador.java#L12-L85)

```java
private int idJogador;
private String nome;
private String posicao;
private int numeroCamisa;
private String dataNascimento; // YYYY-MM-DD (pode ser null)
private int idEquipe;
private String nomeEquipe;     // extra, vem do JOIN

@Override
public String toString() {
    String equipe = (nomeEquipe == null || nomeEquipe.trim().isEmpty())
            ? "" : " (" + nomeEquipe + ")";
    return numeroCamisa + " - " + nome + equipe;
}
```

- **`nomeEquipe` não existe na tabela `JOGADOR`.** É preenchido pelo `ManterJogador` com `e.nome AS nome_equipe`, só para exibição na `JTable`/`toString`.
- **`dataNascimento` pode ser `null`**: o banco aceita `NULL`, e o DAO trata isso com `setNull`.
- **Saída:** `10 - Caio Mendes (Tigres FC)`.
- **Nuance:** a posição é `String`; o combo da tela limita a seis valores, mas o modelo não valida.

### 3.7 `Partida`

[`Partida.java`](../Java_Champs/src/br/com/controle/Partida.java#L3-L167)

```java
private int idPartida;
private int idCampeonato;
private int rodada;
private int idMandante;
private int idVisitante;
private int idEstadio;
private int idArbitro;
private String dataPartida; // YYYY-MM-DD HH:MM:SS
private Integer golsMandante;
private Integer golsVisitante;
private int acrescimo;
private String status;       // AGENDADA, EM_ANDAMENTO, ENCERRADA, CANCELADA
private String tempoPartida; // NULL, PRIMEIRO_TEMPO, SEGUNDO_TEMPO

// extras de exibição (JOIN)
private String nomeCampeonato;
private String nomeMandante;
private String nomeVisitante;
private String nomeEstadio;
private String nomeArbitro;
```

- **Por que `golsMandante`/`golsVisitante` são `Integer` e não `int`?** Porque o banco usa `NULL` para “sem placar”. Um `int` não representa `NULL`; o `Integer` pode ser `null`.
- **`dataPartida` é `String`** no formato `YYYY-MM-DD HH:MM:SS`; o DAO envia com `setString` e o MySQL converte para `DATETIME`.
- **Campos extras** não existem na tabela: são preenchidos pelo JOIN para a tabela da tela mostrar nomes em vez de IDs.
- **Sem `toString()`**: `Partida` não é usada em combo; a tabela usa os getters.

### 3.8 `Classificacao`

[`Classificacao.java`](../Java_Champs/src/br/com/controle/Classificacao.java#L3-L120)

```java
private int idCampeonato;
private int idEquipe;
private String nomeEquipe;       // extra (JOIN)
private Integer colocacaoEquipe; // pode ser null
private int partidasJogadas;
private int vitorias;
private int empates;
private int derrotas;
private int golsMarcados;
private int golsSofridos;
private int saldoGols;
private int pontuacao;
private String statusAtual;
```

- **`colocacaoEquipe` é `Integer`** porque quem ainda não jogou tem `NULL`. O DAO usa `rs.wasNull()` para diferenciar `NULL` de `0`.
- **`saldoGols` e `pontuacao`** são lidos do banco (colunas geradas); o Java não recalcula.
- **`nomeEquipe`** é extra, vindo do `INNER JOIN EQUIPE`.
- **Sem `toString()`**: não é usada em combo.

### 3.9 `RelatorioClassificacao`

[`RelatorioClassificacao.java`](../Java_Champs/src/br/com/controle/RelatorioClassificacao.java#L10-L184)

```java
private String nomeCampeonato;
private Integer posicao;
private String nomeEquipe;
private String cidade;

private int pontos;
private int jogos;
private int vitorias;
private int empates;
private int derrotas;
private int golsPro;
private int golsContra;
private int saldoGols;
private double aproveitamento;

private int jogosCasa;
private int jogosFora;
private int estadios;
private int arbitros;
private String statusAtual;

public String getPosicaoFormatada() {
    return posicao == null ? "-" : posicao + "º";
}

public String getAproveitamentoFormatado() {
    return String.format(java.util.Locale.ROOT, "%.1f%%", aproveitamento);
}
```

- **`posicao` é `Integer`**: a procedure devolve `NULL` para equipe sem jogo.
- **`getPosicaoFormatada()`**: mostra `"-"` ou `"1º"`, `"2º"` etc.
- **`getAproveitamentoFormatado()`**: formata `100.0%` com uma casa; usa `Locale.ROOT` para o separador decimal ser ponto, independente do idioma do Windows.
- **Por que não há `toString()`?** A tela usa os getters para montar as linhas da `JTable`; não há combo com esse objeto.

### 3.10 `RelatorioPartida`

[`RelatorioPartida.java`](../Java_Champs/src/br/com/controle/RelatorioPartida.java#L7-L80)

```java
private int rodada;
private String dataHora;
private String mandante;
private String visitante;
private String estadio;
private String arbitro;
private String placar;
private String status;
```

- É uma linha **já formatada** pela procedure: `dataHora` vem `dd/mm/aaaa hh:mm`, `placar` vem `"2 x 1"` ou `"- x -"`.
- O Java não formata nada; só copia para a `JTable`.

### 3.11 Encapsulamento, `toString()` e a ausência de `equals`

- **Encapsulamento:** todos os atributos são `private`; o acesso é por `get`/`set`. Isso permite mudar a implementação sem quebrar quem usa.
- **`toString()`:** usado implicitamente por `JComboBox`, `JTable` (em alguns casos) e `System.out`. É por isso que os combos mostram nomes legíveis.
- **Não há `equals`/`hashCode` sobrescritos:** dois objetos com o mesmo ID **não** são iguais para o Java. Por isso a tela **não** faz `cb.setSelectedItem(new Equipe(...))`; ela percorre os itens comparando `getIdEquipe()`. Se fizesse `setSelectedItem` com um objeto novo, não encontraria o item.

---

## 4. A classe `DAO` (conexão JDBC)

[`DAO.java`](../Java_Champs/src/br/com/entidade/DAO.java#L11-L39)

```java
public class DAO {
    Connection con;
    PreparedStatement pst;
    ResultSet rs;

    public void abrirBanco() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/campeonato";
            String user = "root";
            String senha = "";
            con = (Connection) DriverManager.getConnection(url, user, senha);
            System.out.println("Conectado ao banco de dados ");
        } catch (ClassNotFoundException ex) {
            System.out.println("Classe não encontrada, adicione o driver nas bibliotecas.");
            Logger.getLogger(DAO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public void fecharBanco() throws Exception {
        if (pst != null) {
            pst.close();
            System.out.println("Execuçao da Query fechada\n");
        }
    }
}
```

### 4.1 Os três campos

| Campo | Tipo | Para que serve |
|---|---|---|
| `con` | `Connection` | conexão aberta com o MySQL |
| `pst` | `PreparedStatement` | comando SQL preparado (INSERT/UPDATE/DELETE/SELECT) |
| `rs` | `ResultSet` | resultado de um SELECT |

- Eles **não** têm modificador de acesso (`private`/`protected`), então são visíveis para as classes do **mesmo pacote** `br.com.entidade` (as `Manter...`). A `TelaPrincipal`, em outro pacote, não os acessa.
- Cada objeto `Manter...` tem seus próprios `con`, `pst` e `rs`, porque herda os campos. A `TelaPrincipal` guarda **um DAO por entidade**.

### 4.2 `abrirBanco()` linha a linha

1. **`Class.forName("com.mysql.jdbc.Driver")`**: carrega a classe do driver JDBC. É o nome **antigo** do Connector/J.
   - O Connector/J 8+ recomenda `com.mysql.cj.jdbc.Driver`.
   - Se a classe não existir, o `catch (ClassNotFoundException)` imprime uma mensagem e **não relança**. O método termina, `con` fica `null`, e a próxima chamada `con.prepareStatement(...)` gera `NullPointerException`.
2. **`url`**: `jdbc:mysql://localhost:3306/campeonato`.
   - `jdbc:mysql://` = protocolo;
   - `localhost:3306` = host e porta;
   - `campeonato` = database.
3. **`user = "root"`, `senha = ""`**: credenciais padrão do ambiente original. Em outra máquina, podem ser diferentes.
4. **`DriverManager.getConnection(url, user, senha)`**: abre a conexão.
5. **`System.out.println("Conectado...")`**: log no console (não aparece na tela).
6. **`catch (SQLException e)`**: imprime e lança `new RuntimeException(e)`. A assinatura declara `throws SQLException`, mas o `catch` converte em `RuntimeException` (não verificada). Isso mantém a assinatura, mas muda o tipo real da exceção.
7. **`ClassNotFoundException`**: é uma exceção verificada; o método não declara `throws ClassNotFoundException`, então precisa capturar. O problema é que ele **não** relança nem ajusta `con`.

**Pergunta provável:** “por que o `Class.forName` é necessário?”
**Resposta:** para carregar o driver JDBC na memória. Com o JDBC 4+, o driver é descoberto automaticamente via `META-INF/services`, então o `Class.forName` é opcional; mas o código o mantém por compatibilidade/didática.

### 4.3 `fecharBanco()` linha a linha

- Verifica se `pst` não é `null`.
- Fecha **somente o `PreparedStatement`**.
- Imprime “Execução da Query fechada”.
- **Não fecha `rs`** explicitamente. Na prática, fechar o statement fecha o `ResultSet` associado.
- **Não fecha `con`**. A conexão fica aberta até o garbage collector (ou até o fim do programa). É um vazamento lógico.
- Se `pst.close()` lançar `SQLException`, o método declara `throws Exception` e propaga; no `finally` de um DAO, isso pode mascarar a exceção original.

**Pergunta provável:** “o `finally` não fecha tudo?”
**Resposta:** fecha o `PreparedStatement`. A conexão e o `ResultSet` não são fechados explicitamente; é uma melhoria pendente.

### 4.4 O ciclo de vida de uma operação

```text
abrirBanco()
   → Class.forName
   → DriverManager.getConnection
   → con = Connection

prepareStatement(query)
   → pst = PreparedStatement

setX(...) nos parâmetros
   → valores enviados separados do SQL

execute() / executeUpdate() / executeQuery()
   → MySQL executa
   → triggers/procedures podem disparar

ler ResultSet (se SELECT)
   → rs.next()
   → rs.getX(...)

fecharBanco()
   → pst.close()
   (con não é fechada)
```

### 4.5 `PreparedStatement` e o `?`

```java
String query = "INSERT INTO EQUIPE (nome, cidade) VALUES (?, ?)";
pst = (PreparedStatement) con.prepareStatement(query);
pst.setString(1, e.getNome());
pst.setString(2, e.getCidade());
pst.execute();
```

- O SQL é enviado ao MySQL **com espaços reservados**; os valores vão separados.
- Isso evita dois problemas:
  1. **Aspas e caracteres especiais** no texto (ex.: `D'Ávila`) não quebram o SQL.
  2. **SQL Injection**: um valor como `' OR 1=1 --` é tratado como texto, não como comando.
- Os setters são tipados: `setInt`, `setString`, `setDate`, `setNull`.
- **`setNull(posição, tipo)`**: usado para `DATE` (nascimento), `INTEGER` (gols) e `VARCHAR` (tempo). O tipo vem de `java.sql.Types`.

### 4.6 `execute`, `executeUpdate` e `executeQuery`

| Método | Usado para | Retorno | Onde aparece |
|---|---|---|---|
| `execute()` | INSERT, DELETE, CALL, DDL | `boolean` (ignorado) | maioria dos `inserir`, `deletar`, `inscreverEquipe` |
| `executeUpdate()` | UPDATE (e INSERT/DELETE) | `int` (linhas afetadas) | `alterar`, `atualizarStatus` |
| `executeQuery()` | SELECT e procedures que retornam linhas | `ResultSet` | `pesquisar...`, `listar...`, relatórios |

- `execute()` serve para qualquer comando; o retorno `boolean` indica se há `ResultSet`, mas o código não usa.
- `executeUpdate()` é mais específico para comandos que alteram linhas; o número de linhas afetadas é ignorado no projeto.
- `executeQuery()` é obrigatório para SELECT; sem ele, o `ResultSet` não é obtido.

### 4.7 `ResultSet` e `wasNull()`

```java
if (rs.next()) {
    Campeonato c = montarCampeonato(rs);
}
```

- O cursor começa **antes** da primeira linha; `next()` avança e devolve `true` se há linha.
- `getInt`, `getString`, `getDouble` leem a coluna pelo nome ou índice (o projeto usa nome).
- **`wasNull()`**: `getInt` devolve `0` tanto para o valor `0` quanto para `NULL`; `wasNull()` diz se a última coluna lida era `NULL`. É usado em:
  - `ManterPartida.montarPartida` para os gols;
  - `ManterClassificacao.montarClassificacao` para a colocação;
  - `ManterRelatorio.classificacaoPorCampeonato` para a posição.
- `getString` devolve `null` quando a coluna é `NULL`; por isso `Jogador.dataNascimento` pode ficar `null` naturalmente.

### 4.8 Transações e `autocommit`

- O JDBC, por padrão, está em **autocommit = true**: cada comando é uma transação própria.
- O projeto **não** usa `setAutoCommit(false)` em nenhum DAO. Portanto, cada `inserir`/`alterar`/`deletar` é atômico sozinho.
- Não há operações compostas no Java que precisem de transação (a sincronização da classificação é feita dentro do trigger/procedure, no mesmo comando).
- O `Champs_dados.sql` usa `START TRANSACTION`/`COMMIT` porque é um script de carga com vários INSERTs.

### 4.9 Fragilidades reais do `DAO`

1. **Conexão não fechada** em `fecharBanco` (só o `pst`).
2. **`ClassNotFoundException` engolida**: se o driver não carregar, `con` fica `null` e o erro real aparece depois como `NullPointerException`.
3. **Driver com nome antigo**: `com.mysql.jdbc.Driver` em vez de `com.mysql.cj.jdbc.Driver`.
4. **`fecharBanco` pode mascarar a exceção original** se `pst.close()` falhar.
5. **Sem pool de conexões**: cada operação abre uma conexão nova; para um sistema maior, seria lento.
6. **Uma conexão por DAO**: como a tela tem oito DAOs, há até oito conexões vivas ao mesmo tempo (na prática, uma por operação, mas as antigas ficam abertas).
7. **Sem `try-with-resources`**: o padrão moderno fecharia `Connection`, `PreparedStatement` e `ResultSet` automaticamente.

> Na apresentação, diga: “o projeto prioriza a clareza didática; o próximo passo seria fechar a conexão e usar um pool”.

---

## 5. As classes `Manter...` (DAOs)

### 5.1 O padrão comum

Todas as classes:

```java
public class ManterX extends DAO {
    public void inserir(X x) throws Exception { ... }
    public void alterar(X x) throws Exception { ... }
    public void deletar(X x) throws Exception { ... }
    public X pesquisarRegistro(String termo) throws Exception { ... }
    public ArrayList<X> pesquisarTudo() throws Exception { ... }
    private X montarX(ResultSet rs) throws Exception { ... }
}
```

- **`extends DAO`**: herda `con`, `pst`, `rs`, `abrirBanco()` e `fecharBanco()`.
- **`throws Exception`**: assinatura ampla; a tela captura com `catch (Exception ex)` e chama `mostrarErro`.
- **`try { ... } finally { fecharBanco(); }`**: garante o fechamento do statement mesmo se ocorrer erro.
- **`montarX(ResultSet)`**: converte a linha do banco em objeto; é `private` porque só o DAO usa.
- **`ArrayList<X>`**: a lista concreta usada; poderia ser `List<X>`.

### 5.2 `ManterCampeonato`

[`ManterCampeonato.java`](../Java_Champs/src/br/com/entidade/ManterCampeonato.java#L8-L116)

| Método | SQL | Retorno |
|---|---|---|
| `inserir(Campeonato)` | `INSERT INTO CAMPEONATO (nome, data_inicio, data_fim, status) VALUES (?, ?, ?, ?)` | `void` |
| `alterar(Campeonato)` | `UPDATE CAMPEONATO SET nome=?, data_inicio=?, data_fim=?, status=? WHERE id_campeonato=?` | `void` |
| `deletar(Campeonato)` | `DELETE FROM CAMPEONATO WHERE id_campeonato=?` | `void` |
| `pesquisarRegistro(String)` | `SELECT ... WHERE id_campeonato=? OR nome LIKE ? ORDER BY (id_campeonato=?) DESC, nome LIMIT 1` | `Campeonato` ou `null` |
| `pesquisarTudo()` | `SELECT ... ORDER BY id_campeonato` | `ArrayList<Campeonato>` |
| `montarCampeonato(ResultSet)` | — | `Campeonato` |

**`inserir`:**

```java
abrirBanco();
String query = "INSERT INTO CAMPEONATO (nome, data_inicio, data_fim, status) VALUES (?, ?, ?, ?)";
pst = (PreparedStatement) con.prepareStatement(query);
pst.setString(1, c.getNome());
pst.setString(2, c.getData_inicio());
pst.setString(3, c.getData_fim());
pst.setString(4, c.getStatus());
pst.execute();
```

- O ID não é enviado; o MySQL gera com `AUTO_INCREMENT`.
- As datas vão como `String`; o MySQL converte para `DATE`.
- O status vem do combo.

**`alterar`:** os quatro campos + o ID no quinto parâmetro. O `WHERE` garante que só aquele registro muda.

**`deletar`:** só o ID. Se houver partidas, a FK `fk_partida_campeonato` (RESTRICT) bloqueia e o erro chega à tela.

**`pesquisarRegistro`:**

```java
String busca = termo.trim();
Integer codigo = null;
try {
    codigo = Integer.valueOf(busca);
} catch (NumberFormatException ex) {
    // texto não numérico: busca só por nome
}
...
String query = "SELECT id_campeonato, nome, data_inicio, data_fim, status "
        + "FROM CAMPEONATO "
        + "WHERE id_campeonato = ? OR nome LIKE ? "
        + "ORDER BY (id_campeonato = ?) DESC, nome "
        + "LIMIT 1";
pst.setInt(1, codigo == null ? -1 : codigo);
pst.setString(2, "%" + busca + "%");
pst.setInt(3, codigo == null ? -1 : codigo);
```

- Se o termo não for número, `codigo` fica `null` e o ID vira `-1`; como não existe ID negativo, a comparação numérica não encontra ninguém e a busca fica só no `LIKE`.
- Se for número, o ID exato tem prioridade no `ORDER BY`.
- `LIKE '%texto%'` procura em qualquer posição; a collation é case-insensitive.
- `LIMIT 1` devolve só o primeiro.
- `montarCampeonato` lê as cinco colunas e usa `getString` para as datas.

**`pesquisarTudo`:** `ORDER BY id_campeonato`; é o que popula a `JTable` e os combos.

### 5.3 `ManterEquipe`

[`ManterEquipe.java`](../Java_Champs/src/br/com/entidade/ManterEquipe.java#L8-L109)

| Método | SQL |
|---|---|
| `inserir` | `INSERT INTO EQUIPE (nome, cidade) VALUES (?, ?)` |
| `alterar` | `UPDATE EQUIPE SET nome=?, cidade=? WHERE id_equipe=?` |
| `deletar` | `DELETE FROM EQUIPE WHERE id_equipe=?` |
| `pesquisarRegistro` | `WHERE id_equipe=? OR nome LIKE ? OR cidade LIKE ? ORDER BY (id_equipe=?) DESC, nome LIMIT 1` |
| `pesquisarTudo` | `ORDER BY id_equipe` |

- A busca aceita **nome ou cidade**, além do ID.
- `montarEquipe` lê `id_equipe`, `nome`, `cidade`.
- A FK `uq_equipe_nome` pode gerar `Duplicate entry`; a tela traduz.
- Excluir equipe com jogador/inscrição/partida gera erro 1451; a tela traduz.

### 5.4 `ManterEstadio`

[`ManterEstadio.java`](../Java_Champs/src/br/com/entidade/ManterEstadio.java#L8-L111)

| Método | SQL |
|---|---|
| `inserir` | `INSERT INTO ESTADIO (nome, cidade, capacidade) VALUES (?, ?, ?)` |
| `alterar` | `UPDATE ESTADIO SET nome=?, cidade=?, capacidade=? WHERE id_estadio=?` |
| `deletar` | `DELETE FROM ESTADIO WHERE id_estadio=?` |
| `pesquisarRegistro` | `WHERE id_estadio=? OR nome LIKE ? OR cidade LIKE ? ORDER BY (id_estadio=?) DESC, nome LIMIT 1` |
| `pesquisarTudo` | `ORDER BY id_estadio` |

- `capacidade` é `setInt`.
- O `CHECK capacidade > 0` é a última barreira; a tela usa `inteiroPositivo`.
- `uq_estadio_nome_cidade` impede mesmo nome na mesma cidade.

### 5.5 `ManterArbitro`

[`ManterArbitro.java`](../Java_Champs/src/br/com/entidade/ManterArbitro.java#L8-L113)

| Método | SQL |
|---|---|
| `inserir` | `INSERT INTO ARBITRO (nome, registro_federacao, categoria) VALUES (?, ?, ?)` |
| `alterar` | `UPDATE ARBITRO SET nome=?, registro_federacao=?, categoria=? WHERE id_arbitro=?` |
| `deletar` | `DELETE FROM ARBITRO WHERE id_arbitro=?` |
| `pesquisarRegistro` | `WHERE id_arbitro=? OR nome LIKE ? OR registro_federacao LIKE ? ORDER BY (id_arbitro=?) DESC, nome LIMIT 1` |
| `pesquisarTudo` | `ORDER BY id_arbitro` |

- A busca aceita **registro** além de ID/nome.
- `uq_arbitro_registro` impede registro repetido.
- Excluir árbitro com partida gera erro 1451.

### 5.6 `ManterJogador`

[`ManterJogador.java`](../Java_Champs/src/br/com/entidade/ManterJogador.java#L15-L149)

#### O `SELECT` base

```java
private static final String SELECT_JOGADOR
        = "SELECT j.id_jogador, j.nome, j.posicao, j.numero_camisa, "
        + "j.data_nascimento, j.id_equipe, e.nome AS nome_equipe "
        + "FROM JOGADOR j "
        + "INNER JOIN EQUIPE e ON e.id_equipe = j.id_equipe ";
```

- `INNER JOIN EQUIPE` traz `e.nome AS nome_equipe`, que **não existe** na tabela `JOGADOR`.
- É a prova de JOIN no CRUD extra.
- Sem o JOIN, a tela mostraria `id_equipe` em vez do nome do time.

#### `inserir` e `alterar`

```java
String query = "INSERT INTO JOGADOR (nome, posicao, numero_camisa, "
        + "data_nascimento, id_equipe) VALUES (?, ?, ?, ?, ?)";
preencherParametros(j);
pst.execute();
```

```java
String query = "UPDATE JOGADOR SET nome = ?, posicao = ?, numero_camisa = ?, "
        + "data_nascimento = ?, id_equipe = ? WHERE id_jogador = ?";
preencherParametros(j);
pst.setInt(6, j.getIdJogador());
```

#### `preencherParametros`

```java
pst.setString(1, j.getNome());
pst.setString(2, j.getPosicao());
pst.setInt(3, j.getNumeroCamisa());

if (j.getDataNascimento() == null || j.getDataNascimento().trim().isEmpty()) {
    pst.setNull(4, Types.DATE);
} else {
    pst.setDate(4, java.sql.Date.valueOf(j.getDataNascimento().trim()));
}

pst.setInt(5, j.getIdEquipe());
```

- **Data opcional:** vazia → `setNull(4, Types.DATE)`; preenchida → `java.sql.Date.valueOf(...)`.
- **`Types.DATE`** diz ao driver qual é o tipo do `NULL`; sem isso, o MySQL pode reclamar.
- O método é compartilhado por INSERT e UPDATE (os cinco primeiros parâmetros são iguais).

#### `pesquisarRegistro`

```java
String query = SELECT_JOGADOR
        + "WHERE j.id_jogador = ? OR j.nome LIKE ? OR j.posicao LIKE ? "
        + "OR e.nome LIKE ? OR CAST(j.numero_camisa AS CHAR) = ? "
        + "ORDER BY (j.id_jogador = ?) DESC, j.nome "
        + "LIMIT 1";
```

- Aceita **ID, nome, posição, nome da equipe ou camisa**.
- **`CAST(j.numero_camisa AS CHAR) = ?`**: compara a camisa como texto. Isso permite buscar `"10"` sem quebrar quando o termo é `"atacante"`; se comparasse `numero_camisa = 'atacante'`, o MySQL converteria e poderia dar comportamento estranho.
- Observação: buscar `"01"` não encontra a camisa `1`, porque a comparação é textual; é uma limitação aceitável.
- `ORDER BY (id = ?) DESC, nome`: ID exato primeiro.
- `LIMIT 1`.

#### `pesquisarTudo` e `montarJogador`

```java
pst = (PreparedStatement) con.prepareStatement(SELECT_JOGADOR
        + "ORDER BY j.id_jogador");
...
j.setDataNascimento(rs.getString("data_nascimento")); // pode ser null
j.setNomeEquipe(rs.getString("nome_equipe"));
```

- A lista é ordenada por ID.
- `getString` devolve `null` para data nula; a tela mostra vazio.
- `nomeEquipe` é preenchido pelo JOIN.

#### Regras do banco que afetam este DAO

- `uq_jogador_camisa` → erro 1062; a tela traduz para “Já existe um jogador com esse número de camisa nessa equipe.”
- `chk_jogador_camisa` → erro 3819; a tela traduz para “O número da camisa precisa estar entre 1 e 99.”
- `fk_jogador_equipe` → erro 1452 se a equipe não existir (a tela só oferece equipes existentes).
- Excluir equipe com jogadores → erro 1451.

### 5.7 `ManterPartida`

[`ManterPartida.java`](../Java_Champs/src/br/com/entidade/ManterPartida.java#L9-L190)

#### O `SELECT` base

```java
private static final String SELECT_PARTIDA
        = "SELECT p.*, c.nome AS nome_camp, m.nome AS nome_mand, "
        + "v.nome AS nome_vis, es.nome AS nome_est, a.nome AS nome_arb "
        + "FROM PARTIDA p "
        + "INNER JOIN CAMPEONATO c ON c.id_campeonato = p.id_campeonato "
        + "INNER JOIN EQUIPE m ON m.id_equipe = p.id_mandante "
        + "INNER JOIN EQUIPE v ON v.id_equipe = p.id_visitante "
        + "INNER JOIN ESTADIO es ON es.id_estadio = p.id_estadio "
        + "INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro ";
```

- `p.*` traz todas as colunas da partida.
- Os aliases `nome_camp`, `nome_mand`, `nome_vis`, `nome_est`, `nome_arb` trazem os nomes.
- **`EQUIPE` aparece duas vezes**: `m` (mandante) e `v` (visitante). Sem apelidos, o SQL seria ambíguo.
- `INNER JOIN` é suficiente porque as FKs garantem que todos os relacionados existem.

#### `inserir` (12 parâmetros)

```java
String query = "INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, "
        + "id_estadio, id_arbitro, data_partida, gols_mandante, gols_visitante, "
        + "acrescimo, status, tempo_partida) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
preencherParametros(p);
pst.execute();
```

#### `alterar` (12 + ID)

```java
String query = "UPDATE PARTIDA SET id_campeonato = ?, rodada = ?, id_mandante = ?, "
        + "id_visitante = ?, id_estadio = ?, id_arbitro = ?, data_partida = ?, "
        + "gols_mandante = ?, gols_visitante = ?, acrescimo = ?, status = ?, tempo_partida = ? "
        + "WHERE id_partida = ?";
preencherParametros(p);
pst.setInt(13, p.getIdPartida());
```

#### `preencherParametros`

```java
pst.setInt(1, p.getIdCampeonato());
pst.setInt(2, p.getRodada());
pst.setInt(3, p.getIdMandante());
pst.setInt(4, p.getIdVisitante());
pst.setInt(5, p.getIdEstadio());
pst.setInt(6, p.getIdArbitro());
pst.setString(7, p.getDataPartida());

if (p.getGolsMandante() == null) {
    pst.setNull(8, Types.INTEGER);
} else {
    pst.setInt(8, p.getGolsMandante());
}

if (p.getGolsVisitante() == null) {
    pst.setNull(9, Types.INTEGER);
} else {
    pst.setInt(9, p.getGolsVisitante());
}

pst.setInt(10, p.getAcrescimo());
pst.setString(11, p.getStatus());

if (p.getTempoPartida() == null || p.getTempoPartida().trim().isEmpty()) {
    pst.setNull(12, Types.VARCHAR);
} else {
    pst.setString(12, p.getTempoPartida());
}
```

- **Gols `null`**: gravados como `NULL`, porque o banco tem `chk_partida_placar_completo` (os dois `NULL` ou os dois preenchidos) e `chk_partida_placar_por_status`.
- **`data_partida`**: `setString` no formato `YYYY-MM-DD HH:MM:SS`; o MySQL converte para `DATETIME`.
- **`tempo_partida`**: vazio vira `NULL`; só pode ser preenchido com `EM_ANDAMENTO` (CHECK).
- **`acrescimo`**: `setInt`.

#### `pesquisarRegistro`

```java
String query = SELECT_PARTIDA
        + "WHERE p.id_partida = ? OR m.nome LIKE ? OR v.nome LIKE ? "
        + "OR c.nome LIKE ? OR es.nome LIKE ? OR a.nome LIKE ? "
        + "ORDER BY (p.id_partida = ?) DESC, p.data_partida DESC, p.id_partida DESC "
        + "LIMIT 1";
```

- Aceita **ID, nome de equipe (mandante ou visitante), campeonato, estádio ou árbitro**.
- Se o termo for numérico, o ID exato vem primeiro; se não, a busca é só por nomes.
- Em empate de nomes, a partida mais recente (`data_partida DESC`) e de maior ID aparece primeiro.
- `LIMIT 1`.

#### `pesquisarTudo` e `montarPartida`

```java
pst = (PreparedStatement) con.prepareStatement(SELECT_PARTIDA
        + "ORDER BY p.id_partida");
...
int gm = rs.getInt("gols_mandante");
p.setGolsMandante(rs.wasNull() ? null : gm);

int gv = rs.getInt("gols_visitante");
p.setGolsVisitante(rs.wasNull() ? null : gv);
...
p.setNomeCampeonato(rs.getString("nome_camp"));
p.setNomeMandante(rs.getString("nome_mand"));
p.setNomeVisitante(rs.getString("nome_vis"));
p.setNomeEstadio(rs.getString("nome_est"));
p.setNomeArbitro(rs.getString("nome_arb"));
```

- **`wasNull()` é essencial**: `getInt` devolve `0` para `NULL`; sem `wasNull`, um jogo sem placar apareceria como `0 x 0`.
- `data_partida` vem como `String` no formato do MySQL.
- Os cinco nomes extras são preenchidos para a `JTable`.

#### Regras do banco que afetam este DAO

- Triggers `BEFORE`/`AFTER`: validação e sincronização.
- `chk_partida_placar_por_status`, `chk_partida_placar_completo`, `chk_partida_tempo`, `chk_partida_times`, `chk_partida_rodada`, `chk_partida_acrescimo`.
- `uq_partida_confronto`.
- FKs.
- `ON DELETE RESTRICT` nas FKs de equipe/estádio/árbitro/campeonato.

### 5.8 `ManterClassificacao`

[`ManterClassificacao.java`](../Java_Champs/src/br/com/entidade/ManterClassificacao.java#L8-L121)

| Método | SQL | Uso |
|---|---|---|
| `inscreverEquipe(int,int)` | `CALL sp_inscrever_equipe(?, ?)` | botão Inscrever |
| `removerInscricao(int,int)` | `DELETE FROM CLASSIFICACAO WHERE id_campeonato=? AND id_equipe=?` | botão Remover |
| `atualizarStatus(int,int,String)` | `UPDATE CLASSIFICACAO SET status_atual=? WHERE ...` | botão Status |
| `pesquisarRegistro(int,int)` | `SELECT ... JOIN EQUIPE WHERE ...` | consulta pontual |
| `listarPorCampeonato(int)` | `SELECT ... JOIN EQUIPE WHERE ... ORDER BY ...` | tabela da aba 6 |
| `montarClassificacao(ResultSet)` | — | conversão |

#### `inscreverEquipe`

```java
String query = "CALL sp_inscrever_equipe(?, ?)";
pst = (PreparedStatement) con.prepareStatement(query);
pst.setInt(1, idCampeonato);
pst.setInt(2, idEquipe);
pst.execute();
```

- Usa `PreparedStatement` (não `CallableStatement`) para chamar a procedure. Funciona porque `CALL` é um comando SQL; o `CallableStatement` seria mais “correto”, mas o efeito é o mesmo.
- A procedure faz `INSERT IGNORE` + sincronização.

#### `removerInscricao`

- `DELETE` direto na `CLASSIFICACAO`.
- O trigger `trg_classificacao_before_delete` bloqueia se a equipe já tiver partida.
- Se a equipe não tiver partida, a linha é removida e a classificação não precisa ser recalculada (a equipe sai da tabela).

#### `atualizarStatus`

- Atualiza `status_atual` para `A_JOGAR`, `CLASSIFICADO` ou `ELIMINADO`.
- **Não** mexe em pontos/colocação; é uma decisão manual do organizador.

#### `pesquisarRegistro(int,int)`

```java
String query = "SELECT c.id_campeonato, c.id_equipe, e.nome AS nome_equipe, "
        + "c.colocacao_equipe, c.partidas_jogadas, c.vitorias, c.empates, c.derrotas, "
        + "c.gols_marcados, c.gols_sofridos, c.saldo_gols, c.pontuacao, c.status_atual "
        + "FROM CLASSIFICACAO c "
        + "INNER JOIN EQUIPE e ON e.id_equipe = c.id_equipe "
        + "WHERE c.id_campeonato = ? AND c.id_equipe = ?";
```

- A chave é composta, então a busca precisa dos **dois** IDs.
- `montarClassificacao` usa `wasNull` para a colocação.
- Não é usada pela tela atual (a tela usa `listarPorCampeonato` e a linha selecionada), mas é o método de “pesquisa individual” da classificação.

#### `listarPorCampeonato(int)`

```java
String query = "SELECT ... FROM CLASSIFICACAO c "
        + "INNER JOIN EQUIPE e ON e.id_equipe = c.id_equipe "
        + "WHERE c.id_campeonato = ? "
        + "ORDER BY (c.colocacao_equipe IS NULL), c.colocacao_equipe ASC, e.nome ASC";
```

- Ordena pela colocação; quem tem `NULL` vai para o fim.
- É o que popula a `JTable` da aba 6.
- A consulta já traz `nome_equipe` do JOIN.

### 5.9 `ManterRelatorio`

[`ManterRelatorio.java`](../Java_Champs/src/br/com/entidade/ManterRelatorio.java#L17-L131)

| Método | Procedure | Retorno |
|---|---|---|
| `classificacaoPorCampeonato(int)` | `sp_relatorio_classificacao(?)` | `ArrayList<RelatorioClassificacao>` |
| `partidasPorPeriodo(int,String,String)` | `sp_relatorio_partidas_periodo(?, ?, ?)` | `ArrayList<RelatorioPartida>` |
| `fecharRecursos(CallableStatement)` | — | fecha cs, pst e con |

#### `classificacaoPorCampeonato`

```java
cs = con.prepareCall("{call sp_relatorio_classificacao(?)}");
cs.setInt(1, idCampeonato);
rs = cs.executeQuery();

while (rs.next()) {
    RelatorioClassificacao linha = new RelatorioClassificacao();
    linha.setNomeCampeonato(rs.getString("campeonato"));

    int posicao = rs.getInt("posicao");
    linha.setPosicao(rs.wasNull() ? null : posicao);

    linha.setNomeEquipe(rs.getString("equipe"));
    linha.setCidade(rs.getString("cidade"));
    linha.setPontos(rs.getInt("pontos"));
    ...
}
```

- **`prepareCall`**: prepara a chamada da procedure.
- **`{call ...}`**: sintaxe JDBC.
- **`setInt`**: parâmetro de entrada.
- **`executeQuery`**: a procedure retorna um `ResultSet`.
- **`wasNull`**: posição `NULL` vira `null`.
- Cada linha vira um `RelatorioClassificacao`.

#### `partidasPorPeriodo`

```java
cs = con.prepareCall("{call sp_relatorio_partidas_periodo(?, ?, ?)}");
cs.setInt(1, idCampeonato);
cs.setDate(2, java.sql.Date.valueOf(dataInicio));
cs.setDate(3, java.sql.Date.valueOf(dataFim));
rs = cs.executeQuery();
```

- **`setDate`**: as datas chegam como `String` `YYYY-MM-DD` da tela e são convertidas com `java.sql.Date.valueOf`.
- O `ResultSet` já vem com data formatada e placar montado pela procedure.

#### `fecharRecursos`

```java
private void fecharRecursos(CallableStatement cs) {
    if (cs != null) {
        try { cs.close(); } catch (SQLException ex) { ... }
    }
    try { fecharBanco(); } catch (Exception ex) { ... }
    if (con != null) {
        try { con.close(); } catch (SQLException ex) { ... }
        con = null;
    }
}
```

- Fecha o `CallableStatement` (e, por consequência, o `ResultSet`).
- Chama `fecharBanco()` (que tenta fechar `pst`; neste DAO, `pst` não é usado, então normalmente é `null`).
- Fecha a `Connection` e zera `con`.
- É o único DAO que fecha a conexão explicitamente. Bom exemplo para explicar a diferença.

---

## 6. A `TelaPrincipal` (View)

[`TelaPrincipal.java`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L21-L2073)

### 6.1 Visão geral

```java
public class TelaPrincipal extends javax.swing.JFrame {

    // um DAO por entidade
    private final ManterCampeonato daoCampeonato = new ManterCampeonato();
    private final ManterEquipe daoEquipe = new ManterEquipe();
    private final ManterJogador daoJogador = new ManterJogador();
    private final ManterEstadio daoEstadio = new ManterEstadio();
    private final ManterArbitro daoArbitro = new ManterArbitro();
    private final ManterClassificacao daoClassificacao = new ManterClassificacao();
    private final ManterPartida daoPartida = new ManterPartida();
    private final ManterRelatorio daoRelatorio = new ManterRelatorio();

    public TelaPrincipal() {
        initComponents();
        configurarJanelaCompleta();
        recarregarTudo();
    }
}
```

- **`extends JFrame`**: herda a janela do Swing.
- **Oito DAOs `final`**: criados uma vez; cada um tem sua própria conexão/statement.
- **Construtor**: inicializa componentes, monta a janela e carrega os dados.

### 6.2 Os campos da tela

| Grupo | Campos | Tipo | Para que serve |
|---|---|---|---|
| DAOs | `daoCampeonato` … `daoRelatorio` | classes `Manter...` | acesso ao banco |
| Aba 1 | `txtIdCamp`, `txtNomeCamp`, `txtInicioCamp`, `txtFimCamp`, `txtBuscaCamp` | `JTextField` | formulário e pesquisa |
| Aba 1 | `cbStatusCamp` | `JComboBox<String>` | status |
| Aba 1 | `tblCampeonato` | `JTable` | listagem |
| Aba 2 | `txtIdEquipe`, `txtNomeEquipe`, `txtCidadeEquipe`, `txtBuscaEquipe` | `JTextField` | formulário |
| Aba 2 | `tblEquipe` | `JTable` | listagem |
| Aba 3 | `txtIdJogador`, `txtNomeJogador`, `txtCamisaJogador`, `txtNascJogador`, `txtBuscaJogador` | `JTextField` | formulário |
| Aba 3 | `cbPosicaoJogador` | `JComboBox<String>` | posição |
| Aba 3 | `cbEquipeJogador` | `JComboBox<Equipe>` | FK equipe |
| Aba 3 | `tblJogador`, `cacheJogadores` | `JTable`, `ArrayList<Jogador>` | listagem e seleção |
| Aba 4 | `txtIdEstadio`, `txtNomeEstadio`, `txtCidadeEstadio`, `txtCapEstadio`, `txtBuscaEstadio` | `JTextField` | formulário |
| Aba 4 | `tblEstadio` | `JTable` | listagem |
| Aba 5 | `txtIdArbitro`, `txtNomeArbitro`, `txtRegArbitro`, `txtBuscaArbitro` | `JTextField` | formulário |
| Aba 5 | `cbCatArbitro` | `JComboBox<String>` | categoria |
| Aba 5 | `tblArbitro` | `JTable` | listagem |
| Aba 6 | `cbCampClass`, `cbEquipeInsc` | `JComboBox<Campeonato>`, `JComboBox<Equipe>` | seleção |
| Aba 6 | `cbStatusClass` | `JComboBox<String>` | status manual |
| Aba 6 | `tblClassificacao` | `JTable` | classificação |
| Aba 6 | `btnInscrever`, `btnStatusClass`, `btnDesinscrever` | `JButton` | ações |
| Aba 7 | `txtIdPartida`, `txtRodadaPartida`, `txtDataPartida`, `txtGolsMand`, `txtGolsVis`, `txtAcrescimo`, `txtBuscaPartida` | `JTextField` | formulário |
| Aba 7 | `cbCampPartida`, `cbMandante`, `cbVisitante`, `cbEstadioPartida`, `cbArbitroPartida` | `JComboBox` de objetos | FKs |
| Aba 7 | `cbStatusPartida`, `cbTempoPartida` | `JComboBox<String>` | status/tempo |
| Aba 7 | `tblPartida`, `cachePartidas` | `JTable`, `ArrayList<Partida>` | listagem e seleção |
| Aba 8 | `cbCampRelatorio`, `txtInicioRel`, `txtFimRel` | campos | filtros |
| Aba 8 | `tblRelClassificacao`, `tblRelPartidas` | `JTable` | relatórios |
| Aba 8 | `abasRelatorio`, `lblResumoRel` | `JTabbedPane`, `JLabel` | abas internas e rodapé |
| Aba 8 | `btnRelClassificacao`, `btnRelPartidas`, `btnImprimirRel` | `JButton` | ações |
| Aba 8 | `validarRelatorio` | `Runnable` | revalidação após gerar |

### 6.3 O construtor e a montagem da janela

```java
public TelaPrincipal() {
    initComponents();
    configurarJanelaCompleta();
    recarregarTudo();
}
```

1. **`initComponents()`**: código gerado pelo NetBeans; define `EXIT_ON_CLOSE`, layout e `pack()`. Como ainda não há componentes, o `pack()` apenas define um tamanho inicial mínimo.
2. **`configurarJanelaCompleta()`**:
   - título `"Champs - Sistema de Gestão de Campeonatos"`;
   - tamanho `1220 × 760`; mínimo `980 × 640`;
   - centraliza na tela (`setLocationRelativeTo(null)`);
   - `getContentPane().removeAll()`: limpa o que o `initComponents` fez;
   - `BorderLayout`;
   - cria o `JTabbedPane` com as 8 abas;
   - `revalidate()` e `repaint()`.
3. **`recarregarTudo()`**: consulta o banco e popula tabelas/combos.

**Por que `setSize` depois de `pack`?** Porque `pack()` calcula o tamanho preferido; `setSize` define o tamanho final desejado. A ordem no construtor é `initComponents` (com `pack`) → `configurarJanelaCompleta` (com `setSize`). O `setMinimumSize` impede encolher demais.

### 6.4 Os helpers de validação e de tabela

#### `vincularValidacao(Runnable, JTextField...)`

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
    validador.run();
}
```

- **`DocumentListener`**: dispara quando o **documento** (texto) do campo muda.
  - `insertUpdate`: caractere inserido;
  - `removeUpdate`: caractere removido;
  - `changedUpdate`: mudança de atributos (raro em `JTextField`).
- Os três chamam o mesmo `Runnable`.
- O `validador.run()` no final executa uma vez logo após a montagem, para o estado inicial dos botões ficar correto.
- **Ponto de atenção:** o validador roda a cada tecla; ele deve ser leve (é).
- **Por que a lambda consegue usar `btnSalvar` se ele é uma variável local?** Porque o Java permite capturar variáveis locais **efetivamente finais** (`effectively final`). Os botões são criados uma vez e nunca recebem outra atribuição; por isso podem ser usados dentro da lambda.

#### `aoMudarCombo(Runnable, JComboBox<?>...)`

```java
for (JComboBox<?> combo : combos) {
    combo.addActionListener(e -> validador.run());
}
```

- `JComboBox` dispara `ActionListener` quando a seleção muda.
- Usado para revalidar botões e para o listener de status da partida.

#### `aoSelecionarTabela(Runnable, JTable...)`

```java
tabela.getSelectionModel().addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting()) {
        validador.run();
    }
});
```

- `ListSelectionListener` dispara quando a seleção muda.
- **`getValueIsAdjusting()`**: durante um arrasto, o evento dispara várias vezes; só agimos quando o usuário terminou.
- Usado na aba 6 para habilitar status/remover.

#### `modelo(String...)`

```java
return new DefaultTableModel(colunas, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
};
```

- Cria um `DefaultTableModel` com as colunas e zero linhas.
- **`isCellEditable = false`**: a tabela é **somente leitura**; a edição é feita pelo formulário e pelo botão Alterar.
- Se fosse editável, o usuário poderia alterar a célula sem passar pelo DAO, e a tela ficaria inconsistente com o banco.

#### `configurarTabela(JTable)`

```java
tabela.setRowHeight(24);
tabela.setAutoCreateRowSorter(false);
tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
```

- Altura das linhas 24 px.
- **Sem ordenação automática**: em Jogador e Partida, a linha selecionada é usada como **índice do cache** (`cacheJogadores.get(row)`, `cachePartidas.get(row)`). Se o usuário clicasse no cabeçalho para ordenar, a linha 0 poderia ser outro registro e o cache ficaria dessincronizado. Desligar o sorter mantém a ordem do `SELECT` igual à do cache.
- Seleção simples: uma linha por vez.

#### Helpers de validação de texto

| Helper | O que faz | Exemplo |
|---|---|---|
| `vazio(String)` | `null` ou `trim().isEmpty()` | `"  "` → `true` |
| `dataValida(String)` | tenta `java.sql.Date.valueOf`; verdadeiro se for uma data ISO | `"2026-09-01"` → `true`; `"01/09/2026"` → `false` |
| `dataHoraValida(String)` | separa em duas partes, valida a data e a hora `HH:MM` ou `HH:MM:SS` | `"2026-09-10 16:00:00"` → `true`; `"2026-09-10 25:00"` → `false` |
| `dataNascimentoValida(String)` | vazio → `true`; senão data válida e não futura | hoje → `true`; amanhã → `false` |
| `inteiroOuNulo(String)` | vazio ou inválido → `null`; senão `Integer` | `"10"` → `10`; `"abc"` → `null` |
| `inteiroPositivo(String)` | inteiro > 0 | `"45000"` → `true`; `"0"` → `false` |
| `inteiroNaoNegativo(String)` | inteiro ≥ 0 | `"0"` → `true`; `"-1"` → `false` |

**Detalhes importantes:**

- `dataValida` usa `java.sql.Date.valueOf`, que espera o formato **ISO** `YYYY-MM-DD`. Formatos brasileiros não são aceitos; a tela mostra o formato no rótulo.
- `dataHoraValida` usa a regex `([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?`: aceita `00:00` a `23:59`, com segundos opcionais; não aceita `24:00`.
- `dataNascimentoValida` compara com `new java.sql.Date(System.currentTimeMillis())`; a data de hoje é permitida.
- `inteiroOuNulo` aceita números negativos; quem decide se pode é o chamador (`inteiroPositivo`, `inteiroNaoNegativo`).
- `Integer.parseInt` é usado depois, nos métodos `montarObjeto...`, quando o botão já está habilitado; a validação garante que o texto é numérico.

### 6.5 Aba 1 — Campeonato

[`criarPainelCampeonato`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L256-L449)

**Layout:**

- `JPanel` com `BorderLayout(10, 10)` e borda vazia de 10 px.
- Formulário `GridLayout(3, 4, 8, 8)`:

```text
ID (automático)          [txtIdCamp]
Nome do Campeonato       [txtNomeCamp]
Data Início (YYYY-MM-DD) [txtInicioCamp]   Data Fim (...) [txtFimCamp]
Status                   [cbStatusCamp]
Pesquisar (ID ou nome)   [txtBuscaCamp]
```

- Botões: Cadastrar, Consultar, Alterar, Excluir, Limpar.
- Tabela: `ID`, `Nome`, `Início`, `Fim`, `Status`, dentro de `JScrollPane`.

**Validação:**

```java
Runnable validar = () -> {
    boolean camposOk = !vazio(txtNomeCamp.getText())
            && dataValida(txtInicioCamp.getText())
            && dataValida(txtFimCamp.getText())
            && !java.sql.Date.valueOf(txtFimCamp.getText().trim())
                    .before(java.sql.Date.valueOf(txtInicioCamp.getText().trim()));
    boolean temId = !vazio(txtIdCamp.getText());
    btnSalvar.setEnabled(camposOk);
    btnConsultar.setEnabled(!vazio(txtBuscaCamp.getText()));
    btnAlterar.setEnabled(camposOk && temId);
    btnDeletar.setEnabled(temId);
};
vincularValidacao(validar, txtIdCamp, txtNomeCamp, txtInicioCamp, txtFimCamp, txtBuscaCamp);
```

| Botão | Condição |
|---|---|
| Cadastrar | nome preenchido + início válido + fim válido + fim ≥ início |
| Consultar | campo de pesquisa não vazio |
| Alterar | tudo do Cadastrar + ID preenchido |
| Excluir | ID preenchido |

- **Seleção na tabela:** preenche o formulário com `campeonatoDaTabela(row)`, que lê as cinco colunas da linha.
- **Cadastrar:** monta `Campeonato` sem ID; `daoCampeonato.inserir`; mensagem; `limparCamposCampeonato`; `recarregarTudo`.
- **Consultar:** chama `consultarCampeonato`, que usa `daoCampeonato.pesquisarRegistro(termo)`; se achar, preenche; se não, mensagem.
- **Alterar:** monta com o ID do campo; `daoCampeonato.alterar`; mensagem; `recarregarTudo`.
- **Excluir:** confirma; monta só com ID; `daoCampeonato.deletar`; mensagem; limpa; `recarregarTudo`.
- **Limpar:** zera campos, volta as datas padrão `2026-09-01`/`2026-12-30`, status índice 0 e limpa a seleção.

**Por que o ID é somente leitura?** Porque é `AUTO_INCREMENT`; o usuário não deve escolhê-lo. Ele é preenchido após consultar/selecionar.

**Erros comuns:** nome repetido (`Duplicate entry`), data final anterior (`chk_campeonato_datas`), campanha com partidas ao excluir (FK 1451).

### 6.6 Aba 2 — Equipe

[`criarPainelEquipe`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L454-L611)

- Formulário `GridLayout(2, 4, 8, 8)`: ID, Nome, Cidade, Pesquisar.
- Botões: Cadastrar, Consultar, Alterar, Excluir, Limpar.
- Tabela: `ID`, `Nome`, `Cidade`.
- Validação: nome e cidade não vazios; ID para Alterar/Excluir; pesquisa para Consultar.
- Seleção: lê direto das células da tabela (`getValueAt`), sem cache.
- Consultar: `daoEquipe.pesquisarRegistro(termo)` aceita ID, nome ou cidade.
- Cadastrar/Alterar/Excluir: mesmo padrão da aba 1.
- **Erro de FK:** excluir equipe com jogador/inscrição/partida → 1451; `mostrarErro` traduz.

### 6.7 Aba 3 — Jogador (CRUD extra)

[`criarPainelJogador`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L616-L818)

**Layout:**

- `GridLayout(4, 4, 8, 8)`:
  - ID, Nome;
  - Posição (`JComboBox<String>` com `GOLEIRO`, `ZAGUEIRO`, `LATERAL`, `VOLANTE`, `MEIA`, `ATACANTE`);
  - Camisa (1 a 99);
  - Nascimento (`YYYY-MM-DD`, opcional);
  - Equipe (`JComboBox<Equipe>`);
  - Pesquisar.
- Tabela: `ID`, `Nome`, `Posição`, `Camisa`, `Nascimento`, `Equipe`.

**Validação:**

```java
Integer camisa = inteiroOuNulo(txtCamisaJogador.getText());
boolean camisaOk = camisa != null && camisa >= 1 && camisa <= 99;
boolean camposOk = !vazio(txtNomeJogador.getText())
        && camisaOk
        && dataNascimentoValida(txtNascJogador.getText())
        && cbEquipeJogador.getSelectedItem() != null;
boolean temId = !vazio(txtIdJogador.getText());
```

| Botão | Condição |
|---|---|
| Cadastrar | nome + camisa 1..99 + data válida/não futura/vazia + equipe selecionada |
| Consultar | termo de pesquisa não vazio |
| Alterar | tudo do Cadastrar + ID |
| Excluir | ID |

- **`cacheJogadores`**: a lista de objetos na mesma ordem das linhas; a seleção usa `cacheJogadores.get(row)`.
- **`preencherCamposJogador`**: preenche ID, nome, posição, camisa, nascimento (vazio se `null`) e seleciona a equipe pelo ID.
- **`montarObjetoJogador`**:
  ```java
  Jogador j = new Jogador();
  j.setNome(txtNomeJogador.getText().trim());
  j.setPosicao(cbPosicaoJogador.getSelectedItem().toString());
  j.setNumeroCamisa(Integer.parseInt(txtCamisaJogador.getText().trim()));
  j.setDataNascimento(vazio(txtNascJogador.getText())
          ? null : txtNascJogador.getText().trim());
  j.setIdEquipe(((Equipe) cbEquipeJogador.getSelectedItem()).getIdEquipe());
  ```
  - A FK vem do **objeto Equipe** selecionado, não de um texto.
  - Data vazia vira `null`.
- **Cadastrar/Alterar/Excluir:** padrão; o Alterar adiciona o ID depois de montar.
- **Limpar:** zera; se houver equipes, seleciona a primeira.
- **Erros:** camisa repetida (`uq_jogador_camisa`), camisa fora de 1..99 (`chk_jogador_camisa`), equipe inexistente (FK), equipe com jogadores ao excluir (FK).

### 6.8 Aba 4 — Estádio

[`criarPainelEstadio`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L823-L993)

- Formulário `GridLayout(3, 4, 8, 8)`: ID, Nome, Cidade, Capacidade, Pesquisar.
- Tabela: `ID`, `Nome`, `Cidade`, `Capacidade`.
- Validação: nome, cidade e `inteiroPositivo(capacidade)`.
- Seleção: lê as células.
- Consultar: `daoEstadio.pesquisarRegistro` aceita ID, nome ou cidade.
- Erros: capacidade ≤ 0 (`chk_estadio_capacidade`), nome+cidade repetidos (`uq_estadio_nome_cidade`), FK ao excluir.

### 6.9 Aba 5 — Árbitro

[`criarPainelArbitro`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L998-L1166)

- Formulário `GridLayout(3, 4, 8, 8)`: ID, Nome, Registro Federação, Categoria, Pesquisar.
- Categorias: `LOCAL`, `ESTADUAL`, `NACIONAL`, `FIFA`.
- Tabela: `ID`, `Nome`, `Registro`, `Categoria`.
- Validação: nome e registro não vazios.
- Consultar: ID, nome ou registro.
- Erros: registro repetido (`uq_arbitro_registro`), FK ao excluir.

### 6.10 Aba 6 — Inscrição & Classificação

[`criarPainelClassificacao`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L1171-L1319)

**Layout:**

- Combos: `Campeonato`, `Equipe para Inscrição`, `Status na Competição`.
- Botões: `Inscrever Equipe no Campeonato`, `Atualizar Status da Equipe Selecionada`, `Remover Inscrição`.
- Tabela: `Posição`, `ID Equipe`, `Equipe`, `PTS`, `J`, `V`, `E`, `D`, `GP`, `GC`, `SG`, `Status`.
- Larguras de coluna definidas em `largurasClassificacao`; `setFillsViewportHeight(true)`.

**Listeners:**

```java
cbCampClass.addActionListener(e -> carregarTabelaClassificacao());
aoMudarCombo(this::atualizarBotoesClassificacao, cbCampClass, cbEquipeInsc);
aoSelecionarTabela(this::atualizarBotoesClassificacao, tblClassificacao);
```

- Trocar o campeonato recarrega a tabela (consulta `ManterClassificacao.listarPorCampeonato`).
- Trocar campeonato/equipe ou selecionar linha reavalia os botões.

**`atualizarBotoesClassificacao`:**

```java
boolean temCampeonato = cbCampClass.getSelectedItem() != null;
boolean temEquipe = cbEquipeInsc.getSelectedItem() != null;
boolean temLinha = tblClassificacao.getSelectedRow() >= 0;
btnInscrever.setEnabled(temCampeonato && temEquipe);
btnStatusClass.setEnabled(temCampeonato && temLinha);
btnDesinscrever.setEnabled(temCampeonato && temLinha);
```

| Botão | Condição |
|---|---|
| Inscrever | campeonato e equipe selecionados |
| Atualizar status | campeonato + linha selecionada |
| Remover inscrição | campeonato + linha selecionada |

**Inscrever:**

```java
daoClassificacao.inscreverEquipe(c.getIdCampeonato(), eq.getIdEquipe());
JOptionPane.showMessageDialog(this, "Equipe inscrita no campeonato!");
carregarTabelaClassificacao();
```

- Chama `CALL sp_inscrever_equipe`.
- Se a equipe já estava inscrita, `INSERT IGNORE` não duplica, mas a mensagem de sucesso ainda aparece. É uma mensagem “otimista”; o estado real é que nada mudou.

**Atualizar status:**

```java
int idEquipe = Integer.parseInt(tblClassificacao.getValueAt(row, 1).toString());
daoClassificacao.atualizarStatus(c.getIdCampeonato(), idEquipe,
        cbStatusClass.getSelectedItem().toString());
```

- Coluna 1 é `ID Equipe`.
- O status é manual; não afeta pontos/colocação.

**Remover inscrição:**

- Confirma.
- `daoClassificacao.removerInscricao`.
- O trigger `trg_classificacao_before_delete` recusa se a equipe tiver partidas.

**`carregarTabelaClassificacao`:**

```java
Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
DefaultTableModel mCl = (DefaultTableModel) tblClassificacao.getModel();
mCl.setRowCount(0);
if (c != null) {
    ArrayList<Classificacao> lista = daoClassificacao.listarPorCampeonato(c.getIdCampeonato());
    for (Classificacao cl : lista) {
        mCl.addRow(new Object[]{
            cl.getColocacaoEquipe() == null ? "-" : cl.getColocacaoEquipe() + "º",
            cl.getIdEquipe(), cl.getNomeEquipe(), cl.getPontuacao(),
            cl.getPartidasJogadas(), cl.getVitorias(), cl.getEmpates(), cl.getDerrotas(),
            cl.getGolsMarcados(), cl.getGolsSofridos(), cl.getSaldoGols(), cl.getStatusAtual()
        });
    }
}
```

- A posição `NULL` vira `"-"`.
- A consulta já vem ordenada por colocação/pontos; quem não jogou fica no fim.
- O `finally` chama `atualizarBotoesClassificacao()`.

### 6.11 Aba 7 — Partidas (CRUD principal)

[`criarPainelPartidas`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L1324-L1625)

**Layout `GridLayout(6, 4, 6, 6)`:**

```text
ID Partida                    [txtIdPartida]
Campeonato (FK)               [cbCampPartida]
Rodada (> 0)                  [txtRodadaPartida]
Data/Hora (YYYY-MM-DD HH:MM:SS) [txtDataPartida]
Equipe Mandante (FK)          [cbMandante]
Equipe Visitante (FK)         [cbVisitante]
Estádio (FK)                  [cbEstadioPartida]
Árbitro (FK)                  [cbArbitroPartida]
Status / Tempo                [cbStatusPartida] [cbTempoPartida]
Placar (Mand x Vis) / Acrésc  [txtGolsMand] [txtGolsVis] [txtAcrescimo]
Pesquisar (...)               [txtBuscaPartida]
```

**O listener do status:**

```java
cbStatusPartida.addActionListener(e -> {
    String status = cbStatusPartida.getSelectedItem().toString();
    boolean emAndamento = status.equals("EM_ANDAMENTO");
    cbTempoPartida.setEnabled(emAndamento);
    if (!emAndamento) {
        cbTempoPartida.setSelectedIndex(0);
    }
    if (status.equals("AGENDADA") || status.equals("CANCELADA")) {
        txtGolsMand.setText("");
        txtGolsVis.setText("");
    } else if (status.equals("ENCERRADA")) {
        if (txtGolsMand.getText().trim().isEmpty()) {
            txtGolsMand.setText("0");
        }
        if (txtGolsVis.getText().trim().isEmpty()) {
            txtGolsVis.setText("0");
        }
    }
});
cbTempoPartida.setEnabled(false);
```

- **`AGENDADA`/`CANCELADA`**: limpa os gols (o banco exige `NULL`).
- **`ENCERRADA`**: se um gol estiver vazio, preenche com `0` (o banco exige os dois).
- **`EM_ANDAMENTO`**: habilita o tempo; não mexe nos gols.
- **Outros status**: desabilita o tempo e volta para o índice 0 (string vazia).
- **Inicialmente**, `cbTempoPartida` fica desabilitado.

**Validação:**

```java
boolean combosOk = cbCampPartida.getSelectedItem() != null
        && cbMandante.getSelectedItem() != null
        && cbVisitante.getSelectedItem() != null
        && cbEstadioPartida.getSelectedItem() != null
        && cbArbitroPartida.getSelectedItem() != null;

boolean golsOk = (vazio(txtGolsMand.getText()) && vazio(txtGolsVis.getText()))
        || (inteiroNaoNegativo(txtGolsMand.getText())
        && inteiroNaoNegativo(txtGolsVis.getText()));
if ("ENCERRADA".equals(cbStatusPartida.getSelectedItem())) {
    golsOk = inteiroNaoNegativo(txtGolsMand.getText())
            && inteiroNaoNegativo(txtGolsVis.getText());
}

boolean timesDiferentes = true;
Equipe mandante = (Equipe) cbMandante.getSelectedItem();
Equipe visitante = (Equipe) cbVisitante.getSelectedItem();
if (mandante != null && visitante != null) {
    timesDiferentes = mandante.getIdEquipe() != visitante.getIdEquipe();
}

boolean camposOk = combosOk
        && inteiroPositivo(txtRodadaPartida.getText())
        && dataHoraValida(txtDataPartida.getText())
        && inteiroNaoNegativo(txtAcrescimo.getText())
        && golsOk
        && timesDiferentes;
boolean temId = !vazio(txtIdPartida.getText());
```

| Botão | Condição |
|---|---|
| Registrar / Agendar | combos + rodada > 0 + data/hora válida + acréscimo ≥ 0 + gols coerentes + mandante ≠ visitante |
| Consultar | termo de pesquisa não vazio |
| Salvar Placar / Alterar | tudo do Agendar + ID |
| Excluir Partida | ID |

**Tabela:** `ID`, `Campeonato`, `Rodada`, `Data/Hora`, `Mandante`, `Placar`, `Visitante`, `Estádio`, `Árbitro`, `Status`.

**Cache:** `cachePartidas` guarda os objetos na mesma ordem das linhas; a seleção usa `cachePartidas.get(row)`.

**Ações:**

- **Agendar:** `montarObjetoPartida()` → `daoPartida.inserir` → mensagem “Partida registrada e classificação sincronizada!” → `recarregarTudo`.
- **Consultar:** `daoPartida.pesquisarRegistro(termo)` → preenche; o termo pode ser ID, equipe, campeonato, estádio ou árbitro.
- **Alterar:** `montarObjetoPartida()` + ID → `daoPartida.alterar` → mensagem “Partida atualizada e classificação recalculada!” → `recarregarTudo`.
- **Excluir:** confirma → `daoPartida.deletar` → mensagem “Partida removida e classificação recalculada!” → limpa → `recarregarTudo`.
- **Limpar:** zera ID, rodada 1, gols, acréscimo 0, busca, status índice 0, tempo índice 0 e desabilita o tempo.

**`preencherCamposPartida`:**

```java
txtIdPartida.setText(...);
txtRodadaPartida.setText(...);
txtDataPartida.setText(p.getDataPartida());
cbStatusPartida.setSelectedItem(p.getStatus());
cbTempoPartida.setSelectedItem(p.getTempoPartida() == null ? "" : p.getTempoPartida());
txtGolsMand.setText(p.getGolsMandante() == null ? "" : ...);
txtGolsVis.setText(p.getGolsVisitante() == null ? "" : ...);
txtAcrescimo.setText(...);

selecionarComboCampeonato(cbCampPartida, p.getIdCampeonato());
selecionarComboEquipe(cbMandante, p.getIdMandante());
selecionarComboEquipe(cbVisitante, p.getIdVisitante());
selecionarComboEstadio(cbEstadioPartida, p.getIdEstadio());
selecionarComboArbitro(cbArbitroPartida, p.getIdArbitro());
```

- O status é definido antes dos gols; o listener do status pode limpar/preencher os gols, mas os valores corretos são escritos **depois**, então o resultado final é o da partida.
- Os combos guardam objetos; `selecionarComboX` percorre os itens comparando IDs.

**`montarObjetoPartida`:**

```java
Partida p = new Partida();
p.setIdCampeonato(((Campeonato) cbCampPartida.getSelectedItem()).getIdCampeonato());
p.setRodada(Integer.parseInt(txtRodadaPartida.getText().trim()));
p.setIdMandante(((Equipe) cbMandante.getSelectedItem()).getIdEquipe());
p.setIdVisitante(((Equipe) cbVisitante.getSelectedItem()).getIdEquipe());
p.setIdEstadio(((Estadio) cbEstadioPartida.getSelectedItem()).getIdEstadio());
p.setIdArbitro(((Arbitro) cbArbitroPartida.getSelectedItem()).getIdArbitro());
p.setDataPartida(txtDataPartida.getText().trim());

String gm = txtGolsMand.getText().trim();
String gv = txtGolsVis.getText().trim();
p.setGolsMandante(gm.isEmpty() ? null : Integer.parseInt(gm));
p.setGolsVisitante(gv.isEmpty() ? null : Integer.parseInt(gv));

p.setAcrescimo(Integer.parseInt(txtAcrescimo.getText().trim()));
p.setStatus(cbStatusPartida.getSelectedItem().toString());
String tempo = cbTempoPartida.getSelectedItem().toString();
p.setTempoPartida(tempo.isEmpty() ? null : tempo);
```

- Os `cast` são seguros porque os combos só contêm esses tipos.
- Gols vazios viram `null`.
- `tempo` vazio vira `null`.
- O `status` é a string do combo (`AGENDADA`, etc.).

**O que a tela **não** valida (e o banco valida):**

- data dentro do período do campeonato;
- equipes inscritas;
- equipe uma vez por rodada;
- árbitro/estádio livres no horário;
- confronto único por rodada.

### 6.12 Aba 8 — Relatórios

[`criarPainelRelatorios`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L1630-L1850)

**Layout:**

- Formulário: `Campeonato`, `Data Início`, `Data Fim`.
- Botões: `Relatório de Classificação (Principal)`, `Partidas por Período`, `Imprimir / Salvar em PDF`, `Limpar`.
- `JTabbedPane` interno com duas abas:
  - “Classificação (rotina principal)” → `tblRelClassificacao`;
  - “Partidas por período” → `tblRelPartidas`.
- `lblResumoRel` no rodapé.

**Colunas da tabela de classificação:**

`Pos.`, `Equipe`, `Cidade`, `PTS`, `J`, `V`, `E`, `D`, `GP`, `GC`, `SG`, `Aprov.`, `Casa`, `Fora`, `Estádios`, `Árbitros`, `Status`.

**Colunas da tabela de partidas:**

`Rodada`, `Data/Hora`, `Mandante`, `Visitante`, `Estádio`, `Árbitro`, `Placar`, `Status`.

**Validação:**

```java
boolean temCamp = cbCampRelatorio.getSelectedItem() != null;
boolean datasOk = dataValida(txtInicioRel.getText())
        && dataValida(txtFimRel.getText())
        && !java.sql.Date.valueOf(txtFimRel.getText().trim())
                .before(java.sql.Date.valueOf(txtInicioRel.getText().trim()));
btnRelClassificacao.setEnabled(temCamp);
btnRelPartidas.setEnabled(temCamp && datasOk);
btnImprimirRel.setEnabled(tabelaRelatorioAtual().getRowCount() > 0);
```

| Botão | Condição |
|---|---|
| Relatório de Classificação | campeonato selecionado |
| Partidas por Período | campeonato + datas válidas + fim ≥ início |
| Imprimir / Salvar em PDF | tabela atual com pelo menos uma linha |

- `vincularValidacao(validar, txtInicioRel, txtFimRel)` roda a cada tecla.
- `cbCampRelatorio.addActionListener(e -> validar.run())`.
- `abasRelatorio.addChangeListener(e -> validar.run())`: ao trocar de aba, o botão de impressão é reavaliado para a tabela visível.
- `validarRelatorio = validar;` guarda o `Runnable` para ser chamado depois de gerar um relatório (quando as linhas são adicionadas).

**`tabelaRelatorioAtual`:**

```java
if (abasRelatorio == null || abasRelatorio.getSelectedIndex() <= 0) {
    return tblRelClassificacao;
}
return tblRelPartidas;
```

- Índice 0 = classificação; índice 1 = partidas.
- Durante a construção, `abasRelatorio` pode ser `null`; nesse caso devolve a tabela de classificação.

**`gerarRelatorioClassificacao`:**

```java
ArrayList<RelatorioClassificacao> lista =
        daoRelatorio.classificacaoPorCampeonato(c.getIdCampeonato());
DefaultTableModel m = (DefaultTableModel) tblRelClassificacao.getModel();
m.setRowCount(0);

int somaJogos = 0;
int somaGols = 0;
for (RelatorioClassificacao linha : lista) {
    m.addRow(new Object[]{
        linha.getPosicaoFormatada(),
        linha.getNomeEquipe(),
        linha.getCidade(),
        linha.getPontos(),
        linha.getJogos(),
        linha.getVitorias(),
        linha.getEmpates(),
        linha.getDerrotas(),
        linha.getGolsPro(),
        linha.getGolsContra(),
        linha.getSaldoGols(),
        linha.getAproveitamentoFormatado(),
        linha.getJogosCasa(),
        linha.getJogosFora(),
        linha.getEstadios(),
        linha.getArbitros(),
        linha.getStatusAtual()
    });
    somaJogos += linha.getJogos();
    somaGols += linha.getGolsPro();
}

int partidas = somaJogos / 2;
int gols = somaGols / 2;
String media = partidas == 0 ? "0,00"
        : String.format(java.util.Locale.ROOT, "%.2f", (double) gols / partidas);
lblResumoRel.setText("Classificação de " + c.getNome()
        + "  |  Equipes: " + lista.size()
        + "  |  Partidas encerradas: " + partidas
        + "  |  Gols: " + gols
        + "  |  Média de gols por partida: " + media);
abasRelatorio.setSelectedIndex(0);
if (validarRelatorio != null) {
    validarRelatorio.run();
}

if (lista.isEmpty()) {
    JOptionPane.showMessageDialog(this,
            "Nenhuma equipe inscrita neste campeonato. Use a aba de Inscrição.");
}
```

- A procedure devolve uma linha por equipe inscrita.
- `somaJogos` conta cada partida **duas vezes** (uma por equipe), então `partidas = somaJogos / 2`.
- `somaGols` soma os gols pró das duas equipes, então `gols = somaGols / 2`.
- A média é `gols / partidas` com duas casas; se não há partida, `"0,00"`.
- O rodapé mostra equipes, partidas encerradas, gols e média.
- Se a lista estiver vazia, avisa para usar a aba de inscrição.

**`gerarRelatorioPartidas`:**

```java
ArrayList<RelatorioPartida> lista = daoRelatorio.partidasPorPeriodo(
        c.getIdCampeonato(), txtInicioRel.getText().trim(), txtFimRel.getText().trim());
...
int encerradas = 0, canceladas = 0, agendadas = 0;
for (RelatorioPartida linha : lista) {
    m.addRow(new Object[]{ ... });
    if ("ENCERRADA".equals(linha.getStatus())) encerradas++;
    else if ("CANCELADA".equals(linha.getStatus())) canceladas++;
    else if ("AGENDADA".equals(linha.getStatus())) agendadas++;
}
lblResumoRel.setText("Partidas de " + c.getNome()
        + "  |  Total: " + lista.size()
        + "  |  Encerradas: " + encerradas
        + "  |  Agendadas: " + agendadas
        + "  |  Canceladas: " + canceladas);
abasRelatorio.setSelectedIndex(1);
if (validarRelatorio != null) {
    validarRelatorio.run();
}
```

- A procedure filtra por campeonato e período e devolve todos os status.
- O resumo conta encerradas, agendadas e canceladas; `EM_ANDAMENTO` não entra em nenhum dos três contadores (mas aparece no total).
- `abasRelatorio.setSelectedIndex(1)` mostra a aba de partidas.

**Impressão:**

```java
JTable tabela = tabelaRelatorioAtual();
if (tabela.getRowCount() == 0) { ... }
String titulo = abasRelatorio.getSelectedIndex() == 0
        ? "Champs - Classificação"
        : "Champs - Partidas por período";
try {
    tabela.print(JTable.PrintMode.FIT_WIDTH,
            new MessageFormat(titulo),
            new MessageFormat("Página {0}"));
} catch (java.awt.print.PrinterException ex) {
    JOptionPane.showMessageDialog(this,
            "Não foi possível imprimir: " + ex.getMessage(),
            "Impressão", JOptionPane.ERROR_MESSAGE);
}
```

- **`JTable.PrintMode.FIT_WIDTH`**: ajusta a tabela à largura da página.
- **`MessageFormat(titulo)`**: cabeçalho.
- **`MessageFormat("Página {0}")`**: rodapé com o número da página; o Swing substitui `{0}`.
- A janela de impressão do sistema permite escolher impressora ou “Salvar como PDF”.
- **Limitação:** o cabeçalho/rodapé é simples; não há logo, data ou nome do campeonato no PDF.

**Limpar:**

```java
((DefaultTableModel) tblRelClassificacao.getModel()).setRowCount(0);
((DefaultTableModel) tblRelPartidas.getModel()).setRowCount(0);
lblResumoRel.setText("Selecione um campeonato e gere um relatório.");
validar.run();
```

- Limpa as duas tabelas e o resumo; o botão de impressão desabilita.

### 6.13 `recarregarTudo()` — a sincronização da tela

[`recarregarTudo`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L1855-L1952)

```java
private void recarregarTudo() {
    try {
        Campeonato campSelecionado = (Campeonato) cbCampRelatorio.getSelectedItem();

        ArrayList<Campeonato> camps = daoCampeonato.pesquisarTudo();
        DefaultTableModel mCamp = (DefaultTableModel) tblCampeonato.getModel();
        mCamp.setRowCount(0);
        cbCampClass.removeAllItems();
        cbCampPartida.removeAllItems();
        cbCampRelatorio.removeAllItems();
        for (Campeonato c : camps) {
            mCamp.addRow(new Object[]{
                c.getIdCampeonato(), c.getNome(), c.getData_inicio(),
                c.getData_fim(), c.getStatus()
            });
            cbCampClass.addItem(c);
            cbCampPartida.addItem(c);
            cbCampRelatorio.addItem(c);
        }
        if (campSelecionado != null) {
            selecionarComboCampeonato(cbCampRelatorio, campSelecionado.getIdCampeonato());
        }

        ArrayList<Equipe> equipes = daoEquipe.pesquisarTudo();
        DefaultTableModel mEq = (DefaultTableModel) tblEquipe.getModel();
        mEq.setRowCount(0);
        cbEquipeInsc.removeAllItems();
        cbMandante.removeAllItems();
        cbVisitante.removeAllItems();
        cbEquipeJogador.removeAllItems();
        for (Equipe eq : equipes) {
            mEq.addRow(new Object[]{eq.getIdEquipe(), eq.getNome(), eq.getCidade()});
            cbEquipeInsc.addItem(eq);
            cbMandante.addItem(eq);
            cbVisitante.addItem(eq);
            cbEquipeJogador.addItem(eq);
        }

        cacheJogadores = daoJogador.pesquisarTudo();
        DefaultTableModel mJog = (DefaultTableModel) tblJogador.getModel();
        mJog.setRowCount(0);
        for (Jogador j : cacheJogadores) {
            mJog.addRow(new Object[]{
                j.getIdJogador(), j.getNome(), j.getPosicao(), j.getNumeroCamisa(),
                j.getDataNascimento() == null ? "" : j.getDataNascimento(),
                j.getNomeEquipe()
            });
        }

        ArrayList<Estadio> estadios = daoEstadio.pesquisarTudo();
        DefaultTableModel mEst = (DefaultTableModel) tblEstadio.getModel();
        mEst.setRowCount(0);
        cbEstadioPartida.removeAllItems();
        for (Estadio es : estadios) {
            mEst.addRow(new Object[]{
                es.getIdEstadio(), es.getNome(), es.getCidade(), es.getCapacidade()
            });
            cbEstadioPartida.addItem(es);
        }

        ArrayList<Arbitro> arbitros = daoArbitro.pesquisarTudo();
        DefaultTableModel mArb = (DefaultTableModel) tblArbitro.getModel();
        mArb.setRowCount(0);
        cbArbitroPartida.removeAllItems();
        for (Arbitro a : arbitros) {
            mArb.addRow(new Object[]{
                a.getIdArbitro(), a.getNome(), a.getRegistro_federacao(), a.getCategoria()
            });
            cbArbitroPartida.addItem(a);
        }

        carregarTabelaClassificacao();

        cachePartidas = daoPartida.pesquisarTudo();
        DefaultTableModel mPart = (DefaultTableModel) tblPartida.getModel();
        mPart.setRowCount(0);
        for (Partida p : cachePartidas) {
            String placar = (p.getGolsMandante() == null)
                    ? "vs"
                    : (p.getGolsMandante() + " x " + p.getGolsVisitante());
            mPart.addRow(new Object[]{
                p.getIdPartida(), p.getNomeCampeonato(), p.getRodada(), p.getDataPartida(),
                p.getNomeMandante(), placar, p.getNomeVisitante(),
                p.getNomeEstadio(), p.getNomeArbitro(), p.getStatus()
            });
        }
    } catch (Exception ex) {
        System.out.println("Aviso ao carregar dados: " + ex.getMessage());
    }
}
```

#### A ordem importa

1. **Campeonatos** primeiro, porque alimentam os combos de inscrição, partida e relatório.
2. **Equipes** depois, porque alimentam inscrição, jogador, mandante, visitante e relatório.
3. **Jogadores** depois das equipes, porque o JOIN precisa das equipes.
4. **Estádios** e **árbitros** para os combos da partida.
5. **Classificação** por último entre os cadastros, porque usa o campeonato selecionado.
6. **Partidas** por último, porque usam todos os anteriores (JOIN).

#### O que é preservado

- Apenas a seleção do **campeonato do relatório** é preservada, via `campSelecionado` e `selecionarComboCampeonato`.
- As seleções de `cbCampClass`, `cbCampPartida`, `cbEquipeInsc`, `cbMandante`, `cbVisitante`, `cbEstadioPartida`, `cbArbitroPartida` e `cbEquipeJogador` **não** são preservadas; após o reload, voltam ao primeiro item.
- Isso é simples, mas pode surpreender: após cadastrar uma partida, os combos voltam ao primeiro campeonato/time.

#### Efeitos colaterais dos listeners

- `cbCampClass.removeAllItems()` / `addItem(...)` disparam `ActionListener` que chama `carregarTabelaClassificacao()`.
- Durante o reload, isso pode executar várias consultas de classificação.
- `cbCampRelatorio` também dispara o `validar` do relatório.
- Funciona, mas é um ponto de ineficiência; uma evolução seria usar `removeActionListener`/`addActionListener` ao redor do reload ou um flag `carregando`.

#### Tratamento de erro

- O `catch` só imprime no console: `"Aviso ao carregar dados: ..."`.
- Se o banco estiver desligado na inicialização, a janela abre vazia e o usuário só descobre o problema ao tentar uma ação; não há `JOptionPane` nesse ponto.
- Se um DAO falhar no meio, os seguintes não são carregados (a exceção interrompe o `try`).

### 6.14 `carregarTabelaClassificacao` e os botões

[`carregarTabelaClassificacao`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L1954-L1977)

- Obtém o campeonato selecionado em `cbCampClass`.
- Limpa a tabela.
- Se houver campeonato, chama `daoClassificacao.listarPorCampeonato`.
- Adiciona as linhas; a posição `null` vira `"-"`.
- No `finally`, chama `atualizarBotoesClassificacao()` para refletir a seleção atual.

**`atualizarBotoesClassificacao`:**

```java
if (btnInscrever == null) {
    return;
}
boolean temCampeonato = cbCampClass.getSelectedItem() != null;
boolean temEquipe = cbEquipeInsc.getSelectedItem() != null;
boolean temLinha = tblClassificacao.getSelectedRow() >= 0;
btnInscrever.setEnabled(temCampeonato && temEquipe);
btnStatusClass.setEnabled(temCampeonato && temLinha);
btnDesinscrever.setEnabled(temCampeonato && temLinha);
```

- O `if (btnInscrever == null)` protege chamadas durante a construção, antes de o botão existir.
- A linha selecionada é a fonte do `id_equipe` para status/remoção.

### 6.15 Os seletores de combo

[`selecionarComboCampeonato`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L1981-L2015) e equivalentes para equipe, estádio e árbitro:

```java
private void selecionarComboEquipe(JComboBox<Equipe> cb, int id) {
    for (int i = 0; i < cb.getItemCount(); i++) {
        if (cb.getItemAt(i).getIdEquipe() == id) {
            cb.setSelectedIndex(i);
            return;
        }
    }
}
```

- Percorre os itens comparando o ID.
- **Por que não `cb.setSelectedItem(new Equipe())`?** Porque `Equipe` não sobrescreve `equals`; o combo compara por identidade e não encontraria o objeto existente.
- Se o ID não existir (registro excluído), o combo permanece no item atual; a tela pode mostrar dados inconsistentes. É um caso raro, mas possível após exclusões.

### 6.16 `mostrarErro` — a tradução das mensagens

[`mostrarErro`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L2017-L2050)

```java
private void mostrarErro(Exception ex) {
    String msg = ex.getMessage();
    if (ex.getCause() != null && ex.getCause().getMessage() != null) {
        msg = ex.getCause().getMessage();
    }
    if (msg == null || msg.trim().isEmpty()) {
        msg = ex.toString();
    }

    String detalhe = msg;
    if (msg.contains("uq_jogador_camisa")) {
        msg = "Já existe um jogador com esse número de camisa nessa equipe. Escolha outro número.";
    } else if (msg.contains("chk_jogador_camisa")) {
        msg = "O número da camisa precisa estar entre 1 e 99.";
    } else if (msg.contains("Duplicate entry")) {
        msg = "Já existe um cadastro com esse valor. Confira o nome, o registro ou a combinação de campos únicos.";
    } else if (msg.contains("foreign key constraint fails")) {
        msg = "Não é possível excluir ou alterar este registro porque ele está sendo usado por outro cadastro (por exemplo, uma partida).";
    } else if (msg.contains("Communications link failure")
            || msg.contains("Connection refused")
            || msg.contains("Unable to load authentication plugin")) {
        msg = "Não foi possível conectar ao MySQL. Confira se o servidor está ligado e se o banco 'campeonato' foi criado.";
    }

    if (!msg.equals(detalhe)) {
        msg = msg + "\n\nDetalhe do banco: " + detalhe;
    }
    JOptionPane.showMessageDialog(this,
            "Erro / Regra do Banco:\n" + msg,
            "Atenção", JOptionPane.WARNING_MESSAGE);
}
```

- **`ex.getCause()`**: o `DAO` pode envolver a `SQLException` em `RuntimeException`; a mensagem real fica na causa.
- **Fallback**: se não houver mensagem, usa `ex.toString()`.
- **Traduções:**
  - `uq_jogador_camisa` → mensagem amigável de camisa repetida;
  - `chk_jogador_camisa` → 1..99;
  - `Duplicate entry` → cadastro duplicado genérico;
  - `foreign key constraint fails` → registro em uso;
  - falhas de conexão → MySQL desligado/banco não criado.
- **Se traduziu**, acrescenta `"Detalhe do banco: ..."` para o professor/usuário ver a mensagem original.
- **Mensagens dos triggers** (em português, sem esses trechos) passam direto: “Data da partida fora do periodo do campeonato”, “Equipe nao inscrita neste campeonato”, etc.
- O diálogo é `WARNING_MESSAGE` com título “Atenção”.

### 6.17 `initComponents` e `main`

[`initComponents`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L2052-L2058)

```java
private void initComponents() {
    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    pack();
}
```

- Código gerado pelo NetBeans, mas mínimo: fecha o programa ao fechar a janela, define `BorderLayout` e `pack`.
- A interface real é montada em `configurarJanelaCompleta`.

[`main`](../Java_Champs/src/br/com/visao/TelaPrincipal.java#L2060-L2072): já explicado na seção 1.3.

### 6.18 Modelo de eventos e concorrência

- Todo o código roda na **EDT** (Event Dispatch Thread): o `main` usa `EventQueue.invokeLater`, e os listeners são chamados pela EDT.
- **Não há threads de fundo** (`SwingWorker`, `Thread`, `Executor`). As consultas JDBC rodam na EDT.
- Consequência: se o banco demorar, a janela “congela” até a consulta terminar. Para o volume do trabalho, é aceitável; para um sistema maior, seria necessário mover o acesso ao banco para uma thread de fundo e atualizar a tela com `SwingUtilities.invokeLater`.
- **Não há concorrência entre DAOs**: como tudo roda na EDT, um comando termina antes do próximo começar. Isso evita problemas de acesso simultâneo aos campos `con`/`pst`/`rs`.

---

## 7. Relatórios: do botão ao PDF

### 7.1 O caminho completo

```text
Usuário escolhe campeonato/período
        ↓
TelaPrincipal.gerarRelatorio...
        ↓
ManterRelatorio.classificacaoPorCampeonato / partidasPorPeriodo
        ↓
con.prepareCall("{call ...}")
        ↓
MySQL executa sp_relatorio_...
        ↓
ResultSet
        ↓
ArrayList<RelatorioClassificacao> / ArrayList<RelatorioPartida>
        ↓
DefaultTableModel.addRow(...)
        ↓
JTable
        ↓
JTable.print(...)  →  janela de impressão  →  PDF
```

### 7.2 Mapeamento das colunas

**Classificação:**

| Coluna da JTable | Getter | Origem |
|---|---|---|
| Pos. | `getPosicaoFormatada()` | `posicao` (`NULL` → `-`) |
| Equipe | `getNomeEquipe()` | `EQUIPE.nome` |
| Cidade | `getCidade()` | `EQUIPE.cidade` |
| PTS | `getPontos()` | `CLASSIFICACAO.pontuacao` |
| J | `getJogos()` | `partidas_jogadas` |
| V | `getVitorias()` | `vitorias` |
| E | `getEmpates()` | `empates` |
| D | `getDerrotas()` | `derrotas` |
| GP | `getGolsPro()` | `gols_marcados` |
| GC | `getGolsContra()` | `gols_sofridos` |
| SG | `getSaldoGols()` | `saldo_gols` |
| Aprov. | `getAproveitamentoFormatado()` | `aproveitamento` formatado |
| Casa | `getJogosCasa()` | subconsulta |
| Fora | `getJogosFora()` | subconsulta |
| Estádios | `getEstadios()` | `COUNT(DISTINCT)` |
| Árbitros | `getArbitros()` | `COUNT(DISTINCT)` |
| Status | `getStatusAtual()` | `status_atual` |

**Partidas:**

| Coluna | Getter | Origem |
|---|---|---|
| Rodada | `getRodada()` | `PARTIDA.rodada` |
| Data/Hora | `getDataHora()` | `DATE_FORMAT` |
| Mandante | `getMandante()` | `EQUIPE m.nome` |
| Visitante | `getVisitante()` | `EQUIPE v.nome` |
| Estádio | `getEstadio()` | `ESTADIO.nome` |
| Árbitro | `getArbitro()` | `ARBITRO.nome` |
| Placar | `getPlacar()` | `CONCAT/COALESCE` |
| Status | `getStatus()` | `PARTIDA.status` |

### 7.3 O resumo do rodapé

- **Classificação:** `Equipes`, `Partidas encerradas`, `Gols`, `Média de gols por partida`.
- **Partidas:** `Total`, `Encerradas`, `Agendadas`, `Canceladas`.
- O resumo é **calculado no Java** a partir da lista devolvida; não vem do banco.
- A média usa `gols / partidas`; se não houver partida, `"0,00"`.

### 7.4 Impressão e PDF

- `JTable.print` usa o serviço de impressão do Java.
- A janela de impressão do sistema operacional oferece a opção **“Salvar como PDF”** (no Windows, “Microsoft Print to PDF” ou similar).
- O PDF gerado é uma impressão da tabela; não é um PDF “gerado” por biblioteca.
- Se o sistema não tiver impressora/PDF, a janela ainda pode oferecer “Salvar como PDF” dependendo do driver instalado.
- **Cabeçalho:** “Champs - Classificação” ou “Champs - Partidas por período”.
- **Rodapé:** “Página 1”, “Página 2”…

---

## 8. Fluxos de ponta a ponta (resumo técnico)

### 8.1 Cadastrar equipe

```text
btnSalvar (lambda)
  → new Equipe()
  → daoEquipe.inserir(eq)
      → abrirBanco()
      → INSERT INTO EQUIPE (nome, cidade) VALUES (?, ?)
      → pst.execute()
      → finally fecharBanco()
  → JOptionPane “Equipe cadastrada!”
  → limparCamposEquipe()
  → recarregarTudo()
      → daoEquipe.pesquisarTudo()
      → tblEquipe + cbEquipe...
```

### 8.2 Consultar equipe

```text
btnConsultar
  → daoEquipe.pesquisarRegistro(termo)
      → SELECT ... WHERE id=? OR nome LIKE ? OR cidade LIKE ?
      → ORDER BY (id=?) DESC, nome LIMIT 1
      → montarEquipe(rs)
  → preencher campos
```

### 8.3 Agendar partida

```text
btnAgendar
  → montarObjetoPartida()
  → daoPartida.inserir(p)
      → INSERT INTO PARTIDA (...)
      → BEFORE INSERT → sp_validar_partida (4 regras)
      → CHECKs/FKs/UNIQUE
      → AFTER INSERT → sp_sincronizar_classificacao
  → mensagem
  → recarregarTudo()
      → classificação recarregada
```

### 8.4 Lançar placar

```text
selecionar partida / consultar
  → preencherCamposPartida
  → mudar status para ENCERRADA (listener preenche gols 0)
  → informar gols
  → btnAtualizar
      → montarObjetoPartida + ID
      → daoPartida.alterar(p)
          → UPDATE PARTIDA ...
          → BEFORE UPDATE → sp_validar_partida
          → AFTER UPDATE → sincroniza OLD (e NEW se mudou)
  → recarregarTudo()
```

### 8.5 Excluir partida

```text
btnExcluir
  → confirma
  → daoPartida.deletar(p)
      → DELETE FROM PARTIDA WHERE id_partida=?
      → AFTER DELETE → sincroniza OLD.id_campeonato
  → limpar
  → recarregarTudo()
```

### 8.6 Inscrever equipe

```text
btnInscrever
  → daoClassificacao.inscreverEquipe(idCamp, idEquipe)
      → CALL sp_inscrever_equipe
          → INSERT IGNORE CLASSIFICACAO
          → sp_sincronizar_classificacao
  → carregarTabelaClassificacao()
```

### 8.7 Gerar relatório

```text
btnRelClassificacao
  → daoRelatorio.classificacaoPorCampeonato(idCamp)
      → prepareCall("{call sp_relatorio_classificacao(?)}")
      → executeQuery()
      → monta ArrayList<RelatorioClassificacao>
  → limpa tblRelClassificacao
  → addRow(...)
  → resumo
  → validarRelatorio.run()
  → habilita impressão
```

---

## 9. Matriz de validação: tela e banco

### 9.1 Campos e regras

| Aba | Campo | Regra no Java | Regra no banco |
|---|---|---|---|
| 1 | nome | não vazio | `NOT NULL`, `UNIQUE` |
| 1 | data_inicio | `dataValida` | `DATE NOT NULL` |
| 1 | data_fim | `dataValida` e ≥ início | `DATE NOT NULL`, `chk_campeonato_datas` |
| 1 | status | combo | `ENUM` |
| 2 | nome | não vazio | `NOT NULL`, `UNIQUE` |
| 2 | cidade | não vazio | `NOT NULL` |
| 3 | nome | não vazio | `NOT NULL` |
| 3 | posição | combo | `VARCHAR NOT NULL` (sem `ENUM`) |
| 3 | camisa | 1..99 | `chk_jogador_camisa`, `uq_jogador_camisa` |
| 3 | nascimento | vazio ou data válida/não futura | `DATE NULL` |
| 3 | equipe | combo não nulo | FK `id_equipe` |
| 4 | nome/cidade | não vazios | `NOT NULL` |
| 4 | capacidade | inteiro > 0 | `chk_estadio_capacidade` |
| 5 | nome | não vazio | `NOT NULL` |
| 5 | registro | não vazio | `NOT NULL`, `UNIQUE` |
| 5 | categoria | combo | `ENUM` |
| 6 | campeonato/equipe | combos | FKs |
| 6 | status | combo | `ENUM` |
| 7 | combos de FK | não nulos | FKs |
| 7 | rodada | inteiro > 0 | `chk_partida_rodada` |
| 7 | data/hora | `dataHoraValida` | `DATETIME NOT NULL` |
| 7 | gols | vazios ou ≥ 0; ENCERRADA exige os dois | `chk_partida_gols`, `chk_partida_placar_completo`, `chk_partida_placar_por_status` |
| 7 | acréscimo | inteiro ≥ 0 | `chk_partida_acrescimo` |
| 7 | mandante ≠ visitante | compara IDs | `chk_partida_times` |
| 7 | tempo | combo; só habilitado em andamento | `chk_partida_tempo` |
| 8 | campeonato | combo | — |
| 8 | datas | válidas e fim ≥ início | — |

### 9.2 Tabela-verdade do placar na partida

| Status | Gols vazios | Um gol | Dois gols | Resultado |
|---|---|---|---|---|
| `AGENDADA` | válido | inválido | inválido | o listener limpa os gols |
| `EM_ANDAMENTO` | válido | inválido | válido | tempo habilitado |
| `ENCERRADA` | inválido | inválido | válido | listener preenche vazios com 0 |
| `CANCELADA` | válido | inválido | inválido | o listener limpa os gols |

- “Um gol” é sempre inválido por `chk_partida_placar_completo` (e pela validação da tela).
- `EM_ANDAMENTO` aceita os dois gols `NULL` ou os dois preenchidos; não aceita um só.
- O listener do status ajuda o usuário, mas o banco é a autoridade.

---

## 10. Catálogo de mensagens e erros

### 10.1 Mensagens dos triggers

| Mensagem | Quando |
|---|---|
| `Data da partida fora do periodo do campeonato` | data fora de `CAMPEONATO.data_inicio`/`data_fim` |
| `Equipe nao inscrita neste campeonato` | mandante ou visitante sem linha em `CLASSIFICACAO` |
| `Equipe ja possui partida nesta rodada` | uma das equipes já joga na mesma rodada |
| `Arbitro ou estadio ja ocupado neste horario` | mesmo árbitro ou estádio no mesmo `DATETIME` |
| `Equipe possui partidas neste campeonato` | tentativa de desinscrever equipe com partidas |

### 10.2 Erros de constraint

| Erro | Constraint provável | Mensagem ao usuário |
|---|---|---|
| `Duplicate entry` | `uq_*` | “Já existe um cadastro com esse valor…” |
| `chk_jogador_camisa` | `chk_jogador_camisa` | “O número da camisa precisa estar entre 1 e 99.” |
| `uq_jogador_camisa` | `uq_jogador_camisa` | “Já existe um jogador com esse número de camisa nessa equipe…” |
| `foreign key constraint fails` | `fk_*` | “Não é possível excluir ou alterar…” |
| `Communications link failure` | conexão | “Não foi possível conectar ao MySQL…” |

### 10.3 Como simular para a apresentação

- **Data fora do período:** aba 7, data `2027-01-10` em um campeonato que termina em `2026-12-30`.
- **Equipe não inscrita:** cadastre uma equipe e não a inscreva; tente usá-la em uma partida.
- **Equipe duas vezes na rodada:** agende um jogo para uma equipe na rodada 1 e tente outro na mesma rodada.
- **Árbitro ocupado:** repita o mesmo árbitro no mesmo horário.
- **Desinscrição bloqueada:** remova a inscrição de uma equipe que já jogou.
- **Camisa repetida:** cadastre dois jogadores com a mesma camisa na mesma equipe.
- **Exclusão bloqueada:** tente excluir uma equipe com jogadores/partidas.

---

## 11. Limitações, decisões e “pegadinhas”

### 11.1 Decisões de projeto que valem explicação

| Decisão | Por quê | Trade-off |
|---|---|---|
| Datas como `String` no modelo | simplicidade com a tela e o `setString` | sem aritmética de datas no Java; validação manual |
| Regras no banco | valem para qualquer cliente | mensagens mais técnicas; menos lógica no Java |
| `TelaPrincipal` como controller | menos classes, direto ao ponto | tela grande (2.073 linhas), difícil de testar |
| Modelo anêmico | classes simples de dados | regras espalhadas entre tela e banco |
| `Integer` para gols/colocação | representa `NULL` | exige `wasNull` e cuidado nos getters |
| `recarregarTudo` após cada escrita | consistência simples | muitas consultas e perda de seleção |
| Sem `equals` nas models | não é necessário para o fluxo | não se pode usar `setSelectedItem(new ...)` |
| Sem framework MVC | projeto acadêmico | mais código manual no Swing |

### 11.2 Pegadinhas que o professor pode usar

1. **“O Java calcula a classificação?”**
   Não. O Java só exibe; o cálculo é do trigger/procedure no MySQL.

2. **“Onde está o Controller?”**
   Não há classe separada: o evento do botão na `TelaPrincipal` faz esse papel. Esse é o MVC didático do professor (`controle` = Model, `entidade` = DAO, `visao` = View); o MVC clássico de 1990 teria um objeto Controller dedicado.

3. **“Por que a conexão não é fechada?”**
   `fecharBanco()` fecha só o `PreparedStatement`; é uma limitação. `ManterRelatorio` é a exceção e fecha a conexão.

4. **“Por que `com.mysql.jdbc.Driver` e não `com.mysql.cj.jdbc.Driver`?”**
   É o nome antigo; o Connector/J 8+ recomenda o novo. Se a classe antiga não existir, o `Class.forName` falha e a conexão fica nula.

5. **“Por que a linha selecionada da JTable corresponde ao cache?”**
   Porque `setAutoCreateRowSorter(false)` mantém a ordem do `SELECT` igual à ordem do `ArrayList`. Se o sorter estivesse ligado, o índice da linha não seria o índice do cache.

6. **“Por que `wasNull()` é necessário?”**
   Porque `getInt` devolve `0` para `NULL`; sem `wasNull`, um jogo sem placar pareceria `0 x 0`.

7. **“Por que a data da partida é `String`?”**
   Para a tela e o DAO trabalharem com texto; o MySQL converte para `DATETIME`.

8. **“Por que o botão Cadastrar fica desabilitado?”**
   Porque o `DocumentListener` reavalia o `validar` a cada tecla e só habilita quando os campos obrigatórios estão válidos.

9. **“O que acontece se eu clicar em Cadastrar com o botão desabilitado?”**
   Em Swing, um botão desabilitado não dispara `ActionListener`; o `if (!btnSalvar.isEnabled())` dentro da lambda é uma proteção extra (redundante no uso normal).

10. **“Por que o `INSERT IGNORE` da inscrição não avisa que já estava inscrita?”**
    Porque ele ignora a duplicata; a tela mostra “Equipe inscrita” mesmo quando nada mudou. É uma mensagem otimista.

11. **“O que acontece se a partida mudar de campeonato?”**
    O `AFTER UPDATE` sincroniza o campeonato antigo e o novo.

12. **“Uma partida cancelada ocupa rodada/árbitro/estádio?”**
    Não, a validação ignora `CANCELADA`; mas FKs, CHECKs e o `UNIQUE` de confronto continuam valendo.

13. **“Uma partida cancelada bloqueia desinscrição?”**
    Sim; o trigger de desinscrição não filtra status.

14. **“Por que a posição do jogador não é `ENUM`?”**
    O schema usa `VARCHAR`; só o combo do Java limita. É uma escolha simplificada.

15. **“Por que o status da classificação é manual?”**
    Porque `CLASSIFICADO`/`ELIMINADO` depende do regulamento do campeonato, não só dos números; o organizador decide.

16. **“Por que o relatório usa procedure e não um SELECT no Java?”**
    Porque recebe parâmetros e centraliza o JOIN no banco; o Java só chama e lê.

17. **“Por que `JTable.print` e não uma biblioteca de PDF?”**
    Porque o Swing já oferece impressão e a janela do sistema permite salvar em PDF; evita dependência extra.

18. **“O que acontece se o banco estiver desligado ao abrir o programa?”**
    `recarregarTudo` captura a exceção e só imprime no console; a janela abre vazia. Ao tentar uma ação, `mostrarErro` mostra a mensagem de conexão.

19. **“Por que a tela não usa `SwingWorker`?”**
    Porque as consultas são rápidas no volume do trabalho; mas a EDT é bloqueada durante o JDBC. É uma melhoria possível.

20. **“Por que `TelaPrincipal.form` não tem os componentes?”**
    Porque a tela é feita em código; o `.form` é um stub mínimo do NetBeans.

21. **“O que acontece se eu tentar excluir uma equipe com inscrição, mas sem partidas?”**
    A FK `fk_classificacao_equipe` (RESTRICT) bloqueia; é preciso remover a inscrição antes.

22. **“O que acontece se eu tentar excluir um campeonato com partidas?”**
    A FK `fk_partida_campeonato` (RESTRICT) bloqueia; sem partidas, o `CASCADE` apaga as inscrições.

23. **“Por que a tabela de classificação mostra `-` na posição?”**
    Porque `colocacao_equipe` é `NULL` para quem ainda não jogou; a tela formata como `-`.

24. **“Por que o relatório de classificação soma jogos e divide por 2?”**
    Porque cada partida encerrada aparece na linha das duas equipes; a soma dos jogos é o dobro do número de partidas.

25. **“O que é o `validarRelatorio`?”**
    É o `Runnable` de validação guardado para ser reexecutado depois de gerar um relatório, quando as linhas são adicionadas e o botão de impressão precisa ser habilitado.

### 11.3 Limitações que você pode citar como “próximos passos”

- Fechar `Connection`/`ResultSet` no `finally` ou usar try-with-resources.
- Trocar `com.mysql.jdbc.Driver` por `com.mysql.cj.jdbc.Driver`.
- Criar uma camada de serviço/controller para tirar a lógica da tela.
- Usar `BigDecimal`/`LocalDate` em vez de `String` para datas e valores.
- Criar testes JUnit para os DAOs e para as regras.
- Usar um pool de conexões (HikariCP) em vez de abrir conexão por operação.
- Padronizar `posicao` como `ENUM` ou tabela de domínio.
- Adicionar `equals`/`hashCode` nas models se forem usadas em coleções/comparções.
- Tratar o `ClassNotFoundException` relançando a exceção.
- Melhorar as mensagens de erro e a tradução de todos os CHECKs/FKs.
- Separar a `TelaPrincipal` em painéis/classes menores.
- Rodar os testes em CI.

---

## 12. Perguntas e respostas (Java/Swing/JDBC)

**1. O que é JDBC?**
É a API do Java para acessar bancos relacionais. O driver (MySQL Connector/J) traduz as chamadas para o protocolo do MySQL.

**2. Quais são os passos de uma consulta JDBC?**
Abrir conexão (`DriverManager.getConnection`), preparar o SQL (`prepareStatement`), preencher parâmetros, executar (`executeQuery`), ler o `ResultSet` e fechar os recursos.

**3. Qual a diferença entre `Statement` e `PreparedStatement`?**
`Statement` concatena o SQL com os valores; `PreparedStatement` usa `?` e envia os valores separadamente, evitando SQL Injection e problemas com aspas.

**4. O que é `ResultSet`?**
É o cursor que aponta para as linhas do resultado; `next()` avança e `getX()` lê as colunas.

**5. O que é `wasNull()`?**
Um método que diz se a última coluna lida era `NULL`; necessário porque `getInt` devolve `0` para `NULL`.

**6. O que é `CallableStatement`?**
Um `PreparedStatement` especial para chamar procedures, com a sintaxe `{call nome(?, ?)}`.

**7. O que faz `executeUpdate()`?**
Executa INSERT/UPDATE/DELETE e devolve o número de linhas afetadas.

**8. O que faz `executeQuery()`?**
Executa um SELECT (ou procedure que retorna linhas) e devolve um `ResultSet`.

**9. O que é `finally`?**
Um bloco que executa sempre, com ou sem exceção; no projeto, fecha o `PreparedStatement`.

**10. Por que usar `try/finally`?**
Para garantir o fechamento do statement mesmo se ocorrer erro no meio da operação.

**11. O que é uma `Connection`?**
A sessão aberta com o banco; por ela passam os statements.

**12. O que é autocommit?**
O modo padrão em que cada comando é confirmado automaticamente; o projeto não desliga.

**13. O que é o `DriverManager`?**
A classe que gerencia os drivers e abre conexões a partir da URL.

**14. O que é `Class.forName`?**
Carrega a classe do driver JDBC na memória; com JDBC 4+ é opcional.

**15. Por que o projeto tem uma classe `DAO`?**
Para concentrar a conexão e o fechamento, e permitir que as classes `Manter...` herdem esses recursos.

**16. O que é herança no projeto?**
As classes `Manter...` herdam de `DAO`; a `TelaPrincipal` herda de `JFrame`.

**17. O que é encapsulamento no projeto?**
Atributos `private` nas models, acessados por `get`/`set`.

**18. O que é polimorfismo no projeto?**
O `toString()` sobrescrito é um exemplo: o `JComboBox` chama `toString()` e cada classe devolve um texto diferente.

**19. O que é uma classe abstrata? Há no projeto?**
Não há classe abstrata; `DAO` é concreta e herdada.

**20. O que é uma interface? Há no projeto?**
As interfaces usadas são do Swing (`ActionListener`, `DocumentListener`, `ListSelectionListener`), implementadas por classes anônimas/lambdas.

**21. O que é uma classe anônima? Onde aparece?**
Uma classe sem nome, criada na hora. Aparece no `DocumentListener` de `vincularValidacao` e no `DefaultTableModel` de `modelo`.

**22. O que é uma lambda? Onde aparece?**
Uma função anônima concisa. Aparece nos `ActionListener` dos botões e nos `Runnable` de validação.

**23. O que é `JFrame`?**
A janela principal do Swing; `TelaPrincipal` herda dela.

**24. O que é `JTabbedPane`?**
O componente de abas; a tela tem 8 abas e a aba 8 tem 2 abas internas.

**25. O que é `JTable`?**
A tabela que exibe linhas e colunas; usa um `TableModel` (`DefaultTableModel`).

**26. O que é `DefaultTableModel`?**
O modelo padrão da `JTable`; permite `addRow`, `setRowCount(0)` etc.

**27. Por que a `JTable` não é editável?**
Porque `isCellEditable` retorna `false`; a edição é feita pelo formulário e pelo botão Alterar.

**28. O que é `JComboBox`?**
Uma lista suspensa; pode guardar `String` ou objetos (`Equipe`, `Campeonato` etc.).

**29. Como o `JComboBox` mostra o objeto?**
Chamando `toString()` do objeto; por isso as models sobrescrevem `toString()`.

**30. O que é `JTextField`?**
Um campo de texto de uma linha.

**31. O que é `DocumentListener`?**
Um listener que avisa quando o texto de um campo muda; tem `insertUpdate`, `removeUpdate` e `changedUpdate`.

**32. O que é `ActionListener`?**
Um listener que reage a uma ação (clique de botão, seleção de combo).

**33. O que é `ListSelectionListener`?**
Um listener que reage à seleção de linhas de uma tabela/lista.

**34. O que é `getValueIsAdjusting()`?**
Um teste para saber se a seleção ainda está sendo ajustada (arrasto); evita validar várias vezes.

**35. O que é `JOptionPane`?**
A classe de diálogos prontos: mensagem, confirmação, erro.

**36. O que é `JScrollPane`?**
O painel com barras de rolagem; envolve as tabelas.

**37. O que é `BorderLayout`, `GridLayout` e `FlowLayout`?**
Gerenciadores de layout: bordas (norte/sul/leste/oeste/centro), grade de células e fluxo horizontal.

**38. O que é a EDT?**
A Event Dispatch Thread, thread única do Swing que executa os eventos e a atualização da interface.

**39. Por que usar `EventQueue.invokeLater`?**
Para criar a tela na EDT, evitando problemas de concorrência do Swing.

**40. O que é `JTable.print`?**
O método que imprime a tabela; abre a janela de impressão do sistema, que permite salvar em PDF.

**41. O que é `MessageFormat`?**
Uma classe que formata textos com placeholders (`{0}`); usada no cabeçalho/rodapé da impressão.

**42. O que é `PrintMode.FIT_WIDTH`?**
Um modo de impressão que ajusta a tabela à largura da página.

**43. Onde está o MVC?**
`controle` = Model; `entidade` = DAO; `visao` = View; o controller está nos eventos da tela.

**44. Por que não há uma classe Controller?**
Porque o MVC do professor é o didático em três camadas (`controle` = Model, `entidade` = DAO, `visao` = View); o controller está no evento do botão. O MVC clássico de 1990 teria um Controller dedicado. A crítica é que a tela ficou grande, não que o padrão do curso esteja ausente.

**45. O que é um DAO?**
Data Access Object: classe que isola o SQL e monta os objetos.

**46. O que é uma model anêmica?**
Uma classe só com dados e get/set, sem regras; é o caso das classes de `controle`.

**47. Onde está a pesquisa individual?**
Nos métodos `pesquisarRegistro(...)` de cada `Manter...`, chamados pelo botão Consultar.

**48. Como a tela preenche os campos após a consulta?**
O DAO devolve o objeto; a tela copia os getters para os `JTextField`/`JComboBox`.

**49. Como a tela sabe qual linha da tabela foi selecionada?**
Pelo `getSelectedRow()`; em Jogador/Partida, o índice é usado no cache.

**50. Por que o cache existe?**
Para guardar os objetos completos (com os nomes do JOIN) na mesma ordem das linhas, evitando reconstruir a partir das células.

**51. O que acontece se a tabela for ordenada?**
O índice da linha deixaria de corresponder ao cache; por isso `setAutoCreateRowSorter(false)`.

**52. Como o erro do banco chega à tela?**
O MySQL devolve `SQLException`; o DAO propaga; a tela captura em `catch (Exception)` e chama `mostrarErro`.

**53. O que é `SQLException`?**
A exceção do JDBC para erros de banco (constraint, conexão, sintaxe etc.).

**54. O que é `RuntimeException`?**
Uma exceção não verificada; `DAO.abrirBanco` envolve a `SQLException` nela.

**55. Por que `throws Exception` nas classes `Manter...`?**
Para não tratar cada tipo de exceção; a tela centraliza o tratamento em `mostrarErro`.

**56. O que é uma `ArrayList`?**
Uma lista dinâmica; usada para devolver as linhas dos SELECTs.

**57. O que é `Integer` vs `int`?**
`int` é primitivo e não aceita `null`; `Integer` é objeto e aceita `null`, necessário para gols/colocação.

**58. O que é `String.format`?**
Formata texto com especificadores (`%.1f`, `%.2f`, `%%`).

**59. O que é `Locale.ROOT`?**
Um locale neutro; garante que o separador decimal seja ponto, independente da configuração do Windows.

**60. O que é `java.sql.Date.valueOf`?**
Converte uma string `YYYY-MM-DD` em `java.sql.Date`; lança `IllegalArgumentException` se o formato for inválido.

**61. O que é `Date.before`?**
Compara se uma data é anterior a outra; usado para validar o período.

**62. O que é `System.currentTimeMillis()`?**
O instante atual em milissegundos; usado para obter a data de hoje.

**63. O que é `trim()`?**
Remove espaços no início e no fim da string; usado antes de validar/gravar.

**64. O que é `isEmpty()`?**
Verifica se a string tem tamanho zero; combinado com `trim`, detecta campos só com espaços.

**65. O que é `StringBuilder`/concatenação?**
O projeto concatena strings com `+`; para muitos trechos, `StringBuilder` seria mais eficiente.

**66. O que é `JTable` sem sorter?**
Uma tabela que mantém a ordem do modelo; clicar no cabeçalho não reordena.

**67. O que é `setFillsViewportHeight(true)`?**
Faz a tabela preencher a altura do viewport, mesmo com poucas linhas.

**68. O que é `setPreferredWidth`?**
Define a largura preferida de uma coluna; usado na tabela de classificação.

**69. O que é `getColumnModel()`?**
O modelo que controla as colunas da `JTable`.

**70. O que é o `TelaPrincipal.form`?**
Um stub mínimo do NetBeans; a tela real é construída em código.

---

## 13. Checklist do Java

- [ ] Sei explicar o construtor da `TelaPrincipal` e a ordem `initComponents → configurarJanelaCompleta → recarregarTudo`.
- [ ] Sei explicar `vincularValidacao`, `aoMudarCombo` e `aoSelecionarTabela`.
- [ ] Sei explicar por que a `JTable` não tem sorter e por que Jogador/Partida usam cache.
- [ ] Sei seguir o clique de Cadastrar/Consultar/Alterar/Excluir em qualquer aba.
- [ ] Sei explicar a validação da partida e o listener de status.
- [ ] Sei explicar `recarregarTudo` e a ordem de carga.
- [ ] Sei explicar `PreparedStatement`, `ResultSet`, `wasNull` e `CallableStatement`.
- [ ] Sei explicar o ciclo de vida da conexão e a limitação do `fecharBanco`.
- [ ] Sei explicar as procedures dos relatórios e o `JTable.print`.
- [ ] Sei explicar o MVC do projeto e suas limitações.
- [ ] Sei responder às pegadinhas da seção 11.2.
