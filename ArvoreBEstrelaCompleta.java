// ArvoreBEstrelaCompleta.java
// Implementa um Sistema de Indexação de Dados para um Banco de Dados de Produtos
// com ID, nome, categoria, utilizando Árvore B* de Ordem 3.

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// --- Classe ItemProduto (Modelo de Dados) ---
// Representa um produto com ID, nome e categoria
// (Replicado aqui para que o arquivo seja autocontido)
class ItemProduto {
    private int idItem;
    private String nomeItem;
    private String categoriaItem;

    public ItemProduto(int idItem, String nomeItem, String categoriaItem) {
        this.idItem = idItem;
        this.nomeItem = nomeItem;
        this.categoriaItem = categoriaItem;
    }

    public int obterId() {
        return idItem;
    }

    public String obterNome() {
        return nomeItem;
    }

    public String obterCategoria() {
        return categoriaItem;
    }

    @Override
    public String toString() {
        return "ID: " + idItem + ", Nome: " + nomeItem + ", Categoria: " + categoriaItem;
    }

    // Formata a informação do produto para ser armazenada como valor na árvore
    public String obterInfoParaArvore() {
        return nomeItem + ", " + categoriaItem;
    }
}

// --- CLASSE DE NÓ PARA ÁRVORE B* ---

// Um nó da árvore B*, que pode ser um nó folha ou um nó interno
class NoBEstrela {
    protected List<Integer> chaves;       // Identificadores dos itens armazenados no nó
    protected List<String> valores;      // Informações dos itens associadas às chaves
    protected List<NoBEstrela> descendentes; // Lista de nós filhos (nulo para nós folha)
    protected boolean ehFolha;           // Indica se o nó é uma folha (true) ou um nó interno (false)
    protected NoBEstrela pai;         // Referência para o nó pai
    protected int maxChaves;              // Número máximo de chaves que um nó pode conter
    protected int minChaves;              // Número mínimo de chaves que um nó deve conter (2/3 da capacidade)

    public NoBEstrela(int maxChaves, boolean ehFolha) {
        this.chaves = new ArrayList<>();
        this.valores = new ArrayList<>();
        this.descendentes = ehFolha ? null : new ArrayList<>();
        this.ehFolha = ehFolha;
        this.maxChaves = maxChaves;
        this.minChaves = (int) Math.ceil((maxChaves * 2.0) / 3.0); // Cálculo de 2/3 da capacidade
        this.pai = null;
    }

    public boolean estaCheio() {
        return chaves.size() >= maxChaves;
    }

    public boolean temMinimoDeChaves() {
        return chaves.size() >= minChaves;
    }

    public boolean podeEmprestarChave() {
        return chaves.size() > minChaves;
    }

    public void inserirChaveValor(int chave, String valor) {
        int posInsercao = 0;
        while (posInsercao < chaves.size() && chaves.get(posInsercao) < chave) {
            posInsercao++;
        }
        chaves.add(posInsercao, chave);
        valores.add(posInsercao, valor);
    }

    public String buscarValor(int chave) {
        int indice = chaves.indexOf(chave);
        return indice != -1 ? valores.get(indice) : null;
    }

    public boolean removerChaveValor(int chave) {
        int indice = chaves.indexOf(chave);
        if (indice != -1) {
            chaves.remove(indice);
            valores.remove(indice);
            return true;
        }
        return false;
    }

    public int encontrarIndiceFilho(int chave) {
        int i = 0;
        while (i < chaves.size() && chave > chaves.get(i)) {
            i++;
        }
        return i;
    }

    public List<Integer> obterChaves() { return chaves; }
    public List<String> obterValores() { return valores; }
    public List<NoBEstrela> obterDescendentes() { return descendentes; }
    public boolean ehFolha() { return ehFolha; }
    public NoBEstrela obterPai() { return pai; }
    public void definirPai(NoBEstrela pai) { this.pai = pai; }
}

// --- CLASSE ARVORE B* (Principal da Implementação) ---
public class ArvoreBEstrelaCompleta {
    private NoBEstrela raiz;
    private int maxChaves;

    private static final int ORDEM_ARVORE = 3; // Ordem para a árvore B*
    private static final String NOME_ARQUIVO_DADOS = "produtos_corrigido.txt"; // Arquivo de dados

    public ArvoreBEstrelaCompleta() {
        this.maxChaves = ORDEM_ARVORE - 1; // Para ordem 3, maxChaves = 2
        this.raiz = new NoBEstrela(maxChaves, true);
    }

    private NoBEstrela encontrarNoAlvo(int chave) {
        NoBEstrela atual = raiz;
        while (!atual.ehFolha()) {
            int indiceFilho = atual.encontrarIndiceFilho(chave);
            atual = atual.obterDescendentes().get(indiceFilho);
        }
        return atual;
    }

    private NoBEstrela encontrarNoComChave(NoBEstrela noAtual, int chave) {
        if (noAtual.obterChaves().contains(chave)) {
            return noAtual;
        }
        if (noAtual.ehFolha()) {
            return null;
        }
        int indiceFilho = noAtual.encontrarIndiceFilho(chave);
        if (indiceFilho >= noAtual.obterDescendentes().size()) {
             return null;
        }
        return encontrarNoComChave(noAtual.obterDescendentes().get(indiceFilho), chave);
    }

    public void inserirItem(int chave, String valor) {
        NoBEstrela noAlvo = encontrarNoAlvo(chave);

        if (noAlvo.buscarValor(chave) != null) {
            int indice = noAlvo.obterChaves().indexOf(chave);
            noAlvo.obterValores().set(indice, valor);
            return;
        }

        noAlvo.inserirChaveValor(chave, valor);

        if (noAlvo.estaCheio()) {
            lidarComTransbordamento(noAlvo);
        }
    }

    private void lidarComTransbordamento(NoBEstrela no) {
        if (no == raiz) {
            dividirRaiz();
            return;
        }
        if (tentarRedistribuir(no)) {
            return;
        }
        dividirNo(no);
    }

    private boolean tentarRedistribuir(NoBEstrela no) {
        NoBEstrela paiNo = no.obterPai();
        if (paiNo == null) {
            return false;
        }

        int indiceNo = paiNo.obterDescendentes().indexOf(no);

        if (indiceNo > 0) {
            NoBEstrela irmaoEsquerda = paiNo.obterDescendentes().get(indiceNo - 1);
            if (irmaoEsquerda.podeEmprestarChave()) {
                redistribuirComIrmaoEsquerda(no, irmaoEsquerda, indiceNo - 1);
                return true;
            }
        }

        if (indiceNo < paiNo.obterDescendentes().size() - 1) {
            NoBEstrela irmaoDireita = paiNo.obterDescendentes().get(indiceNo + 1);
            if (irmaoDireita.podeEmprestarChave()) {
                redistribuirComIrmaoDireita(no, irmaoDireita, indiceNo);
                return true;
            }
        }
        return false;
    }

    private void redistribuirComIrmaoEsquerda(NoBEstrela no, NoBEstrela irmaoEsquerda, int indiceChavePai) {
        NoBEstrela paiNo = no.obterPai();
        int chaveDoPai = paiNo.obterChaves().remove(indiceChavePai);
        String valorDoPai = paiNo.obterValores().remove(indiceChavePai);
        no.inserirChaveValor(chaveDoPai, valorDoPai);

        int ultimoIndiceIrmao = irmaoEsquerda.obterChaves().size() - 1;
        int chaveMovidaIrmao = irmaoEsquerda.obterChaves().remove(ultimoIndiceIrmao);
        String valorMovidoIrmao = irmaoEsquerda.obterValores().remove(ultimoIndiceIrmao);
        paiNo.inserirChaveValor(chaveMovidaIrmao, valorMovidoIrmao);

        if (!no.ehFolha()) {
            NoBEstrela filhoMovido = irmaoEsquerda.obterDescendentes().remove(irmaoEsquerda.obterDescendentes().size() - 1);
            no.obterDescendentes().add(0, filhoMovido);
            filhoMovido.definirPai(no);
        }
    }

    private void redistribuirComIrmaoDireita(NoBEstrela no, NoBEstrela irmaoDireita, int indiceChavePai) {
        NoBEstrela paiNo = no.obterPai();
        int chaveDoPai = paiNo.obterChaves().remove(indiceChavePai);
        String valorDoPai = paiNo.obterValores().remove(indiceChavePai);
        no.inserirChaveValor(chaveDoPai, valorDoPai);

        int chaveMovidaIrmao = irmaoDireita.obterChaves().remove(0);
        String valorMovidoIrmao = irmaoDireita.obterValores().remove(0);
        paiNo.inserirChaveValor(chaveMovidaIrmao, valorMovidoIrmao);

        if (!no.ehFolha()) {
            NoBEstrela filhoMovido = irmaoDireita.obterDescendentes().remove(0);
            no.obterDescendentes().add(filhoMovido);
            filhoMovido.definirPai(no);
        }
    }

    private NoBEstrela dividirNoInterno(NoBEstrela no) {
        NoBEstrela novoNo = new NoBEstrela(maxChaves, no.ehFolha());

        int pontoMedio = no.obterChaves().size() / 2;
        for (int i = pontoMedio; i < no.obterChaves().size(); i++) {
            novoNo.obterChaves().add(no.obterChaves().get(i));
            novoNo.obterValores().add(no.obterValores().get(i));
        }
        no.obterChaves().subList(pontoMedio, no.obterChaves().size()).clear();
        no.obterValores().subList(pontoMedio, no.obterValores().size()).clear();

        if (!no.ehFolha()) {
            for (int i = pontoMedio; i < no.obterDescendentes().size(); i++) {
                NoBEstrela descendente = no.obterDescendentes().get(i);
                novoNo.obterDescendentes().add(descendente);
                descendente.definirPai(novoNo);
            }
            no.obterDescendentes().subList(pontoMedio, no.obterDescendentes().size()).clear();
        }
        return novoNo;
    }

    private void dividirNo(NoBEstrela no) {
        NoBEstrela novoNo = dividirNoInterno(no);
        NoBEstrela paiNo = no.obterPai();

        if (paiNo == null) {
            NoBEstrela novaRaiz = new NoBEstrela(maxChaves, false);
            int chavePromovida = novoNo.obterChaves().remove(0);
            String valorPromovido = novoNo.obterValores().remove(0);
            novaRaiz.inserirChaveValor(chavePromovida, valorPromovido);
            novaRaiz.obterDescendentes().add(no);
            novaRaiz.obterDescendentes().add(novoNo);
            no.definirPai(novaRaiz);
            novoNo.definirPai(novaRaiz);
            this.raiz = novaRaiz;
            return;
        }

        int pontoMedioOriginal = no.obterChaves().size() / 2;
        int chavePromovida = no.obterChaves().get(pontoMedioOriginal);
        String valorPromovido = no.obterValores().get(pontoMedioOriginal);

        no.obterChaves().remove(pontoMedioOriginal);
        no.obterValores().remove(pontoMedioOriginal);

        paiNo.inserirChaveValor(chavePromovida, valorPromovido);

        int indiceInsercao = paiNo.obterDescendentes().indexOf(no) + 1;
        paiNo.obterDescendentes().add(indiceInsercao, novoNo);
        novoNo.definirPai(paiNo);

        if (paiNo.estaCheio()) {
            lidarComTransbordamento(paiNo);
        }
    }

    private void dividirRaiz() {
        NoBEstrela novaRaiz = new NoBEstrela(maxChaves, false);
        NoBEstrela novoNo = dividirNoInterno(raiz);

        int pontoMedioOriginal = raiz.obterChaves().size() / 2;
        int chavePromovida = raiz.obterChaves().remove(pontoMedioOriginal);
        String valorPromovido = raiz.obterValores().remove(pontoMedioOriginal);

        novaRaiz.inserirChaveValor(chavePromovida, valorPromovido);
        novaRaiz.obterDescendentes().add(raiz);
        novaRaiz.obterDescendentes().add(novoNo);
        raiz.definirPai(novaRaiz);
        novoNo.definirPai(novaRaiz);
        this.raiz = novaRaiz;
    }

    public String buscarItem(int chave) {
        return buscarEmNo(raiz, chave);
    }

    private String buscarEmNo(NoBEstrela no, int chave) {
        String valor = no.buscarValor(chave);
        if (valor != null) {
            return valor;
        }
        if (no.ehFolha()) {
            return null;
        }
        int indiceFilho = no.encontrarIndiceFilho(chave);
        return buscarEmNo(no.obterDescendentes().get(indiceFilho), chave);
    }

    public boolean removerItem(int chave) {
        NoBEstrela noAlvo = encontrarNoComChave(raiz, chave);
        if (noAlvo == null) {
            return false;
        }

        if (noAlvo.ehFolha()) {
            noAlvo.removerChaveValor(chave);
        } else {
            removerDeNoInterno(noAlvo, chave);
        }

        if (!noAlvo.temMinimoDeChaves() && noAlvo != raiz) {
            lidarComSubutilizacao(noAlvo);
        }

        if (raiz.obterChaves().isEmpty() && !raiz.ehFolha() && raiz.obterDescendentes().size() == 1) {
            raiz = raiz.obterDescendentes().get(0);
            raiz.definirPai(null);
        } else if (raiz.obterChaves().isEmpty() && raiz.ehFolha()) {
             this.raiz = null;
        }
        return true;
    }

    private void removerDeNoInterno(NoBEstrela no, int chave) {
        int indiceChave = no.obterChaves().indexOf(chave);
        if (indiceChave == -1) return;

        NoBEstrela noSucessor = no.obterDescendentes().get(indiceChave + 1);
        while (!noSucessor.ehFolha()) {
            noSucessor = noSucessor.obterDescendentes().get(0);
        }

        int chaveSucessora = noSucessor.obterChaves().get(0);
        String valorSucessor = noSucessor.obterValores().get(0);
        no.obterChaves().set(indiceChave, chaveSucessora);
        no.obterValores().set(indiceChave, valorSucessor);

        noSucessor.removerChaveValor(chaveSucessora);

        if (!noSucessor.temMinimoDeChaves() && noSucessor != raiz) {
            lidarComSubutilizacao(noSucessor);
        }
    }

    private void lidarComSubutilizacao(NoBEstrela no) {
        NoBEstrela paiNo = no.obterPai();
        if (paiNo == null) return;

        int indiceNo = paiNo.obterDescendentes().indexOf(no);

        if (indiceNo > 0) {
            NoBEstrela irmaoEsquerda = paiNo.obterDescendentes().get(indiceNo - 1);
            if (irmaoEsquerda.podeEmprestarChave()) {
                emprestarDeIrmaoEsquerda(no, irmaoEsquerda, indiceNo - 1);
                return;
            }
        }

        if (indiceNo < paiNo.obterDescendentes().size() - 1) {
            NoBEstrela irmaoDireita = paiNo.obterDescendentes().get(indiceNo + 1);
            if (irmaoDireita.podeEmprestarChave()) {
                emprestarDeIrmaoDireita(no, irmaoDireita, indiceNo);
                return;
            }
        }

        if (indiceNo > 0) {
            fusaoComIrmaoEsquerda(no, indiceNo - 1);
        } else if (indiceNo < paiNo.obterDescendentes().size() - 1) {
            fusaoComIrmaoDireita(no, indiceNo);
        }
    }

    private void emprestarDeIrmaoEsquerda(NoBEstrela no, NoBEstrela irmaoEsquerda, int indiceChavePai) {
        NoBEstrela paiNo = no.obterPai();

        int chaveDoPai = paiNo.obterChaves().remove(indiceChavePai);
        String valorDoPai = paiNo.obterValores().remove(indiceChavePai);
        no.inserirChaveValor(chaveDoPai, valorDoPai);

        int ultimoIndiceIrmao = irmaoEsquerda.obterChaves().size() - 1;
        int chaveDoIrmao = irmaoEsquerda.obterChaves().remove(ultimoIndiceIrmao);
        String valorDoIrmao = irmaoEsquerda.obterValores().remove(ultimoIndiceIrmao);
        paiNo.inserirChaveValor(chaveDoIrmao, valorDoIrmao);

        if (!no.ehFolha()) {
            NoBEstrela filhoMovido = irmaoEsquerda.obterDescendentes().remove(irmaoEsquerda.obterDescendentes().size() - 1);
            no.obterDescendentes().add(0, filhoMovido);
            filhoMovido.definirPai(no);
        }
    }

    private void emprestarDeIrmaoDireita(NoBEstrela no, NoBEstrela irmaoDireita, int indiceChavePai) {
        NoBEstrela paiNo = no.obterPai();

        int chaveDoPai = paiNo.obterChaves().remove(indiceChavePai);
        String valorDoPai = paiNo.obterValores().remove(indiceChavePai);
        no.inserirChaveValor(chaveDoPai, valorDoPai);

        int chaveMovidaIrmao = irmaoDireita.obterChaves().remove(0);
        String valorMovidoIrmao = irmaoDireita.obterValores().remove(0);
        paiNo.inserirChaveValor(chaveMovidaIrmao, valorMovidoIrmao);

        if (!no.ehFolha()) {
            NoBEstrela filhoMovido = irmaoDireita.obterDescendentes().remove(0);
            no.obterDescendentes().add(filhoMovido);
            filhoMovido.definirPai(no);
        }
    }

    private void fusaoComIrmaoEsquerda(NoBEstrela no, int indiceChavePai) {
        NoBEstrela paiNo = no.obterPai();
        NoBEstrela irmaoEsquerda = paiNo.obterDescendentes().get(indiceChavePai);

        int chaveDoPai = paiNo.obterChaves().remove(indiceChavePai);
        String valorDoPai = paiNo.obterValores().remove(indiceChavePai);
        irmaoEsquerda.inserirChaveValor(chaveDoPai, valorDoPai);

        for (int i = 0; i < no.obterChaves().size(); i++) {
            irmaoEsquerda.inserirChaveValor(no.obterChaves().get(i), no.obterValores().get(i));
        }

        if (!no.ehFolha()) {
            for (NoBEstrela descendente : no.obterDescendentes()) {
                irmaoEsquerda.obterDescendentes().add(descendente);
                descendente.definirPai(irmaoEsquerda);
            }
        }

        paiNo.obterDescendentes().remove(no);

        if (paiNo != raiz && !paiNo.temMinimoDeChaves()) {
            lidarComSubutilizacao(paiNo);
        } else if (paiNo == raiz && paiNo.obterChaves().isEmpty()) {
            raiz = irmaoEsquerda;
            irmaoEsquerda.definirPai(null);
        }
    }

    private void fusaoComIrmaoDireita(NoBEstrela no, int indiceChavePai) {
        NoBEstrela paiNo = no.obterPai();
        NoBEstrela irmaoDireita = paiNo.obterDescendentes().get(indiceChavePai + 1);

        int chaveDoPai = paiNo.obterChaves().remove(indiceChavePai);
        String valorDoPai = paiNo.obterValores().remove(indiceChavePai);
        no.inserirChaveValor(chaveDoPai, valorDoPai);

        for (int i = 0; i < irmaoDireita.obterChaves().size(); i++) {
            no.inserirChaveValor(irmaoDireita.obterChaves().get(i), irmaoDireita.obterValores().get(i));
        }

        if (!no.ehFolha()) {
            for (NoBEstrela descendente : irmaoDireita.obterDescendentes()) {
                no.obterDescendentes().add(descendente);
                descendente.definirPai(no);
            }
        }

        paiNo.obterDescendentes().remove(irmaoDireita);

        if (paiNo != raiz && !paiNo.temMinimoDeChaves()) {
            lidarComSubutilizacao(paiNo);
        } else if (paiNo == raiz && paiNo.obterChaves().isEmpty()) {
            raiz = no;
            no.definirPai(null);
        }
    }

    public List<String> buscarIntervalo(int chaveInicio, int chaveFim) {
        List<String> resultados = new ArrayList<>();
        if (raiz == null) return resultados;

        percorrerEmOrdemParaIntervalo(raiz, chaveInicio, chaveFim, resultados);
        return resultados;
    }

    private void percorrerEmOrdemParaIntervalo(NoBEstrela no, int chaveInicio, int chaveFim, List<String> resultados) {
        if (no == null) return;

        if (no.ehFolha()) {
            for (int i = 0; i < no.obterChaves().size(); i++) {
                int chaveAtual = no.obterChaves().get(i);
                if (chaveAtual >= chaveInicio && chaveAtual <= chaveFim) {
                    resultados.add(no.obterValores().get(i));
                } else if (chaveAtual > chaveFim) {
                    return;
                }
            }
        } else {
            for (int i = 0; i < no.obterDescendentes().size(); i++) {
                percorrerEmOrdemParaIntervalo(no.obterDescendentes().get(i), chaveInicio, chaveFim, resultados);

                if (i < no.obterChaves().size()) {
                    int chaveAtual = no.obterChaves().get(i);
                    if (chaveAtual >= chaveInicio && chaveAtual <= chaveFim) {
                        resultados.add(no.obterValores().get(i));
                    } else if (chaveAtual > chaveFim) {
                        return;
                    }
                }
            }
        }
    }

    public void imprimirEmOrdem() {
        System.out.print("Itens em ordem (B*): ");
        imprimirChavesNo(raiz);
        System.out.println();
    }

    private void imprimirChavesNo(NoBEstrela no) {
        if (no == null) return;

        if (no.ehFolha()) {
            for (int i = 0; i < no.obterChaves().size(); i++) {
                System.out.print(no.obterChaves().get(i) + ":" + no.obterValores().get(i) + " ");
            }
        } else {
            for (int i = 0; i < no.obterDescendentes().size(); i++) {
                imprimirChavesNo(no.obterDescendentes().get(i));
                if (i < no.obterChaves().size()) {
                    System.out.print(no.obterChaves().get(i) + ":" + no.obterValores().get(i) + " ");
                }
            }
        }
    }

    // --- LÓGICA PRINCIPAL DE EXECUÇÃO PARA ÁRVORE B* ---
    private static final int ORDEM_ARVORE_BESTRELA = 3;
    private static final String ARQUIVO_DADOS = "produtos_corrigido.txt";

    public static void main(String[] args) {
        ArvoreBEstrela arvoreBEstrela = new ArvoreBEstrela(ORDEM_ARVORE_BESTRELA);
        List<ItemProduto> itensParaProcessar = carregarItensDoArquivo(ARQUIVO_DADOS);

        System.out.println("--- Teste de Indexação com Árvore B* (Ordem " + ORDEM_ARVORE_BESTRELA + ") ---");
        System.out.println("Total de produtos no arquivo: " + itensParaProcessar.size());

        // Medir tempo de inserção
        long tempoInicioInsercao = System.nanoTime();
        for (ItemProduto item : itensParaProcessar) {
            arvoreBEstrela.inserirItem(item.obterId(), item.obterInfoParaArvore());
        }
        long tempoFimInsercao = System.nanoTime();
        long duracaoInsercao = (tempoFimInsercao - tempoInicioInsercao) / 1_000_000;
        System.out.println("Tempo de inserção de " + itensParaProcessar.size() + " produtos: " + duracaoInsercao + " ms");

        // Testar remoção de 10 produtos aleatórios
        System.out.println("\n--- Removendo 10 produtos aleatórios (IDs entre 1000 e 2000) ---");
        Random geradorAleatorio = new Random();
        List<Integer> chavesParaRemover = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            chavesParaRemover.add(geradorAleatorio.nextInt(1001) + 1000); // IDs de 1000 a 2000
        }

        long tempoInicioRemocao = System.nanoTime();
        for (int chave : chavesParaRemover) {
            String infoProduto = arvoreBEstrela.buscarItem(chave); // Busca antes para relatar se existe
            boolean removido = false;
            if (infoProduto != null) {
                removido = arvoreBEstrela.removerItem(chave);
                System.out.println("Tentando remover ID " + chave + ": " + (removido ? "Removido: " + infoProduto : "Falha ao remover produto existente."));
            } else {
                System.out.println("Tentando remover ID " + chave + ": Produto não encontrado.");
            }
        }
        long tempoFimRemocao = System.nanoTime();
        long duracaoRemocao = (tempoFimRemocao - tempoInicioRemocao) / 1_000_000;
        System.out.println("Tempo de remoção de 10 produtos: " + duracaoRemocao + " ms");

        System.out.println("\n--- Teste da Árvore B* Concluído ---");
    }

    // Método auxiliar para carregar dados do arquivo
    private static List<ItemProduto> carregarItensDoArquivo(String nomeArquivo) {
        List<ItemProduto> itens = new ArrayList<>();
        String linha;
        try (BufferedReader br = new BufferedReader(new FileReader(nomeArquivo))) {
            while ((linha = br.readLine()) != null) {
                String[] dados = linha.split(",");
                if (dados.length == 3) {
                    int id = Integer.parseInt(dados[0].trim());
                    String nome = dados[1].trim();
                    String categoria = dados[2].trim();
                    itens.add(new ItemProduto(id, nome, categoria));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo de dados: " + e.getMessage());
            e.printStackTrace(); // Para depuração
        }
        return itens;
    }
}