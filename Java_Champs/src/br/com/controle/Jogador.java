package br.com.controle;

/**
 * Jogador de uma equipe.
 *
 * A tabela JOGADOR guarda o nome, a posicao, o numero da camisa e a data de
 * nascimento (opcional). A equipe e uma chave estrangeira obrigatoria.
 *
 * O campo nomeEquipe nao existe na tabela: ele e preenchido pelo INNER JOIN
 * do DAO apenas para exibir o nome da equipe na JTable.
 */
public class Jogador {

    private int idJogador;
    private String nome;
    private String posicao;
    private int numeroCamisa;
    private String dataNascimento; // Formato YYYY-MM-DD (pode ser null)
    private int idEquipe;

    // Campo extra de exibicao (vem do JOIN com EQUIPE)
    private String nomeEquipe;

    public int getIdJogador() {
        return idJogador;
    }

    public void setIdJogador(int idJogador) {
        this.idJogador = idJogador;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPosicao() {
        return posicao;
    }

    public void setPosicao(String posicao) {
        this.posicao = posicao;
    }

    public int getNumeroCamisa() {
        return numeroCamisa;
    }

    public void setNumeroCamisa(int numeroCamisa) {
        this.numeroCamisa = numeroCamisa;
    }

    public String getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(String dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public int getIdEquipe() {
        return idEquipe;
    }

    public void setIdEquipe(int idEquipe) {
        this.idEquipe = idEquipe;
    }

    public String getNomeEquipe() {
        return nomeEquipe;
    }

    public void setNomeEquipe(String nomeEquipe) {
        this.nomeEquipe = nomeEquipe;
    }

    @Override
    public String toString() {
        String equipe = (nomeEquipe == null || nomeEquipe.trim().isEmpty())
                ? "" : " (" + nomeEquipe + ")";
        return numeroCamisa + " - " + nome + equipe;
    }
}
