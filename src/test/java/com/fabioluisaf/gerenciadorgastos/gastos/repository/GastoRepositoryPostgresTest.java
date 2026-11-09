package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.metodopagamento.CriarMetodoPagamentoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.GastoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.service.CompraParceladaService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.MetodoPagamentoService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

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
	private MetodoPagamentoService metodoPagamentoService;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private FaturaRepository faturaRepository;

	@Test
	void resumeFaturasSomandoGastosEIncluindoFaturasVazias() {
		final var metodoId = criarCartao();
		final var outroMetodoId = criarCartao();
		final var data = LocalDate.of(2026, 11, 1);
		for (final var valor : List.of("10.20", "20.30")) {
			gastoService.criar(new CriarGastoRequest(
					data, "Gasto", new BigDecimal(valor), "Casa", metodoId, null, null, true
			));
		}
		gastoService.criar(new CriarGastoRequest(
				data, "Outro cartao", new BigDecimal("500.00"), "Casa", outroMetodoId, null, null, true
		));
		final var vazia = new Fatura();
		vazia.setMetodoPagamento(entityManager.find(MetodoPagamento.class, metodoId));
		vazia.setDataFechamento(LocalDate.of(2026, 12, 31));
		entityManager.persist(vazia);
		entityManager.flush();
		entityManager.clear();
		final var estatisticas = entityManager.getEntityManagerFactory()
				.unwrap(SessionFactory.class).getStatistics();
		estatisticas.clear();

		final var pagina = faturaRepository.listarResumosPorMetodoPagamentoId(
				metodoId, PageRequest.of(0, 1, Sort.by("dataFechamento", "id"))
		);
		final var segundaPagina = faturaRepository.listarResumosPorMetodoPagamentoId(
				metodoId, PageRequest.of(1, 1, Sort.by("dataFechamento", "id"))
		);
		final var resumos = pagina.getContent();

		assertEquals(2, pagina.getTotalElements());
		assertEquals(2, pagina.getTotalPages());
		assertEquals(1, resumos.size());
		assertEquals(LocalDate.of(2026, 11, 30), resumos.get(0).dataFechamento());
		assertEquals(0, new BigDecimal("30.50").compareTo(resumos.get(0).valorTotal()));
		assertEquals(vazia.getId(), segundaPagina.getContent().get(0).id());
		assertEquals(0, BigDecimal.ZERO.compareTo(segundaPagina.getContent().get(0).valorTotal()));
		assertEquals(4, estatisticas.getPrepareStatementCount());
	}

	@Test
	void impedeDuasFaturasDoMesmoMetodoNoMesmoMes() {
		final var metodo = entityManager.find(MetodoPagamento.class, criarCartao());
		final var outroMetodo = entityManager.find(MetodoPagamento.class, criarCartao());
		for (final var data : List.of(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 12, 10))) {
			final var fatura = new Fatura();
			fatura.setMetodoPagamento(metodo);
			fatura.setDataFechamento(data);
			entityManager.persist(fatura);
		}
		final var outraFatura = new Fatura();
		outraFatura.setMetodoPagamento(outroMetodo);
		outraFatura.setDataFechamento(LocalDate.of(2026, 11, 10));
		entityManager.persist(outraFatura);
		entityManager.flush();

		final var duplicada = new Fatura();
		duplicada.setMetodoPagamento(metodo);
		duplicada.setDataFechamento(LocalDate.of(2026, 11, 15));
		entityManager.persist(duplicada);

		assertThrows(PersistenceException.class, () -> entityManager.flush());
	}

	@Test
	void persisteDatasNaCriacaoEdicaoERecriacao() {
		final var metodoPagamentoId = criarCartao();
		final var compra = compraService.criar(new CriarCompraParceladaRequest(
				LocalDate.of(2024, 1, 31), "Compra", new BigDecimal("90.00"), 3,
				"Casa", metodoPagamentoId, null, null
		));
		entityManager.flush();
		final var id = compra.getId();
		entityManager.clear();
		final var existente = compraService.buscarPorId(id);
		assertEquals(LocalDate.of(2024, 1, 31), existente.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 1).findFirst().orElseThrow().getData());

		compraService.atualizar(id, new AtualizarCompraParceladaRequest(
				"Compra", "Casa", metodoPagamentoId, null, null, new BigDecimal("90.00"), 3,
				LocalDate.of(2025, 12, 31),
				List.of(new BigDecimal("10.00"), new BigDecimal("30.00"), new BigDecimal("50.00"))
		));
		entityManager.clear();
		final var editada = compraService.buscarPorId(id);
		assertEquals(LocalDate.of(2025, 12, 31), editada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 1).findFirst().orElseThrow().getData());
		assertEquals(LocalDate.of(2026, 1, 1), editada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 2).findFirst().orElseThrow().getData());
		assertEquals(new BigDecimal("10.00"), editada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 1).findFirst().orElseThrow().getValor());
		assertEquals(new BigDecimal("50.00"), editada.getParcelas().stream()
				.filter(parcela -> parcela.getNumeroParcela() == 3).findFirst().orElseThrow().getValor());

		compraService.atualizar(id, new AtualizarCompraParceladaRequest(
				"Compra", "Casa", metodoPagamentoId, null, null, new BigDecimal("100.00"), 2,
				LocalDate.of(2026, 1, 31), null
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
		final var metodoPagamentoId = criarCartao();
		final var pixId = metodoPagamentoService.criar(new CriarMetodoPagamentoRequest(
				TipoMetodoPagamento.PIX, InstituicaoFinanceira.CAIXA, "Pix Caixa", null
		)).getId();
		final var compra = compraService.criar(new CriarCompraParceladaRequest(
				LocalDate.of(2026, 2, 1), "Compra", new BigDecimal("90.00"), 3,
				"Casa", metodoPagamentoId, null, null
		));
		compraService.criar(new CriarCompraParceladaRequest(
				LocalDate.of(2026, 2, 1), "Outra compra", new BigDecimal("20.00"), 2,
				"Casa", metodoPagamentoId, null, null
		));
		final var avulso = gastoService.criar(new CriarGastoRequest(
				LocalDate.of(2026, 2, 2), "Avulso", BigDecimal.ONE, "Casa",
				pixId, null, null, true
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

	private UUID criarCartao() {
		return metodoPagamentoService.criar(new CriarMetodoPagamentoRequest(
				TipoMetodoPagamento.CREDITO, InstituicaoFinanceira.NUBANK, "Cartao Nubank", 31
		)).getId();
	}
}
