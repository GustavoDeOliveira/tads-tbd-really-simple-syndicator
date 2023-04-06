package br.edu.ifrs.riogrande.rssr.persistencia;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class ConexaoJedis implements Conexao<Jedis> {
    private static ConexaoJedis instancia;
    private JedisPool reserva;
    private Jedis jedis;
    
    private ConexaoJedis() {
        iniciarReserva();
    }
    
    public static ConexaoJedis instancia() {
        if (instancia == null) {
            instancia = new ConexaoJedis();
        }
        return instancia;
    }
    
    public Jedis buscar() {
        return buscar(false);
    }
    
    @Override
    public Jedis buscar(boolean forcarAbertura) {
        if (forcarAbertura) {
            iniciarReserva();
        }
        if (forcarAbertura || jedis == null || !jedis.isConnected() || jedis.isBroken()) {
            this.jedis = reserva.getResource();
        }
        return this.jedis;
    }

    @Override
    public void fechar() {
        this.jedis.close();
    }
    
    private void iniciarReserva() {
        if (this.reserva != null) {
            this.reserva.close();
            this.reserva.destroy();
        }
        this.reserva = new JedisPool("localhost", 6379);
    }
}
