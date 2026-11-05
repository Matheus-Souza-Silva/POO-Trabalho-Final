package br.com.entidade;

import br.com.controle.RelatorioClassificacao;
import br.com.controle.RelatorioPartida;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * DAO dos relatorios.
 *
 * As consultas ficam em procedures no banco (Champs_relatorios.sql) porque
 * recebem parametros escolhidos na tela. O Java chama a procedure com
 * CallableStatement e le o resultado como se fosse um SELECT comum.
 */
public class ManterRelatorio extends DAO {

    /**
     * Relatorio principal: classificacao de um campeonato.
     * A procedure faz INNER JOIN com CAMPEONATO e EQUIPE e busca na PARTIDA
     * os jogos em casa/fora, estadios e arbitros de cada equipe.
     */
    public ArrayList<RelatorioClassificacao> classificacaoPorCampeonato(int idCampeonato) throws Exception {
        ArrayList<RelatorioClassificacao> lista = new ArrayList<>();
        CallableStatement cs = null;

        try {
            abrirBanco();
            // {call procedure(?)} e a sintaxe JDBC para chamar procedure.
            cs = con.prepareCall("{call sp_relatorio_classificacao(?)}");
            cs.setInt(1, idCampeonato);
            rs = cs.executeQuery();

            while (rs.next()) {
                RelatorioClassificacao linha = new RelatorioClassificacao();
                linha.setNomeCampeonato(rs.getString("campeonato"));

                // wasNull() e necessario: getInt devolve 0 tanto para zero
                // quanto para NULL; queremos distinguir "sem colocacao".
                int posicao = rs.getInt("posicao");
                linha.setPosicao(rs.wasNull() ? null : posicao);

                linha.setNomeEquipe(rs.getString("equipe"));
                linha.setCidade(rs.getString("cidade"));
                linha.setPontos(rs.getInt("pontos"));
                linha.setJogos(rs.getInt("jogos"));
                linha.setVitorias(rs.getInt("vitorias"));
                linha.setEmpates(rs.getInt("empates"));
                linha.setDerrotas(rs.getInt("derrotas"));
                linha.setGolsPro(rs.getInt("gols_pro"));
                linha.setGolsContra(rs.getInt("gols_contra"));
                linha.setSaldoGols(rs.getInt("saldo_gols"));
                linha.setAproveitamento(rs.getDouble("aproveitamento"));
                linha.setJogosCasa(rs.getInt("jogos_casa"));
                linha.setJogosFora(rs.getInt("jogos_fora"));
                linha.setEstadios(rs.getInt("estadios"));
                linha.setArbitros(rs.getInt("arbitros"));
                linha.setStatusAtual(rs.getString("status_atual"));
                lista.add(linha);
            }
        } finally {
            fecharRecursos(cs);
        }
        return lista;
    }

    /**
     * Relatorio de partidas de um campeonato dentro de um periodo.
     */
    public ArrayList<RelatorioPartida> partidasPorPeriodo(int idCampeonato,
            String dataInicio, String dataFim) throws Exception {
        ArrayList<RelatorioPartida> lista = new ArrayList<>();
        CallableStatement cs = null;

        try {
            abrirBanco();
            cs = con.prepareCall("{call sp_relatorio_partidas_periodo(?, ?, ?)}");
            cs.setInt(1, idCampeonato);
            // valueOf exige o texto no formato YYYY-MM-DD validado na tela.
            cs.setDate(2, java.sql.Date.valueOf(dataInicio));
            cs.setDate(3, java.sql.Date.valueOf(dataFim));
            rs = cs.executeQuery();

            while (rs.next()) {
                RelatorioPartida linha = new RelatorioPartida();
                linha.setRodada(rs.getInt("rodada"));
                linha.setDataHora(rs.getString("data_hora"));
                linha.setMandante(rs.getString("mandante"));
                linha.setVisitante(rs.getString("visitante"));
                linha.setEstadio(rs.getString("estadio"));
                linha.setArbitro(rs.getString("arbitro"));
                linha.setPlacar(rs.getString("placar"));
                linha.setStatus(rs.getString("status"));
                lista.add(linha);
            }
        } finally {
            fecharRecursos(cs);
        }
        return lista;
    }

    /**
     * Fecha o CallableStatement e a conexao. A classe DAO fecha o
     * PreparedStatement/ResultSet; aqui fechamos os recursos proprios da
     * procedure para nao deixar conexoes abertas.
     */
    private void fecharRecursos(CallableStatement cs) {
        // O DAO fecha o PreparedStatement; a procedure usa um
        // CallableStatement local e a conexao precisa ser fechada aqui.
        if (cs != null) {
            try {
                cs.close();
            } catch (SQLException ex) {
                System.out.println("Aviso ao fechar CallableStatement: " + ex.getMessage());
            }
        }
        try {
            fecharBanco();
        } catch (Exception ex) {
            System.out.println("Aviso ao fechar banco: " + ex.getMessage());
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                System.out.println("Aviso ao fechar conexao: " + ex.getMessage());
            }
            con = null;
        }
    }
}
