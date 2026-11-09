package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.GastoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.service.CompraParceladaService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;

import jakarta.persistence.EntityManager;

@EnabledIfEnvironmentVariable(named = "TESTAR_POSTGRES_ISOLADO", matches = "true")
@SpringBootTest(properties = {
		"spring.jpa.open-in-view=false",
		"spring.jpa.properties.hibernate.generate_statistics=true"
})
@Transactional
class GastoRepositoryPostgresTest {

	@Autowired
	private GastoRepository repository;
	@Autowired
	private CompraParceladaService compraService;
	@Autowired
	private GastoService gastoService;
	@Autowired
	private EntityManager entityManager;

	@Test
	void persisteDatasNaCriacaoEdicaoERecriacao() {
		final var compra = compraService.criar(new CriarCompraParceladaRequest(
				LocalDate.of(2024, 1, 31), "Compra", new BigDecimal("90.00"), 3,
				"Casa", MetodoPagamento.CREDITO_NUBANK, null
		));
		entityManager.flush();
		final var id = compra.getId();
		entityManager.clear();
		final var existente = compraService.buscarPorId(id);
		assertEquals(LocalDate.of(2024, 1, 31), existente.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 1).findFirst().orElseThrow().getData());

		compraService.atualizar(id, new AtualizarCompraParceladaRequest(
				"Compra", "Casa", MetodoPagamento.CREDITO_NUBANK, null, new BigDecimal("90.00"), 3,
				LocalDate.of(2025, 12, 31)
		));
		entityManager.clear();
		final var editada = compraService.buscarPorId(id);
		assertEquals(LocalDate.of(2025, 12, 31), editada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 1).findFirst().orElseThrow().getData());
		assertEquals(LocalDate.of(2026, 1, 1), editada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 2).findFirst().orElseThrow().getData());

		compraService.atualizar(id, new AtualizarCompraParceladaRequest(
				"Compra", "Casa", MetodoPagamento.CREDITO_NUBANK, null, new BigDecimal("100.00"), 2,
				LocalDate.of(2026, 1, 31)
		));
		entityManager.clear();
		final var recriada = compraService.buscarPorId(id);
		assertEquals(2, recriada.getParcelas().size());
		assertEquals(LocalDate.of(2026, 1, 31), recriada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 1).findFirst().orElseThrow().getData());
		assertEquals(LocalDate.of(2026, 2, 1), recriada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 2).findFirst().orElseThrow().getData());
	}

	@Test
	void carregaDadosDaCompraSemConsultasAdicionaisPorParcela() {
		final var compra = compraService.criar(new CriarCompraParceladaRequest(
				LocalDate.of(2026, 2, 1), "Compra", new BigDecimal("90.00"), 3,
				"Casa", MetodoPagamento.CREDITO_NUBANK, null
		));
		compraService.criar(new CriarCompraParceladaRequest(
				LocalDate.of(2026, 2, 1), "Outra compra", new BigDecimal("20.00"), 2,
				"Casa", MetodoPagamento.CREDITO_NUBANK, null
		));
		final var avulso = gastoService.criar(new CriarGastoRequest(
				LocalDate.of(2026, 2, 2), "Avulso", BigDecimal.ONE, "Casa",
				MetodoPagamento.PIX_CAIXA, null, true
		));
		entityManager.flush();
		final var compraId = compra.getId();
		final var avulsoId = avulso.getId();
		entityManager.clear();

		final var estatisticas = entityManager.getEntityManagerFactory()
				.unwrap(SessionFactory.class).getStatistics();
		estatisticas.clear();
		final var pagina = repository.findByDataBetween(
				LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), PageRequest.of(0, 10)
		);
		assertEquals(3, pagina.getTotalElements());
		entityManager.clear();
		final var respostas = pagina.getContent().stream().map(GastoResponse::from).toList();
		final var parcela = respostas.stream().filter(resposta -> resposta.parcela() != null
				&& resposta.parcela().compraParceladaId().equals(compraId)).findFirst().orElseThrow();
		final var gastoAvulso = respostas.stream().filter(resposta -> resposta.id().equals(avulsoId))
				.findFirst().orElseThrow();
		assertNotNull(parcela.parcela());
		assertEquals(1, parcela.parcela().numero());
		assertEquals(3L, parcela.parcela().quantidade());
		assertEquals(compraId, parcela.parcela().compraParceladaId());
		assertNull(gastoAvulso.parcela());
		assertEquals(1, estatisticas.getPrepareStatementCount());

		estatisticas.clear();
		final var primeiraPagina = repository.findByDataBetween(
				LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), PageRequest.of(0, 1)
		);
		assertEquals(3, primeiraPagina.getTotalElements());
		entityManager.clear();
		primeiraPagina.getContent().stream().map(GastoResponse::from).toList();
		assertEquals(2, estatisticas.getPrepareStatementCount());

		estatisticas.clear();
		final var gasto = repository.findById(parcela.id()).orElseThrow();
		entityManager.clear();
		assertEquals(3L, GastoResponse.from(gasto).parcela().quantidade());
		assertTrue(estatisticas.getPrepareStatementCount() <= 1);
	}
}
