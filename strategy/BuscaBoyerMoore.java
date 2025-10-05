package strategy;

public class BuscaBoyerMoore implements BuscaTexto {

    @Override
    public boolean contem(String texto, String padrao) {
        return boyerMooreSearch(texto.toLowerCase(), padrao.toLowerCase());
    }

    private boolean boyerMooreSearch(String texto, String padrao) {
        int[] badCharTable = buildBadCharTable(padrao);
        int n = texto.length();
        int m = padrao.length();
        int shift = 0;

        while (shift <= (n - m)) {
            int j = m - 1;
            while (j >= 0 && padrao.charAt(j) == texto.charAt(shift + j)) {
                j--;
            }
            if (j < 0) {
                return true;
            } else {
                char badChar = texto.charAt(shift + j);
                int deslocamento = Math.max(1, j - badCharTable[badChar]);
                shift += deslocamento;
            }
        }
        return false;
    }

    private int[] buildBadCharTable(String padrao) {
        final int TAM_ALFABETO = 256;
        int[] tabela = new int[TAM_ALFABETO];
        for (int i = 0; i < TAM_ALFABETO; i++) tabela[i] = -1;
        for (int i = 0; i < padrao.length(); i++) tabela[padrao.charAt(i)] = i;
        return tabela;
    }
}