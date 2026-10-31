package br.com.entidade;


import br.com.controle.Arbitro;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import java.sql.ResultSet;

public class ManterArbitro extends DAO {
    public void inserir(Arbitro a) throws Exception {
        abrirBanco();
        String existe = "SELECT id_arbitro FROM ARBITRO WHERE registro_federacao = ?";
        pst = con.prepareStatement(existe);
        pst.setString(1, a.getRegistro_federacao());
        
        ResultSet tr = pst.executeQuery();
        if (tr.next()) {
            JOptionPane.showMessageDialog(null, "Registro de federação já cadastrado!");
        } else {
            String query = "INSERT INTO ARBITRO (nome,registro_federacao, categoria) VALUES(?,?,?)";
            pst = con.prepareStatement(query);
            pst.setString(1, a.getNome());
            pst.setString(2, a.getRegistro_federacao());
            pst.setString(3, a.getCategoria());
            pst.executeUpdate(); // pst.executeQuery() e pst.executeUpdate() são metodos diferentes
            
            JOptionPane.showMessageDialog(null, "Árbitro Inserido com sucesso!");
        }
        fecharBanco();
    }
    
    public void deletarAluno(Arbitro a) throws Exception {
        abrirBanco();
        String query = "DELETE FROM ARBITRO WHERE id_arbitro = ?";
        pst = con.prepareStatement(query);
        pst.setInt(1, a.getIdArbitro());
        pst.executeUpdate();
        JOptionPane.showMessageDialog(null, "Árbitro deletado com sucesso!");
        fecharBanco();
    }
    
    public void PesquisarRegistro(Arbitro a) throws Exception {
	try {
            abrirBanco();
            String query = "SELECT id_arbitro, nome, registro_federacao, categoria FROM ARBITRO WHERE id_arbitro = ?";
            pst = con.prepareStatement(query);
            pst.setInt(1, a.getIdArbitro());
            ResultSet tr = pst.executeQuery();
            if (tr.next()) {
                a.setIdArbitro(tr.getInt("id_arbitro"));
                a.setNome(tr.getString("nome"));
                a.setRegistro_federacao(tr.getString("registro_federacao"));
                a.setCategoria(tr.getString("categoria"));
            } else {
                JOptionPane.showMessageDialog(null, "Nenhum árbitro encontrado! ");
            }
            fecharBanco();
	} catch (Exception e) {
            System.out.println("Erro " + e.getMessage());
	}
    }
    
    public ArrayList<Arbitro> PesquisarTudo() throws Exception {
        
        ArrayList<Arbitro> arbitros = new ArrayList<Arbitro>();
        
        try {
            abrirBanco();
            String query = "SELECT * FROM ARBITRO";
            pst = con.prepareStatement(query);
            ResultSet tr = pst.executeQuery();
            Arbitro a;
            
            while (tr.next()) {
                a = new Arbitro();
                
                a.setIdArbitro(tr.getInt("id_arbitro"));
                a.setNome(tr.getString("nome"));
                a.setRegistro_federacao(tr.getString("registro_federacao"));
                a.setCategoria("categoria");
                arbitros.add(a);
            }
            fecharBanco();
            
        } catch (Exception e) {
            System.out.println("Erro " + e.getMessage());
        }
        return arbitros;
    }
    
    public void editarAluno(Arbitro a) throws Exception {
        abrirBanco();
        
        String query = "UPDATE arbitro  nome = ?, registro_federacao = ?, categoria = ? where id_arbitro = ?";
        
        pst = con.prepareStatement(query);
        
        pst.setString(1, a.getNome());
        pst.setString(2, a.getRegistro_federacao());
        pst.setString(3, a.getCategoria());
        pst.setInt(4, a.getIdArbitro());
        
        pst.executeUpdate();
        JOptionPane.showMessageDialog(null, "Arbitro Alterado com sucesso!");
        fecharBanco();
    }
}
