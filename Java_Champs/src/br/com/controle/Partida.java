package br.com.controle;

public class Partida {
    private int idPartida;
    private int idCampeonato;
    private int rodada;
    private int idMandante;
    private int idVisitante;
    private int idEstadio;
    private int idArbitro;
    private String dataPartida; // Formato YYYY-MM-DD HH:MM:SS
    private Integer golsMandante;
    private Integer golsVisitante;
    private int acrescimo;
    private String status;       // AGENDADA, EM_ANDAMENTO, ENCERRADA, CANCELADA
    private String tempoPartida; // NULL, PRIMEIRO_TEMPO, SEGUNDO_TEMPO

    // Campos extras de exibição para os JOINs na tabela
    private String nomeCampeonato;
    private String nomeMandante;
    private String nomeVisitante;
    private String nomeEstadio;
    private String nomeArbitro;

    public int getIdPartida() {
        return idPartida;
    }

    public void setIdPartida(int idPartida) {
        this.idPartida = idPartida;
    }

    public int getIdCampeonato() {
        return idCampeonato;
    }

    public void setIdCampeonato(int idCampeonato) {
        this.idCampeonato = idCampeonato;
    }

    public int getRodada() {
        return rodada;
    }

    public void setRodada(int rodada) {
        this.rodada = rodada;
    }

    public int getIdMandante() {
        return idMandante;
    }

    public void setIdMandante(int idMandante) {
        this.idMandante = idMandante;
    }

    public int getIdVisitante() {
        return idVisitante;
    }

    public void setIdVisitante(int idVisitante) {
        this.idVisitante = idVisitante;
    }

    public int getIdEstadio() {
        return idEstadio;
    }

    public void setIdEstadio(int idEstadio) {
        this.idEstadio = idEstadio;
    }

    public int getIdArbitro() {
        return idArbitro;
    }

    public void setIdArbitro(int idArbitro) {
        this.idArbitro = idArbitro;
    }

    public String getDataPartida() {
        return dataPartida;
    }

    public void setDataPartida(String dataPartida) {
        this.dataPartida = dataPartida;
    }

    public Integer getGolsMandante() {
        return golsMandante;
    }

    public void setGolsMandante(Integer golsMandante) {
        this.golsMandante = golsMandante;
    }

    public Integer getGolsVisitante() {
        return golsVisitante;
    }

    public void setGolsVisitante(Integer golsVisitante) {
        this.golsVisitante = golsVisitante;
    }

    public int getAcrescimo() {
        return acrescimo;
    }

    public void setAcrescimo(int acrescimo) {
        this.acrescimo = acrescimo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTempoPartida() {
        return tempoPartida;
    }

    public void setTempoPartida(String tempoPartida) {
        this.tempoPartida = tempoPartida;
    }

    public String getNomeCampeonato() {
        return nomeCampeonato;
    }

    public void setNomeCampeonato(String nomeCampeonato) {
        this.nomeCampeonato = nomeCampeonato;
    }

    public String getNomeMandante() {
        return nomeMandante;
    }

    public void setNomeMandante(String nomeMandante) {
        this.nomeMandante = nomeMandante;
    }

    public String getNomeVisitante() {
        return nomeVisitante;
    }

    public void setNomeVisitante(String nomeVisitante) {
        this.nomeVisitante = nomeVisitante;
    }

    public String getNomeEstadio() {
        return nomeEstadio;
    }

    public void setNomeEstadio(String nomeEstadio) {
        this.nomeEstadio = nomeEstadio;
    }

    public String getNomeArbitro() {
        return nomeArbitro;
    }

    public void setNomeArbitro(String nomeArbitro) {
        this.nomeArbitro = nomeArbitro;
    }
}
