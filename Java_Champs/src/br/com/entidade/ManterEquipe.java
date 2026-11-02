package br.com.entidade;

import br.com.controle.Equipe;
import java.sql.PreparedStatement;
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

    public ArrayList<Equipe> pesquisarTudo() throws Exception {
        ArrayList<Equipe> lista = new ArrayList<>();
        try {
            abrirBanco();
            String query = "SELECT id_equipe, nome, cidade FROM EQUIPE ORDER BY nome";
            pst = (PreparedStatement) con.prepareStatement(query);
            rs = pst.executeQuery();
            while (rs.next()) {
                Equipe e = new Equipe();
                e.setIdEquipe(rs.getInt("id_equipe"));
                e.setNome(rs.getString("nome"));
                e.setCidade(rs.getString("cidade"));
                lista.add(e);
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }
}
