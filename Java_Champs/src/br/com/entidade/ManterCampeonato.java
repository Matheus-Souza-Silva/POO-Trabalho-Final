package br.com.entidade;

import br.com.controle.Campeonato;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ManterCampeonato extends DAO {

    public void inserir(Campeonato c) throws Exception {
        try {
            abrirBanco();
            String query = "INSERT INTO CAMPEONATO (nome, data_inicio, data_fim, status) VALUES (?, ?, ?, ?)";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, c.getNome());
            pst.setString(2, c.getData_inicio());
            pst.setString(3, c.getData_fim());
            pst.setString(4, c.getStatus());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public void alterar(Campeonato c) throws Exception {
        try {
            abrirBanco();
            String query = "UPDATE CAMPEONATO SET nome = ?, data_inicio = ?, data_fim = ?, status = ? "
                    + "WHERE id_campeonato = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, c.getNome());
            pst.setString(2, c.getData_inicio());
            pst.setString(3, c.getData_fim());
            pst.setString(4, c.getStatus());
            pst.setInt(5, c.getIdCampeonato());
            pst.executeUpdate();
        } finally {
            fecharBanco();
        }
    }

    public void deletar(Campeonato c) throws Exception {
        try {
            abrirBanco();
            String query = "DELETE FROM CAMPEONATO WHERE id_campeonato = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, c.getIdCampeonato());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public Campeonato pesquisarRegistro(String termo) throws Exception {
        if (termo == null || termo.trim().isEmpty()) {
            return null;
        }

        String busca = termo.trim();
        Integer codigo = null;
        try {
            codigo = Integer.valueOf(busca);
        } catch (NumberFormatException ex) {
            // Texto nao numerico: a busca sera feita apenas por nome/registro.
        }
        Campeonato c = null;

        try {
            abrirBanco();
            String query = "SELECT id_campeonato, nome, data_inicio, data_fim, status "
                    + "FROM CAMPEONATO "
                    + "WHERE id_campeonato = ? OR nome LIKE ? "
                    + "ORDER BY (id_campeonato = ?) DESC, nome "
                    + "LIMIT 1";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, codigo == null ? -1 : codigo);
            pst.setString(2, "%" + busca + "%");
            pst.setInt(3, codigo == null ? -1 : codigo);
            rs = pst.executeQuery();

            if (rs.next()) {
                c = montarCampeonato(rs);
            }
        } finally {
            fecharBanco();
        }
        return c;
    }

    public ArrayList<Campeonato> pesquisarTudo() throws Exception {
        ArrayList<Campeonato> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT id_campeonato, nome, data_inicio, data_fim, status "
                    + "FROM CAMPEONATO ORDER BY id_campeonato";
            pst = (PreparedStatement) con.prepareStatement(query);
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(montarCampeonato(rs));
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }

    private Campeonato montarCampeonato(ResultSet rs) throws Exception {
        Campeonato c = new Campeonato();
        c.setIdCampeonato(rs.getInt("id_campeonato"));
        c.setNome(rs.getString("nome"));
        c.setData_inicio(rs.getString("data_inicio"));
        c.setData_fim(rs.getString("data_fim"));
        c.setStatus(rs.getString("status"));
        return c;
    }
}
