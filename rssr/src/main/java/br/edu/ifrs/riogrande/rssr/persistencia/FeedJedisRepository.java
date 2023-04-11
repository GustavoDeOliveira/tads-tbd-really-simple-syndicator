package br.edu.ifrs.riogrande.rssr.persistencia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.xml.sax.InputSource;

import com.google.gson.Gson;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

import br.edu.ifrs.riogrande.rssr.negocio.Artigo;
import br.edu.ifrs.riogrande.rssr.negocio.Corpo;
import br.edu.ifrs.riogrande.rssr.negocio.Feed;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.ScanResult;

public class FeedJedisRepository implements Repository<Feed> {
    private final Gson gson;
    private final ConexaoJedis conexaoJedis;
    private final String instanciaFeed = "Feed";
    private final String instanciaArtigos = "Artigos:";
    
    public FeedJedisRepository() {
        gson = new Gson();
        conexaoJedis = ConexaoJedis.instancia();
    }
    
    @Override
    public Optional<Feed> carregar(UUID id) {
        try(var jedis = conexaoJedis.buscar()) {
            var parametros = new ScanParams();
            parametros.match("*:" + id.toString());
            var resultado = jedis.hscan(instanciaFeed, ScanParams.SCAN_POINTER_START, parametros);
            if (resultado.getResult().isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(gson.fromJson(resultado.getResult().get(0).getValue(), Feed.class));
            }
        }
    }

    public Optional<Feed> carregar(UUID id, String categoria) {
        try(var jedis = conexaoJedis.buscar()) {
            if (jedis.hexists(instanciaFeed, id.toString())) {
                var resultado = jedis.hget(instanciaFeed, chave(categoria, id));
                return Optional.of(gson.fromJson(resultado, Feed.class));
            } else {
                return Optional.empty();
            }
        }
    }
    
    public Feed buscarConteudo(Feed feed, int buscar, int pular) {
        try(var jedis = conexaoJedis.buscar()) {
            if (jedis.exists(chaveArtigos(feed, pular, buscar))) {
                System.out.println("Buscando a partir do cache...");
                var parametros = new ZRangeParams(pular, pular + buscar);
                var resultado = jedis.zrange(chaveArtigos(feed, pular, buscar), parametros);
                System.out.println(resultado.size() + " artigos encontrados no cache.");
                feed.setArtigos(resultado.stream()
                    .map(r -> gson.fromJson(r, Artigo.class))
                    .toList());
                return feed;
            }
        }
        System.out.println("Buscando a partir da fonte...");
        String url = feed.getUrl();
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed syndFeed;
        try {
            syndFeed = input.build(new InputSource(url));
        } catch (FeedException ex) {
            System.err.println(ex.getLocalizedMessage());
            var artigos = new ArrayList<Artigo>();
            artigos.add(new Artigo(
                "Não foi possível buscar os artigos da fonte, tente novamente mais tarde",
                "",
                "",
                null,
                null));
            feed.setArtigos(artigos);
            return feed;
        }
        Iterator<SyndEntry> itr = syndFeed.getEntries().iterator();
        while (itr.hasNext()) {
            SyndEntry syndEntry = (SyndEntry) itr.next();
            feed.getArtigos().add(new Artigo(
                syndEntry.getTitle(),
                syndEntry.getLink(),
                syndEntry.getUri(),
                syndEntry.getPublishedDate(),
                syndEntry.getContents().stream()
                    .map(c -> new Corpo(c.getValue(), c.getMode(), c.getType()))
                    .toList()
            ));
        }
        try(var jedis = conexaoJedis.buscar()) {
            System.out.println("Armazenando resultado da busca no cache...");
            jedis.zadd(
                chaveArtigos(feed, pular, buscar),
                feed.getArtigos().stream()
                        .collect(Collectors.<Artigo, String, Double>toMap(
                            a -> gson.toJson(a),
                            a -> (double)a.getDataPublicacao().getTime())));
        }
        return feed;
    }

    @Override
    public void salvar(Feed entidade) {
        try (var jedis = conexaoJedis.buscar()) {
            var chave = chave(entidade);
            entidade.setCategoria(entidade.getCategoria().toLowerCase());
            var valor = gson.toJson(entidade);
            var chaves = buscarChavesPorId(entidade.getId(), jedis);
            var transaction = jedis.multi();
            chaves.forEach(c -> transaction.hdel(instanciaFeed, c));
            transaction.hset(instanciaFeed, chave, valor);
            transaction.exec();
        }
    }

    @Override
    public void remover(UUID id) {
        try (var jedis = conexaoJedis.buscar()) {
            var chaves = buscarChavesPorId(id, jedis);
            var transaction = jedis.multi();            
            chaves.forEach(c -> transaction.hdel(instanciaFeed, c));
            transaction.exec();
        }
    }

    public List<String> buscarChavesPorId(UUID id, Jedis jedis) {
        List<String> chaves = new ArrayList<String>(1);

        var parametros = new ScanParams();
        parametros.match("*:" + id.toString());
        String ponteiro = ScanParams.SCAN_POINTER_START;
        ScanResult<Entry<String, String>> varredura;
        do {
            varredura = jedis.hscan(instanciaFeed, ponteiro, parametros);
            chaves.addAll(varredura.getResult().stream().map(e -> e.getKey()).toList());
        } while (ponteiro != ScanParams.SCAN_POINTER_START);
        
        return chaves;
    }

    @Override
    public List<Feed> listar(int pular, int buscar) {
        return listar(pular, buscar, "");
    }
    
    public List<Feed> listar(int pular, int buscar, String categoria) {
        List<Feed> lista = null;
        var entries = new ArrayList<Entry<String, String>>(pular+buscar);

        try(var jedis = conexaoJedis.buscar()) {
            var parametros = new ScanParams();
            if (categoria != "") {
                parametros.match(categoria + ":*");
            }

            var ponteiro = ScanParams.SCAN_POINTER_START;
            do {
                var varredura = jedis.hscan(instanciaFeed, ponteiro, parametros);
                ponteiro = varredura.getCursor();
                var iterator = varredura.getResult().iterator();
                while(entries.size() <= pular + buscar && iterator.hasNext()) {
                    entries.add(iterator.next());
                }
            } while (!ponteiro.equals(ScanParams.SCAN_POINTER_START) && entries.size() <= pular + buscar);
        }

        if (entries.size() > 0) {
            lista = entries.stream()
                    .skip(pular)
                    .limit(buscar)
                    .map((e) -> gson.fromJson(e.getValue(), Feed.class))
                    .collect(Collectors.toList());
        } else {
            lista = new ArrayList<Feed>(0);
        }
        return lista;
    }
    
    private String chave(Feed entidade) {
        return chave(entidade.getCategoria(), entidade.getId());
    }
        
    private String chave(String categoria, UUID id) {
        return categoria + ":" + id.toString();
    }

    private String chaveArtigos(Feed entidade, int pular, int buscar) {
        return instanciaArtigos + ":" + entidade.getId() + ":" + pular + "-" + buscar;
    }
}
