package br.edu.ifrs.riogrande.rssr.negocio;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Inscricao extends Entidade {
    private UUID id;
    private String nome;
    private String url;
    private String categoria;
    private List<Artigo> feed;
    
    @Override
    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public List<Artigo> getFeed() {
        if (feed == null) feed = new ArrayList<Artigo>();
        return feed;
    }

    public void setFeed(List<Artigo> artigos) {
        this.feed = artigos;
    }
    
}
