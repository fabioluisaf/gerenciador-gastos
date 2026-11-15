package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosPorIntervaloRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;

import jakarta.persistence.EntityNotFoundException;

class GastoConsultaServiceTest {

	private GastoRepository repository;
	private FaturaService faturaService;
	private GastoConsultaService service;
	private UUID metodoPagamentoId;

	@BeforeEach
	void configurar() {
		repository = mock(GastoRepository.class);
		faturaService = mock(FaturaService.class);
		service = new GastoConsultaService(repository, faturaService);
		metodoPagamentoId = UUID.randomUUID();
	}

	@Test
	void listaPeriodosComGastos() {
		final var periodos = List.of(
				new PeriodoComGastosResponse(2025, 12),
				new PeriodoComGastosResponse(2024, 2)
		);
		when(repository.listarPeriodosComGastos()).thenReturn(periodos);

		assertSame(periodos, service.listarPeriodosComGastos());
		verify(repository).listarPeriodosComGastos();
	}

	@Test
	void consultaMesBissextoComPaginacao() {
		final var pageable = PageRequest.of(1, 10);
		final var pagina = new PageImpl<Gasto>(List.of(), pageable, 12);
		final var filtros = new FiltrarGastosRequest(null, null);
		when(repository.listarPorPeriodo(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), null, null, pageable))
				.thenReturn(pagina);
		assertSame(pagina, service.listarPorMesEAno(new BuscarGastosRequest(YearMonth.of(2024, 2)), filtros, pageable));
	}

	@Test
	void consultaIntervaloComPaginacao() {
		final var inicio = LocalDate.of(2026, 11, 25);
		final var fim = LocalDate.of(2026, 12, 5);
		final var pageable = PageRequest.of(1, 10);
		final var pagina = new PageImpl<Gasto>(List.of(), pageable, 12);
		for (final var filtros : List.of(
				new FiltrarGastosRequest(null, null),
				new FiltrarGastosRequest(metodoPagamentoId, null),
				new FiltrarGastosRequest(null, "Alimentacao"),
				new FiltrarGastosRequest(metodoPagamentoId, "Alimentacao")
		)) {
			when(repository.listarPorIntervalo(inicio, fim, filtros.metodoPagamentoId(), filtros.categoria(), pageable))
					.thenReturn(pagina);

			assertSame(pagina, service.listarPorIntervalo(new BuscarGastosPorIntervaloRequest(inicio, fim), filtros, pageable));
			verify(repository).listarPorIntervalo(inicio, fim, filtros.metodoPagamentoId(), filtros.categoria(), pageable);
		}
	}

	@Test
	void repassaFiltrosDoMesEDaFaturaSemAlterarCategoria() {
		final var pageable = PageRequest.of(0, 10);
		for (final var filtros : List.of(
				new FiltrarGastosRequest(metodoPagamentoId, null),
				new FiltrarGastosRequest(null, "Alimentacao"),
				new FiltrarGastosRequest(metodoPagamentoId, "Alimentacao")
		)) {
			when(repository.listarPorPeriodo(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29),
					filtros.metodoPagamentoId(), filtros.categoria(), pageable)).thenReturn(Page.empty(pageable));

			assertEquals(0, service.listarPorMesEAno(new BuscarGastosRequest(YearMonth.of(2024, 2)), filtros, pageable).getTotalElements());

			verify(repository).listarPorPeriodo(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29),
					filtros.metodoPagamentoId(), filtros.categoria(), pageable);
		}
		final var faturaId = UUID.randomUUID();
		final var categoria = "Alimentacao";
		when(repository.listarPorFatura(faturaId, categoria, pageable)).thenReturn(Page.empty(pageable));
		assertEquals(0, service.listarPorFatura(faturaId, categoria, pageable).getTotalElements());
		verify(repository).listarPorFatura(faturaId, categoria, pageable);
	}

	@Test
	void totalizaIntervaloComPaginacaoDosGrupos() {
		final var request = new BuscarGastosPorIntervaloRequest(
				LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 5));
		final var pageable = PageRequest.of(1, 2);
		final var categorias = new PageImpl<>(List.of(
				new TotalCategoriaResponse("Casa", new BigDecimal("10.50"), new BigDecimal("20.75"))
		), pageable, 3);
		final var metodos = new PageImpl<>(List.of(
				new TotalMetodoPagamentoResponse(metodoPagamentoId, InstituicaoFinanceira.CAIXA,
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

	@Test
	void listaPorFaturaComPaginacao() {
		final var faturaId = UUID.randomUUID();
		final var pageable = PageRequest.of(1, 10);
		final var pagina = new PageImpl<>(List.of(new Gasto()), pageable, 11);
		when(repository.listarPorFatura(faturaId, null, pageable)).thenReturn(pagina);

		assertSame(pagina, service.listarPorFatura(faturaId, null, pageable));

		verify(faturaService).validarExistencia(faturaId);
		verify(repository).listarPorFatura(faturaId, null, pageable);
	}

	@Test
	void naoListaGastosDeFaturaInexistente() {
		final var faturaId = UUID.randomUUID();
		final var pageable = PageRequest.of(0, 10);
		doThrow(new EntityNotFoundException()).when(faturaService).validarExistencia(faturaId);

		assertThrows(EntityNotFoundException.class, () -> service.listarPorFatura(faturaId, null, pageable));

		verifyNoInteractions(repository);
	}
}
