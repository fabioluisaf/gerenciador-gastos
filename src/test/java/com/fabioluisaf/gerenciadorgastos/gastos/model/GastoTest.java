package com.fabioluisaf.gerenciadorgastos.gastos.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class GastoTest {

	@Test
	void entidadesSaoIguaisSomenteQuandoCompartilhamId() {
		final var id = UUID.randomUUID();
		final var primeiro = new Gasto();
		final var segundo = new Gasto();
		assertNotEquals(primeiro, segundo);
		assertNotEquals(null, primeiro);
		final var hash = primeiro.hashCode();
		primeiro.setId(id);
		assertEquals(hash, primeiro.hashCode());
		segundo.setId(id);
		assertEquals(primeiro, segundo);
		assertEquals(primeiro.hashCode(), segundo.hashCode());
		segundo.setId(UUID.randomUUID());
		assertNotEquals(primeiro, segundo);
	}
}
