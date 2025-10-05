package strategy;

public class BuscaSequencial implements BuscaTexto {
    @Override
    public boolean contem(String texto, String padrao) {
        return texto.toLowerCase().contains(padrao.toLowerCase());
    }
}