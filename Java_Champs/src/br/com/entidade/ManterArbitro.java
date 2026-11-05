package br.com.entidade;

import br.com.controle.Arbitro;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
            String query = "UPDATE ARBITRO SET nome = ?, registro_federacao = ?, categoria = ? "
                    + "WHERE id_arbitro = ?";
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

    public Arbitro pesquisarRegistro(String termo) throws Exception {
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
        Arbitro arbitro = null;

        try {
            abrirBanco();
            // ID, parte do nome ou registro na federacao.
            String query = "SELECT id_arbitro, nome, registro_federacao, categoria FROM ARBITRO "
                    + "WHERE id_arbitro = ? OR nome LIKE ? OR registro_federacao LIKE ? "
                    + "ORDER BY (id_arbitro = ?) DESC, nome "
                    + "LIMIT 1";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, codigo == null ? -1 : codigo);
            pst.setString(2, "%" + busca + "%");
            pst.setString(3, "%" + busca + "%");
            pst.setInt(4, codigo == null ? -1 : codigo);
            rs = pst.executeQuery();

            if (rs.next()) {
                arbitro = montarArbitro(rs);
            }
        } finally {
            fecharBanco();
        }
        return arbitro;
    }

    public ArrayList<Arbitro> pesquisarTudo() throws Exception {
        ArrayList<Arbitro> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT id_arbitro, nome, registro_federacao, categoria "
                    + "FROM ARBITRO ORDER BY id_arbitro";
            pst = (PreparedStatement) con.prepareStatement(query);
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(montarArbitro(rs));
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }

    private Arbitro montarArbitro(ResultSet rs) throws Exception {
        Arbitro a = new Arbitro();
        a.setIdArbitro(rs.getInt("id_arbitro"));
        a.setNome(rs.getString("nome"));
        a.setRegistro_federacao(rs.getString("registro_federacao"));
        a.setCategoria(rs.getString("categoria"));
        return a;
    }
}
