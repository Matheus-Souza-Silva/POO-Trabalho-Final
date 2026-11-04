package br.com.entidade;

import br.com.controle.Partida;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;

public class ManterPartida extends DAO {

    public void inserir(Partida p) throws Exception {
        try {
            abrirBanco();
            String query = "INSERT INTO PARTIDA (id_campeonato, rodada, id_mandante, id_visitante, "
                    + "id_estadio, id_arbitro, data_partida, gols_mandante, gols_visitante, "
                    + "acrescimo, status, tempo_partida) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pst = (PreparedStatement) con.prepareStatement(query);
            preencherParametros(p);
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public void alterar(Partida p) throws Exception {
        try {
            abrirBanco();
            String query = "UPDATE PARTIDA SET id_campeonato = ?, rodada = ?, id_mandante = ?, "
                    + "id_visitante = ?, id_estadio = ?, id_arbitro = ?, data_partida = ?, "
                    + "gols_mandante = ?, gols_visitante = ?, acrescimo = ?, status = ?, tempo_partida = ? "
                    + "WHERE id_partida = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            preencherParametros(p);
            pst.setInt(13, p.getIdPartida());
            pst.executeUpdate();
        } finally {
            fecharBanco();
        }
    }

    private void preencherParametros(Partida p) throws Exception {
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
    }

    public void deletar(Partida p) throws Exception {
        try {
            abrirBanco();
            String query = "DELETE FROM PARTIDA WHERE id_partida = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, p.getIdPartida());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public ArrayList<Partida> pesquisarTudo() throws Exception {
        ArrayList<Partida> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT p.*, c.nome AS nome_camp, m.nome AS nome_mand, "
                    + "v.nome AS nome_vis, es.nome AS nome_est, a.nome AS nome_arb "
                    + "FROM PARTIDA p "
                    + "INNER JOIN CAMPEONATO c ON c.id_campeonato = p.id_campeonato "
                    + "INNER JOIN EQUIPE m ON m.id_equipe = p.id_mandante "
                    + "INNER JOIN EQUIPE v ON v.id_equipe = p.id_visitante "
                    + "INNER JOIN ESTADIO es ON es.id_estadio = p.id_estadio "
                    + "INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro "
                    + "ORDER BY p.id_campeonato, p.rodada, p.data_partida";
            pst = (PreparedStatement) con.prepareStatement(query);
            rs = pst.executeQuery();
            while (rs.next()) {
                Partida p = new Partida();
                p.setIdPartida(rs.getInt("id_partida"));
                p.setIdCampeonato(rs.getInt("id_campeonato"));
                p.setRodada(rs.getInt("rodada"));
                p.setIdMandante(rs.getInt("id_mandante"));
                p.setIdVisitante(rs.getInt("id_visitante"));
                p.setIdEstadio(rs.getInt("id_estadio"));
                p.setIdArbitro(rs.getInt("id_arbitro"));
                p.setDataPartida(rs.getString("data_partida"));

                int gm = rs.getInt("gols_mandante");
                p.setGolsMandante(rs.wasNull() ? null : gm);

                int gv = rs.getInt("gols_visitante");
                p.setGolsVisitante(rs.wasNull() ? null : gv);

                p.setAcrescimo(rs.getInt("acrescimo"));
                p.setStatus(rs.getString("status"));
                p.setTempoPartida(rs.getString("tempo_partida"));

                p.setNomeCampeonato(rs.getString("nome_camp"));
                p.setNomeMandante(rs.getString("nome_mand"));
                p.setNomeVisitante(rs.getString("nome_vis"));
                p.setNomeEstadio(rs.getString("nome_est"));
                p.setNomeArbitro(rs.getString("nome_arb"));
                lista.add(p);
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }
        public Partida pesquisarRegistro(int idPartida) throws Exception {
        Partida p = null;
        try {
            abrirBanco();
            String query = "SELECT p.*, c.nome AS nome_camp, m.nome AS nome_mand, "
                    + "v.nome AS nome_vis, es.nome AS nome_est, a.nome AS nome_arb "
                    + "FROM PARTIDA p "
                    + "INNER JOIN CAMPEONATO c ON c.id_campeonato = p.id_campeonato "
                    + "INNER JOIN EQUIPE m ON m.id_equipe = p.id_mandante "
                    + "INNER JOIN EQUIPE v ON v.id_equipe = p.id_visitante "
                    + "INNER JOIN ESTADIO es ON es.id_estadio = p.id_estadio "
                    + "INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro "
                    + "WHERE p.id_partida = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, idPartida);
            rs = pst.executeQuery();
            if (rs.next()) {
                p = new Partida();
                p.setIdPartida(rs.getInt("id_partida"));
                p.setIdCampeonato(rs.getInt("id_campeonato"));
                p.setRodada(rs.getInt("rodada"));
                p.setIdMandante(rs.getInt("id_mandante"));
                p.setIdVisitante(rs.getInt("id_visitante"));
                p.setIdEstadio(rs.getInt("id_estadio"));
                p.setIdArbitro(rs.getInt("id_arbitro"));
                p.setDataPartida(rs.getString("data_partida"));
                int gm = rs.getInt("gols_mandante");
                p.setGolsMandante(rs.wasNull() ? null : gm);
                int gv = rs.getInt("gols_visitante");
                p.setGolsVisitante(rs.wasNull() ? null : gv);
                p.setAcrescimo(rs.getInt("acrescimo"));
                p.setStatus(rs.getString("status"));
                p.setTempoPartida(rs.getString("tempo_partida"));
                p.setNomeCampeonato(rs.getString("nome_camp"));
                p.setNomeMandante(rs.getString("nome_mand"));
                p.setNomeVisitante(rs.getString("nome_vis"));
                p.setNomeEstadio(rs.getString("nome_est"));
                p.setNomeArbitro(rs.getString("nome_arb"));
            }
        } finally {
            fecharBanco();
        }
        return p;
    }
}
