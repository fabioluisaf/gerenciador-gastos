package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;

import com.fabioluisaf.gerenciadorgastos.gastos.model.GrupoPlanejado;
import com.fabioluisaf.gerenciadorgastos.gastos.model.ItemGrupoPlanejado;

class GrupoPlanejadoPersistenciaTest {

	private static ItemGrupoPlanejado item(String nome, String valor) {
		return new ItemGrupoPlanejado(nome, new BigDecimal(valor), "Casa", null);
	}

	@Test
	void guardaOsItensNaOrdemESubstituiTodosAoAtualizar() {
		final var configuration = new Configuration()
				.addAnnotatedClass(GrupoPlanejado.class)
				.setProperty("hibernate.connection.driver_class", "org.h2.Driver")
				.setProperty("hibernate.connection.url", "jdbc:h2:mem:grupos_planejados;DB_CLOSE_DELAY=-1")
				.setProperty("hibernate.hbm2ddl.auto", "create-drop");
		try (final var factory = configuration.buildSessionFactory()) {
			final var grupo = new GrupoPlanejado();
			grupo.setNome("Contas fixas");
			grupo.substituirItens(List.of(item("Luz", "120.00"), item("Agua", "80.50"), item("Gas", "40.00")));
			factory.inTransaction(session -> session.persist(grupo));

			factory.inTransaction(session -> {
				final var salvo = session.find(GrupoPlanejado.class, grupo.getId());
				assertEquals(List.of("Luz", "Agua", "Gas"), nomes(salvo));
				assertEquals(new BigDecimal("240.50"), salvo.getValorTotal());
				salvo.substituirItens(List.of(item("Internet", "99.90"), item("Luz", "130.00")));
			});

			factory.inTransaction(session -> {
				final var atualizado = session.find(GrupoPlanejado.class, grupo.getId());
				assertEquals(List.of("Internet", "Luz"), nomes(atualizado));
				session.remove(atualizado);
			});

			factory.inSession(session -> assertEquals(0L, session
					.createNativeQuery("SELECT COUNT(*) FROM itens_grupos_planejados", Long.class)
					.getSingleResult()));
		}
	}

	private static List<String> nomes(GrupoPlanejado grupo) {
		return grupo.getItens().stream().map(ItemGrupoPlanejado::getNome).toList();
	}
}
