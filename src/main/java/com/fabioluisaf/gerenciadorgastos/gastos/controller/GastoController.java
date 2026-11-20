package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarGastoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosPorIntervaloUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarPeriodosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.detalhe.GastoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.GastoResumidoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoConsultaService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/gastos")
@RequiredArgsConstructor
public class GastoController {

	private final GastoService gastoService;
	private final GastoConsultaService gastoConsultaService;
	private final ApiResponsePresenter presenter;

	@PostMapping
	public ResponseEntity<ApiResponse<GastoResumidoResponse>> criar(@Valid @RequestBody CriarGastoBody request) {
		final var gasto = gastoService.criar(request);
		final var localizacao = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(gasto.getId())
				.toUri();

		return ResponseEntity.created(localizacao).body(presenter.apresentar(GastoResumidoResponse.from(gasto)));
	}

	@GetMapping("/periodos")
	public ApiResponse<List<PeriodoComGastosResponse>> listarPeriodosRealizados(
			@ModelAttribute FiltrarPeriodosUrlParams filtros) {

		return presenter.apresentar(gastoConsultaService.listarPeriodosRealizados(filtros));
	}

	@GetMapping("/planejados/periodos")
	public ApiResponse<List<PeriodoComGastosResponse>> listarPeriodosPlanejados(
			@ModelAttribute FiltrarPeriodosUrlParams filtros) {

		return presenter.apresentar(gastoConsultaService.listarPeriodosPlanejados(filtros));
	}

	@GetMapping
	public ApiResponse<List<GastoResumidoResponse>> listarRealizados(
			@Valid @ModelAttribute FiltrarGastosUrlParams filtros,
			@PageableDefault(sort = { "data", "id" }, direction = Direction.DESC) Pageable pageable) {

		return presenter.apresentarPagina(
				gastoConsultaService.listarRealizados(filtros, pageable).map(GastoResumidoResponse::from));
	}

	@GetMapping("/planejados")
	public ApiResponse<List<GastoResumidoResponse>> listarPlanejados(
			@Valid @ModelAttribute FiltrarGastosUrlParams filtros,
			@PageableDefault(sort = { "data", "id" }) Pageable pageable) {

		return presenter.apresentarPagina(
				gastoConsultaService.listarPlanejados(filtros, pageable).map(GastoResumidoResponse::from));
	}

	@GetMapping("/categorias")
	public ApiResponse<List<UsoCategoriaResponse>> listarCategorias() {
		return presenter.apresentar(gastoConsultaService.listarCategorias());
	}

	@GetMapping("/{id}")
	public ApiResponse<GastoResponse> buscarPorId(@PathVariable UUID id) {
		return presenter.apresentar(GastoResponse.from(gastoService.buscarPorId(id)));
	}

	@GetMapping("/totais/categoria")
	public ApiResponse<List<TotalCategoriaResponse>> totalizarPorCategoria(
			@Valid @ModelAttribute BuscarGastosPorIntervaloUrlParams request,
			@PageableDefault(sort = "categoria") Pageable pageable) {

		return presenter.apresentarPagina(gastoConsultaService.totalizarPorCategoria(request, pageable));
	}

	@GetMapping("/totais/metodo-pagamento")
	public ApiResponse<List<TotalMetodoPagamentoResponse>> totalizarPorMetodoPagamento(
			@Valid @ModelAttribute BuscarGastosPorIntervaloUrlParams request,
			@PageableDefault(sort = "metodoPagamento.id") Pageable pageable) {

		return presenter.apresentarPagina(gastoConsultaService.totalizarPorMetodoPagamento(request, pageable));
	}

	@PutMapping("/{id}")
	public ApiResponse<GastoResumidoResponse> atualizar(
			@PathVariable UUID id,
			@Valid @RequestBody AtualizarGastoBody request) {

		return presenter.apresentar(GastoResumidoResponse.from(gastoService.atualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable UUID id) {
		gastoService.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
