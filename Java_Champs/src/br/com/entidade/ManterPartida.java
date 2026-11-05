package br.com.entidade;

import br.com.controle.Partida;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;

public class ManterPartida extends DAO {

    // Consulta base com INNER JOIN: troca os codigos pelos nomes das tabelas
    // relacionadas (campeonato, mandante, visitante, estadio e arbitro).
    private static final String SELECT_PARTIDA
            = "SELECT p.*, c.nome AS nome_camp, m.nome AS nome_mand, "
            + "v.nome AS nome_vis, es.nome AS nome_est, a.nome AS nome_arb "
            + "FROM PARTIDA p "
            + "INNER JOIN CAMPEONATO c ON c.id_campeonato = p.id_campeonato "
            + "INNER JOIN EQUIPE m ON m.id_equipe = p.id_mandante "
            + "INNER JOIN EQUIPE v ON v.id_equipe = p.id_visitante "
            + "INNER JOIN ESTADIO es ON es.id_estadio = p.id_estadio "
            + "INNER JOIN ARBITRO a ON a.id_arbitro = p.id_arbitro ";

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

    /**
     * Pesquisa individual por codigo, nome de equipe, campeonato, estadio
     * ou arbitro. O botao Consultar do CRUD principal usa este metodo.
     */
    public Partida pesquisarRegistro(String termo) throws Exception {
        if (termo == null || termo.trim().isEmpty()) {
            return null;
        }

        String busca = termo.trim();
        Integer codigo = null;
        try {
            codigo = Integer.valueOf(busca);
        } catch (NumberFormatException ex) {
            // Texto nao numerico: a busca sera feita pelos nomes.
        }

        Partida p = null;
        try {
            abrirBanco();
            String query = SELECT_PARTIDA
                    + "WHERE p.id_partida = ? OR m.nome LIKE ? OR v.nome LIKE ? "
                    + "OR c.nome LIKE ? OR es.nome LIKE ? OR a.nome LIKE ? "
                    + "ORDER BY (p.id_partida = ?) DESC, p.data_partida DESC, p.id_partida DESC "
                    + "LIMIT 1";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, codigo == null ? -1 : codigo);
            pst.setString(2, "%" + busca + "%");
            pst.setString(3, "%" + busca + "%");
            pst.setString(4, "%" + busca + "%");
            pst.setString(5, "%" + busca + "%");
            pst.setString(6, "%" + busca + "%");
            pst.setInt(7, codigo == null ? -1 : codigo);
            rs = pst.executeQuery();
            if (rs.next()) {
                p = montarPartida(rs);
            }
        } finally {
            fecharBanco();
        }
        return p;
    }

    public ArrayList<Partida> pesquisarTudo() throws Exception {
        ArrayList<Partida> lista = new ArrayList<>();
        try {
            abrirBanco();
            pst = (PreparedStatement) con.prepareStatement(SELECT_PARTIDA
                    + "ORDER BY p.id_partida");
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(montarPartida(rs));
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }

    private Partida montarPartida(ResultSet rs) throws Exception {
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
        return p;
    }
}
