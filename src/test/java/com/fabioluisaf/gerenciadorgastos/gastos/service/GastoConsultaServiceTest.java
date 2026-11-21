package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.InstituicaoFinanceiraResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosPorIntervaloUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarPeriodosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicoesTeste;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FiltroGastos;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;

class GastoConsultaServiceTest {

	private GastoRepository repository;
	private FaturaService faturaService;
	private GastoConsultaService service;
	private UUID metodoPagamentoId;

	@BeforeEach
	void configurar() {
		repository = mock(GastoRepository.class);
		faturaService = mock(FaturaService.class);
		service = new GastoConsultaService(repository);
		metodoPagamentoId = UUID.randomUUID();
	}

	@Test
	void listaPeriodosAplicandoOsFiltrosDeMetodoECategoria() {
		final var periodos = List.of(
				new PeriodoComGastosResponse(2025, 12, 31),
				new PeriodoComGastosResponse(2024, 2, 4)
		);
		final var filtro = FiltroGastos.porMetodosECategorias(List.of(metodoPagamentoId), List.of("Casa", "Lazer"));
		when(repository.listarPeriodosRealizados(filtro)).thenReturn(periodos);

		assertSame(periodos, service.listarPeriodosRealizados(
				new FiltrarPeriodosUrlParams(List.of(metodoPagamentoId), List.of("Lazer", "Casa", "Casa"), null)));
		verify(repository).listarPeriodosRealizados(filtro);
	}

	@Test
	@SuppressWarnings("unchecked")
	void listaRealizadosEPlanejadosPelaEspecificacaoComAPaginacaoRecebida() {
		final var pageable = PageRequest.of(1, 10);
		final var realizados = new PageImpl<Gasto>(List.of(), pageable, 12);
		final var planejados = new PageImpl<Gasto>(List.of(), pageable, 3);
		final var filtros = new FiltrarGastosUrlParams(
				LocalDate.of(2026, 11, 1), null, List.of(metodoPagamentoId), List.of("Casa"), null, null);
		when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(realizados, planejados);

		assertSame(realizados, service.listarRealizados(filtros, pageable));
		assertSame(planejados, service.listarPlanejados(filtros, pageable));
		verify(repository, times(2)).findAll(any(Specification.class), eq(pageable));
	}

	@Test
	void totalizaIntervaloComPaginacaoDosGrupos() {
		final var request = new BuscarGastosPorIntervaloUrlParams(
				LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 5));
		final var pageable = PageRequest.of(1, 2);
		final var categorias = new PageImpl<>(List.of(
				new TotalCategoriaResponse("Casa", new BigDecimal("10.50"), new BigDecimal("20.75"))
		), pageable, 3);
		final var metodos = new PageImpl<>(List.of(
				new TotalMetodoPagamentoResponse(metodoPagamentoId, InstituicaoFinanceiraResponse.from(InstituicoesTeste.caixa()),
						TipoMetodoPagamento.PIX, "Pix Caixa", new BigDecimal("10.50"), new BigDecimal("20.75"))
		), pageable, 3);
		when(repository.totalizarPorCategoria(request.inicio(), request.fim(), pageable)).thenReturn(categorias);
		when(repository.totalizarPorMetodoPagamento(request.inicio(), request.fim(), pageable)).thenReturn(metodos);

		assertSame(categorias, service.totalizarPorCategoria(request, pageable));
		assertSame(metodos, service.totalizarPorMetodoPagamento(request, pageable));

		verify(repository).totalizarPorCategoria(request.inicio(), request.fim(), pageable);
		verify(repository).totalizarPorMetodoPagamento(request.inicio(), request.fim(), pageable);
		verifyNoInteractions(faturaService);
	}
}
