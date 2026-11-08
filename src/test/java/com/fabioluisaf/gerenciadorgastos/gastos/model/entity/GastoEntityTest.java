package com.fabioluisaf.gerenciadorgastos.gastos.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

class GastoEntityTest {

	@Test
	void expoeOsDadosPelaInterface() {
		LocalDate data = LocalDate.of(2026, 11, 8);
		BigDecimal valor = new BigDecimal("25.50");
		Gasto gasto = new GastoEntity(null, data, "Almoço", valor, "Alimentação", MetodoPagamento.CREDITO_NUBANK,
				"Refeição");

		assertEquals(data, gasto.getData());
		assertEquals("Almoço", gasto.getNome());
		assertEquals(valor, gasto.getValor());
		assertEquals("Alimentação", gasto.getCategoria());
		assertEquals(MetodoPagamento.CREDITO_NUBANK, gasto.getMetodoPagamento());
		assertEquals("Refeição", gasto.getDescricao());
	}

	@Test
	void aceitaDescricaoAusente() {
		Gasto gasto = new GastoEntity(null, LocalDate.of(2026, 11, 8), "Almoço", new BigDecimal("25.50"), "Alimentação",
				MetodoPagamento.PIX_CAIXA, null);

		assertNull(gasto.getDescricao());
	}

	@Test
	void permiteCriarEAtualizarComMetodosGerados() {
		GastoEntity gasto = new GastoEntity();
		LocalDate data = LocalDate.of(2026, 11, 8);
		BigDecimal valor = new BigDecimal("25.50");
		UUID id = UUID.randomUUID();
		gasto.setId(id);
		gasto.setData(data);
		gasto.setNome("Almoço");
		gasto.setValor(valor);
		gasto.setCategoria("Alimentação");
		gasto.setMetodoPagamento(MetodoPagamento.DEBITO_CAIXA);
		gasto.setDescricao("Refeição");

		assertEquals(id, gasto.getId());
		assertEquals(data, gasto.getData());
		assertEquals("Almoço", gasto.getNome());
		assertEquals(valor, gasto.getValor());
		assertEquals("Alimentação", gasto.getCategoria());
		assertEquals(MetodoPagamento.DEBITO_CAIXA, gasto.getMetodoPagamento());
		assertEquals("Refeição", gasto.getDescricao());

		UUID outroId = UUID.randomUUID();
		GastoEntity completo = new GastoEntity(outroId, data, "Almoço", valor, "Alimentação",
				MetodoPagamento.CREDITO_CAIXA, "Refeição");
		assertEquals(outroId, completo.getId());
		assertEquals("Refeição", completo.getDescricao());
	}

	@Test
	void mapeiaMetodoPagamentoComoString() throws NoSuchFieldException {
		Enumerated enumerated = GastoEntity.class.getDeclaredField("metodoPagamento").getAnnotation(Enumerated.class);
		assertEquals(EnumType.STRING, enumerated.value());
	}

	@Test
	void comparaApenasIdsNaoNulos() {
		UUID id = UUID.randomUUID();
		GastoEntity primeiro = new GastoEntity();
		GastoEntity segundo = new GastoEntity();
		primeiro.setId(id);
		segundo.setId(id);
		primeiro.setNome("Primeiro");
		segundo.setNome("Segundo");

		assertEquals(primeiro, segundo);
		assertEquals(segundo, primeiro);
		assertEquals(primeiro.hashCode(), segundo.hashCode());
		segundo.setId(UUID.randomUUID());
		assertNotEquals(primeiro, segundo);
		assertNotEquals(primeiro, null);
		assertNotEquals(primeiro, "outro tipo");
	}

	@Test
	void entidadesSemIdSoSaoIguaisAElaMesmas() {
		GastoEntity primeiro = new GastoEntity();
		GastoEntity segundo = new GastoEntity();

		assertEquals(primeiro, primeiro);
		assertNotEquals(primeiro, segundo);
	}

	@Test
	void hashPermaneceEstavelAposAtribuirId() {
		GastoEntity gasto = new GastoEntity();
		Set<GastoEntity> gastos = new HashSet<>();
		gastos.add(gasto);

		gasto.setId(UUID.randomUUID());

		assertTrue(gastos.contains(gasto));
	}
}
