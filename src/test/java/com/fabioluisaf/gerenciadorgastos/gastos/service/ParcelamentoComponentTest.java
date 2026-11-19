package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicoesTeste;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

class ParcelamentoComponentTest {

	@Test
	void resolveFaturasAntesDeAssociarParcelasEAplicaCadaUmaAoMesCorrespondente() {
		final var faturaService = mock(FaturaService.class);
		final var metodo = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 31, 7);
		metodo.setId(UUID.randomUUID());
		metodo.setInstituicaoFinanceira(InstituicoesTeste.nubank());
		final var primeiraFatura = new Fatura();
		final var segundaFatura = new Fatura();
		when(faturaService.obterOuCriar(metodo.getId(), YearMonth.of(2024, 1))).thenReturn(primeiraFatura);
		when(faturaService.obterOuCriar(metodo.getId(), YearMonth.of(2024, 2))).thenReturn(segundaFatura);
		final var primeira = new Gasto();
		primeira.setNumeroParcela(1);
		primeira.setValor(BigDecimal.ONE);
		final var segunda = new Gasto();
		segunda.setNumeroParcela(2);
		segunda.setValor(BigDecimal.ONE);
		final var parcelas = List.of(primeira, segunda);
		final var compra = new CompraParcelada();
		compra.setParcelas(new ArrayList<>(parcelas));
		final var component = new ParcelamentoComponent(faturaService);
		final var faturas = component.obterFaturas(metodo, YearMonth.of(2024, 1), 2);

		component.aplicarDados(compra, LocalDate.of(2024, 1, 31), "Compra", "Casa",
				metodo, faturas, null);

		assertSame(metodo, primeira.getMetodoPagamento());
		assertSame(metodo, segunda.getMetodoPagamento());
		assertSame(primeiraFatura, primeira.getFatura());
		assertSame(segundaFatura, segunda.getFatura());
	}
}
