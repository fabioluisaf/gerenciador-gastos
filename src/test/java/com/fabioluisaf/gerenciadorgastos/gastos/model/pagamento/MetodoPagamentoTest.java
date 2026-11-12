package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Boleto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Credito;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Debito;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Doc;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Pix;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Ted;

class MetodoPagamentoTest {

	@Test
	void fabricaCriaSubclasseCorrespondenteAoTipo() {
		final var credito = assertInstanceOf(Credito.class, MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 10, 17));
		assertEquals(TipoMetodoPagamento.CREDITO, credito.getTipo());
		assertEquals(10, credito.getDiaFechamento());
		assertEquals(17, credito.getDiaVencimento());
		assertInstanceOf(Debito.class, MetodoPagamento.criar(TipoMetodoPagamento.DEBITO, null, null));
		assertInstanceOf(Pix.class, MetodoPagamento.criar(TipoMetodoPagamento.PIX, null, null));
		assertInstanceOf(Boleto.class, MetodoPagamento.criar(TipoMetodoPagamento.BOLETO, null, null));
		assertInstanceOf(Doc.class, MetodoPagamento.criar(TipoMetodoPagamento.DOC, null, null));
		assertInstanceOf(Ted.class, MetodoPagamento.criar(TipoMetodoPagamento.TED, null, null));
		assertThrows(IllegalArgumentException.class, () -> MetodoPagamento.criar(null, null, null));
	}

	@Test
	void creditoExigeDiaValidoNaCriacaoENaAtualizacao() {
		final var credito = new Credito(1, 8);
		for (final Integer dia : new Integer[] { null, 0, 32 }) {
			assertThrows(PagamentoInvalidoException.class, () -> new Credito(dia, 8));
			assertThrows(PagamentoInvalidoException.class,
					() -> MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, dia, 8));
			assertThrows(PagamentoInvalidoException.class, () -> credito.atualizarDiaFechamento(dia));
			assertEquals(1, credito.getDiaFechamento());
			assertThrows(PagamentoInvalidoException.class, () -> new Credito(1, dia));
			assertThrows(PagamentoInvalidoException.class,
					() -> MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 1, dia));
			assertThrows(PagamentoInvalidoException.class, () -> credito.atualizarDiaVencimento(dia));
			assertEquals(8, credito.getDiaVencimento());
		}
		credito.atualizarDiaFechamento(31);
		assertEquals(31, credito.getDiaFechamento());
		credito.atualizarDiaVencimento(1);
		assertEquals(1, credito.getDiaVencimento());
		credito.atualizarDiaVencimento(31);
		assertEquals(31, credito.getDiaVencimento());
	}

	@Test
	void metodosNaoCreditoNaoAceitamDiaDeFechamento() {
		for (final var tipo : TipoMetodoPagamento.values()) {
			if (tipo == TipoMetodoPagamento.CREDITO) {
				continue;
			}
			final var metodo = MetodoPagamento.criar(tipo, null, null);
			assertEquals(tipo, metodo.getTipo());
			assertNull(metodo.getDiaFechamento());
			metodo.atualizarDiaFechamento(null);
			assertThrows(PagamentoInvalidoException.class, () -> MetodoPagamento.criar(tipo, 10, null));
			assertThrows(PagamentoInvalidoException.class, () -> metodo.atualizarDiaFechamento(10));
			assertNull(metodo.getDiaVencimento());
			metodo.atualizarDiaVencimento(null);
			assertThrows(PagamentoInvalidoException.class, () -> MetodoPagamento.criar(tipo, null, 17));
			assertThrows(PagamentoInvalidoException.class, () -> metodo.atualizarDiaVencimento(17));
		}
	}

	@Test
	// Chamadas diretas exercitam equals inclusive para a mesma instancia, null e outro tipo.
	@SuppressWarnings({ "java:S5785", "unlikely-arg-type" })
	void identidadePermaneceBaseadaNoIdEmTodaAHierarquia() {
		final var credito = new Credito(10, 17);
		final var outro = new Credito(20, 27);
		assertTrue(credito.equals(credito));
		assertFalse(credito.equals(null));
		assertFalse(credito.equals("credito"));
		assertNotEquals(credito, outro);
		final var hash = credito.hashCode();
		credito.setId(UUID.randomUUID());
		assertNotEquals(credito, outro);
		outro.setId(UUID.randomUUID());
		assertNotEquals(credito, outro);
		outro.setId(credito.getId());
		assertEquals(credito, outro);
		assertEquals(outro, credito);
    assertEquals(hash, credito.hashCode());
		assertEquals(credito.hashCode(), outro.hashCode());
	}
}
