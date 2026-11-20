package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public record FiltroGastos(
	LocalDate inicio,
	LocalDate fim,
	Set<UUID> metodoPagamentoIds,
	Set<String> categorias,
	UUID faturaId
) {

	public FiltroGastos {
		metodoPagamentoIds = metodoPagamentoIds == null ? Set.of() : Set.copyOf(metodoPagamentoIds);
		categorias = categorias == null ? Set.of() : Set.copyOf(categorias);
	}

	public static FiltroGastos de(
			LocalDate inicio, LocalDate fim, Collection<UUID> metodoPagamentoIds, Collection<String> categorias,
			UUID faturaId) {

		return new FiltroGastos(inicio, fim, paraConjunto(metodoPagamentoIds), paraConjunto(categorias), faturaId);
	}

	public static FiltroGastos porMetodosECategorias(
			Collection<UUID> metodoPagamentoIds, Collection<String> categorias) {

		return de(null, null, metodoPagamentoIds, categorias, null);
	}

	private static <T> Set<T> paraConjunto(Collection<T> valores) {
		return valores == null ? null : Set.copyOf(valores);
	}
}
