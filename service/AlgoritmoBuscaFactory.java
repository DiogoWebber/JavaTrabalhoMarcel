package service;

import strategy.BuscaBoyerMoore;
import strategy.BuscaSequencial;
import strategy.BuscaTexto;

public class AlgoritmoBuscaFactory {
    public static BuscaTexto criar(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "boyer-moore" -> new BuscaBoyerMoore();
            case "sequencial" -> new BuscaSequencial();
            default -> throw new IllegalArgumentException("Tipo de busca inválido: " + tipo);
        };
    }
}