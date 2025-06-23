// ArvoreBPlusCompleta.java
// Implementa um Sistema de Indexação de Dados para um Banco de Dados de Produtos
// com ID, nome, categoria, utilizando Árvore B+ de Ordem 3.

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// --- Classe ItemProduto (Modelo de Dados) ---
// Representa um produto com ID, nome e categoria
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

// --- CLASSES DE NÓS PARA ÁRVORE B+ ---

// Nó base para a implementação da Árvore B+
class NoBase {
    protected List<Integer> chaves;
    protected boolean ehFolha;
    protected NoBase pai;
    protected int maxChaves;

    public NoBase(int maxChaves, boolean ehFolha) {
        this.chaves = new ArrayList<>();
        this.ehFolha = ehFolha;
        this.maxChaves = maxChaves;
        this.pai = null;
    }

    public boolean estaCheio() {
        return chaves.size() >= maxChaves;
    }

    public boolean temMinimoDeChaves() {
        // Mínimo para B+ é (maxChaves + 1) / 2
        return chaves.size() >= (maxChaves + 1) / 2;
    }

    public List<Integer> obterChaves() {
        return chaves;
    }

    public boolean ehFolha() {
        return ehFolha;
    }

    public NoBase obterPai() {
        return pai;
    }

    public void definirPai(NoBase pai) {
        this.pai = pai;
    }
}

// Nó interno para a implementação da Árvore B+
class NoInterno extends NoBase {
    private List<NoBase> descendentes;

    public NoInterno(int maxChaves) {
        super(maxChaves, false);
        this.descendentes = new ArrayList<>();
    }

    public void adicionarDescendente(NoBase descendente) {
        descendentes.add(descendente);
        descendente.definirPai(this);
    }

    public void inserirDescendente(int indice, NoBase descendente) {
        descendentes.add(indice, descendente);
        descendente.definirPai(this);
    }

    public void removerDescendente(NoBase descendente) {
        descendentes.remove(descendente);
    }

    public NoBase encontrarDescendente(int chave) {
        int i = 0;
        while (i < chaves.size() && chave >= chaves.get(i)) {
            i++;
        }
        return descendentes.get(i);
    }

    public List<NoBase> obterDescendentes() {
        return descendentes;
    }

    public NoBase obterDescendente(int indice) {
        return descendentes.get(indice);
    }
}

// Nó folha para a implementação da Árvore B+
class NoFolha extends NoBase {
    private List<String> valores;
    private NoFolha proximo;
    private NoFolha anterior;

    public NoFolha(int maxChaves) {
        super(maxChaves, true);
        this.valores = new ArrayList<>();
        this.proximo = null;
        this.anterior = null;
    }

    public void inserir(int chave, String valor) {
        int posInsercao = 0;
        while (posInsercao < chaves.size() && chaves.get(posInsercao) < chave) {
            posInsercao++;
        }
        chaves.add(posInsercao, chave);
        valores.add(posInsercao, valor);
    }

    public boolean remover(int chave) {
        int indice = chaves.indexOf(chave);
        if (indice != -1) {
            chaves.remove(indice);
            valores.remove(indice);
            return true;
        }
        return false;
    }

    public String buscar(int chave) {
        int indice = chaves.indexOf(chave);
        return indice != -1 ? valores.get(indice) : null;
    }

    public NoFolha dividir() {
        int pontoMedio = chaves.size() / 2;
        NoFolha novaFolha = new NoFolha(maxChaves);
        for (int i = pontoMedio; i < chaves.size(); i++) {
            novaFolha.chaves.add(chaves.get(i));
            novaFolha.valores.add(valores.get(i));
        }
        chaves.subList(pontoMedio, chaves.size()).clear();
        valores.subList(pontoMedio, valores.size()).clear();
        novaFolha.proximo = this.proximo;
        novaFolha.anterior = this;
        if (this.proximo != null) {
            this.proximo.anterior = novaFolha;
        }
        this.proximo = novaFolha;
        return novaFolha;
    }

    public List<String> obterValores() {
        return valores;
    }

    public NoFolha obterProximo() {
        return proximo;
    }

    public NoFolha obterAnterior() {
        return anterior;
    }

    public void definirProximo(NoFolha proximo) {
        this.proximo = proximo;
    }

    public void definirAnterior(NoFolha anterior) {
        this.anterior = anterior;
    }
}

// --- CLASSE ARVORE B+ (Principal da Implementação) ---
class ArvoreBPlus { // Não é public para permitir a classe externa ArvoreBPlusCompleta ser public
    private NoBase raiz;
    private int maxChaves; // Número máximo de chaves por nó (m-1 para ordem m)
    private NoFolha primeiraFolha; // Primeira folha (para percorrer sequencialmente)

    public ArvoreBPlus(int ordem) { // Ordem 'm' da árvore
        this.maxChaves = ordem - 1; // Para ordem 3, maxChaves = 2
        this.raiz = new NoFolha(maxChaves);
        this.primeiraFolha = (NoFolha) raiz;
    }

    public void inserirItem(int chave, String valor) {
        NoFolha folha = encontrarNoFolha(chave);
        folha.inserir(chave, valor);
        if (folha.estaCheio()) {
            dividirNoFolha(folha);
        }
    }

    public String buscarItem(int chave) {
        NoFolha folha = encontrarNoFolha(chave);
        return folha.buscar(chave);
    }

    public boolean removerItem(int chave) {
        NoFolha folha = encontrarNoFolha(chave);
        boolean removido = folha.remover(chave);

        if (removido) {
            if (!folha.temMinimoDeChaves() && folha != raiz) {
                lidarComSubutilizacaoFolha(folha);
            }
            if (raiz.obterChaves().isEmpty() && !raiz.ehFolha()) {
                raiz = ((NoInterno) raiz).obterDescendente(0);
                raiz.definirPai(null);
            } else if (raiz.obterChaves().isEmpty() && raiz.ehFolha()) {
                this.raiz = null;
                this.primeiraFolha = null;
            }
        }
        return removido;
    }

    private NoFolha encontrarNoFolha(int chave) {
        NoBase atual = raiz;
        while (!atual.ehFolha()) {
            NoInterno interno = (NoInterno) atual;
            int i = 0;
            while (i < interno.obterChaves().size() && chave >= interno.obterChaves().get(i)) {
                i++;
            }
            atual = interno.obterDescendentes().get(i);
        }
        return (NoFolha) atual;
    }

    private void dividirNoFolha(NoFolha folha) {
        NoFolha novaFolha = folha.dividir();
        int chavePromovida = novaFolha.obterChaves().get(0);

        if (folha == raiz) {
            NoInterno novaRaiz = new NoInterno(maxChaves);
            novaRaiz.obterChaves().add(chavePromovida);
            novaRaiz.adicionarDescendente(folha);
            novaRaiz.adicionarDescendente(novaFolha);
            raiz = novaRaiz;
        } else {
            inserirNoPai(folha, chavePromovida, novaFolha);
        }
    }

    private void inserirNoPai(NoBase filhoEsquerdo, int chave, NoBase filhoDireito) {
        NoInterno pai = (NoInterno) filhoEsquerdo.obterPai();
        if (pai == null) {
            NoInterno novaRaiz = new NoInterno(maxChaves);
            novaRaiz.obterChaves().add(chave);
            novaRaiz.adicionarDescendente(filhoEsquerdo);
            novaRaiz.adicionarDescendente(filhoDireito);
            filhoEsquerdo.definirPai(novaRaiz);
            filhoDireito.definirPai(novaRaiz);
            raiz = novaRaiz;
            return;
        }

        int posInsercao = 0;
        while (posInsercao < pai.obterChaves().size() && pai.obterChaves().get(posInsercao) < chave) {
            posInsercao++;
        }
        pai.obterChaves().add(posInsercao, chave);
        pai.inserirDescendente(posInsercao + 1, filhoDireito);

        if (pai.estaCheio()) {
            dividirNoInterno(pai);
        }
    }

    private void dividirNoInterno(NoInterno no) {
        int pontoMedio = no.obterChaves().size() / 2;
        int chavePromovida = no.obterChaves().get(pontoMedio);
        NoInterno novoNoInterno = new NoInterno(maxChaves);

        for (int i = pontoMedio + 1; i < no.obterChaves().size(); i++) {
            novoNoInterno.obterChaves().add(no.obterChaves().get(i));
        }
        for (int i = pontoMedio + 1; i < no.obterDescendentes().size(); i++) {
            NoBase descendente = no.obterDescendentes().get(i);
            novoNoInterno.adicionarDescendente(descendente);
        }

        no.obterChaves().subList(pontoMedio, no.obterChaves().size()).clear();
        no.obterDescendentes().subList(pontoMedio + 1, no.obterDescendentes().size()).clear();

        if (no == raiz) {
            NoInterno novaRaiz = new NoInterno(maxChaves);
            novaRaiz.obterChaves().add(chavePromovida);
            novaRaiz.adicionarDescendente(no);
            novaRaiz.adicionarDescendente(novoNoInterno);
            raiz = novaRaiz;
        } else {
            inserirNoPai(no, chavePromovida, novoNoInterno);
        }
    }

    private void lidarComSubutilizacaoFolha(NoFolha folha) {
        NoInterno pai = (NoInterno) folha.obterPai();
        if (pai == null) return;

        int indiceFolha = pai.obterDescendentes().indexOf(folha);

        if (indiceFolha > 0) {
            NoFolha irmaoEsquerdo = (NoFolha) pai.obterDescendentes().get(indiceFolha - 1);
            if (irmaoEsquerdo.obterChaves().size() > (maxChaves + 1) / 2) {
                int chaveParaMover = irmaoEsquerdo.obterChaves().remove(irmaoEsquerdo.obterChaves().size() - 1);
                String valorParaMover = irmaoEsquerdo.obterValores().remove(irmaoEsquerdo.obterValores().size() - 1);
                folha.inserir(chaveParaMover, valorParaMover);
                pai.obterChaves().set(indiceFolha - 1, folha.obterChaves().get(0));
                return;
            }
        }

        if (indiceFolha < pai.obterDescendentes().size() - 1) {
            NoFolha irmaoDireito = (NoFolha) pai.obterDescendentes().get(indiceFolha + 1);
            if (irmaoDireito.obterChaves().size() > (maxChaves + 1) / 2) {
                int chaveParaMover = irmaoDireito.obterChaves().remove(0);
                String valorParaMover = irmaoDireito.obterValores().remove(0);
                folha.inserir(chaveParaMover, valorParaMover);
                pai.obterChaves().set(indiceFolha, irmaoDireito.obterChaves().get(0));
                return;
            }
        }

        if (indiceFolha > 0) {
            NoFolha irmaoEsquerdo = (NoFolha) pai.obterDescendentes().get(indiceFolha - 1);
            mesclarNosFolha(irmaoEsquerdo, folha, pai, indiceFolha - 1);
        } else if (indiceFolha < pai.obterDescendentes().size() - 1) {
            NoFolha irmaoDireito = (NoFolha) pai.obterDescendentes().get(indiceFolha + 1);
            mesclarNosFolha(folha, irmaoDireito, pai, indiceFolha);
        }
    }

    private void mesclarNosFolha(NoFolha folhaEsquerda, NoFolha folhaDireita, NoInterno pai, int indiceChavePai) {
        folhaEsquerda.obterChaves().addAll(folhaDireita.obterChaves());
        folhaEsquerda.obterValores().addAll(folhaDireita.obterValores());

        folhaEsquerda.definirProximo(folhaDireita.obterProximo());
        if (folhaDireita.obterProximo() != null) {
            folhaDireita.obterProximo().definirAnterior(folhaEsquerda);
        }

        pai.obterChaves().remove(indiceChavePai);
        pai.removerDescendente(folhaDireita);

        if (pai != raiz && !pai.temMinimoDeChaves()) {
            lidarComSubutilizacaoInterna(pai);
        } else if (pai == raiz && pai.obterChaves().isEmpty()) {
            raiz = folhaEsquerda;
            folhaEsquerda.definirPai(null);
        }
    }

    private void lidarComSubutilizacaoInterna(NoInterno no) {
        NoInterno pai = (NoInterno) no.obterPai();
        if (pai == null) return;

        int indiceNo = pai.obterDescendentes().indexOf(no);

        if (indiceNo > 0) {
            NoInterno irmaoEsquerdo = (NoInterno) pai.obterDescendentes().get(indiceNo - 1);
            if (irmaoEsquerdo.obterChaves().size() > (maxChaves + 1) / 2) {
                int chaveDoPai = pai.obterChaves().remove(indiceNo - 1);
                int chaveDoIrmao = irmaoEsquerdo.obterChaves().remove(irmaoEsquerdo.obterChaves().size() - 1);
                NoBase filhoDoIrmao = irmaoEsquerdo.obterDescendentes().remove(irmaoEsquerdo.obterDescendentes().size() - 1);

                no.obterChaves().add(0, chaveDoPai);
                no.inserirDescendente(0, filhoDoIrmao);
                filhoDoIrmao.definirPai(no);

                pai.obterChaves().add(indiceNo - 1, chaveDoIrmao);
                return;
            }
        }

        if (indiceNo < pai.obterDescendentes().size() - 1) {
            NoInterno irmaoDireito = (NoInterno) pai.obterDescendentes().get(indiceNo + 1);
            if (irmaoDireito.obterChaves().size() > (maxChaves + 1) / 2) {
                int chaveDoPai = pai.obterChaves().remove(indiceNo);
                int chaveDoIrmao = irmaoDireito.obterChaves().remove(0);
                NoBase filhoDoIrmao = irmaoDireito.obterDescendentes().remove(0);

                no.obterChaves().add(chaveDoPai);
                no.adicionarDescendente(filhoDoIrmao);
                filhoDoIrmao.definirPai(no);

                pai.obterChaves().add(indiceNo, chaveDoIrmao);
                return;
            }
        }

        if (indiceNo > 0) {
            NoInterno irmaoEsquerdo = (NoInterno) pai.obterDescendentes().get(indiceNo - 1);
            mesclarNosInternos(irmaoEsquerdo, no, pai, indiceNo - 1);
        } else if (indiceNo < pai.obterDescendentes().size() - 1) {
            NoInterno irmaoDireito = (NoInterno) pai.obterDescendentes().get(indiceNo + 1);
            mesclarNosInternos(no, irmaoDireito, pai, indiceNo);
        }
    }

    private void mesclarNosInternos(NoInterno noEsquerdo, NoInterno noDireito, NoInterno pai, int indiceChavePai) {
        noEsquerdo.obterChaves().add(pai.obterChaves().remove(indiceChavePai));

        noEsquerdo.obterChaves().addAll(noDireito.obterChaves());
        for (NoBase descendente : noDireito.obterDescendentes()) {
            noEsquerdo.adicionarDescendente(descendente);
        }
        pai.removerDescendente(noDireito);

        if (pai != raiz && !pai.temMinimoDeChaves()) {
            lidarComSubutilizacaoInterna(pai);
        } else if (pai == raiz && pai.obterChaves().isEmpty()) {
            raiz = noEsquerdo;
            noEsquerdo.definirPai(null);
        }
    }

    public List<String> buscarIntervalo(int chaveInicio, int chaveFim) {
        List<String> resultados = new ArrayList<>();
        NoFolha atual = encontrarNoFolha(chaveInicio);
        while (atual != null) {
            for (int i = 0; i < atual.obterChaves().size(); i++) {
                int chave = atual.obterChaves().get(i);
                if (chave >= chaveInicio && chave <= chaveFim) {
                    resultados.add(atual.obterValores().get(i));
                } else if (chave > chaveFim) {
                    return resultados;
                }
            }
            atual = atual.obterProximo();
        }
        return resultados;
    }

    public void imprimirEmOrdem() {
        NoFolha atual = primeiraFolha;
        System.out.print("Itens em ordem (B+): ");
        while (atual != null) {
            for (int i = 0; i < atual.obterChaves().size(); i++) {
                System.out.print(atual.obterChaves().get(i) + ":" + atual.obterValores().get(i) + " ");
            }
            atual = atual.obterProximo();
        }
        System.out.println();
    }

    // --- LÓGICA PRINCIPAL DE EXECUÇÃO PARA ÁRVORE B+ ---
    private static final int ORDEM_ARVORE_BPLUS = 3;
    private static final String ARQUIVO_DADOS = "produtos_corrigido.txt";

    public static void main(String[] args) {
        ArvoreBPlus arvoreBPlus = new ArvoreBPlus(ORDEM_ARVORE_BPLUS);
        List<ItemProduto> itensParaProcessar = carregarItensDoArquivo(ARQUIVO_DADOS);

        System.out.println("--- Teste de Indexação com Árvore B+ (Ordem " + ORDEM_ARVORE_BPLUS + ") ---");
        System.out.println("Total de produtos no arquivo: " + itensParaProcessar.size());

        // Medir tempo de inserção
        long tempoInicioInsercao = System.nanoTime();
        for (ItemProduto item : itensParaProcessar) {
            arvoreBPlus.inserirItem(item.obterId(), item.obterInfoParaArvore());
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
            String infoProduto = arvoreBPlus.buscarItem(chave); // Busca antes para relatar se existe
            boolean removido = false;
            if (infoProduto != null) {
                removido = arvoreBPlus.removerItem(chave);
                System.out.println("Tentando remover ID " + chave + ": " + (removido ? "Removido: " + infoProduto : "Falha ao remover produto existente."));
            } else {
                System.out.println("Tentando remover ID " + chave + ": Produto não encontrado.");
            }
        }
        long tempoFimRemocao = System.nanoTime();
        long duracaoRemocao = (tempoFimRemocao - tempoInicioRemocao) / 1_000_000;
        System.out.println("Tempo de remoção de 10 produtos: " + duracaoRemocao + " ms");

        System.out.println("\n--- Teste da Árvore B+ Concluído ---");
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