package model;

public class ResultadoBusca {
    private final String caminhoArquivo;
    private final int linha;
    private final String conteudo;

    public ResultadoBusca(String caminhoArquivo, int linha, String conteudo) {
        this.caminhoArquivo = caminhoArquivo;
        this.linha = linha;
        this.conteudo = conteudo;
    }

    @Override
    public String toString() {
        return String.format("Encontrado em %s (linha %d): %s", caminhoArquivo, linha, conteudo);
    }
}
