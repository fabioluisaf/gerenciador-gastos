package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
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
	void selecionaPeloFechamentoOuPelaDataDoGastoIncluindoOsLimites() throws Exception {
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
				.setProperty("hibernate.connection.driver_class", "org.h2.Driver")
				.setProperty("hibernate.connection.url", "jdbc:h2:mem:gastos_intervalo;DB_CLOSE_DELAY=-1")
				.setProperty("hibernate.hbm2ddl.auto", "create-drop");
		final var inicio = LocalDate.of(2026, 11, 1);
		final var fim = LocalDate.of(2026, 11, 30);
		try (final var factory = configuration.buildSessionFactory();
				final var session = factory.openSession()) {
			session.beginTransaction();
			final var credito = new Credito(1, 10);
			credito.setNome("Cartao");
			credito.setInstituicaoFinanceira(InstituicaoFinanceira.CAIXA);
			final var pix = new Pix();
			pix.setNome("Pix");
			pix.setInstituicaoFinanceira(InstituicaoFinanceira.CAIXA);
			session.persist(credito);
			session.persist(pix);
			final var faturaInicio = criarFatura(credito, inicio);
			final var faturaFim = criarFatura(credito, fim);
			final var faturaFora = criarFatura(credito, fim.plusDays(1));
			session.persist(faturaInicio);
			session.persist(faturaFim);
			session.persist(faturaFora);
			final var creditoDataFora = criarGasto(credito, faturaInicio, inicio.minusMonths(1), true);
			final var creditoSemData = criarGasto(credito, faturaFim, null, false);
			final var pixInicio = criarGasto(pix, null, inicio, true);
			final var pixFim = criarGasto(pix, null, fim, false);
			for (final var gasto : new Gasto[] { creditoDataFora, creditoSemData, pixInicio, pixFim,
					criarGasto(credito, faturaFora, inicio, true),
					criarGasto(pix, null, inicio.minusDays(1), true),
					criarGasto(pix, null, fim.plusDays(1), false),
					criarGasto(pix, null, null, false) }) {
				session.persist(gasto);
			}
			session.flush();
			session.clear();
			final var hql = GastoRepository.class.getMethod("listarPorIntervalo", LocalDate.class, LocalDate.class,
					org.springframework.data.domain.Pageable.class).getAnnotation(Query.class).value();
			final var resultados = session.createQuery(hql, Gasto.class)
					.setParameter("inicio", inicio)
					.setParameter("fim", fim)
					.getResultList().stream().map(Gasto::getId).collect(Collectors.toSet());
			assertEquals(Set.of(creditoDataFora.getId(), creditoSemData.getId(), pixInicio.getId(), pixFim.getId()),
					resultados);
			final var total = session.createQuery(hql.replace("select g from Gasto g", "select count(g) from Gasto g"),
					Long.class)
					.setParameter("inicio", inicio)
					.setParameter("fim", fim)
					.getSingleResult();
			assertEquals(4L, total);
			final var pagina = session.createQuery(hql + " order by g.id", Gasto.class)
					.setParameter("inicio", inicio)
					.setParameter("fim", fim)
					.setFirstResult(2)
					.setMaxResults(2)
					.getResultList();
			assertEquals(2, pagina.size());
			session.getTransaction().rollback();
		}
	}

	private Fatura criarFatura(MetodoPagamento metodo, LocalDate fechamento) {
		final var fatura = new Fatura();
		fatura.setMetodoPagamento(metodo);
		fatura.setDataFechamento(fechamento);
		fatura.setDataVencimento(fechamento.plusDays(10));
		return fatura;
	}

	private Gasto criarGasto(MetodoPagamento metodo, Fatura fatura, LocalDate data, boolean realizado) {
		final var gasto = new Gasto();
		gasto.setNome(UUID.randomUUID().toString());
		gasto.setCategoria("Teste");
		gasto.setValor(BigDecimal.ONE);
		gasto.setMetodoPagamento(metodo);
		gasto.setFatura(fatura);
		gasto.setData(data);
		gasto.setRealizado(realizado);
		return gasto;
	}

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
			final var debito = factory.getMetamodel().entity(Debito.class);
			assertThrows(IllegalArgumentException.class, () -> debito.getAttribute("diaFechamento"));
			final var pix = factory.getMetamodel().entity(Pix.class);
			assertThrows(IllegalArgumentException.class, () -> pix.getAttribute("diaFechamento"));
			for (final var tipo : new Class<?>[] { Boleto.class, Doc.class, Ted.class }) {
				final var entidade = factory.getMetamodel().entity(tipo);
				assertThrows(IllegalArgumentException.class, () -> entidade.getAttribute("diaFechamento"));
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
					case "listarPeriodosComGastos" -> Sort.unsorted();
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
