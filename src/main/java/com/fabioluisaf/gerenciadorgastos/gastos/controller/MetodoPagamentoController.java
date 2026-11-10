package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarMetodoPagamentoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarMetodoPagamentoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.MetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.MetodoPagamentoService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/metodos-pagamento")
@RequiredArgsConstructor
public class MetodoPagamentoController {

	private final MetodoPagamentoService service;
	private final ApiResponsePresenter presenter;

	@PostMapping
	public ResponseEntity<ApiResponse<MetodoPagamentoResponse>> criar(
			@Valid @RequestBody CriarMetodoPagamentoRequest request
	) {
		final var metodo = service.criar(request);
		final var localizacao = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(metodo.getId())
				.toUri();

		return ResponseEntity.created(localizacao)
				.body(presenter.apresentar(MetodoPagamentoResponse.from(metodo)));
	}

	@GetMapping
	public ApiResponse<List<MetodoPagamentoResponse>> listar(
			@PageableDefault(sort = { "tipo", "instituicaoFinanceira", "nome" }) Pageable pageable
	) {
		final var metodos = service.listar(pageable).map(MetodoPagamentoResponse::from);

		return presenter.apresentarPagina(metodos);
	}

	@GetMapping("/{id}")
	public ApiResponse<MetodoPagamentoResponse> buscarPorId(@PathVariable UUID id) {
		return presenter.apresentar(MetodoPagamentoResponse.from(service.buscarPorId(id)));
	}

	@PutMapping("/{id}")
	public ApiResponse<MetodoPagamentoResponse> atualizar(
			@PathVariable UUID id, @Valid @RequestBody AtualizarMetodoPagamentoRequest request
	) {
		return presenter.apresentar(MetodoPagamentoResponse.from(service.atualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable UUID id) {
		service.excluir(id);

		return ResponseEntity.noContent().build();
	}
}
