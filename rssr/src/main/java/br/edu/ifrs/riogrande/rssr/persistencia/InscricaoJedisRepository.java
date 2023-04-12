package br.edu.ifrs.riogrande.rssr.persistencia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.xml.sax.InputSource;

import com.google.gson.Gson;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

import br.edu.ifrs.riogrande.rssr.negocio.Artigo;
import br.edu.ifrs.riogrande.rssr.negocio.Corpo;
import br.edu.ifrs.riogrande.rssr.negocio.Inscricao;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class InscricaoJedisRepository implements Repository<Inscricao> {
    private final Gson gson;
    private final ConexaoJedis conexaoJedis;
    private final String instanciaInscricoes = "RSSR:Inscricoes";
    private final String instanciaArtigos = "RSSR:Feed";
    
    public InscricaoJedisRepository() {
        gson = new Gson();
        conexaoJedis = ConexaoJedis.instancia();
    }
    
    @Override
    public Optional<Inscricao> carregar(UUID id) {
        try(var jedis = conexaoJedis.buscar()) {
            var parametros = new ScanParams();
            parametros.match("*:" + id.toString());
            var ponteiro = ScanParams.SCAN_POINTER_START;
            do {
                var resultado = jedis.hscan(instanciaInscricoes, ponteiro, parametros);
                ponteiro = resultado.getCursor();
                if (!resultado.getResult().isEmpty()) {
                    return Optional.of(gson.fromJson(
                        resultado.getResult().get(0).getValue(),
                        Inscricao.class));
                }
            } while (!ponteiro.equals(ScanParams.SCAN_POINTER_START));

            return Optional.empty();
        }
    }

    public Optional<Inscricao> carregar(UUID id, String categoria) {
        try(var jedis = conexaoJedis.buscar()) {
            if (jedis.hexists(instanciaInscricoes, id.toString())) {
                var resultado = jedis.hget(instanciaInscricoes, chaveInscricoes(categoria, id));
                return Optional.of(gson.fromJson(resultado, Inscricao.class));
            } else {
                return Optional.empty();
            }
        }
    }
    
    public Inscricao buscarConteudo(Inscricao inscricao, int buscar, int pular) {
        try(var jedis = conexaoJedis.buscar()) {
            if (jedis.exists(chaveArtigos(inscricao))) {
                System.out.println("Buscando a partir do cache...");
                var resultado = jedis.zrevrange(chaveArtigos(inscricao), pular, pular+buscar-1);
                System.out.println(resultado.size() + " artigos encontrados no cache.");
                inscricao.setFeed(resultado.stream()
                    .map(r -> gson.fromJson(r, Artigo.class))
                    .toList());
                return inscricao;
            }
        }
        System.out.println("Buscando a partir da fonte...");
        String url = inscricao.getUrl();
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed syndFeed;
        try {
            syndFeed = input.build(new InputSource(url));
        } catch (FeedException ex) {
            System.err.println(ex.getLocalizedMessage());
            var feed = new ArrayList<Artigo>();
            feed.add(new Artigo(
                "Não foi possível buscar os artigos da fonte, tente novamente mais tarde",
                "",
                "",
                null,
                null));
            inscricao.setFeed(feed);
            return inscricao;
        }
        Iterator<SyndEntry> itr = syndFeed.getEntries().iterator();
        while (itr.hasNext()) {
            SyndEntry syndEntry = (SyndEntry) itr.next();
            String descricao = syndEntry.getDescription() != null
                    ? syndEntry.getDescription().getValue() : "";
            inscricao.getFeed().add(new Artigo(
                syndEntry.getTitle(),
                descricao,
                syndEntry.getLink(),
                syndEntry.getPublishedDate(),
                syndEntry.getContents().stream()
                    .map(c -> new Corpo(c.getValue(), c.getMode(), c.getType()))
                    .toList()
            ));
        }
        try(var jedis = conexaoJedis.buscar()) {
            System.out.println("Armazenando resultado da busca no cache...");
            jedis.zadd(
                chaveArtigos(inscricao),
                inscricao.getFeed().stream()
                        .collect(Collectors.<Artigo, String, Double>toMap(
                            a -> gson.toJson(a),
                            a -> (double)a.getDataPublicacao().getTime())));
            jedis.expire(chaveArtigos(inscricao), 300);
        }
        inscricao.setFeed(inscricao.getFeed().stream()
            .sorted(Collections.reverseOrder(Comparator.comparingLong(f -> f.getDataPublicacao().getTime())))
            .skip(pular)
            .limit(buscar)
            .toList());
        return inscricao;
    }

    @Override
    public void salvar(Inscricao entidade) {
        try (var jedis = conexaoJedis.buscar()) {
            var chave = chaveInscricoes(entidade);
            entidade.setCategoria(entidade.getCategoria().toLowerCase());
            var valor = gson.toJson(entidade);
            var chaves = buscarChavesPorId(entidade.getId(), jedis);
            var transaction = jedis.multi();
            chaves.forEach(c -> transaction.hdel(instanciaInscricoes, c));
            transaction.del(chaveArtigos(entidade));
            transaction.hset(instanciaInscricoes, chave, valor);
            transaction.exec();
        }
    }

    @Override
    public void remover(UUID id) {
        try (var jedis = conexaoJedis.buscar()) {
            var chaves = buscarChavesPorId(id, jedis);
            var transaction = jedis.multi();            
            chaves.forEach(c -> transaction.hdel(instanciaInscricoes, c));
            transaction.del(chaveArtigos(id));
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
            varredura = jedis.hscan(instanciaInscricoes, ponteiro, parametros);
            chaves.addAll(varredura.getResult().stream().map(e -> e.getKey()).toList());
        } while (ponteiro != ScanParams.SCAN_POINTER_START);
        
        return chaves;
    }

    @Override
    public List<Inscricao> listar(int pular, int buscar) {
        return listar(pular, buscar, "");
    }
    
    public List<Inscricao> listar(int pular, int buscar, String categoria) {
        List<Inscricao> lista = null;
        var registros = new ArrayList<Entry<String, String>>(pular+buscar);

        try(var jedis = conexaoJedis.buscar()) {
            var parametros = new ScanParams();
            if (categoria != "") {
                parametros.match(categoria + ":*");
            }

            var ponteiro = ScanParams.SCAN_POINTER_START;
            do {
                var varredura = jedis.hscan(instanciaInscricoes, ponteiro, parametros);
                ponteiro = varredura.getCursor();
                var iterator = varredura.getResult().iterator();
                while(registros.size() <= pular + buscar && iterator.hasNext()) {
                    registros.add(iterator.next());
                }
            } while (!ponteiro.equals(ScanParams.SCAN_POINTER_START) && registros.size() <= pular + buscar);
        }

        if (registros.size() > 0) {
            lista = registros.stream()
                    .skip(pular)
                    .limit(buscar)
                    .map((e) -> gson.fromJson(e.getValue(), Inscricao.class))
                    .collect(Collectors.toList());
        } else {
            lista = new ArrayList<Inscricao>(0);
        }
        return lista;
    }
    
    private String chaveInscricoes(Inscricao entidade) {
        return chaveInscricoes(entidade.getCategoria(), entidade.getId());
    }
        
    private String chaveInscricoes(String categoria, UUID id) {
        return categoria + ":" + id.toString();
    }

    private String chaveArtigos(Inscricao entidade) {
        return chaveArtigos(entidade.getId());
    }

    private String chaveArtigos(UUID id) {
        return instanciaArtigos + ":" + id.toString();
    }
}
