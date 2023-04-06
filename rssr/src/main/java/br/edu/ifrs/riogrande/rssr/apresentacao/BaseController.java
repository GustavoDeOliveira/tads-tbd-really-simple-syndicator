package br.edu.ifrs.riogrande.rssr.apresentacao;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public abstract class BaseController {

	protected <To, Td> ResponseEntity<List<Td>> listResponse(List<To> list, Function<? super To,? extends Td> mappingFunction) {
		return ResponseEntity.ok(list.stream()
				.map(mappingFunction)
				.collect(Collectors.toList()));
	}

	protected ResponseEntity<Object> created(String path, Map<String, UUID> map) {

		return ResponseEntity.created(
			ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path(path)
				.buildAndExpand(map)
				.toUri()
			).build();
	}
}
