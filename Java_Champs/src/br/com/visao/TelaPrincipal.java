package br.com.visao;

import br.com.controle.*;
import br.com.entidade.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Tela principal do Champs.
 *
 * A tela reune os quatro CRUDs simples (Campeonato, Equipe, Estadio e
 * Arbitro), a inscricao/classificacao e o CRUD principal (Partida), alem da
 * aba de relatorios. Cada botao de escrita so e habilitado quando os campos
 * obrigatorios estao preenchidos e validos.
 */
public class TelaPrincipal extends javax.swing.JFrame {

    // DAOs (camada de entidade)
    private final ManterCampeonato daoCampeonato = new ManterCampeonato();
    private final ManterEquipe daoEquipe = new ManterEquipe();
    private final ManterJogador daoJogador = new ManterJogador();
    private final ManterEstadio daoEstadio = new ManterEstadio();
    private final ManterArbitro daoArbitro = new ManterArbitro();
    private final ManterClassificacao daoClassificacao = new ManterClassificacao();
    private final ManterPartida daoPartida = new ManterPartida();
    private final ManterRelatorio daoRelatorio = new ManterRelatorio();

    // Componentes - aba 1: Campeonato
    private JTextField txtIdCamp, txtNomeCamp, txtInicioCamp, txtFimCamp, txtBuscaCamp;
    private JComboBox<String> cbStatusCamp;
    private JTable tblCampeonato;

    // Componentes - aba 2: Equipe
    private JTextField txtIdEquipe, txtNomeEquipe, txtCidadeEquipe, txtBuscaEquipe;
    private JTable tblEquipe;

    // Componentes - aba 3: Jogador (CRUD extra ligado a Equipe)
    private JTextField txtIdJogador, txtNomeJogador, txtCamisaJogador,
            txtNascJogador, txtBuscaJogador;
    private JComboBox<String> cbPosicaoJogador;
    private JComboBox<Equipe> cbEquipeJogador;
    private JTable tblJogador;
    private ArrayList<Jogador> cacheJogadores = new ArrayList<>();

    // Componentes - aba 4: Estadio
    private JTextField txtIdEstadio, txtNomeEstadio, txtCidadeEstadio, txtCapEstadio, txtBuscaEstadio;
    private JTable tblEstadio;

    // Componentes - aba 5: Arbitro
    private JTextField txtIdArbitro, txtNomeArbitro, txtRegArbitro, txtBuscaArbitro;
    private JComboBox<String> cbCatArbitro;
    private JTable tblArbitro;

    // Componentes - aba 6: Inscricao & Classificacao
    private JComboBox<Campeonato> cbCampClass;
    private JComboBox<Equipe> cbEquipeInsc;
    private JComboBox<String> cbStatusClass;
    private JTable tblClassificacao;
    private JButton btnInscrever, btnStatusClass, btnDesinscrever;

    // Componentes - aba 7: Partidas (CRUD principal)
    private JTextField txtIdPartida, txtRodadaPartida, txtDataPartida, txtGolsMand,
            txtGolsVis, txtAcrescimo, txtBuscaPartida;
    private JComboBox<Campeonato> cbCampPartida;
    private JComboBox<Equipe> cbMandante, cbVisitante;
    private JComboBox<Estadio> cbEstadioPartida;
    private JComboBox<Arbitro> cbArbitroPartida;
    private JComboBox<String> cbStatusPartida, cbTempoPartida;
    private JTable tblPartida;
    private ArrayList<Partida> cachePartidas = new ArrayList<>();

    // Componentes - aba 8: Relatorios
    private JComboBox<Campeonato> cbCampRelatorio;
    private JTextField txtInicioRel, txtFimRel;
    private JTable tblRelClassificacao, tblRelPartidas;
    private JTabbedPane abasRelatorio;
    private JLabel lblResumoRel;
    private JButton btnRelClassificacao, btnRelPartidas, btnImprimirRel;
    private Runnable validarRelatorio;

    public TelaPrincipal() {
        initComponents();
        configurarJanelaCompleta();
        recarregarTudo();
    }

    // =========================================================================
    // HELPERS DE VALIDACAO E DE TABELA
    // =========================================================================

    /**
     * Executa o validador sempre que um dos campos de texto mudar.
     * E o que mantem os botoes "Cadastrar/Alterar/Excluir/Consultar"
     * desabilitados enquanto faltar dado obrigatorio.
     */
    private void vincularValidacao(Runnable validador, JTextField... campos) {
        // DocumentListener tem tres eventos: inserir, remover e alterar.
        // Os tres chamam o mesmo validador, que liga/desliga os botoes.
        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validador.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validador.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validador.run();
            }
        };
        for (JTextField campo : campos) {
            campo.getDocument().addDocumentListener(dl);
        }
        validador.run();
    }

    private void aoMudarCombo(Runnable validador, JComboBox<?>... combos) {
        for (JComboBox<?> combo : combos) {
            combo.addActionListener(e -> validador.run());
        }
    }

    private void aoSelecionarTabela(Runnable validador, JTable... tabelas) {
        for (JTable tabela : tabelas) {
            tabela.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    validador.run();
                }
            });
        }
    }

    private DefaultTableModel modelo(String... colunas) {
        // isCellEditable = false: a JTable e somente leitura; a alteracao
        // acontece pelo formulario e pelo botao Alterar.
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void configurarTabela(JTable tabela) {
        tabela.setRowHeight(24);
        // Sem ordenacao automatica: em Partida/Jogador, a linha selecionada
        // e usada como indice do cache; um sorter mudaria esse indice.
        tabela.setAutoCreateRowSorter(false);
        tabela.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    }

    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private boolean dataValida(String valor) {
        if (vazio(valor)) {
            return false;
        }
        try {
            // valueOf aceita exatamente YYYY-MM-DD; se o texto estiver em
            // outro formato, lanca IllegalArgumentException.
            java.sql.Date.valueOf(valor.trim());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean dataHoraValida(String valor) {
        if (vazio(valor)) {
            return false;
        }
        String[] partes = valor.trim().split("\\s+");
        if (partes.length != 2 || !dataValida(partes[0])) {
            return false;
        }
        // Hora no formato HH:MM ou HH:MM:SS (24h).
        return partes[1].matches("([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?");
    }

    /**
     * A data de nascimento e opcional. Quando preenchida, precisa ser uma
     * data valida e nao pode estar no futuro.
     */
    private boolean dataNascimentoValida(String valor) {
        if (vazio(valor)) {
            return true;
        }
        if (!dataValida(valor)) {
            return false;
        }
        java.sql.Date nascimento = java.sql.Date.valueOf(valor.trim());
        java.sql.Date hoje = new java.sql.Date(System.currentTimeMillis());
        return !nascimento.after(hoje);
    }

    private Integer inteiroOuNulo(String valor) {
        if (vazio(valor)) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean inteiroPositivo(String valor) {
        Integer numero = inteiroOuNulo(valor);
        return numero != null && numero > 0;
    }

    private boolean inteiroNaoNegativo(String valor) {
        Integer numero = inteiroOuNulo(valor);
        return numero != null && numero >= 0;
    }

    private void configurarJanelaCompleta() {
        setTitle("Champs - Sistema de Gestão de Campeonatos");
        setSize(1220, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());

        // A tela e montada em codigo (nao usa o editor visual do NetBeans).
        // Cada aba devolve um JPanel pronto; a TelaPrincipal so os organiza.
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("1. Campeonatos", criarPainelCampeonato());
        abas.addTab("2. Equipes", criarPainelEquipe());
        abas.addTab("3. Jogadores", criarPainelJogador());
        abas.addTab("4. Estádios", criarPainelEstadio());
        abas.addTab("5. Árbitros", criarPainelArbitro());
        abas.addTab("6. Inscrição & Classificação", criarPainelClassificacao());
        abas.addTab("7. Partidas (CRUD Principal)", criarPainelPartidas());
        abas.addTab("8. Relatórios", criarPainelRelatorios());

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
        txtBuscaCamp = new JTextField();
        cbStatusCamp = new JComboBox<>(new String[]{"PLANEJADO", "EM_ANDAMENTO", "ENCERRADO", "CANCELADO"});

        form.add(new JLabel("ID (automático):"));
        form.add(txtIdCamp);
        form.add(new JLabel("Nome do Campeonato:"));
        form.add(txtNomeCamp);
        form.add(new JLabel("Data Início (YYYY-MM-DD):"));
        form.add(txtInicioCamp);
        form.add(new JLabel("Data Fim (YYYY-MM-DD):"));
        form.add(txtFimCamp);
        form.add(new JLabel("Status:"));
        form.add(cbStatusCamp);
        form.add(new JLabel("Pesquisar (ID ou nome):"));
        form.add(txtBuscaCamp);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        // O mesmo validador roda a cada tecla. Cadastrar exige nome e datas
        // coerentes; Alterar/Excluir exigem tambem um ID carregado.
        Runnable validar = () -> {
            boolean camposOk = !vazio(txtNomeCamp.getText())
                    && dataValida(txtInicioCamp.getText())
                    && dataValida(txtFimCamp.getText())
                    && !java.sql.Date.valueOf(txtFimCamp.getText().trim())
                            .before(java.sql.Date.valueOf(txtInicioCamp.getText().trim()));
            boolean temId = !vazio(txtIdCamp.getText());
            btnSalvar.setEnabled(camposOk);
            btnConsultar.setEnabled(!vazio(txtBuscaCamp.getText()));
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        };
        vincularValidacao(validar, txtIdCamp, txtNomeCamp, txtInicioCamp, txtFimCamp, txtBuscaCamp);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblCampeonato = new JTable(modelo("ID", "Nome", "Início", "Fim", "Status"));
        configurarTabela(tblCampeonato);
        // Selecionar a linha copia os dados para o formulario. O ID preenchido
        // e o que habilita os botoes Alterar e Excluir.
        tblCampeonato.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = tblCampeonato.getSelectedRow();
            if (row >= 0) {
                preencherCamposCampeonato(campeonatoDaTabela(row));
            }
        });

        btnSalvar.addActionListener(e -> {
            if (!btnSalvar.isEnabled()) {
                JOptionPane.showMessageDialog(this, "Preencha nome e datas válidas antes de cadastrar!");
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
                limparCamposCampeonato();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> consultarCampeonato());

        btnAlterar.addActionListener(e -> {
            if (!btnAlterar.isEnabled()) {
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
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Excluir o campeonato \"" + txtNomeCamp.getText().trim() + "\"?",
                    "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Campeonato c = new Campeonato();
                c.setIdCampeonato(Integer.parseInt(txtIdCamp.getText().trim()));
                daoCampeonato.deletar(c);
                JOptionPane.showMessageDialog(this, "Campeonato removido!");
                limparCamposCampeonato();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            limparCamposCampeonato();
            tblCampeonato.clearSelection();
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblCampeonato), BorderLayout.CENTER);
        return painel;
    }

    private Campeonato campeonatoDaTabela(int row) {
        Campeonato c = new Campeonato();
        c.setIdCampeonato(Integer.parseInt(tblCampeonato.getValueAt(row, 0).toString()));
        c.setNome(tblCampeonato.getValueAt(row, 1).toString());
        c.setData_inicio(tblCampeonato.getValueAt(row, 2).toString());
        c.setData_fim(tblCampeonato.getValueAt(row, 3).toString());
        c.setStatus(tblCampeonato.getValueAt(row, 4).toString());
        return c;
    }

    private void preencherCamposCampeonato(Campeonato c) {
        txtIdCamp.setText(String.valueOf(c.getIdCampeonato()));
        txtNomeCamp.setText(c.getNome());
        txtInicioCamp.setText(c.getData_inicio());
        txtFimCamp.setText(c.getData_fim());
        cbStatusCamp.setSelectedItem(c.getStatus());
    }

    private void consultarCampeonato() {
        String termo = txtBuscaCamp.getText().trim();
        if (termo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite o ID ou o nome do campeonato no campo de pesquisa.");
            return;
        }
        try {
            // O DAO devolve o objeto completo (ou null). Nao e a JTable que
            // pesquisa: assim o mesmo metodo serve para ID ou nome.
            Campeonato c = daoCampeonato.pesquisarRegistro(termo);
            if (c != null) {
                preencherCamposCampeonato(c);
                tblCampeonato.clearSelection();
            } else {
                JOptionPane.showMessageDialog(this, "Nenhum campeonato encontrado para: " + termo);
            }
        } catch (Exception ex) {
            mostrarErro(ex);
        }
    }

    private void limparCamposCampeonato() {
        txtIdCamp.setText("");
        txtNomeCamp.setText("");
        txtInicioCamp.setText("2026-09-01");
        txtFimCamp.setText("2026-12-30");
        txtBuscaCamp.setText("");
        cbStatusCamp.setSelectedIndex(0);
    }

    // =========================================================================
    // ABA 2: EQUIPE (CRUD SIMPLES)
    // =========================================================================
    private JPanel criarPainelEquipe() {
        // Mesmo esqueleto do Campeonato: formulario, botoes, tabela e
        // validacao. O que muda sao os campos e a query do ManterEquipe.
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        txtIdEquipe = new JTextField();
        txtIdEquipe.setEditable(false);
        txtNomeEquipe = new JTextField();
        txtCidadeEquipe = new JTextField();
        txtBuscaEquipe = new JTextField();

        form.add(new JLabel("ID (automático):"));
        form.add(txtIdEquipe);
        form.add(new JLabel("Nome da Equipe:"));
        form.add(txtNomeEquipe);
        form.add(new JLabel("Cidade:"));
        form.add(txtCidadeEquipe);
        form.add(new JLabel("Pesquisar (ID, nome ou cidade):"));
        form.add(txtBuscaEquipe);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        Runnable validar = () -> {
            boolean camposOk = !vazio(txtNomeEquipe.getText()) && !vazio(txtCidadeEquipe.getText());
            boolean temId = !vazio(txtIdEquipe.getText());
            btnSalvar.setEnabled(camposOk);
            btnConsultar.setEnabled(!vazio(txtBuscaEquipe.getText()));
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        };
        vincularValidacao(validar, txtIdEquipe, txtNomeEquipe, txtCidadeEquipe, txtBuscaEquipe);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblEquipe = new JTable(modelo("ID", "Nome", "Cidade"));
        configurarTabela(tblEquipe);
        tblEquipe.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = tblEquipe.getSelectedRow();
            if (row >= 0) {
                txtIdEquipe.setText(tblEquipe.getValueAt(row, 0).toString());
                txtNomeEquipe.setText(tblEquipe.getValueAt(row, 1).toString());
                txtCidadeEquipe.setText(tblEquipe.getValueAt(row, 2).toString());
            }
        });

        btnSalvar.addActionListener(e -> {
            if (!btnSalvar.isEnabled()) {
                JOptionPane.showMessageDialog(this, "Preencha nome e cidade antes de cadastrar!");
                return;
            }
            try {
                Equipe eq = new Equipe();
                eq.setNome(txtNomeEquipe.getText().trim());
                eq.setCidade(txtCidadeEquipe.getText().trim());
                daoEquipe.inserir(eq);
                JOptionPane.showMessageDialog(this, "Equipe cadastrada!");
                limparCamposEquipe();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = txtBuscaEquipe.getText().trim();
            if (termo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite o ID, nome ou cidade da equipe no campo de pesquisa.");
                return;
            }
            try {
                Equipe eq = daoEquipe.pesquisarRegistro(termo);
                if (eq != null) {
                    txtIdEquipe.setText(String.valueOf(eq.getIdEquipe()));
                    txtNomeEquipe.setText(eq.getNome());
                    txtCidadeEquipe.setText(eq.getCidade());
                    tblEquipe.clearSelection();
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhuma equipe encontrada para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (!btnAlterar.isEnabled()) {
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
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Excluir a equipe \"" + txtNomeEquipe.getText().trim() + "\"?",
                    "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Equipe eq = new Equipe();
                eq.setIdEquipe(Integer.parseInt(txtIdEquipe.getText().trim()));
                daoEquipe.deletar(eq);
                JOptionPane.showMessageDialog(this, "Equipe excluída!");
                limparCamposEquipe();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            limparCamposEquipe();
            tblEquipe.clearSelection();
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblEquipe), BorderLayout.CENTER);
        return painel;
    }

    private void limparCamposEquipe() {
        txtIdEquipe.setText("");
        txtNomeEquipe.setText("");
        txtCidadeEquipe.setText("");
        txtBuscaEquipe.setText("");
    }

    // =========================================================================
    // ABA 3: JOGADOR (CRUD EXTRA LIGADO A EQUIPE)
    // =========================================================================
    private JPanel criarPainelJogador() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(4, 4, 8, 8));
        txtIdJogador = new JTextField();
        txtIdJogador.setEditable(false);
        txtNomeJogador = new JTextField();
        cbPosicaoJogador = new JComboBox<>(new String[]{
            "GOLEIRO", "ZAGUEIRO", "LATERAL", "VOLANTE", "MEIA", "ATACANTE"
        });
        txtCamisaJogador = new JTextField();
        txtNascJogador = new JTextField();
        cbEquipeJogador = new JComboBox<>();
        txtBuscaJogador = new JTextField();

        form.add(new JLabel("ID (automático):"));
        form.add(txtIdJogador);
        form.add(new JLabel("Nome do Jogador:"));
        form.add(txtNomeJogador);
        form.add(new JLabel("Posição:"));
        form.add(cbPosicaoJogador);
        form.add(new JLabel("Camisa (1 a 99):"));
        form.add(txtCamisaJogador);
        form.add(new JLabel("Nascimento (YYYY-MM-DD, opcional):"));
        form.add(txtNascJogador);
        form.add(new JLabel("Equipe (FK):"));
        form.add(cbEquipeJogador);
        form.add(new JLabel("Pesquisar (ID, nome, posição, equipe ou camisa):"));
        form.add(txtBuscaJogador);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        // A camisa e validada no Java (1..99) e tambem no banco pelo CHECK.
        // A data e opcional; quando vazia, o DAO grava NULL.
        Runnable validar = () -> {
            Integer camisa = inteiroOuNulo(txtCamisaJogador.getText());
            boolean camisaOk = camisa != null && camisa >= 1 && camisa <= 99;
            boolean camposOk = !vazio(txtNomeJogador.getText())
                    && camisaOk
                    && dataNascimentoValida(txtNascJogador.getText())
                    && cbEquipeJogador.getSelectedItem() != null;
            boolean temId = !vazio(txtIdJogador.getText());
            btnSalvar.setEnabled(camposOk);
            btnConsultar.setEnabled(!vazio(txtBuscaJogador.getText()));
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        };
        vincularValidacao(validar, txtIdJogador, txtNomeJogador, txtCamisaJogador,
                txtNascJogador, txtBuscaJogador);
        aoMudarCombo(validar, cbPosicaoJogador, cbEquipeJogador);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblJogador = new JTable(modelo(
                "ID", "Nome", "Posição", "Camisa", "Nascimento", "Equipe"));
        configurarTabela(tblJogador);

        tblJogador.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = tblJogador.getSelectedRow();
            if (row >= 0 && row < cacheJogadores.size()) {
                preencherCamposJogador(cacheJogadores.get(row));
            }
        });

        btnSalvar.addActionListener(e -> {
            if (!btnSalvar.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                        "Preencha nome, camisa (1 a 99), equipe e uma data de nascimento válida!");
                return;
            }
            try {
                daoJogador.inserir(montarObjetoJogador());
                JOptionPane.showMessageDialog(this, "Jogador cadastrado!");
                limparCamposJogador();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = txtBuscaJogador.getText().trim();
            if (termo.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Digite o ID, nome, posição, equipe ou camisa do jogador.");
                return;
            }
            try {
                Jogador j = daoJogador.pesquisarRegistro(termo);
                if (j != null) {
                    preencherCamposJogador(j);
                    tblJogador.clearSelection();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Nenhum jogador encontrado para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (!btnAlterar.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                        "Consulte ou selecione um jogador e preencha os campos!");
                return;
            }
            try {
                Jogador j = montarObjetoJogador();
                j.setIdJogador(Integer.parseInt(txtIdJogador.getText().trim()));
                daoJogador.alterar(j);
                JOptionPane.showMessageDialog(this, "Jogador alterado!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnDeletar.addActionListener(e -> {
            if (txtIdJogador.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Consulte ou selecione um jogador para excluir!");
                return;
            }
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Excluir o jogador \"" + txtNomeJogador.getText().trim() + "\"?",
                    "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Jogador j = new Jogador();
                j.setIdJogador(Integer.parseInt(txtIdJogador.getText().trim()));
                daoJogador.deletar(j);
                JOptionPane.showMessageDialog(this, "Jogador excluído!");
                limparCamposJogador();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            limparCamposJogador();
            tblJogador.clearSelection();
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblJogador), BorderLayout.CENTER);
        return painel;
    }

    private void preencherCamposJogador(Jogador j) {
        txtIdJogador.setText(String.valueOf(j.getIdJogador()));
        txtNomeJogador.setText(j.getNome());
        cbPosicaoJogador.setSelectedItem(j.getPosicao());
        txtCamisaJogador.setText(String.valueOf(j.getNumeroCamisa()));
        txtNascJogador.setText(j.getDataNascimento() == null ? "" : j.getDataNascimento());
        selecionarComboEquipe(cbEquipeJogador, j.getIdEquipe());
    }

    private Jogador montarObjetoJogador() {
        // A FK vem do objeto Equipe selecionado no combo, nao do texto.
        // Data vazia vira null para o banco aceitar o campo opcional.
        Jogador j = new Jogador();
        j.setNome(txtNomeJogador.getText().trim());
        j.setPosicao(cbPosicaoJogador.getSelectedItem().toString());
        j.setNumeroCamisa(Integer.parseInt(txtCamisaJogador.getText().trim()));
        j.setDataNascimento(vazio(txtNascJogador.getText())
                ? null : txtNascJogador.getText().trim());
        j.setIdEquipe(((Equipe) cbEquipeJogador.getSelectedItem()).getIdEquipe());
        return j;
    }

    private void limparCamposJogador() {
        txtIdJogador.setText("");
        txtNomeJogador.setText("");
        cbPosicaoJogador.setSelectedIndex(0);
        txtCamisaJogador.setText("");
        txtNascJogador.setText("");
        txtBuscaJogador.setText("");
        if (cbEquipeJogador.getItemCount() > 0) {
            cbEquipeJogador.setSelectedIndex(0);
        }
    }

    // =========================================================================
    // ABA 4: ESTADIO (CRUD SIMPLES)
    // =========================================================================
    private JPanel criarPainelEstadio() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        txtIdEstadio = new JTextField();
        txtIdEstadio.setEditable(false);
        txtNomeEstadio = new JTextField();
        txtCidadeEstadio = new JTextField();
        txtCapEstadio = new JTextField();
        txtBuscaEstadio = new JTextField();

        form.add(new JLabel("ID (automático):"));
        form.add(txtIdEstadio);
        form.add(new JLabel("Nome do Estádio:"));
        form.add(txtNomeEstadio);
        form.add(new JLabel("Cidade:"));
        form.add(txtCidadeEstadio);
        form.add(new JLabel("Capacidade (> 0):"));
        form.add(txtCapEstadio);
        form.add(new JLabel("Pesquisar (ID, nome ou cidade):"));
        form.add(txtBuscaEstadio);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        // Capacidade nao basta estar preenchida: precisa ser inteiro > 0.
        Runnable validar = () -> {
            boolean camposOk = !vazio(txtNomeEstadio.getText())
                    && !vazio(txtCidadeEstadio.getText())
                    && inteiroPositivo(txtCapEstadio.getText());
            boolean temId = !vazio(txtIdEstadio.getText());
            btnSalvar.setEnabled(camposOk);
            btnConsultar.setEnabled(!vazio(txtBuscaEstadio.getText()));
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        };
        vincularValidacao(validar, txtIdEstadio, txtNomeEstadio, txtCidadeEstadio,
                txtCapEstadio, txtBuscaEstadio);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblEstadio = new JTable(modelo("ID", "Nome", "Cidade", "Capacidade"));
        configurarTabela(tblEstadio);
        tblEstadio.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = tblEstadio.getSelectedRow();
            if (row >= 0) {
                txtIdEstadio.setText(tblEstadio.getValueAt(row, 0).toString());
                txtNomeEstadio.setText(tblEstadio.getValueAt(row, 1).toString());
                txtCidadeEstadio.setText(tblEstadio.getValueAt(row, 2).toString());
                txtCapEstadio.setText(tblEstadio.getValueAt(row, 3).toString());
            }
        });

        btnSalvar.addActionListener(e -> {
            if (!btnSalvar.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                        "Preencha nome, cidade e uma capacidade maior que zero!");
                return;
            }
            try {
                Estadio es = new Estadio();
                es.setNome(txtNomeEstadio.getText().trim());
                es.setCidade(txtCidadeEstadio.getText().trim());
                es.setCapacidade(Integer.parseInt(txtCapEstadio.getText().trim()));
                daoEstadio.inserir(es);
                JOptionPane.showMessageDialog(this, "Estádio cadastrado!");
                limparCamposEstadio();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            String termo = txtBuscaEstadio.getText().trim();
            if (termo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite o ID, nome ou cidade do estádio no campo de pesquisa.");
                return;
            }
            try {
                Estadio es = daoEstadio.pesquisarRegistro(termo);
                if (es != null) {
                    txtIdEstadio.setText(String.valueOf(es.getIdEstadio()));
                    txtNomeEstadio.setText(es.getNome());
                    txtCidadeEstadio.setText(es.getCidade());
                    txtCapEstadio.setText(String.valueOf(es.getCapacidade()));
                    tblEstadio.clearSelection();
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhum estádio encontrado para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (!btnAlterar.isEnabled()) {
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
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Excluir o estádio \"" + txtNomeEstadio.getText().trim() + "\"?",
                    "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Estadio es = new Estadio();
                es.setIdEstadio(Integer.parseInt(txtIdEstadio.getText().trim()));
                daoEstadio.deletar(es);
                JOptionPane.showMessageDialog(this, "Estádio excluído!");
                limparCamposEstadio();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            limparCamposEstadio();
            tblEstadio.clearSelection();
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblEstadio), BorderLayout.CENTER);
        return painel;
    }

    private void limparCamposEstadio() {
        txtIdEstadio.setText("");
        txtNomeEstadio.setText("");
        txtCidadeEstadio.setText("");
        txtCapEstadio.setText("");
        txtBuscaEstadio.setText("");
    }

    // =========================================================================
    // ABA 5: ARBITRO (CRUD SIMPLES)
    // =========================================================================
    private JPanel criarPainelArbitro() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(3, 4, 8, 8));
        txtIdArbitro = new JTextField();
        txtIdArbitro.setEditable(false);
        txtNomeArbitro = new JTextField();
        txtRegArbitro = new JTextField();
        txtBuscaArbitro = new JTextField();
        cbCatArbitro = new JComboBox<>(new String[]{"LOCAL", "ESTADUAL", "NACIONAL", "FIFA"});

        form.add(new JLabel("ID (automático):"));
        form.add(txtIdArbitro);
        form.add(new JLabel("Nome do Árbitro:"));
        form.add(txtNomeArbitro);
        form.add(new JLabel("Registro Federação:"));
        form.add(txtRegArbitro);
        form.add(new JLabel("Categoria:"));
        form.add(cbCatArbitro);
        form.add(new JLabel("Pesquisar (ID, nome ou registro):"));
        form.add(txtBuscaArbitro);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSalvar = new JButton("Cadastrar");
        JButton btnConsultar = new JButton("Consultar");
        JButton btnAlterar = new JButton("Alterar");
        JButton btnDeletar = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnSalvar);
        botoes.add(btnConsultar);
        botoes.add(btnAlterar);
        botoes.add(btnDeletar);
        botoes.add(btnLimpar);

        Runnable validar = () -> {
            boolean camposOk = !vazio(txtNomeArbitro.getText()) && !vazio(txtRegArbitro.getText());
            boolean temId = !vazio(txtIdArbitro.getText());
            btnSalvar.setEnabled(camposOk);
            btnConsultar.setEnabled(!vazio(txtBuscaArbitro.getText()));
            btnAlterar.setEnabled(camposOk && temId);
            btnDeletar.setEnabled(temId);
        };
        vincularValidacao(validar, txtIdArbitro, txtNomeArbitro, txtRegArbitro, txtBuscaArbitro);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblArbitro = new JTable(modelo("ID", "Nome", "Registro", "Categoria"));
        configurarTabela(tblArbitro);
        tblArbitro.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = tblArbitro.getSelectedRow();
            if (row >= 0) {
                txtIdArbitro.setText(tblArbitro.getValueAt(row, 0).toString());
                txtNomeArbitro.setText(tblArbitro.getValueAt(row, 1).toString());
                txtRegArbitro.setText(tblArbitro.getValueAt(row, 2).toString());
                cbCatArbitro.setSelectedItem(tblArbitro.getValueAt(row, 3).toString());
            }
        });

        btnSalvar.addActionListener(e -> {
            if (!btnSalvar.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                        "Preencha nome e registro na federação antes de cadastrar!");
                return;
            }
            try {
                Arbitro a = new Arbitro();
                a.setNome(txtNomeArbitro.getText().trim());
                a.setRegistro_federacao(txtRegArbitro.getText().trim());
                a.setCategoria(cbCatArbitro.getSelectedItem().toString());
                daoArbitro.inserir(a);
                JOptionPane.showMessageDialog(this, "Árbitro cadastrado!");
                limparCamposArbitro();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            // O DAO procura em tres colunas: id, nome e registro.
            String termo = txtBuscaArbitro.getText().trim();
            if (termo.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Digite o ID, nome ou registro do árbitro no campo de pesquisa.");
                return;
            }
            try {
                Arbitro a = daoArbitro.pesquisarRegistro(termo);
                if (a != null) {
                    txtIdArbitro.setText(String.valueOf(a.getIdArbitro()));
                    txtNomeArbitro.setText(a.getNome());
                    txtRegArbitro.setText(a.getRegistro_federacao());
                    cbCatArbitro.setSelectedItem(a.getCategoria());
                    tblArbitro.clearSelection();
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhum árbitro encontrado para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnAlterar.addActionListener(e -> {
            if (!btnAlterar.isEnabled()) {
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
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Excluir o árbitro \"" + txtNomeArbitro.getText().trim() + "\"?",
                    "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Arbitro a = new Arbitro();
                a.setIdArbitro(Integer.parseInt(txtIdArbitro.getText().trim()));
                daoArbitro.deletar(a);
                JOptionPane.showMessageDialog(this, "Árbitro excluído!");
                limparCamposArbitro();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            limparCamposArbitro();
            tblArbitro.clearSelection();
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblArbitro), BorderLayout.CENTER);
        return painel;
    }

    private void limparCamposArbitro() {
        txtIdArbitro.setText("");
        txtNomeArbitro.setText("");
        txtRegArbitro.setText("");
        txtBuscaArbitro.setText("");
        cbCatArbitro.setSelectedIndex(0);
    }

    // =========================================================================
    // ABA 6: INSCRICAO & CLASSIFICACAO AUTOMATICA
    // =========================================================================
    private JPanel criarPainelClassificacao() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Duas linhas de campos. Na primeira, dois grupos "rótulo + combo"
        // dividem a largura; o rótulo fica sempre colado ao seu combo.
        cbCampClass = new JComboBox<>();
        cbEquipeInsc = new JComboBox<>();
        cbStatusClass = new JComboBox<>(new String[]{"A_JOGAR", "CLASSIFICADO", "ELIMINADO"});

        JPanel linhaCampos = new JPanel(new GridLayout(1, 2, 12, 0));

        JPanel grupoCamp = new JPanel(new BorderLayout(6, 0));
        grupoCamp.add(new JLabel("Campeonato:"), BorderLayout.WEST);
        grupoCamp.add(cbCampClass, BorderLayout.CENTER);

        JPanel grupoEquipe = new JPanel(new BorderLayout(6, 0));
        grupoEquipe.add(new JLabel("Equipe para Inscrição:"), BorderLayout.WEST);
        grupoEquipe.add(cbEquipeInsc, BorderLayout.CENTER);

        linhaCampos.add(grupoCamp);
        linhaCampos.add(grupoEquipe);

        JPanel linhaStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        linhaStatus.add(new JLabel("Status na Competição:"));
        cbStatusClass.setPreferredSize(new Dimension(190, cbStatusClass.getPreferredSize().height));
        linhaStatus.add(cbStatusClass);

        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.add(linhaCampos, BorderLayout.NORTH);
        form.add(linhaStatus, BorderLayout.SOUTH);

        btnInscrever = new JButton("Inscrever Equipe no Campeonato");
        btnStatusClass = new JButton("Atualizar Status da Equipe Selecionada");
        btnDesinscrever = new JButton("Remover Inscrição");

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(btnInscrever);
        botoes.add(btnStatusClass);
        botoes.add(btnDesinscrever);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblClassificacao = new JTable(modelo(
                "Posição", "ID Equipe", "Equipe", "PTS", "J", "V", "E", "D",
                "GP", "GC", "SG", "Status"));
        configurarTabela(tblClassificacao);
        tblClassificacao.setFillsViewportHeight(true);

        // Larguras para a tabela não ficar com colunas gigantes vazias.
        int[] largurasClassificacao = {65, 75, 240, 55, 45, 45, 45, 45, 45, 45, 45, 110};
        for (int i = 0; i < largurasClassificacao.length; i++) {
            tblClassificacao.getColumnModel().getColumn(i)
                    .setPreferredWidth(largurasClassificacao[i]);
        }

        // Trocar o campeonato recarrega a tabela. A classificacao vem do
        // SELECT com INNER JOIN de ManterClassificacao (nao e calculada aqui).
        cbCampClass.addActionListener(e -> carregarTabelaClassificacao());
        aoMudarCombo(this::atualizarBotoesClassificacao, cbCampClass, cbEquipeInsc);
        aoSelecionarTabela(this::atualizarBotoesClassificacao, tblClassificacao);

        // Inscrever chama a procedure sp_inscrever_equipe; o INSERT IGNORE
        // do banco evita duplicar a mesma equipe no campeonato.
        btnInscrever.addActionListener(e -> {
            Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
            Equipe eq = (Equipe) cbEquipeInsc.getSelectedItem();
            if (c == null || eq == null) {
                JOptionPane.showMessageDialog(this,
                        "Cadastre e selecione um Campeonato e uma Equipe primeiro!");
                return;
            }
            try {
                daoClassificacao.inscreverEquipe(c.getIdCampeonato(), eq.getIdEquipe());
                JOptionPane.showMessageDialog(this, "Equipe inscrita no campeonato!");
                carregarTabelaClassificacao();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        // O status_atual e manual (A_JOGAR/CLASSIFICADO/ELIMINADO); a
        // pontuacao e a colocacao continuam sendo calculadas pelo banco.
        btnStatusClass.addActionListener(e -> {
            int row = tblClassificacao.getSelectedRow();
            Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
            if (row >= 0 && c != null) {
                try {
                    int idEquipe = Integer.parseInt(tblClassificacao.getValueAt(row, 1).toString());
                    daoClassificacao.atualizarStatus(c.getIdCampeonato(), idEquipe,
                            cbStatusClass.getSelectedItem().toString());
                    JOptionPane.showMessageDialog(this, "Status atualizado!");
                    carregarTabelaClassificacao();
                } catch (Exception ex) {
                    mostrarErro(ex);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Selecione uma linha na tabela de classificação.");
            }
        });

        // O DELETE e bloqueado pelo trigger se a equipe ja tiver partidas
        // no campeonato (preserva o historico).
        btnDesinscrever.addActionListener(e -> {
            int row = tblClassificacao.getSelectedRow();
            Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
            if (row >= 0 && c != null) {
                int resposta = JOptionPane.showConfirmDialog(this,
                        "Remover a inscrição da equipe selecionada?",
                        "Confirmar remoção", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (resposta != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    int idEquipe = Integer.parseInt(tblClassificacao.getValueAt(row, 1).toString());
                    daoClassificacao.removerInscricao(c.getIdCampeonato(), idEquipe);
                    JOptionPane.showMessageDialog(this, "Inscrição removida!");
                    carregarTabelaClassificacao();
                } catch (Exception ex) {
                    mostrarErro(ex);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Selecione uma equipe na tabela para remover.");
            }
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblClassificacao), BorderLayout.CENTER);
        atualizarBotoesClassificacao();
        return painel;
    }

    private void atualizarBotoesClassificacao() {
        if (btnInscrever == null) {
            return;
        }
        // Inscrever depende dos dois combos; atualizar/remover dependem de
        // uma linha selecionada na tabela.
        boolean temCampeonato = cbCampClass.getSelectedItem() != null;
        boolean temEquipe = cbEquipeInsc.getSelectedItem() != null;
        boolean temLinha = tblClassificacao.getSelectedRow() >= 0;
        btnInscrever.setEnabled(temCampeonato && temEquipe);
        btnStatusClass.setEnabled(temCampeonato && temLinha);
        btnDesinscrever.setEnabled(temCampeonato && temLinha);
    }

    // =========================================================================
    // ABA 7: PARTIDAS - CRUD PRINCIPAL COM 5 FKs E REGRAS DE NEGOCIO
    // =========================================================================
    private JPanel criarPainelPartidas() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(6, 4, 6, 6));
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
        txtBuscaPartida = new JTextField();

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

        form.add(new JLabel("Pesquisar (ID, equipe, campeonato...):"));
        form.add(txtBuscaPartida);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        // O status manda no placar: jogo agendado/cancelado nao tem gols;
        // jogo encerrado precisa de placar. O tempo so existe em andamento.
        cbStatusPartida.addActionListener(e -> {
            String status = cbStatusPartida.getSelectedItem().toString();
            boolean emAndamento = status.equals("EM_ANDAMENTO");
            cbTempoPartida.setEnabled(emAndamento);
            if (!emAndamento) {
                cbTempoPartida.setSelectedIndex(0);
            }
            if (status.equals("AGENDADA") || status.equals("CANCELADA")) {
                txtGolsMand.setText("");
                txtGolsVis.setText("");
            } else if (status.equals("ENCERRADA")) {
                if (txtGolsMand.getText().trim().isEmpty()) {
                    txtGolsMand.setText("0");
                }
                if (txtGolsVis.getText().trim().isEmpty()) {
                    txtGolsVis.setText("0");
                }
            }
        });
        cbTempoPartida.setEnabled(false);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgendar = new JButton("Registrar / Agendar");
        JButton btnConsultar = new JButton("Consultar");
        JButton btnAtualizar = new JButton("Salvar Placar / Alterar");
        JButton btnExcluir = new JButton("Excluir Partida");
        JButton btnLimpar = new JButton("Limpar");
        botoes.add(btnAgendar);
        botoes.add(btnConsultar);
        botoes.add(btnAtualizar);
        botoes.add(btnExcluir);
        botoes.add(btnLimpar);

        // Validacao mais completa do sistema: combos obrigatorios, rodada,
        // data/hora, acrescimo, placar e regra de mandante != visitante.
        Runnable validar = () -> {
            boolean combosOk = cbCampPartida.getSelectedItem() != null
                    && cbMandante.getSelectedItem() != null
                    && cbVisitante.getSelectedItem() != null
                    && cbEstadioPartida.getSelectedItem() != null
                    && cbArbitroPartida.getSelectedItem() != null;
            // Ou os dois gols estao vazios, ou os dois sao numeros >= 0.
            // Se ENCERRADA, os dois precisam estar preenchidos.
            boolean golsOk = (vazio(txtGolsMand.getText()) && vazio(txtGolsVis.getText()))
                    || (inteiroNaoNegativo(txtGolsMand.getText())
                    && inteiroNaoNegativo(txtGolsVis.getText()));
            if ("ENCERRADA".equals(cbStatusPartida.getSelectedItem())) {
                golsOk = inteiroNaoNegativo(txtGolsMand.getText())
                        && inteiroNaoNegativo(txtGolsVis.getText());
            }
            // O banco tambem tem CHECK, mas a tela evita habilitar o botao.
            boolean timesDiferentes = true;
            Equipe mandante = (Equipe) cbMandante.getSelectedItem();
            Equipe visitante = (Equipe) cbVisitante.getSelectedItem();
            if (mandante != null && visitante != null) {
                timesDiferentes = mandante.getIdEquipe() != visitante.getIdEquipe();
            }
            boolean camposOk = combosOk
                    && inteiroPositivo(txtRodadaPartida.getText())
                    && dataHoraValida(txtDataPartida.getText())
                    && inteiroNaoNegativo(txtAcrescimo.getText())
                    && golsOk
                    && timesDiferentes;
            boolean temId = !vazio(txtIdPartida.getText());
            btnAgendar.setEnabled(camposOk);
            btnConsultar.setEnabled(!vazio(txtBuscaPartida.getText()));
            btnAtualizar.setEnabled(camposOk && temId);
            btnExcluir.setEnabled(temId);
        };
        vincularValidacao(validar, txtIdPartida, txtRodadaPartida, txtDataPartida,
                txtGolsMand, txtGolsVis, txtAcrescimo, txtBuscaPartida);
        aoMudarCombo(validar, cbCampPartida, cbMandante, cbVisitante,
                cbEstadioPartida, cbArbitroPartida, cbStatusPartida);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblPartida = new JTable(modelo(
                "ID", "Campeonato", "Rodada", "Data/Hora", "Mandante", "Placar",
                "Visitante", "Estádio", "Árbitro", "Status"));
        configurarTabela(tblPartida);

        tblPartida.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = tblPartida.getSelectedRow();
            if (row >= 0 && row < cachePartidas.size()) {
                preencherCamposPartida(cachePartidas.get(row));
            }
        });

        // INSERT em PARTIDA: o trigger AFTER chama a sincronizacao da
        // classificacao, por isso a tela recarrega tudo depois.
        btnAgendar.addActionListener(e -> {
            if (!btnAgendar.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                        "Confira os campos obrigatórios, as equipes e o placar antes de agendar!");
                return;
            }
            try {
                Partida p = montarObjetoPartida();
                daoPartida.inserir(p);
                JOptionPane.showMessageDialog(this,
                        "Partida registrada e classificação sincronizada!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnConsultar.addActionListener(e -> {
            // A busca aceita ID ou nomes; o DAO decide qual coluna usar.
            String termo = txtBuscaPartida.getText().trim();
            if (termo.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Digite o ID (ou nome de equipe, campeonato, estádio ou árbitro) para consultar.");
                return;
            }
            try {
                Partida p = daoPartida.pesquisarRegistro(termo);
                if (p != null) {
                    preencherCamposPartida(p);
                    tblPartida.clearSelection();
                } else {
                    JOptionPane.showMessageDialog(this, "Nenhuma partida encontrada para: " + termo);
                }
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        // UPDATE em PARTIDA: o trigger BEFORE valida e o AFTER recalcula.
        btnAtualizar.addActionListener(e -> {
            if (!btnAtualizar.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                        "Consulte ou selecione uma partida e preencha os campos antes de alterar!");
                return;
            }
            try {
                Partida p = montarObjetoPartida();
                p.setIdPartida(Integer.parseInt(txtIdPartida.getText().trim()));
                daoPartida.alterar(p);
                JOptionPane.showMessageDialog(this,
                        "Partida atualizada e classificação recalculada!");
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        // DELETE em PARTIDA: o trigger AFTER tambem recalcula a classificacao.
        btnExcluir.addActionListener(e -> {
            if (txtIdPartida.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Consulte ou selecione uma partida para excluir!");
                return;
            }
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Excluir a partida selecionada? A classificação será recalculada.",
                    "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Partida p = new Partida();
                p.setIdPartida(Integer.parseInt(txtIdPartida.getText().trim()));
                daoPartida.deletar(p);
                JOptionPane.showMessageDialog(this,
                        "Partida removida e classificação recalculada!");
                limparCamposPartida();
                recarregarTudo();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        });

        btnLimpar.addActionListener(e -> {
            limparCamposPartida();
            tblPartida.clearSelection();
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

        // Os combos guardam objetos (Equipe, Estadio...), nao apenas o ID;
        // por isso procuramos o item cujo getId coincide com o da partida.
        selecionarComboCampeonato(cbCampPartida, p.getIdCampeonato());
        selecionarComboEquipe(cbMandante, p.getIdMandante());
        selecionarComboEquipe(cbVisitante, p.getIdVisitante());
        selecionarComboEstadio(cbEstadioPartida, p.getIdEstadio());
        selecionarComboArbitro(cbArbitroPartida, p.getIdArbitro());
    }

    private Partida montarObjetoPartida() {
        // Converte a tela para o objeto que o DAO espera. Gols vazios viram
        // null: o banco usa NULL para jogo sem placar (AGENDADA/CANCELADA).
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

    private void limparCamposPartida() {
        txtIdPartida.setText("");
        txtRodadaPartida.setText("1");
        txtGolsMand.setText("");
        txtGolsVis.setText("");
        txtAcrescimo.setText("0");
        txtBuscaPartida.setText("");
        cbStatusPartida.setSelectedIndex(0);
        cbTempoPartida.setSelectedIndex(0);
        cbTempoPartida.setEnabled(false);
    }

    // =========================================================================
    // ABA 8: RELATORIOS
    // =========================================================================
    private JPanel criarPainelRelatorios() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cbCampRelatorio = new JComboBox<>();
        txtInicioRel = new JTextField("2026-09-01");
        txtFimRel = new JTextField("2026-12-30");

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        form.add(new JLabel("Campeonato:"));
        form.add(cbCampRelatorio);
        form.add(new JLabel("Data Início (YYYY-MM-DD):"));
        form.add(txtInicioRel);
        form.add(new JLabel("Data Fim (YYYY-MM-DD):"));
        form.add(txtFimRel);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        btnRelClassificacao = new JButton("Relatório de Classificação (Principal)");
        btnRelPartidas = new JButton("Partidas por Período");
        btnImprimirRel = new JButton("Imprimir / Salvar em PDF");
        JButton btnLimparRel = new JButton("Limpar");

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(btnRelClassificacao);
        botoes.add(btnRelPartidas);
        botoes.add(btnImprimirRel);
        botoes.add(btnLimparRel);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form, BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        tblRelClassificacao = new JTable(modelo(
                "Pos.", "Equipe", "Cidade", "PTS", "J", "V", "E", "D",
                "GP", "GC", "SG", "Aprov.", "Casa", "Fora", "Estádios", "Árbitros", "Status"));
        configurarTabela(tblRelClassificacao);

        tblRelPartidas = new JTable(modelo(
                "Rodada", "Data/Hora", "Mandante", "Visitante", "Estádio",
                "Árbitro", "Placar", "Status"));
        configurarTabela(tblRelPartidas);

        abasRelatorio = new JTabbedPane();
        abasRelatorio.addTab("Classificação (rotina principal)", new JScrollPane(tblRelClassificacao));
        abasRelatorio.addTab("Partidas por período", new JScrollPane(tblRelPartidas));

        lblResumoRel = new JLabel("Selecione um campeonato e gere um relatório.");
        lblResumoRel.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 2));

        Runnable validar = () -> {
            boolean temCamp = cbCampRelatorio.getSelectedItem() != null;
            boolean datasOk = dataValida(txtInicioRel.getText())
                    && dataValida(txtFimRel.getText())
                    && !java.sql.Date.valueOf(txtFimRel.getText().trim())
                            .before(java.sql.Date.valueOf(txtInicioRel.getText().trim()));
            btnRelClassificacao.setEnabled(temCamp);
            btnRelPartidas.setEnabled(temCamp && datasOk);
            btnImprimirRel.setEnabled(tabelaRelatorioAtual().getRowCount() > 0);
        };
        vincularValidacao(validar, txtInicioRel, txtFimRel);
        validarRelatorio = validar;
        cbCampRelatorio.addActionListener(e -> validar.run());
        abasRelatorio.addChangeListener(e -> validar.run());

        btnRelClassificacao.addActionListener(e -> gerarRelatorioClassificacao());
        btnRelPartidas.addActionListener(e -> gerarRelatorioPartidas());

        btnImprimirRel.addActionListener(e -> {
            JTable tabela = tabelaRelatorioAtual();
            if (tabela.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Gere um relatório antes de imprimir.");
                return;
            }
            String titulo = abasRelatorio.getSelectedIndex() == 0
                    ? "Champs - Classificação"
                    : "Champs - Partidas por período";
            try {
                tabela.print(JTable.PrintMode.FIT_WIDTH,
                        new MessageFormat(titulo),
                        new MessageFormat("Página {0}"));
            } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(this,
                        "Não foi possível imprimir: " + ex.getMessage(),
                        "Impressão", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnLimparRel.addActionListener(e -> {
            ((DefaultTableModel) tblRelClassificacao.getModel()).setRowCount(0);
            ((DefaultTableModel) tblRelPartidas.getModel()).setRowCount(0);
            lblResumoRel.setText("Selecione um campeonato e gere um relatório.");
            validar.run();
        });

        painel.add(topo, BorderLayout.NORTH);
        painel.add(abasRelatorio, BorderLayout.CENTER);
        painel.add(lblResumoRel, BorderLayout.SOUTH);
        validar.run();
        return painel;
    }

    private JTable tabelaRelatorioAtual() {
        if (abasRelatorio == null || abasRelatorio.getSelectedIndex() <= 0) {
            return tblRelClassificacao;
        }
        return tblRelPartidas;
    }

    private void gerarRelatorioClassificacao() {
        // Este e o relatorio principal: a procedure ja devolve a classificacao
        // pronta (com JOINs); aqui so montamos a JTable e o resumo.
        Campeonato c = (Campeonato) cbCampRelatorio.getSelectedItem();
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Cadastre e selecione um campeonato.");
            return;
        }
        try {
            ArrayList<RelatorioClassificacao> lista
                    = daoRelatorio.classificacaoPorCampeonato(c.getIdCampeonato());
            DefaultTableModel m = (DefaultTableModel) tblRelClassificacao.getModel();
            m.setRowCount(0);

            int somaJogos = 0;
            int somaGols = 0;
            for (RelatorioClassificacao linha : lista) {
                m.addRow(new Object[]{
                    linha.getPosicaoFormatada(),
                    linha.getNomeEquipe(),
                    linha.getCidade(),
                    linha.getPontos(),
                    linha.getJogos(),
                    linha.getVitorias(),
                    linha.getEmpates(),
                    linha.getDerrotas(),
                    linha.getGolsPro(),
                    linha.getGolsContra(),
                    linha.getSaldoGols(),
                    linha.getAproveitamentoFormatado(),
                    linha.getJogosCasa(),
                    linha.getJogosFora(),
                    linha.getEstadios(),
                    linha.getArbitros(),
                    linha.getStatusAtual()
                });
                somaJogos += linha.getJogos();
                somaGols += linha.getGolsPro();
            }

            int partidas = somaJogos / 2; // cada partida conta para as duas equipes
            int gols = somaGols / 2;
            String media = partidas == 0 ? "0,00" : String.format(java.util.Locale.ROOT, "%.2f", (double) gols / partidas);
            lblResumoRel.setText("Classificação de " + c.getNome()
                    + "  |  Equipes: " + lista.size()
                    + "  |  Partidas encerradas: " + partidas
                    + "  |  Gols: " + gols
                    + "  |  Média de gols por partida: " + media);
            abasRelatorio.setSelectedIndex(0);
            if (validarRelatorio != null) {
                validarRelatorio.run();
            }

            if (lista.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Nenhuma equipe inscrita neste campeonato. Use a aba de Inscrição.");
            }
        } catch (Exception ex) {
            mostrarErro(ex);
        }
    }

    private void gerarRelatorioPartidas() {
        // O outro relatorio usa a procedure por periodo; os contadores abaixo
        // so alimentam o texto de resumo no rodape.
        Campeonato c = (Campeonato) cbCampRelatorio.getSelectedItem();
        if (c == null || !dataValida(txtInicioRel.getText()) || !dataValida(txtFimRel.getText())) {
            JOptionPane.showMessageDialog(this, "Selecione um campeonato e datas válidas.");
            return;
        }
        try {
            ArrayList<RelatorioPartida> lista = daoRelatorio.partidasPorPeriodo(
                    c.getIdCampeonato(), txtInicioRel.getText().trim(), txtFimRel.getText().trim());
            DefaultTableModel m = (DefaultTableModel) tblRelPartidas.getModel();
            m.setRowCount(0);

            int encerradas = 0;
            int canceladas = 0;
            int agendadas = 0;
            for (RelatorioPartida linha : lista) {
                m.addRow(new Object[]{
                    linha.getRodada(),
                    linha.getDataHora(),
                    linha.getMandante(),
                    linha.getVisitante(),
                    linha.getEstadio(),
                    linha.getArbitro(),
                    linha.getPlacar(),
                    linha.getStatus()
                });
                if ("ENCERRADA".equals(linha.getStatus())) {
                    encerradas++;
                } else if ("CANCELADA".equals(linha.getStatus())) {
                    canceladas++;
                } else if ("AGENDADA".equals(linha.getStatus())) {
                    agendadas++;
                }
            }

            lblResumoRel.setText("Partidas de " + c.getNome()
                    + "  |  Total: " + lista.size()
                    + "  |  Encerradas: " + encerradas
                    + "  |  Agendadas: " + agendadas
                    + "  |  Canceladas: " + canceladas);
            abasRelatorio.setSelectedIndex(1);
            if (validarRelatorio != null) {
                validarRelatorio.run();
            }
        } catch (Exception ex) {
            mostrarErro(ex);
        }
    }

    // =========================================================================
    // SINCRONIZACAO DAS TABELAS, COMBOBOXES E CACHE
    // =========================================================================
    private void recarregarTudo() {
        try {
            // Guarda a selecao do relatorio antes de recriar os combos.
            Campeonato campSelecionado = (Campeonato) cbCampRelatorio.getSelectedItem();

            // Recria a tabela e os combos de campeonato a partir do banco.
            // As consultas de cadastro usam ORDER BY id, entao a ordem e
            // previsivel.
            ArrayList<Campeonato> camps = daoCampeonato.pesquisarTudo();
            DefaultTableModel mCamp = (DefaultTableModel) tblCampeonato.getModel();
            mCamp.setRowCount(0);
            cbCampClass.removeAllItems();
            cbCampPartida.removeAllItems();
            cbCampRelatorio.removeAllItems();
            for (Campeonato c : camps) {
                mCamp.addRow(new Object[]{
                    c.getIdCampeonato(), c.getNome(), c.getData_inicio(),
                    c.getData_fim(), c.getStatus()
                });
                cbCampClass.addItem(c);
                cbCampPartida.addItem(c);
                cbCampRelatorio.addItem(c);
            }
            if (campSelecionado != null) {
                selecionarComboCampeonato(cbCampRelatorio, campSelecionado.getIdCampeonato());
            }

            ArrayList<Equipe> equipes = daoEquipe.pesquisarTudo();
            DefaultTableModel mEq = (DefaultTableModel) tblEquipe.getModel();
            mEq.setRowCount(0);
            cbEquipeInsc.removeAllItems();
            cbMandante.removeAllItems();
            cbVisitante.removeAllItems();
            cbEquipeJogador.removeAllItems();
            for (Equipe eq : equipes) {
                mEq.addRow(new Object[]{eq.getIdEquipe(), eq.getNome(), eq.getCidade()});
                cbEquipeInsc.addItem(eq);
                cbMandante.addItem(eq);
                cbVisitante.addItem(eq);
                cbEquipeJogador.addItem(eq);
            }

            // Jogador tem FK para Equipe; por isso carregamos as equipes
            // primeiro e so depois os jogadores.
            cacheJogadores = daoJogador.pesquisarTudo();
            DefaultTableModel mJog = (DefaultTableModel) tblJogador.getModel();
            mJog.setRowCount(0);
            for (Jogador j : cacheJogadores) {
                mJog.addRow(new Object[]{
                    j.getIdJogador(), j.getNome(), j.getPosicao(), j.getNumeroCamisa(),
                    j.getDataNascimento() == null ? "" : j.getDataNascimento(),
                    j.getNomeEquipe()
                });
            }

            ArrayList<Estadio> estadios = daoEstadio.pesquisarTudo();
            DefaultTableModel mEst = (DefaultTableModel) tblEstadio.getModel();
            mEst.setRowCount(0);
            cbEstadioPartida.removeAllItems();
            for (Estadio es : estadios) {
                mEst.addRow(new Object[]{
                    es.getIdEstadio(), es.getNome(), es.getCidade(), es.getCapacidade()
                });
                cbEstadioPartida.addItem(es);
            }

            ArrayList<Arbitro> arbitros = daoArbitro.pesquisarTudo();
            DefaultTableModel mArb = (DefaultTableModel) tblArbitro.getModel();
            mArb.setRowCount(0);
            cbArbitroPartida.removeAllItems();
            for (Arbitro a : arbitros) {
                mArb.addRow(new Object[]{
                    a.getIdArbitro(), a.getNome(), a.getRegistro_federacao(), a.getCategoria()
                });
                cbArbitroPartida.addItem(a);
            }

            carregarTabelaClassificacao();

            // O cache guarda os objetos na MESMA ordem das linhas da JTable.
            // A selecao de uma linha usa esse cache para preencher os campos.
            cachePartidas = daoPartida.pesquisarTudo();
            DefaultTableModel mPart = (DefaultTableModel) tblPartida.getModel();
            mPart.setRowCount(0);
            for (Partida p : cachePartidas) {
                String placar = (p.getGolsMandante() == null)
                        ? "vs"
                        : (p.getGolsMandante() + " x " + p.getGolsVisitante());
                mPart.addRow(new Object[]{
                    p.getIdPartida(), p.getNomeCampeonato(), p.getRodada(), p.getDataPartida(),
                    p.getNomeMandante(), placar, p.getNomeVisitante(),
                    p.getNomeEstadio(), p.getNomeArbitro(), p.getStatus()
                });
            }
        } catch (Exception ex) {
            System.out.println("Aviso ao carregar dados: " + ex.getMessage());
        }
    }

    private void carregarTabelaClassificacao() {
        try {
            Campeonato c = (Campeonato) cbCampClass.getSelectedItem();
            DefaultTableModel mCl = (DefaultTableModel) tblClassificacao.getModel();
            mCl.setRowCount(0);
            if (c != null) {
                // A consulta ja vem ordenada por colocacao/pontos; quem ainda
                // nao jogou tem colocacao NULL e aparece como "-".
                ArrayList<Classificacao> lista = daoClassificacao.listarPorCampeonato(c.getIdCampeonato());
                for (Classificacao cl : lista) {
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
        } finally {
            atualizarBotoesClassificacao();
        }
    }

    // Os combos guardam objetos; procuramos pelo ID dentro deles em vez de
    // usar setSelectedItem(new Objeto(...)), que nao encontraria o item.
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
        // O SIGNAL/erro do MySQL chega como SQLException; em alguns casos a
        // mensagem real fica na causa, por isso testamos getCause().
        String msg = ex.getMessage();
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            msg = ex.getCause().getMessage();
        }
        if (msg == null || msg.trim().isEmpty()) {
            msg = ex.toString();
        }

        // Traduz os erros mais comuns do MySQL para uma mensagem amigavel.
        String detalhe = msg;
        if (msg.contains("uq_jogador_camisa")) {
            msg = "Já existe um jogador com esse número de camisa nessa equipe. Escolha outro número.";
        } else if (msg.contains("chk_jogador_camisa")) {
            msg = "O número da camisa precisa estar entre 1 e 99.";
        } else if (msg.contains("Duplicate entry")) {
            msg = "Já existe um cadastro com esse valor. Confira o nome, o registro ou a combinação de campos únicos.";
        } else if (msg.contains("foreign key constraint fails")) {
            msg = "Não é possível excluir ou alterar este registro porque ele está sendo usado por outro cadastro (por exemplo, uma partida).";
        } else if (msg.contains("Communications link failure")
                || msg.contains("Connection refused")
                || msg.contains("Unable to load authentication plugin")) {
            msg = "Não foi possível conectar ao MySQL. Confira se o servidor está ligado e se o banco 'campeonato' foi criado.";
        }

        if (!msg.equals(detalhe)) {
            msg = msg + "\n\nDetalhe do banco: " + detalhe;
        }
        JOptionPane.showMessageDialog(this,
                "Erro / Regra do Banco:\n" + msg,
                "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
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
            System.out.println("Não foi possível aplicar o Nimbus: " + ex.getMessage());
        }
        java.awt.EventQueue.invokeLater(() -> new TelaPrincipal().setVisible(true));
    }
}
