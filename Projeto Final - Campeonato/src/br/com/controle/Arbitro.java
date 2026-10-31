package br.com.controle;

public class Arbitro {
    private String nome;
    private String registro_federacao;
    private String categoria;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getRegistro_federacao() {
        return registro_federacao;
    }

    public void setRegistro_federacao(String registro_federacao) {
        this.registro_federacao = registro_federacao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}