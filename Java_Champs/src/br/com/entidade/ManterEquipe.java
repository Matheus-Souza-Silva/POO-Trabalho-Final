package br.com.entidade;

import br.com.controle.Equipe;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ManterEquipe extends DAO {

    public void inserir(Equipe e) throws Exception {
        try {
            abrirBanco();
            String query = "INSERT INTO EQUIPE (nome, cidade) VALUES (?, ?)";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, e.getNome());
            pst.setString(2, e.getCidade());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public void alterar(Equipe e) throws Exception {
        try {
            abrirBanco();
            String query = "UPDATE EQUIPE SET nome = ?, cidade = ? WHERE id_equipe = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, e.getNome());
            pst.setString(2, e.getCidade());
            pst.setInt(3, e.getIdEquipe());
            pst.executeUpdate();
        } finally {
            fecharBanco();
        }
    }

    public void deletar(Equipe e) throws Exception {
        try {
            abrirBanco();
            String query = "DELETE FROM EQUIPE WHERE id_equipe = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, e.getIdEquipe());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public Equipe pesquisarRegistro(String termo) throws Exception {
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
        Equipe equipe = null;

        try {
            abrirBanco();
            String query = "SELECT id_equipe, nome, cidade FROM EQUIPE "
                    + "WHERE id_equipe = ? OR nome LIKE ? OR cidade LIKE ? "
                    + "ORDER BY (id_equipe = ?) DESC, nome "
                    + "LIMIT 1";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, codigo == null ? -1 : codigo);
            pst.setString(2, "%" + busca + "%");
            pst.setString(3, "%" + busca + "%");
            pst.setInt(4, codigo == null ? -1 : codigo);
            rs = pst.executeQuery();

            if (rs.next()) {
                equipe = montarEquipe(rs);
            }
        } finally {
            fecharBanco();
        }
        return equipe;
    }

    public ArrayList<Equipe> pesquisarTudo() throws Exception {
        ArrayList<Equipe> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT id_equipe, nome, cidade FROM EQUIPE ORDER BY id_equipe";
            pst = (PreparedStatement) con.prepareStatement(query);
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(montarEquipe(rs));
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }

    private Equipe montarEquipe(ResultSet rs) throws Exception {
        Equipe e = new Equipe();
        e.setIdEquipe(rs.getInt("id_equipe"));
        e.setNome(rs.getString("nome"));
        e.setCidade(rs.getString("cidade"));
        return e;
    }
}
