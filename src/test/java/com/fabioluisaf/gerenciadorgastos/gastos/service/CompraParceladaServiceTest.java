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
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.CriarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.CompraParceladaRepository;

import jakarta.persistence.EntityNotFoundException;

class CompraParceladaServiceTest {

	private CompraParceladaRepository repository;
	private CompraParceladaService service;
	private CompraParcelada compra;
	private UUID id;

	@BeforeEach
	void configurar() {
		repository = mock(CompraParceladaRepository.class);
		service = new CompraParceladaService(repository);
		when(repository.save(any(CompraParcelada.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		when(repository.saveAndFlush(any(CompraParcelada.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
		compra = service.criar(new CriarCompraParceladaRequest(
				LocalDate.of(2024, 1, 31), "Compra", new BigDecimal("100.00"), 3,
				"Casa", MetodoPagamento.CREDITO_NUBANK, "Original"
		));
		id = UUID.randomUUID();
		compra.setId(id);
		compra.getParcelas().forEach(parcela -> parcela.setId(UUID.randomUUID()));
		when(repository.findById(id)).thenReturn(Optional.of(compra));
	}

	@Test
	void criaParcelasNumeradasComDatasEValoresCorretos() {
		final var parcelas = compra.getParcelas();
		assertEquals(List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1)),
				parcelas.stream().map(parcela -> Objects.requireNonNull(parcela).getData()).toList());
		assertEquals(List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33")),
				parcelas.stream().map(parcela -> Objects.requireNonNull(parcela).getValor()).toList());
		assertEquals(List.of(1, 2, 3),
				parcelas.stream().map(parcela -> Objects.requireNonNull(parcela).getNumeroParcela()).toList());
		assertTrue(parcelas.stream().allMatch(parcela -> parcela.isRealizado() && parcela.getCompraParcelada() == compra));
	}

	@Test
	void alteraDadosDeTodasSemAlterarDatas() {
		final var parcelas = List.copyOf(compra.getParcelas());
		final var request = request("100.0", 3);
		assertSame(compra, service.atualizar(id, request));
		assertEquals(parcelas, compra.getParcelas());
		assertEquals(List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1)),
				parcelas.stream().map(parcela -> Objects.requireNonNull(parcela).getData()).toList());
		assertTrue(parcelas.stream().allMatch(p -> p.getNome().equals("Novo") && p.getCategoria().equals("Outra")
				&& p.getMetodoPagamento() == MetodoPagamento.PIX_CAIXA && p.getDescricao() == null));
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
		assertEquals(List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
				LocalDate.of(2024, 3, 1), LocalDate.of(2024, 4, 1)),
				novas.stream().map(parcela -> Objects.requireNonNull(parcela).getData()).toList());
		assertTrue(novas.stream().allMatch(p -> p.isRealizado() && p.getCompraParcelada() == compra));
		verify(repository).flush();
	}

	@Test
	void reduzQuantidadeMantendoMesInicial() {
		service.atualizar(id, request("100.00", 2));
		assertEquals(List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1)),
				compra.getParcelas().stream().map(parcela -> Objects.requireNonNull(parcela).getData()).toList());
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
	void rejeitaCompraInexistenteOuRecriacaoSemPrimeiraParcela() {
		final var inexistente = UUID.randomUUID();
		compra.getParcelas().get(0).setNumeroParcela(4);
		final var parcelas = List.copyOf(compra.getParcelas());
		final var requestExistente = request("100.00", 3);
		final var requestNovoValor = request("101.00", 3);
		assertThrows(EntityNotFoundException.class, () -> service.atualizar(inexistente, requestExistente));
		assertThrows(IllegalStateException.class, () -> service.atualizar(id, requestNovoValor));
		assertEquals(parcelas, compra.getParcelas());
		assertEquals(new BigDecimal("100.00"), compra.getValorTotal());
		verify(repository, never()).flush();
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

	private AtualizarCompraParceladaRequest request(String total, int quantidade) {
		return new AtualizarCompraParceladaRequest("Novo", "Outra", MetodoPagamento.PIX_CAIXA, null,
				new BigDecimal(total), quantidade);
	}
}
