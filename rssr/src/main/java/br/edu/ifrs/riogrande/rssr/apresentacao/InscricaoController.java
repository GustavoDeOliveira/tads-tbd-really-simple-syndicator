package br.edu.ifrs.riogrande.rssr.apresentacao;

import br.edu.ifrs.riogrande.rssr.apresentacao.viewmodels.InscricaoViewModel;
import br.edu.ifrs.riogrande.rssr.negocio.Inscricao;
import br.edu.ifrs.riogrande.rssr.persistencia.InscricaoJedisRepository;

import java.util.HashMap;
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
public class InscricaoController extends BaseController {
    private InscricaoJedisRepository repository;

    public InscricaoController() {
        repository = new InscricaoJedisRepository();
    }

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> salvar(
                    @RequestBody InscricaoViewModel body) {

        var erro = validarCategoria(body);
        if (erro.isPresent()) return erro.get();
        
        var inscricao = new Inscricao();
        inscricao.setNome(body.getNome());
        inscricao.setUrl(body.getUrl());
        inscricao.setCategoria(body.getCategoria());
        repository.salvar(inscricao);

        return created(
                "/{id}",
                Map.of("id", inscricao.getId()));
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> salvar(
            @PathVariable UUID id,
            @RequestBody InscricaoViewModel body) {

        var erro = validarCategoria(body);
        if (erro.isPresent()) return erro.get();

        var resultadoInscricao = repository.carregar(id);
        if (resultadoInscricao.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var inscricao = resultadoInscricao.get();
        inscricao.setNome(body.getNome());
        inscricao.setUrl(body.getUrl());
        inscricao.setCategoria(body.getCategoria());
        repository.salvar(inscricao);

        return ResponseEntity.ok(inscricao);
    }

    @GetMapping(path = "/{id}/feed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Inscricao> buscar(
            @PathVariable UUID id,
            Optional<Integer> pular,
            Optional<Integer> buscar) {

        if (pular.isEmpty()) pular = Optional.of(0);
        if (buscar.isEmpty()) buscar = Optional.of(10);
        var inscricao = repository.carregar(id);
        if (inscricao.isPresent()) {
            var resposta = repository.buscarConteudo(inscricao.get(), buscar.get(), pular.get());
            return ResponseEntity.ok(resposta);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<UUID, InscricaoViewModel>> listarTodos(
            Optional<Integer> pular,
            Optional<Integer> buscar) {

        return listarPorCategoria(pular, buscar, "");
    }

    @GetMapping(path = "/{categoria}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<UUID, InscricaoViewModel>> listarPorCategoria(
            Optional<Integer> pular,
            Optional<Integer> buscar,
            @PathVariable String categoria) {

        if (pular.isEmpty()) pular = Optional.of(0);
        if (buscar.isEmpty()) buscar = Optional.of(10);
        var inscricoes = repository.listar(pular.get(), buscar.get(), categoria);
        if (inscricoes.size() > 0) {
            return hashMapResponse(
                inscricoes,
                f -> {
                    var model = new InscricaoViewModel();
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
    public ResponseEntity<InscricaoViewModel> remover(
            @PathVariable UUID id) {
                        
        repository.remover(id);
        return ResponseEntity.noContent().build();
    }

    private Optional<ResponseEntity<Map<String, String>>> validarCategoria(InscricaoViewModel body) {
        if (body.getCategoria().matches("(?:^[^\\w_].*)|(?:.*[^\\w_-].*)|(?:.*[^\\w_]$)")) {
            var erros = new HashMap<String, String>();
            erros.put("categoria", "Formato da categoria inválido, deve ser amigável para urls. Ex: minha-categoria");
            var resposta = ResponseEntity.badRequest();
            return Optional.of(resposta.body(erros));
        }
        return Optional.empty();
    }
}