package br.com.controle;

/**
 * Linha do relatorio principal (classificacao de um campeonato).
 *
 * A classe guarda os dados que vem da procedure sp_relatorio_classificacao:
 * dados da CLASSIFICACAO + nome da EQUIPE + nome do CAMPEONATO e alguns
 * numeros calculados a partir das PARTIDAS (casa/fora, estadios e arbitros).
 */
public class RelatorioClassificacao {

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

    public String getNomeCampeonato() {
        return nomeCampeonato;
    }

    public void setNomeCampeonato(String nomeCampeonato) {
        this.nomeCampeonato = nomeCampeonato;
    }

    public Integer getPosicao() {
        return posicao;
    }

    public void setPosicao(Integer posicao) {
        this.posicao = posicao;
    }

    public String getNomeEquipe() {
        return nomeEquipe;
    }

    public void setNomeEquipe(String nomeEquipe) {
        this.nomeEquipe = nomeEquipe;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public int getPontos() {
        return pontos;
    }

    public void setPontos(int pontos) {
        this.pontos = pontos;
    }

    public int getJogos() {
        return jogos;
    }

    public void setJogos(int jogos) {
        this.jogos = jogos;
    }

    public int getVitorias() {
        return vitorias;
    }

    public void setVitorias(int vitorias) {
        this.vitorias = vitorias;
    }

    public int getEmpates() {
        return empates;
    }

    public void setEmpates(int empates) {
        this.empates = empates;
    }

    public int getDerrotas() {
        return derrotas;
    }

    public void setDerrotas(int derrotas) {
        this.derrotas = derrotas;
    }

    public int getGolsPro() {
        return golsPro;
    }

    public void setGolsPro(int golsPro) {
        this.golsPro = golsPro;
    }

    public int getGolsContra() {
        return golsContra;
    }

    public void setGolsContra(int golsContra) {
        this.golsContra = golsContra;
    }

    public int getSaldoGols() {
        return saldoGols;
    }

    public void setSaldoGols(int saldoGols) {
        this.saldoGols = saldoGols;
    }

    public double getAproveitamento() {
        return aproveitamento;
    }

    public void setAproveitamento(double aproveitamento) {
        this.aproveitamento = aproveitamento;
    }

    public int getJogosCasa() {
        return jogosCasa;
    }

    public void setJogosCasa(int jogosCasa) {
        this.jogosCasa = jogosCasa;
    }

    public int getJogosFora() {
        return jogosFora;
    }

    public void setJogosFora(int jogosFora) {
        this.jogosFora = jogosFora;
    }

    public int getEstadios() {
        return estadios;
    }

    public void setEstadios(int estadios) {
        this.estadios = estadios;
    }

    public int getArbitros() {
        return arbitros;
    }

    public void setArbitros(int arbitros) {
        this.arbitros = arbitros;
    }

    public String getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(String statusAtual) {
        this.statusAtual = statusAtual;
    }

    public String getPosicaoFormatada() {
        return posicao == null ? "-" : posicao + "º";
    }

    public String getAproveitamentoFormatado() {
        return String.format(java.util.Locale.ROOT, "%.1f%%", aproveitamento);
    }
}
