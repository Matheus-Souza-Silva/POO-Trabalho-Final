package br.com.visao;

import br.com.controle.*;
import br.com.entidade.*;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class TelaPrincipal extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TelaPrincipal.class.getName());

    // DAOs (Camada de Entidade)
    private final ManterCampeonato daoCampeonato = new ManterCampeonato();
    private final ManterEquipe daoEquipe = new ManterEquipe();
    private final ManterEstadio daoEstadio = new ManterEstadio();
    private final ManterArbitro daoArbitro = new ManterArbitro();
    private final ManterClassificacao daoClassificacao = new ManterClassificacao();
    private final ManterPartida daoPartida = new ManterPartida();

    // Componentes Aba 1: Campeonato
    private JTextField txtIdCamp, txtNomeCamp, txtInicioCamp, txtFimCamp;
    private JComboBox<String> cbStatusCamp;
    private JTable tblCampeonato;

    // Componentes Aba 2: Equipe
    private JTextField txtIdEquipe, txtNomeEquipe, txtCidadeEquipe;
    private JTable tblEquipe;

    // Componentes Aba 3: Estádio
    private JTextField txtIdEstadio, txtNomeEstadio, txtCidadeEstadio, txtCapEstadio;
    private JTable tblEstadio;

    // Componentes Aba 4: Árbitro
    private JTextField txtIdArbitro, txtNomeArbitro, txtRegArbitro;
    private JComboBox<String> cbCatArbitro;
    private JTable tblArbitro;

    // Componentes Aba 5: Inscrição & Classificação
    private JComboBox<Campeonato> cbCampClass;
    private JComboBox<Equipe> cbEquipeInsc;
    private JComboBox<String> cbStatusClass;
    private JTable tblClassificacao;

    // Componentes Aba 6: Partidas (CRUD Principal)
    private JTextField txtIdPartida, txtRodadaPartida, txtDataPartida, txtGolsMand, txtGolsVis, txtAcrescimo;
    private JComboBox<Campeonato> cbCampPartida;
    private JComboBox<Equipe> cbMandante, cbVisitante;
    private JComboBox<Estadio> cbEstadioPartida;
    private JComboBox<Arbitro> cbArbitroPartida;
    private JComboBox<String> cbStatusPartida, cbTempoPartida;
    private JTable tblPartida;
    private ArrayList<Partida> cachePartidas = new ArrayList<>();

    public TelaPrincipal() {
        initComponents();
        configurarJanelaCompleta();
        recarregarTudo();
    }

    // Helper para travar/habilitar os botões enquanto os campos estiverem vazios
    private void vincularValidacao(Runnable validador, JTextField... campos) {
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                validador.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                validador.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                validador.run();
            }
        };
        for (JTextField c : campos) {
            c.getDocument().addDocumentListener(dl);
        }
        validador.run();
    }

    private void configurarJanelaCompleta() {
        setTitle("Champs - Sistema de Gestão de Campeonatos (Fases 4 e 5)");
        setSize(1140, 700);
        setLocationRelativeTo(null);
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("1. Campeonatos", criarPainelCampeonato());
        abas.addTab("2. Equipes", criarPainelEquipe());
        abas.addTab("3. Estádios", criarPainelEstadio());
        abas.addTab("4. Árbitros", criarPainelArbitro());
        abas.addTab("5. Inscrição & Classificação", criarPainelClassificacao());
        abas.addTab("6. Partidas (CRUD Principal)", criarPainelPartidas());

        abas.addChangeListener(e -> recarregarTudo());
        getContentPane().add(abas, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // =========================================================================
    // ABA 1: CAMPEONATO (CRUD SIMPLES)
    // =========================================================================
    private JPanel criarPainelCampeonato() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        txtIdCamp = new JTextField();
        txtIdCamp.setEditable(false);
        txtNomeCamp = new JTextField();
        txtInicioCamp = new JTextField("2026-09-01");
        txtFimCamp = new JTextField("2026-12-30");
        cbStatusCamp = new JComboBox<>(new String[]{"PLANEJADO", "EM_ANDAMENTO", "ENCERRADO", "CANCELADO"});

        form.add(new JLabel("ID:"));
        form.add(txtIdCamp);
        form.add(new JLabel("Nome do Campeonato:"));
        form.add(txtNomeCamp);
        form.add(new JLabel("Data Início (YYYY-MM-DD):"));
        form.add(txtInicioCamp);
        form.add(new JLabel("Data Fim (YYYY-MM-DD):"));
        form.add(txtFimCamp);
        form.add(new JLabel("Status:"));
        form.add(cbStatusCamp);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar (ID/Nome)");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        vincularValidacao(() -> {
            boolean camposOk = !txtNomeCamp.getText().trim().isEmpty()
                    && !txtInicioCamp.getText().trim().isEmpty()
                    && !txtFimCamp.getText().trim().isEmpty();
            boolean temId = !txtIdCamp.getText().trim().isEmpty();
            btnSalvar.setEnabled(camposOk);
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        }, txtIdCamp, txtNomeCamp, txtInicioCamp, txtFimCamp);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblCampeonato = new JTable(new DefaultTableModel(
                new Object[]{"ID", "Nome", "Início", "Fim", "Status"}, 0));
        tblCampeonato.getSelectionModel().addListSelectionListener(e -> {
            int row = tblCampeonato.getSelectedRow();
            if (row >= 0) {
                txtIdCamp.setText(tblCampeonato.getValueAt(row, 0).toString());
                txtNomeCamp.setText(tblCampeonato.getValueAt(row, 1).toString());
                txtInicioCamp.setText(tblCampeonato.getValueAt(row, 2).toString());
                txtFimCamp.setText(tblCampeonato.getValueAt(row, 3).toString());
                cbStatusCamp.setSelectedItem(tblCampeonato.getValueAt(row, 4).toString());
            }
        });

        btnSalvar.addActionListener(e -> {
            if (txtNomeCamp.getText().trim().isEmpty() || txtInicioCamp.getText().trim().isEmpty() || txtFimCamp.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha todos os campos antes de cadastrar!");
                return;
            }
            try {
                Campeonato c = new Campeonato();
                c.setNome(txtNomeCamp.getText().trim());
                c.setData_inicio(txtInicioCamp.getText().trim());
                c.setData_fim(txtFimCamp.getText().trim());
                c.setStatus(cbStatusCamp.getSelectedItem().toString());
                daoCampeonato.inserir(c);
                JOptionPane.showMessageDialog(this, "Campeonato cadastrado com sucesso!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = txtNomeCamp.getText().trim();
            if (termo.isEmpty()) {
                termo = JOptionPane.showInputDialog(this, "Digite o ID ou Nome do Campeonato para consultar:");
            }
            if (termo == null || termo.trim().isEmpty()) {
                return;
            }
            try {
                Campeonato c = daoCampeonato.pesquisarRegistro(termo);
                if (c != null) {
                    txtIdCamp.setText(String.valueOf(c.getIdCampeonato()));
                    txtNomeCamp.setText(c.getNome());
                    txtInicioCamp.setText(c.getData_inicio());
                    txtFimCamp.setText(c.getData_fim());
                    cbStatusCamp.setSelectedItem(c.getStatus());
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhum campeonato encontrado para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (txtIdCamp.getText().trim().isEmpty() || txtNomeCamp.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione um campeonato e preencha os campos!");
                return;
            }
            try {
                Campeonato c = new Campeonato();
                c.setIdCampeonato(Integer.parseInt(txtIdCamp.getText().trim()));
                c.setNome(txtNomeCamp.getText().trim());
                c.setData_inicio(txtInicioCamp.getText().trim());
                c.setData_fim(txtFimCamp.getText().trim());
                c.setStatus(cbStatusCamp.getSelectedItem().toString());
                daoCampeonato.alterar(c);
                JOptionPane.showMessageDialog(this, "Campeonato atualizado!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnDeletar.addActionListener(e -> {
            if (txtIdCamp.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione um campeonato para excluir!");
                return;
            }
            try {
                Campeonato c = new Campeonato();
                c.setIdCampeonato(Integer.parseInt(txtIdCamp.getText().trim()));
                daoCampeonato.deletar(c);
                JOptionPane.showMessageDialog(this, "Campeonato removido!");
                txtIdCamp.setText("");
                txtNomeCamp.setText("");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            txtIdCamp.setText("");
            txtNomeCamp.setText("");
            txtInicioCamp.setText("2026-09-01");
            txtFimCamp.setText("2026-12-30");
            cbStatusCamp.setSelectedIndex(0);
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblCampeonato), BorderLayout.CENTER);
        return painel;
    }

    // =========================================================================
    // ABA 2: EQUIPE (CRUD SIMPLES)
    // =========================================================================
    private JPanel criarPainelEquipe() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        txtIdEquipe = new JTextField();
        txtIdEquipe.setEditable(false);
        txtNomeEquipe = new JTextField();
        txtCidadeEquipe = new JTextField();

        form.add(new JLabel("ID:"));
        form.add(txtIdEquipe);
        form.add(new JLabel("Nome da Equipe:"));
        form.add(txtNomeEquipe);
        form.add(new JLabel("Cidade:"));
        form.add(txtCidadeEquipe);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar (ID/Nome)");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        vincularValidacao(() -> {
            boolean camposOk = !txtNomeEquipe.getText().trim().isEmpty()
                    && !txtCidadeEquipe.getText().trim().isEmpty();
            boolean temId = !txtIdEquipe.getText().trim().isEmpty();
            btnSalvar.setEnabled(camposOk);
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        }, txtIdEquipe, txtNomeEquipe, txtCidadeEquipe);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblEquipe = new JTable(new DefaultTableModel(new Object[]{"ID", "Nome", "Cidade"}, 0));
        tblEquipe.getSelectionModel().addListSelectionListener(e -> {
            int row = tblEquipe.getSelectedRow();
            if (row >= 0) {
                txtIdEquipe.setText(tblEquipe.getValueAt(row, 0).toString());
                txtNomeEquipe.setText(tblEquipe.getValueAt(row, 1).toString());
                txtCidadeEquipe.setText(tblEquipe.getValueAt(row, 2).toString());
            }
        });

        btnSalvar.addActionListener(e -> {
            if (txtNomeEquipe.getText().trim().isEmpty() || txtCidadeEquipe.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha todos os campos antes de cadastrar!");
                return;
            }
            try {
                Equipe eq = new Equipe();
                eq.setNome(txtNomeEquipe.getText().trim());
                eq.setCidade(txtCidadeEquipe.getText().trim());
                daoEquipe.inserir(eq);
                JOptionPane.showMessageDialog(this, "Equipe cadastrada!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = txtNomeEquipe.getText().trim();
            if (termo.isEmpty()) {
                termo = JOptionPane.showInputDialog(this, "Digite o ID ou Nome da Equipe para consultar:");
            }
            if (termo == null || termo.trim().isEmpty()) {
                return;
            }
            try {
                Equipe eq = daoEquipe.pesquisarRegistro(termo);
                if (eq != null) {
                    txtIdEquipe.setText(String.valueOf(eq.getIdEquipe()));
                    txtNomeEquipe.setText(eq.getNome());
                    txtCidadeEquipe.setText(eq.getCidade());
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhuma equipe encontrada para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (txtIdEquipe.getText().trim().isEmpty() || txtNomeEquipe.getText().trim().isEmpty() || txtCidadeEquipe.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione uma equipe e preencha os campos!");
                return;
            }
            try {
                Equipe eq = new Equipe();
                eq.setIdEquipe(Integer.parseInt(txtIdEquipe.getText().trim()));
                eq.setNome(txtNomeEquipe.getText().trim());
                eq.setCidade(txtCidadeEquipe.getText().trim());
                daoEquipe.alterar(eq);
                JOptionPane.showMessageDialog(this, "Equipe alterada!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnDeletar.addActionListener(e -> {
            if (txtIdEquipe.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione uma equipe para excluir!");
                return;
            }
            try {
                Equipe eq = new Equipe();
                eq.setIdEquipe(Integer.parseInt(txtIdEquipe.getText().trim()));
                daoEquipe.deletar(eq);
                JOptionPane.showMessageDialog(this, "Equipe excluída!");
                txtIdEquipe.setText("");
                txtNomeEquipe.setText("");
                txtCidadeEquipe.setText("");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            txtIdEquipe.setText("");
            txtNomeEquipe.setText("");
            txtCidadeEquipe.setText("");
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblEquipe), BorderLayout.CENTER);
        return painel;
    }

    // =========================================================================
    // ABA 3: ESTÁDIO (CRUD SIMPLES)
    // =========================================================================
    private JPanel criarPainelEstadio() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        txtIdEstadio = new JTextField();
        txtIdEstadio.setEditable(false);
        txtNomeEstadio = new JTextField();
        txtCidadeEstadio = new JTextField();
        txtCapEstadio = new JTextField();

        form.add(new JLabel("ID:"));
        form.add(txtIdEstadio);
        form.add(new JLabel("Nome do Estádio:"));
        form.add(txtNomeEstadio);
        form.add(new JLabel("Cidade:"));
        form.add(txtCidadeEstadio);
        form.add(new JLabel("Capacidade (> 0):"));
        form.add(txtCapEstadio);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar (ID/Nome)");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        vincularValidacao(() -> {
            boolean camposOk = !txtNomeEstadio.getText().trim().isEmpty()
                    && !txtCidadeEstadio.getText().trim().isEmpty()
                    && !txtCapEstadio.getText().trim().isEmpty();
            boolean temId = !txtIdEstadio.getText().trim().isEmpty();
            btnSalvar.setEnabled(camposOk);
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        }, txtIdEstadio, txtNomeEstadio, txtCidadeEstadio, txtCapEstadio);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblEstadio = new JTable(new DefaultTableModel(new Object[]{"ID", "Nome", "Cidade", "Capacidade"}, 0));
        tblEstadio.getSelectionModel().addListSelectionListener(e -> {
            int row = tblEstadio.getSelectedRow();
            if (row >= 0) {
                txtIdEstadio.setText(tblEstadio.getValueAt(row, 0).toString());
                txtNomeEstadio.setText(tblEstadio.getValueAt(row, 1).toString());
                txtCidadeEstadio.setText(tblEstadio.getValueAt(row, 2).toString());
                txtCapEstadio.setText(tblEstadio.getValueAt(row, 3).toString());
            }
        });

        btnSalvar.addActionListener(e -> {
            if (txtNomeEstadio.getText().trim().isEmpty() || txtCidadeEstadio.getText().trim().isEmpty() || txtCapEstadio.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha todos os campos antes de cadastrar!");
                return;
            }
            try {
                Estadio es = new Estadio();
                es.setNome(txtNomeEstadio.getText().trim());
                es.setCidade(txtCidadeEstadio.getText().trim());
                es.setCapacidade(Integer.parseInt(txtCapEstadio.getText().trim()));
                daoEstadio.inserir(es);
                JOptionPane.showMessageDialog(this, "Estádio cadastrado!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = txtNomeEstadio.getText().trim();
            if (termo.isEmpty()) {
                termo = JOptionPane.showInputDialog(this, "Digite o ID ou Nome do Estádio para consultar:");
            }
            if (termo == null || termo.trim().isEmpty()) {
                return;
            }
            try {
                Estadio es = daoEstadio.pesquisarRegistro(termo);
                if (es != null) {
                    txtIdEstadio.setText(String.valueOf(es.getIdEstadio()));
                    txtNomeEstadio.setText(es.getNome());
                    txtCidadeEstadio.setText(es.getCidade());
                    txtCapEstadio.setText(String.valueOf(es.getCapacidade()));
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhum estádio encontrado para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (txtIdEstadio.getText().trim().isEmpty() || txtNomeEstadio.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione um estádio e preencha os campos!");
                return;
            }
            try {
                Estadio es = new Estadio();
                es.setIdEstadio(Integer.parseInt(txtIdEstadio.getText().trim()));
                es.setNome(txtNomeEstadio.getText().trim());
                es.setCidade(txtCidadeEstadio.getText().trim());
                es.setCapacidade(Integer.parseInt(txtCapEstadio.getText().trim()));
                daoEstadio.alterar(es);
                JOptionPane.showMessageDialog(this, "Estádio alterado!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnDeletar.addActionListener(e -> {
            if (txtIdEstadio.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione um estádio para excluir!");
                return;
            }
            try {
                Estadio es = new Estadio();
                es.setIdEstadio(Integer.parseInt(txtIdEstadio.getText().trim()));
                daoEstadio.deletar(es);
                JOptionPane.showMessageDialog(this, "Estádio excluído!");
                txtIdEstadio.setText("");
                txtNomeEstadio.setText("");
                txtCidadeEstadio.setText("");
                txtCapEstadio.setText("");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            txtIdEstadio.setText("");
            txtNomeEstadio.setText("");
            txtCidadeEstadio.setText("");
            txtCapEstadio.setText("");
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblEstadio), BorderLayout.CENTER);
        return painel;
    }

    // =========================================================================
    // ABA 4: ÁRBITRO (CRUD SIMPLES)
    // =========================================================================
    private JPanel criarPainelArbitro() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        txtIdArbitro = new JTextField();
        txtIdArbitro.setEditable(false);
        txtNomeArbitro = new JTextField();
        txtRegArbitro = new JTextField();
        cbCatArbitro = new JComboBox<>(new String[]{"LOCAL", "ESTADUAL", "NACIONAL", "FIFA"});

        form.add(new JLabel("ID:"));
        form.add(txtIdArbitro);
        form.add(new JLabel("Nome do Árbitro:"));
        form.add(txtNomeArbitro);
        form.add(new JLabel("Registro Federação:"));
        form.add(txtRegArbitro);
        form.add(new JLabel("Categoria:"));
        form.add(cbCatArbitro);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar (ID/Nome/Reg)");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        vincularValidacao(() -> {
            boolean camposOk = !txtNomeArbitro.getText().trim().isEmpty()
                    && !txtRegArbitro.getText().trim().isEmpty();
            boolean temId = !txtIdArbitro.getText().trim().isEmpty();
            btnSalvar.setEnabled(camposOk);
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        }, txtIdArbitro, txtNomeArbitro, txtRegArbitro);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblArbitro = new JTable(new DefaultTableModel(new Object[]{"ID", "Nome", "Registro", "Categoria"}, 0));
        tblArbitro.getSelectionModel().addListSelectionListener(e -> {
            int row = tblArbitro.getSelectedRow();
            if (row >= 0) {
                txtIdArbitro.setText(tblArbitro.getValueAt(row, 0).toString());
                txtNomeArbitro.setText(tblArbitro.getValueAt(row, 1).toString());
                txtRegArbitro.setText(tblArbitro.getValueAt(row, 2).toString());
                cbCatArbitro.setSelectedItem(tblArbitro.getValueAt(row, 3).toString());
            }
        });

        btnSalvar.addActionListener(e -> {
            if (txtNomeArbitro.getText().trim().isEmpty() || txtRegArbitro.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Preencha todos os campos antes de cadastrar!");
                return;
            }
            try {
                Arbitro a = new Arbitro();
                a.setNome(txtNomeArbitro.getText().trim());
                a.setRegistro_federacao(txtRegArbitro.getText().trim());
                a.setCategoria(cbCatArbitro.getSelectedItem().toString());
                daoArbitro.inserir(a);
                JOptionPane.showMessageDialog(this, "Árbitro cadastrado!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = txtNomeArbitro.getText().trim();
            if (termo.isEmpty()) {
                termo = txtRegArbitro.getText().trim();
            }
            if (termo.isEmpty()) {
                termo = JOptionPane.showInputDialog(this, "Digite o ID, Nome ou Registro do Árbitro para consultar:");
            }
            if (termo == null || termo.trim().isEmpty()) {
                return;
            }
            try {
                Arbitro a = daoArbitro.pesquisarRegistro(termo);
                if (a != null) {
                    txtIdArbitro.setText(String.valueOf(a.getIdArbitro()));
                    txtNomeArbitro.setText(a.getNome());
                    txtRegArbitro.setText(a.getRegistro_federacao());
                    cbCatArbitro.setSelectedItem(a.getCategoria());
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhum árbitro encontrado para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (txtIdArbitro.getText().trim().isEmpty() || txtNomeArbitro.getText().trim().isEmpty() || txtRegArbitro.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione um árbitro e preencha os campos!");
                return;
            }
            try {
                Arbitro a = new Arbitro();
                a.setIdArbitro(Integer.parseInt(txtIdArbitro.getText().trim()));
                a.setNome(txtNomeArbitro.getText().trim());
                a.setRegistro_federacao(txtRegArbitro.getText().trim());
                a.setCategoria(cbCatArbitro.getSelectedItem().toString());
                daoArbitro.alterar(a);
                JOptionPane.showMessageDialog(this, "Árbitro atualizado!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnDeletar.addActionListener(e -> {
            if (txtIdArbitro.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione um árbitro para excluir!");
                return;
            }
            try {
                Arbitro a = new Arbitro();
                a.setIdArbitro(Integer.parseInt(txtIdArbitro.getText().trim()));
                daoArbitro.deletar(a);
                JOptionPane.showMessageDialog(this, "Árbitro excluído!");
                txtIdArbitro.setText("");
                txtNomeArbitro.setText("");
                txtRegArbitro.setText("");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            txtIdArbitro.setText("");
            txtNomeArbitro.setText("");
            txtRegArbitro.setText("");
            cbCatArbitro.setSelectedIndex(0);
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblArbitro), BorderLayout.CENTER);
        return painel;
    }

    // =========================================================================
    // ABA 5: INSCRIÇÃO & CLASSIFICAÇÃO AUTOMÁTICA (FASE 5)
    // =========================================================================
    private JPanel criarPainelClassificacao() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(2, 3, 8, 8));
        cbCampClass = new JComboBox<>();
        cbEquipeInsc = new JComboBox<>();
        cbStatusClass = new JComboBox<>(new String[]{"A_JOGAR", "CLASSIFICADO", "ELIMINADO"});

        form.add(new JLabel("Campeonato:"));
        form.add(cbCampClass);
        form.add(new JLabel("Equipe para Inscrição:"));
        form.add(cbEquipeInsc);
        form.add(new JLabel("Status na Competição:"));
        form.add(cbStatusClass);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnInscrever = new JButton("Inscrever Equipe no Campeonato");
        JButton btnStatus = new JButton("Atualizar Status da Equipe Selecionada");
        JButton btnDesinscrever = new JButton("Remover Inscrição");
        botoes.add(btnInscrever);
        botoes.add(btnStatus);
        botoes.add(btnDesinscrever);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblClassificacao = new JTable(new DefaultTableModel(
                new Object[]{"Posição", "ID Equipe", "Equipe", "PTS", "J", "V", "E", "D", "GP", "GC", "SG", "Status"}, 0));

        cbCampClass.addActionListener(e -> carregarTabelaClassificacao());

        btnInscrever.addActionListener(e -> {
            try {
                Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
                Equipe eq = (Equipe) cbEquipeInsc.getSelectedItem();
                if (c == null || eq == null) {
                    JOptionPane.showMessageDialog(this, "Cadastre e selecione um Campeonato e uma Equipe primeiro!");
                    return;
                }
                daoClassificacao.inscreverEquipe(c.getIdCampeonato(), eq.getIdEquipe());
                JOptionPane.showMessageDialog(this, "Equipe inscrita no campeonato!");
                carregarTabelaClassificacao();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnStatus.addActionListener(e -> {
            try {
                int row = tblClassificacao.getSelectedRow();
                Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
                if (row >= 0 && c != null) {
                    int idEquipe = Integer.parseInt(tblClassificacao.getValueAt(row, 1).toString());
                    daoClassificacao.atualizarStatus(c.getIdCampeonato(), idEquipe, cbStatusClass.getSelectedItem().toString());
                    carregarTabelaClassificacao();
                } else {
                    JOptionPane.showMessageDialog(this, "Selecione uma linha na tabela de classificação.");
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnDesinscrever.addActionListener(e -> {
            try {
                int row = tblClassificacao.getSelectedRow();
                Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
                if (row >= 0 && c != null) {
                    int idEquipe = Integer.parseInt(tblClassificacao.getValueAt(row, 1).toString());
                    daoClassificacao.removerInscricao(c.getIdCampeonato(), idEquipe);
                    JOptionPane.showMessageDialog(this, "Inscrição removida!");
                    carregarTabelaClassificacao();
                } else {
                    JOptionPane.showMessageDialog(this, "Selecione uma equipe na tabela para remover.");
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblClassificacao), BorderLayout.CENTER);
        return painel;
    }

    // =========================================================================
    // ABA 6: PARTIDAS - CRUD PRINCIPAL COM 5 FKs E REGRAS DE NEGÓCIO (FASE 5)
    // =========================================================================
    private JPanel criarPainelPartidas() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(5, 4, 6, 6));
        txtIdPartida = new JTextField();
        txtIdPartida.setEditable(false);
        cbCampPartida = new JComboBox<>();
        txtRodadaPartida = new JTextField("1");
        txtDataPartida = new JTextField("2026-09-10 16:00:00");
        cbMandante = new JComboBox<>();
        cbVisitante = new JComboBox<>();
        cbEstadioPartida = new JComboBox<>();
        cbArbitroPartida = new JComboBox<>();
        cbStatusPartida = new JComboBox<>(new String[]{"AGENDADA", "EM_ANDAMENTO", "ENCERRADA", "CANCELADA"});
        cbTempoPartida = new JComboBox<>(new String[]{"", "PRIMEIRO_TEMPO", "SEGUNDO_TEMPO"});
        txtGolsMand = new JTextField();
        txtGolsVis = new JTextField();
        txtAcrescimo = new JTextField("0");

        form.add(new JLabel("ID Partida:"));
        form.add(txtIdPartida);
        form.add(new JLabel("Campeonato (FK):"));
        form.add(cbCampPartida);

        form.add(new JLabel("Rodada (> 0):"));
        form.add(txtRodadaPartida);
        form.add(new JLabel("Data/Hora (YYYY-MM-DD HH:MM:SS):"));
        form.add(txtDataPartida);

        form.add(new JLabel("Equipe Mandante (FK):"));
        form.add(cbMandante);
        form.add(new JLabel("Equipe Visitante (FK):"));
        form.add(cbVisitante);

        form.add(new JLabel("Estádio (FK):"));
        form.add(cbEstadioPartida);
        form.add(new JLabel("Árbitro (FK):"));
        form.add(cbArbitroPartida);

        form.add(new JLabel("Status / Tempo:"));
        JPanel pStatusTempo = new JPanel(new GridLayout(1, 2, 4, 4));
        pStatusTempo.add(cbStatusPartida);
        pStatusTempo.add(cbTempoPartida);
        form.add(pStatusTempo);

        form.add(new JLabel("Placar (Mand x Vis) / Acrésc:"));
        JPanel pPlacar = new JPanel(new GridLayout(1, 3, 4, 4));
        pPlacar.add(txtGolsMand);
        pPlacar.add(txtGolsVis);
        pPlacar.add(txtAcrescimo);
        form.add(pPlacar);

        cbStatusPartida.addActionListener(e -> {
            String st = cbStatusPartida.getSelectedItem().toString();
            if (st.equals("AGENDADA") || st.equals("CANCELADA")) {
                txtGolsMand.setText("");
                txtGolsVis.setText("");
                cbTempoPartida.setSelectedIndex(0);
            } else if (st.equals("ENCERRADA")) {
                cbTempoPartida.setSelectedIndex(0);
                if (txtGolsMand.getText().isEmpty()) {
                    txtGolsMand.setText("0");
                }
                if (txtGolsVis.getText().isEmpty()) {
                    txtGolsVis.setText("0");
                }
            }
        });

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgendar = new JButton("Registrar / Agendar");
        JButton btnConsultar = new JButton("Consultar (ID)");
        JButton btnAtualizar = new JButton("Salvar Placar / Alterar");
        JButton btnExcluir = new JButton("Excluir Partida");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnAgendar);
        botoes.add(btnConsultar);
        botoes.add(btnAtualizar);
        botoes.add(btnExcluir);
        botoes.add(btnLimpar);

        vincularValidacao(() -> {
            boolean camposOk = !txtRodadaPartida.getText().trim().isEmpty()
                    && !txtDataPartida.getText().trim().isEmpty()
                    && !txtAcrescimo.getText().trim().isEmpty();
            boolean temId = !txtIdPartida.getText().trim().isEmpty();
            btnAgendar.setEnabled(camposOk);
            btnAtualizar.setEnabled(camposOk && temId);
            btnExcluir.setEnabled(temId);
        }, txtIdPartida, txtRodadaPartida, txtDataPartida, txtAcrescimo);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblPartida = new JTable(new DefaultTableModel(
                new Object[]{"ID", "Campeonato", "Rodada", "Data/Hora", "Mandante", "Placar", "Visitante", "Estádio", "Árbitro", "Status"}, 0));

        tblPartida.getSelectionModel().addListSelectionListener(e -> {
            int row = tblPartida.getSelectedRow();
            if (row >= 0 && row < cachePartidas.size()) {
                preencherCamposPartida(cachePartidas.get(row));
            }
        });

        btnAgendar.addActionListener(e -> {
            if (cbCampPartida.getSelectedItem() == null || cbMandante.getSelectedItem() == null
                    || cbVisitante.getSelectedItem() == null || cbEstadioPartida.getSelectedItem() == null
                    || cbArbitroPartida.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Cadastre Campeonato, Equipes, Estádio e Árbitro antes de agendar!");
                return;
            }
            try {
                Partida p = montarObjetoPartida();
                daoPartida.inserir(p);
                JOptionPane.showMessageDialog(this, "Partida registrada e classificação sincronizada!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = JOptionPane.showInputDialog(this, "Digite o ID da Partida para consultar:");
            if (termo == null || termo.trim().isEmpty()) {
                return;
            }
            try {
                Partida p = daoPartida.pesquisarRegistro(Integer.parseInt(termo.trim()));
                if (p != null) {
                    preencherCamposPartida(p);
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhuma partida encontrada com ID: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAtualizar.addActionListener(e -> {
            if (txtIdPartida.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione uma partida antes de alterar!");
                return;
            }
            try {
                Partida p = montarObjetoPartida();
                p.setIdPartida(Integer.parseInt(txtIdPartida.getText().trim()));
                daoPartida.alterar(p);
                JOptionPane.showMessageDialog(this, "Partida atualizada e classificação recalculada!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnExcluir.addActionListener(e -> {
            if (txtIdPartida.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione uma partida para excluir!");
                return;
            }
            try {
                Partida p = new Partida();
                p.setIdPartida(Integer.parseInt(txtIdPartida.getText().trim()));
                daoPartida.deletar(p);
                JOptionPane.showMessageDialog(this, "Partida removida e classificação recalculada!");
                txtIdPartida.setText("");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            txtIdPartida.setText("");
            txtRodadaPartida.setText("1");
            txtGolsMand.setText("");
            txtGolsVis.setText("");
            txtAcrescimo.setText("0");
            cbStatusPartida.setSelectedIndex(0);
            cbTempoPartida.setSelectedIndex(0);
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblPartida), BorderLayout.CENTER);
        return painel;
    }

    private void preencherCamposPartida(Partida p) {
        txtIdPartida.setText(String.valueOf(p.getIdPartida()));
        txtRodadaPartida.setText(String.valueOf(p.getRodada()));
        txtDataPartida.setText(p.getDataPartida());
        cbStatusPartida.setSelectedItem(p.getStatus());
        cbTempoPartida.setSelectedItem(p.getTempoPartida() == null ? "" : p.getTempoPartida());
        txtGolsMand.setText(p.getGolsMandante() == null ? "" : String.valueOf(p.getGolsMandante()));
        txtGolsVis.setText(p.getGolsVisitante() == null ? "" : String.valueOf(p.getGolsVisitante()));
        txtAcrescimo.setText(String.valueOf(p.getAcrescimo()));

        selecionarComboCampeonato(cbCampPartida, p.getIdCampeonato());
        selecionarComboEquipe(cbMandante, p.getIdMandante());
        selecionarComboEquipe(cbVisitante, p.getIdVisitante());
        selecionarComboEstadio(cbEstadioPartida, p.getIdEstadio());
        selecionarComboArbitro(cbArbitroPartida, p.getIdArbitro());
    }

    private Partida montarObjetoPartida() {
        Partida p = new Partida();
        p.setIdCampeonato(((Campeonato) cbCampPartida.getSelectedItem()).getIdCampeonato());
        p.setRodada(Integer.parseInt(txtRodadaPartida.getText().trim()));
        p.setIdMandante(((Equipe) cbMandante.getSelectedItem()).getIdEquipe());
        p.setIdVisitante(((Equipe) cbVisitante.getSelectedItem()).getIdEquipe());
        p.setIdEstadio(((Estadio) cbEstadioPartida.getSelectedItem()).getIdEstadio());
        p.setIdArbitro(((Arbitro) cbArbitroPartida.getSelectedItem()).getIdArbitro());
        p.setDataPartida(txtDataPartida.getText().trim());

        String gm = txtGolsMand.getText().trim();
        String gv = txtGolsVis.getText().trim();
        p.setGolsMandante(gm.isEmpty() ? null : Integer.parseInt(gm));
        p.setGolsVisitante(gv.isEmpty() ? null : Integer.parseInt(gv));

        p.setAcrescimo(Integer.parseInt(txtAcrescimo.getText().trim()));
        p.setStatus(cbStatusPartida.getSelectedItem().toString());
        String tempo = cbTempoPartida.getSelectedItem().toString();
        p.setTempoPartida(tempo.isEmpty() ? null : tempo);
        return p;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE SINCRONIZAÇÃO DAS TABELAS E COMBOBOXES
    // =========================================================================
    private void recarregarTudo() {
        try {
            ArrayList<Campeonato> camps = daoCampeonato.pesquisarTudo();
            DefaultTableModel mCamp = (DefaultTableModel) tblCampeonato.getModel();
            mCamp.setRowCount(0);
            cbCampClass.removeAllItems();
            cbCampPartida.removeAllItems();
            for (Campeonato c : camps) {
                mCamp.addRow(new Object[]{c.getIdCampeonato(), c.getNome(), c.getData_inicio(), c.getData_fim(), c.getStatus()});
                cbCampClass.addItem(c);
                cbCampPartida.addItem(c);
            }

            ArrayList<Equipe> equipes = daoEquipe.pesquisarTudo();
            DefaultTableModel mEq = (DefaultTableModel) tblEquipe.getModel();
            mEq.setRowCount(0);
            cbEquipeInsc.removeAllItems();
            cbMandante.removeAllItems();
            cbVisitante.removeAllItems();
            for (Equipe eq : equipes) {
                mEq.addRow(new Object[]{eq.getIdEquipe(), eq.getNome(), eq.getCidade()});
                cbEquipeInsc.addItem(eq);
                cbMandante.addItem(eq);
                cbVisitante.addItem(eq);
            }

            ArrayList<Estadio> estadios = daoEstadio.pesquisarTudo();
            DefaultTableModel mEst = (DefaultTableModel) tblEstadio.getModel();
            mEst.setRowCount(0);
            cbEstadioPartida.removeAllItems();
            for (Estadio es : estadios) {
                mEst.addRow(new Object[]{es.getIdEstadio(), es.getNome(), es.getCidade(), es.getCapacidade()});
                cbEstadioPartida.addItem(es);
            }

            ArrayList<Arbitro> arbitros = daoArbitro.pesquisarTudo();
            DefaultTableModel mArb = (DefaultTableModel) tblArbitro.getModel();
            mArb.setRowCount(0);
            cbArbitroPartida.removeAllItems();
            for (Arbitro a : arbitros) {
                mArb.addRow(new Object[]{a.getIdArbitro(), a.getNome(), a.getRegistro_federacao(), a.getCategoria()});
                cbArbitroPartida.addItem(a);
            }

            carregarTabelaClassificacao();

            cachePartidas = daoPartida.pesquisarTudo();
            DefaultTableModel mPart = (DefaultTableModel) tblPartida.getModel();
            mPart.setRowCount(0);
            for (Partida p : cachePartidas) {
                String placar = (p.getGolsMandante() == null) ? "vs" : (p.getGolsMandante() + " x " + p.getGolsVisitante());
                mPart.addRow(new Object[]{
                    p.getIdPartida(), p.getNomeCampeonato(), p.getRodada(), p.getDataPartida(),
                    p.getNomeMandante(), placar, p.getNomeVisitante(),
                    p.getNomeEstadio(), p.getNomeArbitro(), p.getStatus()
                });
            }
        } catch (Exception ex) {
            System.out.println("Aviso ao carregar dados iniciais: " + ex.getMessage());
        }
    }

    private void carregarTabelaClassificacao() {
        try {
            Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
            DefaultTableModel mCl = (DefaultTableModel) tblClassificacao.getModel();
            mCl.setRowCount(0);
            if (c != null) {
                ArrayList<Classificao> lista = daoClassificacao.listarPorCampeonato(c.getIdCampeonato());
                for (Classificao cl : lista) {
                    mCl.addRow(new Object[]{
                        cl.getColocacaoEquipe() == null ? "-" : cl.getColocacaoEquipe() + "º",
                        cl.getIdEquipe(), cl.getNomeEquipe(), cl.getPontuacao(),
                        cl.getPartidasJogadas(), cl.getVitorias(), cl.getEmpates(), cl.getDerrotas(),
                        cl.getGolsMarcados(), cl.getGolsSofridos(), cl.getSaldoGols(), cl.getStatusAtual()
                    });
                }
            }
        } catch (Exception ex) {
            mostrarErro(ex);
        }
    }

    private void selecionarComboCampeonato(JComboBox<Campeonato> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).getIdCampeonato() == id) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selecionarComboEquipe(JComboBox<Equipe> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).getIdEquipe() == id) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selecionarComboEstadio(JComboBox<Estadio> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).getIdEstadio() == id) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selecionarComboArbitro(JComboBox<Arbitro> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).getIdArbitro() == id) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    private void mostrarErro(Exception ex) {
        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        JOptionPane.showMessageDialog(this, "Erro / Regra do Banco:\n" + msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new TelaPrincipal().setVisible(true));
    }

}
