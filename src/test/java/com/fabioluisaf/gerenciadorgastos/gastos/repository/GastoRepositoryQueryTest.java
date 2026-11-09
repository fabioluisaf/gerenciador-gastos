package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.stream.Stream;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.QueryUtils;

import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;

// Eclipse cannot infer the non-null receiver of Spring Data property references.
@SuppressWarnings("null")
class GastoRepositoryQueryTest {

	@Test
	void validaConsultasSemConectarAoBanco() {
		final var configuration = new Configuration()
				.addAnnotatedClass(Gasto.class)
				.addAnnotatedClass(CompraParcelada.class)
				.addAnnotatedClass(Fatura.class)
				.addAnnotatedClass(MetodoPagamento.class)
				.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
				.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");

		try (final var factory = configuration.buildSessionFactory();
				final var session = factory.openSession()) {
			final var methods = Stream.concat(Stream.of(GastoRepository.class.getDeclaredMethods()),
					Stream.of(FaturaRepository.class.getDeclaredMethods())).toList();
			for (final var method : methods) {
				final var query = method.getAnnotation(Query.class);
				if (query == null) {
					continue;
				}
				final var sort = switch (method.getName()) {
					case "listarResumosPorMetodoPagamentoId" ->
						Sort.by(TypedPropertyPath.path(Fatura::getDataFechamento), TypedPropertyPath.path(Fatura::getId));
					case "totalizarPorCategoria" ->
						Sort.by(TypedPropertyPath.path(Gasto::getCategoria));
					case "totalizarPorMetodoPagamento" ->
						Sort.by(TypedPropertyPath.path(Gasto::getMetodoPagamento)
								.then(MetodoPagamento::getId));
					default -> Sort.by(TypedPropertyPath.path(Gasto::getData), TypedPropertyPath.path(Gasto::getId));
				};
				final var ordenada = QueryUtils.applySorting(query.value(), sort);

				assertDoesNotThrow(() -> session.createQuery(ordenada, Object.class), method.getName());
				if (!query.countQuery().isEmpty()) {
					assertDoesNotThrow(() -> session.createQuery(query.countQuery(), Long.class), method.getName());
				}
			}
		}
	}
}
