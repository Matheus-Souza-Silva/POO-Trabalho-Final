package br.com.entidade;

import br.com.controle.Jogador;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;

/**
 * DAO do jogador (CRUD extra ligado a EQUIPE).
 *
 * A tabela JOGADOR tem uma FK para EQUIPE e um UNIQUE (id_equipe,
 * numero_camisa): o mesmo numero de camisa nao pode repetir na mesma equipe.
 */
public class ManterJogador extends DAO {

    private static final String SELECT_JOGADOR
            = "SELECT j.id_jogador, j.nome, j.posicao, j.numero_camisa, "
            + "j.data_nascimento, j.id_equipe, e.nome AS nome_equipe "
            + "FROM JOGADOR j "
            + "INNER JOIN EQUIPE e ON e.id_equipe = j.id_equipe ";

    public void inserir(Jogador j) throws Exception {
        try {
            abrirBanco();
            String query = "INSERT INTO JOGADOR (nome, posicao, numero_camisa, "
                    + "data_nascimento, id_equipe) VALUES (?, ?, ?, ?, ?)";
            pst = (PreparedStatement) con.prepareStatement(query);
            preencherParametros(j);
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    public void alterar(Jogador j) throws Exception {
        try {
            abrirBanco();
            String query = "UPDATE JOGADOR SET nome = ?, posicao = ?, numero_camisa = ?, "
                    + "data_nascimento = ?, id_equipe = ? WHERE id_jogador = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            preencherParametros(j);
            pst.setInt(6, j.getIdJogador());
            pst.executeUpdate();
        } finally {
            fecharBanco();
        }
    }

    private void preencherParametros(Jogador j) throws Exception {
        pst.setString(1, j.getNome());
        pst.setString(2, j.getPosicao());
        pst.setInt(3, j.getNumeroCamisa());

        if (j.getDataNascimento() == null || j.getDataNascimento().trim().isEmpty()) {
            pst.setNull(4, Types.DATE);
        } else {
            pst.setDate(4, java.sql.Date.valueOf(j.getDataNascimento().trim()));
        }

        pst.setInt(5, j.getIdEquipe());
    }

    public void deletar(Jogador j) throws Exception {
        try {
            abrirBanco();
            String query = "DELETE FROM JOGADOR WHERE id_jogador = ?";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, j.getIdJogador());
            pst.execute();
        } finally {
            fecharBanco();
        }
    }

    /**
     * Pesquisa individual usada pelo botao Consultar do CRUD.
     * Aceita id, parte do nome, posicao, nome da equipe ou numero da camisa.
     */
    public Jogador pesquisarRegistro(String termo) throws Exception {
        if (termo == null || termo.trim().isEmpty()) {
            return null;
        }

        String busca = termo.trim();
        Integer codigo = null;
        try {
            codigo = Integer.valueOf(busca);
        } catch (NumberFormatException ex) {
            // Texto nao numerico: a busca sera feita pelos nomes/posicao.
        }

        Jogador jogador = null;
        try {
            abrirBanco();
            String query = SELECT_JOGADOR
                    + "WHERE j.id_jogador = ? OR j.nome LIKE ? OR j.posicao LIKE ? "
                    + "OR e.nome LIKE ? OR CAST(j.numero_camisa AS CHAR) = ? "
                    + "ORDER BY (j.id_jogador = ?) DESC, j.nome "
                    + "LIMIT 1";
            pst = (PreparedStatement) con.prepareStatement(query);
            pst.setInt(1, codigo == null ? -1 : codigo);
            pst.setString(2, "%" + busca + "%");
            pst.setString(3, "%" + busca + "%");
            pst.setString(4, "%" + busca + "%");
            pst.setString(5, busca);
            pst.setInt(6, codigo == null ? -1 : codigo);
            rs = pst.executeQuery();
            if (rs.next()) {
                jogador = montarJogador(rs);
            }
        } finally {
            fecharBanco();
        }
        return jogador;
    }

    public ArrayList<Jogador> pesquisarTudo() throws Exception {
        ArrayList<Jogador> lista = new ArrayList<>();
        try {
            abrirBanco();
            pst = (PreparedStatement) con.prepareStatement(SELECT_JOGADOR
                    + "ORDER BY j.id_jogador");
            rs = pst.executeQuery();
            while (rs.next()) {
                lista.add(montarJogador(rs));
            }
        } finally {
            fecharBanco();
        }
        return lista;
    }

    private Jogador montarJogador(ResultSet rs) throws Exception {
        Jogador j = new Jogador();
        j.setIdJogador(rs.getInt("id_jogador"));
        j.setNome(rs.getString("nome"));
        j.setPosicao(rs.getString("posicao"));
        j.setNumeroCamisa(rs.getInt("numero_camisa"));
        j.setDataNascimento(rs.getString("data_nascimento"));
        j.setIdEquipe(rs.getInt("id_equipe"));
        j.setNomeEquipe(rs.getString("nome_equipe"));
        return j;
    }
}
