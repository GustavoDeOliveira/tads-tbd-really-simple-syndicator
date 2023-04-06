package br.edu.ifrs.riogrande.rssr.persistencia;

public interface Conexao<T> {
    public T buscar(boolean forcarAbertura);
    public void fechar();
}
