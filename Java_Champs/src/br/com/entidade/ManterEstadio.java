package br.com.entidade;

import br.com.controle.Estadio;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ManterEstadio extends DAO {

    public void inserir(Estadio es) throws Exception {
        try {
            abrirBanco();
            String query = "INSERT INTO ESTADIO (nome, cidade, capacidade) VALUES (?, ?, ?)";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, es.getNome());
            pst.setString(2, es.getCidade());
            pst.setInt(3, es.getCapacidade());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public void alterar(Estadio es) throws Exception {
        try {
            abrirBanco();
            String query = "UPDATE ESTADIO SET nome = ?, cidade = ?, capacidade = ? WHERE id_estadio = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, es.getNome());
            pst.setString(2, es.getCidade());
            pst.setInt(3, es.getCapacidade());
            pst.setInt(4, es.getIdEstadio());
            pst.executeUpdate();
        } finally {
            fecharBanco();
        }
    }

    public void deletar(Estadio es) throws Exception {
        try {
            abrirBanco();
            String query = "DELETE FROM ESTADIO WHERE id_estadio = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, es.getIdEstadio());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public Estadio pesquisarRegistro(String termo) throws Exception {
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
        Estadio estadio = null;

        try {
            abrirBanco();
            String query = "SELECT id_estadio, nome, cidade, capacidade FROM ESTADIO "
                    + "WHERE id_estadio = ? OR nome LIKE ? OR cidade LIKE ? "
                    + "ORDER BY (id_estadio = ?) DESC, nome "
                    + "LIMIT 1";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, codigo == null ? -1 : codigo);
            pst.setString(2, "%" + busca + "%");
            pst.setString(3, "%" + busca + "%");
            pst.setInt(4, codigo == null ? -1 : codigo);
            rs = pst.executeQuery();

            if (rs.next()) {
                estadio = montarEstadio(rs);
            }
        } finally {
            fecharBanco();
        }
        return estadio;
    }

    public ArrayList<Estadio> pesquisarTudo() throws Exception {
        ArrayList<Estadio> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT id_estadio, nome, cidade, capacidade FROM ESTADIO ORDER BY id_estadio";
            pst = (PreparedStatement) con.prepareStatement(query);
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(montarEstadio(rs));
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }

    private Estadio montarEstadio(ResultSet rs) throws Exception {
        Estadio es = new Estadio();
        es.setIdEstadio(rs.getInt("id_estadio"));
        es.setNome(rs.getString("nome"));
        es.setCidade(rs.getString("cidade"));
        es.setCapacidade(rs.getInt("capacidade"));
        return es;
    }
}
