package br.edu.ifrs.riogrande.rssr.negocio;

import java.util.Date;
import java.util.List;

public class Artigo {
    private String titulo;
    private String link;
    private String urlImg;
    private Date dataPublicacao;
    private List<Corpo> conteudo;

    public Artigo() {
    }

    public Artigo(String titulo, String link, String urlImg, Date dataPublicacao, List<Corpo> conteudo) {
        this.titulo = titulo;
        this.link = link;
        this.urlImg = urlImg;
        this.dataPublicacao = dataPublicacao;
        this.conteudo = conteudo;
    }

    public String getTitulo() {
        return this.titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getLink() {
        return this.link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getUrlImg() {
        return this.urlImg;
    }

    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
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
