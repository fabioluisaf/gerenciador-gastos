package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.AplicarGrupoPlanejadoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.SalvarGrupoPlanejadoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.detalhe.GrupoPlanejadoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.GastoResumidoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GrupoPlanejadoService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/grupos-planejados")
@RequiredArgsConstructor
public class GrupoPlanejadoController {

	private final GrupoPlanejadoService service;
	private final ApiResponsePresenter presenter;

	@PostMapping
	public ResponseEntity<ApiResponse<GrupoPlanejadoResponse>> criar(
			@Valid @RequestBody SalvarGrupoPlanejadoBody request) {

		final var grupo = service.criar(request);
		final var localizacao = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(grupo.getId())
				.toUri();

		return ResponseEntity.created(localizacao).body(presenter.apresentar(GrupoPlanejadoResponse.from(grupo)));
	}

	@GetMapping
	public ApiResponse<List<GrupoPlanejadoResponse>> listar(
			@PageableDefault(size = 100, sort = "nome") Pageable pageable) {

		return presenter.apresentarPagina(service.listar(pageable).map(GrupoPlanejadoResponse::from));
	}

	@GetMapping("/{id}")
	public ApiResponse<GrupoPlanejadoResponse> buscarPorId(@PathVariable UUID id) {
		return presenter.apresentar(GrupoPlanejadoResponse.from(service.buscarPorId(id)));
	}

	@PutMapping("/{id}")
	public ApiResponse<GrupoPlanejadoResponse> atualizar(
			@PathVariable UUID id, @Valid @RequestBody SalvarGrupoPlanejadoBody request) {

		return presenter.apresentar(GrupoPlanejadoResponse.from(service.atualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable UUID id) {
		service.excluir(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/aplicacoes")
	public ResponseEntity<ApiResponse<List<GastoResumidoResponse>>> aplicar(
			@PathVariable UUID id, @Valid @RequestBody AplicarGrupoPlanejadoBody request) {

		final var gastos = service.aplicar(id, request).stream().map(GastoResumidoResponse::from).toList();

		return ResponseEntity.status(HttpStatus.CREATED).body(presenter.apresentar(gastos));
	}
}
