package br.com.entidade;

import br.com.controle.Classificacao;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ManterClassificacao extends DAO {

    // Inscreve equipe chamando a procedure sp_inscrever_equipe do banco
    public void inscreverEquipe(int idCampeonato, int idEquipe) throws Exception {
        try {
            abrirBanco();
            String query = "CALL sp_inscrever_equipe(?, ?)";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, idCampeonato);
            pst.setInt(2, idEquipe);
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    // Remove inscrição (o trigger trg_classificacao_before_delete bloqueia se já houver partidas)
    public void removerInscricao(int idCampeonato, int idEquipe) throws Exception {
        try {
            abrirBanco();
            String query = "DELETE FROM CLASSIFICACAO WHERE id_campeonato = ? AND id_equipe = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, idCampeonato);
            pst.setInt(2, idEquipe);
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    // Atualiza o status_atual (A_JOGAR, CLASSIFICADO, ELIMINADO)
    public void atualizarStatus(int idCampeonato, int idEquipe, String statusAtual) throws Exception {
        try {
            abrirBanco();
            String query = "UPDATE CLASSIFICACAO SET status_atual = ? WHERE id_campeonato = ? AND id_equipe = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, statusAtual);
            pst.setInt(2, idCampeonato);
            pst.setInt(3, idEquipe);
            pst.executeUpdate();
        } finally {
            fecharBanco();
        }
    }

    public Classificacao pesquisarRegistro(int idCampeonato, int idEquipe) throws Exception {
        // Chave composta: a mesma equipe pode estar em varios campeonatos,
        // entao a busca precisa dos dois IDs.
        Classificacao cl = null;
        try {
            abrirBanco();
            String query = "SELECT c.id_campeonato, c.id_equipe, e.nome AS nome_equipe, "
                    + "c.colocacao_equipe, c.partidas_jogadas, c.vitorias, c.empates, c.derrotas, "
                    + "c.gols_marcados, c.gols_sofridos, c.saldo_gols, c.pontuacao, c.status_atual "
                    + "FROM CLASSIFICACAO c "
                    + "INNER JOIN EQUIPE e ON e.id_equipe = c.id_equipe "
                    + "WHERE c.id_campeonato = ? AND c.id_equipe = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, idCampeonato);
            pst.setInt(2, idEquipe);
            rs = pst.executeQuery();
            if (rs.next()) {
                cl = montarClassificacao(rs);
            }
        } finally {
            fecharBanco();
        }
        return cl;
    }

    // Lista a tabela de classificação com INNER JOIN em EQUIPE
    public ArrayList<Classificacao> listarPorCampeonato(int idCampeonato) throws Exception {
        ArrayList<Classificacao> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT c.id_campeonato, c.id_equipe, e.nome AS nome_equipe, "
                    + "c.colocacao_equipe, c.partidas_jogadas, c.vitorias, c.empates, c.derrotas, "
                    + "c.gols_marcados, c.gols_sofridos, c.saldo_gols, c.pontuacao, c.status_atual "
                    + "FROM CLASSIFICACAO c "
                    + "INNER JOIN EQUIPE e ON e.id_equipe = c.id_equipe "
                    + "WHERE c.id_campeonato = ? "
                    // Quem ainda nao jogou tem colocacao NULL; o primeiro
                    // criterio joga esses registros para o fim da tabela.
                    + "ORDER BY (c.colocacao_equipe IS NULL), c.colocacao_equipe ASC, e.nome ASC";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, idCampeonato);
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(montarClassificacao(rs));
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }

    private Classificacao montarClassificacao(ResultSet rs) throws Exception {
        Classificacao cl = new Classificacao();
        cl.setIdCampeonato(rs.getInt("id_campeonato"));
        cl.setIdEquipe(rs.getInt("id_equipe"));
        cl.setNomeEquipe(rs.getString("nome_equipe"));
        int coloc = rs.getInt("colocacao_equipe");
        cl.setColocacaoEquipe(rs.wasNull() ? null : coloc);
        cl.setPartidasJogadas(rs.getInt("partidas_jogadas"));
        cl.setVitorias(rs.getInt("vitorias"));
        cl.setEmpates(rs.getInt("empates"));
        cl.setDerrotas(rs.getInt("derrotas"));
        cl.setGolsMarcados(rs.getInt("gols_marcados"));
        cl.setGolsSofridos(rs.getInt("gols_sofridos"));
        cl.setSaldoGols(rs.getInt("saldo_gols"));
        cl.setPontuacao(rs.getInt("pontuacao"));
        cl.setStatusAtual(rs.getString("status_atual"));
        return cl;
    }
}
