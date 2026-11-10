package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.TotalizarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.detalhe.GastoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.GastoResumidoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoConsultaService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/gastos")
@RequiredArgsConstructor
public class GastoController {

	private final GastoService gastoService;
	private final GastoConsultaService gastoConsultaService;
	private final ApiResponsePresenter presenter;

	@PostMapping
	public ResponseEntity<ApiResponse<GastoResumidoResponse>> criar(@Valid @RequestBody CriarGastoRequest request) {
		final var gasto = gastoService.criar(request);
		final var localizacao = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(gasto.getId())
				.toUri();

		return ResponseEntity.created(localizacao).body(presenter.apresentar(GastoResumidoResponse.from(gasto)));
	}

	@GetMapping
	public ApiResponse<List<GastoResumidoResponse>> listar(
        @Valid @ModelAttribute BuscarGastosRequest request,
        @ModelAttribute FiltrarGastosRequest filtros,
        @PageableDefault(sort = { "data", "id" }) Pageable pageable
    ) {
		final var gastos = gastoConsultaService.listarPorMesEAno(request, filtros, pageable).map(GastoResumidoResponse::from);

		return presenter.apresentarPagina(gastos);
	}

	@GetMapping("/fatura/{id}")
	public ApiResponse<List<GastoResumidoResponse>> listarPorFatura(
			@PathVariable("id") UUID faturaId,
			@RequestParam(required = false) String categoria,
			@PageableDefault(sort = { "data", "id" }) Pageable pageable
	) {
		final var gastos = gastoConsultaService.listarPorFatura(faturaId, categoria, pageable).map(GastoResumidoResponse::from);

		return presenter.apresentarPagina(gastos);
	}

	@GetMapping("/{id}")
	public ApiResponse<GastoResponse> buscarPorId(@PathVariable UUID id) {
		return presenter.apresentar(GastoResponse.from(gastoService.buscarPorId(id)));
	}

	@GetMapping("/totais/categoria")
	@Operation(summary = "Totais realizados e planejados por categoria",
			description = "Considera todo o mes ou fatura, ignorando filtros de categoria e metodo. "
					+ "A paginacao limita os grupos retornados, nao os gastos somados. Grupos sem gastos nao sao retornados.")
	public ApiResponse<List<TotalCategoriaResponse>> totalizarPorCategoria(
			@Valid @ModelAttribute TotalizarGastosRequest request,
			@PageableDefault(sort = "categoria") Pageable pageable
	) {
		return presenter.apresentarPagina(gastoConsultaService.totalizarPorCategoria(request, pageable));
	}

	@GetMapping("/totais/metodo-pagamento")
	@Operation(summary = "Totais realizados e planejados por metodo de pagamento",
			description = "Agrupa pelo ID do metodo, considerando todo o mes ou fatura. "
					+ "Ignora filtros de categoria e metodo. A paginacao limita os grupos, nao os gastos somados.")
	public ApiResponse<List<TotalMetodoPagamentoResponse>> totalizarPorMetodoPagamento(
			@Valid @ModelAttribute TotalizarGastosRequest request,
			@PageableDefault(sort = "metodoPagamento.id") Pageable pageable
	) {
		return presenter.apresentarPagina(gastoConsultaService.totalizarPorMetodoPagamento(request, pageable));
	}

	@PutMapping("/{id}")
	public ApiResponse<GastoResumidoResponse> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody AtualizarGastoRequest request
    ) {
		return presenter.apresentar(GastoResumidoResponse.from(gastoService.atualizar(id, request)));
	}

	@PatchMapping("/{id}/realizado")
	public ApiResponse<GastoResumidoResponse> atualizarRealizado(
			@PathVariable UUID id, 
            @NotNull @RequestBody Boolean realizado
	) {
		return presenter.apresentar(GastoResumidoResponse.from(gastoService.atualizarRealizado(id, realizado)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable UUID id) {
		gastoService.excluir(id);

		return ResponseEntity.noContent().build();
	}
}
