package app;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import model.ResultadoBusca;
import service.AlgoritmoBuscaFactory;
import service.BuscaArquivoService;
import strategy.BuscaTexto;

public class BuscaArquivosApp extends JFrame {

    private JTextField txtDiretorio;
    private JTextField txtPalavra;
    private JTextArea txtResultado;
    private JComboBox<String> cmbMetodo;
    private Set<String> nomesDisponiveis;

    private JList<String> sugestoesList;
    private JScrollPane scrollSugestoes;
    private JPopupMenu popupSugestoes;
    //grafico
    private DefaultCategoryDataset dataset;
    private ChartPanel chartPanel;

    public BuscaArquivosApp() {
        super("Busca em Arquivos de Texto 🔍");
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        // Painel superior
        JPanel painelTopo = new JPanel(new GridLayout(3, 2, 5, 5));
        painelTopo.add(new JLabel("Diretório:"));
        txtDiretorio = new JTextField("txt");
        painelTopo.add(txtDiretorio);

        painelTopo.add(new JLabel("Palavra a buscar:"));
        txtPalavra = new JTextField();
        painelTopo.add(txtPalavra);

        painelTopo.add(new JLabel("Método de busca:"));
        cmbMetodo = new JComboBox<>(new String[]{"Sequencial", "Boyer-Moore"});
        painelTopo.add(cmbMetodo);

        add(painelTopo, BorderLayout.NORTH);

        txtResultado = new JTextArea();
        txtResultado.setEditable(false);
        add(new JScrollPane(txtResultado), BorderLayout.CENTER);
        
        dataset = new DefaultCategoryDataset();
        JFreeChart chart = ChartFactory.createBarChart(
                "Tempo da Busca / Resultados",
                "Palavra",
                "Valor",
                dataset
        );
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(600, 200));

        add(chartPanel, BorderLayout.EAST); 

        JButton btnBuscar = new JButton("Buscar");
        add(btnBuscar, BorderLayout.SOUTH);

        inicializarAutocomplete();

        carregarNomes();

        btnBuscar.addActionListener(e -> iniciarBusca());
    }

    private void carregarNomes() {
        File pasta = new File(txtDiretorio.getText());
        if (!pasta.exists() || !pasta.isDirectory()) {
            nomesDisponiveis = new TreeSet<>();
            return;
        }
        BuscaArquivoService serviceTemp = new BuscaArquivoService(new BuscaTexto() {
            @Override
            public boolean contem(String texto, String padrao) { return true; }
        });
        nomesDisponiveis = serviceTemp.carregarNomesDoDiretorio(pasta);
    }

    private void inicializarAutocomplete() {
        sugestoesList = new JList<>();
        sugestoesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        scrollSugestoes = new JScrollPane(sugestoesList);
        scrollSugestoes.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY));

        popupSugestoes = new JPopupMenu();
        popupSugestoes.setLayout(new BorderLayout());
        popupSugestoes.add(scrollSugestoes, BorderLayout.CENTER);

        // Document listener para digitação
        txtPalavra.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { atualizarSugestoes(); }
            public void removeUpdate(DocumentEvent e) { atualizarSugestoes(); }
            public void changedUpdate(DocumentEvent e) { atualizarSugestoes(); }
        });

        // Clique na lista
        sugestoesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    txtPalavra.setText(sugestoesList.getSelectedValue());
                    popupSugestoes.setVisible(false);
                }
            }
        });

        // Teclas do teclado
        txtPalavra.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                if (popupSugestoes.isVisible()) {
                    if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
                        int idx = sugestoesList.getSelectedIndex();
                        if (idx < sugestoesList.getModel().getSize() - 1)
                            sugestoesList.setSelectedIndex(idx + 1);
                    } else if (evt.getKeyCode() == KeyEvent.VK_UP) {
                        int idx = sugestoesList.getSelectedIndex();
                        if (idx > 0) sugestoesList.setSelectedIndex(idx - 1);
                    } else if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                        txtPalavra.setText(sugestoesList.getSelectedValue());
                        popupSugestoes.setVisible(false);
                        evt.consume();
                    }
                }
            }
        });
    }

    private void atualizarSugestoes() {
        String texto = txtPalavra.getText();
        if (texto.isBlank()) {
            popupSugestoes.setVisible(false);
            return;
        }

        // Filtra apenas os nomes que começam com o texto digitado
        List<String> filtrados = nomesDisponiveis.stream()
                .filter(n -> n.toLowerCase().startsWith(texto.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());

        if (filtrados.isEmpty()) {
            popupSugestoes.setVisible(false);
            return;
        }

        sugestoesList.setListData(filtrados.toArray(new String[0]));
        sugestoesList.setSelectedIndex(0);

        popupSugestoes.setPopupSize(txtPalavra.getWidth(), Math.min(150, filtrados.size() * 20));
        popupSugestoes.show(txtPalavra, 0, txtPalavra.getHeight());
    }

    private void iniciarBusca() {
        String caminho = txtDiretorio.getText();
        String palavra = txtPalavra.getText().trim();
        String metodo = (String) cmbMetodo.getSelectedItem();
        
        if (palavra.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe a palavra a ser buscada!");
            return;
        }

        File pasta = new File(caminho);
        if (!pasta.exists() || !pasta.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Diretório inválido!");
            return;
        }

        BuscaTexto algoritmo = AlgoritmoBuscaFactory.criar(metodo);
        BuscaArquivoService service = new BuscaArquivoService(algoritmo);

        txtResultado.setText("Buscando \"" + palavra + "\" com " + metodo + "...\n\n");

        long inicio = System.currentTimeMillis();
        List<ResultadoBusca> resultados = service.buscarEmDiretorio(pasta, palavra);
        long fim = System.currentTimeMillis();

        long tempoBusca = fim - inicio; 
        int quantidadeResultados = resultados.size();

        if (resultados.isEmpty()) {
            txtResultado.append("Nenhum resultado encontrado.\n");
        } else {
            resultados.forEach(r -> txtResultado.append(r + "\n"));
        }

        txtResultado.append("\nBusca finalizada ✅\n");
        txtResultado.append("Tempo da busca: " + tempoBusca + " ms\n");
        txtResultado.append("Resultados encontrados: " + quantidadeResultados + "\n");

        if (dataset != null) {
            dataset.clear();
            dataset.addValue(tempoBusca, "Tempo (ms)", palavra);
            dataset.addValue(quantidadeResultados, "Resultados", palavra);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BuscaArquivosApp().setVisible(true));
    }
}
