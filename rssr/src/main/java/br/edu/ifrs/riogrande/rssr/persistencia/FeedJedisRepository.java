package br.edu.ifrs.riogrande.rssr.persistencia;

import br.edu.ifrs.riogrande.rssr.negocio.Feed;
import com.google.gson.Gson;

import java.time.ZoneOffset;
import java.time.temporal.TemporalField;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FeedJedisRepository implements Repository<Feed> {
    private final Gson gson;
    private final ConexaoJedis conexaoJedis;
    
    public FeedJedisRepository() {
        gson = new Gson();
        conexaoJedis = ConexaoJedis.instancia();
    }
    
    @Override
    public Optional<Feed> carregar(UUID id) {
        try(var jedis = conexaoJedis.buscar()) {
            if (jedis.exists(id.toString())) {
                var resultado = jedis.get(id.toString());
                return Optional.of(gson.fromJson(resultado, Feed.class));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void salvar(Feed entidade) {
        try (var jedis = conexaoJedis.buscar()) {
            var valor = gson.toJson(entidade);
            jedis.set(entidade.getId().toString(), valor);

            //jedis.zadd("rssr:feed", entidade.getDataAdicionado().toEpochSecond(ZoneOffset.UTC), entidade.getId().toString(), valor);
        }
    }

    @Override
    public void remover(UUID id) {
        try (var jedis = conexaoJedis.buscar()) {
            if (jedis.exists(id.toString())) {
                jedis.del(id.toString());
            }
        }
    }

    @Override
    public List<Feed> listar(int pular, int buscar) {
        try(var jedis = conexaoJedis.buscar()) {
            if (jedis.exists(id.toString())) {
                var resultado = jedis.get(id.toString());
                return Optional.of(gson.fromJson(resultado, Feed.class));
            } else {
                return Optional.empty();
            }
        }
    }
    
    public List<Feed> listar(int pular, int buscar, String categoria) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
