package br.edu.ifrs.riogrande.rssr.apresentacao;

import br.edu.ifrs.riogrande.rssr.apresentacao.viewmodels.FeedViewModel;
import br.edu.ifrs.riogrande.rssr.negocio.Feed;
import br.edu.ifrs.riogrande.rssr.persistencia.FeedJedisRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ResponseEntity<Object> salvar(
                    @RequestBody FeedViewModel body) {

        var feed = new Feed();
        feed.setNome(body.getNome());
        feed.setUrl(body.getUrl());
        feed.setCategoria(body.getCategoria());
        repository.salvar(feed);

        return created(
                "/{id}",
                Map.of("id", feed.getId()));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FeedViewModel> buscar(
                    @PathVariable UUID id) {

        var feed = repository.carregar(id);
        if (feed.isPresent()) {
            var value = feed.get();

            var model = new FeedViewModel();
            model.setNome(value.getNome());
            model.setUrl(value.getUrl());
            model.setCategoria(value.getCategoria());

            return ResponseEntity.ok(model);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<FeedViewModel> remover(
                    @PathVariable UUID id) {
                        
        repository.remover(id);
        return ResponseEntity.noContent().build();
    }
}