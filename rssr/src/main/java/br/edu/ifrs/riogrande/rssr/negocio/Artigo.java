package br.edu.ifrs.riogrande.rssr.negocio;

import java.util.Date;
import java.util.List;

public class Artigo {
    private String titulo;
    private String descricao;
    private String link;
    private Date dataPublicacao;
    private List<Corpo> conteudo;

    public Artigo() {
    }

    public Artigo(String titulo, String descricao, String link, Date dataPublicacao, List<Corpo> conteudo) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.link = link;
        this.dataPublicacao = dataPublicacao;
        this.conteudo = conteudo;
    }

    public String getTitulo() {
        return this.titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Date getDataPublicacao() {
        return dataPublicacao;
    }

    public void setDataPublicacao(Date dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }

    public List<Corpo> getConteudo() {
        return this.conteudo;
    }

    public void setConteudo(List<Corpo> conteudo) {
        this.conteudo = conteudo;
    }
}
