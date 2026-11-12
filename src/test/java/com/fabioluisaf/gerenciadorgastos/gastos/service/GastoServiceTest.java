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
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.GastoRealizadoSemDataException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ParcelaOperacaoInvalidaException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;

import jakarta.persistence.EntityNotFoundException;

class GastoServiceTest {

	private GastoRepository repository;
	private MetodoPagamentoService metodoPagamentoService;
	private FaturaService faturaService;
	private MetodoPagamento metodoPagamento;
	private GastoService service;

	@BeforeEach
	void configurar() {
		repository = mock(GastoRepository.class);
		metodoPagamentoService = mock(MetodoPagamentoService.class);
		faturaService = mock(FaturaService.class);
		metodoPagamento = MetodoPagamento.criar(TipoMetodoPagamento.PIX, null, null);
		metodoPagamento.setId(UUID.randomUUID());
		metodoPagamento.setInstituicaoFinanceira(InstituicaoFinanceira.CAIXA);
		metodoPagamento.setNome("Pix Caixa");
		service = new GastoService(repository, metodoPagamentoService, faturaService);
	}

	@Test
	void selecionaPeriodoExplicitoOuSugeridoParaCredito() {
		final var credito = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 10, 17);
		final var metodoId = UUID.randomUUID();
		final var periodo = YearMonth.of(2026, 12);
		final var data = LocalDate.of(2026, 11, 11);
		final var fatura = new Fatura();
		when(metodoPagamentoService.buscarEntidadePorId(metodoId)).thenReturn(credito);
		when(faturaService.sugerirPeriodo(credito, data)).thenReturn(periodo);
		when(faturaService.obterOuCriar(metodoId, periodo)).thenReturn(fatura);
		when(repository.save(any(Gasto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		final var explicito = new CriarGastoRequest(null, "Compra", BigDecimal.TEN, "Casa",
				metodoId, periodo, null, false);
		assertSame(fatura, service.criar(explicito).getFatura());
		final var sugerido = new CriarGastoRequest(data, "Compra", BigDecimal.TEN, "Casa",
				metodoId, null, null, true);
		assertSame(fatura, service.criar(sugerido).getFatura());

		final var semPeriodo = new CriarGastoRequest(null, "Compra", BigDecimal.TEN, "Casa",
				metodoId, null, null, false);
		assertEquals("FATURA_OBRIGATORIA_SEM_DATA_DA_COMPRA",
				assertThrows(PagamentoInvalidoException.class, () -> service.criar(semPeriodo)).getMessage());
		when(metodoPagamentoService.buscarEntidadePorId(metodoId)).thenReturn(metodoPagamento);
		assertEquals("FATURA_PERMITIDA_APENAS_PARA_CREDITO",
				assertThrows(PagamentoInvalidoException.class, () -> service.criar(explicito)).getMessage());
	}

	@Test
	void criaGastoComDadosDaRequisicao() {
		final var request = new CriarGastoRequest(
				null, "Mercado", new BigDecimal("25.50"), "Alimentacao",
				metodoPagamento.getId(), null, "compra", false
		);
		when(metodoPagamentoService.buscarEntidadePorId(metodoPagamento.getId())).thenReturn(metodoPagamento);
		when(repository.save(any(Gasto.class))).thenAnswer(invocation -> invocation.getArgument(0));
		final var gasto = service.criar(request);
		assertNull(gasto.getId());
		assertEquals(request.data(), gasto.getData());
		assertEquals(request.nome(), gasto.getNome());
		assertEquals(request.valor(), gasto.getValor());
		assertEquals(request.categoria(), gasto.getCategoria());
		assertEquals(metodoPagamento, gasto.getMetodoPagamento());
		assertEquals(request.descricao(), gasto.getDescricao());
		assertEquals(request.realizado(), gasto.isRealizado());
		verify(repository).save(gasto);
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
				metodoPagamento.getId(), null, "atualizado", true
		);
		when(metodoPagamentoService.buscarEntidadePorId(metodoPagamento.getId())).thenReturn(metodoPagamento);
		assertSame(gasto, service.buscarPorId(id));
		assertSame(gasto, service.atualizar(id, request));
		assertEquals(id, gasto.getId());
		assertEquals(request.data(), gasto.getData());
		assertEquals(request.nome(), gasto.getNome());
		assertEquals(request.valor(), gasto.getValor());
		assertEquals(request.categoria(), gasto.getCategoria());
		assertEquals(metodoPagamento, gasto.getMetodoPagamento());
		assertEquals(request.descricao(), gasto.getDescricao());
		assertEquals(request.realizado(), gasto.isRealizado());
		service.excluir(id);
		verify(repository).save(gasto);
		verify(repository).delete(gasto);
	}

	@Test
	void atualizaSomenteEstadoDeRealizacaoDoGasto() {
		final var id = UUID.randomUUID();
		final var gasto = new Gasto();
		gasto.setId(id);
		gasto.setNome("Mercado");
		gasto.setData(LocalDate.of(2026, 2, 15));
		gasto.setRealizado(false);
		when(repository.findById(id)).thenReturn(Optional.of(gasto));
		when(repository.save(gasto)).thenReturn(gasto);

		assertSame(gasto, service.atualizarRealizado(id, true));

		assertEquals("Mercado", gasto.getNome());
		assertEquals(true, gasto.isRealizado());
		verify(repository).save(gasto);
	}

	@Test
	void naoMarcaComoRealizadoGastoSemData() {
		final var id = UUID.randomUUID();
		final var gasto = new Gasto();
		gasto.setId(id);
		gasto.setRealizado(false);
		when(repository.findById(id)).thenReturn(Optional.of(gasto));

		assertThrows(GastoRealizadoSemDataException.class, () -> service.atualizarRealizado(id, true));

		verify(repository, never()).save(gasto);
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
		assertThrows(EntityNotFoundException.class, () -> service.atualizarRealizado(id, true));
		assertThrows(EntityNotFoundException.class, () -> service.excluir(id));
	}
}
