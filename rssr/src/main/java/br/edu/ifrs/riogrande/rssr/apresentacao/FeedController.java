package br.edu.ifrs.riogrande.rssr.apresentacao;

import br.edu.ifrs.riogrande.rssr.apresentacao.viewmodels.FeedViewModel;
import br.edu.ifrs.riogrande.rssr.negocio.Feed;
import br.edu.ifrs.riogrande.rssr.persistencia.FeedJedisRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
public class FeedController extends BaseController {
    private FeedJedisRepository repository;

    public FeedController() {
        repository = new FeedJedisRepository();
    }

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> salvar(
                    @RequestBody FeedViewModel body) {

        var erro = validarCategoria(body);
        if (erro.isPresent()) return erro.get();
        
        var feed = new Feed();
        feed.setNome(body.getNome());
        feed.setUrl(body.getUrl());
        feed.setCategoria(body.getCategoria());
        repository.salvar(feed);

        return created(
                "/{id}",
                Map.of("id", feed.getId()));
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> salvar(
            @PathVariable UUID id,
            @RequestBody FeedViewModel body) {

        var erro = validarCategoria(body);
        if (erro.isPresent()) return erro.get();

        var resultadoFeed = repository.carregar(id);
        if (resultadoFeed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var feed = resultadoFeed.get();
        feed.setNome(body.getNome());
        feed.setUrl(body.getUrl());
        feed.setCategoria(body.getCategoria());
        repository.salvar(feed);

        return ResponseEntity.ok(feed);
    }

    @GetMapping(path = "/{id}/content", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Feed> buscar(
            @PathVariable UUID id,
            Optional<Integer> pular,
            Optional<Integer> buscar) {

        if (pular.isEmpty()) pular = Optional.of(0);
        if (buscar.isEmpty()) buscar = Optional.of(10);
        var feed = repository.carregar(id);
        if (feed.isPresent()) {
            var value = feed.get();
            value = repository.buscarConteudo(value, buscar.get(), pular.get());
            return ResponseEntity.ok(value);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<UUID, FeedViewModel>> listarTodos(
            Optional<Integer> pular,
            Optional<Integer> buscar) {

        return listarPorCategoria(pular, buscar, "");
    }

    @GetMapping(path = "/{categoria}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<UUID, FeedViewModel>> listarPorCategoria(
            Optional<Integer> pular,
            Optional<Integer> buscar,
            @PathVariable String categoria) {

        if (pular.isEmpty()) pular = Optional.of(0);
        if (buscar.isEmpty()) buscar = Optional.of(10);
        var feeds = repository.listar(pular.get(), buscar.get(), categoria);
        if (feeds.size() > 0) {
            return hashMapResponse(
                feeds,
                f -> {
                    var model = new FeedViewModel();
                    model.setNome(f.getNome());
                    model.setUrl(f.getUrl());
                    model.setCategoria(f.getCategoria());
                    return model;
                },
                f -> f.getId());
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<FeedViewModel> remover(
            @PathVariable UUID id) {
                        
        repository.remover(id);
        return ResponseEntity.noContent().build();
    }

    private Optional<ResponseEntity<Map<String, String>>> validarCategoria(FeedViewModel body) {
        if (body.getCategoria().matches("(?:^[^\\w_].*)|(?:.*[^\\w_-].*)|(?:.*[^\\w_]$)")) {
            var erros = new HashMap<String, String>();
            erros.put("categoria", "Formato da categoria inválido, deve ser amigável para urls. Ex: minha-categoria");
            var resposta = ResponseEntity.badRequest();
            return Optional.of(resposta.body(erros));
        }
        return Optional.empty();
    }
}