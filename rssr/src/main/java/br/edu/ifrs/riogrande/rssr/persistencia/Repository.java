package br.edu.ifrs.riogrande.rssr.persistencia;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Repository<T extends Serializable> {
    public Optional<T> carregar(UUID id);
    public void salvar(T entidade);
    public void remover(UUID id);
    public List<T> listar(int pular, int buscar);
}
