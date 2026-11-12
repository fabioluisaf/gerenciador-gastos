package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Credito;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;

import jakarta.persistence.EntityNotFoundException;

class FaturaServiceTest {

	@Test
	void calculaFechamentoPeloCartaoERespeitaUltimoDiaDoMes() {
		final var repository = mock(FaturaRepository.class);
		final var metodoService = mock(MetodoPagamentoService.class);
		final var service = new FaturaService(repository, metodoService);
		final var credito = new Credito(31, 7);
		final var id = UUID.randomUUID();
		when(metodoService.buscarCreditoPorId(id, "FATURA_PERMITIDA_APENAS_PARA_CREDITO")).thenReturn(credito);
		when(repository.save(any(Fatura.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		assertEquals(LocalDate.of(2026, 2, 28), service.obterOuCriar(id, YearMonth.of(2026, 2)).getDataFechamento());
		assertEquals(LocalDate.of(2024, 2, 29), service.obterOuCriar(id, YearMonth.of(2024, 2)).getDataFechamento());
		assertEquals(LocalDate.of(2026, 4, 30), service.obterOuCriar(id, YearMonth.of(2026, 4)).getDataFechamento());
		assertEquals("PERIODO_FATURA_OBRIGATORIO",
				assertThrows(PagamentoInvalidoException.class, () -> service.obterOuCriar(id, null)).getMessage());
	}

	@Test
	void calculaProximoVencimentoAposFechamentoComLimitesDoCalendario() {
		final var repository = mock(FaturaRepository.class);
		final var metodoService = mock(MetodoPagamentoService.class);
		final var service = new FaturaService(repository, metodoService);
		final var id = UUID.randomUUID();
		when(repository.save(any(Fatura.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		for (final var caso : new int[][] {
				{ 2026, 11, 10, 17, 2026, 11, 17 },
				{ 2026, 11, 20, 10, 2026, 12, 10 },
				{ 2026, 12, 10, 10, 2027, 1, 10 },
				{ 2026, 1, 31, 31, 2026, 2, 28 },
				{ 2024, 1, 31, 31, 2024, 2, 29 },
				{ 2026, 2, 10, 31, 2026, 2, 28 },
				{ 2026, 2, 31, 30, 2026, 3, 30 },
				{ 2026, 4, 29, 31, 2026, 4, 30 }
		}) {
			final var credito = new Credito(caso[2], caso[3]);
			when(metodoService.buscarCreditoPorId(id, "FATURA_PERMITIDA_APENAS_PARA_CREDITO"))
					.thenReturn(credito);
			final var fatura = service.obterOuCriar(id, YearMonth.of(caso[0], caso[1]));
			assertSame(credito, fatura.getMetodoPagamento());
			assertEquals(LocalDate.of(caso[4], caso[5], caso[6]), fatura.getDataVencimento());
		}
	}

	@Test
	void sugerePeriodoAntesNoDiaEDepoisDoFechamento() {
		final var service = new FaturaService(mock(FaturaRepository.class), mock(MetodoPagamentoService.class));
		final var credito = new Credito(31, 7);

		assertEquals(YearMonth.of(2026, 2), service.sugerirPeriodo(credito, LocalDate.of(2026, 2, 27)));
		assertEquals(YearMonth.of(2026, 3), service.sugerirPeriodo(credito, LocalDate.of(2026, 2, 28)));
		credito.atualizarDiaFechamento(10);
		assertEquals(YearMonth.of(2026, 12), service.sugerirPeriodo(credito, LocalDate.of(2026, 12, 8)));
		credito.atualizarDiaVencimento(25);
		assertEquals(YearMonth.of(2026, 12), service.sugerirPeriodo(credito, LocalDate.of(2026, 12, 8)));
		assertEquals(YearMonth.of(2027, 1), service.sugerirPeriodo(credito, LocalDate.of(2026, 12, 10)));
		assertEquals(YearMonth.of(2027, 1), service.sugerirPeriodo(credito, LocalDate.of(2026, 12, 11)));
		assertNull(service.sugerirPeriodo(credito, null));
		assertNull(service.sugerirPeriodo(MetodoPagamento.criar(TipoMetodoPagamento.PIX, null, null),
				LocalDate.of(2026, 2, 27)));
	}

	@Test
	void validaExistenciaDaFatura() {
		final var repository = mock(FaturaRepository.class);
		final var service = new FaturaService(repository, mock(MetodoPagamentoService.class));
		final var id = UUID.randomUUID();
		when(repository.existsById(id)).thenReturn(true, false);

		assertDoesNotThrow(() -> service.validarExistencia(id));
		assertThrows(EntityNotFoundException.class, () -> service.validarExistencia(id));
	}

	@Test
	void reutilizaFaturaDoMesMesmoAposAlteracaoDoDiaDeFechamento() {
		final var repository = mock(FaturaRepository.class);
		final var metodoService = mock(MetodoPagamentoService.class);
		final var service = new FaturaService(repository, metodoService);
		final var metodo = new Credito(10, 17);
		metodo.setId(UUID.randomUUID());
		when(metodoService.buscarCreditoPorId(metodo.getId(), "FATURA_PERMITIDA_APENAS_PARA_CREDITO"))
				.thenReturn(metodo);
		when(repository.findByMetodoPagamentoIdAndDataFechamentoBetween(
				metodo.getId(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)
		)).thenReturn(Optional.empty());
		when(repository.save(any(Fatura.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		final var fatura = service.obterOuCriar(metodo.getId(), YearMonth.of(2026, 11));
		when(repository.findByMetodoPagamentoIdAndDataFechamentoBetween(
				metodo.getId(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)
		)).thenReturn(Optional.of(fatura));
		metodo.atualizarDiaFechamento(15);
		metodo.atualizarDiaVencimento(22);

		assertSame(fatura, service.obterOuCriar(metodo.getId(), YearMonth.of(2026, 11)));
		assertEquals(LocalDate.of(2026, 11, 10), fatura.getDataFechamento());
		assertEquals(LocalDate.of(2026, 11, 17), fatura.getDataVencimento());
		verify(repository).save(fatura);
	}

	@Test
	void preservaErrosDeMetodoNaoCreditoEInexistente() {
		final var repository = mock(FaturaRepository.class);
		final var metodoRepository = mock(MetodoPagamentoRepository.class);
		final var metodoService = new MetodoPagamentoService(
				metodoRepository, mock(GastoRepository.class), repository
		);
		final var service = new FaturaService(repository, metodoService);
		final var id = UUID.randomUUID();
		final var data = YearMonth.of(2026, 11);
		when(metodoRepository.findById(id))
				.thenReturn(Optional.of(MetodoPagamento.criar(TipoMetodoPagamento.PIX, null, null)))
				.thenReturn(Optional.empty());

		assertEquals("FATURA_PERMITIDA_APENAS_PARA_CREDITO",
				assertThrows(PagamentoInvalidoException.class, () -> service.obterOuCriar(id, data)).getMessage());
		assertThrows(EntityNotFoundException.class, () -> service.obterOuCriar(id, data));
		verifyNoInteractions(repository);
	}

	@Test
	void buscaCreditoDesembrulhandoProxyDoMetodoPagamento() {
		final var repository = mock(MetodoPagamentoRepository.class);
		final var service = new MetodoPagamentoService(
				repository, mock(GastoRepository.class), mock(FaturaRepository.class)
		);
		final var credito = new Credito(10, 17);
		final var id = UUID.randomUUID();
		final var metodo = mock(MetodoPagamento.class, withSettings().extraInterfaces(HibernateProxy.class));
		final var proxy = (HibernateProxy) metodo;
		final var inicializador = mock(LazyInitializer.class);
		when(proxy.asHibernateProxy()).thenReturn(proxy);
		when(proxy.getHibernateLazyInitializer()).thenReturn(inicializador);
		when(inicializador.getImplementation()).thenReturn(credito);
		when(repository.findById(id)).thenReturn(Optional.of(metodo));

		assertSame(credito, service.buscarCreditoPorId(id, "FATURA_PERMITIDA_APENAS_PARA_CREDITO"));
	}
}
