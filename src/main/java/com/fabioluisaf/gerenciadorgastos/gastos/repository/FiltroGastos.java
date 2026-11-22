package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record FiltroGastos(
	LocalDate inicio,
	LocalDate fim,
	Set<UUID> metodoPagamentoIds,
	Set<String> categorias,
	UUID faturaId,
	String nome
) {

	public FiltroGastos {
		metodoPagamentoIds = metodoPagamentoIds == null ? Set.of() : Set.copyOf(metodoPagamentoIds);
		categorias = categorias == null ? Set.of() : Set.copyOf(categorias);
		nome = normalizarNome(nome);
	}

	public static FiltroGastos de(
			LocalDate inicio, LocalDate fim, Collection<UUID> metodoPagamentoIds, Collection<String> categorias,
			UUID faturaId) {

		return new FiltroGastos(inicio, fim, paraConjunto(metodoPagamentoIds), paraConjunto(categorias), faturaId, null);
	}

	public static FiltroGastos porMetodosECategorias(
			Collection<UUID> metodoPagamentoIds, Collection<String> categorias) {

		return de(null, null, metodoPagamentoIds, categorias, null);
	}

	public FiltroGastos comNome(String nome) {
		return new FiltroGastos(inicio, fim, metodoPagamentoIds, categorias, faturaId, nome);
	}

	public static String normalizarNome(String nome) {
		if (nome == null || nome.isBlank()) {
			return null;
		}
		return Normalizer.normalize(nome.strip(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
	}

	private static <T> Set<T> paraConjunto(Collection<T> valores) {
		return valores == null ? null : Set.copyOf(valores);
	}
}
