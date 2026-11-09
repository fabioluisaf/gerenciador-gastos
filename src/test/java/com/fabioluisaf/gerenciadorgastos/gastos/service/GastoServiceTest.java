package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.BuscarGastosPorMesEAnoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ParcelaOperacaoInvalidaException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;

import jakarta.persistence.EntityNotFoundException;

class GastoServiceTest {

	private GastoRepository repository;
	private GastoService service;

	@BeforeEach
	void configurar() {
		repository = mock(GastoRepository.class);
		service = new GastoService(repository);
	}

	@Test
	void criaGastoComDadosDaRequisicao() {
		final var request = new CriarGastoRequest(
				LocalDate.of(2026, 2, 15), "Mercado", new BigDecimal("25.50"), "Alimentacao",
				MetodoPagamento.PIX_CAIXA, "compra", false
		);
		when(repository.save(any(Gasto.class))).thenAnswer(invocation -> invocation.getArgument(0));
		final var gasto = service.criar(request);
		assertNull(gasto.getId());
		assertEquals(request.data(), gasto.getData());
		assertEquals(request.nome(), gasto.getNome());
		assertEquals(request.valor(), gasto.getValor());
		assertEquals(request.categoria(), gasto.getCategoria());
		assertEquals(request.metodoPagamento(), gasto.getMetodoPagamento());
		assertEquals(request.descricao(), gasto.getDescricao());
		assertEquals(request.realizado(), gasto.isRealizado());
		verify(repository).save(gasto);
	}

	@Test
	void consultaMesBissextoComPaginacao() {
		final var pageable = PageRequest.of(1, 10);
		final var pagina = new PageImpl<Gasto>(List.of(), pageable, 12);
		when(repository.findByDataBetween(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), pageable))
				.thenReturn(pagina);
		assertSame(pagina, service.listarPorMesEAno(new BuscarGastosPorMesEAnoRequest(2, 2024), pageable));
	}

	@Test
	void buscaAtualizaEExcluiGastoExistente() {
		final var id = UUID.randomUUID();
		final var gasto = new Gasto();
		gasto.setId(id);
		when(repository.findById(id)).thenReturn(Optional.of(gasto));
		when(repository.save(gasto)).thenReturn(gasto);
		final var request = new AtualizarGastoRequest(
				LocalDate.of(2026, 2, 16), "Novo", BigDecimal.ZERO, "Casa",
				MetodoPagamento.DEBITO_NUBANK, "atualizado", true
		);
		assertSame(gasto, service.buscarPorId(id));
		assertSame(gasto, service.atualizar(id, request));
		assertEquals(id, gasto.getId());
		assertEquals(request.data(), gasto.getData());
		assertEquals(request.nome(), gasto.getNome());
		assertEquals(request.valor(), gasto.getValor());
		assertEquals(request.categoria(), gasto.getCategoria());
		assertEquals(request.metodoPagamento(), gasto.getMetodoPagamento());
		assertEquals(request.descricao(), gasto.getDescricao());
		assertEquals(request.realizado(), gasto.isRealizado());
		service.excluir(id);
		verify(repository).save(gasto);
		verify(repository).delete(gasto);
	}

	@Test
	void impedeAlteracaoEExclusaoDeParcelaPeloCrudDeGastos() {
		final var id = UUID.randomUUID();
		final var parcela = new Gasto();
		parcela.setCompraParcelada(new CompraParcelada());
		when(repository.findById(id)).thenReturn(Optional.of(parcela));
		assertThrows(ParcelaOperacaoInvalidaException.class, () -> service.atualizar(id, null));
		assertThrows(ParcelaOperacaoInvalidaException.class, () -> service.excluir(id));
		verify(repository, never()).save(parcela);
		verify(repository, never()).delete(parcela);
	}

	@Test
	void lancaExcecaoQuandoGastoNaoExiste() {
		final var id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());
		assertThrows(EntityNotFoundException.class, () -> service.buscarPorId(id));
		assertThrows(EntityNotFoundException.class, () -> service.atualizar(id, null));
		assertThrows(EntityNotFoundException.class, () -> service.excluir(id));
	}
}
