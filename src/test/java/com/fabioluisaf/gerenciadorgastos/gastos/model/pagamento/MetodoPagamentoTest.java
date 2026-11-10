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
		final var credito = assertInstanceOf(Credito.class, MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 10));
		assertEquals(TipoMetodoPagamento.CREDITO, credito.getTipo());
		assertEquals(10, credito.getDiaFechamento());
		assertInstanceOf(Debito.class, MetodoPagamento.criar(TipoMetodoPagamento.DEBITO, null));
		assertInstanceOf(Pix.class, MetodoPagamento.criar(TipoMetodoPagamento.PIX, null));
		assertInstanceOf(Boleto.class, MetodoPagamento.criar(TipoMetodoPagamento.BOLETO, null));
		assertInstanceOf(Doc.class, MetodoPagamento.criar(TipoMetodoPagamento.DOC, null));
		assertInstanceOf(Ted.class, MetodoPagamento.criar(TipoMetodoPagamento.TED, null));
		assertThrows(IllegalArgumentException.class, () -> MetodoPagamento.criar(null, null));
	}

	@Test
	void creditoExigeDiaValidoNaCriacaoENaAtualizacao() {
		final var credito = new Credito(1);
		for (final Integer dia : new Integer[] { null, 0, 32 }) {
			assertThrows(PagamentoInvalidoException.class, () -> new Credito(dia));
			assertThrows(PagamentoInvalidoException.class,
					() -> MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, dia));
			assertThrows(PagamentoInvalidoException.class, () -> credito.atualizarDiaFechamento(dia));
			assertEquals(1, credito.getDiaFechamento());
		}
		credito.atualizarDiaFechamento(31);
		assertEquals(31, credito.getDiaFechamento());
	}

	@Test
	void metodosNaoCreditoNaoAceitamDiaDeFechamento() {
		for (final var tipo : TipoMetodoPagamento.values()) {
			if (tipo == TipoMetodoPagamento.CREDITO) {
				continue;
			}
			final var metodo = MetodoPagamento.criar(tipo, null);
			assertEquals(tipo, metodo.getTipo());
			assertNull(metodo.getDiaFechamento());
			metodo.atualizarDiaFechamento(null);
			assertThrows(PagamentoInvalidoException.class, () -> MetodoPagamento.criar(tipo, 10));
			assertThrows(PagamentoInvalidoException.class, () -> metodo.atualizarDiaFechamento(10));
		}
	}

	@Test
	// Chamadas diretas exercitam equals inclusive para a mesma instancia, null e outro tipo.
	@SuppressWarnings({ "java:S5785", "unlikely-arg-type" })
	void identidadePermaneceBaseadaNoIdEmTodaAHierarquia() {
		final var credito = new Credito(10);
		final var outro = new Credito(20);
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
