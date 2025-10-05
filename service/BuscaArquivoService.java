package service;

import java.io.*;
import java.util.*;

import model.ResultadoBusca;
import strategy.BuscaTexto;

public class BuscaArquivoService {

    private final BuscaTexto algoritmo;

    public BuscaArquivoService(BuscaTexto algoritmo) {
        this.algoritmo = algoritmo;
    }

    // Método para autocomplete
    public Set<String> carregarNomesDoDiretorio(File diretorio) {
        Set<String> nomes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        carregarRecursivo(diretorio, nomes);
        return nomes;
    }

    private void carregarRecursivo(File dir, Set<String> nomes) {
        File[] arquivos = dir.listFiles();
        if (arquivos == null) return;

        for (File arquivo : arquivos) {
            if (arquivo.isDirectory()) {
                carregarRecursivo(arquivo, nomes);
            } else if (arquivo.getName().endsWith(".txt")) {
                try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        String[] palavras = linha.split("\\s+");
                        for (String palavra : palavras) {
                            if (!palavra.isBlank()) {
                                nomes.add(palavra.trim());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao ler arquivo: " + arquivo.getPath());
                }
            }
        }
    }

    // ==============================
    // Métodos de busca já existentes
    // ==============================
    public List<ResultadoBusca> buscarEmDiretorio(File diretorio, String termo) {
        List<ResultadoBusca> resultados = new ArrayList<>();
        buscarRecursivo(diretorio, termo, resultados);
        return resultados;
    }

    private void buscarRecursivo(File dir, String termo, List<ResultadoBusca> resultados) {
        File[] arquivos = dir.listFiles();
        if (arquivos == null) return;

        for (File arquivo : arquivos) {
            if (arquivo.isDirectory()) {
                buscarRecursivo(arquivo, termo, resultados);
            } else if (arquivo.getName().endsWith(".txt")) {
                lerArquivo(arquivo, termo, resultados);
            }
        }
    }

    private void lerArquivo(File arquivo, String termo, List<ResultadoBusca> resultados) {
        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            int numeroLinha = 1;

            while ((linha = reader.readLine()) != null) {
                if (algoritmo.contem(linha, termo)) {
                    resultados.add(new ResultadoBusca(arquivo.getPath(), numeroLinha, linha.trim()));
                }
                numeroLinha++;
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler arquivo: " + arquivo.getPath());
        }
    }
}
