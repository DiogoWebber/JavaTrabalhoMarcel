package strategy;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import model.ResultadoBusca;

public class BuscasService {

    public List<ResultadoBusca> buscar(File diretorio, String termo, String metodo) {
        switch (metodo.toLowerCase()) {
            case "boyer-moore" -> {
                return buscaBoyerMoore(diretorio, termo);
            }
            case "sequencial" -> {
                return buscaSequencial(diretorio, termo);
            }
            case "paralelo" -> {
                return buscarParalelo(diretorio, termo);
            }
            case "paralelo otimizado" -> {
                return buscarParaleloOtimizado(diretorio, termo);
            }
            default -> throw new IllegalArgumentException("Tipo de busca inválido: " + metodo);
        }
    }

    // <editor-fold desc="Sequencial">

    private List<ResultadoBusca> buscaSequencial(File diretorio, String termo) {
        List<ResultadoBusca> resultados = new ArrayList<>();

        if (diretorio == null || !diretorio.isDirectory()) {
            System.out.println("Diretório inválido");
            return resultados;
        }

        File[] arquivos = diretorio.listFiles();
        if (arquivos == null) return resultados;

        for (File arquivo : arquivos) {
            if (arquivo.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
                    String linha;
                    int numeroLinha = 0;
                    while ((linha = reader.readLine()) != null) {
                        numeroLinha++;
                        if (linha.contains(termo)) {  // se a linha contém o termo
                            ResultadoBusca resultado = new ResultadoBusca(arquivo.getName(), numeroLinha, linha);
                            resultados.add(resultado);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Erro lendo arquivo: " + arquivo.getName());
                    e.printStackTrace();
                }
            }
        }

        return resultados;
    }
    // </editor-fold>

    // <editor-fold desc="Boyer-Moore">
    private List<ResultadoBusca> buscaBoyerMoore(File diretorio, String termo) {
        List<ResultadoBusca> resultados = new ArrayList<>();
        if (diretorio == null || !diretorio.isDirectory()) return resultados;

        for (File arquivo : diretorio.listFiles()) {
            if (arquivo.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
                    String linha;
                    int numeroLinha = 0;
                    while ((linha = reader.readLine()) != null) {
                        numeroLinha++;
                        if (containsBoyerMoore(linha, termo)) {
                            ResultadoBusca resultado = new ResultadoBusca(arquivo.getName(), numeroLinha, linha);
                            resultados.add(resultado);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return resultados;
    }

    // Implementação do Boyer-Moore simples
    private boolean containsBoyerMoore(String texto, String padrao) {
        int[] badChar = buildBadCharTable(padrao);
        int n = texto.length();
        int m = padrao.length();
        int shift = 0;

        while (shift <= n - m) {
            int j = m - 1;
            while (j >= 0 && padrao.charAt(j) == texto.charAt(shift + j)) {
                j--;
            }
            if (j < 0) return true; // achou o padrão
            else {
                char c = texto.charAt(shift + j);
                int bcShift = badChar[c];
                shift += Math.max(1, j - bcShift);
            }
        }
        return false;
    }

    private int[] buildBadCharTable(String padrao) {
        final int ASCII_SIZE = 256;
        int[] table = new int[ASCII_SIZE];
        for (int i = 0; i < table.length; i++) table[i] = -1;
        for (int i = 0; i < padrao.length(); i++) {
            table[padrao.charAt(i)] = i;
        }
        return table;
    }
    // </editor-fold>

    // <editor-fold desc="Paralelismo">

    public List<ResultadoBusca> buscarParalelo(File diretorio, String termo) {
        List<ResultadoBusca> resultados = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        File[] arquivos = diretorio.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (arquivos != null) {
            for (File arquivo : arquivos) {
                Thread thread = new Thread(() -> {
                    List<ResultadoBusca> resultadosLocais = new ArrayList<>();
                    int numeroLinha = 0;
                    try (Scanner scanner = new Scanner(arquivo)) {
                        while (scanner.hasNextLine()) {
                            String linha = scanner.nextLine();
                            numeroLinha++;
                            if (linha.toLowerCase().contains(termo.toLowerCase())) {
                                resultadosLocais.add(new ResultadoBusca(arquivo.getAbsolutePath(), numeroLinha, linha));
                            }
                        }
                    } catch (FileNotFoundException e) {
                        System.err.println("Erro ao ler o arquivo: " + arquivo.getAbsolutePath());
                    }
                    // Junta os resultados locais na lista compartilhada uma única vez
                    if (!resultadosLocais.isEmpty()) {
                        synchronized (resultados) {
                            resultados.addAll(resultadosLocais);
                        }
                    }
                });
                threads.add(thread);
                thread.start();
            }
        }

        // Espera todas as threads terminarem
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return resultados;
    }

    // </editor-fold>

    public List<ResultadoBusca> buscarParaleloOtimizado(File diretorio, String termo) {
        List<ResultadoBusca> resultados = new ArrayList<>();
        File[] arquivos = diretorio.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (arquivos == null || arquivos.length == 0) return resultados;

        // Pré-processa o termo para evitar lowerCase repetido
        final String termoLower = termo.toLowerCase();

        // Usa um pool fixo menor que o total de arquivos
        int numThreads = Math.min(Runtime.getRuntime().availableProcessors() * 2, arquivos.length);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<List<ResultadoBusca>>> futures = new ArrayList<>();

        for (File arquivo : arquivos) {
            futures.add(executor.submit(() -> {
                List<ResultadoBusca> resultadosLocais = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
                    String linha;
                    int numeroLinha = 0;
                    while ((linha = reader.readLine()) != null) {
                        numeroLinha++;
                        // Normaliza linha uma vez
                        if (linha.toLowerCase().contains(termoLower)) {
                            resultadosLocais.add(new ResultadoBusca(arquivo.getName(), numeroLinha, linha));
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler: " + arquivo.getName());
                }
                return resultadosLocais;
            }));
        }

        // Coleta os resultados de todas as threads
        for (Future<List<ResultadoBusca>> future : futures) {
            try {
                resultados.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        return resultados;
    }
}
