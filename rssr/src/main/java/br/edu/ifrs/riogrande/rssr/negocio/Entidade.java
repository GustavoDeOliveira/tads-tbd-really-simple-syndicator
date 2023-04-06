package br.edu.ifrs.riogrande.rssr.negocio;

import java.io.Serializable;
import java.util.UUID;

public abstract class Entidade implements Serializable {
    public abstract UUID getId();
}
