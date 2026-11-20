package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.cfg.Configuration;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.QueryUtils;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoMetodoPagamentoResponse;
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
	void listaETotalizaPeloFechamentoOuPelaDataDoGastoIncluindoOsLimites() throws Exception {
		final var configuration = new Configuration()
				.addAnnotatedClass(Gasto.class)
				.addAnnotatedClass(CompraParcelada.class)
				.addAnnotatedClass(Fatura.class)
				.addAnnotatedClass(MetodoPagamento.class)
				.addAnnotatedClass(InstituicaoFinanceira.class)
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
			final var caixa = new InstituicaoFinanceira("Caixa");
			session.persist(caixa);
			final var credito = new Credito(1, 10);
			credito.setNome("Cartao");
			credito.setInstituicaoFinanceira(caixa);
			final var pix = new Pix();
			pix.setNome("Pix");
			pix.setInstituicaoFinanceira(caixa);
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
			creditoSemData.setCategoria("Outra");
			pixFim.setCategoria("Outra");
			final var creditoFaturaFora = criarGasto(credito, faturaFora, inicio, true);
			final var creditoSemFatura = criarGasto(credito, null, inicio, false);
			final var pixAntes = criarGasto(pix, null, inicio.minusDays(1), true);
			final var pixDepois = criarGasto(pix, null, fim.plusDays(1), false);
			final var pixSemData = criarGasto(pix, null, null, false);
			for (final var gasto : new Gasto[] { creditoDataFora, creditoSemData, pixInicio, pixFim,
					creditoFaturaFora, creditoSemFatura, pixAntes, pixDepois, pixSemData }) {
				session.persist(gasto);
			}
			final var debito = new Debito();
			debito.setNome("Debito");
			debito.setInstituicaoFinanceira(caixa);
			session.persist(debito);
			final var gastoFora = criarGasto(debito, null, inicio.minusDays(1), true);
			gastoFora.setCategoria("Fora");
			session.persist(gastoFora);
			session.flush();
			session.clear();
			final var realizados = Set.of(creditoDataFora.getId(), pixInicio.getId(), creditoFaturaFora.getId(),
					pixAntes.getId(), gastoFora.getId());
			final var todos = Stream.of(creditoDataFora, creditoSemData, pixInicio, pixFim, creditoFaturaFora,
					creditoSemFatura, pixAntes, pixDepois, pixSemData, gastoFora).map(Gasto::getId)
					.collect(Collectors.toSet());
			final var casos = Map.ofEntries(
					Map.entry(filtro(null, null, null, null, null), todos),
					Map.entry(filtro(inicio, fim, null, null, null),
							Set.of(creditoDataFora.getId(), creditoSemData.getId(), pixInicio.getId(), pixFim.getId())),
					Map.entry(filtro(inicio, fim, credito.getId(), null, null),
							Set.of(creditoDataFora.getId(), creditoSemData.getId())),
					Map.entry(filtro(inicio, fim, pix.getId(), null, null), Set.of(pixInicio.getId(), pixFim.getId())),
					Map.entry(filtro(inicio, fim, null, "Teste", null), Set.of(creditoDataFora.getId(), pixInicio.getId())),
					Map.entry(filtro(inicio, fim, null, "Outra", null), Set.of(creditoSemData.getId(), pixFim.getId())),
					Map.entry(filtro(inicio, fim, credito.getId(), "Teste", null), Set.of(creditoDataFora.getId())),
					Map.entry(filtro(inicio, fim, pix.getId(), "Outra", null), Set.of(pixFim.getId())),
					Map.entry(filtro(inicio, fim, UUID.randomUUID(), null, null), Set.<UUID>of()),
					Map.entry(filtro(inicio, fim, null, "Inexistente", null), Set.<UUID>of()),
					Map.entry(filtro(inicio, null, null, null, null), Set.of(creditoDataFora.getId(), creditoSemData.getId(),
							creditoFaturaFora.getId(), pixInicio.getId(), pixFim.getId(), pixDepois.getId())),
					Map.entry(filtro(null, fim, null, null, null), Set.of(creditoDataFora.getId(), creditoSemData.getId(),
							pixInicio.getId(), pixFim.getId(), pixAntes.getId(), gastoFora.getId())),
					Map.entry(filtro(null, null, null, null, faturaFim.getId()), Set.of(creditoSemData.getId())),
					Map.entry(filtro(null, null, null, null, faturaFora.getId()), Set.of(creditoFaturaFora.getId())),
					Map.entry(filtro(inicio, fim, null, null, faturaFora.getId()), Set.<UUID>of()),
					Map.entry(filtro(null, null, pix.getId(), null, null), Set.of(pixInicio.getId(), pixFim.getId(),
							pixAntes.getId(), pixDepois.getId(), pixSemData.getId())),
					Map.entry(filtro(null, null, null, "Fora", null), Set.of(gastoFora.getId())),
					Map.entry(FiltroGastos.porMetodosECategorias(List.of(credito.getId(), pix.getId()), null),
							Set.of(creditoDataFora.getId(), creditoSemData.getId(), pixInicio.getId(), pixFim.getId(),
									creditoFaturaFora.getId(), creditoSemFatura.getId(), pixAntes.getId(),
									pixDepois.getId(), pixSemData.getId())),
					Map.entry(FiltroGastos.porMetodosECategorias(null, List.of("Outra", "Fora")),
							Set.of(creditoSemData.getId(), pixFim.getId(), gastoFora.getId())),
					Map.entry(FiltroGastos.porMetodosECategorias(List.of(pix.getId(), debito.getId()), List.of("Outra", "Fora")),
							Set.of(pixFim.getId(), gastoFora.getId())));
			for (final var caso : casos.entrySet()) {
				for (final var realizado : List.of(true, false)) {
					final var filtros = caso.getKey();
					final var esperados = caso.getValue().stream()
							.filter(gastoId -> realizados.contains(gastoId) == realizado)
							.collect(Collectors.toSet());
					final var descricao = filtros + " realizado=" + realizado;
					final var resultados = listar(session, realizado, filtros).stream()
							.map(Gasto::getId).collect(Collectors.toSet());
					assertEquals(esperados, resultados, descricao);
					assertEquals((long) esperados.size(), contar(session, realizado, filtros), descricao);
				}
			}
			final var periodos = new PeriodosGastosRepositoryImpl(session);
			assertEquals(List.of(new PeriodoComGastosResponse(2026, 11, 2), new PeriodoComGastosResponse(2026, 10, 3)),
					periodos.listarPeriodosRealizados(FiltroGastos.porMetodosECategorias(null, null)));
			assertEquals(List.of(new PeriodoComGastosResponse(2026, 11, 1), new PeriodoComGastosResponse(2026, 10, 1)),
					periodos.listarPeriodosRealizados(FiltroGastos.porMetodosECategorias(List.of(pix.getId()), null)));
			assertEquals(List.of(new PeriodoComGastosResponse(2026, 10, 1)),
					periodos.listarPeriodosRealizados(FiltroGastos.porMetodosECategorias(
							List.of(pix.getId(), debito.getId()), List.of("Fora"))));
			assertEquals(List.of(new PeriodoComGastosResponse(2026, 11, 3), new PeriodoComGastosResponse(2026, 12, 1),
					new PeriodoComGastosResponse(null, null, 1)),
					periodos.listarPeriodosPlanejados(FiltroGastos.porMetodosECategorias(null, null)));
			assertEquals(List.of(new PeriodoComGastosResponse(2026, 11, 2)),
					periodos.listarPeriodosPlanejados(FiltroGastos.porMetodosECategorias(List.of(credito.getId()), null)));
			final var planejados = listar(session, false, filtro(null, null, null, null, null)).stream()
					.map(Gasto::getId).toList();
			assertEquals(creditoSemFatura.getId(), planejados.get(0));
			assertEquals(Set.of(creditoSemData.getId(), pixFim.getId()), Set.copyOf(planejados.subList(1, 3)));
			assertEquals(List.of(pixDepois.getId(), pixSemData.getId()), planejados.subList(3, 5));

			final var consultaUsoCategorias = GastoRepository.class.getMethod("contarGastosPorCategoria")
					.getAnnotation(Query.class);
			assertEquals(List.of(new UsoCategoriaResponse("Teste", 7L), new UsoCategoriaResponse("Outra", 2L),
					new UsoCategoriaResponse("Fora", 1L)),
					session.createQuery(consultaUsoCategorias.value(), UsoCategoriaResponse.class).getResultList());

			final var consultaCategorias = GastoRepository.class.getMethod("totalizarPorCategoria",
					LocalDate.class, LocalDate.class, Pageable.class).getAnnotation(Query.class);
			final var totaisCategorias = session.createQuery(consultaCategorias.value(), TotalCategoriaResponse.class)
					.setParameter("inicio", inicio)
					.setParameter("fim", fim)
					.getResultList().stream().collect(Collectors.toMap(TotalCategoriaResponse::categoria,
							total -> List.of(total.realizado().stripTrailingZeros(), total.planejado().stripTrailingZeros())));
			assertEquals(Map.of(
					"Teste", List.of(BigDecimal.TWO, BigDecimal.TWO),
					"Outra", List.of(BigDecimal.ZERO, BigDecimal.TWO)), totaisCategorias);

			final var consultaMetodos = GastoRepository.class.getMethod("totalizarPorMetodoPagamento",
					LocalDate.class, LocalDate.class, Pageable.class).getAnnotation(Query.class);
			final var totaisMetodos = session.createQuery(consultaMetodos.value(), TotalMetodoPagamentoResponse.class)
					.setParameter("inicio", inicio)
					.setParameter("fim", fim)
					.getResultList().stream().collect(Collectors.toMap(TotalMetodoPagamentoResponse::metodoPagamentoId,
							total -> List.of(total.realizado().stripTrailingZeros(), total.planejado().stripTrailingZeros())));
			assertEquals(Map.of(
					credito.getId(), List.of(BigDecimal.ONE, BigDecimal.TWO),
					pix.getId(), List.of(BigDecimal.ONE, BigDecimal.TWO)), totaisMetodos);
			for (final var total : session.createQuery(consultaMetodos.value(), TotalMetodoPagamentoResponse.class)
					.setParameter("inicio", inicio)
					.setParameter("fim", fim)
					.getResultList()) {
				assertEquals(caixa.getId(), total.instituicaoFinanceira().id());
				assertEquals("Caixa", total.instituicaoFinanceira().nome());
				assertEquals(false, total.instituicaoFinanceira().possuiIcone());
			}

			for (final var consulta : List.of(consultaCategorias, consultaMetodos)) {
				assertEquals(2L, session.createQuery(consulta.countQuery(), Long.class)
						.setParameter("inicio", inicio)
						.setParameter("fim", fim)
						.getSingleResult());
				assertEquals(1, session.createQuery(consulta.value(), Object.class)
						.setParameter("inicio", inicio)
						.setParameter("fim", fim)
						.setFirstResult(1)
						.setMaxResults(1)
						.getResultList().size());
				assertEquals(List.of(), session.createQuery(consulta.value(), Object.class)
						.setParameter("inicio", inicio.minusYears(1))
						.setParameter("fim", fim.minusYears(1))
						.getResultList());
				assertEquals(0L, session.createQuery(consulta.countQuery(), Long.class)
						.setParameter("inicio", inicio.minusYears(1))
						.setParameter("fim", fim.minusYears(1))
						.getSingleResult());
			}
			session.getTransaction().rollback();
		}
	}

	@Test
	void listaCadaCompraParceladaUmaVezExcetoNoFiltroPorFatura() throws Exception {
		final var configuration = new Configuration()
				.addAnnotatedClass(Gasto.class)
				.addAnnotatedClass(CompraParcelada.class)
				.addAnnotatedClass(Fatura.class)
				.addAnnotatedClass(MetodoPagamento.class)
				.addAnnotatedClass(InstituicaoFinanceira.class)
				.addAnnotatedClass(Credito.class)
				.addAnnotatedClass(Debito.class)
				.addAnnotatedClass(Pix.class)
				.addAnnotatedClass(Boleto.class)
				.addAnnotatedClass(Doc.class)
				.addAnnotatedClass(Ted.class)
				.setProperty("hibernate.connection.driver_class", "org.h2.Driver")
				.setProperty("hibernate.connection.url", "jdbc:h2:mem:gastos_parcelados;DB_CLOSE_DELAY=-1")
				.setProperty("hibernate.hbm2ddl.auto", "create-drop");
		final var dataCompra = LocalDate.of(2026, 11, 9);
		try (final var factory = configuration.buildSessionFactory();
				final var session = factory.openSession()) {
			session.beginTransaction();
			final var nubank = new InstituicaoFinanceira("Nubank");
			session.persist(nubank);
			final var credito = new Credito(14, 21);
			credito.setNome("Cartao");
			credito.setInstituicaoFinanceira(nubank);
			session.persist(credito);
			final var compra = new CompraParcelada();
			compra.setValorTotal(BigDecimal.valueOf(300));
			session.persist(compra);
			final var faturas = new ArrayList<Fatura>();
			for (int numero = 1; numero <= 3; numero++) {
				final var fatura = criarFatura(credito, LocalDate.of(2026, 11, 14).plusMonths(numero - 1L));
				session.persist(fatura);
				faturas.add(fatura);
				final var parcela = criarGasto(credito, fatura, dataCompra, true);
				parcela.setCompraParcelada(compra);
				parcela.setNumeroParcela(numero);
				session.persist(parcela);
			}
			final var avulso = criarGasto(credito, faturas.get(1), dataCompra, true);
			session.persist(avulso);
			final var pix = new Pix();
			pix.setNome("Pix");
			pix.setInstituicaoFinanceira(nubank);
			session.persist(pix);
			session.persist(criarGasto(pix, null, dataCompra, false));
			final var semUso = new Pix();
			semUso.setNome("Sem uso");
			semUso.setInstituicaoFinanceira(nubank);
			session.persist(semUso);
			session.flush();
			session.clear();

			final var listados = listar(session, true, filtro(null, null, null, null, null));
			assertEquals(2, listados.size());
			assertEquals(Set.of(1), listados.stream().filter((gasto) -> gasto.getCompraParcelada() != null)
					.map(Gasto::getNumeroParcela).collect(Collectors.toSet()));

			final var naFatura = listar(session, true, filtro(null, null, null, null, faturas.get(1).getId()));
			assertEquals(2, naFatura.size());
			assertEquals(Set.of(2), naFatura.stream().filter((gasto) -> gasto.getCompraParcelada() != null)
					.map(Gasto::getNumeroParcela).collect(Collectors.toSet()));

			assertEquals(List.of(new PeriodoComGastosResponse(2026, 11, 2)), new PeriodosGastosRepositoryImpl(session)
					.listarPeriodosRealizados(FiltroGastos.porMetodosECategorias(null, null)));

			final var consultaUso = GastoRepository.class.getMethod("contarGastosPorMetodoPagamento")
					.getAnnotation(Query.class);
			final var uso = session.createQuery(consultaUso.value(), UsoMetodoPagamentoResponse.class)
					.getResultList().stream().collect(Collectors.toMap(
							UsoMetodoPagamentoResponse::metodoPagamentoId, UsoMetodoPagamentoResponse::quantidadeGastos));
			assertEquals(Map.of(credito.getId(), 2L, pix.getId(), 1L), uso);
			session.getTransaction().rollback();
		}
	}

	private static FiltroGastos filtro(
			LocalDate inicio, LocalDate fim, UUID metodoPagamentoId, String categoria, UUID faturaId) {
		return FiltroGastos.de(inicio, fim, metodoPagamentoId == null ? null : List.of(metodoPagamentoId),
				categoria == null ? null : List.of(categoria), faturaId);
	}

	private static List<Gasto> listar(Session session, boolean realizado, FiltroGastos filtro) {
		final var cb = session.getCriteriaBuilder();
		final var consulta = cb.createQuery(Gasto.class);
		final var root = consulta.from(Gasto.class);
		consulta.where(GastoSpecifications.listagem(realizado, filtro).toPredicate(root, consulta, cb));
		return session.createQuery(consulta).getResultList();
	}

	private static long contar(Session session, boolean realizado, FiltroGastos filtro) {
		final var cb = session.getCriteriaBuilder();
		final var consulta = cb.createQuery(Long.class);
		final var root = consulta.from(Gasto.class);
		consulta.select(cb.count(root))
				.where(GastoSpecifications.listagem(realizado, filtro).toPredicate(root, consulta, cb));
		return session.createQuery(consulta).getSingleResult();
	}

	private Fatura criarFatura(MetodoPagamento metodo, LocalDate vencimento) {
		final var fatura = new Fatura();
		fatura.setMetodoPagamento(metodo);
		fatura.setDataFechamento(vencimento.minusDays(7));
		fatura.setDataVencimento(vencimento);
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
				.addAnnotatedClass(InstituicaoFinanceira.class)
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
					case "listarResumosPorMetodoPagamentoId" ->
						Sort.by(TypedPropertyPath.path(Fatura::getDataFechamento), TypedPropertyPath.path(Fatura::getId));
					case "totalizarPorCategoria" ->
						Sort.by(TypedPropertyPath.path(Gasto::getCategoria));
					case "contarGastosPorMetodoPagamento", "contarGastosPorCategoria" -> Sort.unsorted();
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
