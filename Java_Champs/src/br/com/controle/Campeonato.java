package br.com.controle;

public class Campeonato {
    private int idCampeonato;
    private String nome;
    private String data_inicio; // Formato YYYY-MM-DD
    private String data_fim;    // Formato YYYY-MM-DD
    private String status;      // PLANEJADO, EM_ANDAMENTO, ENCERRADO, CANCELADO

    public int getIdCampeonato() {
        return idCampeonato;
    }

    public void setIdCampeonato(int idCampeonato) {
        this.idCampeonato = idCampeonato;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getData_inicio() {
        return data_inicio;
    }

    public void setData_inicio(String data_inicio) {
        this.data_inicio = data_inicio;
    }

    public String getData_fim() {
        return data_fim;
    }

    public void setData_fim(String data_fim) {
        this.data_fim = data_fim;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return idCampeonato + " - " + nome + " [" + status + "]";
    }
}

