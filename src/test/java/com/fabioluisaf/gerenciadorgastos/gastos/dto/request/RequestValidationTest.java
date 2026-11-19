package com.fabioluisaf.gerenciadorgastos.gastos.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarCompraParceladaBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarGastoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarMetodoPagamentoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarCompraParceladaBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarMetodoPagamentoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicoesTeste;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

class RequestValidationTest {

	private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

	@BeforeEach
	void configurar() {
		validator.afterPropertiesSet();
	}

	@Test
	void aceitaLimitesDePrecisaoTamanhoEParcelas() {
		final var nome = "n".repeat(255);
		final var descricao = "d".repeat(255);
		final var valor = new BigDecimal("99999999999999999.99");
		final var metodoPagamentoId = UUID.randomUUID();

		for (final var request : List.of(
				new CriarGastoBody(LocalDate.of(2026, 1, 1), nome, valor, "Casa",
						metodoPagamentoId, null, descricao, false),
				new AtualizarGastoBody(LocalDate.of(2026, 1, 1), nome, valor, "Casa",
						metodoPagamentoId, null, descricao, false),
				new CriarCompraParceladaBody(LocalDate.of(2026, 1, 1), nome, valor, 1000,
						"Casa", metodoPagamentoId, null, descricao, null),
				new AtualizarCompraParceladaBody(nome, "Casa", metodoPagamentoId, null, descricao,
						valor, 1000, LocalDate.of(2026, 1, 1), null, false),
				new CriarMetodoPagamentoBody(TipoMetodoPagamento.PIX, InstituicoesTeste.NUBANK_ID,
						nome, null, null),
				new AtualizarMetodoPagamentoBody(InstituicoesTeste.NUBANK_ID, nome, null, null)
		)) {
			assertTrue(validator.validate(request).isEmpty(), request.getClass().getSimpleName());
		}
	}

	@Test
	void aceitaEstornoComValorNegativoApenasEmGastoAvulso() {
		final var estorno = new BigDecimal("-50.00");
		final var metodoPagamentoId = UUID.randomUUID();
		final var data = LocalDate.of(2026, 1, 1);

		assertTrue(validator.validate(new CriarGastoBody(data, "Estorno", estorno, "Casa",
				metodoPagamentoId, null, null, true)).isEmpty());
		assertTrue(validator.validate(new AtualizarGastoBody(data, "Estorno", estorno, "Casa",
				metodoPagamentoId, null, null, true)).isEmpty());

		final var compra = validator.validate(new CriarCompraParceladaBody(data, "Compra", estorno, 2,
				"Casa", metodoPagamentoId, null, null, null));
		assertEquals(Set.of("valorTotal"), compra.stream()
				.map(violacao -> violacao.getPropertyPath().toString()).collect(Collectors.toSet()));
		final var atualizacao = validator.validate(new AtualizarCompraParceladaBody("Compra", "Casa",
				metodoPagamentoId, null, null, estorno, 2, data, null, true));
		assertEquals(Set.of("valorTotal"), atualizacao.stream()
				.map(violacao -> violacao.getPropertyPath().toString()).collect(Collectors.toSet()));
	}

	@Test
	void rejeitaValoresForaDosLimites() {
		final var nomeLongo = "n".repeat(256);
		final var descricaoLonga = "d".repeat(256);
		final var metodoPagamentoId = UUID.randomUUID();

		assertViolacoes(
				new CriarGastoBody(LocalDate.of(2026, 1, 1), nomeLongo, new BigDecimal("1.001"),
						"Casa", metodoPagamentoId, null, descricaoLonga, false),
				Set.of("nome", "valor", "descricao")
		);
		assertViolacoes(
				new AtualizarGastoBody(LocalDate.of(2026, 1, 1), nomeLongo, new BigDecimal("1.001"),
						"Casa", metodoPagamentoId, null, descricaoLonga, false),
				Set.of("nome", "valor", "descricao")
		);
		assertViolacoes(
				new CriarCompraParceladaBody(LocalDate.of(2026, 1, 1), nomeLongo, BigDecimal.ONE, 1001,
						"Casa", metodoPagamentoId, null, descricaoLonga, null),
				Set.of("nome", "descricao", "quantidadeParcelas")
		);
		assertViolacoes(
				new AtualizarCompraParceladaBody(nomeLongo, "Casa", metodoPagamentoId, null, descricaoLonga,
						BigDecimal.ONE, 1001, LocalDate.of(2026, 1, 1), null, true),
				Set.of("nome", "descricao", "quantidadeParcelas")
		);
		assertViolacoes(
				new CriarMetodoPagamentoBody(TipoMetodoPagamento.PIX, InstituicoesTeste.NUBANK_ID,
						nomeLongo, null, null),
				Set.of("nome")
		);
		assertViolacoes(
				new AtualizarMetodoPagamentoBody(InstituicoesTeste.NUBANK_ID, nomeLongo, null, null),
				Set.of("nome")
		);
	}

	private void assertViolacoes(Object request, Set<String> esperadas) {
		final var obtidas = validator.validate(request).stream()
				.map(violacao -> violacao.getPropertyPath().toString())
				.collect(Collectors.toSet());

		assertEquals(esperadas, obtidas,
				() -> request.getClass().getSimpleName() + ": " + obtidas);
	}

	@Test
	void validaValoresIndividuaisNaCriacao() {
		for (final var valor : java.util.Arrays.asList(null, new BigDecimal("-0.01"),
				new BigDecimal("0.001"), new BigDecimal("100000000000000000.00"))) {
			assertViolacoes(new CriarCompraParceladaBody(
					LocalDate.of(2026, 1, 1), "Compra", BigDecimal.ONE, 1, "Casa", UUID.randomUUID(),
					null, null, java.util.Collections.singletonList(valor)
			), Set.of("valoresParcelas[0].<list element>"));
		}
		assertTrue(validator.validate(new CriarCompraParceladaBody(
				LocalDate.of(2026, 1, 1), "Compra", new BigDecimal("1.23"), 2, "Casa", UUID.randomUUID(),
				null, null, List.of(BigDecimal.ZERO, new BigDecimal("1.23"))
		)).isEmpty());
	}
}
