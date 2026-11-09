package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;

class FaturaServiceTest {

	@Test
	void reutilizaFaturaDoMesMesmoAposAlteracaoDoDiaDeFechamento() {
		final var repository = mock(FaturaRepository.class);
		final var metodoService = mock(MetodoPagamentoService.class);
		final var service = new FaturaService(repository, metodoService);
		final var metodo = new MetodoPagamento();
		metodo.setId(UUID.randomUUID());
		metodo.setTipo(TipoMetodoPagamento.CREDITO);
		metodo.setDiaFechamento(10);
		when(metodoService.buscarEntidadePorId(metodo.getId())).thenReturn(metodo);
		when(repository.findByMetodoPagamentoIdAndDataFechamentoBetween(
				metodo.getId(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)
		)).thenReturn(Optional.empty());
		when(repository.save(any(Fatura.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		final var fatura = service.obterOuCriar(metodo.getId(), LocalDate.of(2026, 11, 10));
		when(repository.findByMetodoPagamentoIdAndDataFechamentoBetween(
				metodo.getId(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)
		)).thenReturn(Optional.of(fatura));
		metodo.setDiaFechamento(15);

		assertSame(fatura, service.obterOuCriar(metodo.getId(), LocalDate.of(2026, 11, 15)));
		assertEquals(LocalDate.of(2026, 11, 10), fatura.getDataFechamento());
		verify(repository).save(fatura);
	}
}
