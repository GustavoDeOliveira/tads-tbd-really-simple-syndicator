package br.edu.ifrs.riogrande.rssr.negocio;

public class Corpo {
    private String valor;
    private String modo;
    private String tipo;

    public Corpo(String valor, String modo, String tipo) {
        this.valor = valor;
        this.modo = modo;
        this.tipo = tipo;
    }
    
    public String getValor() {
        return valor;
    }
    public void setValor(String valor) {
        this.valor = valor;
    }
    public String getModo() {
        return modo;
    }
    public void setModo(String modo) {
        this.modo = modo;
    }
    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
