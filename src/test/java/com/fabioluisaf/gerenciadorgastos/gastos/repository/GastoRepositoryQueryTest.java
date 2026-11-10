package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.QueryUtils;

import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Credito;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Debito;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Pix;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Boleto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Doc;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Ted;

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
				.addAnnotatedClass(Credito.class)
				.addAnnotatedClass(Debito.class)
				.addAnnotatedClass(Pix.class)
				.addAnnotatedClass(Boleto.class)
				.addAnnotatedClass(Doc.class)
				.addAnnotatedClass(Ted.class)
				.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
				.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");

		try (final var factory = configuration.buildSessionFactory();
				final var session = factory.openSession()) {
			assertEquals(Integer.class, factory.getMetamodel().entity(Credito.class)
					.getAttribute("diaFechamento").getJavaType());
			assertThrows(IllegalArgumentException.class,
					() -> factory.getMetamodel().entity(Debito.class).getAttribute("diaFechamento"));
			assertThrows(IllegalArgumentException.class,
					() -> factory.getMetamodel().entity(Pix.class).getAttribute("diaFechamento"));
			for (final var tipo : new Class<?>[] { Boleto.class, Doc.class, Ted.class }) {
				assertThrows(IllegalArgumentException.class,
						() -> factory.getMetamodel().entity(tipo).getAttribute("diaFechamento"));
			}
			assertDoesNotThrow(() -> session.createQuery("from MetodoPagamento", MetodoPagamento.class));
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
