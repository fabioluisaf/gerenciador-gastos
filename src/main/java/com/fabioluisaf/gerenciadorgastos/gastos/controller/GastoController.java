package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.BuscarGastosPorMesEAnoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.GastoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiPageResponse;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/gastos")
@RequiredArgsConstructor
public class GastoController {

	private final GastoService gastoService;
	private final ApiResponsePresenter presenter;

	@PostMapping
	public ResponseEntity<ApiResponse<GastoResponse>> criar(@Valid @RequestBody CriarGastoRequest request) {
		final var gasto = gastoService.criar(request);
		final var localizacao = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(gasto.getId())
				.toUri();
		return ResponseEntity.created(localizacao).body(presenter.apresentar(GastoResponse.from(gasto)));
	}

	@GetMapping
	public ApiPageResponse<GastoResponse> listar(
        @Valid @ModelAttribute BuscarGastosPorMesEAnoRequest request,
        @PageableDefault(sort = { "data", "id" }) Pageable pageable
    ) {
		final var gastos = gastoService.listarPorMesEAno(request, pageable).map(GastoResponse::from);
		return presenter.apresentarPagina(gastos);
	}

	@GetMapping("/{id}")
	public ApiResponse<GastoResponse> buscarPorId(@PathVariable UUID id) {
		return presenter.apresentar(GastoResponse.from(gastoService.buscarPorId(id)));
	}

	@PutMapping("/{id}")
	public ApiResponse<GastoResponse> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody AtualizarGastoRequest request
    ) {
		return presenter.apresentar(GastoResponse.from(gastoService.atualizar(id, request)));
	}

	@PatchMapping("/{id}/realizado")
	public ApiResponse<GastoResponse> atualizarRealizado(
			@PathVariable UUID id, 
            @NotNull @RequestBody Boolean realizado
	) {
		return presenter.apresentar(GastoResponse.from(gastoService.atualizarRealizado(id, realizado)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable UUID id) {
		gastoService.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
