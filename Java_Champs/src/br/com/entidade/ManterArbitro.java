package br.com.entidade;

import br.com.controle.Arbitro;
import java.sql.PreparedStatement;
import java.util.ArrayList;

public class ManterArbitro extends DAO {

    public void inserir(Arbitro a) throws Exception {
        try {
            abrirBanco();
            String query = "INSERT INTO ARBITRO (nome, registro_federacao, categoria) VALUES (?, ?, ?)";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, a.getNome());
            pst.setString(2, a.getRegistro_federacao());
            pst.setString(3, a.getCategoria());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public void alterar(Arbitro a) throws Exception {
        try {
            abrirBanco();
            String query = "UPDATE ARBITRO SET nome = ?, registro_federacao = ?, categoria = ? WHERE id_arbitro = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setString(1, a.getNome());
            pst.setString(2, a.getRegistro_federacao());
            pst.setString(3, a.getCategoria());
            pst.setInt(4, a.getIdArbitro());
            pst.executeUpdate();
        } finally {
            fecharBanco();
        }
    }

    public void deletar(Arbitro a) throws Exception {
        try {
            abrirBanco();
            String query = "DELETE FROM ARBITRO WHERE id_arbitro = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, a.getIdArbitro());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public ArrayList<Arbitro> pesquisarTudo() throws Exception {
        ArrayList<Arbitro> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT id_arbitro, nome, registro_federacao, categoria FROM ARBITRO ORDER BY nome";
            pst = (PreparedStatement) con.prepareStatement(query);
            rs = pst.executeQuery();
            while (rs.next()) {
                Arbitro a = new Arbitro();
                a.setIdArbitro(rs.getInt("id_arbitro"));
                a.setNome(rs.getString("nome"));
                a.setRegistro_federacao(rs.getString("registro_federacao"));
                a.setCategoria(rs.getString("categoria"));
                lista.add(a);
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }
        public Arbitro pesquisarRegistro(String termo) throws Exception {
        Arbitro a = null;
        try {
            abrirBanco();
            String query = "SELECT id_arbitro, nome, registro_federacao, categoria FROM ARBITRO "
                         + "WHERE id_arbitro = ? OR nome LIKE ? OR registro_federacao LIKE ? LIMIT 1";
            pst = (PreparedStatement) con.prepareStatement(query);
            int id = 0;
            try { id = Integer.parseInt(termo.trim()); } catch (NumberFormatException ex) {}
            pst.setInt(1, id);
            pst.setString(2, "%" + termo.trim() + "%");
            pst.setString(3, "%" + termo.trim() + "%");
            rs = pst.executeQuery();
            if (rs.next()) {
                a = new Arbitro();
                a.setIdArbitro(rs.getInt("id_arbitro"));
                a.setNome(rs.getString("nome"));
                a.setRegistro_federacao(rs.getString("registro_federacao"));
                a.setCategoria(rs.getString("categoria"));
            }
        } finally {
            fecharBanco();
        }
        return a;
    }

}
