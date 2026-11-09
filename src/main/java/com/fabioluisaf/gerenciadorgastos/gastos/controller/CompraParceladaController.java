package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.UUID;

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

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.CompraParceladaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.CompraParceladaService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/compras-parceladas")
@RequiredArgsConstructor
public class CompraParceladaController {

	private final CompraParceladaService service;
	private final ApiResponsePresenter presenter;

	@PostMapping
	public ResponseEntity<ApiResponse<CompraParceladaResponse>> criar(
			@Valid @RequestBody CriarCompraParceladaRequest request
	) {
		final var compra = service.criar(request);
		final var localizacao = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(compra.getId())
				.toUri();
		return ResponseEntity.created(localizacao).body(presenter.apresentar(CompraParceladaResponse.from(compra)));
	}

	@GetMapping("/{id}")
	public ApiResponse<CompraParceladaResponse> buscarPorId(@PathVariable UUID id) {
		return presenter.apresentar(CompraParceladaResponse.from(service.buscarPorId(id)));
	}

	@PutMapping("/{id}")
	public ApiResponse<CompraParceladaResponse> atualizar(
			@PathVariable UUID id, @Valid @RequestBody AtualizarCompraParceladaRequest request
	) {
		return presenter.apresentar(CompraParceladaResponse.from(service.atualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable UUID id) {
		service.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
