package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarCompraParceladaBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarCompraParceladaBody;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ValoresParcelasInvalidosException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicoesTeste;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.CompraParceladaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;

import jakarta.persistence.EntityNotFoundException;

class CompraParceladaServiceTest {

	private CompraParceladaRepository repository;
	private MetodoPagamentoRepository metodoPagamentoRepository;
	private FaturaRepository faturaRepository;
	private CompraParceladaService service;
	private CompraParcelada compra;
	private UUID id;
	private MetodoPagamento metodoPagamento;

	@BeforeEach
	void configurar() {
		repository = mock(CompraParceladaRepository.class);
		metodoPagamentoRepository = mock(MetodoPagamentoRepository.class);
		final var metodoPagamentoService = new MetodoPagamentoService(
				metodoPagamentoRepository, mock(GastoRepository.class), mock(FaturaRepository.class),
				mock(InstituicaoFinanceiraService.class)
		);
		faturaRepository = mock(FaturaRepository.class);
		metodoPagamento = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 31, 7);
		metodoPagamento.setId(UUID.randomUUID());
		metodoPagamento.setInstituicaoFinanceira(InstituicoesTeste.nubank());
		metodoPagamento.setNome("Cartao Nubank");
		when(metodoPagamentoRepository.findById(metodoPagamento.getId())).thenReturn(Optional.of(metodoPagamento));
		when(metodoPagamentoRepository.findByIdForUpdate(metodoPagamento.getId()))
				.thenReturn(Optional.of(metodoPagamento));
		when(faturaRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
		final var faturaService = new FaturaService(faturaRepository, metodoPagamentoService);
		final var beans = new StaticListableBeanFactory();
		service = new CompraParceladaService(repository, metodoPagamentoService,
				new ParcelamentoComponent(faturaService), beans.getBeanProvider(CompraParceladaService.class));
		beans.addBean("compraParceladaService", service);
		when(repository.save(any(CompraParcelada.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		when(repository.saveAndFlush(any(CompraParcelada.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		compra = service.criar(new CriarCompraParceladaBody(
				LocalDate.of(2024, 1, 31), "Compra", new BigDecimal("100.00"), 3,
				"Casa", metodoPagamento.getId(), null, "Original", null
		));
		id = UUID.randomUUID();
		compra.setId(id);
		compra.getParcelas().forEach(parcela -> parcela.setId(UUID.randomUUID()));
		when(repository.findById(id)).thenReturn(Optional.of(compra));
	}

	@Test
	void criaComValoresIndividuaisERejeitaQuantidadeOuSomaDivergentes() {
		final var valores = List.of(BigDecimal.ZERO, new BigDecimal("30.10"), new BigDecimal("69.90"));
		final var criada = service.criar(new CriarCompraParceladaBody(
				LocalDate.of(2024, 1, 31), "Compra", new BigDecimal("100.00"), 3,
				"Casa", metodoPagamento.getId(), null, "Original", valores
		));
		assertEquals(valores, criada.getParcelas().stream().map(parcela -> parcela.getValor()).toList());

		for (final var invalidos : List.of(List.of(BigDecimal.ONE), List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))) {
			final var request = new CriarCompraParceladaBody(
					LocalDate.of(2024, 1, 31), "Compra", new BigDecimal("100.00"), 3,
					"Casa", metodoPagamento.getId(), null, "Original", invalidos
			);
			final var erro = assertThrows(ValoresParcelasInvalidosException.class, () -> service.criar(request));
			assertEquals(invalidos.size() == 3 ? "SOMA_PARCELAS_DIFERENTE_DO_TOTAL"
					: "QUANTIDADE_VALORES_PARCELAS_INVALIDA", erro.getMessage());
		}
	}

	@Test
	void usaFaturaInicialSelecionadaENaoASugerida() {
		final var request = new AtualizarCompraParceladaBody(
				"Novo", "Outra", metodoPagamento.getId(), YearMonth.of(2024, 5),
				null, new BigDecimal("100.00"), 3, LocalDate.of(2024, 1, 31), null, true
		);

		service.atualizar(id, request);

		assertEquals(List.of(LocalDate.of(2024, 5, 31), LocalDate.of(2024, 6, 30), LocalDate.of(2024, 7, 31)),
				compra.getParcelas().stream().map(parcela -> parcela.getFatura().getDataFechamento()).toList());
	}

	@Test
	void rejeitaMetodoNaoCreditoOuAusenciaDeDataEFatura() {
		final var request = request("100.00", 3);
		for (final var tipo : TipoMetodoPagamento.values()) {
			if (tipo == TipoMetodoPagamento.CREDITO) {
				continue;
			}
			final var metodo = MetodoPagamento.criar(tipo, null, null);
			when(metodoPagamentoRepository.findById(metodoPagamento.getId())).thenReturn(Optional.of(metodo));
			assertEquals("COMPRA_PARCELADA_EXIGE_CREDITO",
					assertThrows(PagamentoInvalidoException.class, () -> service.atualizar(id, request)).getMessage());
		}

		when(metodoPagamentoRepository.findById(metodoPagamento.getId())).thenReturn(Optional.of(metodoPagamento));
		final var semData = request("100.00", 3, null);
		assertEquals("FATURA_OBRIGATORIA_SEM_DATA_DA_COMPRA",
				assertThrows(PagamentoInvalidoException.class, () -> service.atualizar(id, semData)).getMessage());
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void alteraValoresPorNumeroDaParcelaERecalculaQuandoOmitidosSemRecriar() {
		final var antigas = List.copyOf(compra.getParcelas());
		java.util.Collections.reverse(compra.getParcelas());
		final var valores = List.of(new BigDecimal("0.00"), new BigDecimal("30.10"), new BigDecimal("69.90"));

		service.atualizar(id, requestComValores("100.0", 3, valores));
		for (int indice = 0; indice < antigas.size(); indice++) {
			assertEquals(valores.get(indice), antigas.get(indice).getValor());
		}
		service.atualizar(id, request("100.00", 3));
		assertEquals(List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33")),
				antigas.stream().map(parcela -> parcela.getValor()).toList());
		assertTrue(compra.getParcelas().containsAll(antigas));
		verify(repository, never()).flush();
	}

	@Test
	void alteraRealizadoDeTodasAsParcelasInclusiveQuandoRecriadas() {
		for (final var quantidade : List.of(3, 4)) {
			for (final var realizado : List.of(false, true)) {
				service.atualizar(id, new AtualizarCompraParceladaBody(
						"Compra", "Casa", metodoPagamento.getId(), null, "Original",
						new BigDecimal("100.00"), quantidade, LocalDate.of(2024, 1, 31), null, realizado
				));
				assertEquals(quantidade, compra.getParcelas().size());
				assertTrue(compra.getParcelas().stream().allMatch(parcela -> parcela.isRealizado() == realizado));
			}
		}
	}

	@Test
	void recriaComValoresPersonalizadosERecalculaQuandoOmitidos() {
		final var valores = List.of(new BigDecimal("10.00"), new BigDecimal("20.00"),
				new BigDecimal("30.00"), new BigDecimal("60.00"));
		service.atualizar(id, requestComValores("120.00", 4, valores));
		assertEquals(valores,
				compra.getParcelas().stream().map(parcela -> Objects.requireNonNull(parcela).getValor()).toList());
		assertEquals(new BigDecimal("120.00"), compra.getValorTotal());
		assertEquals(List.of(LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 31),
				LocalDate.of(2024, 4, 30), LocalDate.of(2024, 5, 31)),
				compra.getParcelas().stream().map(parcela -> parcela.getFatura().getDataFechamento()).toList());

		service.atualizar(id, request("100.00", 4));
		assertTrue(compra.getParcelas().stream().allMatch(parcela ->
				parcela.getValor().compareTo(new BigDecimal("25.00")) == 0));
	}

	@Test
	void rejeitaQuantidadeESomaInvalidasAntesDeAlterarCompra() {
		final var parcelas = List.copyOf(compra.getParcelas());
		for (final var valores : List.of(List.<BigDecimal>of(), List.of(new BigDecimal("100.00")),
				List.of(BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("97.99")),
				List.of(BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("98.01")))) {
			final var request = requestComValores("100.00", 3, valores);
			final var erro = assertThrows(ValoresParcelasInvalidosException.class,
					() -> service.atualizar(id, request));
			assertEquals(valores.size() == 3 
                    ? "SOMA_PARCELAS_DIFERENTE_DO_TOTAL" 
                    : "QUANTIDADE_VALORES_PARCELAS_INVALIDA"
                , erro.getMessage()
            );
		}
		assertEquals(parcelas, compra.getParcelas());
		assertEquals("Compra", parcelas.get(0).getNome());
		assertEquals(new BigDecimal("33.34"), parcelas.get(0).getValor());
		assertEquals(new BigDecimal("100.00"), compra.getValorTotal());
		verify(repository, never()).flush();
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void criaParcelasNumeradasComDatasEValoresCorretos() {
		final var parcelas = compra.getParcelas();
		assertTrue(parcelas.stream().allMatch(parcela -> LocalDate.of(2024, 1, 31).equals(parcela.getData())));
		assertEquals(List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33")),
				parcelas.stream().map(parcela -> Objects.requireNonNull(parcela).getValor()).toList());
		assertEquals(List.of(1, 2, 3),
				parcelas.stream().map(parcela -> Objects.requireNonNull(parcela).getNumeroParcela()).toList());
		assertTrue(parcelas.stream().allMatch(parcela -> parcela.isRealizado() && parcela.getCompraParcelada() == compra));
		assertEquals(List.of(LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 31), LocalDate.of(2024, 4, 30)),
				parcelas.stream().map(parcela -> Objects.requireNonNull(parcela).getFatura().getDataFechamento()).toList());
	}

	@Test
	void criaParcelasPlanejadasQuandoACompraAindaNaoFoiRealizada() {
		final var planejada = service.criar(new CriarCompraParceladaBody(
				LocalDate.of(2024, 5, 10), "Viagem", new BigDecimal("300.00"), 3,
				"Lazer", metodoPagamento.getId(), null, null, null, false
		));

		assertEquals(3, planejada.getParcelas().size());
		assertTrue(planejada.getParcelas().stream().noneMatch(parcela -> parcela.isRealizado()));
	}

	@Test
	void alteraDadosDeTodasSemAlterarDatas() {
		final var parcelas = List.copyOf(compra.getParcelas());
		final var request = request("100.0", 3);
		assertSame(compra, service.atualizar(id, request));
		assertEquals(parcelas, compra.getParcelas());
		assertTrue(parcelas.stream().allMatch(parcela -> LocalDate.of(2024, 1, 31).equals(parcela.getData())));
		assertTrue(parcelas.stream().allMatch(p -> p.getNome().equals("Novo") && p.getCategoria().equals("Outra")
				&& p.getMetodoPagamento() == metodoPagamento && p.getDescricao() == null));
		verify(repository, never()).flush();
	}

	@Test
	void recriaParcelasMantendoMesInicial() {
		final var antigas = List.copyOf(compra.getParcelas());
		final var request = request("100.01", 4);
		java.util.Collections.reverse(compra.getParcelas());
		service.atualizar(id, request);
		final var novas = compra.getParcelas();
		assertEquals(4, novas.size());
		assertNotSame(antigas.get(0), novas.get(0));
		assertEquals(List.of(new BigDecimal("25.01"), new BigDecimal("25.00"),
				new BigDecimal("25.00"), new BigDecimal("25.00")),
				novas.stream().map(parcela -> Objects.requireNonNull(parcela).getValor()).toList());
		assertTrue(novas.stream().allMatch(parcela -> LocalDate.of(2024, 1, 31).equals(parcela.getData())));
		assertTrue(novas.stream().allMatch(p -> p.isRealizado() && p.getCompraParcelada() == compra));
		verify(repository).flush();
	}

	@Test
	void reduzQuantidadeMantendoMesInicial() {
		service.atualizar(id, request("100.00", 2));
		assertTrue(compra.getParcelas().stream().allMatch(parcela -> LocalDate.of(2024, 1, 31).equals(parcela.getData())));
	}

	@Test
	void alteraDadosSemDataERecriaPorValorSemAlteracaoIndividual() {
		final var parcelas = List.copyOf(compra.getParcelas());
		service.atualizar(id, request("100.00", 3));
		assertSame(parcelas.get(0), compra.getParcelas().get(0));
		assertEquals("Novo", parcelas.get(0).getNome());
		service.atualizar(id, request("120.00", 3));
		assertNotSame(parcelas.get(0), compra.getParcelas().get(0));
		assertEquals(List.of(new BigDecimal("40.00"), new BigDecimal("40.00"), new BigDecimal("40.00")),
				compra.getParcelas().stream().map(parcela -> Objects.requireNonNull(parcela).getValor()).toList());
		verify(repository).flush();
	}

	@Test
	void alteraDataDeTodasSemRecriarEAvancaSomenteFaturasMesmoForaDeOrdem() {
		final var antigas = List.copyOf(compra.getParcelas());
		java.util.Collections.reverse(compra.getParcelas());
		final var data = LocalDate.of(2024, 12, 31);
		service.atualizar(id, request("100.00", 3, data));
		assertTrue(antigas.stream().allMatch(parcela -> data.equals(parcela.getData())));
		assertEquals(List.of(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 31)),
				antigas.stream().map(parcela -> parcela.getFatura().getDataFechamento()).toList());
		verify(repository, never()).flush();
	}

	@Test
	void recriaComNovaDataEQuantidade() {
		service.atualizar(id, request("120.00", 4, LocalDate.of(2025, 1, 31)));
		assertTrue(compra.getParcelas().stream().allMatch(parcela -> LocalDate.of(2025, 1, 31).equals(parcela.getData())));
		verify(repository).flush();
	}

	@Test
	void rejeitaCompraInexistenteERecriaParcelasSemDependerDaNumeracaoAnterior() {
		final var inexistente = UUID.randomUUID();
		compra.getParcelas().get(0).setNumeroParcela(4);
		final var parcelas = List.copyOf(compra.getParcelas());
		final var requestExistente = request("100.00", 3);
		final var requestNovoValor = request("101.00", 3);
		assertThrows(EntityNotFoundException.class, () -> service.atualizar(inexistente, requestExistente));
		assertEquals(parcelas, compra.getParcelas());
		assertEquals(new BigDecimal("100.00"), compra.getValorTotal());
		verify(repository, never()).flush();

		service.atualizar(id, requestNovoValor);

		assertEquals(List.of(1, 2, 3),
				compra.getParcelas().stream().map(parcela -> Objects.requireNonNull(parcela).getNumeroParcela()).toList());
		assertEquals(new BigDecimal("101.00"), compra.getValorTotal());
		verify(repository).flush();
	}

	@Test
	void excluiCompraOuInformaQuandoNaoExiste() {
		service.excluir(id);
		verify(repository).delete(compra);
		final var inexistente = UUID.randomUUID();
		assertThrows(EntityNotFoundException.class, () -> service.excluir(inexistente));
		assertFalse(compra.getParcelas().isEmpty());
	}

	@Test
	void consultaCompraPorId() {
		assertSame(compra, service.buscarPorId(id));
		final var inexistente = UUID.randomUUID();
		assertThrows(EntityNotFoundException.class, () -> service.buscarPorId(inexistente));
	}

	private AtualizarCompraParceladaBody requestComValores(String total, int quantidade, List<BigDecimal> valores) {
		return new AtualizarCompraParceladaBody(
				"Novo", "Outra", metodoPagamento.getId(), null, null, new BigDecimal(total),
				quantidade, LocalDate.of(2024, 1, 31), valores, true
		);
	}

	private AtualizarCompraParceladaBody request(String total, int quantidade) {
		return request(total, quantidade, LocalDate.of(2024, 1, 31));
	}

	private AtualizarCompraParceladaBody request(String total, int quantidade, LocalDate data) {
		return new AtualizarCompraParceladaBody(
				"Novo", "Outra", metodoPagamento.getId(), null, null, new BigDecimal(total), quantidade, data, null, true
		);
	}
}
